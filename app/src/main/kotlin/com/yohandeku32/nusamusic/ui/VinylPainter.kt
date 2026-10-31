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

/** Pressed grooves rotate with the record; specular lighting is drawn in a stationary layer. */
internal class VinylPainter(size: Size) {
    private val radius = size.minDimension / 2f
    private val discCenter = Offset(size.width / 2f, size.height / 2f)
    private val face = Brush.radialGradient(
        0f to Color(0xFF0C0D0D),
        0.57f to Color(0xFF050606),
        0.65f to Color(0xFF101112),
        0.94f to Color(0xFF08090A),
        0.985f to Color(0xFF191B1C),
        1f to Color.Black,
        center = discCenter,
        radius = radius
    )
    private val reflection = Brush.sweepGradient(
        0f to Color.Transparent, 0.25f to Color.White.copy(alpha = 0.025f),
        0.35f to Color(0xFFD6DEE2).copy(alpha = 0.20f),
        0.405f to Color(0xFFE6ECEF).copy(alpha = 0.58f),
        0.47f to Color.White.copy(alpha = 0.10f), 0.54f to Color.Transparent,
        0.70f to Color.Transparent, 0.78f to Color.White.copy(alpha = 0.10f),
        0.845f to Color(0xFFDCE6EB).copy(alpha = 0.52f),
        0.91f to Color.White.copy(alpha = 0.16f), 1f to Color.Transparent,
        center = discCenter
    )
    private val grooves = Random(781).let { random ->
        val count = (radius * 0.326f / 0.85f).roundToInt().coerceIn(48, 240)
        List(count) { index ->
            Groove(
                radius = radius * (0.65f + index.toFloat() / (count - 1) * 0.326f),
                alpha = 0.025f + random.nextFloat() * 0.05f,
                width = 0.3f + random.nextFloat() * 0.25f
            )
        }
    }
    private val sheenBands = List(24) { index ->
        val position = (index + 0.5f) / 24f
        radius * (0.65f + position * 0.326f) to 0.55f * sin(PI * position).toFloat()
    }
    private val sheenWidth = radius * 0.326f / 24f + 0.25f

    fun DrawScope.drawFace() {
        drawCircle(Color.Black, radius, discCenter)
        drawCircle(face, radius * 0.995f, discCenter)
        for (groove in grooves) {
            drawCircle(Color.White.copy(alpha = groove.alpha), groove.radius,
                discCenter, style = Stroke(groove.width))
            drawCircle(Color.Black.copy(alpha = 0.35f), groove.radius + 0.38f,
                discCenter, style = Stroke(0.3f))
        }
    }

    fun DrawScope.drawReflections() {
        for ((bandRadius, strength) in sheenBands) {
            drawCircle(reflection, bandRadius, discCenter, alpha = strength, style = Stroke(sheenWidth))
        }
        for (groove in grooves) {
            val position = (groove.radius / radius - 0.65f) / 0.326f
            val softness = sin(PI * position.coerceIn(0f, 1f)).toFloat()
            drawCircle(reflection, groove.radius, discCenter,
                alpha = (0.32f + groove.alpha * 6f) * (0.3f + 0.7f * softness),
                style = Stroke(groove.width + 0.35f))
        }
        for (band in listOf(0.58f, 0.606f, 0.632f)) {
            drawCircle(reflection, radius * band, discCenter, alpha = 0.16f,
                style = Stroke(0.4f))
        }
        drawCircle(Color.White.copy(alpha = 0.06f), radius * 0.985f, discCenter,
            style = Stroke((radius * 0.004f).coerceAtLeast(0.5f)))
        drawCircle(Color.Black.copy(alpha = 0.65f), radius * 0.997f, discCenter,
            style = Stroke((radius * 0.006f).coerceAtLeast(0.75f)))
    }

    fun DrawScope.drawSpindle() {
        drawCircle(Color(0xFF080808), radius * 0.06f, discCenter)
        drawCircle(Color(0xFFA8A8A8), radius * 0.017f, discCenter)
    }

    private data class Groove(val radius: Float, val alpha: Float, val width: Float)
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
