package com.yohandeku32.nusamusic.ui

import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeAlignment
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeSyllable
import com.mocharealm.accompanist.lyrics.core.model.synced.SyncedLine
import org.junit.Assert.*
import org.junit.Test

class LyricsTimelineTest {
    private fun line(start: Int, end: Int) = SyncedLine("Line", null, start, end)
    private fun karaoke(start: Int, end: Int, background: Boolean = false) = KaraokeLine(
        listOf(KaraokeSyllable("Word", start, end)), null, background, KaraokeAlignment.Start, start, end)

    @Test fun overlapWithNonMonotonicEndTimesDoesNotLoseAnActiveVoice() {
        val timeline = LyricsTimeline(listOf(line(0, 100_000), line(1_000, 2_000), line(3_000, 4_000)))
        assertEquals(setOf(0, 2), timeline.frame(3_500).active)
        assertEquals(2, timeline.frame(3_500).anchor)
    }
    @Test fun gapHoldsThePreviousLineUntilTheNextOneStarts() {
        val timeline = LyricsTimeline(listOf(line(1_000, 2_000), line(8_000, 10_000)))
        assertEquals(LyricsFrame(0, emptySet(), LyricsGap(2_000, 8_000, 1)), timeline.frame(5_000))
        assertEquals(LyricsFrame(1, setOf(1)), timeline.frame(8_000))
    }
    @Test fun seekingBackwardRecomputesFocusWithoutRememberingTheOldIndex() {
        val timeline = LyricsTimeline(listOf(line(0, 1_000), line(1_000, 2_000), line(2_000, 3_000)))
        assertEquals(LyricsFrame(2, setOf(2)), timeline.frame(2_500))
        assertEquals(LyricsFrame(0, setOf(0)), timeline.frame(500))
    }
    @Test fun sameStartDuetAnchorsTheFirstVoiceAndHighlightsBoth() {
        val timeline = LyricsTimeline(listOf(karaoke(1_000, 4_000), karaoke(1_000, 4_000), karaoke(2_000, 3_000, true)))
        assertEquals(LyricsFrame(0, setOf(0, 1, 2)), timeline.frame(2_500))
    }
    @Test fun backgroundVoiceDoesNotMoveTheAnchorAwayFromItsMainLine() {
        val timeline = LyricsTimeline(listOf(karaoke(0, 2_000), karaoke(1_000, 4_000, true)))
        assertEquals(LyricsFrame(0, setOf(1)), timeline.frame(3_000))
    }
    @Test fun exactEndDoesNotHighlightThePreviousLine() {
        val timeline = LyricsTimeline(listOf(line(0, 1_000), line(1_000, 2_000)))
        assertEquals(LyricsFrame(1, setOf(1)), timeline.frame(1_000))
    }
    @Test fun emptyAndIntroStatesHaveSafeAnchors() {
        assertEquals(LyricsFrame(-1, emptySet()), LyricsTimeline(emptyList()).frame(0))
        assertEquals(LyricsFrame(0, emptySet(), LyricsGap(0, 5_000, 0)), LyricsTimeline(listOf(line(5_000, 6_000))).frame(0))
    }
    @Test fun largeLyricsSupportDistantSeeksInBothDirections() {
        val timeline = LyricsTimeline(List(8_000) { line(it * 1_000, (it + 1) * 1_000) })
        assertEquals(LyricsFrame(7_900, setOf(7_900)), timeline.frame(7_900_500))
        assertEquals(LyricsFrame(10, setOf(10)), timeline.frame(10_500))
    }
    @Test fun introAndBreakCountdownEndExactlyWhenSingingStarts() {
        val timeline = LyricsTimeline(listOf(line(5_000, 7_000), line(12_000, 14_000)))
        assertEquals(LyricsGap(0, 5_000, 0), timeline.frame(4_999).gap)
        assertNull(timeline.frame(5_000).gap)
        assertEquals(setOf(0), timeline.frame(5_000).active)
        assertEquals(LyricsGap(7_000, 12_000, 1), timeline.frame(7_000).gap)
        assertTrue(timeline.frame(7_000).active.isEmpty())
        assertNull(timeline.frame(12_000).gap)
        assertEquals(setOf(1), timeline.frame(12_000).active)
    }
    @Test fun backgroundAndOverlappingVoicesSuppressFalseCountdowns() {
        val timeline = LyricsTimeline(listOf(karaoke(0, 2_000), karaoke(1_000, 9_000, true), karaoke(12_000, 14_000)))
        assertNull(timeline.frame(5_000).gap)
        assertEquals(setOf(1), timeline.frame(5_000).active)
        assertEquals(LyricsGap(9_000, 12_000, 2), timeline.frame(10_000).gap)
    }
    @Test fun shortSilenceIsInactiveWithoutAFlashingCountdown() {
        val timeline = LyricsTimeline(listOf(line(0, 2_000), line(4_000, 5_000)))
        assertTrue(timeline.frame(3_000).active.isEmpty())
        assertNull(timeline.frame(3_000).gap)
    }
    @Test fun seekRecomputesBreakAndOutroNeverCountsDownToANonexistentLine() {
        val timeline = LyricsTimeline(listOf(line(5_000, 7_000), line(12_000, 14_000)))
        assertNull(timeline.frame(20_000).gap)
        assertTrue(timeline.frame(20_000).active.isEmpty())
        assertEquals(LyricsGap(7_000, 12_000, 1), timeline.frame(10_000).gap)
        assertEquals(LyricsGap(0, 5_000, 0), timeline.frame(1_000).gap)
    }
    @Test fun authoredSilenceInsideOneLineUsesUnitTimingRatherThanContainerDuration() {
        val main = KaraokeLine(listOf(KaraokeSyllable("Before", 1000, 2000), KaraokeSyllable("After", 7000, 8000)),
            null, false, KaraokeAlignment.Start, 1000, 8000)
        assertEquals(LyricsGap(2000, 7000, 0), LyricsTimeline(listOf(main)).frame(4000).gap)
        assertNull(LyricsTimeline(listOf(main)).frame(7000).gap)
        // An overlapping backing voice means this is not an instrumental break.
        assertNull(LyricsTimeline(listOf(main, karaoke(3000, 6000, true))).frame(4000).gap)
    }
}
