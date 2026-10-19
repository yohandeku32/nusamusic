package com.yohandeku32.nusamusic.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MiuixPagerMotionTest {
    @Test fun selectedPageKeepsFullScaleWithoutOverlay() {
        val v = miuixPagerVisuals(0f, 1080f, false)
        assertEquals(0f, v.translationX, 0.001f)
        assertEquals(1f, v.scale, 0.001f)
        assertEquals(1f, v.alpha, 0.001f)
        assertEquals(0f, v.scrimAlpha, 0.001f)
    }

    @Test fun coveredPageKeepsNaturalScaleAndFivePercentParallax() {
        val v = miuixPagerVisuals(1f, 1080f, false)
        assertEquals(54f, v.translationX, 0.1f)
        assertEquals(1f, v.scale, 0.001f)
        assertEquals(0f, v.scrimAlpha, 0.001f)
        assertEquals(1f, v.alpha, 0.001f)
    }

    @Test fun reversedLayoutMirrorsHorizontalMotionOnly() {
        val ltr = miuixPagerVisuals(0.4f, 1080f, false)
        val rtl = miuixPagerVisuals(0.4f, 1080f, true)
        assertEquals(-ltr.translationX, rtl.translationX, 0.001f)
        assertEquals(ltr.scale, rtl.scale, 0.001f)
    }

    @Test fun invalidInputsNeverProduceNaN() {
        val v = miuixPagerVisuals(Float.NaN, Float.POSITIVE_INFINITY, false)
        assertTrue(v.translationX.isFinite())
        assertTrue(v.scale.isFinite())
        assertTrue(v.scrimAlpha.isFinite())
    }
}
