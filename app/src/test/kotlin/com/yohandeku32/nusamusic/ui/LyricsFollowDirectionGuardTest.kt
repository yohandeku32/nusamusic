package com.yohandeku32.nusamusic.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LyricsFollowDirectionGuardTest {
    @Test fun settledForwardFollowNeverBouncesBackward() {
        val guard = LyricsFollowDirectionGuard()
        guard.markSettled()
        assertEquals(4f, guard.step(4f, 12f), 0f)
        assertEquals(1.5f, guard.step(6f, 1.5f), 0f)
        assertEquals(0f, guard.step(-0.8f, -0.8f), 0f)
        assertEquals(0f, guard.step(-1.5f, -1.5f), 0f)
        assertEquals(2f, guard.step(2f, 5f), 0f)
    }

    @Test fun seeksAndReturningFromManualBrowsingMayInitiallyScrollBack() {
        // New guard is instantiated only after explicit seek/rejoin.
        val fresh = LyricsFollowDirectionGuard()
        assertEquals(-8f, fresh.step(-8f, -40f), 0f)
        assertEquals(-2f, fresh.step(-4f, -2f), 0f)
        fresh.markSettled()
        assertEquals(0f, fresh.step(-0.9f, -0.9f), 0f)
        assertTrue(fresh.isReverseCorrection(-0.8f))
    }

    @Test fun springMomentumCannotOvershootOrMoveAwayFromTarget() {
        val guard = LyricsFollowDirectionGuard()
        assertEquals(6f, guard.step(20f, 6f), 0f)
        assertEquals(-4f, guard.step(-9f, -4f), 0f)
        assertEquals(0f, guard.step(5f, -5f), 0f)
        assertEquals(0f, guard.step(-5f, 5f), 0f)
        assertEquals(0f, guard.step(0.2f, 0.2f), 0f)
        assertTrue(guard.isReverseCorrection(-10f))
    }

    @Test fun frameRateIndependentGuardsCannotReverseAfterCentering() {
        for (fps in listOf(60, 90, 120)) {
            val motion = LyricsFollowMotion()
            val guard = LyricsFollowDirectionGuard()
            guard.markSettled()
            val dt = (1000L / fps).coerceAtLeast(1)
            val errors = listOf(40f, 20f, 8f, 2f, -1f, -0.7f, 4f)
            val steps = errors.map { error ->
                guard.step(motion.step(error, dt), error)
            }
            assertTrue("$fps Hz follow reversed unexpectedly", steps.all { it >= 0f })
        }
    }
}
