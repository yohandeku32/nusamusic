package com.yohandeku32.nusamusic.ui

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs

/** The dominant artwork hue plus a genuinely represented, distinct complementary artwork hue. */
data class VinylArtworkPalette(val primary: Color, val secondary: Color = primary)

/** One small bitmap sample and one background-worker pass; never sample album art per animation frame. */
internal suspend fun vinylArtworkPalette(bitmap: Bitmap): VinylArtworkPalette? = withContext(Dispatchers.Default) {
    if (bitmap.isRecycled) return@withContext null
    val sample = Bitmap.createScaledBitmap(bitmap, 32, 32, true)
    try {
        val pixels = IntArray(32 * 32)
        sample.getPixels(pixels, 0, 32, 0, 0, 32, 32)
        if (pixels.none { it ushr 24 >= 128 }) return@withContext null
        val dominant = dominantArtworkBackdrop(pixels)
        val secondary = vinylArtworkCompanion(pixels, dominant) ?: dominant
        VinylArtworkPalette(Color(dominant), Color(secondary))
    } finally {
        if (sample !== bitmap) sample.recycle()
    }
}

/** Kept for the existing Vinyl/ArtworkView callers that need only a primary hue. */
internal suspend fun vinylArtworkColor(bitmap: Bitmap): Color? =
    vinylArtworkPalette(bitmap)?.primary

private fun vinylArtworkCompanion(pixels: IntArray, dominant: Int): Int? {
    // Group the sampled artwork into 20-degree hue families. Only colors occupying
    // a meaningful region can become a second ambient color: tiny text/logo pixels
    // and near-white/black noise should not tint the entire player.
    val counts = IntArray(18)
    val sumsRed = LongArray(18)
    val sumsGreen = LongArray(18)
    val sumsBlue = LongArray(18)
    val sumsSaturation = FloatArray(18)
    val hsl = FloatArray(3)
    val primaryHsl = FloatArray(3)
    ColorUtils.colorToHSL(dominant, primaryHsl)
    var visiblePixels = 0

    for (pixel in pixels) {
        if (pixel ushr 24 < 128) continue
        visiblePixels++
        ColorUtils.colorToHSL(pixel, hsl)
        if (hsl[1] < 0.16f || hsl[2] < 0.11f || hsl[2] > 0.91f) continue
        val index = (hsl[0] / 20f).toInt().coerceIn(0, 17)
        counts[index]++
        sumsRed[index] += android.graphics.Color.red(pixel).toLong()
        sumsGreen[index] += android.graphics.Color.green(pixel).toLong()
        sumsBlue[index] += android.graphics.Color.blue(pixel).toLong()
        sumsSaturation[index] += hsl[1]
    }

    val minimumRegion = maxOf(14, (visiblePixels * 0.045f).toInt())
    var selected = -1
    var topScore = 0f
    for (i in counts.indices) {
        val count = counts[i]
        if (count < minimumRegion) continue
        val hueCenter = i * 20f + 10f
        val distance = abs(hueCenter - primaryHsl[0])
        val separation = minOf(distance, 360f - distance)
        if (primaryHsl[1] >= 0.15f && separation < 32f) continue
        val meanSaturation = sumsSaturation[i] / count
        val score = count * (0.6f + meanSaturation * 0.55f) *
            (0.85f + separation / 180f * 0.15f)
        if (score > topScore) {
            topScore = score
            selected = i
        }
    }
    if (selected < 0) return null
    val count = counts[selected]
    return android.graphics.Color.rgb(
        (sumsRed[selected] / count).toInt(),
        (sumsGreen[selected] / count).toInt(),
        (sumsBlue[selected] / count).toInt()
    )
}

internal fun vinylArtworkTint(color: Color?): Color {
    if (color == null) return VinylPaper
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(color.toArgb(), hsl)
    // Bright but muted paper tints preserve both artwork identity and readable text.
    hsl[1] = (hsl[1] * 0.82f).coerceAtMost(0.60f)
    hsl[2] = 0.82f
    var tint = ColorUtils.HSLToColor(hsl)
    repeat(12) {
        if (ColorUtils.calculateContrast(0xFF505055.toInt(), tint) >= 4.5 &&
            ColorUtils.calculateContrast(0xFF686760.toInt(), tint) >= 3.0) return Color(tint)
        // Float blending can truncate an opaque alpha to 254; the paper must stay opaque.
        tint = ColorUtils.setAlphaComponent(ColorUtils.blendARGB(tint, VinylPaper.toArgb(), 0.09f), 255)
    }
    return VinylPaper
}
