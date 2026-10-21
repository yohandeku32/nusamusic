package com.yohandeku32.nusamusic.data

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.charset.Charset

/**
 * Preserve every existing ID3 frame byte-for-byte and the complete MPEG audio.
 * Add a standards-compliant USLT for general players and a TXXX:LYRICS frame
 * containing original TTML/Enhanced LRC timing for NusaMusic.
 *
 * Deliberately reject unsupported tag flags/versions rather than corrupting
 * an MP3 with unusual extended headers, global unsynchronization or a footer.
 * Output is a separate temporary file; the user's original is never modified
 * by this pure writer.
 */
internal object Mp3EmbeddedLyricsWriter {
    private const val MAX_TAG_BYTES = 16 * 1024 * 1024
    private const val MAX_LYRICS_BYTES = 512 * 1024
    private const val MAX_FRAME_BYTES = 16 * 1024 * 1024
    private val id = Regex("[A-Z0-9]{4}")

    fun write(original: File, output: File, fullLyrics: String, plainLyrics: String) {
        require(fullLyrics.isNotBlank() &&
            fullLyrics.toByteArray(Charsets.UTF_8).size <= MAX_LYRICS_BYTES) {
            "Lyrics are empty or too large."
        }
        require(original.absolutePath != output.absolutePath)
        RandomAccessFile(original, "r").use { source ->
            require(source.length() > 8L) { "The selected audio file is empty." }
            val first = ByteArray(10)
            source.readFully(first)
            val hasId3 = first.copyOfRange(0, 3).contentEquals("ID3".toByteArray())
            val version: Int
            val audioOffset: Long
            val oldFrames = ByteArrayOutputStream()
            if (hasId3) {
                version = first[3].toInt() and 0xFF
                require(version in 3..4) {
                    "Only standard MP3 ID3v2.3 and ID3v2.4 tags can be edited safely."
                }
                val flags = first[5].toInt() and 0xFF
                require(flags == 0) {
                    "This MP3 uses ID3 flags that need a specialized tag editor."
                }
                val bytes = syncsafeRead(first, 6)
                require(bytes in 0..MAX_TAG_BYTES && bytes.toLong() <= source.length() - 10L) {
                    "Invalid or oversized MP3 metadata."
                }
                val tag = ByteArray(bytes)
                source.readFully(tag)
                audioOffset = bytes.toLong() + 10L
                var p = 0
                var frames = 0
                while (p + 10 <= tag.size) {
                    if (tag.copyOfRange(p, p + 4).all { it == 0.toByte() }) {
                        require(tag.copyOfRange(p, tag.size).all { it == 0.toByte() })
                        break
                    }
                    require(++frames <= 4096) { "Too many ID3 frames." }
                    val name = tag.copyOfRange(p, p + 4).toString(Charsets.ISO_8859_1)
                    require(id.matches(name)) { "Unsupported MP3 metadata frame layout." }
                    val size = if (version == 4) syncsafeRead(tag, p + 4) else
                        ((tag[p + 4].toInt() and 255) shl 24) or
                        ((tag[p + 5].toInt() and 255) shl 16) or
                        ((tag[p + 6].toInt() and 255) shl 8) or
                        (tag[p + 7].toInt() and 255)
                    require(size in 0..MAX_FRAME_BYTES && size <= tag.size - p - 10) {
                        "Unsupported ID3 frame size."
                    }
                    // Never interpret/replace other providers' USLT frames.
                    // Update only our own TXXX:LYRICS and USLT:NusaMusic.
                    val body = tag.copyOfRange(p + 10, p + 10 + size)
                    if (!isOwnedLyrics(name, body)) oldFrames.write(tag, p, 10 + size)
                    p += size + 10
                }
            } else {
                version = 3
                audioOffset = 0L
                require((first[0].toInt() and 255) == 255 &&
                    (first[1].toInt() and 0xE0) == 0xE0) {
                    "This is not an MP3 file."
                }
            }

            val tagPayload = ByteArrayOutputStream()
            tagPayload.write(oldFrames.toByteArray())
            val timed = fullLyrics.toByteArray(Charsets.UTF_8)
            val plain = plainLyrics.takeIf { it.isNotBlank() } ?: fullLyrics
            addFrame(tagPayload, "TXXX", if (version == 4) {
                byteArrayOf(3) + "LYRICS".toByteArray(Charsets.UTF_8) + byteArrayOf(0) + timed
            } else {
                byteArrayOf(1) + "LYRICS".toByteArray(Charsets.UTF_16) +
                    byteArrayOf(0, 0) + fullLyrics.toByteArray(Charsets.UTF_16)
            }, version)
            addFrame(tagPayload, "USLT", if (version == 4) {
                byteArrayOf(3) + "eng".toByteArray(Charsets.US_ASCII) +
                    "NusaMusic".toByteArray(Charsets.UTF_8) + byteArrayOf(0) +
                    plain.toByteArray(Charsets.UTF_8)
            } else {
                byteArrayOf(1) + "eng".toByteArray(Charsets.US_ASCII) +
                    "NusaMusic".toByteArray(Charsets.UTF_16) + byteArrayOf(0, 0) +
                    plain.toByteArray(Charsets.UTF_16)
            }, version)
            val payload = tagPayload.toByteArray()
            require(payload.size < MAX_TAG_BYTES) { "The combined MP3 metadata is too large." }
            FileOutputStream(output).use { destination ->
                destination.write(byteArrayOf(
                    'I'.code.toByte(), 'D'.code.toByte(), '3'.code.toByte(),
                    version.toByte(), 0, 0
                ))
                destination.write(syncsafe(payload.size))
                destination.write(payload)
                source.seek(audioOffset)
                sourceCopy(source, destination)
                destination.fd.sync()
            }
        }
    }

