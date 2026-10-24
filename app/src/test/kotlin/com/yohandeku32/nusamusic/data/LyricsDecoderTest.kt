package com.yohandeku32.nusamusic.data

import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine
import com.mocharealm.accompanist.lyrics.core.model.synced.SyncedLine
import org.junit.Assert.*
import org.junit.Test

class LyricsDecoderTest {
    @Test fun appleMusicM4aTtmlAcceptsBareDecimalSecondsFromEmbeddedLyrics() {
        // Same timing dialect and metadata namespaces as the supplied M4A:
        // some units have bare decimal seconds, later ones use m:ss.mmm.
        val content = """
            <tt xmlns="http://www.w3.org/ns/ttml"
                xmlns:itunes="http://music.apple.com/lyric-ttml-internal"
                xmlns:ttm="http://www.w3.org/ns/ttml#metadata"
                itunes:timing="Word" xml:lang="id">
                <head><metadata>
                    <ttm:agent type="person" xml:id="v1"/>
                </metadata></head>
                <body dur="4:14.401">
                    <div begin="7.208" end="11.283" itunes:songPart="Chorus">
                        <p begin="7.208" end="7.816" ttm:agent="v1">
                            <span begin="7.208" end="7.446">Ko</span><span begin="7.446" end="7.816">to</span>
                        </p>
                    </div>
                    <div begin="1:07.208" end="1:10.500">
                        <p begin="1:07.208" end="1:10.500" ttm:agent="v1">
                            <span begin="1:07.208" end="1:10.500">Lanjut</span>
                        </p>
                    </div>
                </body>
            </tt>
        """.trimIndent()
        val lines = LyricsDecoder.decodeWithTiming(content, 254_401).lyrics.lines
        assertEquals(2, lines.size)
        val first = lines.first() as KaraokeLine
        assertEquals(listOf(7_208, 7_446), first.syllables.map { it.start })
        assertEquals(listOf(7_446, 7_816), first.syllables.map { it.end })
        assertEquals(67_208, lines[1].start)
    }

    @Test fun inferredEnhancedTailIsFlaggedWithoutChangingHighlightTimestamps() {
        val decoded = LyricsDecoder.decodeWithTiming("[00:01]<00:01>Hello <00:02>world\n[00:30]Next", 35000)
        val line = decoded.lyrics.lines.first() as KaraokeLine
        assertEquals(30000, line.syllables.last().end)
        assertTrue(LyricsUnitKey(2000, "world") in decoded.inferredEnds)
        assertFalse(LyricsUnitKey(1000, "Hello") in decoded.inferredEnds)
    }

    @Test fun explicitEnhancedEndSuppliesEvidenceEvenForAVeryLongHold() {
        val decoded = LyricsDecoder.decodeWithTiming("[00:01]<00:01>Hello<00:20>\n[00:30]Next", 35000)
        assertTrue(decoded.inferredEnds.isEmpty())
        assertEquals(20000, decoded.lyrics.lines.first().end)
    }

    @Test fun inheritedTtmlContainerEndIsNotEvidenceOfAHeldVocal() {
        val decoded = LyricsDecoder.decodeWithTiming("""<tt><body><p begin="1s" end="30s"><span begin="1s">Hello</span></p></body></tt>""", 35000)
        assertEquals(30000, decoded.lyrics.lines.single().end)
        assertTrue(LyricsUnitKey(1000, "Hello") in decoded.inferredEnds)
        val explicit = LyricsDecoder.decodeWithTiming("""<tt><body><p begin="1s" end="30s"><span begin="1s" dur="5s">Hello</span></p></body></tt>""", 35000)
        assertTrue(explicit.inferredEnds.isEmpty())
    }

