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
