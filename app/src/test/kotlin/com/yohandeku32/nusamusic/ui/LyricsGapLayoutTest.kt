package com.yohandeku32.nusamusic.ui

import com.mocharealm.accompanist.lyrics.core.model.ISyncedLine
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeAlignment
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeSyllable
import com.mocharealm.accompanist.lyrics.core.model.synced.SyncedLine
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs

class LyricsGapLayoutTest {
    private fun line(start: Int, end: Int) = SyncedLine("Lyrics", null, start, end)
    private fun rowForGap(lines: List<ISyncedLine>, now: Int): Pair<LyricsGap, Int> {
        val gap = requireNotNull(LyricsTimeline(lines).frame(now).gap)
        return gap to LyricsDisplayRows(lines).lineToRow[gap.nextLine]
    }

    @Test fun introAndMidSongCountdownsBothReserveSpaceAboveTheUpcomingRow() {
        val intro = rowForGap(listOf(line(5000, 6000)), 2000)
        val middle = rowForGap(listOf(line(1000, 2000), line(8000, 9000)), 4000)
        assertEquals(0, intro.second)
        assertEquals(1, middle.second)
        assertEquals(1f, countdownLayoutFraction(intro.first, 2000), 0f)
        assertEquals(1f, countdownLayoutFraction(middle.first, 4000), 0f)
    }
    @Test fun intraLineAndBackingGapsHaveARealContainingRowToo() {
        val internal = KaraokeLine(listOf(KaraokeSyllable("Before", 1000, 2000),
            KaraokeSyllable("After", 8000, 9000)), null, false, KaraokeAlignment.Start, 1000, 9000)
        assertEquals(0, rowForGap(listOf(internal), 4000).second)
        val backing = KaraokeLine(listOf(KaraokeSyllable("Back", 8000, 9000)),
            null, true, KaraokeAlignment.Start, 8000, 9000)
        assertEquals(0, rowForGap(listOf(line(1000, 2000), backing), 4000).second)
    }
    @Test fun countdownPushesTextAndFollowingRowsDownWithClearanceForDots() {
        val gap = LyricsGap(2000, 8000, 1)
        val height = 64f * countdownLayoutFraction(gap, 4000)
        val dotBottom = height / 2f + 5f
        val textTop = height + 8f
        assertTrue(textTop - dotBottom >= 24f)
        val nextRowWithoutCountdown = 80f
        assertEquals(144f, nextRowWithoutCountdown + height, 0f)
        assertEquals(32f, countdownRowAnchor(80f, 64f, 1f), 0f)
        assertEquals(32f, countdownRowAnchor(200f, 64f, 1f), 0f)
    }
    @Test fun countdownSpaceCollapsesBeforeVocalsResumeAndNeverLingersAfterward() {
        val gap = LyricsGap(2000, 8000, 1)
        assertEquals(0f, countdownLayoutFraction(gap, 1900), 0f)
        assertEquals(0f, countdownLayoutFraction(gap, 2000), 0f)
        assertEquals(0.5f, countdownLayoutFraction(gap, 2060), 0.001f)
        assertEquals(0.5f, countdownLayoutFraction(gap, 7780), 0.001f)
        assertEquals(0f, countdownLayoutFraction(gap, 8000), 0f)
        assertEquals(0f, countdownLayoutFraction(gap, 9000), 0f)
        assertEquals(40f, countdownRowAnchor(80f, 64f, 0f), 0f)
    }
    @Test fun pauseHoldsSpacingAndSeekRecomputesItWithoutAQueuedTransition() {
        val gap = LyricsGap(2000, 8000, 1)
        val held = countdownLayoutFraction(gap, 2060)
        repeat(120) { assertEquals(held, countdownLayoutFraction(gap, 2060), 0f) }
        assertEquals(0f, countdownLayoutFraction(gap, 9000), 0f)
        assertEquals(1f, countdownLayoutFraction(gap, 4000), 0f)
        assertEquals(0f, countdownLayoutFraction(gap, 1000), 0f)
    }
    @Test fun heightAndFollowAnchorBlendContinuouslyAt60_90_120Hz() {
        val gap = LyricsGap(2000, 8000, 1)
        for (hz in listOf(60, 90, 120)) {
            var lastHeight = 0f
            var lastAnchor = 80f
            for (frame in 0..hz * 9) {
                val now = (frame * 1000.0 / hz).toInt()
                val fraction = countdownLayoutFraction(gap, now)
                val height = 64f * fraction
                val anchor = countdownRowAnchor(160f, 64f, fraction)
                assertTrue(abs(height - lastHeight) < 14f)
                assertTrue(abs(anchor - lastAnchor) < 14f)
                lastHeight = height; lastAnchor = anchor
            }
            assertEquals(0f, lastHeight, 0f)
            assertEquals(80f, lastAnchor, 0f)
        }
    }
}
