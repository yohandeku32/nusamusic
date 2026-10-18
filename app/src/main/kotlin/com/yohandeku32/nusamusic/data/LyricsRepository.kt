package com.yohandeku32.nusamusic.data

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.provider.OpenableColumns
import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine
import com.yohandeku32.nusamusic.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer
import kotlin.coroutines.coroutineContext

data class SongLyrics(val stored: StoredLyrics, val synced: SyncedLyrics?)

/** The only source is the current audio URI. Old imported/remote caches are never read. */
class LyricsRepository(context: Context) {
    private val resolver = context.applicationContext.contentResolver
    private val cache = LyricsDiskStore(File(context.applicationContext.cacheDir, "lyrics/embedded-v2"), maxEntries = 120)

    companion object {
        fun cacheKey(song: Song) = listOf(song.uri, song.durationMs.toString(), song.dateAddedMs.toString()).joinToString("\u0000")
    }

    suspend fun load(song: Song, retry: Boolean = false): SongLyrics = withContext(Dispatchers.IO) {
        val uri = Uri.parse(song.uri)
        if (uri.scheme !in setOf("content", "file")) return@withContext EmbeddedLyricsSelector.select(emptyList(), song.durationMs)
        val key = cacheKey(song) + "\u0000" + fingerprint(uri)
        if (!retry) cache.read(key)?.let { entry ->
            return@withContext EmbeddedLyricsSelector.select(listOf(entry.content), song.durationMs, entry.credits)
        }
        val loadContext = coroutineContext
        val metadata = resolver.openAssetFileDescriptor(uri, "r")?.use { asset ->
            asset.createInputStream().use { input ->
                val channel = input.channel
                val size = if (asset.declaredLength >= 0) asset.declaredLength else channel.size() - asset.startOffset
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
        val result = EmbeddedLyricsSelector.select(metadata.lyrics, song.durationMs, metadata.credits)
        coroutineContext.ensureActive()
        runCatching { cache.write(key, result.stored) }
        result
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

internal object EmbeddedLyricsSelector {
    private val timed = Regex("(?m)^\\s*\\[\\d+:|^\\s*<(?:\\w+:)?tt(?:\\s|>)")

    fun select(candidates: List<String>, durationMs: Long, credits: SongCredits = SongCredits()): SongLyrics {
        var best = SongLyrics(StoredLyrics("", "embedded", credits = credits), null)
        var score = 0L
        for (raw in candidates) {
            if (raw.isBlank() || raw.toByteArray(Charsets.UTF_8).size > LyricsDecoder.MAX_BYTES) continue
            val decoded = runCatching { LyricsDecoder.decode(raw, durationMs) }.getOrNull()
            // Corrupt timed metadata must not become unsynchronised timestamp gibberish.
            if (decoded == null && timed.containsMatchIn(raw)) continue
            val units = decoded?.lines?.filterIsInstance<KaraokeLine>()?.sumOf { it.syllables.size } ?: 0
            val candidateScore = when { units > 0 -> 3_000_000L + units; decoded != null -> 2_000_000L + decoded.lines.size; else -> 1L }
            if (candidateScore > score) {
                best = SongLyrics(StoredLyrics(raw, "embedded", plain = decoded == null, credits = credits), decoded)
                score = candidateScore
            }
        }
        return best
    }
}
