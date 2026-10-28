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
    private val pronunciations = linkedMapOf<String, Element>()

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
                    } else if (text(entry).isNotBlank()) pronunciations.putIfAbsent(key, entry)
                }
            }
        }
    }

    fun read(element: Element, parent: Element? = null): TtmlCompanion {
        val inline = element.children().filter { isCompanion(it) }
        val separateSyllables = hasCjkSource(element)
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
            pronunciation = inline.firstOrNull { role(it) in pronunciationRoles }?.let { pronunciation(it, separateSyllables) }
                ?: pronunciations[ownKey]?.let { pronunciation(it, separateSyllables) }
        )
    }

    private fun pronunciation(element: Element, separateSyllables: Boolean): LyricsPronunciation? {
        val parts = element.children().filter { it.localName == "span" }
        if (parts.isEmpty()) return text(element).takeIf { it.isNotBlank() }?.let { LyricsPronunciation(it) }
        // CJK readings need pinyin/romaji separation; Latin syllables retain authored joins.
        val segments = ArrayList<LyricsPronunciationWord>()
        val raw = buildString {
            for (i in 0 until element.childNodes.length) {
                val node = element.childNodes.item(i)
                if (node is Element && node.localName == "span") {
                    val words = if (separateSyllables) text(node) else text(node, 1)
                    if (words.isBlank()) {
                        if (!separateSyllables) append(words.replace(Regex("\\s+"), " "))
                        continue
                    }
                    if (separateSyllables && isNotEmpty() && !last().isWhitespace()) append(' ')
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
                        segments += LyricsPronunciationWord(
                            (offset + words.indexOfFirst { !it.isWhitespace() })..
                                (offset + words.indexOfLast { !it.isWhitespace() }), begin, end)
                } else if (node.nodeType == Node.TEXT_NODE || node.nodeType == Node.CDATA_SECTION_NODE) {
                    append(ttmlTextNode(node).replace(Regex("\\s+"), " "))
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

        private fun hasCjkSource(node: Node, depth: Int = 0): Boolean {
            require(depth <= 64)
            if (node is Element && (isCompanion(node) || (depth > 0 && role(node) == "x-bg"))) return false
            if (node.nodeType == Node.TEXT_NODE || node.nodeType == Node.CDATA_SECTION_NODE) {
                return node.nodeValue.orEmpty().codePoints().anyMatch {
                    Character.UnicodeScript.of(it) in setOf(Character.UnicodeScript.HAN,
                        Character.UnicodeScript.HIRAGANA, Character.UnicodeScript.KATAKANA)
                }
            }
            return (0 until node.childNodes.length).any { hasCjkSource(node.childNodes.item(it), depth + 1) }
        }

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
            if (node.nodeType == Node.TEXT_NODE || node.nodeType == Node.CDATA_SECTION_NODE) return ttmlTextNode(node)
            if (node is Element && node.localName == "br") return "\n"
            return buildString {
                for (i in 0 until node.childNodes.length) append(text(node.childNodes.item(i), depth + 1))
            }.let { if (depth == 0) it.trim() else it }
        }

        private fun Element.children(): List<Element> = (0 until childNodes.length).mapNotNull { childNodes.item(it) as? Element }
    }
}
