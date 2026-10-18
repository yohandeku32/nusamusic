package com.yohandeku32.nusamusic.ui

import org.junit.Assert.*
import org.junit.Test

class AmbientArtworkBlendTest {
    @Test fun neutralArtworkDoesNotGetAnInventedTint() {
        val neutral = intArrayOf(0xFF343434.toInt(), 0xFF888888.toInt(), 0xFFCCCCCC.toInt())
        assertArrayEquals(neutral, blendAmbientArtwork(neutral, neutral))
    }

    @Test fun keepColourAreasFromTheRealArtworkInsteadOfReplacingThemWithAverage() {
        val purpleBase = IntArray(6) { 0xFF803080.toInt() }
        val artworkShapes = intArrayOf(
            0xFFE52223.toInt(), 0xFFE52223.toInt(), 0xFFD72B37.toInt(),
            0xFF2861C9.toInt(), 0xFF2861C9.toInt(), 0xFF1951D9.toInt()
        )
        val result = blendAmbientArtwork(purpleBase, artworkShapes)
        val firstRed = (result[0] ushr 16) and 255
        val firstBlue = result[0] and 255
        val lastRed = (result[5] ushr 16) and 255
        val lastBlue = result[5] and 255
        assertTrue("the red portion of the cover must remain warm", firstRed > firstBlue)
        assertTrue("the blue portion of the cover must remain cool", lastBlue > lastRed)
        assertNotEquals(result[0], result[5])
    }

    @Test fun blendingRetainsMoreSoftStructureThanHeavyBlurAlone() {
        val width = 64
        val height = 32
        val original = IntArray(width * height) { index ->
            if (index % width < width / 2) 0xFFE25422.toInt() else 0xFF2363D9.toInt()
        }
        val broad = blurArgb(original, width, height, radius = 9, passes = 2)
        val detailed = blurArgb(original, width, height, radius = 3, passes = 2)
        val ambient = blendAmbientArtwork(broad, detailed)
        val edgeLeft = 15 * width + 28
        val edgeRight = 15 * width + 35
        val channelDifference = kotlin.math.abs(((ambient[edgeLeft] ushr 16) and 255) -
            ((ambient[edgeRight] ushr 16) and 255))
        val broadDifference = kotlin.math.abs(((broad[edgeLeft] ushr 16) and 255) -
            ((broad[edgeRight] ushr 16) and 255))
        assertTrue("retain faint album shapes under diffusion", channelDifference > broadDifference)
        assertTrue("still blurred, not an untreated full-res image",
            channelDifference < kotlin.math.abs(((original[edgeLeft] ushr 16) and 255) -
                ((original[edgeRight] ushr 16) and 255)))
    }

    @Test fun processingDoesNotMutatePrecomputedArtworkAndPreservesOpaqueAlpha() {
        val broad = intArrayOf(0xFF941E22.toInt(), 0xFF172882.toInt())
        val detail = intArrayOf(0xFFA31418.toInt(), 0xFF222EBE.toInt())
        val originalBroad = broad.copyOf()
        val originalDetail = detail.copyOf()
        val result = blendAmbientArtwork(broad, detail)
        assertArrayEquals(originalBroad, broad)
        assertArrayEquals(originalDetail, detail)
        assertTrue(result.all { (it ushr 24) == 255 })
    }

    @Test(expected = IllegalArgumentException::class)
    fun inconsistentBitmapSizesAreRejected() {
        blendAmbientArtwork(intArrayOf(0), intArrayOf(0, 0))
    }
}
