package com.yohandeku32.nusamusic.data

import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine
import com.mocharealm.accompanist.lyrics.core.model.synced.SyncedLine
import org.junit.Assert.*
import org.junit.Test

class LyricsDecoderTest {
    @Test fun loneSpanTaggedGuestDoesNotBecomeTheDefaultBodyLead() {
        val xml = """
            <tt xmlns:ttm="http://www.w3.org/ns/ttml#metadata"><body>
                <p begin="1s" end="2s">
                    <span begin="1s" end="2s" ttm:agent="v2">Guest</span>
                </p>
            </body></tt>
        """.trimIndent()
        assertEquals(setOf(0), LyricsDecoder.decodeWithTiming(xml, 3000).duetRightLines)
    }

    @Test fun victoryLapFourGuestAtThreeMinutes27IsRightWhileBodyLeadStaysLeft() {
        val xml = """
            <tt xmlns:ttm="http://www.w3.org/ns/ttml#metadata">
                <head><metadata>
                    <ttm:agent type="person" xml:id="v1"/>
                    <ttm:agent type="person" xml:id="v3"/>
                    <ttm:agent type="person" xml:id="v5"/>
                </metadata></head>
                <body ttm:agent="v5"><div>
                    <p begin="3:26.122" end="3:27.365">
                        <span begin="3:26.122" end="3:27.365">Lead</span>
                    </p>
                    <p begin="3:27.696" end="3:28.750" ttm:agent="v3">
                        <span begin="3:27.696" end="3:28.750">Guest</span>
                    </p>
                </div></body>
            </tt>
        """.trimIndent()
        val result = LyricsDecoder.decodeWithTiming(xml, 344571)
        assertEquals(207696, result.lyrics.lines[1].start)
        assertEquals(setOf(1), result.duetRightLines)
    }

    @Test fun thirdNamedSingerIsAGuestInsteadOfAlternatingBackToTheLeft() {
        val xml = """
            <tt xmlns:ttm="http://www.w3.org/ns/ttml#metadata">
                <head><metadata>
                    <ttm:agent type="person" xml:id="lead"/>
                    <ttm:agent type="person" xml:id="guestA"/>
                    <ttm:agent type="person" xml:id="guestB"/>
                </metadata></head>
                <body>
                    <p begin="1s" end="2s" ttm:agent="lead">Lead</p>
                    <p begin="2s" end="3s" ttm:agent="guestA">First guest</p>
                    <p begin="3s" end="4s" ttm:agent="guestB">Second guest</p>
                </body>
            </tt>
        """.trimIndent()
        assertEquals(setOf(1, 2), LyricsDecoder.decodeWithTiming(xml, 5000).duetRightLines)
    }

    @Test fun rightSingerSurvivesInlineTranslationAndEarlierContainerOnset() {
        val xml = """
            <tt xmlns:ttm="http://www.w3.org/ns/ttml#metadata"><body>
                <p begin="1s" end="4s" ttm:agent="v2">
                    <span begin="2s" end="3s">Guest</span>
                    <span ttm:role="x-translation" xml:lang="id">Tamu</span>
                </p>
                <p begin="4s" end="6s" ttm:agent="v1">
                    <span begin="4s" end="5s">Lead</span>
                </p>
            </body></tt>
        """.trimIndent()
        val result = LyricsDecoder.decodeWithTiming(xml, 7000)
        assertEquals(setOf(0), result.duetRightLines)
        assertEquals(2000, result.lyrics.lines[0].start)
        assertEquals("Tamu", (result.lyrics.lines[0] as KaraokeLine).translation)
    }

    @Test fun sourceSideSurvivesCoreXmlEntityDecoding() {
        val xml = """
            <tt xmlns:ttm="http://www.w3.org/ns/ttml#metadata"><body>
                <p begin="1s" end="3s" ttm:agent="v2">
                    <span begin="1s" end="3s">A &amp;lt; B</span>
                </p>
            </body></tt>
        """.trimIndent()
        assertEquals(setOf(0), LyricsDecoder.decodeWithTiming(xml, 4000).duetRightLines)
    }

