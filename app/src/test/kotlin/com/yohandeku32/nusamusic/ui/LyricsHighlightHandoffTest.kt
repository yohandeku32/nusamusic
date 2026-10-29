package com.yohandeku32.nusamusic.ui

import kotlin.math.abs
import org.junit.Assert.*
import org.junit.Test

class LyricsHighlightHandoffTest {
    private fun pair(gap: Int = 0) = listOf(
        LyricsHeldWordUnit("你", 1000, 1400, 0f, 34f),
        LyricsHeldWordUnit("好", 1400 + gap, 1800 + gap, 40f, 74f)
    )

    @Test fun adjacentCharactersShareTheSameSoftFrontBeforeAndAfterTheBoundary() {
        val handoff = LyricsHighlightHandoff(pair())
        assertEquals(requireNotNull(handoff.edgeDp(0, 1400)),
            requireNotNull(handoff.edgeDp(1, 1400)) + 40f, 0.00001f)
        val incoming = requireNotNull(handoff.edgeDp(1, 1380))
        assertTrue("The leading shoulder must reach the next character before onset",
            incoming > -handoff.featherDp(1) && incoming < 0f)
        val outgoing = requireNotNull(handoff.edgeDp(0, 1420))
        assertTrue("The previous character must finish gradually after the handoff",
            outgoing > 34f && outgoing < 34f + handoff.featherDp(0))
    }

    @Test fun theCursorDoesNotJumpReverseOrRestartAtAnyTransitionEdge() {
        for (gap in listOf(0, 20, 40)) {
            val handoff = LyricsHighlightHandoff(pair(gap))
            for (index in 0..1) {
                var previous = requireNotNull(handoff.edgeDp(index, 1200))
                for (now in 1201..1650) {
                    val edge = requireNotNull(handoff.edgeDp(index, now))
                    assertTrue("Cursor reversed at $now", edge >= previous - 0.0001f)
                    assertTrue("Cursor jumped at $now", edge - previous < 0.25f)
                    previous = edge
                }
                val window = minOf(90 - gap, 80)
                for (join in listOf(1400 - window, 1400 + gap + window)) {
                    val left = requireNotNull(handoff.edgeDp(index, join)) -
                        requireNotNull(handoff.edgeDp(index, join - 1))
                    val right = requireNotNull(handoff.edgeDp(index, join + 1)) -
                        requireNotNull(handoff.edgeDp(index, join))
                    assertEquals("Velocity must match across $join", left, right, 0.003f)
                }
            }
        }
    }

    @Test fun restsOverlapsInferredTimingAndDistantGroupsKeepTheExistingFill() {
        for (next in listOf(pair(46)[1], pair()[1].copy(startMs = 1399),
            pair()[1].copy(reliableHold = false), pair()[1].copy(pathStartDp = 90f, pathEndDp = 124f))) {
            val handoff = LyricsHighlightHandoff(listOf(pair()[0], next))
            for (now in 1000..1900 step 20) {
                assertNull(handoff.edgeDp(0, now))
                assertNull(handoff.edgeDp(1, now))
            }
        }
    }

    @Test fun longHoldsCannotAnticipateTheNextWordUntilTheLast90ms() {
        val units = listOf(LyricsHeldWordUnit("長", 1000, 6000, 0f, 34f),
            LyricsHeldWordUnit("い", 6000, 7000, 34f, 68f))
        val handoff = LyricsHighlightHandoff(units)
        assertTrue(requireNotNull(handoff.edgeDp(1, 5909)) <= -handoff.featherDp(1))
        val held = LyricsHeldWordWave(units)
        assertFalse(held.hasHold(1))
        for (now in 1000..7000 step 8) {
            assertEquals(0f, held.pose(1, now, 51f).depth, 0f)
            assertEquals(0f, held.letterPose(1, now, 51f).liftDp, 0f)
        }
    }

    @Test fun pauseSeekAndFrameRateNeverChangeTheSampleOrTiming() {
        val units = pair()
        val handoff = LyricsHighlightHandoff(units)
        val target = handoff.edgeDp(1, 1380)
        repeat(30) { assertEquals(target, handoff.edgeDp(1, 1380)) }
        for (hz in listOf(60, 90, 120)) {
            var previous = requireNotNull(handoff.edgeDp(1, 1000))
            for (frame in 1..hz) {
                val now = 1000 + (frame * 1000.0 / hz).toInt()
                val next = requireNotNull(handoff.edgeDp(1, now))
                assertTrue("The highlight shoulder must advance gradually at $hz Hz / $now",
                    abs(next - previous) / (2f * handoff.featherDp(1)) < 0.16f)
                previous = next
            }
            assertEquals(target, handoff.edgeDp(1, 1380))
        }
        assertEquals(1400, units[1].startMs)
        assertEquals(1800, units[1].endMs)
    }

    @Test fun mixedLatinWidthsAndWrappedPathsRemainMonotoneWithBoundedAnticipation() {
        val units = listOf(
            LyricsHeldWordUnit("aku ", 1000, 1750, 0f, 60f),
            LyricsHeldWordUnit("menyanyikan ", 1750, 2800, 65f, 245f),
            LyricsHeldWordUnit("ini", 2800, 3100, 257f, 287f)
        )
        val handoff = LyricsHighlightHandoff(units)
        for ((index, unit) in units.withIndex()) {
            var previous = -handoff.featherDp(index)
            for (now in 900..3300) {
                val edge = requireNotNull(handoff.edgeDp(index, now))
                assertTrue("Sweep reversed in ${unit.content} at $now", edge >= previous - 0.0001f)
                if (now < unit.startMs - 90) assertTrue(edge <= -handoff.featherDp(index))
                if (now > unit.endMs + 90) assertTrue(edge >= unit.pathEndDp - unit.pathStartDp + handoff.featherDp(index))
                previous = edge
            }
        }
    }

    @Test fun shortRestsCountTowardsThe90msLimitOnBothSides() {
        for (gap in listOf(0, 20, 40)) {
            val handoff = LyricsHighlightHandoff(pair(gap))
            assertEquals(-handoff.featherDp(1), handoff.edgeDp(1, 1400 + gap - 91))
            assertEquals(34f + handoff.featherDp(0), handoff.edgeDp(0, 1491))
        }
    }
}
