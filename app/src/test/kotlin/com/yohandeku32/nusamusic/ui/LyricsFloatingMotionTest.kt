package com.yohandeku32.nusamusic.ui

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs

class LyricsFloatingMotionTest {
    @Test fun sharedWaveNeverRestartsWhenActiveSyllableChanges() {
        val motion = LyricsFloatingMotion(horizontalDp = 100f)
        // An authored word boundary at 3000 does not appear in the carrier API.
        val samples = (2900..3100 step 10).map { time ->
            motion.update(time, lyricsLineWaveIntensity(1000, 8000, time))
            motion.liftDp
        }
        assertTrue(samples.zipWithNext().all { (a, b) -> abs(a - b) < 0.055f })
        assertTrue(samples.any { abs(it) > 0.5f })
    }

    @Test fun nearbyGlyphsSharePhaseButTheRowBendsRatherThanBouncingAsABlock() {
        val left = LyricsFloatingMotion(horizontalDp = 20f)
        val middle = LyricsFloatingMotion(horizontalDp = 145f)
        val right = LyricsFloatingMotion(horizontalDp = 280f)
        val intensity = lyricsLineWaveIntensity(1000, 8000, 4000)
        left.update(4000, intensity)
        middle.update(4000, intensity)
        right.update(4000, intensity)
        assertTrue(abs(left.liftDp - middle.liftDp) > 0.1f)
        assertTrue(abs(right.liftDp - middle.liftDp) > 0.1f)
        assertTrue(abs(left.liftDp) <= 2.5f)
        assertTrue(abs(middle.liftDp) <= 2.5f)
        assertTrue(abs(right.liftDp) <= 2.5f)
        // Neighbouring letters are only a few degrees apart.
        val neighbour = LyricsFloatingMotion(horizontalDp = 150f)
        neighbour.update(4000, intensity)
        assertTrue(abs(neighbour.liftDp - middle.liftDp) < 0.1f)
    }

    @Test fun longHeldNoteBreathesUsingOneUninterruptedGlobalTimeline() {
        val motion = LyricsFloatingMotion(horizontalDp = 160f)
        val intensity = lyricsLineWaveIntensity(1000, 14000, 7000)
        assertEquals(1f, intensity, 0.0001f)
        motion.update(7000, intensity)
        val lift = motion.liftDp
        val scale = motion.scale
        motion.update(8500, intensity)
        assertNotEquals(lift, motion.liftDp, 0.02f)
        assertNotEquals(scale, motion.scale, 0.001f)
        assertTrue(motion.scale in 1f..1.02f)
        assertTrue(abs(motion.liftDp) <= 2.5f)
    }

    @Test fun adjacentLinesCrossfadeTheirIntensityWithoutChangingWavePhase() {
        val previous = LyricsFloatingMotion(100f)
        val next = LyricsFloatingMotion(100f)
        val boundary = 5000
        for (time in 4800..5200 step 10) {
            val old = lyricsLineWaveIntensity(1000, boundary, time)
            val fresh = lyricsLineWaveIntensity(boundary, 9000, time)
            previous.update(time, old)
            next.update(time, fresh)
            assertEquals(1f, old + fresh, 0.001f)
            assertTrue(abs(previous.liftDp + next.liftDp) <= 2.5f)
        }
        assertEquals(0.5f, lyricsLineWaveIntensity(1000, boundary, boundary), 0.0001f)
        assertEquals(0.5f, lyricsLineWaveIntensity(boundary, 9000, boundary), 0.0001f)
    }

    @Test fun pauseHoldsPhaseAndSeekingRecomputesItWithoutReplay() {
        val motion = LyricsFloatingMotion(horizontalDp = 190f)
        motion.update(4500, 1f)
        val firstLift = motion.liftDp
        val firstScale = motion.scale
        repeat(120) {
            motion.update(4500, 1f)
            assertEquals(firstLift, motion.liftDp, 0f)
            assertEquals(firstScale, motion.scale, 0f)
        }
        motion.update(9500, 1f)
        motion.update(4500, 1f)
        assertEquals(firstLift, motion.liftDp, 0f)
        assertEquals(firstScale, motion.scale, 0f)
    }

    @Test fun amplitudeAndPerFrameMovementStayBoundedAt60_90_120Hz() {
        for (hz in listOf(60, 90, 120)) {
            val motion = LyricsFloatingMotion(horizontalDp = 85f)
            var previousLift = 0f
            var previousScale = 1f
            for (frame in 0..hz * 12) {
                val now = (frame * 1000.0 / hz).toInt()
                val intensity = lyricsLineWaveIntensity(1000, 11000, now)
                motion.update(now, intensity)
                assertTrue(motion.scale in 1f..1.02f)
                assertTrue(abs(motion.liftDp) <= 2.5f)
                assertTrue("Jump at $hz Hz / $now ms", abs(motion.liftDp - previousLift) < 0.25f)
                assertTrue(abs(motion.scale - previousScale) < 0.005f)
                previousLift = motion.liftDp
                previousScale = motion.scale
            }
        }
    }

    @Test fun silenceAndDegenerateRowsHaveNoMotionOrInvalidValues() {
        val motion = LyricsFloatingMotion(horizontalDp = 10f)
        for (time in listOf(-200, 0, 500, 1000, 9000, 9100)) {
            val intensity = lyricsLineWaveIntensity(1000, 9000, time)
            motion.update(time, intensity)
            assertTrue(motion.scale.isFinite() && motion.liftDp.isFinite())
            assertTrue(motion.scale in 1f..1.02f)
        }
        motion.update(9500, lyricsLineWaveIntensity(1000, 9000, 9500))
        assertEquals(1f, motion.scale, 0f)
        assertEquals(0f, motion.liftDp, 0f)
        assertEquals(0f, lyricsLineWaveIntensity(5000, 5000, 5000), 0f)
        assertEquals(0f, lyricsLineWaveIntensity(5000, 1000, 3000), 0f)
        assertTrue(lyricsLineWaveIntensity(Int.MIN_VALUE, Int.MAX_VALUE, 0).isFinite())
    }
}
