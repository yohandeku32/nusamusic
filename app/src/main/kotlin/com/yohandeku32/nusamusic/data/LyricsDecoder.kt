package com.yohandeku32.nusamusic.data

import com.mocharealm.accompanist.lyrics.core.model.ISyncedLine
import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine
import com.mocharealm.accompanist.lyrics.core.model.synced.SyncedLine
import com.mocharealm.accompanist.lyrics.core.parser.EnhancedLrcParser
import com.mocharealm.accompanist.lyrics.core.parser.TTMLParser
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.xml.sax.InputSource
import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory

/** Sidecar evidence for decoration only; imported playback intervals stay intact. */
data class LyricsUnitKey(val start: Int, val content: String)
data class DecodedLyrics(val lyrics: SyncedLyrics, val inferredEnds: Set<LyricsUnitKey>)

/** Adapts common local-file dialects to Accompanist's timed model. */
object LyricsDecoder {
    const val MAX_BYTES = 512 * 1024
    private val stamp = Regex("\\[(\\d{1,3}):([0-5]\\d)(?:[.:](\\d{1,3}))?]")
    private val wordStamp = Regex("<(\\d{1,3}):([0-5]\\d)(?:[.:](\\d{1,3}))?>")
    private val offsetTag = Regex("\\[offset:([+-]?\\d+)]", RegexOption.IGNORE_CASE)

    fun decode(raw: String, durationMs: Long): SyncedLyrics = decodeWithTiming(raw, durationMs).lyrics

    fun decodeWithTiming(raw: String, durationMs: Long): DecodedLyrics {
        require(raw.toByteArray(Charsets.UTF_8).size <= MAX_BYTES) { "Lyrics file is too large" }
        val content = raw.trimStart('\uFEFF').trim()
        val duration = durationMs.coerceIn(1L, Int.MAX_VALUE.toLong()).toInt()
        val inferredEnds = mutableSetOf<LyricsUnitKey>()
        val parsed = if (content.startsWith('<')) {
            val normalized = normalizeTtml(content, duration, inferredEnds)
            val ttml = TTMLParser.parse(normalized.first)
            ttml.copy(lines = ttml.lines.map { line ->
                if (line is KaraokeLine && (line.start to line.syllables.joinToString("") { it.content }.trim()) in normalized.second)
                    SyncedLine(line.syllables.joinToString("") { it.content }, line.translation, line.start, line.end)
                else line
            })
        } else {
            parseLrc(content, duration, inferredEnds)
        }
        val lines = parsed.lines.sortedBy { it.start }.filter { line ->
            when (line) {
                is SyncedLine -> line.content.isNotBlank()
                is KaraokeLine -> line.syllables.any { it.content.isNotBlank() }
                else -> false
            }
        }.map { line ->
            fun start(time: Int) = time.coerceIn(0, Int.MAX_VALUE - 1)
            when (line) {
                is KaraokeLine -> {
                    val syllables = line.syllables.map { syllable ->
                        val begin = start(syllable.start)
                        syllable.copy(start = begin, end = syllable.end.coerceAtLeast(begin + 1))
                    }.dropWhile { it.content.isBlank() }.dropLastWhile { it.content.isBlank() }
                    val sung = syllables.filter { it.content.isNotBlank() }
                    val begin = sung.minOf { it.start }
                    // Container and whitespace timing can include an instrumental passage.
                    line.copy(start = begin, end = sung.maxOf { it.end }.coerceAtLeast(begin + 1), syllables = syllables)
                }
                is SyncedLine -> line.copy(start = start(line.start), end = line.end.coerceAtLeast(start(line.start) + 1))
                else -> line
            }
        }.sortedBy { it.start }
        require(lines.isNotEmpty() && lines.size <= 8_000) { "No supported timed lyrics" }
        return DecodedLyrics(parsed.copy(lines = lines), inferredEnds.toSet())
    }

    private fun time(match: MatchResult): Int = (
        match.groupValues[1].toLong() * 60_000 + match.groupValues[2].toLong() * 1_000 +
            match.groupValues[3].padEnd(3, '0').toLong()
        ).coerceAtMost(Int.MAX_VALUE.toLong() - 1).toInt()

