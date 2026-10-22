package com.yohandeku32.nusamusic.data

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile

/**
 * Rewrites unfragmented .m4a metadata without changing a single audio byte.
 *
 * Chunk offsets in stco/co64 are absolute file offsets. If moov is ahead of
 * mdat, leave a same-size free atom at the original moov location and append
 * the updated moov at EOF: original mdat offsets are then unchanged.
 * Existing metadata, cover art, track samples and unrelated atoms are copied
 * as-is. Fragmented / unusual containers are rejected rather than damaged.
 */
internal object M4aEmbeddedLyricsWriter {
    private const val MAX_MOOV = 48L * 1024 * 1024
    private const val MAX_LYRICS = 512 * 1024
    private data class FileAtom(val offset: Long, val size: Long, val header: Int, val type: String)
    private data class Box(val start: Int, val end: Int, val type: String)

    fun write(original: File, output: File, timed: String, plain: String) {
        require(original.canonicalPath != output.canonicalPath)
        require(timed.isNotBlank() && timed.toByteArray(Charsets.UTF_8).size <= MAX_LYRICS) {
            "Lyrics are empty or oversized."
        }
        RandomAccessFile(original, "r").use { input ->
            val length = input.length()
            val top = fileAtoms(input, length)
            require(top.firstOrNull()?.type == "ftyp" && top.any { it.type == "mdat" }) {
                "This is not a standard M4A audio file."
            }
            require(top.none { it.type in setOf("moof", "mfra", "sidx", "ssix", "iloc") }) {
                "Fragmented or indexed MP4 files cannot be edited safely."
            }
            val moovs = top.filter { it.type == "moov" }
            require(moovs.size == 1) { "M4A must contain exactly one moov metadata atom." }
            val moov = moovs.single()
            require(moov.size in 8..MAX_MOOV && moov.header == 8) {
                "Unsupported M4A metadata size/header."
            }
            // An mdat with size=0 consumes the remainder of the file; appending
            // a moov would put it inside audio instead. Reject this layout.
            require(top.none { it.type == "mdat" && it.offset + it.size == length &&
                it.header == 0 }) { "Unsupported open-ended mdat layout." }
            val bytes = ByteArray(moov.size.toInt())
            input.seek(moov.offset)
            input.readFully(bytes)
            val newMoov = updateMoov(bytes, timed, plain)
            FileOutputStream(output).use { out ->
                copyRange(input, out, 0, moov.offset)
                if (moov.offset + moov.size == length) {
                    // A tail moov can be replaced directly; all earlier chunk
                    // offsets still point at the original audio bytes.
                    out.write(newMoov)
                } else {
                    // The original position must remain occupied so moving moov
                    // does NOT shift any audio sample referenced by stco/co64.
                    writeFreeAtom(out, moov.size)
                    copyRange(input, out, moov.offset + moov.size,
                        length - moov.offset - moov.size)
                    out.write(newMoov)
                }
                out.fd.sync()
            }
        }
    }

    private fun fileAtoms(input: RandomAccessFile, length: Long): List<FileAtom> {
        val result = ArrayList<FileAtom>()
        var offset = 0L
        while (offset < length) {
            require(result.size < 4096 && length - offset >= 8) {
                "Malformed M4A top-level atom."
            }
            input.seek(offset)
            val first = input.readInt().toLong() and 0xFFFFFFFFL
            val typeBytes = ByteArray(4).also(input::readFully)
            val type = typeBytes.toString(Charsets.ISO_8859_1)
            val header = if (first == 1L) 16 else if (first == 0L) 0 else 8
            val size = when (first) {
                0L -> length - offset
                1L -> input.readLong()
                else -> first
            }
            require(size >= maxOf(8, header).toLong() && size <= length - offset) {
                "Invalid M4A atom size."
            }
            result += FileAtom(offset, size, header, type)
            offset += size
        }
        return result
    }

    private fun updateMoov(source: ByteArray, timed: String, plain: String): ByteArray {
        val boxes = boxes(source, 8, source.size)
        require(boxes.none { it.type in setOf("mvex", "cmov") }) {
            "Fragmented/compressed M4A metadata is not supported."
        }
        val existingUdta = boxes.filter { it.type == "udta" }
        require(existingUdta.size <= 1) { "Ambiguous multiple M4A udta boxes." }
        val children = ByteArrayOutputStream()
        var replaced = false
        for (box in boxes) {
            if (box.type == "udta") {
                children.write(rewriteUdta(source.copyOfRange(box.start + 8, box.end), timed, plain))
                replaced = true
            } else children.write(source, box.start, box.end - box.start)
        }
        if (!replaced) children.write(atom("udta", newMeta(timed, plain)))
        return atom("moov", children.toByteArray())
    }

    private fun rewriteUdta(payload: ByteArray, timed: String, plain: String): ByteArray {
        val entries = boxes(payload, 0, payload.size)
        require(entries.count { it.type == "meta" } <= 1) {
            "Ambiguous M4A metadata containers."
        }
        val out = ByteArrayOutputStream()
        var updated = false
        for (box in entries) {
            if (box.type == "meta") {
                out.write(rewriteMeta(payload.copyOfRange(box.start + 8, box.end), timed, plain))
                updated = true
            } else out.write(payload, box.start, box.end - box.start)
        }
        if (!updated) out.write(newMeta(timed, plain))
        return atom("udta", out.toByteArray())
    }

