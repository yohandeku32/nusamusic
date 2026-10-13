package com.yohandeku32.nusamusic.ui

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.roundToInt

internal fun resolveArtworkBottom(artistBottom: Float, frameHeight: Float): Float {
    if (frameHeight <= 0f) return 0f
    return (if (artistBottom.isFinite()) artistBottom else frameHeight * 0.56f)
        .coerceIn(1f.coerceAtMost(frameHeight), frameHeight)
}

internal class ImmersiveArtworkPainter(
    private val frameSize: Size,
    private val artworkBottom: Float,
    private val transition: Float
) {
    // Cached, filtered masks make the broad curved fade a single draw per frame.
    private val artworkFade by lazy { createMask(dark = false) }
    private val scrim by lazy { createMask(dark = true) }

    private fun createMask(dark: Boolean): ImageBitmap {
        val width = 96
        val height = 256
        val mask = ImageBitmap(width, height)
        val bottom = artworkBottom / frameSize.height.coerceAtLeast(1f) * height
        val span = (transition * if (dark) 1.3f else 1f)
            .coerceIn(1f, artworkBottom.coerceAtLeast(1f)) /
            frameSize.height.coerceAtLeast(1f) * height
        CanvasDrawScope().draw(
            Density(1f), LayoutDirection.Ltr, Canvas(mask), Size(width.toFloat(), height.toFloat())
        ) {
            drawRect(if (dark) Color.Black.copy(alpha = 0.58f) else Color.Transparent)
            val contour = Path()
            for (step in 192 downTo 0) {
                val progress = step / 192f
                val smooth = progress * progress * (3f - 2f * progress)
                val y = bottom - span + span * progress
                contour.reset()
                contour.moveTo(0f, -height.toFloat())
                contour.lineTo(0f, y - span * 0.28f)
                contour.cubicTo(
                    width * 0.24f, y + span * 0.10f,
                    width * 0.64f, y - span * 0.03f,
                    width.toFloat(), y - span * 0.18f
                )
                contour.lineTo(width.toFloat(), -height.toFloat())
                contour.close()
                val colour = if (dark) Color.Black.copy(alpha = 0.58f * smooth)
                    else Color.White.copy(alpha = 1f - smooth)
                drawPath(contour, colour, blendMode = BlendMode.Src)
            }
        }
        return mask
    }

    private fun DrawScope.drawMask(mask: ImageBitmap, blendMode: BlendMode) {
        drawImage(
            mask,
            dstSize = IntSize(size.width.roundToInt().coerceAtLeast(1), size.height.roundToInt().coerceAtLeast(1)),
            filterQuality = FilterQuality.Low,
            blendMode = blendMode
        )
    }

    fun DrawScope.fadeArtwork() {
        drawMask(artworkFade, BlendMode.DstIn)
    }

    fun DrawScope.drawScrim() {
        drawMask(scrim, BlendMode.SrcOver)
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