    private fun parseLrc(content: String, duration: Int, inferredEnds: MutableSet<LyricsUnitKey>): SyncedLyrics {
        val offset = (offsetTag.find(content)?.groupValues?.get(1)?.toLongOrNull() ?: 0L)
            .coerceIn(-Int.MAX_VALUE.toLong(), Int.MAX_VALUE.toLong())
        val entries = content.lineSequence().flatMap { rawLine ->
            val line = rawLine.trim()
            val tags = stamp.findAll(line).toList()
            if (tags.isEmpty() || tags.first().range.first != 0) emptySequence() else {
                val end = tags.last().range.last + 1
                // Only consecutive leading timestamps repeat a lyric line.
                val prefix = line.substring(0, end).replace(stamp, "").trim()
                if (prefix.isNotEmpty()) emptySequence() else tags.asSequence().map {
                    time(it) to line.substring(end).trim()
                }
            }
        }.sortedBy { it.first }.toList()
        require(entries.size <= 8_000)
        val grouped = entries.groupBy { it.first }.entries.toList()
        val result = ArrayList<ISyncedLine>()
        grouped.forEachIndexed { index, (start, texts) ->
            val end = (grouped.getOrNull(index + 1)?.key ?: duration.coerceAtLeast(start + 1))
                .coerceAtLeast(start + 1)
            val text = texts.first().second
            if (text.isBlank()) return@forEachIndexed
            val translation = texts.drop(1).map { it.second }.filter { it.isNotBlank() }
                .joinToString("\n").ifBlank { null }
            // Normalize whole-second and one-digit fractional tags for the upstream parser.
            val wordTags = wordStamp.findAll(text).toList()
            val normalizedText = text.replace(wordStamp) { "<${clock(time(it))}>" }
            val enhanced = if (wordTags.isNotEmpty()) EnhancedLrcParser.parse(listOf("[${clock(start)}]$normalizedText"))
                .lines.firstOrNull() as? KaraokeLine else null
            val line = if (enhanced != null) {
                val terminal = wordTags.last().takeIf { it.range.last == text.lastIndex }?.let(::time)
                val lastWord = enhanced.syllables.last()
                val inferred = end
                val wordEnd = (terminal ?: inferred).coerceAtLeast(lastWord.start + 1)
                if (terminal == null) {
                    val shiftedStart = (lastWord.start.toLong() - offset).coerceIn(0, Int.MAX_VALUE - 1L).toInt()
                    inferredEnds += LyricsUnitKey(shiftedStart, lastWord.content.trim())
                }
                enhanced.copy(
                    end = wordEnd,
                    syllables = enhanced.syllables.mapIndexed { i, syllable ->
                        val syllableEnd = enhanced.syllables.getOrNull(i + 1)?.start ?: wordEnd
                        syllable.copy(end = syllableEnd.coerceAtLeast(syllable.start + 1))
                    }, translation = translation
                )
            } else SyncedLine(text, translation, start, end)
            result += shift(line, offset)
        }
        return SyncedLyrics(result)
    }

    private fun shift(line: ISyncedLine, offset: Long): ISyncedLine {
        // LRC positive offset means the lyrics appear earlier.
        fun adjusted(time: Int) = (time.toLong() - offset).coerceIn(0, Int.MAX_VALUE.toLong()).toInt()
        return when (line) {
            is SyncedLine -> line.copy(start = adjusted(line.start), end = adjusted(line.end))
            is KaraokeLine -> line.copy(start = adjusted(line.start), end = adjusted(line.end),
                syllables = line.syllables.map { it.copy(start = adjusted(it.start), end = adjusted(it.end)) })
            else -> line
        }
    }

    private fun clock(ms: Int): String = "%02d:%02d.%03d".format(java.util.Locale.ROOT, ms / 60_000, ms / 1_000 % 60, ms % 1_000)

