package com.yohandeku32.nusamusic.data

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.URI

/**
 * Bounded HLS warm cache for Apple portrait artwork video bytes.
 *
 * Cache the selected playlist and first segment(s), then give the SAME cache
 * data source to Media3 when the song starts. Merely prefetching an m3u8 URL
 * would not make the first video frame available any faster.
 *
 * Never initialize SimpleCache from Compose/draw; do all disk and network IO
 * on Dispatchers.IO. This is HLS only; Spotify MP4 has its own cache.
 */
@OptIn(UnstableApi::class)
internal object AppleTallVideoCache {
    private const val MAX_CACHE_BYTES = 48L * 1024 * 1024
    private const val DIR_NAME = "apple-tall-hls-v1"
    private val openMutex = Mutex()
    @Volatile private var store: SimpleCache? = null

    suspend fun prepare(context: Context) {
        if (store != null) return
        withContext(Dispatchers.IO) {
            openMutex.withLock {
                if (store == null) {
                    store = runCatching {
                        SimpleCache(
                            File(context.applicationContext.cacheDir, DIR_NAME),
                            LeastRecentlyUsedCacheEvictor(MAX_CACHE_BYTES),
                            StandaloneDatabaseProvider(context.applicationContext)
                        )
                    }.getOrNull()
                }
            }
        }
    }

    private fun sourceFactory(): CacheDataSource.Factory? {
        val cache = store ?: return null
        return CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(
                DefaultHttpDataSource.Factory()
                    .setUserAgent("NusaMusic/AppleTall")
                    .setConnectTimeoutMs(4_000)
                    .setReadTimeoutMs(8_000)
                    .setAllowCrossProtocolRedirects(false)
            )
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }

    fun mediaSourceFactory(context: Context): DefaultMediaSourceFactory? {
        val factory = sourceFactory() ?: return null
        return DefaultMediaSourceFactory(context.applicationContext)
            .setDataSourceFactory(factory)
    }

    private fun allowed(uri: URI): Boolean {
        val host = uri.host?.lowercase() ?: return false
        return uri.scheme.equals("https", true) && (
            host == "mzstatic.com" || host.endsWith(".mzstatic.com") ||
                host == "apple.com" || host.endsWith(".apple.com") ||
                host == "cdn-apple.com" || host.endsWith(".cdn-apple.com")
            )
    }

    /**
     * Read only the requested prefix; CacheDataSource writes it to disk.
     * The Media3 HLS source can resume loading the remainder transparently.
     */
    private suspend fun read(
        factory: CacheDataSource.Factory,
        url: URI,
        limitBytes: Int
    ): ByteArray? {
        if (!allowed(url)) return null
        return withContext(Dispatchers.IO) {
            val source = factory.createDataSource()
            val bytes = ByteArrayOutputStream(minOf(limitBytes, 32 * 1024))
            try {
                source.open(DataSpec.Builder().setUri(url.toString()).build())
                val chunk = ByteArray(16 * 1024)
                while (bytes.size() < limitBytes) {
                    currentCoroutineContext().ensureActive()
                    val count = source.read(chunk, 0, minOf(chunk.size, limitBytes - bytes.size()))
                    if (count < 0) break
                    if (count == 0) continue
                    bytes.write(chunk, 0, count)
                }
                bytes.toByteArray()
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                null
            } finally {
                runCatching { source.close() }
            }
        }
    }

    private fun mediaUris(playlist: String, base: URI): List<URI> {
        val lines = playlist.lineSequence().map(String::trim).toList()
        val init = Regex("""#EXT-X-MAP:.*URI="([^"]+)"""")
            .find(playlist)?.groupValues?.getOrNull(1)
        val segments = lines.filter { it.isNotBlank() && !it.startsWith("#") }
        return (listOfNotNull(init) + segments).mapNotNull { value ->
            runCatching { base.resolve(value) }.getOrNull()?.takeIf(::allowed)
        }
    }

    suspend fun warm(context: Context, url: String, firstInQueue: Boolean) {
        val initial = runCatching { URI(url) }.getOrNull() ?: return
        if (!allowed(initial)) return
        prepare(context)
        val factory = sourceFactory() ?: return
        val first = read(factory, initial, 160 * 1024)
            ?.toString(Charsets.UTF_8)
            ?: return
        if (!first.trimStart().startsWith("#EXTM3U")) return

        // Warm the first child playlist from the selected portrait stream.
        val firstUri = mediaUris(first, initial).firstOrNull() ?: return
        val playlistUri: URI
        val playlist: String
        if (first.contains("#EXT-X-STREAM-INF")) {
            playlistUri = firstUri
            playlist = read(factory, playlistUri, 160 * 1024)
                ?.toString(Charsets.UTF_8) ?: return
            if (!playlist.trimStart().startsWith("#EXTM3U")) return
        } else {
            playlistUri = initial
            playlist = first
        }
        val resources = mediaUris(playlist, playlistUri)
        // First queued song gets a larger warm buffer, remaining four are
        // bounded so Wi-Fi/data bandwidth isn't saturated in parallel.
        val maxSegments = if (firstInQueue) 2 else 1
        for (segment in resources.take(maxSegments)) {
            currentCoroutineContext().ensureActive()
            read(factory, segment, if (firstInQueue) 1_000_000 else 400_000)
        }
    }
}
