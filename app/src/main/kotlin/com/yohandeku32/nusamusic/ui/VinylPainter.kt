package com.yohandeku32.nusamusic.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.roundToInt
import kotlin.random.Random

internal const val VINYL_LABEL_FRACTION = 0.56f

internal fun vinylLabelDiameter(discDiameter: Int): Int {
    if (discDiameter <= 1) return discDiameter.coerceAtLeast(0)
    val diameter = (discDiameter * VINYL_LABEL_FRACTION).roundToInt().coerceIn(2, discDiameter)
    // Matching pixel parity puts the artwork, rings and spindle on the same center.
    return if ((discDiameter - diameter) % 2 == 0) diameter else diameter - 1
}

/** Brushes and the original fine groove geometry are cached at the measured size. */
internal class VinylPainter(size: Size) {
    private val radius = size.minDimension / 2f
    private val discCenter = Offset(size.width / 2f, size.height / 2f)
    private val face = Brush.radialGradient(
        0f to Color(0xFF323232),
        0.24f to Color(0xFF1A1A1A),
        0.52f to Color(0xFF090909),
        0.78f to Color(0xFF020202),
        1f to Color.Black,
        center = Offset(size.width * 0.32f, size.height * 0.27f),
        radius = radius * 1.08f
    )
    private val reflection = Brush.radialGradient(
        0f to Color.White.copy(alpha = 0.085f),
        0.22f to Color.White.copy(alpha = 0.042f),
        0.52f to Color.White.copy(alpha = 0.012f),
        1f to Color.Transparent,
        center = Offset(size.width * 0.27f, size.height * 0.19f),
        radius = radius * 0.90f
    )
    private val grooves = List(179) { index ->
        Groove(
            radius = radius * (0.232f + index / 178f * 0.742f),
            alpha = when {
                index % 31 == 0 -> 0.082f
                index % 13 == 0 -> 0.042f
                index % 5 == 0 -> 0.019f
                else -> 0.0075f
            },
            width = if (index % 31 == 0) 0.90f else 0.28f,
            hasDarkEdge = index % 7 == 0
        )
    }

    fun DrawScope.drawFace() {
        drawCircle(Color.Black, radius, discCenter)
        drawCircle(face, radius * 0.995f, discCenter)
        for (groove in grooves) {
            drawCircle(Color.White.copy(alpha = groove.alpha), groove.radius,
                discCenter, style = Stroke(groove.width))
            if (groove.hasDarkEdge) {
                drawCircle(Color.Black.copy(alpha = 0.20f), groove.radius + 0.55f,
                    discCenter, style = Stroke(0.65f))
            }
        }
    }

    fun DrawScope.drawReflections() {
        drawCircle(reflection, radius * 0.995f, discCenter)
        drawCircle(Color.White.copy(alpha = 0.06f), radius * 0.985f, discCenter,
            style = Stroke((radius * 0.004f).coerceAtLeast(0.5f)))
        drawCircle(Color.Black.copy(alpha = 0.65f), radius * 0.997f, discCenter,
            style = Stroke((radius * 0.006f).coerceAtLeast(0.75f)))
    }

    fun DrawScope.drawSpindle() {
        drawCircle(Color(0xFF080808), radius * 0.06f, discCenter)
        drawCircle(Color(0xFFA8A8A8), radius * 0.017f, discCenter)
    }

    private data class Groove(val radius: Float, val alpha: Float, val width: Float, val hasDarkEdge: Boolean)
}

internal class VinylLabelPainter(size: Size) {
    private val radius = size.minDimension / 2f
    private val labelCenter = Offset(size.width / 2f, size.height / 2f)
    private val shading = Brush.radialGradient(
        0f to Color.Transparent,
        0.85f to Color.Black.copy(alpha = 0.015f),
        0.98f to Color.Black.copy(alpha = 0.10f),
        1f to Color.Black.copy(alpha = 0.24f),
        center = labelCenter,
        radius = radius
    )
    private val random = Random(2047)
    private val grain = List(180) {
        val angle = random.nextFloat() * (2 * PI).toFloat()
        val distance = radius * kotlin.math.sqrt(random.nextFloat())
        labelCenter + Offset(cos(angle) * distance, sin(angle) * distance)
    }

    fun DrawScope.drawFinish() {
        drawCircle(Color(0xFF302C24).copy(alpha = 0.045f), radius, labelCenter)
        drawCircle(shading, radius, labelCenter)
        for ((index, point) in grain.withIndex()) {
            drawCircle(
                if (index % 2 == 0) Color.White.copy(alpha = 0.035f)
                else Color.Black.copy(alpha = 0.025f),
                radius = 0.3f,
                center = point
            )
        }
        val borderWidth = (radius * 0.008f).coerceAtLeast(0.6f)
        drawCircle(
            Color.Black.copy(alpha = 0.20f), radius - borderWidth * 0.5f - 0.25f, labelCenter,
            style = Stroke(borderWidth)
        )
    }
}