    private fun newMeta(timed: String, plain: String): ByteArray {
        val handler = byteArrayOf(0, 0, 0, 0, 0, 0, 0, 0) +
            "mdir".toByteArray(Charsets.US_ASCII) + ByteArray(12) +
            "appl".toByteArray(Charsets.US_ASCII) + byteArrayOf(0)
        return atom("meta", ByteArray(4) + atom("hdlr", handler) +
            atom("ilst", rewrittenIlst(byteArrayOf(), timed, plain)))
    }

    private fun rewriteMeta(payload: ByteArray, timed: String, plain: String): ByteArray {
        require(payload.size >= 4) { "Invalid M4A meta full box." }
        val entries = boxes(payload, 4, payload.size)
        require(entries.count { it.type == "ilst" } <= 1) {
            "Ambiguous M4A ilst metadata."
        }
        val out = ByteArrayOutputStream()
        out.write(payload, 0, 4) // existing full-box version and flags
        var updated = false
        for (box in entries) {
            if (box.type == "ilst") {
                out.write(atom("ilst", rewrittenIlst(
                    payload.copyOfRange(box.start + 8, box.end), timed, plain
                )))
                updated = true
            } else out.write(payload, box.start, box.end - box.start)
        }
        if (!updated) out.write(atom("ilst", rewrittenIlst(byteArrayOf(), timed, plain)))
        return atom("meta", out.toByteArray())
    }

    private fun rewrittenIlst(payload: ByteArray, timed: String, plain: String): ByteArray {
        val entries = boxes(payload, 0, payload.size)
        val hasReadable = entries.any { it.type == "©lyr" }
        val out = ByteArrayOutputStream()
        for (entry in entries) {
            val raw = payload.copyOfRange(entry.start, entry.end)
            if (entry.type == "----" && isOwnedFreeform(raw)) continue
            out.write(raw)
        }
        // Add common Apple-readable ©lyr only if not already present.
        if (!hasReadable) out.write(atom("©lyr", atom("data", textData(plain))))
        // Authored sync is stored in a NusaMusic-owned freeform entry; another
        // user's pre-existing ©lyr/---- metadata is never removed.
        out.write(atom("----", atom("mean", ByteArray(4) +
            "com.nusamusic".toByteArray(Charsets.UTF_8)) +
            atom("name", ByteArray(4) + "LYRICS".toByteArray(Charsets.UTF_8)) +
            atom("data", textData(timed))))
        return out.toByteArray()
    }

    private fun textData(value: String): ByteArray =
        byteArrayOf(0, 0, 0, 1, 0, 0, 0, 0) + value.toByteArray(Charsets.UTF_8)

    private fun isOwnedFreeform(bytes: ByteArray): Boolean {
        return runCatching {
            val fields = boxes(bytes, 8, bytes.size)
            fun tag(key: String): String? {
                val item = fields.firstOrNull { it.type == key } ?: return null
                if (item.end - item.start < 12) return null
                return bytes.copyOfRange(item.start + 12, item.end)
                    .toString(Charsets.UTF_8)
            }
            tag("mean") == "com.nusamusic" && tag("name") == "LYRICS"
        }.getOrDefault(false)
    }

    private fun boxes(source: ByteArray, from: Int, end: Int): List<Box> {
        require(from >= 0 && end <= source.size && from <= end)
        val result = ArrayList<Box>()
        var cursor = from
        while (cursor < end) {
            require(result.size < 4096 && cursor + 8 <= end)
            val size = ((source[cursor].toInt() and 255).toLong() shl 24) or
                ((source[cursor + 1].toInt() and 255).toLong() shl 16) or
                ((source[cursor + 2].toInt() and 255).toLong() shl 8) or
                (source[cursor + 3].toInt() and 255).toLong()
            require(size in 8..(end - cursor).toLong()) {
                "Unsupported M4A nested atom size."
            }
            val type = String(source, cursor + 4, 4, Charsets.ISO_8859_1)
            result += Box(cursor, cursor + size.toInt(), type)
            cursor += size.toInt()
        }
        return result
    }

    private fun atom(name: String, payload: ByteArray): ByteArray {
        require(payload.size <= Int.MAX_VALUE - 8)
        val type = name.toByteArray(Charsets.ISO_8859_1)
        require(type.size == 4)
        val length = payload.size + 8
        return byteArrayOf(
            (length ushr 24).toByte(), (length ushr 16).toByte(),
            (length ushr 8).toByte(), length.toByte()
        ) + type + payload
    }

    private fun writeFreeAtom(output: FileOutputStream, size: Long) {
        require(size in 8..Int.MAX_VALUE.toLong())
        val atomSize = size.toInt()
        output.write(byteArrayOf(
            (atomSize ushr 24).toByte(), (atomSize ushr 16).toByte(),
            (atomSize ushr 8).toByte(), atomSize.toByte()
        ))
        output.write("free".toByteArray(Charsets.US_ASCII))
        var rest = size - 8
        val zeros = ByteArray(64 * 1024)
        while (rest > 0) {
            val n = minOf(rest, zeros.size.toLong()).toInt()
            output.write(zeros, 0, n)
            rest -= n
        }
    }

    private fun copyRange(input: RandomAccessFile, out: FileOutputStream, start: Long, length: Long) {
        input.seek(start)
        var left = length
        val buffer = ByteArray(64 * 1024)
        while (left > 0) {
            val n = input.read(buffer, 0, minOf(buffer.size.toLong(), left).toInt())
            require(n > 0) { "Source M4A was truncated during metadata update." }
            out.write(buffer, 0, n)
            left -= n
        }
    }
}
