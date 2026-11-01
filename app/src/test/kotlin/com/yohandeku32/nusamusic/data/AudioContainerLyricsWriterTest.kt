package com.yohandeku32.nusamusic.data

import java.io.ByteArrayOutputStream
import java.io.File
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioContainerLyricsWriterTest {
    private val authored = "[00:01]<00:01>Aku <00:02>cinta <00:03>kamu<00:05>"
    private val plain = "Aku cinta kamu"

    @Test fun supportedFormatsFollowCurrentAudioFile() {
        assertEquals(EmbeddedAudioFormat.MP3, EmbeddedAudioFormat.fromFilename("One.MP3"))
        assertEquals(EmbeddedAudioFormat.FLAC, EmbeddedAudioFormat.fromFilename("One.FLAC"))
        assertEquals(EmbeddedAudioFormat.M4A, EmbeddedAudioFormat.fromFilename("One.m4a"))
        assertEquals(null, EmbeddedAudioFormat.fromFilename("One.wav"))
    }

    @Test fun flacKeepsAudioAndPictureWhileSavingAndUpgradingLyrics() {
        val original = temp(".flac")
        val output = temp(".flac")
        val picture = byteArrayOf(10, 20, 30, 40, 50)
        val audio = ByteArray(253) { (it xor 0x55).toByte() }
        val info = ByteArray(34)
        val comments = vorbis("reference", "ARTIST=Ada", "ALBUM=Example")
        original.writeBytes(
            "fLaC".toByteArray() +
                flacBlock(0, info, false) +
                flacBlock(6, picture, false) +
                flacBlock(4, comments, true) + audio
        )
        FlacEmbeddedLyricsWriter.write(original, output, authored, plain)
        val updated = output.readBytes()
        assertArrayEquals(audio, updated.copyOfRange(updated.size - audio.size, updated.size))
        assertTrue(updated.indexOfSlice(picture) >= 0)
        assertTrue(updated.indexOfSlice("ARTIST=Ada".toByteArray()) >= 0)
        assertTrue(lyrics(updated).contains(authored))
        FlacEmbeddedLyricsWriter.write(output, original, "[00:10.00]Updated", "Updated")
        val twice = original.readBytes()
        assertTrue(lyrics(twice).contains("[00:10.00]Updated"))
        assertFalse(lyrics(twice).contains(authored))
        assertArrayEquals(audio, twice.copyOfRange(twice.size - audio.size, twice.size))
    }

    @Test fun flacWithoutVorbisCommentCreatesNewMetadataBlock() {
        val original = temp(".flac")
        val output = temp(".flac")
        val audio = byteArrayOf(-1, -8, 5, 4, 3)
        original.writeBytes("fLaC".toByteArray() +
            flacBlock(0, ByteArray(34), true) + audio)
        FlacEmbeddedLyricsWriter.write(original, output, authored, plain)
        assertTrue(lyrics(output.readBytes()).contains(authored))
        assertArrayEquals(audio, output.readBytes().takeLast(audio.size).toByteArray())
    }

    @Test fun m4aBeforeAudioKeepsExactMdatLocationAndReadsTimedLyrics() {
        val original = temp(".m4a")
        val output = temp(".m4a")
        val media = byteArrayOf(0, 3, -1, -5, 1, 11, 64, -2)
        val ftyp = atom("ftyp", "M4A ".toByteArray() + ByteArray(4) +
            "isom".toByteArray())
        val moov = atom("moov", atom("trak", atom("stco", ByteArray(16))))
        val mdat = atom("mdat", media)
        original.writeBytes(ftyp + moov + mdat)
        M4aEmbeddedLyricsWriter.write(original, output, authored, plain)
        val updated = output.readBytes()
        val originalMdatOffset = ftyp.size + moov.size
        assertArrayEquals(mdat, updated.copyOfRange(
            originalMdatOffset, originalMdatOffset + mdat.size
        ))
        assertEquals("free", String(updated, ftyp.size + 4, 4, Charsets.US_ASCII))
        assertTrue(lyrics(updated).contains(authored))
        // Second Save replaces NusaMusic's own timed freeform field.
        M4aEmbeddedLyricsWriter.write(output, original, "[00:10.00]Updated", "Updated")
        assertTrue(lyrics(original.readBytes()).contains("[00:10.00]Updated"))
        assertFalse(lyrics(original.readBytes()).contains(authored))
    }

    @Test fun m4aAfterAudioPreservesExternalMetadataAndArtwork() {
        val original = temp(".m4a")
        val output = temp(".m4a")
        val ftyp = atom("ftyp", "M4A ".toByteArray() + ByteArray(4) +
            "isom".toByteArray())
        val artwork = atom("covr", atom("data", ByteArray(8) + byteArrayOf(1, 2, 3, 4)))
        val externalLyrics = atom("©lyr", atom("data",
            byteArrayOf(0, 0, 0, 1, 0, 0, 0, 0) + "Old lyrics".toByteArray()))
        val ilst = atom("ilst", artwork + externalLyrics)
        val meta = atom("meta", ByteArray(4) + ilst)
        val moov = atom("moov", atom("udta", meta))
        val mdat = atom("mdat", byteArrayOf(5, 6, 7, 8))
        original.writeBytes(ftyp + mdat + moov)
        M4aEmbeddedLyricsWriter.write(original, output, authored, plain)
        val result = output.readBytes()
        assertArrayEquals(ftyp + mdat, result.copyOfRange(0, (ftyp + mdat).size))
        assertTrue(result.indexOfSlice(artwork) >= 0)
        assertFalse(result.indexOfSlice(externalLyrics) >= 0)
        assertTrue(lyrics(result).contains(authored))
        val metadata = EmbeddedLyricsReader(result.size.toLong()) { offset, count ->
            result.copyOfRange(offset.toInt(), offset.toInt() + count)
        }.readMetadata()
        assertEquals(plain, metadata.preferredPlainLyrics)
    }

    @Test fun flacReplacesStandardLyricsAndMigratesLegacyAppTags() {
        val original = temp(".flac")
        val output = temp(".flac")
        val audio = byteArrayOf(-1, -8, 1, 3, 5, 7)
        original.writeBytes("fLaC".toByteArray() + flacBlock(0, ByteArray(34), false) +
            flacBlock(4, vorbis("NusaMusic", "ARTIST=Keep artist", "LYRICS=Old words",
                "UNSYNCEDLYRICS=Old alternate", "NUSAMUSIC_LYRICS=[00:01]Old timed"), true) + audio)
        FlacEmbeddedLyricsWriter.write(original, output, authored, plain)
        val updated = output.readBytes()
        val text = updated.toString(Charsets.UTF_8)
        assertTrue(lyrics(updated).contains(plain))
        assertTrue(lyrics(updated).contains(authored))
        assertTrue(text.contains("ARTIST=Keep artist"))
        assertTrue(text.contains("ARUNA_LYRICS="))
        assertFalse(text.contains("NUSAMUSIC_LYRICS="))
        assertFalse(text.contains("NusaMusic"))
        assertFalse(text.contains("Old words"))
        assertFalse(text.contains("Old alternate"))
        assertArrayEquals(audio, updated.takeLast(audio.size).toByteArray())
    }

    @Test fun m4aMigratesTheOwnedFreeformAndUpdatesTheStandardLyric() {
        val original = temp(".m4a")
        val output = temp(".m4a")
        val media = atom("mdat", byteArrayOf(1, 2, 3, 4))
        val legacy = atom("----", atom("mean", ByteArray(4) + "com.nusamusic".toByteArray()) +
            atom("name", ByteArray(4) + "LYRICS".toByteArray()) +
            atom("data", byteArrayOf(0, 0, 0, 1, 0, 0, 0, 0) + "[00:01]Old timed".toByteArray()))
        val plainOld = atom("©lyr", atom("data", byteArrayOf(0, 0, 0, 1, 0, 0, 0, 0) + "Old words".toByteArray()))
        original.writeBytes(atom("ftyp", "M4A ".toByteArray() + ByteArray(4) + "isom".toByteArray()) +
            media + atom("moov", atom("udta", atom("meta", ByteArray(4) + atom("ilst", legacy + plainOld)))))
        M4aEmbeddedLyricsWriter.write(original, output, authored, plain)
        val updated = output.readBytes()
        assertTrue(lyrics(updated).contains(authored))
        assertTrue(lyrics(updated).contains(plain))
        val text = updated.toString(Charsets.UTF_8)
        assertTrue(text.contains("com.aruna"))
        assertFalse(text.contains("com.nusamusic"))
        assertFalse(text.contains("Old words"))
        assertFalse(text.contains("Old timed"))
        assertTrue(updated.indexOfSlice(media) >= 0)
    }

    private fun temp(ext: String): File = File.createTempFile("aruna-lyrics-test-", ext)
        .also { it.deleteOnExit() }

    private fun lyrics(bytes: ByteArray): List<String> =
        EmbeddedLyricsReader(bytes.size.toLong()) { offset, count ->
            bytes.copyOfRange(offset.toInt(), offset.toInt() + count)
        }.read()

    private fun flacBlock(type: Int, body: ByteArray, last: Boolean): ByteArray =
        byteArrayOf(
            ((if (last) 128 else 0) or type).toByte(),
            (body.size ushr 16).toByte(),
            (body.size ushr 8).toByte(),
            body.size.toByte()
        ) + body

    private fun vorbis(vendor: String, vararg fields: String): ByteArray {
        val out = ByteArrayOutputStream()
        fun writeInt(n: Int) {
            out.write(n); out.write(n ushr 8); out.write(n ushr 16); out.write(n ushr 24)
        }
        val name = vendor.toByteArray(Charsets.UTF_8)
        writeInt(name.size); out.write(name)
        writeInt(fields.size)
        fields.forEach {
            val text = it.toByteArray(Charsets.UTF_8)
            writeInt(text.size); out.write(text)
        }
        return out.toByteArray()
    }

    private fun atom(name: String, body: ByteArray): ByteArray {
        val size = body.size + 8
        return byteArrayOf(
            (size ushr 24).toByte(), (size ushr 16).toByte(),
            (size ushr 8).toByte(), size.toByte()
        ) + name.toByteArray(Charsets.ISO_8859_1) + body
    }

    private fun ByteArray.indexOfSlice(needle: ByteArray): Int =
        indices.firstOrNull { start ->
            start + needle.size <= size &&
                needle.indices.all { this[start + it] == needle[it] }
        } ?: -1
}
