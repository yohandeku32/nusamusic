package com.yohandeku32.nusamusic.data

import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine
import com.mocharealm.accompanist.lyrics.core.model.synced.SyncedLine
import org.junit.Assert.*
import org.junit.Test

class TtmlCompanionsTest {
    private fun decode(metadata: String = "", body: String) = LyricsDecoder.decodeWithTiming("""
        <tt xmlns="http://www.w3.org/ns/ttml" xmlns:m="http://www.w3.org/ns/ttml#metadata"
            xmlns:a="http://music.apple.com/lyric-ttml-internal">
            <head><metadata>$metadata</metadata></head><body>$body</body>
        </tt>
    """.trimIndent(), 30_000)

    @Test fun appleMetadataKeepsSeparateTranslationAndTimedPronunciation() {
        val result = decode("""
            <a:iTunesMetadata><a:translations><a:translation xml:lang="id">
                <a:text for="L1">Selamat malam</a:text>
            </a:translation></a:translations><a:transliterations><a:transliteration xml:lang="en-Latn">
                <a:text for="L1"><span begin="1s" end="2s">wan</span><span begin="2s" end="3s">an</span></a:text>
            </a:transliteration></a:transliterations></a:iTunesMetadata>
        """, """<p a:key="L1" begin="1s" end="3s"><span begin="1s" end="2s">晚</span><span begin="2s" end="3s">安</span></p>""")
        val line = result.lyrics.lines.single() as KaraokeLine
        assertEquals("晚安", line.syllables.joinToString("") { it.content })
        assertEquals("Selamat malam", line.translation)
        assertEquals("wan an", result.pronunciations[0]?.text)
        assertEquals(listOf(1_000, 2_000), result.pronunciations[0]?.words?.map { it.start })
        assertEquals(listOf(2_000, 3_000), result.pronunciations[0]?.words?.map { it.end })
        assertTrue(result.inferredEnds.isEmpty())
    }

    @Test fun inlineCompanionsCannotBecomeSungWordsOrExtendPlayback() {
        val result = decode(body = """
            <p begin="1s" end="3s"><span begin="1s" end="3s">晚安</span>
                <span m:role="x-roman" begin="1s" end="29s">wan an</span>
                <span m:role="x-translation"><span>Selamat &amp; tenang</span></span>
            </p>
        """)
        val line = result.lyrics.lines.single() as KaraokeLine
        assertEquals(listOf("晚安"), line.syllables.map { it.content.trim() })
        assertEquals(3_000, line.end)
        assertEquals("Selamat & tenang", line.translation)
        assertEquals("wan an", result.pronunciations[0]?.text)
        assertTrue(result.inferredEnds.isEmpty())
    }

    @Test fun lineTimedLyricsWithInlineTranslationRemainLineTimed() {
        val result = decode(body = """<p begin="1s" end="3s">Good night<span m:role="x-translation">Selamat malam</span><span m:role="x-roman">gud nait</span></p>""")
        val line = result.lyrics.lines.single() as SyncedLine
        assertEquals("Good night", line.content)
        assertEquals("Selamat malam", line.translation)
        assertEquals("gud nait", result.pronunciations[0]?.text)
    }

    @Test fun sourceKeysSurviveSortingAndTrimmedContainerStarts() {
        val result = decode("""
            <a:transliterations><a:transliteration><a:text for="late">later</a:text><a:text for="early">earlier</a:text></a:transliteration></a:transliterations>
        """, """
            <p a:key="late" begin="0s" end="10s" m:agent="v2"><span begin="5s" end="7s">Later</span></p>
            <p a:key="early" begin="2s" end="4s">Earlier</p>
        """)
        assertEquals(listOf(2_000, 5_000), result.lyrics.lines.map { it.start })
        assertEquals("earlier", result.pronunciations[0]?.text)
        assertEquals("later", result.pronunciations[1]?.text)
        assertEquals(setOf(1), result.duetRightLines)
    }

    @Test fun timedNestedTranslationDoesNotInventWordTimingOrDuetAlignment() {
        val result = decode(body = """
            <p begin="1s" end="3s">Good night<span m:role="x-translation"><span begin="1s" end="20s" m:agent="v2">Selamat malam</span></span></p>
        """)
        val line = result.lyrics.lines.single() as SyncedLine
        assertEquals("Good night", line.content)
        assertEquals("Selamat malam", line.translation)
        assertEquals(3_000, line.end)
        assertTrue(result.duetRightLines.isEmpty())
    }

    @Test fun simultaneousDuetRowsKeepTheirOwnCompanions() {
        val result = decode(body = """
            <p begin="1s" end="3s" m:agent="v1"><span begin="1s" end="3s">Lead</span><span m:role="x-roman">liid</span><span m:role="x-translation">Utama</span></p>
            <p begin="1s" end="3s" m:agent="v2"><span begin="1s" end="3s">Guest</span><span m:role="x-roman">gest</span><span m:role="x-translation">Tamu</span></p>
        """)
        assertEquals(listOf("liid", "gest"), result.pronunciations.values.map { it.text })
        assertEquals(listOf("Utama", "Tamu"), result.lyrics.lines.map { (it as KaraokeLine).translation })
        assertEquals(setOf(1), result.duetRightLines)
    }

