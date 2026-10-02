package com.yohandeku32.nusamusic.data

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.container.MdtaMetadataEntry
import androidx.media3.extractor.metadata.id3.BinaryFrame
import androidx.media3.extractor.metadata.id3.TextInformationFrame
import androidx.media3.extractor.metadata.vorbis.VorbisComment
import androidx.media3.inspector.MetadataRetriever
import com.yohandeku32.nusamusic.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.w3c.dom.Element
import java.io.StringReader
import java.util.concurrent.TimeUnit
import javax.xml.parsers.DocumentBuilderFactory
import org.xml.sax.InputSource

data class LyricLine(
    val startMs: Long,
    val endMs: Long,
    val text: String
)

data class LyricsResult(
    val lines: List<LyricLine>,
    val source: String
)

@OptIn(UnstableApi::class)
object LyricsLoader {
    suspend fun load(
        context: Context,
        song: Song
    ): LyricsResult? = withContext(Dispatchers.IO) {
        // Parse the actual audio container first. Media3's high-level metadata
        // path does not expose every lyric tag used by local music files.
        extractRawEmbeddedLyrics(context, song.uri)?.let { raw ->
            parseEmbeddedText(raw)?.let {
                return@withContext LyricsResult(it, "Embedded")
            }
        }

        // Keep Media3 as a secondary fallback.
        loadEmbedded(context, song.uri)
    }


    private fun extractRawEmbeddedLyrics(
        context: Context,
        uriString: String
    ): String? {
        val uri = runCatching { Uri.parse(uriString) }.getOrNull() ?: return null

        val bytes = runCatching {
            context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        }.getOrNull() ?: return null

        if (bytes.isEmpty()) return null

        // TTML may be embedded directly in an MP4/M4A metadata payload.
        findEmbeddedTtml(bytes)?.let { return it }

        return when {
            bytes.startsWithAscii("ID3") -> extractId3Lyrics(bytes)
            bytes.startsWithAscii("fLaC") -> extractFlacLyrics(bytes)
            looksLikeMp4(bytes) -> extractMp4Lyrics(bytes)
            looksLikeMp3(bytes) -> extractId3Lyrics(bytes)
            else -> extractTaggedText(bytes, "LYRICS")
        }
    }

    private fun looksLikeMp3(bytes: ByteArray): Boolean {
        var offset = 0
        if (bytes.size >= 10 && bytes.startsWithAscii("ID3")) {
            offset = (10 + syncSafeInt(bytes, 6)).coerceAtMost(bytes.size)
        }

        while (offset + 2 < bytes.size) {
            if ((bytes[offset].toInt() and 0xFF) == 0xFF) {
                val second = bytes[offset + 1].toInt() and 0xFF
                if ((second and 0xE0) == 0xE0) return true
            }
            if (bytes[offset].toInt() != 0) break
            offset++
        }
        return false
    }

    private fun looksLikeMp4(bytes: ByteArray): Boolean {
        if (bytes.size < 12) return false
        val ftyp = "ftyp".toByteArray(Charsets.US_ASCII)
        return bytes.indexOfBytes(ftyp, 0, minOf(bytes.size, 64)) >= 0
    }

