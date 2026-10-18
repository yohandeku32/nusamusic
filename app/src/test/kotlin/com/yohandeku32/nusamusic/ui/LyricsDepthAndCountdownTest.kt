package com.yohandeku32.nusamusic.ui

import kotlin.math.abs
import org.junit.Assert.*
import org.junit.Test

class LyricsDepthAndCountdownTest {
    @Test fun activeLyricsAreSharpAndFullyOpaque() {
        for (distance in listOf(-24f, 0f, 12f, 24f)) {
            val depth = lyricsFocusDepth(distance)
            assertEquals(0f, depth.blurDp, 0f)
            assertEquals(1f, depth.opacity, 0f)
        }
    }

    @Test fun blurAndOpacityFollowContinuousVisualDistance() {
        val near = lyricsFocusDepth(105f)
        val far = lyricsFocusDepth(250f)
        assertEquals(1.2f, near.blurDp, 0.0001f)
        assertEquals(0.76f, near.opacity, 0.0001f)
        assertEquals(2.7f, far.blurDp, 0.0001f)
        assertEquals(0.48f, far.opacity, 0.0001f)
        assertEquals(far, lyricsFocusDepth(600f))
        assertEquals(near, lyricsFocusDepth(-105f))

        var last = lyricsFocusDepth(0f)
        for (pixel in 1..3500) {
            val current = lyricsFocusDepth(pixel / 10f)
            assertTrue("blur must increase smoothly", current.blurDp >= last.blurDp)
            assertTrue("opacity must fade smoothly", current.opacity <= last.opacity)
            assertTrue(abs(current.blurDp - last.blurDp) < 0.005f)
            assertTrue(abs(current.opacity - last.opacity) < 0.001f)
            last = current
        }
    }

    @Test fun seekingAndManualScrollUseTheSameStableFocusCurve() {
        val samples = listOf(120f, 90f, 55f, 20f, -10f, -50f, -90f, -120f)
            .map(::lyricsFocusDepth)
        assertTrue(samples[3].blurDp == 0f && samples[3].opacity == 1f)
        assertTrue(samples[5].blurDp > samples[4].blurDp)
        assertTrue(samples[0].blurDp > samples[1].blurDp)
    }

    @Test fun threeDotsBreatheWithOneSlowContinuousClock() {
        val poses = (0..2).map { countdownDotPose(1750, it) }
        assertEquals(3, poses.size)
        assertNotEquals(poses[0].liftDp, poses[1].liftDp, 0.0001f)
        assertNotEquals(poses[1].liftDp, poses[2].liftDp, 0.0001f)
        for (pose in poses) {
            assertTrue(abs(pose.liftDp) <= 1.5f)
            assertTrue(pose.scale in 1f..1.08f)
            assertTrue(pose.opacity in 0.8f..1f)
        }
        val before = countdownDotPose(700, 1)
        val looped = countdownDotPose(2900, 1)
        assertEquals(before.liftDp, looped.liftDp, 0.00001f)
        assertEquals(before.scale, looped.scale, 0.00001f)
    }

    @Test fun pauseAndSeekSampleTheSameDotPoseWithoutRestarting() {
        val held = countdownDotPose(4321, 2)
        repeat(120) {
            assertEquals(held, countdownDotPose(4321, 2))
        }
        countdownDotPose(150, 2)
        assertEquals(held, countdownDotPose(4321, 2))
    }

    @Test fun dotMotionHasNoJumpsAt60_90_120Hz() {
        for (hz in listOf(60, 90, 120)) {
            val previous = arrayOfNulls<CountdownDotPose>(3)
            for (frame in 0..hz * 6) {
                val now = (frame * 1000.0 / hz).toInt()
                for (dot in 0..2) {
                    val pose = countdownDotPose(now, dot)
                    previous[dot]?.let { old ->
                        assertTrue(abs(pose.liftDp - old.liftDp) < 0.09f)
                        assertTrue(abs(pose.scale - old.scale) < 0.004f)
                        assertTrue(abs(pose.opacity - old.opacity) < 0.005f)
                    }
                    previous[dot] = pose
                }
            }
        }
    }

    @Test fun oldCountdownFadeAndGapCollapseStillEndAtNextVocal() {
        val gap = LyricsGap(2000, 8000, 1)
        assertTrue(countdownOpacity(gap, 7300) > 0f)
        assertTrue(countdownOpacity(gap, 7800) > countdownOpacity(gap, 7960))
        assertEquals(0f, countdownOpacity(gap, 8000), 0f)
        assertEquals(0f, countdownLayoutFraction(gap, 8000), 0f)
        assertEquals(0f, countdownLayoutFraction(gap, 8500), 0f)
    }
}
