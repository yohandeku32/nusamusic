package com.yohandeku32.nusamusic.data

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.io.File
import java.net.URL

/**
 * Process-wide, disk-backed cache for Spotify Canvas MP4 loops only.
 *
 * Apple Music HLS keeps its existing network path. All disk indexing happens
 * on Dispatchers.IO before Compose builds the player (never on a draw frame).
 * One SimpleCache owns one dedicated directory for the lifetime of the app.
 * Eviction is automatic; missing/unavailable cache fails open to streaming.
 *
 * Cached bytes survive process restarts until Android evicts app cache.
 */
@OptIn(UnstableApi::class)
internal object SpotifyCanvasVideoCache {
    private const val MAX_BYTES = 64L * 1024 * 1024
    private const val DIR_NAME = "spotify-canvas-video-v1"
    private val initMutex = Mutex()
    // Speculative requests must not compete with each other during rapid skips.
    private val prefetchMutex = Mutex()

    @Volatile private var store: SimpleCache? = null

    // Give the foreground stream its first frame or rebuffer before a
    // speculative request can consume the same CDN/cache I/O bandwidth.
    @Volatile private var foregroundBufferingUrl: String? = null

    fun setForegroundBuffering(url: String, buffering: Boolean) {
        if (!SpotifyCanvasArtworkLoader.isSpotifyCanvasVideo(url)) return
        if (buffering) foregroundBufferingUrl = url
        else if (foregroundBufferingUrl == url) foregroundBufferingUrl = null
    }

    suspend fun prepare(context: Context) {
        if (store != null) return
        val app = context.applicationContext
        withContext(Dispatchers.IO) {
            initMutex.withLock {
                if (store != null) return@withLock
                // Cache indexing can touch disk; never perform it on the
                // rendering thread. A failure does not block video playback.
                store = runCatching {
                    SimpleCache(
                        File(app.cacheDir, DIR_NAME),
                        LeastRecentlyUsedCacheEvictor(MAX_BYTES),
                        StandaloneDatabaseProvider(app)
                    )
                }.getOrNull()
            }
        }
    }

    /**
     * Factory creation is lightweight; SimpleCache has already opened on IO.
     * Return null until ready so first playback works even without caching.
     */
    private fun dataSourceFactory(): CacheDataSource.Factory? {
        val cache = store ?: return null
        val upstream = DefaultHttpDataSource.Factory()
            .setUserAgent("Aruna/SpotifyCanvas")
            .setConnectTimeoutMs(4_000)
            .setReadTimeoutMs(8_000)
            .setAllowCrossProtocolRedirects(false)
        return spotifyCanvasDataSourceFactory(cache, upstream)
    }

    fun mediaSourceFactory(context: Context): DefaultMediaSourceFactory? {
        val source = dataSourceFactory() ?: return null
        return DefaultMediaSourceFactory(context.applicationContext)
            .setDataSourceFactory(source)
    }

    /**
     * Cache the entire short Canvas loop whenever it fits within a bounded
     * byte budget. The previous fixed-length CacheWriter (4 MiB for Next)
     * could read beyond EOF for a short MP4 and produce a failed prefetch,
     * making every repeat go back to the third-party CDN.
     *
     * An unknown-length DataSpec reads until real EOF. Normal Media3 playback
     * shares the exact same SimpleCache; cached loops replay without network
     * buffering. If the clip exceeds the budget it still retains a useful
     * prefix and falls back to streaming.
     */
    suspend fun prefetchNext(
        context: Context, url: String, immediateNext: Boolean = false
    ) {
        if (!SpotifyCanvasArtworkLoader.isSpotifyCanvasVideo(url)) return
        prepare(context)
        withContext(Dispatchers.IO) {
            prefetchMutex.withLock {
                val factory = dataSourceFactory() ?: return@withLock
                // Up to five prepared tracks but only one low-priority reader.
                val maxBytes = if (immediateNext) 10L * 1024 * 1024
                    else 3L * 1024 * 1024
                try {
                    // Foreground video wins at startup or during a stall.
                    // Do not start a secondary request that competes with it.
                    if (!waitForForeground()) return@withLock
                    currentCoroutineContext().ensureActive()
                    fillSpotifyCanvasCache(factory.createDataSource(), url, maxBytes) { foregroundBufferingUrl != null }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    // A slow or temporarily unavailable CDN does not erase
                    // the selected provider or previously committed bytes.
                    currentCoroutineContext().ensureActive()
                }
            }
        }
    }

    private suspend fun waitForForeground(): Boolean {
        var elapsed = 0L
        while (foregroundBufferingUrl != null && elapsed < 6_000L) {
            kotlinx.coroutines.currentCoroutineContext().ensureActive()
            delay(100L)
            elapsed += 100L
        }
        return foregroundBufferingUrl == null
    }
}

@OptIn(UnstableApi::class)
internal fun spotifyCanvasDataSourceFactory(cache: Cache, upstream: DataSource.Factory): CacheDataSource.Factory =
    CacheDataSource.Factory().setCache(cache).setUpstreamDataSourceFactory(upstream)
        .setCacheKeyFactory { it.key ?: spotifyCanvasCacheKey(it.uri.toString()) }
        .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

/** Preserve the common legacy key while sharing trusted host/query-order aliases. */
internal fun spotifyCanvasCacheKey(value: String): String {
    if (!SpotifyCanvasArtworkLoader.isSpotifyCanvasVideo(value)) return value
    val url = URL(value)
    if (url.host.equals("canvaz.scdn.co", true)) return value
    val id = url.query.split('&').first { it.startsWith("id=") }.substringAfter('=')
    return "https://www.spotycovs.lol/api/canvas?id=$id&stream=1"
}

@OptIn(UnstableApi::class)
internal suspend fun fillSpotifyCanvasCache(
    reader: DataSource, url: String, maxBytes: Long, shouldYield: () -> Boolean
) {
    try {
        currentCoroutineContext().ensureActive()
        if (maxBytes <= 0L || shouldYield()) return
        reader.open(DataSpec.Builder().setUri(url).build())
        val buffer = ByteArray(32 * 1024)
        var readBytes = 0L
        while (readBytes < maxBytes) {
            currentCoroutineContext().ensureActive()
            // Release the writer when playback needs bandwidth; never wait while holding its span.
            if (shouldYield()) break
            val count = reader.read(buffer, 0, minOf(buffer.size.toLong(), maxBytes - readBytes).toInt())
            if (count == -1) break
            if (count > 0) readBytes += count
        }
    } finally { runCatching { reader.close() } }
}
