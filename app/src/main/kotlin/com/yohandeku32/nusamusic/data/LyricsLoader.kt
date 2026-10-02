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
        loadEmbedded(context, song.uri)
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
            .map { runCatching { it.decode(bytes).toString().trim(' ', ' ', '\n', '\r', '\t') }.getOrDefault("") }
            .maxByOrNull { it.count { ch -> ch.isLetter() || ch.isWhitespace() } }
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
