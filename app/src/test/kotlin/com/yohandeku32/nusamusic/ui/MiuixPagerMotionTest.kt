package com.yohandeku32.nusamusic.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MiuixPagerMotionTest {
    @Test fun restingPagesHaveNoShadowOrRoundedCardMask() {
        for (page in 0..2) {
            val v = miuixCardVisuals(page, page.toFloat(), false)
            assertEquals(0f, v.shadowElevationDp, 0.0001f)
            assertEquals(0f, v.cornerRadiusDp, 0.0001f)
            assertEquals(0f, v.edgeShadeAlpha, 0.0001f)
        }
    }

    @Test fun cardStackOrderNeverChangesDuringLeftOrRightSwipe() {
        assertTrue(miuixPageZIndex(0) < miuixPageZIndex(1))
        assertTrue(miuixPageZIndex(1) < miuixPageZIndex(2))
    }

    @Test fun rightSwipeRevealsArtistBehindTheExistingPlayerCard() {
        val player = miuixCardVisuals(1, 0.5f, true)
        val artist = miuixCardVisuals(0, 0.5f, true)
        assertEquals(16f, player.cornerRadiusDp, 0f)
        assertTrue(player.shadowElevationDp > 0f)
        assertTrue(player.edgeShadeAlpha > 0f)
        assertEquals(0f, artist.shadowElevationDp, 0f)
    }

    @Test fun leftSwipeSlidesLibraryCardOverPlayer() {
        val library = miuixCardVisuals(2, 1.5f, true)
        val player = miuixCardVisuals(1, 1.5f, true)
        assertEquals(16f, library.cornerRadiusDp, 0f)
        assertTrue(library.shadowElevationDp > 0f)
        assertEquals(0f, player.shadowElevationDp, 0f)
    }

    @Test fun reversingEitherGestureKeepsTheSameForegroundCard() {
        for (position in listOf(0.25f, 0.5f, 0.75f)) {
            assertTrue(miuixCardVisuals(1, position, true).shadowElevationDp > 0f)
            assertEquals(0f, miuixCardVisuals(0, position, true).shadowElevationDp, 0f)
        }
        for (position in listOf(1.25f, 1.5f, 1.75f)) {
            assertTrue(miuixCardVisuals(2, position, true).shadowElevationDp > 0f)
            assertEquals(0f, miuixCardVisuals(1, position, true).shadowElevationDp, 0f)
        }
    }

    @Test fun shadowAndEdgeAreBoundedAndSymmetricAroundMidGesture() {
        val one = miuixCardVisuals(1, 0.25f, true)
        val two = miuixCardVisuals(1, 0.75f, true)
        assertEquals(one.shadowElevationDp, two.shadowElevationDp, 0.001f)
        assertEquals(one.edgeShadeAlpha, two.edgeShadeAlpha, 0.001f)
        for (step in 0..100) {
            val v = miuixCardVisuals(1, step / 100f, true)
            assertTrue(v.shadowElevationDp in 0f..9f)
            assertTrue(v.edgeShadeAlpha in 0f..0.19f)
        }
    }

    @Test fun returnedToRestHasNoCardEffectAndInvalidInputsAreSafe() {
        assertEquals(0f, miuixCardVisuals(1, 1f, false).shadowElevationDp, 0f)
        assertEquals(0f, miuixCardVisuals(2, Float.NaN, true).shadowElevationDp, 0f)
        assertEquals(0f, miuixCardVisuals(2, Float.POSITIVE_INFINITY, true).edgeShadeAlpha, 0f)
    }

    @Test fun invisibleNeighboursCannotAcceptTapsOnSettledPlayer() {
        for (page in 0..2) {
            assertEquals(page == 1, miuixPageAcceptsInput(page, 1, 1, 0f, false))
        }
    }

    @Test fun noPageAcceptsPlaybackTapsWhileDraggingOrSettling() {
        for (settled in 0..2) {
            for (current in 0..2) {
                for (page in 0..2) {
                    for (offset in listOf(-0.49f, 0f, 0.49f)) {
                        assertFalse(miuixPageAcceptsInput(page, settled, current, offset, true))
                    }
                }
            }
        }
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
}