    @Test fun simultaneousUntaggedLeadAndTranslatedRightBackingKeepTheirSides() {
        val xml = """
            <tt xmlns:ttm="http://www.w3.org/ns/ttml#metadata"><body>
                <p begin="1s" end="4s">
                    <span begin="2s" end="3s">Same</span>
                    <span ttm:role="x-bg" ttm:agent="v2" begin="1s" end="4s">
                        <span begin="2s" end="3s">Same</span>
                        <span ttm:role="x-translation">Terjemahan</span>
                    </span>
                </p>
                <p begin="1s" end="4s" ttm:agent="v2">
                    <span begin="2s" end="3s">Same</span>
                </p>
            </body></tt>
        """.trimIndent()
        val result = LyricsDecoder.decodeWithTiming(xml, 5000)
        assertEquals(3, result.lyrics.lines.size)
        assertEquals(setOf(1, 2), result.duetRightLines)
        assertTrue((result.lyrics.lines[1] as KaraokeLine).isAccompaniment)
        assertEquals("Terjemahan", (result.lyrics.lines[1] as KaraokeLine).translation)
    }

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

    @Test fun duetLineTimedTtmlPreservesSidesAfterKaraokeIsConvertedToPlainLine() {
        val content = """
            <tt xmlns="http://www.w3.org/ns/ttml"
                xmlns:ttm="http://www.w3.org/ns/ttml#metadata">
                <head><metadata>
                    <ttm:agent xml:id="left" type="person"/>
                    <ttm:agent xml:id="right" type="person"/>
                </metadata></head>
                <body><div>
                    <p begin="1s" end="2s" ttm:agent="right">First on right</p>
                    <p begin="2s" end="3s" ttm:agent="left">Then on left</p>
                </div></body>
            </tt>
        """.trimIndent()
        val decoded = LyricsDecoder.decodeWithTiming(content, 5_000)
        assertEquals(2, decoded.lyrics.lines.size)
        assertTrue(decoded.lyrics.lines.all { it is SyncedLine })
        assertEquals(setOf(0), decoded.duetRightLines)
    }

    @Test fun ttmlSingerAssignmentIgnoresFormattingWhitespaceBetweenWordSpans() {
        val content = """
            <tt xmlns="http://www.w3.org/ns/ttml"
                xmlns:ttm="http://www.w3.org/ns/ttml#metadata">
                <head><metadata>
                    <ttm:agent xml:id="left" type="person"/>
                    <ttm:agent xml:id="right" type="person"/>
                </metadata></head>
                <body><div>
                    <p begin="1s" end="2s" ttm:agent="right">
                        <span begin="1s" end="1.4s">Good</span>
                        <span begin="1.4s" end="2s">morning</span>
                    </p>
                    <p begin="2s" end="3s" ttm:agent="left">
                        <span begin="2s" end="3s">Hello</span>
                    </p>
                </div></body>
            </tt>
        """.trimIndent()
        val decoded = LyricsDecoder.decodeWithTiming(content, 5_000)
        assertEquals(2, decoded.lyrics.lines.size)
        assertEquals(setOf(0), decoded.duetRightLines)
    }

    @Test fun untaggedLineDoesNotInventSecondSingerAlignment() {
        val content = """
            <tt xmlns="http://www.w3.org/ns/ttml"
                xmlns:ttm="http://www.w3.org/ns/ttml#metadata">
                <head><metadata>
                    <ttm:agent xml:id="left" type="person"/>
                    <ttm:agent xml:id="right" type="person"/>
                </metadata></head>
                <body><div>
                    <p begin="1s" end="2s">Unassigned</p>
                    <p begin="2s" end="3s" ttm:agent="right">
                        <span begin="2s" end="3s">Right</span>
                    </p>
                    <p begin="3s" end="4s" ttm:agent="left">
                        <span begin="3s" end="4s">Left</span>
                    </p>
                </div></body>
            </tt>
        """.trimIndent()
        val decoded = LyricsDecoder.decodeWithTiming(content, 5_000)
        assertEquals(3, decoded.lyrics.lines.size)
        assertEquals(setOf(1), decoded.duetRightLines)
    }

    @Test fun ttmlWithoutAgentDeclarationsKeepsStableV1V2SideAssignment() {
        val content = """
            <tt xmlns:ttm="http://www.w3.org/ns/ttml#metadata">
                <body><div>
                    <p begin="1s" end="2s" ttm:agent="v2">
                        <span begin="1s" end="2s">Right starts</span>
                    </p>
                    <p begin="2s" end="3s" ttm:agent="v1">
                        <span begin="2s" end="3s">Left replies</span>
                    </p>
                    <p begin="3s" end="4s" ttm:agent="v2">
                        <span begin="3s" end="4s">Right again</span>
                    </p>
                </div></body>
            </tt>
        """.trimIndent()
        val decoded = LyricsDecoder.decodeWithTiming(content, 5_000)
        assertEquals(3, decoded.lyrics.lines.size)
        assertEquals(setOf(0, 2), decoded.duetRightLines)
    }