    @Test fun backingCompanionsStayWithTheirOwnVoice() {
        val result = decode(body = """
            <p begin="1s" end="4s"><span begin="1s" end="3s">Lead</span><span m:role="x-roman">liid</span>
                <span m:role="x-bg" m:agent="v2" begin="2s" end="4s"><span begin="2s" end="4s">Echo</span><span m:role="x-translation">Gema</span><span m:role="x-roman">eko</span></span>
            </p>
        """)
        assertEquals(2, result.lyrics.lines.size)
        assertEquals("liid", result.pronunciations[0]?.text)
        assertEquals("eko", result.pronunciations[1]?.text)
        assertEquals("Gema", (result.lyrics.lines[1] as KaraokeLine).translation)
        assertTrue((result.lyrics.lines[1] as KaraokeLine).isAccompaniment)
        assertEquals(setOf(1), result.duetRightLines)
    }

    @Test fun metadataWithNoMatchingKeyDoesNotAttachToAnotherLine() {
        val result = decode("""<a:transliterations><a:transliteration><a:text for="missing">wrong</a:text></a:transliteration></a:transliterations>""",
            """<p a:key="L1" begin="1s" end="3s">Hello</p>""")
        assertTrue(result.pronunciations.isEmpty())
    }

    @Test fun firstAuthoredLanguageVariantIsStableAndInlineTextTakesPrecedence() {
        val result = decode("""
            <a:translations><a:translation xml:lang="id"><a:text for="L1">Pertama</a:text></a:translation><a:translation xml:lang="en"><a:text for="L1">Second</a:text></a:translation></a:translations>
        """, """<p a:key="L1" begin="1s" end="3s">Hello<span m:role="x-translation">Inline</span></p>""")
        assertEquals("Inline", (result.lyrics.lines.single() as SyncedLine).translation)
        val metadataOnly = decode("""<a:translations><a:translation><a:text for="L1">Pertama</a:text></a:translation><a:translation><a:text for="L1">Second</a:text></a:translation></a:translations>""",
            """<p a:key="L1" begin="1s" end="3s">Hello</p>""")
        assertEquals("Pertama", (metadataOnly.lyrics.lines.single() as SyncedLine).translation)
    }

    @Test fun invalidOptionalWordTimingFallsBackToReadablePronunciation() {
        val result = decode("""
            <a:transliterations><a:transliteration><a:text for="L1"><span begin="bad" end="2s">wan</span><span begin="3s" end="2s">an</span></a:text></a:transliteration></a:transliterations>
        """, """<p a:key="L1" begin="1s" end="3s">晚安</p>""")
        assertEquals("wan an", result.pronunciations[0]?.text)
        assertTrue(result.pronunciations[0]!!.words.isEmpty())
        assertEquals(1_000, result.lyrics.lines.single().start)
    }

    @Test fun lrcTranslationsKeepWorkingWithoutPronunciations() {
        val result = LyricsDecoder.decodeWithTiming("[00:01]Hello\n[00:01]Halo", 5_000)
        assertEquals("Halo", (result.lyrics.lines.single() as SyncedLine).translation)
        assertTrue(result.pronunciations.isEmpty())
    }

    @Test fun repeatedWordsKeepTheirExactOffsetsEvenWhenEarlierWordsHaveNoTiming() {
        val result = decode("""
            <a:transliterations><a:transliteration><a:text for="L1"> wan <span begin="bad" end="2s">wan</span><span begin="2s" end="3s">wan</span></a:text></a:transliteration></a:transliterations>
        """, """<p a:key="L1" begin="1s" end="3s">晚晚晚</p>""")
        val pronunciation = result.pronunciations.getValue(0)
        assertEquals("wan wan wan", pronunciation.text)
        assertEquals(8..10, pronunciation.words.single().range)
        assertEquals(2_000, pronunciation.words.single().start)
    }

    @Test fun selectorCarriesCompanionsAlongWithTheOriginalTtml() {
        val raw = """<tt xmlns:m="http://www.w3.org/ns/ttml#metadata"><body><p begin="1s" end="3s">晚安<span m:role="x-roman">wan an</span><span m:role="x-translation">Selamat malam</span></p></body></tt>"""
        val selected = EmbeddedLyricsSelector.select(listOf(raw), 5_000)
        assertEquals(raw, selected.stored.content)
        assertEquals("wan an", selected.pronunciations[0]?.text)
        assertEquals("Selamat malam", (selected.synced!!.lines.single() as SyncedLine).translation)
    }
}
