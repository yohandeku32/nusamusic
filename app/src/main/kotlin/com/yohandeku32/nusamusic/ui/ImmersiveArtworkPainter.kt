package com.yohandeku32.nusamusic.ui

import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope

internal fun resolveArtworkBottom(artistBottom: Float, frameHeight: Float): Float {
    if (frameHeight <= 0f) return 0f
    return (if (artistBottom.isFinite()) artistBottom else frameHeight * 0.56f)
        .coerceIn(1f.coerceAtMost(frameHeight), frameHeight)
}

internal class ImmersiveArtworkPainter(artworkBottom: Float, transition: Float) {
    private val transitionStart = (artworkBottom - transition).coerceAtLeast(0f)
    private val artworkFade = Brush.verticalGradient(
        0f to Color.White,
        0.20f to Color.White.copy(alpha = 0.94f),
        0.50f to Color.White.copy(alpha = 0.60f),
        0.80f to Color.White.copy(alpha = 0.12f),
        0.98f to Color.Transparent,
        1f to Color.Transparent,
        startY = transitionStart,
        endY = artworkBottom
    )
    private val scrim = Brush.verticalGradient(
        0f to Color.Transparent,
        0.35f to Color.Black.copy(alpha = 0.12f),
        0.72f to Color.Black.copy(alpha = 0.40f),
        1f to Color.Black.copy(alpha = 0.58f),
        startY = (artworkBottom - transition * 1.5f).coerceAtLeast(0f),
        endY = artworkBottom
    )

    fun DrawScope.fadeArtwork() {
        drawRect(artworkFade, blendMode = BlendMode.DstIn)
    }

    fun DrawScope.drawScrim() {
        drawRect(scrim)
    }
}

/** Muted colour used while the blurred cover is loading. */
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