    @Test fun nestedXBgSpanCanBelongToTheOtherDuetSinger() {
        val content = """
            <tt xmlns="http://www.w3.org/ns/ttml"
                xmlns:ttm="http://www.w3.org/ns/ttml#metadata">
                <head><metadata>
                    <ttm:agent xml:id="left" type="person"/>
                    <ttm:agent xml:id="right" type="person"/>
                </metadata></head>
                <body><div>
                    <p begin="1s" end="4s" ttm:agent="left">
                        <span begin="1s" end="2s">Hello </span>
                        <span begin="2s" end="4s">world</span>
                        <span ttm:role="x-bg" ttm:agent="right"
                              begin="2s" end="3s">
                            <span begin="2s" end="3s">Oh</span>
                        </span>
                    </p>
                </div></body>
            </tt>
        """.trimIndent()
        val decoded = LyricsDecoder.decodeWithTiming(content, 5_000)
        val backingIndices = decoded.lyrics.lines.indices.filter {
            (decoded.lyrics.lines[it] as? KaraokeLine)?.isAccompaniment == true
        }
        assertTrue("Nested x-bg should remain a separate backing row",
            backingIndices.isNotEmpty())
        assertTrue("Right singer backing should align on the right",
            backingIndices.any { it in decoded.duetRightLines })
        assertTrue("Left lead should stay on the left",
            decoded.lyrics.lines.indices.filterNot { it in backingIndices }
                .none { it in decoded.duetRightLines })
    }

    @Test fun nestedXBgSingerDoesNotStealRightLeadsIdentity() {
        val content = """
            <tt xmlns="http://www.w3.org/ns/ttml"
                xmlns:ttm="http://www.w3.org/ns/ttml#metadata">
                <head><metadata>
                    <ttm:agent xml:id="left" type="person"/>
                    <ttm:agent xml:id="right" type="person"/>
                </metadata></head>
                <body><div>
                    <p begin="1s" end="4s" ttm:agent="right">
                        <span begin="1s" end="2s">Good </span>
                        <span begin="2s" end="4s">morning</span>
                        <span ttm:role="x-bg" ttm:agent="left"
                              begin="2s" end="3s">
                            <span begin="2s" end="3s">Yeah</span>
                        </span>
                    </p>
                </div></body>
            </tt>
        """.trimIndent()
        val decoded = LyricsDecoder.decodeWithTiming(content, 5_000)
        val mainIndices = decoded.lyrics.lines.indices.filter {
            (decoded.lyrics.lines[it] as? KaraokeLine)?.isAccompaniment != true
        }
        assertTrue(mainIndices.isNotEmpty())
        assertTrue(mainIndices.all { it in decoded.duetRightLines })
    }

    @Test fun duetSecondSingerOnRightEvenIfFirstSingerIsSilentInThisTtml() {
        val xml = """
            <tt xmlns:ttm="http://www.w3.org/ns/ttml#metadata">
                <head><metadata>
                    <ttm:agent xml:id="v1" type="person"/>
                    <ttm:agent xml:id="v2" type="person"/>
                </metadata></head>
                <body><p begin="1s" end="3s" ttm:agent="v2">
                    <span begin="1s" end="3s">Only second singer</span>
                </p></body>
            </tt>
        """.trimIndent()
        val result = LyricsDecoder.decodeWithTiming(xml, 4000)
        assertEquals(1, result.lyrics.lines.size)
        assertEquals(setOf(0), result.duetRightLines)
    }

    @Test fun v2IsRightEvenWhenTheProviderOmitsV1AndItsDeclarations() {
        val xml = """
            <tt xmlns:ttm="http://www.w3.org/ns/ttml#metadata">
                <body><p begin="1s" end="2s" ttm:agent="v2">
                    <span begin="1s" end="2s">Right alone</span>
                </p></body>
            </tt>
        """.trimIndent()
        val result = LyricsDecoder.decodeWithTiming(xml, 3000)
        assertEquals(setOf(0), result.duetRightLines)
    }

    @Test fun providerStylesCanExplicitlyPutRightAlignedLineOnRightWithoutAgents() {
        val xml = """
            <tt xmlns="http://www.w3.org/ns/ttml"
                xmlns:tts="http://www.w3.org/ns/ttml#styling">
                <head><styling>
                    <style xml:id="rightStyle" tts:textAlign="right"/>
                    <style xml:id="leftStyle" tts:textAlign="left"/>
                </styling></head>
                <body><div>
                    <p begin="1s" end="2s" style="rightStyle">
                        <span begin="1s" end="2s">Right explicitly</span>
                    </p>
                    <p begin="2s" end="3s" style="leftStyle">
                        <span begin="2s" end="3s">Left explicitly</span>
                    </p>
                </div></body>
            </tt>
        """.trimIndent()
        val result = LyricsDecoder.decodeWithTiming(xml, 4000)
        assertEquals(setOf(0), result.duetRightLines)
    }

