package com.yohandeku32.nusamusic.data

import org.w3c.dom.Element
import org.w3c.dom.Node

data class LyricsPronunciation(
    val text: String,
    val words: List<LyricsPronunciationWord> = emptyList()
)

data class LyricsPronunciationWord(val range: IntRange, val start: Int, val end: Int)

internal data class TtmlCompanion(
    val translation: String? = null,
    val pronunciation: LyricsPronunciation? = null
)

/** Companion metadata is keyed to source rows, never guessed from playback time. */
internal class TtmlCompanionReader(root: Element, private val time: (String) -> Int) {
    private val translations = linkedMapOf<String, String>()
    private val pronunciations = linkedMapOf<String, LyricsPronunciation>()

    init {
        val metadata = root.getElementsByTagNameNS("*", "metadata")
        for (i in 0 until metadata.length) {
            val entries = (metadata.item(i) as Element).getElementsByTagName("*")
            for (j in 0 until entries.length) {
                val variant = entries.item(j) as Element
                if (variant.localName !in setOf("translation", "transliteration")) continue
                for (entry in variant.children()) {
                    if (entry.localName != "text") continue
                    val key = entry.getAttribute("for").trim().removePrefix("#")
                    if (key.isEmpty()) continue
                    if (variant.localName == "translation") {
                        text(entry).takeIf { it.isNotBlank() }?.let { translations.putIfAbsent(key, it) }
                    } else pronunciation(entry)?.let { pronunciations.putIfAbsent(key, it) }
                }
            }
        }
    }

    fun read(element: Element, parent: Element? = null): TtmlCompanion {
        val inline = element.children().filter { isCompanion(it) }
        val ownKey = key(element)
        val inheritedKey = parent?.let(::key).orEmpty()
        val rawTranslation = translations[ownKey] ?: translations[inheritedKey]
        // Some exports attach a backing translation in full-width brackets
        // to the main line's key instead of giving the backing its own key.
        val bracket = rawTranslation?.let { Regex("^(.*?)（(.*?)）$", RegexOption.DOT_MATCHES_ALL).matchEntire(it) }
        val metadataTranslation = if (parent != null) {
            bracket?.groupValues?.get(2) ?: rawTranslation
        } else bracket?.groupValues?.get(1) ?: rawTranslation
        return TtmlCompanion(
            translation = inline.firstOrNull { role(it) == "x-translation" }?.let { text(it) }
                ?.takeIf { it.isNotBlank() } ?: metadataTranslation?.trim()?.takeIf { it.isNotBlank() },
            pronunciation = inline.firstOrNull { role(it) in pronunciationRoles }?.let(::pronunciation)
                ?: pronunciations[ownKey]
        )
    }

    private fun pronunciation(element: Element): LyricsPronunciation? {
        val parts = element.children().filter { it.localName == "span" }
        if (parts.isEmpty()) return text(element).takeIf { it.isNotBlank() }?.let { LyricsPronunciation(it) }
        // Timed pinyin exports often omit spaces between adjacent spans.
        // Keep readable word separation without assigning invented timestamps.
        val segments = ArrayList<LyricsPronunciationWord>()
        val raw = buildString {
            for (i in 0 until element.childNodes.length) {
                val node = element.childNodes.item(i)
                if (node is Element && node.localName == "span") {
                    val words = text(node)
                    if (words.isBlank()) continue
                    if (isNotEmpty() && !last().isWhitespace()) append(' ')
                    val begin = runCatching { time(node.getAttribute("begin")) }.getOrNull()
                    val end = runCatching {
                        node.getAttribute("end").takeIf { it.isNotBlank() }?.let(time)
                            ?: node.getAttribute("dur").takeIf { it.isNotBlank() }?.let {
                                Math.addExact(requireNotNull(begin), time(it))
                            }
                    }.getOrNull()
                    val offset = length
                    append(words)
                    if (begin != null && end != null && end > begin)
                        segments += LyricsPronunciationWord(offset until length, begin, end)
                } else if (node.nodeType == Node.TEXT_NODE || node.nodeType == Node.CDATA_SECTION_NODE) {
                    append(node.nodeValue.orEmpty().replace(Regex("\\s+"), " "))
                }
            }
        }
        val leadingSpace = raw.indexOfFirst { !it.isWhitespace() }.coerceAtLeast(0)
        return raw.trim().takeIf { it.isNotBlank() }?.let { value ->
            LyricsPronunciation(value, segments.map { word ->
                word.copy(range = (word.range.first - leadingSpace)..(word.range.last - leadingSpace))
            })
        }
    }

    companion object {
        private val pronunciationRoles = setOf("x-roman", "x-romanization", "x-transliteration")

        fun isCompanion(element: Element) = role(element) == "x-translation" || role(element) in pronunciationRoles

        private fun role(element: Element): String = (0 until element.attributes.length)
            .map { element.attributes.item(it) }
            .firstOrNull { (it.localName ?: it.nodeName) == "role" }?.nodeValue.orEmpty().trim().lowercase()

        private fun key(element: Element): String = (0 until element.attributes.length)
            .map { element.attributes.item(it) }
            .firstOrNull { (it.localName ?: it.nodeName) == "key" }?.nodeValue.orEmpty()
            .ifBlank { element.getAttribute("xml:id").ifBlank { element.getAttribute("id") } }.trim().removePrefix("#")

        private fun text(node: Node, depth: Int = 0): String {
            require(depth <= 64)
            if (node.nodeType == Node.TEXT_NODE || node.nodeType == Node.CDATA_SECTION_NODE) return node.nodeValue.orEmpty()
            if (node is Element && node.localName == "br") return "\n"
            return buildString {
                for (i in 0 until node.childNodes.length) append(text(node.childNodes.item(i), depth + 1))
            }.let { if (depth == 0) it.trim() else it }
        }

        private fun Element.children(): List<Element> = (0 until childNodes.length).mapNotNull { childNodes.item(it) as? Element }
    }
}
