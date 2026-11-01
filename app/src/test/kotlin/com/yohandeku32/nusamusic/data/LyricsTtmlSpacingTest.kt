package com.yohandeku32.nusamusic.data

import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine
import org.junit.Assert.*
import org.junit.Test

class LyricsTtmlSpacingTest {
    private val pieces = listOf("men", "cin", "tai", "mu")
    private fun spans(values: List<String> = pieces, separator: String = "") = values.mapIndexed { index, word ->
        "<span begin=\"${1000 + index * 300}ms\" end=\"${1300 + index * 300}ms\">$word</span>"
    }.joinToString(separator)
    private fun xml(body: String, attributes: String = "") = """<tt xmlns="http://www.w3.org/ns/ttml" xmlns:m="http://www.w3.org/ns/ttml#metadata" $attributes><body><p begin="1s" end="3s">$body</p></body></tt>"""
    private fun decode(raw: String) = LyricsDecoder.decodeWithTiming(raw, 10_000)

    @Test fun formattingIndentationDoesNotTurnTimedSyllablesIntoSeparateWords() {
        for (indent in listOf("\n    ", "\r\n\t", "\t")) {
            val line = decode(xml(spans(separator = indent))).lyrics.lines.single() as KaraokeLine
            assertEquals("mencintaimu", line.syllables.joinToString("") { it.content })
            assertEquals(pieces, line.syllables.map { it.content })
            assertEquals(listOf(1000, 1300, 1600, 1900), line.syllables.map { it.start })
            assertEquals(listOf(1300, 1600, 1900, 2200), line.syllables.map { it.end })
        }
    }

    @Test fun compactSyllablesAndRealWordSpacesKeepTheirOriginalMeaning() {
        assertEquals("mencintaimu", visibleMainLyrics(EmbeddedLyricsSelector.select(listOf(xml(spans())), 10_000)))
        val body = spans(listOf("men", "cin", "tai", "mu "), "\n  ") +
            "\n  <span begin=\"2.2s\" end=\"3s\">selalu</span>"
        val line = decode(xml(body)).lyrics.lines.single() as KaraokeLine
        assertEquals("mencintaimu selalu", line.syllables.joinToString("") { it.content })
        val separated = decode(xml(spans(separator = " "))).lyrics.lines.single() as KaraokeLine
        assertEquals("men cin tai mu", separated.syllables.joinToString("") { it.content })
    }

    @Test fun explicitPreserveRetainsItsAuthoredWhitespace() {
        val line = decode(xml(spans(separator = "\n  "), "xml:space=\"preserve\""))
            .lyrics.lines.single() as KaraokeLine
        assertEquals("men cin tai mu", line.syllables.joinToString("") { it.content })
    }

    @Test fun latinPronunciationDoesNotAcquireSpacesBetweenAdjacentSyllableSpans() {
        val body = spans() + "<span m:role=\"x-roman\">" + spans() + "</span>"
        val result = decode(xml(body))
        assertEquals("mencintaimu", result.pronunciations.getValue(0).text)
        assertEquals(listOf(0..2, 3..5, 6..8, 9..10), result.pronunciations.getValue(0).words.map { it.range })
    }

    @Test fun cjkPronunciationRetainsReadableSyllableSpacing() {
        val body = spans(listOf("我", "爱", "着", "你")) +
            "<span m:role=\"x-roman\">" + spans(listOf("wo", "ai", "zhe", "ni")) + "</span>"
        assertEquals("wo ai zhe ni", decode(xml(body)).pronunciations.getValue(0).text)
    }

    @Test fun metadataLatinReadingsKeepRealWordSpacesAndIgnoreChineseTranslations() {
        val reading = spans(listOf("men", "cin", "tai", "mu "), "\n  ") +
            "\n <span begin=\"2.2s\" end=\"3s\">selalu</span>"
        val raw = xml(spans() + "<span m:role=\"x-translation\">永远爱你</span>")
            .replace("<body>", """<head><metadata><transliterations><transliteration><text for="L1">$reading</text></transliteration></transliterations></metadata></head><body>""")
            .replace("<p begin=", "<p xml:id=\"L1\" begin=")
        val result = decode(raw).pronunciations.getValue(0)
        assertEquals("mencintaimu selalu", result.text)
        assertEquals(listOf("men", "cin", "tai", "mu", "selalu"), result.words.map { result.text.substring(it.range) })
    }

    @Test fun paragraphDefaultCanOverrideAnInheritedPreserveSetting() {
        val raw = xml(spans(separator = "\n  "), "xml:space=\"preserve\"")
            .replace("<p begin=", "<p xml:space=\"default\" begin=")
        assertEquals("mencintaimu", (decode(raw).lyrics.lines.single() as KaraokeLine).syllables.joinToString("") { it.content })
    }
}