    private fun extractId3Lyrics(bytes: ByteArray): String? {
        if (!bytes.startsWithAscii("ID3") || bytes.size < 10) return null

        val version = bytes[3].toInt() and 0xFF
        val flags = bytes[5].toInt() and 0xFF
        var offset = 10

        if ((flags and 0x40) != 0 && offset + 4 <= bytes.size) {
            val extensionSize = if (version >= 4) {
                syncSafeInt(bytes, offset)
            } else {
                int32(bytes, offset)
            }
            offset += if (version >= 4) extensionSize else extensionSize + 4
        }

        val end = (10 + syncSafeInt(bytes, 6)).coerceAtMost(bytes.size)

        while (offset + 10 <= end) {
            val id = bytes.copyOfRange(offset, offset + 4)
                .toString(Charsets.ISO_8859_1)
                .trim('\u0000', ' ')

            if (id.isBlank()) break

            val frameSize = if (version >= 4) {
                syncSafeInt(bytes, offset + 4)
            } else {
                int32(bytes, offset + 4)
            }

            if (frameSize <= 0 || offset + 10 + frameSize > end) break

            val payload = bytes.copyOfRange(
                offset + 10,
                offset + 10 + frameSize
            )

            when (id.uppercase()) {
                "USLT" -> {
                    val text = decodeUsltText(payload)
                    if (!text.isNullOrBlank()) return text
                }

                "SYLT" -> {
                    parseSyltAsLines(payload)?.let { lines ->
                        return lines.joinToString("\n") { line ->
                            "[" + (line.startMs / 60000) + ":" +
                                ((line.startMs / 1000) % 60).toString().padStart(2, '0') +
                                "." +
                                (line.startMs % 1000).toString().padStart(3, '0') +
                                "] " + line.text
                        }
                    }
                }

                "TXXX" -> {
                    decodeTxxx(payload)?.let { (description, text) ->
                        if (
                            description.contains(
                                "LYRIC",
                                ignoreCase = true
                            ) &&
                            text.isNotBlank()
                        ) {
                            return text
                        }
                    }
                }
            }

            offset += 10 + frameSize
        }

        return null
    }

    private fun decodeUsltText(payload: ByteArray): String? {
        if (payload.size < 4) return null

        val encoding = payload[0].toInt() and 0xFF
        val charset = when (encoding) {
            0 -> Charsets.ISO_8859_1
            1 -> Charsets.UTF_16
            2 -> Charsets.UTF_16BE
            3 -> Charsets.UTF_8
            else -> Charsets.UTF_8
        }
        val terminatorSize = if (encoding == 1 || encoding == 2) 2 else 1

        var offset = 4
        while (offset + terminatorSize <= payload.size) {
            val terminated = if (terminatorSize == 2) {
                payload[offset].toInt() == 0 &&
                    payload[offset + 1].toInt() == 0
            } else {
                payload[offset].toInt() == 0
            }

            if (terminated) {
                offset += terminatorSize
                break
            }
            offset++
        }

        if (offset >= payload.size) return null

        return runCatching {
            charset.decode(
                java.nio.ByteBuffer.wrap(
                    payload,
                    offset,
                    payload.size - offset
                )
            ).toString().trim()
        }.getOrNull()
    }

    private fun decodeTxxx(payload: ByteArray): Pair<String, String>? {
        if (payload.isEmpty()) return null

        val encoding = payload[0].toInt() and 0xFF
        val charset = when (encoding) {
            0 -> Charsets.ISO_8859_1
            1 -> Charsets.UTF_16
            2 -> Charsets.UTF_16BE
            3 -> Charsets.UTF_8
            else -> Charsets.UTF_8
        }

        val terminatorSize = if (encoding == 1 || encoding == 2) 2 else 1
        val separator = findTerminator(payload, 1, terminatorSize)
        if (separator < 0) return null

        val description = runCatching {
            charset.decode(
                java.nio.ByteBuffer.wrap(
                    payload,
                    1,
                    separator - 1
                )
            ).toString().trim()
        }.getOrNull().orEmpty()

        val textStart = separator + terminatorSize
        if (textStart >= payload.size) return null

        val text = runCatching {
            charset.decode(
                java.nio.ByteBuffer.wrap(
                    payload,
                    textStart,
                    payload.size - textStart
                )
            ).toString().trim()
        }.getOrNull().orEmpty()

        return description to text
    }

