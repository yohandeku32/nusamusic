package com.yohandeku32.nusamusic.data

import android.content.Context
import com.yohandeku32.nusamusic.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
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
internal enum class ArtworkSourceOrder {
    APPLE_FIRST, SPOTIFY_FIRST, APPLE_ONLY, SPOTIFY_ONLY;
    companion object {
        fun fromPreference(value: String?): ArtworkSourceOrder =
            entries.firstOrNull { it.name == value } ?: APPLE_FIRST
    }
}

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
    private val coverSlots = Semaphore(1)

    private const val APPLE_TTL_MS = 12L * 60 * 60 * 1_000
    private const val SPOTIFY_FALLBACK_TTL_MS = 5L * 60 * 1_000
    private const val MISS_TTL_MS = 45_000L

    private fun key(song: Song, order: ArtworkSourceOrder): String =
        order.name + "|" + listOf(song.uri, song.title, song.artist, song.album)
            .joinToString("|") { it.trim().lowercase(Locale.ROOT) }

    fun ready(song: Song, order: ArtworkSourceOrder = ArtworkSourceOrder.APPLE_FIRST): Result? =
        synchronized(monitor) {
            resolved[key(song, order)]?.takeIf { it.expiresAt > System.currentTimeMillis() }?.result
        }

    /**
     * Cancellations of a stale queue's speculative await never kill a lookup
     * already shared with the Now Playing screen.
     */
    suspend fun resolve(
        context: Context, song: Song,
        order: ArtworkSourceOrder = ArtworkSourceOrder.APPLE_FIRST
    ): Result? {
        val cacheKey = key(song, order)
        val now = System.currentTimeMillis()
        val task: Deferred<Result?>
        synchronized(monitor) {
            val existing = resolved[cacheKey]
            if (existing != null && existing.expiresAt > now) return existing.result
            task = inFlight[cacheKey] ?: scope.async {
                val result = try {
                    lookupSlots.withPermit {
                        val app = context.applicationContext
                        suspend fun apple() =
                            AppleMusicAnimatedArtworkLoader.loadAnimatedArtworkUrl(app, song)
                                ?.let { Result(Provider.APPLE_TALL, it) }
                        suspend fun spotify() =
                            SpotifyCanvasArtworkLoader.findCanvasUrl(app, song)
                                ?.let { Result(Provider.SPOTIFY_CANVAS, it) }
                        when (order) {
                            ArtworkSourceOrder.APPLE_FIRST -> apple() ?: spotify()
                            ArtworkSourceOrder.SPOTIFY_FIRST -> spotify() ?: apple()
                            ArtworkSourceOrder.APPLE_ONLY -> apple()
                            ArtworkSourceOrder.SPOTIFY_ONLY -> spotify()
                        }
                    }
                } catch (cancelled: kotlinx.coroutines.CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    null
                }
                synchronized(monitor) {
                    // Spotify may have won only because Apple's lookup
                    // temporarily failed. Recheck Apple sooner rather than
                    // locking Spotify as the provider for half a day.
                    val ttl = when (result?.provider) {
                        Provider.APPLE_TALL -> APPLE_TTL_MS
                        Provider.SPOTIFY_CANVAS -> SPOTIFY_FALLBACK_TTL_MS
                        null -> MISS_TTL_MS
                    }
                    resolved[cacheKey] = Entry(result, System.currentTimeMillis() + ttl)
                    inFlight.remove(cacheKey)
                }
                result
            }.also { inFlight[cacheKey] = it }
        }
        return task.await()
    }

    /** Preload the chosen provider's video bytes, not just the artwork URL. */
    suspend fun preload(
        context: Context, song: Song, priority: Int,
        order: ArtworkSourceOrder = ArtworkSourceOrder.APPLE_FIRST
    ) = coroutineScope {
        // Small album cover is cheap to retain in RAM and can be shown during
        // the first video frame wait. Never load five 1600px full covers.
        val coverJob = launch(Dispatchers.IO) {
            coverSlots.withPermit {
                ArtworkLoader.load(
                    context, song.uri, maxSize = 512,
                    cacheKey = song.albumId.takeIf { it > 0L }?.toString()
                )
            }
        }
        val candidate = resolve(context, song, order)
        if (candidate != null) {
            videoSlots.withPermit {
                when (candidate.provider) {
                    Provider.APPLE_TALL ->
                        AppleTallVideoCache.warm(context, candidate.url, firstInQueue = priority == 0)
                    Provider.SPOTIFY_CANVAS ->
                        SpotifyCanvasVideoCache.prefetchNext(context, candidate.url, immediateNext = priority == 0)
                }
            }
        }
        coverJob.join()
    }

    /** A failed stream is never handed back from the prefetch cache. */
    fun invalidate(song: Song?, failedUrl: String) {
        synchronized(monitor) {
            if (song != null) {
                resolved.entries.removeAll { (cacheKey, entry) ->
                    cacheKey.contains("|" + song.uri.lowercase(Locale.ROOT) + "|") &&
                        entry.result?.url == failedUrl
                }
            } else {
                resolved.entries.removeAll { it.value.result?.url == failedUrl }
            }
        }
    }

    fun choose(
        apple: String?, spotify: String?,
        order: ArtworkSourceOrder = ArtworkSourceOrder.APPLE_FIRST
    ): Result? = when (order) {
        ArtworkSourceOrder.APPLE_FIRST ->
            apple?.let { Result(Provider.APPLE_TALL, it) }
                ?: spotify?.let { Result(Provider.SPOTIFY_CANVAS, it) }
        ArtworkSourceOrder.SPOTIFY_FIRST ->
            spotify?.let { Result(Provider.SPOTIFY_CANVAS, it) }
                ?: apple?.let { Result(Provider.APPLE_TALL, it) }
        ArtworkSourceOrder.APPLE_ONLY -> apple?.let { Result(Provider.APPLE_TALL, it) }
        ArtworkSourceOrder.SPOTIFY_ONLY -> spotify?.let { Result(Provider.SPOTIFY_CANVAS, it) }
    }
}