    @Test fun inferredTimingEvidenceFollowsTheSameLrcOffsetAsPlayback() {
        val decoded = LyricsDecoder.decodeWithTiming("[offset:500]\n[00:01]<00:01>Hello", 5000)
        assertEquals(500, decoded.lyrics.lines.single().start)
        assertTrue(LyricsUnitKey(500, "Hello") in decoded.inferredEnds)
    }

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
        val line = LyricsDecoder.decode("""<tt xmlns="http://www.w3.org/ns/ttml"><body><div><p begin="1.5s" dur="2500ms">Hello world</p></div></body></tt>""", 8_000).lines.single() as SyncedLine
        assertEquals(1_500, line.start)
        assertEquals(4_000, line.end)
        assertEquals("Hello world", line.content)
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
    @Test fun startOnlyLrcKeepsAuthoredBoundariesWithoutVocalEstimates() {
        val lines = LyricsDecoder.decode("[00:05]Hello\n[00:25]Next", 50_000).lines
        assertEquals(25_000, lines.first().end)
        assertEquals(25_000, lines.last().start)
        assertEquals(50_000, lines.last().end)
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
    @Test fun missingEnhancedEndUsesNextAuthoredLineBoundary() {
        val line = LyricsDecoder.decode("[00:01]<00:01>Hello <00:02>world\n[00:30]Next", 35_000).lines.first()
        assertEquals(30_000, line.end)
    }
    @Test fun ttmlRetainsAdjacentSyllablesInsideTheSameWord() {
        val line = LyricsDecoder.decode("""<tt><body><p begin="1s" end="2.3s"><span begin="1s" end="1.4s">ber</span><span begin="1.4s" end="1.8s">sa</span><span begin="1.8s" end="2.3s">ma</span></p></body></tt>""", 5_000).lines.single() as KaraokeLine
        assertEquals(listOf("ber", "sa", "ma"), line.syllables.map { it.content })
        assertEquals(listOf(1_000, 1_400, 1_800), line.syllables.map { it.start })
        assertEquals(listOf(1_400, 1_800, 2_300), line.syllables.map { it.end })
    }
    @Test fun enhancedLrcRetainsAdjacentSyllablesInsideTheSameWord() {
        val line = LyricsDecoder.decode("[00:01.000]<00:01.000>ber<00:01.400>sa<00:01.800>ma<00:02.300>", 5_000).lines.single() as KaraokeLine
        assertEquals(listOf("ber", "sa", "ma"), line.syllables.map { it.content })
        assertEquals(listOf(1_000, 1_400, 1_800), line.syllables.map { it.start })
        assertEquals(listOf(1_400, 1_800, 2_300), line.syllables.map { it.end })
    }
    @Test fun duetTtmlUsesExplicitAgentTagsInsteadOfAlternatingRows() {
        val content = """
            <tt xmlns="http://www.w3.org/ns/ttml"
                xmlns:ttm="http://www.w3.org/ns/ttml#metadata">
                <head><metadata>
                    <ttm:agent xml:id="singer1" type="person"/>
                    <ttm:agent xml:id="singer2" type="person"/>
                </metadata></head>
                <body><div>
                    <p begin="1s" end="3s" ttm:agent="singer1">
                        <span begin="1s" end="2s">Hello </span>
                        <span begin="2s" end="3s">there</span>
                    </p>
                    <p begin="3s" end="5s" ttm:agent="singer2">
                        <span begin="3s" end="4s">Answer </span>
                        <span begin="4s" end="5s">me</span>
                    </p>
                    <p begin="5s" end="7s" ttm:agent="singer1">
                        <span begin="5s" end="7s">Together</span>
                    </p>
                </div></body>
            </tt>
        """.trimIndent()
        val result = LyricsDecoder.decodeWithTiming(content, 10_000)
        assertEquals(3, result.lyrics.lines.size)
        assertEquals(setOf(1), result.duetRightLines)
        assertTrue(result.lyrics.lines[1] is KaraokeLine)
        assertEquals(listOf(3_000, 4_000),
            (result.lyrics.lines[1] as KaraokeLine).syllables.map { it.start })
    }

    @Test fun soloTtmlWithOnlyOneAgentKeepsEveryLineLeft() {
        val content = """
            <tt xmlns:ttm="http://www.w3.org/ns/ttml#metadata">
                <body>
                    <p begin="1s" end="2s" ttm:agent="solo">Only one</p>
                    <p begin="3s" end="4s" ttm:agent="solo">Still solo</p>
                </body>
            </tt>
        """.trimIndent()
        assertTrue(LyricsDecoder.decodeWithTiming(content, 5_000).duetRightLines.isEmpty())
    }

    @Test fun simultaneousDuetLinesRetainSourceAndRightSideIdentity() {
        val content = """
            <tt xmlns:ttm="http://www.w3.org/ns/ttml#metadata">
                <body><div>
                    <p begin="2s" end="4s" ttm:agent="first">
                        <span begin="2s" end="4s">First line</span>
                    </p>
                    <p begin="2s" end="4s" ttm:agent="second">
                        <span begin="2s" end="4s">Second line</span>
                    </p>
                </div></body>
            </tt>
        """.trimIndent()
        val result = LyricsDecoder.decodeWithTiming(content, 5_000)
        assertEquals(listOf(2_000, 2_000), result.lyrics.lines.map { it.start })
        assertEquals(setOf(1), result.duetRightLines)
    }

    @Test fun ttmlDuetAlwaysUsesDeclaredSingersEvenWhenRightSingerStartsFirst() {
        val content = """
            <tt xmlns="http://www.w3.org/ns/ttml"
                xmlns:ttm="http://www.w3.org/ns/ttml#metadata">
                <head><metadata>
                    <ttm:agent xml:id="leftLead" type="person"/>
                    <ttm:agent xml:id="rightSinger" type="person"/>
                </metadata></head>
                <body><div>
                    <p begin="1s" end="2s" ttm:agent="rightSinger">
                        <span begin="1s" end="2s">Right opens</span>
                    </p>
                    <p begin="2s" end="3s" ttm:agent="leftLead">
                        <span begin="2s" end="3s">Left replies</span>
                    </p>
                    <p begin="3s" end="4s" ttm:agent="rightSinger">
                        <span begin="3s" end="4s">Right returns</span>
                    </p>
                    <p begin="4s" end="5s" ttm:agent="leftLead">
                        <span begin="4s" end="5s">Left returns</span>
                    </p>
                </div></body>
            </tt>
        """.trimIndent()
        val decoded = LyricsDecoder.decodeWithTiming(content, 6_000)
        assertEquals(listOf(1000, 2000, 3000, 4000),
            decoded.lyrics.lines.map { it.start })
        assertEquals(setOf(0, 2), decoded.duetRightLines)
    }

    @Test fun ttmlDuetKeepsSecondSingerOnRightWhenOnlySpansCarryAgent() {
        val content = """
            <tt xmlns="http://www.w3.org/ns/ttml"
                xmlns:ttm="http://www.w3.org/ns/ttml#metadata">
                <head><metadata>
                    <ttm:agent xml:id="left" type="person"/>
                    <ttm:agent xml:id="right" type="person"/>
                </metadata></head>
                <body><div>
                    <p begin="1s" end="2s">
                        <span begin="1s" end="2s" ttm:agent="right">Hi</span>
                    </p>
                    <p begin="2s" end="3s">
                        <span begin="2s" end="3s" ttm:agent="left">Hello</span>
                    </p>
                </div></body>
            </tt>
        """.trimIndent()
        val decoded = LyricsDecoder.decodeWithTiming(content, 4_000)
        assertEquals(2, decoded.lyrics.lines.size)
        assertEquals(setOf(0), decoded.duetRightLines)
    }

    @Test fun ttmlDuetAlignsIdenticalSimultaneousTextUsingSourceIdentity() {
        val content = """
            <tt xmlns="http://www.w3.org/ns/ttml"
                xmlns:ttm="http://www.w3.org/ns/ttml#metadata">
                <head><metadata>
                    <ttm:agent xml:id="left" type="person"/>
                    <ttm:agent xml:id="right" type="person"/>
                </metadata></head>
                <body><div>
                    <p begin="1s" end="3s" ttm:agent="left">
                        <span begin="1s" end="3s">Together</span>
                    </p>
                    <p begin="1s" end="3s" ttm:agent="right">
                        <span begin="1s" end="3s">Together</span>
                    </p>
                </div></body>
            </tt>
        """.trimIndent()
        val decoded = LyricsDecoder.decodeWithTiming(content, 4_000)
        assertEquals(2, decoded.lyrics.lines.size)
        assertEquals(setOf(1), decoded.duetRightLines)
    }

}