    private fun extractFlacLyrics(bytes: ByteArray): String? {
        if (!bytes.startsWithAscii("fLaC")) return null

        var offset = 4
        while (offset + 4 <= bytes.size) {
            val type = bytes[offset].toInt() and 0x7F
            val isLast = (bytes[offset].toInt() and 0x80) != 0
            val blockSize = ((bytes[offset + 1].toInt() and 0xFF) shl 16) or
                ((bytes[offset + 2].toInt() and 0xFF) shl 8) or
                (bytes[offset + 3].toInt() and 0xFF)

            val blockStart = offset + 4
            val blockEnd = blockStart + blockSize
            if (blockEnd > bytes.size) break

            if (type == 4) {
                parseVorbisComments(
                    bytes,
                    blockStart,
                    blockEnd
                )?.let { return it }
            }

            offset = blockEnd
            if (isLast) break
        }

        return null
    }

    private fun parseVorbisComments(
        bytes: ByteArray,
        start: Int,
        end: Int
    ): String? {
        if (start + 4 > end) return null

        var offset = start
        val vendorLength = littleEndianInt(bytes, offset)
        offset += 4
        if (vendorLength < 0 || offset + vendorLength > end) return null
        offset += vendorLength

        if (offset + 4 > end) return null
        val commentCount = littleEndianInt(bytes, offset)
        offset += 4

        repeat(commentCount.coerceIn(0, 10_000)) {
            if (offset + 4 > end) return null

            val length = littleEndianInt(bytes, offset)
            offset += 4
            if (length < 0 || offset + length > end) return null

            val comment = runCatching {
                bytes.copyOfRange(offset, offset + length)
                    .toString(Charsets.UTF_8)
            }.getOrNull().orEmpty()

            val equals = comment.indexOf('=')
            if (equals > 0) {
                val key = comment.substring(0, equals)
                val value = comment.substring(equals + 1)

                if (
                    key.equals("LYRICS", true) ||
                    key.equals("UNSYNCEDLYRICS", true) ||
                    key.contains("LYRIC", true)
                ) {
                    if (value.isNotBlank()) return value
                }
            }

            offset += length
        }

        return null
    }

    private fun extractMp4Lyrics(bytes: ByteArray): String? {
        var result: String? = null

        fun readDataAtom(atomStart: Int, atomEnd: Int): String? {
            if (atomEnd - atomStart < 16) return null
            return decodeLikelyText(
                bytes.copyOfRange(atomStart + 16, atomEnd)
            )
        }

        fun walk(
            start: Int,
            end: Int,
            depth: Int,
            lyricContext: Boolean = false
        ) {
            if (result != null || depth > 16) return

            var offset = start
            while (offset + 8 <= end) {
                var atomSize =
                    int32(bytes, offset).toLong() and 0xFFFF_FFFFL
                val type = bytes.copyOfRange(offset + 4, offset + 8)
                    .toString(Charsets.ISO_8859_1)

                var headerSize = 8
                if (atomSize == 1L) {
                    if (offset + 16 > end) return
                    atomSize = long64(bytes, offset + 8)
                    headerSize = 16
                } else if (atomSize == 0L) {
                    atomSize = (end - offset).toLong()
                }

                val atomEndLong = offset.toLong() + atomSize
                if (
                    atomEndLong > end ||
                    atomEndLong <= offset + headerSize
                ) return

                val atomEnd = atomEndLong.toInt()

                when (type) {
                    "mdat", "free", "skip", "wide" -> Unit

                    "meta" -> {
                        if (atomEnd - offset - headerSize >= 4) {
                            walk(
                                offset + headerSize + 4,
                                atomEnd,
                                depth + 1,
                                lyricContext
                            )
                        }
                    }

                    "moov", "udta", "ilst", "----" -> {
                        walk(
                            offset + headerSize,
                            atomEnd,
                            depth + 1,
                            lyricContext || type == "----"
                        )
                    }

                    "©lyr" -> {
                        walk(
                            offset + headerSize,
                            atomEnd,
                            depth + 1,
                            true
                        )
                        if (result == null) {
                            result = readDataAtom(offset, atomEnd)
                        }
                    }

                    "data" -> {
                        val text = readDataAtom(offset, atomEnd)
                        if (
                            text != null &&
                            (
                                lyricContext ||
                                    text.contains("<tt", true) ||
                                    text.contains("[00:", true) ||
                                    text.contains("[0:", true)
                            )
                        ) {
                            result = text
                        }
                    }
                }

                offset = atomEnd
            }
        }

        walk(0, bytes.size, 0)
        return result ?: extractTaggedText(bytes, "LYRICS")
    }

