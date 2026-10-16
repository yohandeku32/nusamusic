package com.yohandeku32.nusamusic.ui

import org.junit.Assert.*
import org.junit.Test

class ImmersivePaletteTest {
    @Test fun mostlyBlackCoverStaysBlackDespiteWhiteTypography() {
        val pixels = IntArray(100) { if (it < 70) 0xFF080808.toInt() else 0xFFFFFFFF.toInt() }
        assertEquals(0xFF060606.toInt(), dominantArtworkBackdrop(pixels))
    }
    @Test fun dominantColourSurvivesSmallWhiteAndBlackLabels() {
        val pixels = IntArray(100) {
            when { it < 80 -> 0xFF204080.toInt(); it < 90 -> -1; else -> 0xFF000000.toInt() }
        }
        val colour = dominantArtworkBackdrop(pixels)
        assertTrue((colour and 255) > ((colour ushr 16) and 255) * 2)
    }
    @Test fun brightCoverIsNotForcedToBlack() {
        assertEquals(0xFFC6C6C6.toInt(), dominantArtworkBackdrop(IntArray(64) { -1 }))
    }
    @Test fun transparentPixelsDoNotDecidePalette() {
        assertEquals(0xFF141414.toInt(), dominantArtworkBackdrop(IntArray(64)))
        assertEquals(0xFF141414.toInt(), dominantArtworkBackdrop(intArrayOf()))
    }
    @Test fun equalBlackAndWhiteAreasPreferDarkTone() {
        assertEquals(0xFF000000.toInt(), dominantArtworkBackdrop(intArrayOf(-1, 0xFF000000.toInt())))
    }
}