    @Test fun explicitTextAlignmentOverridesAgentNumber() {
        val xml = """
            <tt xmlns:ttm="http://www.w3.org/ns/ttml#metadata"
                xmlns:tts="http://www.w3.org/ns/ttml#styling">
                <head><metadata>
                    <ttm:agent xml:id="v1" type="person"/>
                    <ttm:agent xml:id="v2" type="person"/>
                </metadata></head>
                <body>
                    <p begin="1s" end="2s" ttm:agent="v1" tts:textAlign="right">
                        <span begin="1s" end="2s">Right by provider</span>
                    </p>
                    <p begin="2s" end="3s" ttm:agent="v2" tts:textAlign="left">
                        <span begin="2s" end="3s">Left by provider</span>
                    </p>
                </body>
            </tt>
        """.trimIndent()
        val result = LyricsDecoder.decodeWithTiming(xml, 4000)
        assertEquals(setOf(0), result.duetRightLines)
    }

    @Test fun providerRegionDefinesRightDuetPositionEvenWithoutAgentMetadata() {
        val xml = """
            <tt xmlns="http://www.w3.org/ns/ttml"
                xmlns:tts="http://www.w3.org/ns/ttml#styling">
                <head>
                  <styling><style xml:id="right" tts:textAlign="end"/></styling>
                  <layout>
                    <region xml:id="rightSide" style="right"/>
                    <region xml:id="leftSide" tts:textAlign="start"/>
                  </layout>
                </head>
                <body><div>
                    <p region="rightSide" begin="1s" end="2s">
                        <span begin="1s" end="2s">Right verse</span>
                    </p>
                    <p region="leftSide" begin="2s" end="3s">
                        <span begin="2s" end="3s">Left verse</span>
                    </p>
                </div></body>
            </tt>
        """.trimIndent()
        val decoded = LyricsDecoder.decodeWithTiming(xml, 4000)
        assertEquals(2, decoded.lyrics.lines.size)
        assertEquals(setOf(0), decoded.duetRightLines)
    }

    @Test fun declaredOnlySecondAgentStillRespectsItsRecognizableId() {
        val xml = """
            <tt xmlns:ttm="http://www.w3.org/ns/ttml#metadata">
                <head><metadata><ttm:agent xml:id="v2" type="person"/></metadata></head>
                <body><p begin="1s" end="2s" ttm:agent="v2">
                    <span begin="1s" end="2s">Second</span>
                </p></body>
            </tt>
        """.trimIndent()
        val decoded = LyricsDecoder.decodeWithTiming(xml, 3000)
        assertEquals(setOf(0), decoded.duetRightLines)
    }

    @Test fun ttmlRightAlignOnWordSpansAppliesToTheirEnclosingLine() {
        val xml = """
            <tt xmlns="http://www.w3.org/ns/ttml"
                xmlns:tts="http://www.w3.org/ns/ttml#styling">
                <body><p begin="1s" end="3s">
                    <span begin="1s" end="2s" tts:textAlign="end">A </span>
                    <span begin="2s" end="3s" tts:textAlign="end">second</span>
                </p></body>
            </tt>
        """.trimIndent()
        val decoded = LyricsDecoder.decodeWithTiming(xml, 4000)
        assertEquals(1, decoded.lyrics.lines.size)
        assertEquals(setOf(0), decoded.duetRightLines)
    }