    private fun findEmbeddedTtml(bytes: ByteArray): String? {
        val marker = "<tt".toByteArray(Charsets.UTF_8)
        val start = bytes.indexOfBytes(marker, 0, bytes.size)
        if (start < 0) return null

        val endMarker = "</tt>".toByteArray(Charsets.UTF_8)
        val end = bytes.indexOfBytes(
            endMarker,
            start,
            bytes.size
        )
        if (end < 0) return null

        return bytes.copyOfRange(
            start,
            end + endMarker.size
        ).toString(Charsets.UTF_8).trim()
    }

    private fun extractTaggedText(
        bytes: ByteArray,
        key: String
    ): String? {
        val keyBytes = (key + "=").toByteArray(Charsets.UTF_8)
        val start = bytes.indexOfBytes(
            keyBytes,
            0,
            bytes.size
        )
        if (start < 0) return null

        val valueStart = start + keyBytes.size
        var end = valueStart
        while (end < bytes.size && bytes[end].toInt() != 0) {
            end++
        }

        return bytes.copyOfRange(valueStart, end)
            .toString(Charsets.UTF_8)
            .trim()
            .takeIf { it.isNotBlank() }
    }

    private fun syncSafeInt(bytes: ByteArray, offset: Int): Int {
        if (offset + 4 > bytes.size) return 0
        return ((bytes[offset].toInt() and 0x7F) shl 21) or
            ((bytes[offset + 1].toInt() and 0x7F) shl 14) or
            ((bytes[offset + 2].toInt() and 0x7F) shl 7) or
            (bytes[offset + 3].toInt() and 0x7F)
    }

    private fun int32(bytes: ByteArray, offset: Int): Int {
        if (offset + 4 > bytes.size) return 0
        return ((bytes[offset].toInt() and 0xFF) shl 24) or
            ((bytes[offset + 1].toInt() and 0xFF) shl 16) or
            ((bytes[offset + 2].toInt() and 0xFF) shl 8) or
            (bytes[offset + 3].toInt() and 0xFF)
    }

    private fun long64(bytes: ByteArray, offset: Int): Long {
        if (offset + 8 > bytes.size) return 0L
        var value = 0L
        repeat(8) { index ->
            value = (value shl 8) or
                (bytes[offset + index].toLong() and 0xFF)
        }
        return value
    }

    private fun littleEndianInt(bytes: ByteArray, offset: Int): Int {
        if (offset + 4 > bytes.size) return 0
        return (bytes[offset].toInt() and 0xFF) or
            ((bytes[offset + 1].toInt() and 0xFF) shl 8) or
            ((bytes[offset + 2].toInt() and 0xFF) shl 16) or
            ((bytes[offset + 3].toInt() and 0xFF) shl 24)
    }

    private fun decodeLikelyText(bytes: ByteArray): String? {
        val candidates = listOf(
            Charsets.UTF_8,
            Charsets.UTF_16,
            Charsets.UTF_16BE,
            Charsets.ISO_8859_1
        )

        return candidates
            .mapNotNull { charset ->
                runCatching {
                    charset.decode(java.nio.ByteBuffer.wrap(bytes))
                        .toString()
                        .trim('\u0000', ' ', '\n', '\r', '\t')
                }.getOrNull()
            }
            .filter { it.isNotBlank() }
            .maxByOrNull { value ->
                value.count { ch ->
                    ch.isLetter() ||
                        ch.isWhitespace() ||
                        ch.isPunctuation()
                }
            }
    }

    private fun ByteArray.startsWithAscii(text: String): Boolean {
        val value = text.toByteArray(Charsets.US_ASCII)
        return size >= value.size &&
            copyOfRange(0, value.size).contentEquals(value)
    }

