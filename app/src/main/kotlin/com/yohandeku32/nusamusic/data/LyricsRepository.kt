package com.yohandeku32.nusamusic.data

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.provider.OpenableColumns
import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine
import com.yohandeku32.nusamusic.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.nio.ByteBuffer
import kotlin.coroutines.coroutineContext

data class SongLyrics(
    val stored: StoredLyrics,
    val synced: SyncedLyrics?,
    val inferredEnds: Set<LyricsUnitKey> = emptySet(),
    val duetRightLines: Set<Int> = emptySet(),
    // User-visible reason only when neither local nor online lyrics exist.
    val onlineNotice: String? = null
)

/**
 * Offline-first online upgrade. Embedded karaoke always wins; lrc.red can
 * upgrade missing, plain or line-synced local lyrics with authored timing.
 * No WebView and no audio leaves the device.
 */
class LyricsRepository(context: Context) {
    private val resolver = context.applicationContext.contentResolver
    private val embedded = LyricsDiskStore(File(context.applicationContext.cacheDir, "lyrics/embedded-v2"), maxEntries = 120)
    private val online = LyricsDiskStore(File(context.applicationContext.cacheDir, "lyrics/online-lrcred-v2"), maxEntries = 120)
    private val service = OnlineLyricsService()
    private val savedRequests = LyricsPendingSaveQueue(context.applicationContext)

    companion object {
        fun cacheKey(song: Song) = listOf(song.uri, song.durationMs.toString(), song.dateAddedMs.toString()).joinToString("\u0000")
        internal fun onlineKey(song: Song) = listOf(
            normalizeLyricsTag(song.title),
            normalizeLyricsTag(primaryLyricsArtist(song.artist)),
            normalizeLyricsTag(song.album),
            (song.durationMs / 1000L).toString()
        ).joinToString("\u0000")
    }

    /**
     * The callback publishes local lyrics immediately before a potentially
     * slow online lookup. Called on Main and never after cancellation.
     */
    suspend fun load(
        song: Song,
        retry: Boolean = false,
        onLocalResult: suspend (SongLyrics) -> Unit = {}
    ): SongLyrics = withContext(Dispatchers.IO) {
        val local = loadEmbedded(song, retry)
        withContext(Dispatchers.Main) { onLocalResult(local) }
        if (lyricQuality(local) >= 3) return@withContext local

        val key = onlineKey(song)
        if (!retry) online.read(key)?.let { cached ->
            val decoded = decodeOnline(cached, song.durationMs)
            if (decoded != null) return@withContext preferLyrics(local, decoded)
            // Negative result is retained only for the short miss TTL.
            if (cached.content.isEmpty()) return@withContext local.copy(
                onlineNotice = "No matching online lyrics found"
            )
        }

        // Public providers can be slow or offline. Keep a hard upper bound
        // on the whole lookup so an empty Lyrics screen never waits forever.
        val response = try {
            withTimeoutOrNull(16_000L) { service.lookup(song) }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) { null }

        coroutineContext.ensureActive()
        val found = response?.result
        if (found != null) {
            val entry = StoredLyrics(found.content, found.source)
            val decoded = decodeOnline(entry, song.durationMs)
            if (decoded != null) {
                runCatching { online.write(key, entry) }
                return@withContext preferLyrics(local, decoded)
            }
        }
        if (response?.conclusivelyMissing == true) runCatching {
            online.write(key, StoredLyrics("", "online"))
        }
        local.copy(onlineNotice = if (response?.conclusivelyMissing == true)
            "No matching online lyrics found"
        else "Cannot reach the online lyrics service")
    }

    private fun decodeOnline(stored: StoredLyrics, durationMs: Long): SongLyrics? {
        if (stored.content.isBlank()) return null
        val selected = EmbeddedLyricsSelector.select(
            listOf(stored.content), durationMs
        )
        if (lyricQuality(selected) == 0) return null
        return selected.copy(stored = selected.stored.copy(source = stored.source))
    }

