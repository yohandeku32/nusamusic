package com.yohandeku32.nusamusic.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test

class MiuixPagerMotionTest {
    @Test fun selectedPageIsFullyOpaqueWithoutZoom() {
        val v = miuixPagerVisuals(0f, 1080f, false)
        assertEquals(0f, v.translationX, 0.001f)
        assertEquals(1f, v.scale, 0.001f)
        assertEquals(1f, v.alpha, 0.001f)
        assertEquals(0f, v.scrimAlpha, 0.001f)
    }

    @Test fun adjacentPageGlidesTwentyFourPercentWithRestrainedDepth() {
        val v = miuixPagerVisuals(1f, 1080f, false)
        // Native 1080px travel minus 76% compensation leaves 259.2px.
        assertEquals(820.8f, v.translationX, 0.1f)
        assertEquals(259.2f, 1080f - v.translationX, 0.1f)
        assertEquals(0.984f, v.scale, 0.001f)
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
        assertEquals(right.scale, left.scale, 0.001f)
        assertTrue(right.scale < 1f)
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
            assertTrue(v.scale in 0.984f..1f)
            assertEquals(0f, v.scrimAlpha, 0.001f)
        }
        assertEquals(1f, miuixPagerVisuals(0f, 1080f, false).alpha, 0.001f)
        assertEquals(0f, miuixPagerVisuals(1f, 1080f, false).alpha, 0.001f)
    }

    @Test fun physicalGlideIsMonotonicOnEveryReversibleSwipe() {
        for (sign in listOf(-1f, 1f)) {
            var lastTravel = -1f
            for (step in 0..100) {
                val fraction = step / 100f
                val visuals = miuixPagerVisuals(sign * fraction, 1080f, false)
                val physicalTravel = kotlin.math.abs(sign * fraction * 1080f - visuals.translationX)
                assertTrue(physicalTravel + 0.01f >= lastTravel)
                assertTrue(physicalTravel <= 259.3f)
                lastTravel = physicalTravel
            }
        }
    }

    @Test fun incomingPageLeadsOnlyDuringMotion() {
        assertEquals(1f, miuixPageZIndex(1, 1, false), 0f)
        assertEquals(0f, miuixPageZIndex(0, 1, false), 0f)
        assertEquals(0f, miuixPageZIndex(1, 1, true), 0f)
        assertEquals(1f, miuixPageZIndex(0, 1, true), 0f)
        assertEquals(1f, miuixPageZIndex(2, 1, true), 0f)
    }

    @Test fun invalidInputsNeverProduceNaN() {
        val v = miuixPagerVisuals(Float.NaN, Float.POSITIVE_INFINITY, false)
        assertTrue(v.translationX.isFinite())
        assertTrue(v.alpha.isFinite())
        assertTrue(v.scale.isFinite())
        assertTrue(v.scrimAlpha.isFinite())
    }

    @Test fun transparentNeighboursCannotAcceptTapsOnSettledPlayer() {
        for (page in 0..2) {
            assertEquals(page == 1, miuixPageAcceptsInput(page, 1, 1, 0f, false))
        }
    }

    @Test fun noPageAcceptsPlaybackTapsWhileDraggingOrSettlingInEitherDirection() {
        for (settled in 0..2) {
            for (current in 0..2) {
                for (page in 0..2) {
                    for (offset in listOf(-0.49f, 0f, 0.49f)) {
                        assertFalse(miuixPageAcceptsInput(page, settled, current, offset, true))
                    }
                }
            }
        }
        // Midpoint currentPage change must not activate the incoming page.
        assertFalse(miuixPageAcceptsInput(0, 1, 0, 0f, false))
        assertFalse(miuixPageAcceptsInput(1, 1, 1, 0.1f, false))
        assertFalse(miuixPageAcceptsInput(1, 1, 1, Float.NaN, false))
    }

    @Test fun destinationAndCancelledSwipeRestoreOnlyTheirOwnControls() {
        assertTrue(miuixPageAcceptsInput(0, 0, 0, 0f, false))
        assertFalse(miuixPageAcceptsInput(1, 0, 0, 0f, false))
        assertTrue(miuixPageAcceptsInput(1, 1, 1, 0f, false))
        assertFalse(miuixPageAcceptsInput(0, 1, 1, 0f, false))
    }

    @Test fun revealHasSoftEndpointsAndMonotonicOpacity() {
        assertTrue(miuixPagerVisuals(0.01f, 1080f, false).alpha > 0.999f)
        assertTrue(miuixPagerVisuals(0.99f, 1080f, false).alpha < 0.001f)
        var last = 1f
        for (step in 0..100) {
            val alpha = miuixPagerVisuals(step / 100f, 1080f, false).alpha
            assertTrue(alpha <= last)
            last = alpha
        }
    }
}
