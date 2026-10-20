package com.yohandeku32.nusamusic.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ImmersiveArtistPortalMotionTest {
    @Test fun slowDownwardSwipeDismissesButNormalScrollAndHorizontalGesturesDoNot() {
        assertTrue(shouldDismissOverlaySwipe(8f, 140f, 96f))
        assertEquals(false, shouldDismissOverlaySwipe(8f, 40f, 96f))
        assertEquals(false, shouldDismissOverlaySwipe(120f, 140f, 96f))
        assertEquals(false, shouldDismissOverlaySwipe(0f, -130f, 96f))
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
