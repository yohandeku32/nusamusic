package com.yohandeku32.nusamusic.ui

import org.junit.Assert.*
import org.junit.Test

class ImmersiveSheetPhysicsTest {
    @Test fun panelStopsJustBelowSafeTopInsetOnMultipleDisplays() {
        for ((height, safeInset, gap) in listOf(
            Triple(1280, 42, 16),
            Triple(2340, 72, 24),
            Triple(2700, 88, 24),
            Triple(900, 24, 12)
        )) {
            val top = immersiveLyricsSheetTopPx(safeInset, height, gap)
            assertEquals(safeInset + gap, top)
            assertEquals(height - top, height - immersiveLyricsSheetTopPx(safeInset, height, gap))
            assertTrue(top < height / 2) // never the previous half-window sheet
        }
    }

    @Test fun heightAndInsetsAreClampedToTheViewport() {
        assertEquals(0, immersiveLyricsSheetTopPx(-50, 1000, -20))
        assertEquals(1000, immersiveLyricsSheetTopPx(975, 1000, 50))
        assertEquals(0, immersiveLyricsSheetTopPx(500, -1, 10))
        assertEquals(1000, immersiveLyricsSheetTopPx(Int.MAX_VALUE, 1000, Int.MAX_VALUE))
    }

    @Test fun slowDragRequiresADeliberateDistance() {
        val v = 900f
        assertFalse(shouldDismissImmersiveLyricsSheet(0f, 0f, v))
        assertFalse(shouldDismissImmersiveLyricsSheet(0.279f, 0f, v))
        assertTrue(shouldDismissImmersiveLyricsSheet(0.28f, 0f, v))
        assertTrue(shouldDismissImmersiveLyricsSheet(0.75f, 0f, v))
    }

    @Test fun velocityDrivesNaturalReleaseButUpwardFlickAlwaysReopens() {
        assertTrue(shouldDismissImmersiveLyricsSheet(0.05f, 1100f, 900f))
        assertFalse(shouldDismissImmersiveLyricsSheet(0.85f, -1100f, 900f))
        assertFalse(shouldDismissImmersiveLyricsSheet(0.15f, 500f, 900f))
        assertTrue(shouldDismissImmersiveLyricsSheet(0.5f, -100f, 900f))
    }

    @Test fun partialDragsBehaveConsistentlyOn60_90_120HzScreens() {
        for (hz in listOf(60, 90, 120)) {
            val height = 1600f
            var fraction = 0f
            for (frame in 0 until hz / 3) {
                val pixels = (height * 0.23f) / (hz / 3)
                fraction = (fraction + pixels / height).coerceIn(0f, 1f)
            }
            assertEquals(0.23f, fraction, 0.0001f)
            assertFalse(shouldDismissImmersiveLyricsSheet(fraction, 0f, 900f))
        }
    }

    @Test fun repeatedOpeningAndClosingNeverDependOnStaticHalfSheetHeight() {
        val heights = listOf(700, 800, 1200, 2000)
        for (height in heights) {
            val top = immersiveLyricsSheetTopPx(78, height, 12)
            val sheetHeight = height - top
            assertEquals(height - 90, sheetHeight)
            assertTrue(sheetHeight > height / 2)
        }
    }
}