    private fun ByteArray.indexOfBytes(
        needle: ByteArray,
        from: Int,
        to: Int
    ): Int {
        if (needle.isEmpty() || to - from < needle.size) return -1

        outer@ for (i in from..(to - needle.size)) {
            for (j in needle.indices) {
                if (this[i + j] != needle[j]) continue@outer
            }
            return i
        }

        return -1
    }

    private fun loadEmbedded(context: Context, uriString: String): LyricsResult? {
        return try {
            val mediaItem = MediaItem.fromUri(Uri.parse(uriString))
            MetadataRetriever.Builder(context, mediaItem).build().use { retriever ->
                val groups = retriever.retrieveTrackGroups().get(8, TimeUnit.SECONDS)

                for (groupIndex in 0 until groups.length) {
                    val group = groups.get(groupIndex)

                    for (formatIndex in 0 until group.length) {
                        val metadata = group.getFormat(formatIndex).metadata ?: continue

                        for (entryIndex in 0 until metadata.length()) {
                            when (val entry = metadata.get(entryIndex)) {
                                is VorbisComment -> {
                                    val key = entry.key.uppercase()
                                    if (key == "LYRICS" || key == "UNSYNCEDLYRICS") {
                                        parseEmbeddedText(entry.value)?.let {
                                            return LyricsResult(it, "Embedded")
                                        }
                                    }
                                }

                                is BinaryFrame -> {
                                    when (entry.id.uppercase()) {
                                        "USLT" -> {
                                            parseUslt(entry.data)?.let {
                                                return LyricsResult(it, "Embedded")
                                            }
                                        }

                                        // SYLT is synchronized ID3 lyrics. It is
                                        // deliberately handled as plain text for
                                        // now when a parser cannot safely recover
                                        // every timestamp.
                                        "SYLT" -> {
                                            parseSyltAsLines(entry.data)?.let {
                                                return LyricsResult(it, "Embedded")
                                            }
                                        }
                                    }
                                }

                                is TextInformationFrame -> {
                                    // Some taggers expose lyrics through a
                                    // text-like custom frame. Only accept fields
                                    // whose id/description clearly indicate lyrics.
                                    val id = entry.id.uppercase()
                                    if (id.contains("LYRIC")) {
                                        entry.values.firstOrNull()?.let { value ->
                                            parseEmbeddedText(value)?.let {
                                                return LyricsResult(it, "Embedded")
                                            }
                                        }
                                    }
                                }

                                is MdtaMetadataEntry -> {
                                    val key = entry.key.lowercase()
                                    if (key.contains("lyr")) {
                                        val value = decodeMdtaText(entry.value)
                                        parseEmbeddedText(value)?.let {
                                            return LyricsResult(it, "Embedded")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun parseEmbeddedText(raw: String): List<LyricLine>? {
        val normalized = raw
            .replace("\r\n", "\n")
            .replace("\r", "\n")
            .trim()

        if (normalized.isBlank()) return null

        // Some local metadata may itself contain TTML XML. Support that
        // without making any network request.
        if (normalized.startsWith("<?xml", ignoreCase = true) ||
            normalized.contains("<tt", ignoreCase = true)
        ) {
            parseTtml(normalized)?.let { return it }
        }

        // Support embedded LRC as well as plain unsynchronized lyrics.
        val lrc = Regex(
            """\[(\d{1,2}):(\d{2})(?:[.:](\d{1,3}))?\]\s*(.*)"""
        )

        val parsed = normalized
            .lineSequence()
            .mapNotNull { line ->
                val match = lrc.matchEntire(line.trim()) ?: return@mapNotNull null
                val minutes = match.groupValues[1].toLong()
                val seconds = match.groupValues[2].toLong()
                val fraction = match.groupValues[3]
                    .padEnd(3, '0')
                    .take(3)
                    .toLongOrNull() ?: 0L
                val text = match.groupValues[4].trim()
                if (text.isBlank()) return@mapNotNull null

                LyricLine(
                    startMs = minutes * 60_000L + seconds * 1_000L + fraction,
                    endMs = Long.MAX_VALUE,
                    text = text
                )
            }
            .toList()

        if (parsed.isNotEmpty()) {
            return parsed
                .sortedBy { it.startMs }
                .mapIndexed { index, line ->
                    line.copy(
                        endMs = parsed.getOrNull(index + 1)?.startMs ?: Long.MAX_VALUE
                    )
                }
        }

        return normalized
            .lineSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .mapIndexed { index, text ->
                LyricLine(index * 4_000L, (index + 1) * 4_000L, text)
            }
            .toList()
            .takeIf { it.isNotEmpty() }
    }

    private fun parseUslt(data: ByteArray): List<LyricLine>? {
        if (data.size < 4) return null

        val encoding = data[0].toInt() and 0xFF
        val textCharset = when (encoding) {
            0 -> Charsets.ISO_8859_1
            1 -> Charsets.UTF_16
            2 -> Charsets.UTF_16BE
            3 -> Charsets.UTF_8
            else -> Charsets.UTF_8
        }

        var offset = 4 // encoding + ISO-639-2 language
        val terminatorSize = if (encoding == 1 || encoding == 2) 2 else 1

        while (offset + terminatorSize <= data.size) {
            val terminated = if (terminatorSize == 2) {
                data[offset].toInt() == 0 && data[offset + 1].toInt() == 0
            } else {
                data[offset].toInt() == 0
            }

            if (terminated) {
                offset += terminatorSize
                break
            }
            offset++
        }

        if (offset >= data.size) return null

        val lyricsText = runCatching {
            textCharset.decode(java.nio.ByteBuffer.wrap(data, offset, data.size - offset))
                .toString()
        }.getOrNull()?.trim().orEmpty()

        return parseEmbeddedText(lyricsText)
    }

    private fun parseSyltAsLines(data: ByteArray): List<LyricLine>? {
        if (data.size < 6) return null

        // SYLT's full structure is encoding + language + timestamp format +
        // content type + descriptor + repeated (text + timestamp). This parser
        // extracts the text entries conservatively and uses their timestamps
        // when they can be decoded without guessing the text encoding.
        val encoding = data[0].toInt() and 0xFF
        val charset = when (encoding) {
            0 -> Charsets.ISO_8859_1
            1 -> Charsets.UTF_16
            2 -> Charsets.UTF_16BE
            3 -> Charsets.UTF_8
            else -> Charsets.UTF_8
        }
        val wide = encoding == 1 || encoding == 2

        var offset = 6 // encoding + language(3) + timestamp format + content type
        val termSize = if (wide) 2 else 1

        while (offset + termSize <= data.size) {
            val end = findTerminator(data, offset, termSize)
            if (end >= 0) {
                offset = end + termSize
                break
            }
            break
        }

        val results = mutableListOf<LyricLine>()

        while (offset + termSize + 4 <= data.size) {
            val textEnd = findTerminator(data, offset, termSize)
            if (textEnd < 0) break

            val textBytes = data.copyOfRange(offset, textEnd)
            val text = runCatching { charset.decode(java.nio.ByteBuffer.wrap(textBytes)).toString() }
                .getOrNull()
                ?.trim()

            offset = textEnd + termSize
            if (offset + 4 > data.size || text.isNullOrBlank()) break

            val timestamp = java.nio.ByteBuffer.wrap(data, offset, 4)
                .order(java.nio.ByteOrder.BIG_ENDIAN)
                .int
                .toLong() * 1_000L
            offset += 4

            results += LyricLine(timestamp, Long.MAX_VALUE, text)
        }

        return results
            .sortedBy { it.startMs }
            .mapIndexed { index, line ->
                line.copy(endMs = results.getOrNull(index + 1)?.startMs ?: Long.MAX_VALUE)
            }
            .takeIf { it.isNotEmpty() }
    }

    private fun findTerminator(data: ByteArray, start: Int, size: Int): Int {
        if (size == 1) {
            for (i in start until data.size) {
                if (data[i].toInt() == 0) return i
            }
        } else {
            var i = start
            while (i + 1 < data.size) {
                if (data[i].toInt() == 0 && data[i + 1].toInt() == 0) return i
                i += 2
            }
        }
        return -1
    }

    private fun decodeMdtaText(bytes: ByteArray): String {
        val candidates = listOf(
            Charsets.UTF_8,
            Charsets.UTF_16,
            Charsets.ISO_8859_1
        )

        return candidates
            .map {
                runCatching {
                    it.decode(java.nio.ByteBuffer.wrap(bytes))
                        .toString()
                        .trim('\u0000', ' ', '\n', '\r', '\t')
                }.getOrDefault("")
            }
            .maxByOrNull { value ->
                value.count { ch -> ch.isLetter() || ch.isWhitespace() }
            }
            .orEmpty()
    }

    private fun parseTtml(raw: String): List<LyricLine>? {
        val factory = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
            runCatching { setFeature("http://apache.org/xml/features/disallow-doctype-decl", true) }
            runCatching { setFeature("http://xml.org/sax/features/external-general-entities", false) }
            runCatching { setFeature("http://xml.org/sax/features/external-parameter-entities", false) }
        }

        val document = runCatching {
            factory.newDocumentBuilder().parse(InputSource(StringReader(raw)))
        }.getOrNull() ?: return null

        val nodes = document.getElementsByTagNameNS("*", "p")
        val result = buildList {
            for (index in 0 until nodes.length) {
                val element = nodes.item(index) as? Element ?: continue
                val begin = parseTimeMs(element.getAttribute("begin")) ?: continue
                val end = parseTimeMs(element.getAttribute("end"))
                    ?: (parseTimeMs(element.getAttribute("dur"))?.let { begin + it })
                    ?: Long.MAX_VALUE

                val text = element.textContent
                    .replace("\r", "")
                    .replace("\n", " ")
                    .replace(Regex("\\s+"), " ")
                    .trim()

                if (text.isNotBlank()) {
                    add(LyricLine(begin, end, text))
                }
            }
        }

        return result
            .sortedBy { it.startMs }
            .mapIndexed { index, line ->
                if (line.endMs == Long.MAX_VALUE) {
                    line.copy(endMs = result.getOrNull(index + 1)?.startMs ?: Long.MAX_VALUE)
                } else {
                    line
                }
            }
            .takeIf { it.isNotEmpty() }
    }

    private fun parseTimeMs(value: String?): Long? {
        val text = value?.trim().orEmpty()
        if (text.isBlank()) return null

        text.toLongOrNull()?.let { return it }

        val milliseconds = Regex("""^(\d+)(?:\.(\d+))?ms$""", RegexOption.IGNORE_CASE)
            .matchEntire(text)
        if (milliseconds != null) {
            val base = milliseconds.groupValues[1].toLong()
            return base + milliseconds.groupValues[2].padEnd(3, '0').take(3).toLongOrNull().orZero()
        }

        val seconds = Regex("""^(\d+(?:\.\d+)?)s$""", RegexOption.IGNORE_CASE)
            .matchEntire(text)
        if (seconds != null) {
            return (seconds.groupValues[1].toDouble() * 1000.0).toLong()
        }

        val clock = Regex("""^(?:(\d+):)?(\d{1,2}):(\d{2})(?:\.(\d+))?$""")
            .matchEntire(text) ?: return null

        val hours = clock.groupValues[1].ifBlank { "0" }.toLong()
        val minutes = clock.groupValues[2].toLong()
        val wholeSeconds = clock.groupValues[3].toLong()
        val fraction = clock.groupValues[4].padEnd(3, '0').take(3).toLongOrNull().orZero()

        return hours * 3_600_000L +
            minutes * 60_000L +
            wholeSeconds * 1_000L +
            fraction
    }

    private fun Long?.orZero(): Long = this ?: 0L
}
