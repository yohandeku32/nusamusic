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
        assertEquals(LyricsFrame(0, emptySet()), timeline.frame(5_000))
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
        assertEquals(LyricsFrame(0, emptySet()), LyricsTimeline(listOf(line(5_000, 6_000))).frame(0))
    }
    @Test fun largeLyricsSupportDistantSeeksInBothDirections() {
        val timeline = LyricsTimeline(List(8_000) { line(it * 1_000, (it + 1) * 1_000) })
        assertEquals(LyricsFrame(7_900, setOf(7_900)), timeline.frame(7_900_500))
        assertEquals(LyricsFrame(10, setOf(10)), timeline.frame(10_500))
    }
}
