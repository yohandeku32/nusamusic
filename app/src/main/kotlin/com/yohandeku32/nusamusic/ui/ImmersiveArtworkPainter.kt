package com.yohandeku32.nusamusic.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
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
    // Cached, filtered masks keep the progressive vertical fade cheap to draw.
    private val artworkFade by lazy { createMask(dark = false) }
    private val scrim by lazy { createMask(dark = true) }

    private fun createMask(dark: Boolean): ImageBitmap {
        val width = 192
        val height = 256
        val mask = ImageBitmap(width, height)
        val bottom = artworkBottom / frameSize.height.coerceAtLeast(1f) * height
        val span = (transition * if (dark) 1.3f else 1f)
            .coerceIn(1f, artworkBottom.coerceAtLeast(1f)) /
            frameSize.height.coerceAtLeast(1f) * height
        CanvasDrawScope().draw(
            Density(1f), LayoutDirection.Ltr, Canvas(mask), Size(width.toFloat(), height.toFloat())
        ) {
            val stops = Array(33) { step ->
                val progress = step / 32f
                val smooth = progress * progress * (3f - 2f * progress)
                progress to if (dark) Color.Black.copy(alpha = smooth)
                    else Color.White.copy(alpha = 1f - smooth)
            }
            if (dark) {
                // Legacy scrim behaviour remains a straight feather.
                drawRect(
                    brush = Brush.verticalGradient(*stops,
                        startY = bottom - span, endY = bottom),
                    blendMode = BlendMode.Src
                )
            } else {
                // A soft U-shaped alpha mask dissolves the sharp cover into
                // its own real blurred bitmap. Edges feather slightly higher,
                // the centre flows lower: no hard horizontal seam, no tint.
                // Cache once per artwork size rather than per animation frame.
                val curveDepth = (bottom * 0.14f).coerceAtMost(height * 0.11f)
                for (column in 0 until width) {
                    val normalizedX = (column + 0.5f) / width.toFloat()
                    val distance = kotlin.math.abs(normalizedX * 2f - 1f)
                    val edgeLift = distance * distance * curveDepth
                    val curveBottom = bottom - edgeLift
                    drawRect(
                        brush = Brush.verticalGradient(*stops,
                            startY = curveBottom - span,
                            endY = curveBottom),
                        topLeft = Offset(column.toFloat(), 0f),
                        size = Size(1f, height.toFloat()),
                        blendMode = BlendMode.Src
                    )
                }
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

/** Preserve the dominant tonal group instead of mixing black and white into grey. */
internal fun artworkBackdropColor(pixels: IntArray): Color = Color(dominantArtworkBackdrop(pixels))