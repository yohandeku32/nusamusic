package com.yohandeku32.nusamusic.data

import java.io.ByteArrayOutputStream
import java.nio.charset.Charset
import java.util.Locale

/** Bounded, seekable metadata reader. Never scans audio payloads or sibling files. */
internal class EmbeddedLyricsReader(
    private val length: Long,
    private val readAt: (Long, Int) -> ByteArray
) {
    private val candidates = ArrayList<String>()
    private val composers = LinkedHashSet<String>()
    private val lyricists = LinkedHashSet<String>()
    private var bytesRead = 0
    private var atomsRead = 0
    private var preferredPlainLyrics: String? = null
    // MP4/QuickTime "mdta" indexes their text fields through keys + numeric
    // ilst entries rather than iTunes ©lyr. Many external taggers use this.
    private var indexedMetadataKeys: Map<Long, String> = emptyMap()
    private val lyricKeys = setOf("LYRICS", "UNSYNCEDLYRICS", "UNSYNCED LYRICS", "SYNCEDLYRICS",
        "SYNCED LYRICS", "SYNCHRONIZEDLYRICS", "SYLT", "USLT", "LRC", "ENHANCED LRC", "TTML",
        "NUSAMUSIC_LYRICS")

    fun read(): List<String> = readMetadata().lyrics

    fun readMetadata(): EmbeddedLyricsMetadata {
        var start = 0L
        val header = bytes(0, 12)
        if (header.size < 10) return EmbeddedLyricsMetadata(emptyList())
        if (header.text(0, 3) == "ID3") {
            val size = syncSafe(header, 6)
            if (size in 0..MAX_TAG) {
                isolate { id3(bytes(0, size + 10)) }
                start = size + 10L + if (header.u(3) == 4 && header.u(5) and 0x10 != 0) 10 else 0
            }
        }
        val magic = bytes(start, 12)
        when {
            magic.text(0, 4) == "fLaC" -> isolate { flac(start + 4) }
            magic.text(0, 4) == "OggS" -> isolate { ogg(start) }
            magic.text(4, 4) == "ftyp" -> isolate { atoms(start, length, 0, false) }
            magic.text(0, 4) in setOf("RIFF", "FORM") -> isolate { riff(start, magic.text(0, 4) == "RIFF") }
        }
        return EmbeddedLyricsMetadata(
            candidates.toList(), SongCredits(composers.toList(), lyricists.toList()),
            preferredPlainLyrics
        )
    }

    private inline fun isolate(block: () -> Unit) {
        try { block() } catch (_: IllegalArgumentException) { /* Malformed optional metadata. */ }
        catch (_: IndexOutOfBoundsException) { /* Retain other valid lyric frames. */ }
    }

    private fun bytes(offset: Long, count: Int): ByteArray {
        require(offset >= 0 && count in 0..MAX_TAG + 10 && bytesRead.toLong() + count <= MAX_READ)
        if (offset >= length) return byteArrayOf()
        val result = readAt(offset, minOf(count.toLong(), length - offset).toInt())
        bytesRead += result.size
        return result
    }

    private fun add(text: String) {
        val clean = text.trim('\uFEFF', '\u0000', ' ', '\r', '\n')
        if (clean.isNotBlank() && clean.toByteArray(Charsets.UTF_8).size <= LyricsDecoder.MAX_BYTES &&
            candidates.size < 32 && clean !in candidates) candidates += clean
    }

    private fun field(key: String, text: String) {
        val role = key.uppercase(Locale.ROOT)
        if (role in lyricKeys) { add(text); return }
        val names = when (role) {
            "COMPOSER", "TCOM", "TCM" -> composers
            "LYRICIST", "TEXT", "TXT" -> lyricists
            else -> return
        }
        if (text.length > 8192) return
        text.split('\u0000', '\n', '\r').forEach { value ->
            val clean = value.trim().filter { !it.isISOControl() }
            if (clean.isNotBlank() && clean.length <= 2048 && names.size < 32) names += clean
        }
    }

    private fun id3(tag: ByteArray) {
        require(tag.size >= 10 && tag.text(0, 3) == "ID3")
        val version = tag.u(3)
        require(version in 2..4)
        val flags = tag.u(5)
        // v2.2 tag compression has no compatible frame layout.
        if (version == 2 && flags and 0x40 != 0) return
        var body = tag.copyOfRange(10, minOf(tag.size, 10 + syncSafe(tag, 6)))
        if (version < 4 && flags and 0x80 != 0) body = deUnsync(body)
        var p = 0
        if (version >= 3 && flags and 0x40 != 0) {
            p = if (version == 4) syncSafe(body, 0) else body.be(0, 4).toInt() + 4
            require(p in 0..body.size)
        }
        val headerSize = if (version == 2) 6 else 10
        var frames = 0
        while (p + headerSize <= body.size && frames++ < 4096) {
            val name = body.text(p, if (version == 2) 3 else 4)
            if (name.all { it == '\u0000' }) break
            val size = if (version == 4) syncSafe(body, p + 4) else body.be(p + if (version == 2) 3 else 4, if (version == 2) 3 else 4).toInt()
            require(size >= 0 && size <= body.size - p - headerSize)
            val format = if (version == 2) 0 else body.u(p + 9)
            val payloadStart = p + headerSize
            p += headerSize + size
            if (name !in setOf("USLT", "ULT", "SYLT", "SLT", "TXXX", "TXX", "TCOM", "TCM", "TEXT", "TXT")) continue
            var payload = body.copyOfRange(payloadStart, p)
            if ((version == 3 && format and 0xC0 != 0) || (version == 4 && format and 0x0C != 0)) continue
            isolate {
                if (version == 4 && (format and 0x02 != 0 || flags and 0x80 != 0)) payload = deUnsync(payload)
                var prefix = if ((version == 3 && format and 0x20 != 0) || (version == 4 && format and 0x40 != 0)) 1 else 0
                if (version == 4 && format and 0x01 != 0) prefix += 4
                if (prefix > 0) payload = payload.copyOfRange(prefix, payload.size)
                when (name) {
                    "USLT", "ULT" -> {
                        val encoding = payload.u(0)
                        val (_, afterDescription) = terminated(payload, 4, encoding)
                        add(decode(payload.copyOfRange(afterDescription, payload.size), encoding))
                    }
                    "SYLT", "SLT" -> sylt(payload)
                    "TCOM", "TCM", "TEXT", "TXT" -> field(name, decode(payload.copyOfRange(1, payload.size), payload.u(0)))
                    else -> {
                        val encoding = payload.u(0)
                        val (key, valueStart) = terminated(payload, 1, encoding)
                        field(key, decode(payload.copyOfRange(valueStart, payload.size), encoding))
                    }
                }
            }
        }
    }

    private fun sylt(payload: ByteArray) {
        val encoding = payload.u(0)
        // MPEG-frame timestamps require a frame index; never pretend they are milliseconds.
        if (payload.u(4) != 2 || payload.u(5) !in setOf(0, 1)) return
        var p = terminated(payload, 6, encoding).second
        val units = ArrayList<Pair<Long, String>>()
        var inheritedEncoding = encoding
        while (p < payload.size && units.size < 8000) {
            val (text, next) = terminated(payload, p, inheritedEncoding)
            if (encoding == 1 && next - p >= 4) {
                if (payload.u(p) == 0xFF && payload.u(p + 1) == 0xFE) inheritedEncoding = 4
                if (payload.u(p) == 0xFE && payload.u(p + 1) == 0xFF) inheritedEncoding = 2
            }
            require(next + 4 <= payload.size)
            val time = payload.be(next, 4)
            require(time < Int.MAX_VALUE)
            units += time to text.replace("\r\n", "\n").replace('\r', '\n')
            p = next + 4
        }
        // Repair out-of-order entries without inventing new boundaries within their text.
        val rows = ArrayList<MutableList<Pair<Long, String>>>()
        var row = ArrayList<Pair<Long, String>>()
        for ((time, text) in units.sortedBy { it.first }) {
            val pieces = text.split('\n')
            pieces.forEachIndexed { index, piece ->
                if (index > 0 && row.isNotEmpty()) { rows += row; row = ArrayList() }
                if (piece.isNotEmpty()) row += time to piece
            }
        }
        if (row.isNotEmpty()) rows += row
        add(rows.joinToString("\n") { parts ->
            val start = "[${clock(parts.first().first)}]"
            if (parts.size == 1) start + parts.single().second
            else start + parts.joinToString("") { (time, text) -> "<${clock(time)}>$text" }
        })
    }

    private fun flac(begin: Long) {
        var p = begin
        repeat(128) {
            val header = bytes(p, 4)
            require(header.size == 4)
            val size = header.be(1, 3).toInt()
            if (header.u(0) and 0x7F == 4) isolate { comments(bytes(p + 4, size), 0) }
            p += size + 4L
            if (header.u(0) and 0x80 != 0) return
        }
    }

    private fun comments(data: ByteArray, start: Int) {
        var p = start
        fun sized(): ByteArray {
            val count = data.le(p, 4)
            p += 4
            require(count <= data.size - p)
            return data.copyOfRange(p, p + count.toInt()).also { p += count.toInt() }
        }
        sized() // vendor
        val count = data.le(p, 4)
        p += 4
        require(count <= 20_000)
        repeat(count.toInt()) {
            val field = sized().toString(Charsets.UTF_8)
            field(field.substringBefore('='), field.substringAfter('=', ""))
        }
    }

    private fun ogg(begin: Long) {
        var p = begin
        val packets = HashMap<Long, ByteArrayOutputStream>()
        val completed = HashMap<Long, Int>()
        repeat(512) {
            val header = bytes(p, 27)
            require(header.size == 27 && header.text(0, 4) == "OggS" && header.u(4) == 0)
            val serial = header.le(14, 4)
            require(packets.size < 16 || serial in packets)
            val laces = bytes(p + 27, header.u(26))
            var offset = p + 27 + laces.size
            val packet = packets.getOrPut(serial) { ByteArrayOutputStream() }
            for (lace in laces) {
                val size = lace.toInt() and 255
                require(packet.size() + size <= MAX_TAG)
                packet.write(bytes(offset, size))
                offset += size
                if (size < 255) {
                    val data = packet.toByteArray()
                    packet.reset()
                    when {
                        data.text(0, 8) == "OpusTags" -> { comments(data, 8); return }
                        data.uOrNull(0) == 3 && data.text(1, 6) == "vorbis" -> { comments(data, 7); return }
                    }
                    completed[serial] = (completed[serial] ?: 0) + 1
                    if (completed.getValue(serial) > 3) return
                }
            }
            p = offset
            require(p - begin <= MAX_TAG)
        }
    }

    private data class Atom(val name: String, val start: Long, val end: Long)
    private fun atomList(start: Long, end: Long): List<Atom> {
        val result = ArrayList<Atom>()
        var p = start
        while (p + 8 <= end && ++atomsRead <= 4096) {
            val header = bytes(p, minOf(16L, end - p).toInt())
            require(header.size >= 8)
            val smallSize = header.be(0, 4)
            val size = when (smallSize) { 0L -> end - p; 1L -> header.be(8, 8); else -> smallSize }
            val headerSize = if (smallSize == 1L) 16 else 8
            require(size >= headerSize && size <= end - p)
            result += Atom(header.text(4, 4), p + headerSize, p + size)
            p += size // Skip mdat/artwork without reading it, including moov-at-end files.
        }
        return result
    }

    // QuickTime user-data ©lyr atoms can contain a 16-bit byte count and
    // language code followed by text, rather than an iTunes nested data box.
    // Only accept a well-formed length and printable UTF-8, never audio bytes.
    private fun quickTimeCopyrightText(start: Long, end: Long): String? {
        val length = end - start
        if (length !in 5L..(LyricsDecoder.MAX_BYTES + 4).toLong()) return null
        val raw = bytes(start, length.toInt())
        if (raw.size < 5) return null
        val textLength = ((raw[0].toInt() and 255) shl 8) or (raw[1].toInt() and 255)
        if (textLength <= 0 || textLength > raw.size - 4) return null
        val value = raw.copyOfRange(4, textLength + 4).toString(Charsets.UTF_8)
            .trim('\uFEFF', '\u0000', ' ', '\r', '\n')
        if (value.isEmpty() || value.any {
                it == '\uFFFD' || (it.isISOControl() && it !in setOf('\n', '\r', '\t'))
            }) return null
        return value
    }

    private fun readIndexedMetadataKeys(start: Long, end: Long) {
        if (end - start < 8) return
        val header = bytes(start, 8)
        if (header.size != 8) return
        val count = header.be(4, 4)
        if (count !in 1L..256L) return
        var cursor = start + 8
        val discovered = LinkedHashMap<Long, String>()
        for (index in 1..count.toInt()) {
            if (cursor + 8 > end) return
            val entry = bytes(cursor, 8)
            if (entry.size != 8) return
            val size = entry.be(0, 4)
            if (size < 8 || size > end - cursor) return
            if (entry.text(4, 4) == "mdta" && size <= 520) {
                val name = bytes(cursor + 8, (size - 8).toInt()).toString(Charsets.UTF_8)
                if (name.isNotBlank() && name.none { it == '\uFFFD' }) {
                    discovered[index.toLong()] = name
                }
            }
            cursor += size
        }
        indexedMetadataKeys = discovered
    }

    private fun indexedMetadataField(type: String): String? {
        if (type.length != 4) return null
        val index = type.fold(0L) { value, char ->
            (value shl 8) or (char.code.toLong() and 0xFF)
        }
        return indexedMetadataKeys[index]
            ?.substringAfterLast('.')
            ?.substringAfterLast(':')
    }

    private fun atoms(start: Long, end: Long, depth: Int, inIlst: Boolean) {
        require(depth <= 12)
        for (atom in atomList(start, end)) isolate {
            val indexedField = if (inIlst) indexedMetadataField(atom.name) else null
            when {
                atom.name in setOf("moov", "udta", "ilst") -> atoms(atom.start, atom.end, depth + 1, atom.name == "ilst")
                atom.name == "meta" -> {
                    // iTunes uses a 4-byte version/flags prefix; QuickTime
                    // editors also write meta as a plain container without it.
                    // Probe headers instead of silently losing those lyrics.
                    val withFullBoxHeader = runCatching {
                        atomList(atom.start + 4, atom.end).any {
                            it.name in setOf("ilst", "hdlr", "keys", "meta")
                        }
                    }.getOrDefault(false)
                    val bodyStart = if (withFullBoxHeader) atom.start + 4 else atom.start
                    // Decode keys before ilst: QuickTime does not require keys
                    // to appear earlier than the indexed values.
                    atomList(bodyStart, atom.end)
                        .firstOrNull { it.name == "keys" }
                        ?.let { readIndexedMetadataKeys(it.start, it.end) }
                    atoms(bodyStart, atom.end, depth + 1, false)
                }
                atom.name == "ID32" && atom.end - atom.start <= MAX_TAG -> {
                    val data = bytes(atom.start, (atom.end - atom.start).toInt())
                    val index = (0..minOf(16, data.size - 3)).firstOrNull { data.text(it, 3) == "ID3" }
                    if (index != null) id3(data.copyOfRange(index, data.size))
                }
                (inIlst && (atom.name in setOf("©lyr", "©wrt", "----") ||
                    indexedField != null)) || atom.name == "©lyr" -> {
                    val children = runCatching { atomList(atom.start, atom.end) }.getOrNull()
                    if (children == null) {
                        if (atom.name == "©lyr") {
                            quickTimeCopyrightText(atom.start, atom.end)?.let { value ->
                                add(value)
                                preferredPlainLyrics = value
                            }
                        }
                        return@isolate
                    }
                    val name = children.firstOrNull { it.name == "name" }?.let {
                        if (it.end - it.start in 4..256) bytes(it.start + 4, (it.end - it.start - 4).toInt()).toString(Charsets.UTF_8) else ""
                    }
                    val key = when (atom.name) {
                        "©lyr" -> "LYRICS"
                        "©wrt" -> "COMPOSER"
                        "----" -> name.orEmpty().substringAfterLast('.').substringAfterLast(':')
                        else -> indexedField.orEmpty()
                    }
                    if (key.uppercase(Locale.ROOT) in lyricKeys + setOf("COMPOSER", "LYRICIST")) {
                        children.filter { it.name == "data" }.forEach {
                            if (it.end - it.start in 8..LyricsDecoder.MAX_BYTES + 8L) {
                                val data = bytes(it.start, (it.end - it.start).toInt())
                                val type = data.be(0, 4).toInt() and 0xFFFFFF
                                if (type in 0..2) {
                                    val decoded = decode(
                                        data.copyOfRange(8, data.size),
                                        if (type == 2) 2 else 3
                                    )
                                    field(key, decoded)
                                    if ((atom.name == "©lyr" ||
                                        indexedField?.uppercase(Locale.ROOT) in
                                            setOf("LYRICS", "UNSYNCEDLYRICS", "UNSYNCED LYRICS")) &&
                                        decoded.isNotBlank()) {
                                        preferredPlainLyrics = decoded
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun riff(begin: Long, littleEndian: Boolean) {
        var p = begin + 12
        repeat(2048) {
            if (p + 8 > length) return
            val header = bytes(p, 8)
            require(header.size == 8)
            val size = if (littleEndian) header.le(4, 4) else header.be(4, 4)
            require(size <= length - p - 8)
            if (header.text(0, 4).equals("id3 ", true) && size <= MAX_TAG) isolate { id3(bytes(p + 8, size.toInt())) }
            p += 8 + size + size % 2
        }
    }

    companion object {
        private const val MAX_TAG = 16 * 1024 * 1024
        private const val MAX_READ = 32 * 1024 * 1024
        private fun ByteArray.u(p: Int) = this[p].toInt() and 255
        private fun ByteArray.uOrNull(p: Int) = getOrNull(p)?.toInt()?.and(255)
        private fun ByteArray.text(p: Int, count: Int) = if (p >= 0 && p + count <= size) String(this, p, count, Charsets.ISO_8859_1) else ""
        private fun ByteArray.be(p: Int, count: Int): Long {
            require(p >= 0 && count >= 0 && p + count <= size)
            var result = 0L
            repeat(count) { require(result <= Long.MAX_VALUE ushr 8); result = (result shl 8) or u(p + it).toLong() }
            return result
        }
        private fun ByteArray.le(p: Int, count: Int): Long {
            require(p >= 0 && p + count <= size)
            var result = 0L
            repeat(count) { result = result or (u(p + it).toLong() shl (it * 8)) }
            return result
        }
        private fun syncSafe(data: ByteArray, p: Int): Int {
            var result = 0
            repeat(4) { require(data.u(p + it) < 128); result = (result shl 7) or data.u(p + it) }
            return result
        }
        private fun deUnsync(data: ByteArray): ByteArray = ByteArrayOutputStream(data.size).apply {
            var p = 0
            while (p < data.size) { val value = data.u(p++); write(value); if (value == 255 && p < data.size && data.u(p) == 0) p++ }
        }.toByteArray()
        private fun decode(data: ByteArray, encoding: Int): String {
            val charset: Charset = when (encoding) {
                0 -> Charsets.ISO_8859_1; 1 -> Charsets.UTF_16; 2 -> Charsets.UTF_16BE
                3 -> Charsets.UTF_8; 4 -> Charsets.UTF_16LE; else -> throw IllegalArgumentException("Unsupported ID3 encoding")
            }
            return data.toString(charset).trim('\uFEFF', '\u0000')
        }
        private fun terminated(data: ByteArray, begin: Int, encoding: Int): Pair<String, Int> {
            val width = if (encoding in setOf(1, 2, 4)) 2 else 1
            var end = begin
            while (end + width <= data.size) {
                if (data.u(end) == 0 && (width == 1 || data.u(end + 1) == 0)) return decode(data.copyOfRange(begin, end), encoding) to end + width
                end += width
            }
            throw IllegalArgumentException("Unterminated metadata text")
        }
        private fun clock(time: Long) = "%02d:%02d.%03d".format(Locale.ROOT, time / 60_000, time / 1000 % 60, time % 1000)
    }
}