    private fun ttmlTime(value: String): Int {
        val unit = Regex("^(\\d+(?:\\.\\d+)?)(ms|s|m|h)$").matchEntire(value)
        val millis = if (unit != null) {
            unit.groupValues[1].toDouble() * when (unit.groupValues[2]) {
                "ms" -> 1; "s" -> 1_000; "m" -> 60_000; else -> 3_600_000
            }
        } else {
            val parts = value.split(':')
            require(parts.size in 2..3)
            parts.fold(0.0) { total, part -> total * 60 + part.toDouble() } * 1_000
        }
        require(millis.isFinite() && millis >= 0 && millis < Int.MAX_VALUE)
        return millis.toInt()
    }

    /** Handle seconds/duration notation and line-timed TTML, which older Core omits. */
    private fun normalizeTtml(content: String, duration: Int, inferredEnds: MutableSet<LyricsUnitKey>): Pair<String, Set<Pair<Int, String>>> {
        require(!content.contains("<!DOCTYPE", ignoreCase = true) && !content.contains("<!ENTITY", ignoreCase = true))
        val factory = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
        val builder = factory.newDocumentBuilder().apply {
            setEntityResolver { _, _ -> InputSource(StringReader("")) }
            setErrorHandler(object : org.xml.sax.helpers.DefaultHandler() {
                override fun error(e: org.xml.sax.SAXParseException) { throw e }
                override fun fatalError(e: org.xml.sax.SAXParseException) { throw e }
            })
        }
        val root = builder.parse(InputSource(StringReader(content))).documentElement
        require(root.localName == "tt" || root.nodeName == "tt")
        require(Regex("\\bbegin\\s*=").containsMatchIn(content)) { "TTML has no timed lyrics" }
        val lineOnly = mutableSetOf<Pair<Int, String>>()
        var nodes = 0
        fun escaped(text: String) = text.replace("&", "&amp;").replace("<", "&lt;")
            .replace(">", "&gt;").replace("\"", "&quot;")
        fun render(node: Node, parentStart: Int, parentEnd: Int, depth: Int): String {
            require(depth <= 64 && ++nodes <= 20_000)
            if (node !is Element) return if (node.nodeType == Node.TEXT_NODE || node.nodeType == Node.CDATA_SECTION_NODE) escaped(node.nodeValue.orEmpty()) else ""
            val name = node.localName ?: node.nodeName
            val start = node.getAttribute("begin").takeIf { it.isNotBlank() }?.let(::ttmlTime) ?: parentStart
            val end = node.getAttribute("end").takeIf { it.isNotBlank() }?.let(::ttmlTime)
                ?: node.getAttribute("dur").takeIf { it.isNotBlank() }?.let { (start.toLong() + ttmlTime(it)).coerceAtMost(Int.MAX_VALUE.toLong()).toInt() }
                ?: parentEnd
            require(end >= start)
            if (name == "span" && !node.hasAttribute("end") && !node.hasAttribute("dur")) {
                inferredEnds += LyricsUnitKey(start.coerceIn(0, Int.MAX_VALUE - 1), node.textContent.trim())
            }
            val attributes = buildString {
                for (i in 0 until node.attributes.length) {
                    val attr = node.attributes.item(i)
                    if (attr.nodeName !in setOf("begin", "end", "dur")) append(" ${attr.nodeName}=\"${escaped(attr.nodeValue)}\"")
                }
                if (name in setOf("p", "span")) append(" begin=\"${clock(start)}\" end=\"${clock(end)}\"")
            }
            var children = buildString {
                for (i in 0 until node.childNodes.length) append(render(node.childNodes.item(i), start, end, depth + 1))
            }
            if (name == "p") {
                val spans = node.getElementsByTagName("*")
                val hasTimedUnit = (0 until spans.length).any { index ->
                    val element = spans.item(index) as Element
                    (element.localName ?: element.nodeName) == "span" && element.hasAttribute("begin")
                }
                if (!hasTimedUnit) lineOnly += start to node.textContent.trim()
            }
            if (name == "p" && (0 until node.childNodes.length).none { node.childNodes.item(it) is Element }) {
                children = "<span begin=\"${clock(start)}\" end=\"${clock(end)}\">$children</span>"
            }
            return "<$name$attributes>$children</$name>"
        }
        val rendered = render(root, 0, duration, 0)
        return rendered to lineOnly
    }
}
