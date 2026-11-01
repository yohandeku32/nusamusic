package com.yohandeku32.nusamusic.data

import android.content.ContentProvider
import android.content.ContentValues
import android.content.pm.ProviderInfo
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import java.io.File
import kotlinx.coroutines.*
import org.junit.*
import org.junit.Assert.*
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowContentResolver

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AudioLyricsEmbedderTest {
    @get:Rule val temporary = TemporaryFolder()
    private val context get() = RuntimeEnvironment.getApplication()
    private lateinit var provider: SongProvider
    private val raw = "[00:01]<00:01>Musik <00:02>tetap <00:03>bersama<00:05>"
    private val plain = "Musik tetap bersama"
    private val audio = byteArrayOf(0xFF.toByte(), 0xFB.toByte(), 0x90.toByte(), 0x64) +
        ByteArray(512) { (it % 127).toByte() }

    @Before fun register() {
        provider = SongProvider()
        provider.attachInfo(context, ProviderInfo().apply { authority = "aruna.lyrics.test" })
        ShadowContentResolver.registerProviderInternal("aruna.lyrics.test", provider)
    }

    private fun uri(file: File, name: String): Uri = Uri.parse("content://aruna.lyrics.test/$name").also {
        provider.files[it.toString()] = file
    }

    private fun atom(type: String, payload: ByteArray): ByteArray {
        val n = payload.size + 8
        return byteArrayOf((n ushr 24).toByte(), (n ushr 16).toByte(), (n ushr 8).toByte(), n.toByte()) +
            type.toByteArray(Charsets.ISO_8859_1) + payload
    }

    private fun song(extension: String): File = temporary.newFile("song.$extension").apply {
        writeBytes(when (extension) {
            "flac" -> "fLaC".toByteArray() + byteArrayOf(0x80.toByte(), 0, 0, 34) + ByteArray(34) + audio
            "m4a" -> atom("ftyp", "M4A ".toByteArray() + ByteArray(4) + "isom".toByteArray()) +
                atom("mdat", audio) + atom("moov", byteArrayOf())
            else -> audio
        })
    }

    private fun readMetadata(file: File): EmbeddedLyricsMetadata {
        val bytes = file.readBytes()
        return EmbeddedLyricsReader(bytes.size.toLong()) { offset, count ->
            bytes.copyOfRange(offset.toInt(), offset.toInt() + count)
        }.readMetadata()
    }

    private fun <T> complete(block: suspend () -> T): T {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val task = scope.async { block() }
        val deadline = System.nanoTime() + 15_000_000_000L
        while (!task.isCompleted && System.nanoTime() < deadline) {
            shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(5)
        }
        if (!task.isCompleted) { scope.cancel(); fail("Metadata write did not complete") }
        return try { runBlocking { task.await() } } finally { scope.cancel() }
    }

    @Test fun writesOriginalMp3FlacAndM4aReadableWithoutTheAppCache() {
        for (extension in listOf("mp3", "flac", "m4a")) {
            val original = song(extension)
            val library = uri(original, "library-$extension")
            val document = uri(original, "document-$extension")
            complete { AudioLyricsEmbedder(context).embedPrepared(library.toString(), document, raw, plain) }
            // Read only original-file bytes, with no repository or lyric cache.
            val metadata = readMetadata(original)
            assertTrue("$extension authored timing", metadata.lyrics.contains(raw))
            assertTrue("$extension readable lyrics", metadata.lyrics.contains(plain))
            if (extension == "m4a") assertEquals(plain, metadata.preferredPlainLyrics)
            val bytes = original.readBytes()
            assertTrue("$extension original audio survives", bytes.asList().windowed(audio.size)
                .any { it == audio.asList() })
        }
    }

    @Test fun rejectsADifferentDocumentWithoutChangingEitherFile() {
        val original = song("mp3")
        val other = temporary.newFile("other.mp3").apply { writeBytes(audio + byteArrayOf(7)) }
        val before = original.readBytes()
        val otherBefore = other.readBytes()
        try {
            complete { AudioLyricsEmbedder(context).embedPrepared(
                uri(original, "library").toString(), uri(other, "document"), raw, plain) }
            fail("A different original must be rejected")
        } catch (_: IllegalArgumentException) { }
        assertArrayEquals(before, original.readBytes())
        assertArrayEquals(otherBefore, other.readBytes())
    }

    @Test fun activeSourceGuardRejectsBeforeOverwritingTheOriginal() {
        val original = song("mp3")
        val before = original.readBytes()
        val document = uri(original, "guarded")
        try {
            complete { AudioLyricsEmbedder(context).embedPrepared(document.toString(), document, raw, plain,
                beforeWrite = { error("This file is still in use") }) }
            fail("Active source must not be overwritten")
        } catch (_: IllegalStateException) { }
        assertArrayEquals(before, original.readBytes())
        assertEquals(0, provider.writes)
    }

    @Test fun verificationFailureRestoresTheOriginalAudio() {
        val original = song("mp3")
        val before = original.readBytes()
        val document = uri(original, "rollback")
        provider.corruptAfterFirstWrite = true
        try {
            complete { AudioLyricsEmbedder(context).embedPrepared(document.toString(), document, raw, plain) }
            fail("Corrupt provider output must not count as saved")
        } catch (_: java.io.IOException) { }
        assertArrayEquals(before, original.readBytes())
        assertEquals(2, provider.writes)
    }

    class SongProvider : ContentProvider() {
        val files = mutableMapOf<String, File>()
        var writes = 0
        var corruptAfterFirstWrite = false
        private var corruptNextRead = false
        override fun onCreate() = true
        override fun getType(uri: Uri) = "audio/mpeg"
        override fun query(uri: Uri, projection: Array<out String>?, selection: String?,
            selectionArgs: Array<out String>?, sortOrder: String?): Cursor {
            val file = files.getValue(uri.toString())
            val columns = projection ?: arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE)
            return MatrixCursor(columns).apply { addRow(columns.map { column ->
                when (column) {
                    OpenableColumns.DISPLAY_NAME -> file.name
                    OpenableColumns.SIZE -> file.length()
                    else -> null
                }
            }) }
        }
        override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
            val file = files.getValue(uri.toString())
            if ('w' in mode) {
                writes++
                if (corruptAfterFirstWrite && writes == 1) corruptNextRead = true
            } else if (corruptNextRead) {
                corruptNextRead = false
                file.writeBytes(byteArrayOf(1, 2, 3))
            }
            return ParcelFileDescriptor.open(file, ParcelFileDescriptor.parseMode(mode))
        }
        override fun insert(uri: Uri, values: ContentValues?): Uri? = null
        override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?) = 0
        override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?) = 0
    }
}
