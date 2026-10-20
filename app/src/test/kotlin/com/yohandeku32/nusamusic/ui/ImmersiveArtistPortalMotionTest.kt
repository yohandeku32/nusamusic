package com.yohandeku32.nusamusic.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ImmersiveArtistPortalMotionTest {
    @Test fun downwardDragProgressFollowsFingerAndReversesSmoothly() {
        val fullHeight = 1000f
        assertEquals(1f, overlayDragRevealFraction(0f, fullHeight), 0.001f)
        assertEquals(0.5f, overlayDragRevealFraction(360f, fullHeight), 0.001f)
        assertEquals(0f, overlayDragRevealFraction(720f, fullHeight), 0.001f)
        assertEquals(0f, overlayDragRevealFraction(1000f, fullHeight), 0.001f)
        assertEquals(1f, overlayDragRevealFraction(Float.NaN, fullHeight), 0.001f)
        assertEquals(1f, overlayDragRevealFraction(80f, 0f), 0.001f)
        var previous = 1f
        for (distance in 0..720 step 12) {
            val fraction = overlayDragRevealFraction(distance.toFloat(), fullHeight)
            assertTrue(fraction <= previous)
            previous = fraction
        }
    }

    @Test fun slowDownwardSwipeDismissesButNormalScrollAndHorizontalGesturesDoNot() {
        assertTrue(shouldDismissOverlaySwipe(8f, 64f, 64f))
        assertTrue(shouldDismissOverlaySwipe(8f, 140f, 64f))
        assertEquals(false, shouldDismissOverlaySwipe(8f, 40f, 64f))
        assertEquals(false, shouldDismissOverlaySwipe(120f, 140f, 64f))
        assertEquals(false, shouldDismissOverlaySwipe(0f, -130f, 64f))
    }

    @Test fun pillStartsToTheRightOfArtistCircleAndCentersWhenOpen() {
        val circleLeft = 34f
        val diameter = 56f
        val gap = 10f
        assertEquals(100f, artistPillLeftPx(circleLeft, diameter, gap, 0f), 0.001f)
        assertEquals(67f, artistPillLeftPx(circleLeft, diameter, gap, 1f), 0.001f)
    }

    @Test fun pillReturnsContinuouslyToItsOriginalRealHitPosition() {
        var previous = artistPillLeftPx(34f, 56f, 10f, 1f)
        for (step in 1..100) {
            val closeProgress = 1f - step / 100f
            val position = artistPillLeftPx(34f, 56f, 10f, closeProgress)
            assertTrue(position >= previous)
            previous = position
        }
        assertEquals(100f, previous, 0.001f)
    }

    @Test fun reversedProgressAndOverflowNeverShiftPillOutsideLimits() {
        assertEquals(100f, artistPillLeftPx(34f, 56f, 10f, -1f), 0.001f)
        assertEquals(67f, artistPillLeftPx(34f, 56f, 10f, 3f), 0.001f)
        val midpoint = artistPillLeftPx(34f, 56f, 10f, 0.5f)
        assertEquals(83.5f, midpoint, 0.001f)
    }
}