    private suspend fun loadEmbedded(song: Song, retry: Boolean): SongLyrics {
        // Save is immediately durable in the app even while Media3 retains its
        // source descriptor. Preview queued authored timing without touching
        // the active track or waiting for an explicit Next.
        savedRequests.savedForPlayback(song)?.let { raw ->
            return EmbeddedLyricsSelector.select(listOf(raw), song.durationMs).let {
                it.copy(stored = it.stored.copy(source = "saved-in-app"))
            }
        }
        val uri = Uri.parse(song.uri)
        if (uri.scheme !in setOf("content", "file"))
            return EmbeddedLyricsSelector.select(emptyList(), song.durationMs)
        val key = cacheKey(song) + "\u0000" + fingerprint(uri)
        if (!retry) embedded.read(key)?.let { entry ->
            return EmbeddedLyricsSelector.select(listOf(entry.content), song.durationMs, entry.credits)
        }
        val loadContext = coroutineContext
        val metadata = try {
            resolver.openAssetFileDescriptor(uri, "r")?.use { asset ->
                asset.createInputStream().use { input ->
                    val channel = input.channel
                    val size = if (asset.declaredLength >= 0) asset.declaredLength
                        else channel.size() - asset.startOffset
                    EmbeddedLyricsReader(size) { offset, count ->
                        loadContext.ensureActive()
                        val buffer = ByteBuffer.allocate(count)
                        while (buffer.hasRemaining()) {
                            if (channel.read(buffer, asset.startOffset + offset + buffer.position()) <= 0) break
                        }
                        buffer.array().copyOf(buffer.position())
                    }.readMetadata()
                }
            } ?: EmbeddedLyricsMetadata(emptyList())
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            // A provider may deny metadata reads while the music file still
            // plays; use the online lookup rather than failing the Lyrics UI.
            EmbeddedLyricsMetadata(emptyList())
        }
        val result = EmbeddedLyricsSelector.select(metadata.lyrics, song.durationMs, metadata.credits)
        coroutineContext.ensureActive()
        runCatching { embedded.write(key, result.stored) }
        return result
    }

    private fun fingerprint(uri: Uri): String {
        if (uri.scheme == "file") return File(uri.path.orEmpty()).let { it.length().toString() + ":" + it.lastModified() }
        return runCatching {
            resolver.query(uri, arrayOf(OpenableColumns.SIZE, MediaStore.MediaColumns.DATE_MODIFIED), null, null, null)?.use {
                if (it.moveToFirst()) it.getLong(0).toString() + ":" + it.getLong(1) else "unknown"
            }
        }.getOrNull() ?: runCatching {
            resolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use {
                if (it.moveToFirst()) it.getLong(0).toString() else "unknown"
            }
        }.getOrNull() ?: "unknown"
    }
}

/** Preserve richer timing and avoid replacing valid local lyrics with plain online text. */
internal fun lyricQuality(value: SongLyrics): Int = when {
    value.synced?.lines?.any {
        it is KaraokeLine && it.syllables.any { syllable -> syllable.content.isNotBlank() }
    } == true -> 3
    value.synced?.lines?.isNotEmpty() == true -> 2
    value.stored.content.isNotBlank() -> 1
    else -> 0
}

internal fun preferLyrics(local: SongLyrics, remote: SongLyrics): SongLyrics =
    if (lyricQuality(remote) > lyricQuality(local)) remote.copy(
        // Remote text must not erase composer / lyricist credits authored
        // in the locally owned audio metadata.
        stored = remote.stored.copy(credits = local.stored.credits)
    ) else local

internal object EmbeddedLyricsSelector {
    private val timed = Regex("(?m)^\\s*\\[\\d+:|^\\s*<(?:\\w+:)?tt(?:\\s|>)")

    fun select(candidates: List<String>, durationMs: Long, credits: SongCredits = SongCredits()): SongLyrics {
        var best = SongLyrics(StoredLyrics("", "embedded", credits = credits), null)
        var score = 0L
        for (raw in candidates) {
            if (raw.isBlank() || raw.toByteArray(Charsets.UTF_8).size > LyricsDecoder.MAX_BYTES) continue
            val timing = runCatching { LyricsDecoder.decodeWithTiming(raw, durationMs) }.getOrNull()
            val decoded = timing?.lyrics
            // Corrupt timed metadata must not become unsynchronised timestamp gibberish.
            if (decoded == null && timed.containsMatchIn(raw)) continue
            val units = decoded?.lines?.filterIsInstance<KaraokeLine>()?.sumOf { it.syllables.size } ?: 0
            val candidateScore = when { units > 0 -> 3_000_000L + units; decoded != null -> 2_000_000L + decoded.lines.size; else -> 1L }
            if (candidateScore > score) {
                best = SongLyrics(StoredLyrics(raw, "embedded", plain = decoded == null, credits = credits), decoded,
                    timing?.inferredEnds.orEmpty(), timing?.duetRightLines.orEmpty())
                score = candidateScore
            }
        }
        return best
    }
}
