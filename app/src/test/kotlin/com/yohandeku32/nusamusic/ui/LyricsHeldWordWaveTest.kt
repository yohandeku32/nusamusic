package com.yohandeku32.nusamusic.ui

import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LyricsHeldWordWaveTest {
    private fun phrase(): LyricsHeldWordWave = LyricsHeldWordWave(listOf(
        LyricsHeldWordUnit("aku ", 1000, 1400, 0f, 52f),
        LyricsHeldWordUnit("cinta ", 1400, 2000, 58f, 134f),
        LyricsHeldWordUnit("kamu", 2000, 7000, 140f, 222f)
    ))

    @Test fun longKamuMovesWhileAkuAndCintaNeverMove() {
        val wave = phrase()
        for (time in 0..7400 step 80) {
            assertEquals(LyricsHeldWordPose(), wave.pose(0, time, 24f))
            assertEquals(LyricsHeldWordPose(), wave.pose(1, time, 96f))
        }
        val held = wave.pose(2, 4300, 182f)
        assertTrue(held.liftDp < -2f)
        assertTrue(held.scaleY > 1f)
        assertTrue(held.intensity > 0.5f)
        assertEquals(LyricsHeldWordPose(), wave.pose(2, 2000, 182f))
        assertEquals(LyricsHeldWordPose(), wave.pose(2, 7000, 182f))
    }

    @Test fun wordSplitAcrossSyllableUnitsFormsOneLocalArch() {
        val wave = LyricsHeldWordWave(listOf(
            LyricsHeldWordUnit("aku ", 1000, 1500, 0f, 50f),
            LyricsHeldWordUnit("ka", 1500, 1900, 55f, 81f),
            LyricsHeldWordUnit("mu", 1900, 6000, 81f, 119f),
            LyricsHeldWordUnit(" lagi", 6000, 6300, 125f, 183f)
        ))
        assertEquals(LyricsHeldWordPose(), wave.pose(0, 3700, 25f))
        assertEquals(LyricsHeldWordPose(), wave.pose(3, 3700, 160f))
        val ka = wave.pose(1, 3700, 68f)
        val mu = wave.pose(2, 3700, 100f)
        assertTrue("the short 'ka' participates in the held word", ka.liftDp < -0.5f)
        assertTrue("the held 'mu' participates in the same arch", mu.liftDp < -0.5f)
        assertTrue("adjacent syllables keep a coherent wave", abs(ka.liftDp - mu.liftDp) < 2.5f)
    }

    @Test fun rapidAndInferredUnitsStayStill() {
        val wave = LyricsHeldWordWave(listOf(
            LyricsHeldWordUnit("rap ", 0, 180, 0f, 34f),
            LyricsHeldWordUnit("next ", 180, 430, 40f, 80f),
            LyricsHeldWordUnit("unverified ", 430, 9000, 85f, 160f, reliableHold = false),
            LyricsHeldWordUnit("two words", 9000, 17000, 162f, 270f)
        ))
        for (i in 0..3) {
            val time = if (i < 2) 100 else if (i == 2) 4500 else 12000
            assertEquals(LyricsHeldWordPose(), wave.pose(i, time, 150f))
        }
    }

    @Test fun zeroToPeakAndBackIsContinuousAt60_90And120Hz() {
        val wave = phrase()
        for (hz in listOf(60, 90, 120)) {
            var before = LyricsHeldWordPose()
            var maxLift = 0f
            for (frame in 0..hz * 8) {
                val now = (frame * 1000.0 / hz).toInt()
                val pose = wave.pose(2, now, 182f)
                maxLift = maxOf(maxLift, abs(pose.liftDp))
                assertTrue("lift jump at $hz Hz, $now ms",
                    abs(pose.liftDp - before.liftDp) < 0.15f)
                assertTrue("scale jump at $hz Hz, $now ms",
                    abs(pose.scaleY - before.scaleY) < 0.0005f)
                assertTrue("tilt jump at $hz Hz, $now ms",
                    abs(pose.rotationDegrees - before.rotationDegrees) < 0.03f)
                before = pose
            }
            assertTrue("held word must visibly rise", maxLift > 2.5f)
        }
    }

    @Test fun pauseAndBackwardSeekReproduceExactlyTheSamePose() {
        val wave = phrase()
        val held = wave.pose(2, 4600, 182f)
        repeat(60) { assertEquals(held, wave.pose(2, 4600, 182f)) }
        wave.pose(2, 6800, 182f)
        assertEquals(held, wave.pose(2, 4600, 182f))
    }
}
