package com.yohandeku32.nusamusic.data

import java.io.File
import java.io.RandomAccessFile
import java.nio.file.Files
import org.junit.Assert.*
import org.junit.Test

class Mp3EmbeddedLyricsWriterTest {
    private val ttml = """<tt><body><p begin="1s" end="6s"><span begin="1s" end="2s">Aku </span><span begin="2s" end="6s">cinta kamu</span></p></body></tt>"""
    private val mp3Audio = byteArrayOf(0xff.toByte(), 0xfb.toByte(), 0x90.toByte(), 0x64) +
        ByteArray(4096) { (it % 251).toByte() }

    private fun sampleMp3(directory: File, name: String = "original.mp3"): File =
        File(directory, name).apply { writeBytes(mp3Audio) }

    private fun lyricsFound(file: File): List<String> = RandomAccessFile(file, "r").use { input ->
        EmbeddedLyricsReader(input.length()) { at, count ->
            input.seek(at)
            val buffer = ByteArray(count)
            val read = input.read(buffer)
            if (read < 0) byteArrayOf() else buffer.copyOf(read)
        }.read()
    }

    @Test fun newId3WritesTimedTtmlAndHumanReadableLyricsWithoutTouchingAudio() {
        val directory = Files.createTempDirectory("embed-lyrics").toFile()
        try {
            val original = sampleMp3(directory)
            val before = original.readBytes()
            val output = File(directory, "updated.mp3")
            Mp3EmbeddedLyricsWriter.write(original, output, ttml, "Aku cinta kamu")
            assertArrayEquals(before, original.readBytes())
            assertTrue(output.readBytes().takeLast(before.size).toByteArray()
                .contentEquals(before))
            val found = lyricsFound(output)
            assertTrue(found.contains(ttml))
            assertTrue(found.contains("Aku cinta kamu"))
            assertEquals(3, lyricQuality(EmbeddedLyricsSelector.select(found, 180000L)))
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test fun existingTitleFrameSurvivesNewLyricsAndSubsequentEmbeddingReplacesOwnedTags() {
        val directory = Files.createTempDirectory("embed-preserved").toFile()
        try {
            val original = sampleMp3(directory)
            val title = "TIT2".toByteArray() + byteArrayOf(0, 0, 0, 5, 0, 0, 0) +
                "Song".toByteArray()
            val size = title.size
            val header = "ID3".toByteArray() + byteArrayOf(3, 0, 0, 0, 0,
                ((size ushr 7) and 127).toByte(), (size and 127).toByte())
            original.writeBytes(header + title + mp3Audio)
            val first = File(directory, "first.mp3")
            val second = File(directory, "second.mp3")
            Mp3EmbeddedLyricsWriter.write(original, first, ttml, "Aku cinta kamu")
            val secondLyrics = ttml.replace("cinta kamu", "sayang kamu")
            Mp3EmbeddedLyricsWriter.write(first, second, secondLyrics, "Aku sayang kamu")
            val updated = second.readBytes()
            assertTrue(updated.toString(Charsets.ISO_8859_1).contains("TIT2"))
            assertArrayEquals(mp3Audio, updated.copyOfRange(updated.size - mp3Audio.size, updated.size))
            assertTrue(lyricsFound(second).contains(secondLyrics))
            assertFalse(lyricsFound(second).contains(ttml))
            assertEquals(1, lyricsFound(second).count { it.startsWith("<tt>") })
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test fun unsafeUnsynchronizedTagIsRejectedBeforeOutputIsTouched() {
        val directory = Files.createTempDirectory("embed-unsafe").toFile()
        try {
            val original = sampleMp3(directory)
            original.writeBytes("ID3".toByteArray() + byteArrayOf(3, 0, 0x80.toByte(),
                0, 0, 0, 0) + mp3Audio)
            val output = File(directory, "updated.mp3")
            assertThrows(IllegalArgumentException::class.java) {
                Mp3EmbeddedLyricsWriter.write(original, output, ttml, "Aku cinta kamu")
            }
            assertFalse(output.exists())
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test fun malformedOrOversizedLyricsAreRejectedWithoutWriting() {
        val directory = Files.createTempDirectory("embed-bounds").toFile()
        try {
            val original = sampleMp3(directory)
            val output = File(directory, "output.mp3")
            assertThrows(IllegalArgumentException::class.java) {
                Mp3EmbeddedLyricsWriter.write(original, output, "", "plain")
            }
            assertThrows(IllegalArgumentException::class.java) {
                Mp3EmbeddedLyricsWriter.write(original, output, "a".repeat(600_000), "plain")
            }
            assertFalse(output.exists())
        } finally {
            directory.deleteRecursively()
        }
    }
}
