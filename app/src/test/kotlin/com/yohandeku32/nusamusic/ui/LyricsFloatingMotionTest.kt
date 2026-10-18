package com.yohandeku32.nusamusic.ui

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs

class LyricsFloatingMotionTest {
    @Test fun longNoteBreathesAndFloatsAfterTheInitialAttack() {
        val motion = LyricsFloatingMotion(1000, 9000, 0)
        motion.update(2400); val scale = motion.scale; val lift = motion.liftDp
        motion.update(4200)
        assertNotEquals(scale, motion.scale, 0.0001f)
        assertNotEquals(lift, motion.liftDp, 0.01f)
        assertTrue(motion.scale in 1f..1.025f)
    }
    @Test fun neighbouringUnitsHaveStaggeredWavePhase() {
        val first = LyricsFloatingMotion(1000, 9000, 0)
        val next = LyricsFloatingMotion(1000, 9000, 1)
        first.update(4000); next.update(4000)
        assertNotEquals(first.liftDp, next.liftDp, 0.001f)
    }
    @Test fun movementReturnsToBaselineBeforeAuthoredEndWithoutLayoutMotion() {
        val motion = LyricsFloatingMotion(1000, 9000, 0)
        motion.update(8999)
        assertEquals(1f, motion.scale, 0.0001f)
        assertEquals(0f, motion.liftDp, 0.001f)
        for (now in listOf(0, 1000, 9000, 9200, Int.MAX_VALUE)) {
            motion.update(now)
            assertEquals(1f, motion.scale, 0f)
            assertEquals(0f, motion.liftDp, 0f)
        }
    }
    @Test fun pauseAndSeekAreDeterministicWithNoTransitionReplay() {
        val motion = LyricsFloatingMotion(1000, 9000, 2)
        motion.update(4500); val scale = motion.scale; val lift = motion.liftDp
        repeat(120) { motion.update(4500); assertEquals(scale, motion.scale, 0f); assertEquals(lift, motion.liftDp, 0f) }
        motion.update(8000); motion.update(4500)
        val fresh = LyricsFloatingMotion(1000, 9000, 2).apply { update(4500) }
        assertEquals(fresh.scale, motion.scale, 0f)
        assertEquals(fresh.liftDp, motion.liftDp, 0f)
    }
    @Test fun frameStepsStaySubtleAt60_90_120Hz() {
        for (hz in listOf(60, 90, 120)) {
            val motion = LyricsFloatingMotion(1000, 9000, 1)
            var lastScale = 1f; var lastLift = 0f
            for (frame in 0..hz * 10) {
                motion.update((frame * 1000.0 / hz).toInt())
                assertTrue(motion.scale in 1f..1.025f)
                assertTrue(abs(motion.liftDp) <= 1.71f)
                assertTrue(abs(motion.scale - lastScale) < 0.004f)
                assertTrue(abs(motion.liftDp - lastLift) < 0.24f)
                lastScale = motion.scale; lastLift = motion.liftDp
            }
        }
    }
    @Test fun slowCarrierDoesNotOscillateRapidlyDuringALongVocal() {
        val motion = LyricsFloatingMotion(0, 12_000, 0)
        var previousLift = 0f
        var directionChanges = 0
        var previousSign = 0
        for (now in 1500..10_000 step 10) {
            motion.update(now)
            assertTrue(motion.scale in 1f..1.015f)
            assertTrue(abs(motion.liftDp) <= 1.5f)
            val delta = motion.liftDp - previousLift
            if (now > 1510) assertTrue("Wave jumped at $now", abs(delta) < 0.05f)
            val sign = when {
                delta > 0.00001f -> 1
                delta < -0.00001f -> -1
                else -> 0
            }
            if (previousSign != 0 && sign != 0 && sign != previousSign) directionChanges++
            if (sign != 0) previousSign = sign
            previousLift = motion.liftDp
        }
        // ~3.4s carrier: only a few direction reversals across 8.5s.
        assertTrue("Wave oscillates too quickly", directionChanges in 3..7)
    }

    @Test fun onsetPulseHappensOnlyNearTheSyllableBoundary() {
        val motion = LyricsFloatingMotion(1000, 9000, 0)
        motion.update(1120)
        val early = motion.liftDp
        motion.update(4900)
        val middle = motion.liftDp
        motion.update(8000)
        val late = motion.liftDp
        assertTrue(early.isFinite() && middle.isFinite() && late.isFinite())
        motion.update(8999)
        assertTrue(abs(motion.liftDp) < 0.001f)
        assertTrue(abs(motion.scale - 1f) < 0.0001f)
    }

    @Test fun shortUnitsDoNotCompressGlowIntoAFastRepeatedCycle() {
        var previous = lyricBreathing(1000, 1350, 1000)
        for (time in 1010..1350 step 10) {
            val next = lyricBreathing(1000, 1350, time)
            assertTrue(next in 0.88f..1.001f)
            assertTrue(abs(next - previous) < 0.003f)
            previous = next
        }
    }

}
