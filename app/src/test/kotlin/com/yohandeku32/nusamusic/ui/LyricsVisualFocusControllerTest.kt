package com.yohandeku32.nusamusic.ui

import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeAlignment
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeSyllable
import com.mocharealm.accompanist.lyrics.core.model.synced.SyncedLine
import kotlin.math.abs
import org.junit.Assert.*
import org.junit.Test

class LyricsVisualFocusControllerTest {
    private fun line(start: Int, end: Int) = SyncedLine("Line", null, start, end)
    private fun target(plan: LyricsVisualFocusController) =
        plan.fromSource + (plan.toSource - plan.fromSource) * plan.blend

    @Test fun fastRowsUseOneContinuousForwardTargetAndCalmerSpringResponse() {
        val lines = (0..15).map { line(1000 + it * 200, 1200 + it * 200) }
        val plan = LyricsVisualFocusController(lines)
        val timeline = LyricsTimeline(lines)
        for (hz in listOf(60, 90, 120)) {
            var previous = 0f
            for (frame in 0..hz * 5) {
                val now = (frame * 1000.0 / hz).toInt()
                plan.update(now)
                val current = target(plan)
                assertTrue(current >= previous - 0.0001f)
                assertTrue(abs(current - previous) < 0.24f)
                assertTrue(plan.responsiveness in 0.78f..0.85f)
                if (now in 1000..4199) assertTrue(timeline.frame(now).active.isNotEmpty())
                previous = current
            }
        }
    }

    @Test fun lookAheadMovesTheTargetButDoesNotActivateTheNextLyric() {
        val lines = listOf(line(1000, 4000), line(4000, 8000))
        val plan = LyricsVisualFocusController(lines)
        plan.update(3800)
        assertTrue(plan.blend > 0f && plan.blend < 1f)
        assertEquals(setOf(0), LyricsTimeline(lines).frame(3800).active)
        assertTrue(plan.focusAmount(1, 3800) < 1f)
        assertEquals(1f, plan.focusAmount(0, 3800), 0f)
    }

    @Test fun completedNewerLinesNeverPullTheVisualAnchorBackToAnOlderOverlap() {
        val lines = listOf(line(1000, 9000), line(2000, 3000), line(7000, 10000))
        val plan = LyricsVisualFocusController(lines)
        plan.update(4000)
        assertEquals(1, plan.fromSource)
        assertEquals(1, plan.toSource)
        assertEquals(0, LyricsTimeline(lines).frame(4000).anchor)
        assertEquals(1f, plan.focusAmount(0, 4000), 0f)
    }

    @Test fun simultaneousDuetsAndBackingDoNotStealMainFocus() {
        val backing = KaraokeLine(listOf(KaraokeSyllable("Oh", 1500, 7000)), null, true,
            KaraokeAlignment.Start, 1500, 7000)
        val lines = listOf(line(1000, 4000), line(1000, 5000), backing, line(6000, 9000))
        val plan = LyricsVisualFocusController(lines)
        plan.update(2500)
        assertEquals(0, plan.fromSource)
        assertEquals(0, plan.toSource)
        assertEquals(1f, plan.focusAmount(0, 2500), 0f)
        assertEquals(1f, plan.focusAmount(1, 2500), 0f)
        assertNull(LyricsTimeline(lines).frame(5000).gap)
    }

    @Test fun instrumentalBreaksHoldPositionUntilTheNextLineApproaches() {
        val plan = LyricsVisualFocusController(listOf(line(1000, 2000), line(10000, 12000)))
        for (now in listOf(2500, 5000, 9000)) {
            plan.update(now)
            assertEquals(0f, target(plan), 0f)
        }
        plan.update(9800)
        assertTrue(target(plan) > 0f)
    }

    @Test fun countdownRejoinsTheIncomingRowWithoutAPostGapBackwardCorrection() {
        val lines = listOf(line(1000, 2000), line(10000, 12000))
        val plan = LyricsVisualFocusController(lines)
        for (now in 10000..10250 step 10) {
            plan.update(now)
            assertEquals(1f, target(plan), 0f)
        }
        plan.update(9999)
        assertTrue(target(plan) > 0.999f)
    }

    @Test fun activeFocusIsStableAcrossSyllablesAndFastIncomingRowsStayBlurredUntilNearOnset() {
        val plan = LyricsVisualFocusController((0..5).map { line(1000 + it * 200, 1200 + it * 200) })
        assertEquals(0f, plan.focusAmount(3, 1200), 0f)
        assertTrue(plan.focusAmount(3, 1550) in 0f..1f)
        for (now in 1600..1799) assertEquals(1f, plan.focusAmount(3, now), 0f)
        assertEquals(0f, plan.focusAmount(3, 2040), 0f)
        assertEquals(0f, plan.focusAmount(-1, 1600), 0f)
    }

    @Test fun pauseAndSeekHaveNoOldTransitionHistory() {
        val plan = LyricsVisualFocusController(listOf(line(1000, 4000), line(4000, 8000)))
        plan.update(3800)
        val expected = listOf(plan.fromSource.toFloat(), plan.toSource.toFloat(), plan.blend, plan.responsiveness)
        for (now in listOf(3800, 10000, 0, 3800, 6000, 3800)) {
            plan.update(now)
            if (now == 3800) assertEquals(expected,
                listOf(plan.fromSource.toFloat(), plan.toSource.toFloat(), plan.blend, plan.responsiveness))
        }
        val empty = LyricsVisualFocusController(emptyList())
        empty.update(1000)
        assertEquals(-1, empty.fromSource)
    }

    @Test fun adaptiveSpringFollowsRapidTargetsWithoutReversalsOrLargeLag() {
        val plan = LyricsVisualFocusController((0..20).map { line(1000 + it * 200, 1200 + it * 200) })
        val motion = LyricsFollowMotion()
        var position = 0f
        for (now in 0..6000 step 16) {
            plan.update(now)
            val desired = target(plan) * 60f
            val step = motion.step(desired - position, 16, plan.responsiveness)
            assertTrue(step >= -0.1f)
            position += step
            assertTrue(abs(desired - position) < 100f)
        }
        assertEquals(1200f, position, 0.5f)
    }

    @Test fun rapidLineBlurAndOpacityUseTheSameSmoothFocusAtEveryRefreshRate() {
        val lines = (0..5).map { line(1000 + it * 200, 1200 + it * 200) }
        val plan = LyricsVisualFocusController(lines)
        for (hz in listOf(60, 90, 120)) {
            var previousBlur = 5.5f
            for (frame in 0..hz * 3) {
                val now = (frame * 1000.0 / hz).toInt()
                val focus = plan.focusAmount(3, now)
                val depth = lyricsMainFocusDepth(315f, 1600, 1800, now, focus)
                assertTrue(abs(depth.blurDp - previousBlur) < 0.6f)
                assertEquals(depth.blurDp / 5.5f, (1f - depth.opacity) / 0.65f, 0.0001f)
                previousBlur = depth.blurDp
            }
        }
    }
}
