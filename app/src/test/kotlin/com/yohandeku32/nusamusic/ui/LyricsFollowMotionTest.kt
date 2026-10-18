package com.yohandeku32.nusamusic.ui

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs

class LyricsFollowMotionTest {
    @Test fun changingTargetRetainsMomentumThenSettles() {
        val motion = LyricsFollowMotion()
        var position = 0f
        repeat(8) { position += motion.step(200f - position, 16) }
        val carried = motion.step(0f, 16)
        assertTrue(carried > 0f)
        position += carried
        repeat(150) { position += motion.step(300f - position, 16) }
        assertEquals(300f, position, 0.5f)
    }
    @Test fun pausedAudioFreezesScrollEvenWithStoredMomentum() {
        val motion = LyricsFollowMotion()
        repeat(8) { motion.step(200f, 16) }
        repeat(120) { assertEquals(0f, motion.step(200f, 0), 0f) }
        assertTrue(motion.step(200f, 16) > 0f)
    }
    @Test fun resetClearsMomentumAtASeekOrScrollBoundary() {
        val motion = LyricsFollowMotion()
        repeat(8) { motion.step(200f, 16) }
        motion.reset()
        assertEquals(0f, motion.step(0f, 16), 0f)
    }
    @Test fun frameRatesConvergeAndOvershootRemainsSubtle() {
        for (delta in listOf(8L, 11L, 16L, 33L)) {
            val motion = LyricsFollowMotion()
            var position = 0f
            var peak = 0f
            repeat((2400 / delta).toInt()) {
                position += motion.step(200f - position, delta)
                peak = maxOf(peak, position)
            }
            assertTrue(peak < 201f)
            assertTrue(abs(position - 200f) < 0.5f)
        }
    }
}
