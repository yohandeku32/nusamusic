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
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import kotlin.coroutines.coroutineContext

data class SongLyrics(
    val stored: StoredLyrics,
    val synced: SyncedLyrics?,
    val inferredEnds: Set<LyricsUnitKey> = emptySet(),
    val duetRightLines: Set<Int> = emptySet(),
    // User-visible reason only when neither local nor online lyrics exist.
    val onlineNotice: String? = null,
    val pronunciations: Map<Int, LyricsPronunciation> = emptyMap()
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
     * When onlineEnabled is false, even the online disk cache is skipped.
     */
    suspend fun load(
        song: Song,
        retry: Boolean = false,
        onlineEnabled: Boolean = true,
        forceEmbeddedRefresh: Boolean = false,
        onLocalResult: suspend (SongLyrics) -> Unit = {}
    ): SongLyrics = withContext(Dispatchers.IO) {
        val local = loadEmbedded(song, retry || forceEmbeddedRefresh)
        withContext(Dispatchers.Main) { onLocalResult(local) }
        if (!shouldLookupOnlineLyrics(local, onlineEnabled)) return@withContext local

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
        // Content URIs often have no .m4a suffix; Android's MediaStore can
        // retain stale SIZE / DATE_MODIFIED after another editor writes tags.
        // Never trust the persisted embedded cache for content URIs or M4A.
        // This read is bounded, positional and runs off the UI thread.
        val localFileWithReliableCache = uri.scheme == "file" &&
            !uri.path.orEmpty().endsWith(".m4a", ignoreCase = true)
        if (!retry && localFileWithReliableCache)
            embedded.read(key)?.let { entry ->
            return EmbeddedLyricsSelector.select(listOf(entry.content), song.durationMs, entry.credits)
        }
        val loadContext = coroutineContext
        fun readAt(channel: FileChannel, base: Long, length: Long): EmbeddedLyricsMetadata =
            EmbeddedLyricsReader(length) { offset, count ->
                loadContext.ensureActive()
                val buffer = ByteBuffer.allocate(count)
                while (buffer.hasRemaining()) {
                    if (channel.read(buffer, base + offset + buffer.position()) <= 0) break
                }
                buffer.array().copyOf(buffer.position())
            }.readMetadata()

        // Open a NEW full-file descriptor first on EVERY embedded refresh.
        // Content providers can serve a stale/bounded AssetFileDescriptor
        // even when another editor has already replaced the M4A metadata.
        // The actual descriptor channel size is authoritative: MediaStore's
        // database SIZE and an AssetFileDescriptor's declared length can lag.
        // Do not read the active Media3 player descriptor or scan mdat audio.
        val primary = try {
            resolver.openFileDescriptor(uri, "r")?.use { descriptor ->
                FileInputStream(descriptor.fileDescriptor).use { input ->
                    val length = input.channel.size().takeIf { it > 0L }
                        ?: descriptor.statSize.coerceAtLeast(0L)
                    readAt(input.channel, 0L, length)
                }
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) { null }

        // Some SAF providers support only the bounded asset interface.
        // Fall back only if the full-file descriptor has no readable lyrics.
        val metadata = if (primary != null && primary.lyrics.isNotEmpty()) primary else try {
            resolver.openAssetFileDescriptor(uri, "r")?.use { asset ->
                asset.createInputStream().use { input ->
                    val size = input.channel.size()
                    val length = if (asset.declaredLength > 0) asset.declaredLength
                        else (size - asset.startOffset).coerceAtLeast(0L)
                    readAt(input.channel, asset.startOffset, length)
                }
            } ?: primary ?: EmbeddedLyricsMetadata(emptyList())
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            primary ?: EmbeddedLyricsMetadata(emptyList())
        }
        val result = selectLatestEmbeddedLyrics(metadata, song.durationMs)
        coroutineContext.ensureActive()
        if (localFileWithReliableCache) runCatching { embedded.write(key, result.stored) }
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

/**
 * A third-party editor commonly updates the standard iTunes ©lyr atom but
 * leaves NusaMusic's extra karaoke/TTML freeform atom untouched. Its old
 * timestamps may score higher than a newly edited plain lyric, so comparing
 * timing scores alone hides the user's edit indefinitely.
 *
 * The official ©lyr text is authoritative *only when its words differ*
 * from the timed candidate. Matching text keeps richer karaoke/backing vocals.
 */
internal fun selectLatestEmbeddedLyrics(
    metadata: EmbeddedLyricsMetadata,
    durationMs: Long
): SongLyrics {
    val selected = EmbeddedLyricsSelector.select(metadata.lyrics, durationMs, metadata.credits)
    val userFacing = metadata.preferredPlainLyrics?.trim()?.takeIf { it.isNotEmpty() }
        ?: return selected
    val plain = EmbeddedLyricsSelector.select(listOf(userFacing), durationMs, metadata.credits)
    if (selected.stored.content.isBlank() || selected.stored.content == userFacing) return selected

    val allTimedText = selected.synced?.lines?.joinToString(" ") { line ->
        when (line) {
            is KaraokeLine -> line.syllables.joinToString("") { it.content }
            is com.mocharealm.accompanist.lyrics.core.model.synced.SyncedLine -> line.content
            else -> ""
        }
    }?.takeIf { it.isNotBlank() } ?: selected.stored.content

    // The standard ©lyr field is the user-editable version. Preserve
    // punctuation and capitalization changes too; only normalize spacing
    // because the timed engine joins separate lines with spaces.
    fun visibleText(value: String) = value.trim().replace(Regex("\\s+"), " ")
    return if (visibleText(userFacing) != visibleText(allTimedText)) plain else selected
}

/** Accompaniment must be *decoded*, not merely mentioned in untrusted XML text. */
internal fun hasBackingVocals(value: SongLyrics): Boolean =
    value.synced?.lines?.any { it is KaraokeLine && it.isAccompaniment } == true

/**
 * Even authored local karaoke may be missing the accompanying singer.
 * One cached lookup can provide a genuine richer TTML rendition, while
 * turning Online Lyrics off strictly prevents network and online cache reads.
 */
internal fun shouldLookupOnlineLyrics(local: SongLyrics, onlineEnabled: Boolean): Boolean =
    onlineEnabled && (lyricQuality(local) < 3 || !hasBackingVocals(local))

/** Preserve richer timing and avoid replacing valid local lyrics with plain online text. */
internal fun lyricQuality(value: SongLyrics): Int = when {
    value.synced?.lines?.any {
        it is KaraokeLine && it.syllables.any { syllable -> syllable.content.isNotBlank() }
    } == true -> 3
    value.synced?.lines?.isNotEmpty() == true -> 2
    value.stored.content.isNotBlank() -> 1
    else -> 0
}

/**
 * Compare only the main sung lyrics, not TTML background vocals or karaoke
 * timestamps. Online timing may enrich the *same* song text, but must never
 * mask edits made by an external tag editor to embedded lyrics. This remains
 * true after force-close because both sources can be restored from disk.
 */
internal fun mainLyricsText(value: SongLyrics): String {
    val main = value.synced?.lines?.filterNot {
        it is KaraokeLine && it.isAccompaniment
    }?.joinToString(" ") { line ->
        when (line) {
            is KaraokeLine -> line.syllables.joinToString("") { it.content }
            is com.mocharealm.accompanist.lyrics.core.model.synced.SyncedLine -> line.content
            else -> ""
        }
    }?.takeIf { it.isNotBlank() } ?: value.stored.content
    return normalizeLyricsTag(main)
}

/**
 * Compare the visible words exactly, including punctuation and case.
 * Search-key normalization is useful for finding the song, but can erase
 * a user's latest edits ("Hello!" vs "hello") and revive cached online
 * karaoke that no longer matches the text embedded by an external editor.
 */
internal fun visibleMainLyrics(value: SongLyrics): String {
    val text = value.synced?.lines?.filterNot {
        it is KaraokeLine && it.isAccompaniment
    }?.joinToString(" ") { line ->
        when (line) {
            is KaraokeLine -> line.syllables.joinToString("") { it.content }
            is com.mocharealm.accompanist.lyrics.core.model.synced.SyncedLine -> line.content
            else -> ""
        }
    }?.takeIf { it.isNotBlank() } ?: value.stored.content
    return text.trim().replace(Regex("\\s+"), " ")
}

internal fun preferLyrics(local: SongLyrics, remote: SongLyrics): SongLyrics {
    val localQuality = lyricQuality(local)
    val remoteQuality = lyricQuality(remote)
    val canUpgrade = localQuality == 0 ||
        (visibleMainLyrics(local).isNotBlank() &&
            visibleMainLyrics(local) == visibleMainLyrics(remote))
    val improvesTiming = remoteQuality > localQuality
    val addsBacking = remoteQuality == localQuality &&
        hasBackingVocals(remote) && !hasBackingVocals(local)
    // A richer online file without speaker agents must not erase reliable
    // left/right assignments read from an embedded duet TTML. Retain the
    // source with singer identities until an online file preserves them too.
    val losesDuetIdentity = local.duetRightLines.isNotEmpty() &&
        remote.duetRightLines.isEmpty()
    return if (canUpgrade && !losesDuetIdentity &&
        (improvesTiming || addsBacking)
    ) remote.copy(
        // Local composer/lyricist credits always survive a valid online upgrade.
        stored = remote.stored.copy(credits = local.stored.credits)
    ) else local
}

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
                    timing?.inferredEnds.orEmpty(), timing?.duetRightLines.orEmpty(),
                    pronunciations = timing?.pronunciations.orEmpty())
                score = candidateScore
            }
        }
        return best
    }
}
