package com.yohandeku32.nusamusic.ui

import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope

internal const val IMMERSIVE_ARTWORK_HEIGHT_FRACTION = 0.84f

internal class ImmersiveArtworkPainter {
    private val artworkFade = Brush.verticalGradient(
        0f to Color.White,
        0.48f to Color.White,
        0.60f to Color.White.copy(alpha = 0.95f),
        0.75f to Color.White.copy(alpha = 0.72f),
        0.90f to Color.White.copy(alpha = 0.22f),
        0.98f to Color.Transparent,
        1f to Color.Transparent
    )
    private val scrim = Brush.verticalGradient(
        0f to Color.Transparent,
        0.28f to Color.Transparent,
        0.42f to Color.Black.copy(alpha = 0.14f),
        0.55f to Color.Black.copy(alpha = 0.48f),
        0.67f to Color.Black.copy(alpha = 0.66f),
        0.84f to Color.Black.copy(alpha = 0.34f),
        1f to Color.Black.copy(alpha = 0.20f)
    )

    fun DrawScope.fadeArtwork() {
        drawRect(artworkFade, blendMode = BlendMode.DstIn)
    }

    fun DrawScope.drawScrim() {
        drawRect(scrim)
    }
}

/** Muted colour from the lower cover keeps the controls connected to the artwork. */
internal fun artworkBackdropColor(pixels: IntArray): Color {
    if (pixels.isEmpty()) return Color(0xFF141414)
    var red = 0L
    var green = 0L
    var blue = 0L
    for (pixel in pixels) {
        red += (pixel shr 16) and 0xFF
        green += (pixel shr 8) and 0xFF
        blue += pixel and 0xFF
    }
    return Color(
        red = (red / pixels.size * 0.30f).toInt(),
        green = (green / pixels.size * 0.30f).toInt(),
        blue = (blue / pixels.size * 0.30f).toInt()
    )
}
