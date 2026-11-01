package com.yohandeku32.nusamusic.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class ImmersiveNavigationCenteringTest {
    private val width = 400f
    private val pillWidth = 176f
    private val circle = 56f
    private val gap = 10f
    private val profileWithGap = circle + gap
    private val baseCircle = (width - profileWithGap - pillWidth) / 2f
    private val basePill = baseCircle + profileWithGap

    @Test fun libraryCircleAndPillStayCenteredAtEveryTransitionFrame() {
        for (step in 0..100) {
            val t = step / 100f
            val circleLeft = navigationProfileLeftPx(baseCircle, 48f, t)
            val pillLeft = navigationPillLeftPx(basePill, 48f, profileWithGap, t, false)
            val rightEdge = pillLeft + pillWidth + 48f * t
            assertEquals("navigation centered at $t", width / 2f,
                (circleLeft + rightEdge) / 2f, 0.001f)
            assertEquals("stable separation at $t", gap,
                pillLeft - circleLeft - circle, 0.001f)
        }
    }

    @Test fun artistPageCentersPillWithoutInvisibleProfileSpace() {
        val start = navigationPillLeftPx(basePill, 48f, profileWithGap, 0f, true)
        assertEquals(basePill, start, 0.001f)
        val expanded = navigationPillLeftPx(basePill, 48f, profileWithGap, 1f, true)
        assertEquals(width / 2f,
            expanded + (pillWidth + 48f) / 2f, 0.001f)
    }

    @Test fun drawingPositionsClampUnexpectedProgressValues() {
        assertEquals(
            navigationPillLeftPx(basePill, 48f, profileWithGap, 1f, true),
            navigationPillLeftPx(basePill, 48f, profileWithGap, 3f, true), 0f)
        assertEquals(
            navigationProfileLeftPx(baseCircle, 48f, 0f),
            navigationProfileLeftPx(baseCircle, 48f, -2f), 0f)
    }
}