    @Test fun appleMultiVocalTtmlPreservesBodyLeadAndGuestDuetSides() {
        // Reproduces the user's supplied Apple-style header and real line
        // positions, including an 'other' agent between v1 and v2.
        val xml = """
            <tt xmlns="http://www.w3.org/ns/ttml"
                xmlns:itunes="http://music.apple.com/lyric-ttml-internal"
                xmlns:ttm="http://www.w3.org/ns/ttml#metadata"
                itunes:timing="Word" xml:lang="en">
                <head><metadata>
                    <ttm:agent type="person" xml:id="v1"/>
                    <ttm:agent type="other" xml:id="v2000"/>
                    <ttm:agent type="person" xml:id="v2"/>
                    <ttm:agent type="person" xml:id="v3"/>
                    <ttm:agent type="person" xml:id="v4"/>
                    <ttm:agent type="person" xml:id="v5"/>
                    <ttm:agent type="person" xml:id="v6"/>
                </metadata></head>
                <body dur="5:44.571" ttm:agent="v5">
                    <div begin="0.000" end="1.558" itunes:songPart="Intro" ttm:agent="v1">
                        <p begin="0.000" end="0.341" itunes:key="L1" ttm:agent="v5">
                            <span begin="0.000" end="0.100">I</span>
                            <span begin="0.100" end="0.207">need</span>
                            <span begin="0.207" end="0.341">that</span>
                        </p>
                        <p begin="0.341" end="0.771" itunes:key="L2" ttm:agent="v1">
                            <span begin="0.341" end="0.441">I</span>
                            <span begin="0.441" end="0.548">need</span>
                            <span begin="0.548" end="0.771">that</span>
                        </p>
                        <p begin="0.771" end="1.217" itunes:key="L3" ttm:agent="v2">
                            <span begin="0.771" end="0.871">I</span>
                            <span begin="0.871" end="0.978">need</span>
                            <span begin="0.978" end="1.217">that</span>
                        </p>
                        <p begin="1.217" end="1.558" itunes:key="L4" ttm:agent="v3">
                            <span begin="1.217" end="1.317">I</span>
                            <span begin="1.317" end="1.424">need</span>
                            <span begin="1.424" end="1.558">that</span>
                        </p>
                    </div>
                    <div itunes:songPart="Verse">
                        <p begin="1:37.659" end="1:39.363" itunes:key="L69" ttm:agent="v4">
                            <span begin="1:37.659" end="1:38.004">Money</span>
                            <span begin="1:38.004" end="1:39.363">in my briefcase</span>
                        </p>
                        <p begin="1:50.926" end="1:51.299" itunes:key="L77" ttm:agent="v2000">
                            <span begin="1:50.926" end="1:51.299">Yah!</span>
                        </p>
                        <p begin="2:31.165" end="2:32.163" itunes:key="L98" ttm:agent="v2">
                            <span begin="2:31.165" end="2:31.288">I</span>
                            <span begin="2:31.288" end="2:31.411">need</span>
                            <span begin="2:31.411" end="2:32.163">that</span>
                        </p>
                        <p begin="3:27.696" end="3:28.750" itunes:key="L133" ttm:agent="v3">
                            <span begin="3:27.696" end="3:28.320">Check</span>
                            <span begin="3:28.320" end="3:28.750">check</span>
                        </p>
                        <p begin="4:32.488" end="4:33.778" itunes:key="L170" ttm:agent="v6">
                            <span begin="4:32.488" end="4:33.778">Oerrrr!</span>
                        </p>
                    </div>
                </body>
            </tt>
        """.trimIndent()
        val parsed = LyricsDecoder.decodeWithTiming(xml, 344_571)
        fun index(timeMs: Int): Int = parsed.lyrics.lines.indexOfFirst {
            it.start == timeMs
        }.also { assertTrue("Missing user example lyric at $timeMs", it >= 0) }
        // Guest IDs do not alternate left/right by their numeric parity.
        assertTrue(index(771) in parsed.duetRightLines) // v2 / L3
        assertTrue(index(151165) in parsed.duetRightLines) // v2 / L98
        assertTrue(index(97659) in parsed.duetRightLines) // v4 / L69
        assertTrue(index(272488) in parsed.duetRightLines) // v6 / L170
        assertFalse(index(0) in parsed.duetRightLines) // v5
        assertFalse(index(341) in parsed.duetRightLines) // v1
        assertTrue(index(1217) in parsed.duetRightLines) // v3
        assertFalse(index(110926) in parsed.duetRightLines) // v2000, 'other'
        assertTrue(index(207696) in parsed.duetRightLines) // v3, user's 03:27 report
    }

    @Test fun untypedAndNamedSingersPreserveTraditionalTwoSingerAlternation() {
        val xml = """
            <tt xmlns:ttm="http://www.w3.org/ns/ttml#metadata">
                <head><metadata>
                    <ttm:agent xml:id="singerLead" type="person"/>
                    <ttm:agent xml:id="singerGuest" type="person"/>
                </metadata></head>
                <body>
                    <p begin="1s" end="2s" ttm:agent="singerGuest">
                        <span begin="1s" end="2s">Guest on right</span>
                    </p>
                    <p begin="2s" end="3s" ttm:agent="singerLead">
                        <span begin="2s" end="3s">Lead on left</span>
                    </p>
                </body>
            </tt>
        """.trimIndent()
        val result = LyricsDecoder.decodeWithTiming(xml, 4000)
        assertEquals(setOf(0), result.duetRightLines)
    }

}
