package com.yohandeku32.nusamusic.data

import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile

/**
 * Lossless FLAC metadata rewrite. The STREAMINFO/audio frames and unrelated
 * Vorbis comments, pictures and application blocks remain byte-for-byte intact.
 * A dedicated NUSAMUSIC_LYRICS Vorbis field holds authored TTML/Enhanced LRC;
 * conventional LYRICS text is added only if it does not already exist.
 */
internal object FlacEmbeddedLyricsWriter {
    private const val MAX_METADATA_BLOCK = 0xFFFFFF
    private const val MAX_TOTAL_METADATA = 48L * 1024 * 1024
    private const val MAX_LYRICS = 512 * 1024

    private data class Block(val type: Int, val contents: ByteArray)

    fun write(original: File, output: File, timed: String, plain: String) {
        require(original.canonicalPath != output.canonicalPath)
        require(timed.isNotBlank() && timed.toByteArray(Charsets.UTF_8).size <= MAX_LYRICS) {
            "Lyrics exceed the supported metadata size."
        }
        RandomAccessFile(original, "r").use { source ->
            val magic = ByteArray(4)
            source.readFully(magic)
            require(magic.contentEquals("fLaC".toByteArray())) {
                "The selected file is not a native FLAC stream."
            }
            val blocks = ArrayList<Block>()
            var last = false
            var commentIndex = -1
            var consumed = 4L
            while (!last) {
                require(blocks.size < 128 && consumed < MAX_TOTAL_METADATA) {
                    "FLAC has too many metadata blocks."
                }
                val header = ByteArray(4)
                source.readFully(header)
                last = header[0].toInt() and 0x80 != 0
                val type = header[0].toInt() and 0x7F
                val size = ((header[1].toInt() and 255) shl 16) or
                    ((header[2].toInt() and 255) shl 8) or (header[3].toInt() and 255)
                require(type != 127 && consumed + 4L + size <= MAX_TOTAL_METADATA &&
                    consumed + 4L + size <= source.length()) { "Invalid FLAC metadata." }
                val payload = ByteArray(size)
                source.readFully(payload)
                consumed += 4L + size
                if (blocks.isEmpty()) require(type == 0 && size == 34) {
                    "FLAC must begin with a 34-byte STREAMINFO block."
                }
                if (type == 4) {
                    require(commentIndex == -1) { "Multiple Vorbis comments are not supported safely." }
                    commentIndex = blocks.size
                }
                blocks += Block(type, payload)
            }
            val initial = if (commentIndex >= 0) blocks[commentIndex].contents else null
            val changed = updatedComments(initial, timed, plain)
            require(changed.size <= MAX_METADATA_BLOCK) { "FLAC lyric metadata is too large." }
            if (commentIndex >= 0) blocks[commentIndex] = Block(4, changed)
            else blocks += Block(4, changed)

            FileOutputStream(output).use { out ->
                out.write(magic)
                blocks.forEachIndexed { i, block ->
                    val length = block.contents.size
                    out.write((block.type or if (i == blocks.lastIndex) 0x80 else 0).toByte().toInt())
                    out.write(length ushr 16)
                    out.write(length ushr 8)
                    out.write(length)
                    out.write(block.contents)
                }
                val buffer = ByteArray(64 * 1024)
                while (true) {
                    val count = source.read(buffer)
                    if (count < 0) break
                    out.write(buffer, 0, count)
                }
                out.fd.sync()
            }
        }
    }

    private fun updatedComments(
        original: ByteArray?, timed: String, plain: String
    ): ByteArray {
        var vendor = "NusaMusic".toByteArray(Charsets.UTF_8)
        val entries = ArrayList<ByteArray>()
        var hasRegularLyrics = false
        if (original != null) {
            var p = 0
            fun next(): ByteArray {
                require(p + 4 <= original.size) { "Truncated FLAC Vorbis comment." }
                val length = (original[p].toInt() and 255) or
                    ((original[p + 1].toInt() and 255) shl 8) or
                    ((original[p + 2].toInt() and 255) shl 16) or
                    ((original[p + 3].toInt() and 255) shl 24)
                p += 4
                require(length in 0..MAX_METADATA_BLOCK && length <= original.size - p) {
                    "Invalid Vorbis comment field length."
                }
                return original.copyOfRange(p, p + length).also { p += length }
            }
            vendor = next()
            require(p + 4 <= original.size)
            val count = (original[p].toInt() and 255) or
                ((original[p + 1].toInt() and 255) shl 8) or
                ((original[p + 2].toInt() and 255) shl 16) or
                ((original[p + 3].toInt() and 255) shl 24)
            require(count in 0..20_000)
            p += 4
            repeat(count) {
                val field = next()
                val key = field.toString(Charsets.UTF_8).substringBefore('=')
                if (key.equals("NUSAMUSIC_LYRICS", ignoreCase = true)) return@repeat
                if (key.equals("LYRICS", ignoreCase = true) ||
                    key.equals("UNSYNCEDLYRICS", ignoreCase = true)) hasRegularLyrics = true
                entries += field
            }
            require(p == original.size) { "Unexpected trailing FLAC comment bytes." }
        }
        if (!hasRegularLyrics) entries += ("LYRICS=" + plain).toByteArray(Charsets.UTF_8)
        entries += ("NUSAMUSIC_LYRICS=" + timed).toByteArray(Charsets.UTF_8)
        val out = ByteArrayOutputStream()
        DataOutputStream(out).use { data ->
            fun writeLE(value: Int) {
                data.writeByte(value)
                data.writeByte(value ushr 8)
                data.writeByte(value ushr 16)
                data.writeByte(value ushr 24)
            }
            fun writeField(bytes: ByteArray) {
                writeLE(bytes.size)
                data.write(bytes)
            }
            writeField(vendor)
            writeLE(entries.size)
            entries.forEach(::writeField)
        }
        return out.toByteArray()
    }
}
