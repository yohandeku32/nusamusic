package com.yohandeku32.nusamusic.ui

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class VinylArtworkTintTest {
    @Test fun artworkSamplingPreservesTheCoverHueAndDoesNotRecycleSharedArtwork() = runBlocking {
        val bitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(0xFF328145.toInt())
        val tint = vinylArtworkTint(vinylArtworkColor(bitmap))
        assertTrue(tint.green > tint.red && tint.green > tint.blue)
        assertFalse(bitmap.isRecycled)
        bitmap.eraseColor(0xFF8852AC.toInt())
        val purple = vinylArtworkTint(vinylArtworkColor(bitmap))
        assertTrue(purple.blue > purple.green && purple.red > purple.green)
        bitmap.recycle()
    }

    @Test fun realTwoColorArtworkProducesAnActualSecondHue() = runBlocking {
        val bitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
        for (y in 0 until 64) for (x in 0 until 64) {
            bitmap.setPixel(x, y, if (x < 35) 0xFF2751BA.toInt() else 0xFFE07828.toInt())
        }
        try {
            val palette = requireNotNull(vinylArtworkPalette(bitmap))
            val first = FloatArray(3)
            val second = FloatArray(3)
            ColorUtils.colorToHSL(palette.primary.toArgb(), first)
            ColorUtils.colorToHSL(palette.secondary.toArgb(), second)
            val distance = kotlin.math.abs(first[0] - second[0])
            assertTrue("A split blue/orange cover should supply two distinct real hues",
                minOf(distance, 360f - distance) >= 32f)
            assertEquals("Legacy primary extraction still agrees with the palette",
                palette.primary, vinylArtworkColor(bitmap))
            for (color in listOf(palette.primary, palette.secondary)) {
                assertTrue("Tint must retain readable artist metadata",
                    ColorUtils.calculateContrast(0xFF505055.toInt(), vinylArtworkTint(color).toArgb()) >= 4.5)
            }
            assertFalse(bitmap.isRecycled)
        } finally {
            bitmap.recycle()
        }
    }

    @Test fun oneColorAndNeutralArtworkAvoidInventingASecondHue() = runBlocking {
        val bitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
        try {
            bitmap.eraseColor(0xFF8C4FC1.toInt())
            val solid = requireNotNull(vinylArtworkPalette(bitmap))
            assertEquals("Solid covers must not invent a complementary accent", solid.primary, solid.secondary)
            bitmap.eraseColor(0xFF666666.toInt())
            val gray = requireNotNull(vinylArtworkPalette(bitmap))
            assertEquals("Monochrome album art stays monochrome", gray.primary, gray.secondary)
            bitmap.eraseColor(android.graphics.Color.TRANSPARENT)
            assertNull("Transparent/missing art has no palette", vinylArtworkPalette(bitmap))
        } finally {
            bitmap.recycle()
        }
    }

    @Test fun everyHueKeepsArtistTextReadableAndMissingArtworkStaysNeutral() {
        assertEquals(VinylPaper, vinylArtworkTint(null))
        for (hue in 0..359 step 5) {
            val color = Color(ColorUtils.HSLToColor(floatArrayOf(hue.toFloat(), 1f, 0.4f)))
            assertTrue(ColorUtils.calculateContrast(0xFF505055.toInt(), vinylArtworkTint(color).toArgb()) >= 4.5)
            assertTrue(ColorUtils.calculateContrast(0xFF686760.toInt(), vinylArtworkTint(color).toArgb()) >= 3.0)
        }
        val gray = vinylArtworkTint(Color.Gray)
        assertEquals(gray.red, gray.green, 0.01f)
        assertEquals(gray.green, gray.blue, 0.01f)
    }
}
