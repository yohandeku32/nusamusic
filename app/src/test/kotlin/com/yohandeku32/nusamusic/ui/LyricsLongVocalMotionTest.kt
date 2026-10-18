package com.yohandeku32.nusamusic.ui

import kotlin.math.abs
import org.junit.Assert.*
import org.junit.Test

class LyricsLongVocalMotionTest {
    @Test fun shortSyllablesAndUnsungTextRetainExactlyTheOriginalMotion() {
        for (duration in listOf(100, 350, 650, 700)) {
            val bubble = LyricsLongVocalMotion(1000, 1000 + duration, 150f)
            for (now in 800..(1300 + duration) step 20) {
                bubble.update(now, 1f)
                assertEquals(1f, bubble.scaleX, 0f)
                assertEquals(1f, bubble.scaleY, 0f)
                assertEquals(0f, bubble.offsetXDp, 0f)
                assertEquals(0f, bubble.offsetYDp, 0f)
                assertEquals(0f, bubble.glowBoost, 0f)
            }
        }
        val long = LyricsLongVocalMotion(1000, 8500, 150f)
        long.update(4500, 0f)
        assertEquals(1f, long.scaleX, 0f)
        assertEquals(1f, long.scaleY, 0f)
        assertEquals(0f, long.glowBoost, 0f)
    }

    @Test fun heldVocalHasSlowBubbleBreathingAndSoftResonance() {
        val bubble = LyricsLongVocalMotion(1000, 11000, 150f)
        val samples = (3000..9000 step 100).map { now ->
            bubble.update(now, 1f)
            assertTrue(bubble.scaleX in 1f..1.0241f)
            assertTrue(bubble.scaleY in 1f..1.0261f)
            assertTrue(abs(bubble.offsetXDp) <= 0.191f)
            assertTrue(abs(bubble.offsetYDp) <= 0.311f)
            assertTrue(bubble.glowBoost in 0f..1f)
            bubble.scaleY
        }
        assertTrue("Held vowels must actually breathe", samples.maxOrNull()!! - samples.minOrNull()!! > 0.02f)
        assertTrue("The period must feel slow, not like jitter",
            samples.zipWithNext().all { (a, b) -> abs(a - b) < 0.003f })
    }

    @Test fun continuousGlobalPhaseVariesGentlyAcrossTextPositions() {
        val left = LyricsLongVocalMotion(1000, 10000, 100f)
        val right = LyricsLongVocalMotion(1000, 10000, 190f)
        left.update(5100, 1f)
        right.update(5100, 1f)
        assertTrue(abs(left.scaleX - right.scaleX) in 0.0001f..0.008f)
        assertTrue(abs(left.offsetYDp - right.offsetYDp) < 0.3f)
    }

    @Test fun envelopeBuildsAndReleasesWithoutSuddenStartOrFinish() {
        val bubble = LyricsLongVocalMotion(1000, 8000, 75f)
        val samples = (900..8100 step 10).map { now ->
            bubble.update(now, 1f)
            Triple(bubble.scaleY, bubble.offsetYDp, bubble.glowBoost)
        }
        assertEquals(Triple(1f, 0f, 0f), samples.first())
        assertEquals(Triple(1f, 0f, 0f), samples.last())
        assertTrue(samples.zipWithNext().all { (a, b) ->
            abs(a.first - b.first) < 0.002f &&
                abs(a.second - b.second) < 0.04f &&
                abs(a.third - b.third) < 0.06f
        })
    }

    @Test fun 60_90_120HzPlaybackSamplingStaysWithinSubtleFrameDeltas() {
        for (hz in listOf(60, 90, 120)) {
            val bubble = LyricsLongVocalMotion(1000, 9500, 135f)
            val wave = LyricsFloatingMotion(135f)
            var lastX = 0f
            var lastY = 0f
            var lastScaleX = 1f
            var lastScaleY = 1f
            for (frame in 0..hz * 11) {
                val now = (frame * 1000.0 / hz).toInt()
                val influence = lyricsLineWaveIntensity(1000, 9500, now)
                wave.update(now, influence)
                bubble.update(now, influence)
                val x = bubble.offsetXDp
                val y = bubble.offsetYDp - wave.liftDp
                val sx = wave.scale * bubble.scaleX
                val sy = wave.scale * bubble.scaleY
                assertTrue(sx in 1f..1.045f)
                assertTrue(sy in 1f..1.045f)
                assertTrue(abs(x - lastX) < 0.05f)
                assertTrue(abs(y - lastY) < 0.19f)
                assertTrue(abs(sx - lastScaleX) < 0.004f)
                assertTrue(abs(sy - lastScaleY) < 0.004f)
                lastX = x
                lastY = y
                lastScaleX = sx
                lastScaleY = sy
            }
        }
    }

    @Test fun pauseAndSeekReplayPreciselyTheSamePose() {
        val bubble = LyricsLongVocalMotion(1000, 10000, 135f)
        bubble.update(5400, 0.87f)
        val expected = listOf(
            bubble.scaleX, bubble.scaleY, bubble.offsetXDp, bubble.offsetYDp,
            bubble.glowBoost
        )
        repeat(120) {
            bubble.update(5400, 0.87f)
            assertEquals(expected, listOf(
                bubble.scaleX, bubble.scaleY, bubble.offsetXDp,
                bubble.offsetYDp, bubble.glowBoost
            ))
        }
        bubble.update(8000, 0.5f)
        bubble.update(5400, 0.87f)
        assertEquals(expected, listOf(
            bubble.scaleX, bubble.scaleY, bubble.offsetXDp,
            bubble.offsetYDp, bubble.glowBoost
        ))
    }

    @Test fun longVocalGlowFadesWithTheBubbleEnvelope() {
        val bubble = LyricsLongVocalMotion(1000, 9000, 40f)
        bubble.update(1000, 1f)
        assertEquals(0f, bubble.glowBoost, 0f)
        bubble.update(4500, 1f)
        assertTrue(bubble.glowBoost > 0f)
        bubble.update(8980, 1f)
        assertTrue(bubble.glowBoost < 0.015f)
        bubble.update(9000, 1f)
        assertEquals(0f, bubble.glowBoost, 0f)
        assertEquals(1f, bubble.scaleX, 0f)
    }

    @Test fun onlyAudioTimeControlsThePulseAcrossSyllableBoundaries() {
        // The two instances have different authored timing but, during their
        // sustained portions, share the same global bubble carrier phase.
        val a = LyricsLongVocalMotion(1000, 15000, 100f)
        val b = LyricsLongVocalMotion(2000, 16000, 100f)
        a.update(7200, 1f)
        b.update(7200, 1f)
        assertEquals(a.scaleX, b.scaleX, 0f)
        assertEquals(a.scaleY, b.scaleY, 0f)
        assertEquals(a.offsetYDp, b.offsetYDp, 0f)
    }
}
