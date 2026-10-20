package com.yohandeku32.nusamusic.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MiuixPagerMotionTest {
    @Test fun selectedPageIsFullyOpaqueWithoutZoom() {
        val v = miuixPagerVisuals(0f, 1080f, false)
        assertEquals(0f, v.translationX, 0.001f)
        assertEquals(1f, v.scale, 0.001f)
        assertEquals(1f, v.alpha, 0.001f)
        assertEquals(0f, v.scrimAlpha, 0.001f)
    }

    @Test fun nextPageRevealsWithOnlyThreePercentPhysicalDrift() {
        val v = miuixPagerVisuals(1f, 1080f, false)
        // Native pager would move 1080px; transform counters 97% of it.
        assertEquals(1047.6f, v.translationX, 0.1f)
        assertEquals(1f, v.scale, 0.001f)
        assertEquals(0f, v.scrimAlpha, 0.001f)
        assertEquals(0f, v.alpha, 0.001f)
    }

    @Test fun midpointDissolvePreservesBrightnessWithoutBlackFlash() {
        val outgoing = miuixPagerVisuals(0.5f, 1080f, false)
        val incoming = miuixPagerVisuals(-0.5f, 1080f, false)
        assertEquals(0.75f, outgoing.alpha, 0.001f)
        assertEquals(0.75f, incoming.alpha, 0.001f)
        // Their alpha-over composition covers 93.75% of the screen.
        val combinedOpacity = 1f - (1f - outgoing.alpha) * (1f - incoming.alpha)
        assertTrue(combinedOpacity >= 0.93f)
        assertEquals(-outgoing.translationX, incoming.translationX, 0.001f)
    }

    @Test fun reverseSwipeMirrorsDirectionAndOpacity() {
        val right = miuixPagerVisuals(0.4f, 1080f, false)
        val left = miuixPagerVisuals(-0.4f, 1080f, false)
        assertEquals(-right.translationX, left.translationX, 0.001f)
        assertEquals(right.alpha, left.alpha, 0.001f)
        assertEquals(1f, right.scale, 0.001f)
        assertEquals(1f, left.scale, 0.001f)
    }

    @Test fun rtlMirrorsMotionWithoutChangingFade() {
        val ltr = miuixPagerVisuals(0.4f, 1080f, false)
        val rtl = miuixPagerVisuals(0.4f, 1080f, true)
        assertEquals(-ltr.translationX, rtl.translationX, 0.001f)
        assertEquals(ltr.alpha, rtl.alpha, 0.001f)
    }

    @Test fun scrubBackAndForthKeepsOpaqueEndpoints() {
        for (depth in listOf(-1f, -0.8f, -0.4f, 0f, 0.4f, 0.8f, 1f)) {
            val v = miuixPagerVisuals(depth, 1080f, false)
            assertTrue(v.alpha in 0f..1f)
            assertEquals(1f, v.scale, 0.001f)
            assertEquals(0f, v.scrimAlpha, 0.001f)
        }
        assertEquals(1f, miuixPagerVisuals(0f, 1080f, false).alpha, 0.001f)
        assertEquals(0f, miuixPagerVisuals(1f, 1080f, false).alpha, 0.001f)
    }

    @Test fun invalidInputsNeverProduceNaN() {
        val v = miuixPagerVisuals(Float.NaN, Float.POSITIVE_INFINITY, false)
        assertTrue(v.translationX.isFinite())
        assertTrue(v.alpha.isFinite())
        assertTrue(v.scale.isFinite())
        assertTrue(v.scrimAlpha.isFinite())
    }
}
