package com.yohandeku32.nusamusic.ui

import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.runtime.MonotonicFrameClock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.*
import org.junit.Test

class LyricsFollowMotionTest {
    private class Frames : MonotonicFrameClock {
        private var time = 0L
        override suspend fun <R> withFrameNanos(onFrame: (Long) -> R): R {
            time += 16_666_667L
            check(time < 30_000_000_000L) { "Motion did not settle" }
            return onFrame(time)
        }
    }

    @Test fun cancelledFollowingContinuesWithMomentumAndSettlesAtNewTarget() = runBlocking {
        withContext(Frames()) {
            val motion = LyricsFollowMotion()
            var moved = 0f
            val interrupted = ScrollableState { delta ->
                moved += delta
                if (moved > 20f) throw CancellationException("New lyric target")
                delta
            }
            try { motion.move(interrupted, 200f); fail("Expected cancellation") }
            catch (_: CancellationException) { }
            var resumed = 0f
            var peak = 0f
            // A zero-distance target still moves briefly when momentum is retained.
            val next = ScrollableState { delta -> resumed += delta; peak = maxOf(peak, resumed); delta }
            motion.move(next, 0f)
            assertTrue("Retargeting should retain velocity", peak > 1f)
            assertEquals(0f, resumed, 0.6f)
            assertFalse(next.isScrollInProgress)
            motion.move(next, 100f)
            assertEquals(100f, resumed, 0.6f)
        }
    }

    @Test fun scrollBoundaryStopsMotionAndClearsMomentum() = runBlocking {
        withContext(Frames()) {
            val motion = LyricsFollowMotion()
            var calls = 0
            motion.move(ScrollableState { calls++; 0f }, 200f)
            assertTrue(calls < 10)
            var moved = 0f
            motion.move(ScrollableState { moved += it; it }, 0f)
            assertEquals(0f, moved, 0.01f)
        }
    }
}
