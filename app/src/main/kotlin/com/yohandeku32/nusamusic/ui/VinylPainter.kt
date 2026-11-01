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

internal const val VINYL_LABEL_FRACTION = 0.50f

internal fun vinylLabelDiameter(discDiameter: Int, compactLabel: Boolean = false): Int {
    if (discDiameter <= 1) return discDiameter.coerceAtLeast(0)
    val diameter = (discDiameter * if (compactLabel) 0.32f else VINYL_LABEL_FRACTION).roundToInt().coerceIn(2, discDiameter)
    // Matching pixel parity puts the artwork, rings and spindle on the same center.
    return if ((discDiameter - diameter) % 2 == 0) diameter else diameter - 1
}

/** Grooves and material sheen share the record's rotation so movement remains visible outside the cover. */
internal class VinylPainter(size: Size, compactLabel: Boolean = false) {
    private val grooveStart = if (compactLabel) 0.42f else 0.58f
    private val grooveSpan = if (compactLabel) 0.556f else 0.396f
    private val collar = if (compactLabel) 0.40f else 0.56f
    private val collarEdge = if (compactLabel) 0.405f else 0.565f
    private val radius = size.minDimension / 2f
    private val discCenter = Offset(size.width / 2f, size.height / 2f)
    private val face = Brush.radialGradient(
        0f to Color(0xFF0C0D0D),
        (if (compactLabel) 0.41f else 0.57f) to Color(0xFF050606),
        (if (compactLabel) 0.49f else 0.65f) to Color(0xFF101112),
        0.94f to Color(0xFF08090A),
        0.985f to Color(0xFF191B1C),
        1f to Color.Black,
        center = discCenter,
        radius = radius
    )
    private val reflection = Brush.sweepGradient(
        0f to Color.Transparent,
        0.17f to Color.Transparent,
        0.25f to Color.White.copy(alpha = 0.06f),
        0.34f to Color.Transparent,
        0.57f to Color.Transparent,
        0.66f to Color.White.copy(alpha = 0.04f),
        0.74f to Color.White.copy(alpha = 0.28f),
        0.81f to Color.White.copy(alpha = 0.09f),
        0.90f to Color.Transparent,
        1f to Color.Transparent,
        center = discCenter
    )
    private val grooves = Random(781).let { random ->
        val count = (radius * grooveSpan / 0.85f).roundToInt().coerceIn(48, 240)
        List(count) { index ->
            Groove(
                radius = radius * (grooveStart + index.toFloat() / (count - 1) * grooveSpan),
                alpha = 0.012f + random.nextFloat() * 0.025f,
                width = 0.3f + random.nextFloat() * 0.25f
            )
        }
    }
    private val sheenBands = List(24) { index ->
        val position = (index + 0.5f) / 24f
        radius * (grooveStart + position * grooveSpan) to 0.55f * sin(PI * position).toFloat()
    }
    private val sheenWidth = radius * grooveSpan / 24f + 0.25f

    fun DrawScope.drawFace() {
        drawCircle(Color.Black, radius, discCenter)
        drawCircle(face, radius * 0.995f, discCenter)
        drawCircle(Color(0xFF080909), radius * collar, discCenter)
        for (groove in grooves) {
            drawCircle(Color.White.copy(alpha = groove.alpha), groove.radius,
                discCenter, style = Stroke(groove.width))
            drawCircle(Color.Black.copy(alpha = 0.35f), groove.radius + 0.38f,
                discCenter, style = Stroke(0.3f))
        }
        for (band in listOf(0.62f, 0.78f, 0.90f)) {
            drawCircle(Color.Black.copy(alpha = 0.55f), radius * band, discCenter,
                style = Stroke((radius * 0.006f).coerceAtLeast(0.7f)))
        }
        // Small variations in the pressed surface turn with the label beneath fixed lighting.
        drawArc(Color.White.copy(alpha = 0.09f), 18f, 9f, false,
            topLeft = discCenter - Offset(radius * 0.84f, radius * 0.84f),
            size = Size(radius * 1.68f, radius * 1.68f), style = Stroke(0.6f))
        drawArc(Color.White.copy(alpha = 0.06f), 192f, 6f, false,
            topLeft = discCenter - Offset(radius * 0.94f, radius * 0.94f),
            size = Size(radius * 1.88f, radius * 1.88f), style = Stroke(0.5f))
    }

    fun DrawScope.drawReflections() {
        for ((bandRadius, strength) in sheenBands) {
            drawCircle(reflection, bandRadius, discCenter, alpha = strength, style = Stroke(sheenWidth))
        }
        for (groove in grooves) {
            val position = (groove.radius / radius - grooveStart) / grooveSpan
            val softness = sin(PI * position.coerceIn(0f, 1f)).toFloat()
            drawCircle(reflection, groove.radius, discCenter,
                alpha = (0.32f + groove.alpha * 6f) * (0.3f + 0.7f * softness),
                style = Stroke(groove.width + 0.35f))
        }
        for (band in listOf(collarEdge)) {
            drawCircle(reflection, radius * band, discCenter, alpha = 0.16f,
                style = Stroke(0.4f))
        }
        drawCircle(Color.White.copy(alpha = 0.06f), radius * 0.985f, discCenter,
            style = Stroke((radius * 0.004f).coerceAtLeast(0.5f)))
        drawCircle(Color.Black.copy(alpha = 0.65f), radius * 0.997f, discCenter,
            style = Stroke((radius * 0.006f).coerceAtLeast(0.75f)))
    }

    fun DrawScope.drawSpindle() {
        drawCircle(Color(0xFF080808), radius * 0.025f, discCenter + Offset(0.4f, 0.7f))
        drawCircle(Color(0xFF9B9E9B), radius * 0.017f, discCenter)
        drawCircle(Color(0xFFE3E5DF), radius * 0.007f,
            discCenter - Offset(radius * 0.004f, radius * 0.004f))
    }

    private data class Groove(val radius: Float, val alpha: Float, val width: Float)
}

internal class VinylLabelPainter(size: Size) {
    private val radius = size.minDimension / 2f
    private val labelCenter = Offset(size.width / 2f, size.height / 2f)
    private val random = Random(2047)
    private val grain = List(180) {
        val angle = random.nextFloat() * (2 * PI).toFloat()
        val distance = radius * kotlin.math.sqrt(random.nextFloat())
        labelCenter + Offset(cos(angle) * distance, sin(angle) * distance)
    }

    fun DrawScope.drawFinish() {
        drawCircle(Color(0xFF302C24).copy(alpha = 0.045f), radius, labelCenter)
        for ((index, point) in grain.withIndex()) {
            drawCircle(
                if (index % 2 == 0) Color.White.copy(alpha = 0.035f)
                else Color.Black.copy(alpha = 0.025f),
                radius = 0.3f,
                center = point
            )
        }
    }
}
