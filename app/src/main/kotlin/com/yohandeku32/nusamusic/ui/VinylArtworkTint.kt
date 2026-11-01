package com.yohandeku32.nusamusic.ui

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal suspend fun vinylArtworkColor(bitmap: Bitmap): Color? = withContext(Dispatchers.Default) {
    if (bitmap.isRecycled) return@withContext null
    val sample = Bitmap.createScaledBitmap(bitmap, 32, 32, true)
    try {
        val pixels = IntArray(32 * 32)
        sample.getPixels(pixels, 0, 32, 0, 0, 32, 32)
        if (pixels.none { it ushr 24 >= 128 }) null else Color(dominantArtworkBackdrop(pixels))
    } finally {
        if (sample !== bitmap) sample.recycle()
    }
}

internal fun vinylArtworkTint(color: Color?): Color {
    if (color == null) return VinylPaper
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(color.toArgb(), hsl)
    hsl[1] = hsl[1].coerceAtMost(0.65f)
    hsl[2] = 0.80f
    var tint = ColorUtils.HSLToColor(hsl)
    // Artist metadata remains readable even for strongly saturated cover hues.
    repeat(10) {
        if (ColorUtils.calculateContrast(0xFF505055.toInt(), tint) >= 4.5) return Color(tint)
        tint = ColorUtils.blendARGB(tint, VinylPaper.toArgb(), 0.08f)
    }
    return VinylPaper
}
