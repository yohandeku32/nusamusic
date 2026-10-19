package com.yohandeku32.nusamusic.data

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.CacheWriter
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.ensureActive
import java.io.File

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
            .setUserAgent("NusaMusic/SpotifyCanvas")
            .setConnectTimeoutMs(4_000)
            .setReadTimeoutMs(8_000)
            .setAllowCrossProtocolRedirects(false)
        return CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(upstream)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }

    fun mediaSourceFactory(context: Context): DefaultMediaSourceFactory? {
        val source = dataSourceFactory() ?: return null
        return DefaultMediaSourceFactory(context.applicationContext)
            .setDataSourceFactory(source)
    }

    /**
     * Speculatively cache only the first 512 KiB of the NEXT track, never
     * download whole albums/playlists. The URL has already been validated by
     * SpotifyCanvasArtworkLoader. Canceled prefetches don't affect playback.
     */
    suspend fun prefetchNext(context: Context, url: String) {
        if (!SpotifyCanvasArtworkLoader.isSpotifyCanvasVideo(url)) return
        prepare(context)
        withContext(Dispatchers.IO) {
            val prefetchContext = currentCoroutineContext()
            prefetchMutex.withLock {
                prefetchContext.ensureActive()
                val factory = dataSourceFactory() ?: return@withLock
                val spec = DataSpec.Builder()
                    .setUri(url)
                    .setPosition(0)
                    .setLength(512L * 1024)
                    .build()
                lateinit var writer: CacheWriter
                // CacheWriter.cache() performs blocking reads. Cancelling the
                // coroutine alone doesn't stop them, so cancel the writer at
                // the next progress callback when the queue changes.
                val progress = CacheWriter.ProgressListener { _, _, _ ->
                    if (!prefetchContext.isActive) writer.cancel()
                }
                writer = CacheWriter(factory.createDataSource(), spec, null, progress)
                try {
                    writer.cache()
                    prefetchContext.ensureActive()
                } catch (cancelled: CancellationException) {
                    writer.cancel()
                    throw cancelled
                } catch (_: Exception) {
                    // Network errors are best-effort, but an IO exception
                    // caused by cancellation must not turn into normal work.
                    prefetchContext.ensureActive()
                } finally {
                    if (!prefetchContext.isActive) writer.cancel()
                }
            }
        }
    }
}