    private fun isOwnedLyrics(name: String, payload: ByteArray): Boolean {
        if (payload.size < 6) return false
        val encoding = payload[0].toInt() and 255
        // Only exact owned descriptors are replaced; unrelated lyrics survive.
        return when (name) {
            "TXXX" -> descriptor(payload, 1, encoding) == "LYRICS"
            "USLT" -> payload.size > 5 && descriptor(payload, 4, encoding) == "NusaMusic"
            else -> false
        }
    }

    private fun descriptor(bytes: ByteArray, start: Int, encoding: Int): String? {
        val unicode = encoding == 1 || encoding == 2
        val width = if (unicode) 2 else 1
        var cursor = start
        while (cursor + width <= bytes.size) {
            val terminated = bytes[cursor] == 0.toByte() &&
                (width == 1 || bytes[cursor + 1] == 0.toByte())
            if (terminated) {
                return runCatching {
                    val charset: Charset = when (encoding) {
                        1 -> Charsets.UTF_16
                        2 -> Charsets.UTF_16BE
                        3 -> Charsets.UTF_8
                        else -> Charsets.ISO_8859_1
                    }
                    bytes.copyOfRange(start, cursor).toString(charset).trim('\uFEFF')
                }.getOrNull()
            }
            cursor += width
        }
        return null
    }

    private fun addFrame(target: ByteArrayOutputStream, name: String, data: ByteArray, version: Int) {
        require(data.size <= MAX_FRAME_BYTES)
        target.write(name.toByteArray(Charsets.US_ASCII))
        if (version == 4) target.write(syncsafe(data.size))
        else target.write(byteArrayOf(
            (data.size ushr 24).toByte(), (data.size ushr 16).toByte(),
            (data.size ushr 8).toByte(), data.size.toByte()
        ))
        target.write(byteArrayOf(0, 0)) // No compression, encryption or grouping
        target.write(data)
    }

    private fun syncsafeRead(bytes: ByteArray, at: Int): Int {
        var result = 0
        for (offset in 0..3) {
            val b = bytes[at + offset].toInt() and 255
            require(b < 128) { "Invalid ID3 synchsafe size." }
            result = (result shl 7) or b
        }
        return result
    }

    private fun syncsafe(value: Int): ByteArray {
        require(value >= 0 && value < (1 shl 28))
        return byteArrayOf(
            ((value ushr 21) and 127).toByte(), ((value ushr 14) and 127).toByte(),
            ((value ushr 7) and 127).toByte(), (value and 127).toByte()
        )
    }

    private fun sourceCopy(input: RandomAccessFile, output: FileOutputStream) {
        val buffer = ByteArray(64 * 1024)
        while (true) {
            val n = input.read(buffer)
            if (n < 0) return
            output.write(buffer, 0, n)
        }
    }
}
