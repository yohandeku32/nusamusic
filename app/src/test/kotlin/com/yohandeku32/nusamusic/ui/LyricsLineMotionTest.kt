package com.yohandeku32.nusamusic.ui

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs
import kotlin.math.roundToLong
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.snapshots.SnapshotStateObserver

class LyricsLineMotionTest {
    private fun model(count: Int = 6) = LyricsLineMotion(IntArray(count) { it * 2_000 })
    private fun offsets(motion: LyricsLineMotion, count: Int = 6) =
        (0 until count).map(motion::offsetDp)

    @Test fun theFirstRenderedPoseObservesLaterMotionAndResetDoesNotDetachIt() {
        val motion = model()
        val observer = SnapshotStateObserver { it() }
        observer.start()
        try {
            repeat(2) {
                var invalidations = 0
                observer.observeReads(Unit, { invalidations++ }) { motion.offsetDp(1) }
                Snapshot.withMutableSnapshot { motion.advance(12f, 16, 0, 5, 1f, 80f) }
                Snapshot.sendApplyNotifications()
                assertTrue(invalidations > 0)
                motion.reset()
                Snapshot.sendApplyNotifications()
            }
        } finally { observer.stop() }
    }

    @Test fun activeSentenceLeadsUpcomingSentencesInsteadOfMovingAsOnePanel() {
        val motion = model()
        motion.advance(12f, 16, 0, 5, 1f, 80f)
        assertTrue(motion.offsetDp(0) < motion.offsetDp(1))
        assertTrue(motion.offsetDp(1) < motion.offsetDp(2))
        assertTrue(motion.offsetDp(2) < motion.offsetDp(3))
        assertTrue(offsets(motion).all { it in 0f..28f })
    }

    @Test fun followersSettleWithoutBouncingBelowTheirLayoutPosition() {
        val motion = model()
        motion.advance(20f, 16, 0, 5, 1f, 80f)
        var previous = offsets(motion)
        repeat(90) {
            motion.advance(0f, 16, 0, 5, 1f, 80f)
            val current = offsets(motion)
            assertTrue(current.indices.all { current[it] <= previous[it] + 0.0001f })
            assertTrue(current.all { it >= 0f })
            previous = current
        }
        assertTrue(previous.all { it < 0.05f })
    }

    @Test fun changingTheActiveCueRetainsTheCurrentPoseAndKeepsRowsOrdered() {
        val motion = model()
        repeat(8) { motion.advance(3f, 16, 0, 5, 1f, 90f) }
        val before = offsets(motion)
        motion.advance(2f, 16, 0, 5, 2f, 90f)
        val after = offsets(motion)
        assertTrue(after[2] > 0f)
        assertTrue(after.indices.all { abs(after[it] - before[it]) < 4f })
        assertTrue(after.zipWithNext().all { (a, b) -> a <= b })
    }

    @Test fun pausedPlaybackHoldsEverySentenceExactly() {
        val motion = model()
        repeat(8) { motion.advance(3f, 16, 0, 5, 1f, 80f) }
        val paused = offsets(motion)
        repeat(120) { motion.advance(0f, 0, 0, 5, 1f, 80f) }
        assertEquals(paused, offsets(motion))
        motion.advance(0f, 16, 0, 5, 1f, 80f)
        assertTrue(motion.offsetDp(1) < paused[1])
    }

    @Test fun seekResetReverseRejoinAndLongDiscontinuitiesDoNotReplayATrail() {
        for (boundary in 0..3) {
            val motion = model()
            repeat(8) { motion.advance(3f, 16, 0, 5, 1f, 80f) }
            when (boundary) {
                0 -> motion.reset()
                1 -> motion.advance(-30f, 16, 0, 5, 1f, 80f)
                2 -> motion.advance(0f, 700, 0, 5, 1f, 80f)
                3 -> motion.advance(0f, -100, 0, 5, 1f, 80f)
            }
            assertTrue(offsets(motion).all { it == 0f })
        }
    }

    @Test fun simultaneousDuetVoicesShareOnePhase() {
        val motion = LyricsLineMotion(intArrayOf(0, 2_000, 2_000, 4_000))
        assertEquals(motion.rank(1), motion.rank(2), 0f)
        repeat(45) {
            motion.advance(2f, 16, 0, 3, 1f, 80f)
            assertEquals(motion.offsetDp(1), motion.offsetDp(2), 0f)
        }
    }

    @Test fun rowsReenteringTheViewportHaveNoOldAnimationState() {
        val motion = model()
        repeat(8) { motion.advance(3f, 16, 0, 2, 1f, 80f) }
        motion.advance(0f, 16, 3, 5, 4f, 80f)
        assertTrue((0..2).all { motion.offsetDp(it) == 0f })
        motion.advance(0f, 16, 0, 2, 1f, 80f)
        assertTrue(offsets(motion).all { it == 0f })
    }

    @Test fun durationScaleZeroClearsMotionIncludingWhilePaused() {
        val motion = model()
        repeat(8) { motion.advance(3f, 16, 0, 5, 1f, 80f) }
        motion.advance(0f, 0, 0, 5, 1f, 80f, durationScale = 0f)
        assertTrue(offsets(motion).all { it == 0f })
    }

    @Test fun frameRatesProduceComparableSentencePositions() {
        val results = listOf(60, 90, 120).map { hz ->
            val motion = model()
            var previous = 0L
            for (frame in 1..hz) {
                val now = (frame * 1000.0 / hz).roundToLong()
                val delta = now - previous
                motion.advance(120f * delta / 1000f, delta, 0, 5, 1f, 100f)
                previous = now
            }
            offsets(motion)
        }
        for (index in 0..5) assertTrue(results.maxOf { it[index] } - results.minOf { it[index] } < 1f)
    }

    @Test fun rapidSentencesRemainBoundedAndNeverCrossAdjacentRows() {
        val motion = model(15)
        for (now in 0..2_800 step 16) {
            motion.advance(4f, 16, 0, 14, now / 200f, 68f)
            val current = offsets(motion, 15)
            assertTrue(current.all { it.isFinite() && it in 0f..23.8f })
            assertTrue(current.zipWithNext().all { (a, b) -> a <= b })
        }
    }

    @Test fun invalidMeasurementsCannotPublishNonfiniteTransforms() {
        for (bad in listOf(Float.NaN, Float.POSITIVE_INFINITY)) {
            val motion = model()
            motion.advance(3f, 16, 0, 5, 1f, 80f)
            motion.advance(bad, 16, 0, 5, 1f, 80f)
            assertTrue(offsets(motion).all { it == 0f })
        }
    }
}
