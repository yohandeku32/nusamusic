package com.yohandeku32.nusamusic.data

import android.content.Context
import com.yohandeku32.nusamusic.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.util.Locale

/**
 * One result per track, shared by speculative preloads and foreground playback.
 *
 * Apple Music's genuine Tall stream is always checked first; a negative Apple
 * result is the only reason to consult Spotify Canvas. Do not start five
 * simultaneous album lookups: limit network resolution to two tracks at once,
 * and warm actual video bytes one stream at a time.
 */
internal object AnimatedArtworkQueuePreloader {
    enum class Provider { APPLE_TALL, SPOTIFY_CANVAS }
    data class Result(val provider: Provider, val url: String)

    private data class Entry(val result: Result?, val expiresAt: Long)
    private val monitor = Any()
    private val resolved = object : LinkedHashMap<String, Entry>(64, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Entry>?): Boolean =
            size > 96
    }
    private val inFlight = HashMap<String, Deferred<Result?>>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val lookupSlots = Semaphore(2)
    private val videoSlots = Semaphore(1)

    private const val SUCCESS_TTL_MS = 12L * 60 * 60 * 1_000
    private const val MISS_TTL_MS = 45_000L

    private fun key(song: Song): String =
        listOf(song.uri, song.title, song.artist, song.album)
            .joinToString("|") { it.trim().lowercase(Locale.ROOT) }

    fun ready(song: Song): Result? = synchronized(monitor) {
        resolved[key(song)]?.takeIf { it.expiresAt > System.currentTimeMillis() }?.result
    }

    /**
     * Cancellations of a stale queue's speculative await never kill a lookup
     * already shared with the Now Playing screen.
     */
    suspend fun resolve(context: Context, song: Song): Result? {
        val cacheKey = key(song)
        val now = System.currentTimeMillis()
        val task: Deferred<Result?>
        synchronized(monitor) {
            val existing = resolved[cacheKey]
            if (existing != null && existing.expiresAt > now) return existing.result
            task = inFlight[cacheKey] ?: scope.async {
                val result = try {
                    lookupSlots.withPermit {
                        val apple = AppleMusicAnimatedArtworkLoader.loadAnimatedArtworkUrl(
                            context.applicationContext, song
                        )
                        if (apple != null) Result(Provider.APPLE_TALL, apple)
                        else {
                            SpotifyCanvasArtworkLoader.findCanvasUrl(
                                context.applicationContext, song
                            )?.let { Result(Provider.SPOTIFY_CANVAS, it) }
                        }
                    }
                } catch (cancelled: kotlinx.coroutines.CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    null
                }
                synchronized(monitor) {
                    resolved[cacheKey] = Entry(
                        result, System.currentTimeMillis() +
                            if (result == null) MISS_TTL_MS else SUCCESS_TTL_MS
                    )
                    inFlight.remove(cacheKey)
                }
                result
            }.also { inFlight[cacheKey] = it }
        }
        return task.await()
    }

    /** Preload the chosen provider's video bytes, not just the artwork URL. */
    suspend fun preload(context: Context, song: Song, priority: Int) {
        val candidate = resolve(context, song) ?: return
        videoSlots.withPermit {
            when (candidate.provider) {
                Provider.APPLE_TALL ->
                    AppleTallVideoCache.warm(context, candidate.url, firstInQueue = priority == 0)
                Provider.SPOTIFY_CANVAS ->
                    SpotifyCanvasVideoCache.prefetchNext(context, candidate.url)
            }
        }
    }

    /** A failed stream is never handed back from the prefetch cache. */
    fun invalidate(song: Song?, failedUrl: String) {
        synchronized(monitor) {
            if (song != null) {
                val cacheKey = key(song)
                if (resolved[cacheKey]?.result?.url == failedUrl) resolved.remove(cacheKey)
            } else {
                resolved.entries.removeAll { it.value.result?.url == failedUrl }
            }
        }
    }

    fun choose(apple: String?, spotify: String?): Result? = when {
        apple != null -> Result(Provider.APPLE_TALL, apple)
        spotify != null -> Result(Provider.SPOTIFY_CANVAS, spotify)
        else -> null
    }
}
