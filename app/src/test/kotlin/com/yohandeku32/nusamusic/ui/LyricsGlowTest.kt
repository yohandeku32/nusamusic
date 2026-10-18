package com.yohandeku32.nusamusic.ui

import org.junit.Assert.*
import org.junit.Test

class LyricsGlowTest {
    @Test fun authoredBoundariesFadeToZeroInsteadOfSwitchingTheHaloOff() {
        assertEquals(0f, syllableGlowEnvelope(1000, 2000, 999), 0f)
        assertEquals(0f, syllableGlowEnvelope(1000, 2000, 1000), 0f)
        assertTrue(syllableGlowEnvelope(1000, 2000, 1001) < 0.001f)
        assertEquals(1f, syllableGlowEnvelope(1000, 2000, 1500), 0f)
        assertTrue(syllableGlowEnvelope(1000, 2000, 1999) < 0.001f)
        assertEquals(0f, syllableGlowEnvelope(1000, 2000, 2000), 0f)
        assertEquals(0f, syllableGlowEnvelope(1000, 2000, 2001), 0f)
    }
    @Test fun shortSyllablesStillHaveContinuousBoundedGlow() {
        val values = (100..140).map { syllableGlowEnvelope(100, 140, it) }
        assertTrue(values.all { it.isFinite() && it in 0f..1f })
        assertEquals(0f, values.first(), 0f)
        assertEquals(0f, values.last(), 0f)
        assertEquals(1f, values[20], 0f)
        assertTrue(values.zipWithNext().all { (a, b) -> kotlin.math.abs(a - b) < 0.08f })
    }
    @Test fun pauseHoldsTheSameHaloAndBackwardSeekRecomputesIt() {
        val held = syllableGlowEnvelope(1000, 3000, 1060)
        repeat(120) { assertEquals(held, syllableGlowEnvelope(1000, 3000, 1060), 0f) }
        assertEquals(0f, syllableGlowEnvelope(1000, 3000, 500), 0f)
        assertEquals(1f, syllableGlowEnvelope(1000, 3000, 2000), 0f)
    }
    @Test fun degenerateAndExtremeTimestampsDoNotOverflow() {
        assertEquals(0f, syllableGlowEnvelope(10, 10, 10), 0f)
        assertEquals(0f, syllableGlowEnvelope(11, 10, 10), 0f)
        assertTrue(syllableGlowEnvelope(Int.MIN_VALUE, Int.MAX_VALUE, 0).isFinite())
        assertEquals(0f, syllableGlowEnvelope(Int.MAX_VALUE - 1, Int.MAX_VALUE, Int.MAX_VALUE), 0f)
    }
}
