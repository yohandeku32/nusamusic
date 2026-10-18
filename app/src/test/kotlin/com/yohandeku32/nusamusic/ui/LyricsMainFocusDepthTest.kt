package com.yohandeku32.nusamusic.ui

import kotlin.math.abs
import org.junit.Assert.*
import org.junit.Test

class LyricsMainFocusDepthTest {
    @Test fun inactiveDepthMatchesNearFarAndFarthestTargetsInBothDirections() {
        for ((distance, blur, opacity) in listOf(Triple(105f, 2.5f, 0.70f),
            Triple(210f, 4.5f, 0.48f), Triple(315f, 5.5f, 0.35f))) {
            for (sign in listOf(-1, 1)) {
                val upcoming = lyricsMainFocusDepth(distance * sign, 10000, 12000, 0)
                val previous = lyricsMainFocusDepth(distance * sign, 0, 1000, 10000)
                assertEquals(blur, upcoming.blurDp, 0.0001f)
                assertEquals(opacity, upcoming.opacity, 0.0001f)
                assertEquals(upcoming, previous)
            }
        }
        assertEquals(lyricsMainFocusDepth(315f, 0, 1000, 10000),
            lyricsMainFocusDepth(10000f, 0, 1000, 10000))
    }

    @Test fun activeAndOverlappingLinesStaySharpEvenWhenScrolledAway() {
        for (distance in listOf(-1000f, -105f, 0f, 210f, 1000f)) {
            for ((start, end) in listOf(1000 to 4000, 2000 to 5000)) {
                for (now in listOf(start, 2500, end - 1)) {
                    assertEquals(LyricsFocusDepth(0f, 1f), lyricsMainFocusDepth(distance, start, end, now))
                }
            }
        }
    }

    @Test fun focusPullFinishesAtOnsetAndReleasesOver420ms() {
        assertEquals(0f, lyricsMainFocusAmount(1000, 2000, 580), 0f)
        assertEquals(0.5f, lyricsMainFocusAmount(1000, 2000, 790), 0.0001f)
        assertEquals(1f, lyricsMainFocusAmount(1000, 2000, 1000), 0f)
        assertEquals(1f, lyricsMainFocusAmount(1000, 2000, 2000), 0f)
        assertEquals(0.5f, lyricsMainFocusAmount(1000, 2000, 2210), 0.0001f)
        assertEquals(0f, lyricsMainFocusAmount(1000, 2000, 2420), 0f)
        for (now in 580..2420) {
            val depth = lyricsMainFocusDepth(315f, 1000, 2000, now)
            // Opacity and blur use the same interpolation, without separate clocks.
            assertEquals(depth.blurDp / 5.5f, (1f - depth.opacity) / 0.65f, 0.00001f)
        }
    }

    @Test fun scrollingHasContinuousBoundedDepthWithNoRowIndexSwitches() {
        var previous = lyricsMainFocusDepth(0f, 10000, 12000, 0)
        for (step in 1..4000) {
            val current = lyricsMainFocusDepth(step / 10f, 10000, 12000, 0)
            assertTrue(current.blurDp >= previous.blurDp)
            assertTrue(current.opacity <= previous.opacity)
            assertTrue(abs(current.blurDp - previous.blurDp) < 0.003f)
            assertTrue(abs(current.opacity - previous.opacity) < 0.001f)
            assertTrue(current.blurDp in 2.5f..5.5f)
            assertTrue(current.opacity in 0.3499f..0.70f)
            previous = current
        }
    }

    @Test fun focusIsContinuousAcrossBothVocalBoundariesAtCommonRefreshRates() {
        for (hz in listOf(60, 90, 120, 144)) {
            var previous = lyricsMainFocusDepth(315f, 1000, 2000, 0)
            for (frame in 1..hz * 3) {
                val current = lyricsMainFocusDepth(315f, 1000, 2000, frame * 1000 / hz)
                assertTrue(abs(current.blurDp - previous.blurDp) < 0.34f)
                assertTrue(abs(current.opacity - previous.opacity) < 0.04f)
                previous = current
            }
        }
    }

    @Test fun pauseAndRapidSeeksHaveNoAnimationHistoryToReplay() {
        val held = lyricsMainFocusDepth(210f, 1000, 2000, 790)
        repeat(120) { assertEquals(held, lyricsMainFocusDepth(210f, 1000, 2000, 790)) }
        for (now in listOf(50000, 1500, 0, 2200, 1500, 790)) {
            val depth = lyricsMainFocusDepth(210f, 1000, 2000, now)
            if (now == 1500) assertEquals(LyricsFocusDepth(0f, 1f), depth)
            if (now == 790) assertEquals(held, depth)
        }
        assertEquals(0f, lyricsMainFocusAmount(Int.MAX_VALUE - 100, Int.MAX_VALUE, Int.MIN_VALUE), 0f)
        assertEquals(0f, lyricsMainFocusAmount(Int.MIN_VALUE, Int.MIN_VALUE + 100, Int.MAX_VALUE), 0f)
    }

    @Test fun malformedTimingAndNonFiniteDistanceCannotCreateInvalidEffects() {
        assertEquals(0f, lyricsMainFocusAmount(2000, 1000, 1500), 0f)
        assertEquals(0f, lyricsMainFocusAmount(1000, 1000, 1000), 0f)
        for (distance in listOf(Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY)) {
            assertEquals(LyricsFocusDepth(2.5f, 0.70f), lyricsMainFocusDepth(distance, 1000, 2000, 0))
            assertEquals(LyricsFocusDepth(0f, 1f), lyricsMainFocusDepth(distance, 1000, 2000, 1500))
        }
    }
}
