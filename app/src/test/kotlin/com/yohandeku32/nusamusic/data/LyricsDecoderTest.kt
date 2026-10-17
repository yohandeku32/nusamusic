package com.yohandeku32.nusamusic.data

import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine
import com.mocharealm.accompanist.lyrics.core.model.synced.SyncedLine
import org.junit.Assert.*
import org.junit.Test

class LyricsDecoderTest {
    @Test fun zeroDurationTtmlWordsProduceFiniteKaraokeProgress() {
        val content = """<tt><body><p begin="1s" end="1s"><span begin="1s" end="1s">Hello</span></p></body></tt>"""
        val line = LyricsDecoder.decode(content, 5_000).lines.single() as KaraokeLine
        assertTrue(line.duration > 0)
        assertTrue(line.syllables.single().duration > 0)
        assertTrue(line.syllables.single().progress(1_000).isFinite())
    }
    @Test fun offsetClippedWordTimingCannotDivideByZero() {
        val line = LyricsDecoder.decode("[offset:5000]\n[00:01]<00:01>Hello<00:02>", 8_000).lines.single() as KaraokeLine
        assertTrue(line.duration > 0)
        assertTrue(line.syllables.single().progress(0).isFinite())
    }
    @Test fun repeatedTimestampsAreExpandedAndSorted() {
        val lyrics = LyricsDecoder.decode("[00:10][00:30.5]Chorus\n[00:20.123]Verse", 40_000)
        assertEquals(listOf(10_000, 20_123, 30_500), lyrics.lines.map { it.start })
        assertEquals(listOf("Chorus", "Verse", "Chorus"), lyrics.lines.map { (it as SyncedLine).content })
        assertEquals(listOf(20_123, 30_500, 40_000), lyrics.lines.map { it.end })
    }
    @Test fun positiveOffsetAdvancesLyricsAndClampsBeforeZero() {
        val lines = LyricsDecoder.decode("[offset:+500]\n[00:00.20]First\n[00:03]Second", 8_000).lines
        assertEquals(0, lines[0].start)
        assertEquals(2_500, lines[1].start)
        assertEquals(7_500, lines[1].end)
    }
    @Test fun negativeOffsetDelaysLyrics() {
        assertEquals(1_300, LyricsDecoder.decode("[offset:-300]\n[00:01]Hello", 5_000).lines.first().start)
    }
    @Test fun translationSharesItsOriginalLine() {
        val line = LyricsDecoder.decode("[00:01.50]Hello\n[00:01.50]Halo", 5_000).lines.single() as SyncedLine
        assertEquals("Hello", line.content)
        assertEquals("Halo", line.translation)
    }
    @Test fun enhancedLrcRetainsWordTimingAndLastWordDuration() {
        val line = LyricsDecoder.decode("[00:01.000]<00:01.000>Hello <00:02.000>world<00:03.000>\n[00:04.000]Next", 8_000).lines.first() as KaraokeLine
        assertEquals(listOf(1_000, 2_000), line.syllables.map { it.start })
        assertTrue(line.syllables.all { it.duration > 0 })
        assertEquals(3_000, line.syllables.last().end)
    }
    @Test fun enhancedLrcNormalizesWholeSecondsAndTenths() {
        val line = LyricsDecoder.decode("[00:01]<00:01>Hello <00:02.5>world", 5_000).lines.single() as KaraokeLine
        assertEquals(listOf(1_000, 2_500), line.syllables.map { it.start })
        assertEquals(5_000, line.syllables.last().end)
    }
    @Test fun ttmlPreservesWordsAndXmlEntities() {
        val content = """<tt xmlns="http://www.w3.org/ns/ttml"><body><div><p begin="00:01.000" end="00:04.000"><span begin="00:01.000" end="00:02.000">You &amp; </span><span begin="00:02.000" end="00:04.000">me</span></p></div></body></tt>"""
        val line = LyricsDecoder.decode(content, 8_000).lines.single() as KaraokeLine
        assertEquals(listOf(1_000, 2_000), line.syllables.map { it.start })
        assertEquals("You & me", line.syllables.joinToString("") { it.content })
    }
    @Test fun ttmlSupportsLineTimingSecondsAndDurations() {
        val line = LyricsDecoder.decode("""<tt xmlns="http://www.w3.org/ns/ttml"><body><div><p begin="1.5s" dur="2500ms">Hello world</p></div></body></tt>""", 8_000).lines.single() as KaraokeLine
        assertEquals(1_500, line.start)
        assertEquals(4_000, line.end)
        assertEquals("Hello world", line.syllables.single().content)
    }
    @Test fun prefixedTtmlElementsWork() {
        val lines = LyricsDecoder.decode("""<tt:tt xmlns:tt="http://www.w3.org/ns/ttml"><tt:body><tt:div><tt:p begin="00:01" end="00:03">Halo</tt:p></tt:div></tt:body></tt:tt>""", 5_000).lines
        assertEquals(1_000, lines.single().start)
    }
    @Test(expected = IllegalArgumentException::class) fun rejectsUntimedImport() {
        LyricsDecoder.decode("just plain text", 5_000)
    }
    @Test(expected = IllegalArgumentException::class) fun rejectsUntimedTtml() {
        LyricsDecoder.decode("<tt><body><div><p>Hello</p></div></body></tt>", 5_000)
    }
    @Test(expected = IllegalArgumentException::class) fun rejectsExternalEntities() {
        LyricsDecoder.decode("""<!DOCTYPE tt [<!ENTITY leak SYSTEM "file:///private">]><tt><p begin="1s" end="3s">&leak;</p></tt>""", 5_000)
    }
    @Test(expected = IllegalArgumentException::class) fun rejectsOversizedInput() {
        LyricsDecoder.decode("a".repeat(LyricsDecoder.MAX_BYTES + 1), 5_000)
    }
    @Test fun ttmlContainerPaddingDoesNotKeepTheSingerActiveThroughSilence() {
        val line = LyricsDecoder.decode("""<tt><body><p begin="0s" end="30s"><span begin="5s" end="7s">Hello</span><span begin="7s" end="30s"> </span></p></body></tt>""", 35_000).lines.single()
        assertEquals(5_000, line.start)
        assertEquals(7_000, line.end)
        assertEquals(7_000, (line as KaraokeLine).syllables.maxOf { it.end })
    }
    @Test fun actualWordStartsAreResortedAfterContainerPaddingIsRemoved() {
        val lines = LyricsDecoder.decode("""<tt><body><p begin="0s" end="10s"><span begin="5s" end="7s">Later</span></p><p begin="2s" end="4s">Earlier</p></body></tt>""", 12_000).lines
        assertEquals(listOf(2_000, 5_000), lines.map { it.start })
    }
    @Test fun longStartOnlyLrcHoldsAreEstimatedWithoutMovingTheNextStart() {
        val lines = LyricsDecoder.decode("[00:05]Hello\n[00:25]Next", 50_000).lines
        assertTrue(lines.first().end in 8_000..14_000)
        assertEquals(25_000, lines.last().start)
        assertTrue(lines.last().end < 50_000)
    }
    @Test fun explicitBlankLrcEndPreservesALongHeldLine() {
        val lines = LyricsDecoder.decode("[00:05]Hello\n[00:25]\n[00:35]Next", 40_000).lines
        assertEquals(25_000, lines.first().end)
        assertEquals(35_000, lines.last().start)
    }
    @Test fun explicitEnhancedEndPreservesALongHeldWord() {
        val line = LyricsDecoder.decode("[00:01]<00:01>Hello<00:20>\n[00:30]Next", 35_000).lines.first()
        assertEquals(20_000, line.end)
    }
    @Test fun missingEnhancedEndDoesNotHoldTheLastWordThroughALongBreak() {
        val line = LyricsDecoder.decode("[00:01]<00:01>Hello <00:02>world\n[00:30]Next", 35_000).lines.first()
        assertTrue(line.end in 5_000..11_000)
    }
}
