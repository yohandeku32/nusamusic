package com.yohandeku32.nusamusic.ui

import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HeldVocalGlowTest {
    private fun heldWord() = LyricsHeldWordWave(listOf(
        LyricsHeldWordUnit("love", 2_000, 7_000, 0f, 90f)
    ))

    @Test fun onlySustainedWordsReceiveEnhancedGlow() {
        assertEquals(0f, heldVocalGlowEnergy(0f, 1f), 0f)
        assertEquals(0f, heldVocalGlowEnergy(0f, 0f), 0f)
        assertEquals(1f, heldVocalGlowEnergy(1f, 1f), 0f)
        assertEquals(0.55f, heldVocalGlowEnergy(1f, 0f), 0.00001f)
        assertEquals(1f, heldVocalGlowEnergy(2f, 5f), 0f)
        assertEquals(0f, heldVocalGlowEnergy(-1f, 1f), 0f)
    }

    @Test fun glowFadesWith3dProjectionWithoutASecondBlinkAt60_90_120Hz() {
        val wave = heldWord()
        for (hz in listOf(60, 90, 120)) {
            var before = 0f
            var lastRelease = Float.MAX_VALUE
            for (frame in 0..hz * 8) {
                val time = (frame * 1000.0 / hz).toInt()
                val pose = wave.pose(0, time, 45f)
                // Isolate the enhancement: the original karaoke glow can
                // breathe independently, but this envelope must never reset.
                val energy = heldVocalGlowEnergy(pose.depth, 0.95f)
                assertTrue("glow popped between frames at " + hz + " Hz",
                    abs(energy - before) < 0.019f)
                if (time in 4_100..7_000) {
                    assertTrue("held glow must decay rather than flicker at " + time,
                        energy <= lastRelease + 0.00001f)
                    lastRelease = energy
                }
                before = energy
            }
        }
        assertEquals(0f, heldVocalGlowEnergy(wave.pose(0, 7_000, 45f).depth, 1f), 0f)
    }

    @Test fun seekAndPauseGiveIdenticalLightingAtIdenticalAudioTime() {
        val wave = heldWord()
        val sample = heldVocalGlowEnergy(wave.pose(0, 4_900, 45f).depth, 1f)
        repeat(100) {
            assertEquals(sample, heldVocalGlowEnergy(wave.pose(0, 4_900, 45f).depth, 1f), 0f)
        }
        wave.pose(0, 6_700, 45f)
        assertEquals(sample, heldVocalGlowEnergy(wave.pose(0, 4_900, 45f).depth, 1f), 0f)
    }
}
