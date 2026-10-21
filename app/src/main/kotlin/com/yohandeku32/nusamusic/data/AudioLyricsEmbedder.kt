package com.yohandeku32.nusamusic.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine
import com.mocharealm.accompanist.lyrics.core.model.synced.SyncedLine
import com.yohandeku32.nusamusic.model.Song
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.security.MessageDigest
import kotlin.coroutines.coroutineContext

/** SAF write to a user-selected original MP3, never a silent permission escalation. */
internal class AudioLyricsEmbedder(private val context: Context) {
    companion object {
        private const val MAX_AUDIO_BYTES = 384L * 1024 * 1024
        private const val BUFFER_SIZE = 64 * 1024
    }

    suspend fun embed(
        song: Song,
        selectedUri: Uri,
        lyric: SongLyrics,
        beforeWrite: suspend () -> Unit = {}
    ) = withContext(Dispatchers.IO) {
        val name = context.contentResolver.query(
            selectedUri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null
        )?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        } ?: throw IOException("Cannot identify the selected file.")
        require(name.endsWith(".mp3", ignoreCase = true)) {
            "Embedding currently supports MP3 files with standard ID3 tags."
        }
        val raw = lyric.stored.content
        require(raw.isNotBlank()) { "There are no lyrics to embed." }
        val plain = readableLyrics(lyric)
        val resolver = context.contentResolver
        val fileSize = resolver.openFileDescriptor(selectedUri, "r")?.use {
            it.statSize
        } ?: -1L
        require(fileSize in 1..MAX_AUDIO_BYTES) {
            "The audio file is too large or its size cannot be verified."
        }
        // We keep both an untouched backup and a separate fully constructed
        // output until completion. Rewriting without these can corrupt a song
        // when Android or its DocumentsProvider interrupts the operation.
        val requiredSpace = fileSize * 2 + 32L * 1024 * 1024
        require(context.cacheDir.usableSpace > requiredSpace) {
            "Not enough free space to back up the song before embedding."
        }
        val backup = File.createTempFile("nusa-lyrics-original-", ".mp3", context.cacheDir)
        val updated = File.createTempFile("nusa-lyrics-updated-", ".mp3", context.cacheDir)
        try {
            val chosenHash = resolver.openInputStream(selectedUri)?.use { source ->
                FileOutputStream(backup).use { out -> copyAndHash(source, out, MAX_AUDIO_BYTES) }
            } ?: throw IOException("The selected file cannot be read.")
            // A SAF document URI is not the same as a MediaStore content URI.
            // Match ALL original bytes, not merely filename/size/cover art,
            // before modifying the selected document.
            val libraryHash = resolver.openInputStream(Uri.parse(song.uri))?.use { source ->
                copyAndHash(source, null, MAX_AUDIO_BYTES)
            } ?: throw IOException("The currently playing song cannot be verified.")
            require(chosenHash.contentEquals(libraryHash)) {
                "Please select the original file for the currently playing song."
            }

            Mp3EmbeddedLyricsWriter.write(backup, updated, raw, plain)
            verifyLyricsInFile(updated, raw)
            // Keep playback paused for the very short replacement phase.
            withContext(Dispatchers.Main) { beforeWrite() }

            var attemptedWrite = false
            try {
                attemptedWrite = true
                overwriteOriginal(selectedUri, updated)
                // Some document providers silently truncate or transform data.
                val actualHash = resolver.openInputStream(selectedUri)?.use {
                    copyAndHash(it, null, MAX_AUDIO_BYTES + 2L * LyricsDecoder.MAX_BYTES)
                } ?: throw IOException("Unable to verify saved audio.")
                val expectedHash = FileInputStream(updated).use {
                    copyAndHash(it, null, MAX_AUDIO_BYTES + 2L * LyricsDecoder.MAX_BYTES)
                }
                check(actualHash.contentEquals(expectedHash)) {
                    "The saved audio does not match the verified output."
                }
            } catch (error: Exception) {
                if (attemptedWrite) {
                    val restored = runCatching { overwriteOriginal(selectedUri, backup) }
                    if (restored.isFailure) {
                        throw IOException(
                            "Writing failed and the original could not be restored. " +
                                "Do not delete the file; check storage before retrying.",
                            error
                        )
                    }
                }
                throw IOException("Embedding failed; the original file was restored.", error)
            }
        } finally {
            backup.delete()
            updated.delete()
        }
    }

    private fun overwriteOriginal(uri: Uri, file: File) {
        val fd = context.contentResolver.openFileDescriptor(uri, "rwt")
            ?: throw IOException("The selected file is read-only.")
        fd.use { descriptor ->
            FileInputStream(file).use { input ->
                FileOutputStream(descriptor.fileDescriptor).use { output ->
                    input.copyTo(output, BUFFER_SIZE)
                    output.flush()
                    output.fd.sync()
                }
            }
        }
    }

    private suspend fun copyAndHash(
        input: java.io.InputStream,
        output: java.io.OutputStream?,
        limit: Long
    ): ByteArray {
        val sha = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(BUFFER_SIZE)
        var size = 0L
        while (true) {
            coroutineContext.ensureActive()
            val count = input.read(buffer)
            if (count < 0) break
            size += count
            if (size > limit) throw IOException("Audio exceeds the safe editing limit.")
            sha.update(buffer, 0, count)
            output?.write(buffer, 0, count)
        }
        output?.flush()
        return sha.digest()
    }

    private fun verifyLyricsInFile(file: File, raw: String) {
        java.io.RandomAccessFile(file, "r").use { input ->
            val found = EmbeddedLyricsReader(input.length()) { offset, size ->
                val buffer = ByteArray(size)
                input.seek(offset)
                val count = input.read(buffer)
                if (count <= 0) byteArrayOf() else buffer.copyOf(count)
            }.read()
            check(raw in found) {
                "The staged MP3 did not retain synchronized lyrics."
            }
        }
    }
}

internal fun readableLyrics(value: SongLyrics): String =
    value.synced?.lines?.joinToString("\n") { line ->
        when (line) {
            is KaraokeLine -> line.syllables.joinToString("") { it.content }
            is SyncedLine -> line.content
            else -> ""
        }
    }?.takeIf { it.isNotBlank() } ?: value.stored.content
