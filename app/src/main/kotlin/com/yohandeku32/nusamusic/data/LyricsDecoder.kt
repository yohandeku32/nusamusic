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
data class DecodedLyrics(
    val lyrics: SyncedLyrics,
    val inferredEnds: Set<LyricsUnitKey>,
    /** Indices into lyrics.lines whose explicitly tagged duet singer belongs on the right. */
    val duetRightLines: Set<Int> = emptySet()
)

private data class TtmlVoiceEntry(
    val start: Int, val isAccompaniment: Boolean, val agentId: String,
    val explicitRight: Boolean? = null
)
private data class NormalizedTtml(
    val xml: String,
    val lineOnly: Set<Pair<Int, String>>,
    val voices: List<TtmlVoiceEntry>,
    val declaredAgents: List<String>,
    val nonSingerAgents: Set<String>
)

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
        var taggedVoices: List<TtmlVoiceEntry> = emptyList()
        var declaredAgents: List<String> = emptyList()
        var nonSingerAgents: Set<String> = emptySet()
        val parsed = if (content.startsWith('<')) {
            val normalized = normalizeTtml(content, duration, inferredEnds)
            taggedVoices = normalized.voices
            declaredAgents = normalized.declaredAgents
            nonSingerAgents = normalized.nonSingerAgents
            val ttml = TTMLParser.parse(normalized.xml)
            ttml.copy(lines = ttml.lines.map { line ->
                if (line is KaraokeLine && (line.start to line.syllables.joinToString("") { it.content }.trim()) in normalized.lineOnly)
                    SyncedLine(line.syllables.joinToString("") { it.content }, line.translation, line.start, line.end)
                else line
            })
        } else {
            parseLrc(content, duration, inferredEnds)
        }
        // Bind source rows before trimming whitespace or replacing container
        // onsets with sung onsets. Core keeps source order for equal starts.
        val voicesByStart = taggedVoices.groupBy { it.start to it.isAccompaniment }
            .mapValues { (_, voices) -> ArrayDeque(voices) }
        val voiceByLine = java.util.IdentityHashMap<ISyncedLine, TtmlVoiceEntry>()
        parsed.lines.forEach { line ->
            voicesByStart[line.start to ((line as? KaraokeLine)?.isAccompaniment == true)]
                ?.removeFirstOrNull()?.let { voiceByLine[line] = it }
        }
        val sourceVoices = ArrayList<TtmlVoiceEntry?>()
        val lines = parsed.lines.sortedBy { it.start }.filter { line ->
            when (line) {
                is SyncedLine -> line.content.isNotBlank()
                is KaraokeLine -> line.syllables.any { it.content.isNotBlank() }
                else -> false
            }
        }.map { line ->
            fun start(time: Int) = time.coerceIn(0, Int.MAX_VALUE - 1)
            val normalizedLine = when (line) {
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
            sourceVoices += voiceByLine[line]
            normalizedLine
        }
        val sortedIndices = lines.indices.sortedBy { lines[it].start }
        val sortedLines = sortedIndices.map { lines[it] }
        require(lines.isNotEmpty() && lines.size <= 8_000) { "No supported timed lyrics" }

        // Apple lyric TTML is allowed to declare MANY voices. Its metadata
        // may interleave "other" pseudo-voices (e.g. v2000) with people:
        // v1, v2000 [type=other], v2, v3, v4, v5, v6.
        // Counting the second <ttm:agent> as the right singer incorrectly
        // maps v2000 -> right and every actual person v2..v6 -> left.
        //
        // For Apple's numbered *person* voices, odd IDs occupy the left lane
        // and even IDs the right lane. Generic named people alternate in their
        // declared order. A supplied tts:textAlign always takes precedence.
        // Both main and backing vocals use this same side metadata.
        val singerIds = (
            declaredAgents.filterNot { it in nonSingerAgents } +
                taggedVoices.map { it.agentId }
                    .filter { it.isNotBlank() && it !in nonSingerAgents }
                    .distinct().sortedWith(
                        compareBy<String> {
                            Regex("(\\d+)$").find(it)?.value?.toIntOrNull()
                                ?: Int.MAX_VALUE
                        }.thenBy { it }
                    )
        ).distinct()
        val numberedVoice = Regex("^v([1-9][0-9]*)$", RegexOption.IGNORE_CASE)
        fun isRightVoice(voice: TtmlVoiceEntry): Boolean {
            voice.explicitRight?.let { return it }
            if (voice.agentId.isBlank() || voice.agentId in nonSingerAgents) return false
            val number = numberedVoice.matchEntire(voice.agentId)
                ?.groupValues?.get(1)?.toIntOrNull()
            if (number != null && number in 1..99) return number % 2 == 0
            val declaredIndex = singerIds.indexOf(voice.agentId)
            return declaredIndex >= 0 && declaredIndex % 2 == 1
        }
        val duetRight = buildSet {
            sortedIndices.forEachIndexed { index, sourceIndex ->
                sourceVoices[sourceIndex]?.let { if (isRightVoice(it)) add(index) }
            }
        }
        return DecodedLyrics(parsed.copy(lines = sortedLines), inferredEnds.toSet(), duetRight)
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
        val time = value.trim()
        val unit = Regex("^(\\d+(?:\\.\\d+)?)(ms|s|m|h)$").matchEntire(time)
        val millis = when {
            unit != null -> unit.groupValues[1].toDouble() * when (unit.groupValues[2]) {
                "ms" -> 1; "s" -> 1_000; "m" -> 60_000; else -> 3_600_000
            }
            // Apple Music exports iTunes lyric TTML using bare decimal seconds:
            // begin="7.208", end="7.446". These are valid for the music
            // source we import, but require an explicit interpretation here.
            Regex("^\\d+(?:\\.\\d+)?$").matches(time) -> time.toDouble() * 1_000
            else -> {
                val parts = time.split(':')
                require(parts.size in 2..3)
                parts.fold(0.0) { total, part -> total * 60 + part.toDouble() } * 1_000
            }
        }
        require(millis.isFinite() && millis >= 0 && millis < Int.MAX_VALUE)
        // Avoid losing a millisecond to floating-point representation when
        // converting sample offsets such as 7.208 -> 7208 ms.
        return kotlin.math.round(millis).toInt()
    }

    /** Handle seconds/duration notation and line-timed TTML, which older Core omits. */
    private fun normalizeTtml(content: String, duration: Int, inferredEnds: MutableSet<LyricsUnitKey>): NormalizedTtml {
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
        val taggedVoices = ArrayList<TtmlVoiceEntry>()
        // Namespaced tts:textAlign may be placed directly on p/span or
        // declared by a referenced <style xml:id="..."> element.
        val styleElements = HashMap<String, Element>()
        val regionElements = HashMap<String, Element>()
        val allElements = root.getElementsByTagName("*")
        for (i in 0 until allElements.length) {
            val el = allElements.item(i) as? Element ?: continue
            if (el.localName != "style" && el.localName != "region") continue
            val id = el.getAttribute("xml:id").ifBlank { el.getAttribute("id") }
            if (id.isNotBlank()) {
                if (el.localName == "style") styleElements[id.trim()] = el
                else regionElements[id.trim()] = el
            }
        }
        fun explicitRight(element: Element): Boolean? {
            val seen = HashSet<String>()
            fun value(el: Element): Boolean? {
                val attrs = el.attributes
                for (i in 0 until attrs.length) {
                    val attr = attrs.item(i)
                    if ((attr.localName ?: attr.nodeName.substringAfter(':'))
                            .equals("textAlign", ignoreCase = true)) {
                        when (attr.nodeValue.trim().lowercase()) {
                            "right", "end" -> return true
                            "left", "start" -> return false
                        }
                    }
                }
                val refs = el.getAttribute("style").trim().split(Regex("\\s+"))
                for (ref in refs.asReversed()) {
                    val target = styleElements[ref] ?: continue
                    if (seen.add(ref)) value(target)?.let { return it }
                }
                // A right-aligned duet lyric may be positioned by a TTML
                // region instead of an inline style. Resolve its reference
                // and any style linked from that region.
                val regionId = el.getAttribute("region").trim()
                if (regionId.isNotBlank()) {
                    val region = regionElements[regionId]
                    if (region != null && seen.add("region:$regionId")) {
                        value(region)?.let { return it }
                    }
                }
                return null
            }
            var node: Node? = element
            while (node is Element) {
                value(node)?.let { return it }
                node = node.parentNode
            }
            return null
        }
        // Metadata declares the singer identities independently of who
        // happens to sing first. Keep its order stable through sorted lines.
        val nonSingerAgents = HashSet<String>()
        val declaredAgents = buildList {
            val children = root.getElementsByTagName("*")
            for (index in 0 until children.length) {
                val child = children.item(index) as? Element ?: continue
                if (child.localName != "agent" ||
                    child.namespaceURI != "http://www.w3.org/ns/ttml#metadata"
                ) continue
                val id = child.getAttribute("xml:id").ifBlank {
                    child.getAttribute("id")
                }.trim().removePrefix("#")
                if (id.isBlank()) continue
                // Apple emits e.g. <ttm:agent type="other" xml:id="v2000"/>
                // which is not a primary singer and cannot occupy a duet lane.
                if (child.getAttribute("type").trim().equals("other", ignoreCase = true)) {
                    nonSingerAgents += id
                } else if (id !in this) add(id)
            }
        }
        fun isBacking(element: Element): Boolean {
            val attrs = element.attributes
            for (i in 0 until attrs.length) {
                val attribute = attrs.item(i)
                val name = (attribute.localName ?: attribute.nodeName.substringAfter(':')).lowercase()
                if (name == "role" &&
                    attribute.nodeValue.trim().lowercase() in
                        setOf("x-bg", "bg", "background", "backing")
                ) return true
            }
            return false
        }

        fun withinBacking(node: Node, parent: Element): Boolean {
            var cursor: Node? = node
            while (cursor is Element && cursor !== parent) {
                if (isBacking(cursor)) return true
                cursor = cursor.parentNode
            }
            return false
        }

        // Keep nested backing vocals out of line-only content classification.
        fun mainText(node: Node): String = when {
            node is Element && isBacking(node) -> ""
            node.nodeType == Node.TEXT_NODE || node.nodeType == Node.CDATA_SECTION_NODE ->
                node.nodeValue.orEmpty()
            else -> buildString {
                for (i in 0 until node.childNodes.length) {
                    append(mainText(node.childNodes.item(i)))
                }
            }
        }

        fun hasSungSpans(container: Element): Boolean =
            (0 until container.childNodes.length).any { i ->
                val span = container.childNodes.item(i) as? Element
                    ?: return@any false
                val hasExcludedRole = (0 until span.attributes.length).any { j ->
                    val attr = span.attributes.item(j)
                    attr.nodeName.endsWith(":role") &&
                        attr.nodeValue in setOf("x-bg", "x-translation")
                }
                span.localName == "span" &&
                    span.textContent.replace("  ", "").isNotEmpty() && !hasExcludedRole
            }

        // Resolve the nearest TTML metadata ttm:agent, plus commonly exported
        // speaker/voice attributes. A one-singer TTML remains left aligned.
        fun speaker(element: Element): String {
            // Explicit paragraph voice has priority over an inherited voice.
            var cursor: Node? = element
            while (cursor is Element) {
                val attrs = cursor.attributes
                for (i in 0 until attrs.length) {
                    val attr = attrs.item(i)
                    val name = (attr.localName ?: attr.nodeName.substringAfter(':')).lowercase()
                    if (name in setOf("agent", "voice", "speaker") &&
                        attr.nodeValue.isNotBlank()
                    ) return attr.nodeValue.trim().removePrefix("#")
                }
                cursor = cursor.parentNode
            }
            // Some word-timed TTML tags only label their spans. Treat the
            // paragraph as one singer only if all its tagged spans agree.
            val speakers = mutableSetOf<String>()
            val spans = element.getElementsByTagName("*")
            for (i in 0 until spans.length) {
                val span = spans.item(i) as? Element ?: continue
                if (span.localName != "span" || withinBacking(span, element)) continue
                val attrs = span.attributes
                for (j in 0 until attrs.length) {
                    val attr = attrs.item(j)
                    val name = (attr.localName ?: attr.nodeName.substringAfter(':')).lowercase()
                    if (name in setOf("agent", "voice", "speaker") &&
                        attr.nodeValue.isNotBlank()
                    ) speakers += attr.nodeValue.trim().removePrefix("#")
                }
            }
            return speakers.singleOrNull().orEmpty()
        }
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
            if (name == "span" && isBacking(node) &&
                (node.parentNode as? Element)?.localName == "p" && hasSungSpans(node)) {
                val voiceId = speaker(node)
                val right = explicitRight(node)
                taggedVoices += TtmlVoiceEntry(start, true, voiceId, right)
            }
            if (name == "p") {
                val spans = node.getElementsByTagName("*")
                val voiceId = speaker(node)
                val leadText = mainText(node).trim()
                // A few providers put the positioning style on each word
                // span instead of its enclosing paragraph. Respect it only
                // when all explicitly aligned main-vocal spans agree.
                val right = explicitRight(node) ?: buildSet {
                    for (i in 0 until spans.length) {
                        val span = spans.item(i) as? Element ?: continue
                        if (span.localName != "span" || span.textContent.isBlank() ||
                            withinBacking(span, node)) continue
                        explicitRight(span)?.let { add(it) }
                    }
                }.singleOrNull()
                val plainParagraph = (0 until node.childNodes.length).none {
                    node.childNodes.item(it) is Element
                }
                if (hasSungSpans(node) || (plainParagraph && leadText.isNotEmpty())) {
                    taggedVoices += TtmlVoiceEntry(start, false, voiceId, right)
                }
                val hasTimedUnit = (0 until spans.length).any { index ->
                    val element = spans.item(index) as Element
                    (element.localName ?: element.nodeName) == "span" && element.hasAttribute("begin")
                }
                if (!hasTimedUnit) lineOnly += start to leadText
            }
            if (name == "p" && (0 until node.childNodes.length).none { node.childNodes.item(it) is Element }) {
                children = "<span begin=\"${clock(start)}\" end=\"${clock(end)}\">$children</span>"
            }
            return "<$name$attributes>$children</$name>"
        }
        val rendered = render(root, 0, duration, 0)
        return NormalizedTtml(rendered, lineOnly, taggedVoices, declaredAgents, nonSingerAgents)
    }
}
