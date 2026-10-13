package com.yohandeku32.nusamusic.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.roundToInt
import kotlin.random.Random

internal const val VINYL_LABEL_FRACTION = 0.318f

internal fun vinylLabelDiameter(discDiameter: Int): Int {
    if (discDiameter <= 1) return discDiameter.coerceAtLeast(0)
    val diameter = (discDiameter * VINYL_LABEL_FRACTION).roundToInt().coerceIn(2, discDiameter)
    // Matching pixel parity puts the artwork, rings and spindle on the same center.
    return if ((discDiameter - diameter) % 2 == 0) diameter else diameter - 1
}

/** Geometry and brushes are cached at the record's measured size. */
internal class VinylPainter(size: Size) {
    private val radius = size.minDimension / 2f
    private val discCenter = Offset(size.width / 2f, size.height / 2f)
    private val faceRadius = radius * 0.995f
    private val face = Brush.radialGradient(
        0f to Color(0xFF191C1B),
        0.55f to Color(0xFF121515),
        1f to Color(0xFF0D0F0F),
        center = discCenter + Offset(-radius * 0.28f, -radius * 0.35f),
        radius = radius * 1.75f
    )
    private val random = Random(417)
    private val groovePitch = (radius * 0.0048f).coerceAtLeast(1.4f)
    private val grooveCount = (radius * 0.606f / groovePitch).toInt().coerceIn(48, 220)
    private val grooves = List(grooveCount) { index ->
        val progress = index.toFloat() / (grooveCount - 1)
        Groove(
            radius = radius * (0.366f + progress * 0.606f) + (random.nextFloat() - 0.5f) * groovePitch * 0.10f,
            width = groovePitch * (0.28f + random.nextFloat() * 0.12f),
            albedo = 0.040f + random.nextFloat() * 0.045f,
            reflection = (0.42f + random.nextFloat() * 0.50f) * (0.7f + progress * 0.3f)
        )
    }
    private val grain = List(560) {
        val angle = random.nextFloat() * (2 * PI).toFloat()
        val distance = radius * kotlin.math.sqrt(random.nextFloat()) * 0.978f
        Grain(
            position = discCenter + Offset(cos(angle) * distance, sin(angle) * distance),
            radius = 0.18f + random.nextFloat() * 0.25f,
            alpha = 0.008f + random.nextFloat() * 0.015f
        )
    }
    private val recordedArea = annulus(faceRadius, radius * 0.363f)
    private val runOutArea = annulus(radius * 0.363f, radius * 0.323f)
    private val rim = annulus(radius, radius * 0.991f)

    // A feathered light band reveals the groove walls on either side of the label.
    private val reflection = Brush.linearGradient(
        0f to Color.White.copy(alpha = 0f),
        0.10f to Color.White.copy(alpha = 0.005f),
        0.22f to Color.White.copy(alpha = 0.018f),
        0.33f to Color.White.copy(alpha = 0.085f),
        0.42f to Color.White.copy(alpha = 0.20f),
        0.5f to Color.White.copy(alpha = 0.29f),
        0.59f to Color.White.copy(alpha = 0.20f),
        0.70f to Color.White.copy(alpha = 0.08f),
        0.82f to Color.White.copy(alpha = 0.018f),
        0.90f to Color.White.copy(alpha = 0.005f),
        1f to Color.White.copy(alpha = 0f),
        start = discCenter - Offset(0f, radius * 0.30f),
        end = discCenter + Offset(0f, radius * 0.30f)
    )
    private val edgeLight = Brush.linearGradient(
        0f to Color.White.copy(alpha = 0.12f),
        0.4f to Color.White.copy(alpha = 0.025f),
        1f to Color.Black.copy(alpha = 0.55f),
        start = discCenter - Offset(radius, radius),
        end = discCenter + Offset(radius, radius)
    )
    private val spindle = Brush.linearGradient(
        listOf(Color(0xFFC0C0B9), Color(0xFF8D908C), Color(0xFF484C4A)),
        start = discCenter - Offset(radius * 0.01f, radius * 0.01f),
        end = discCenter + Offset(radius * 0.01f, radius * 0.01f)
    )
    private val runOut = Path().apply {
        for (step in 0..240) {
            val progress = step / 240f
            val angle = progress * (4 * PI).toFloat()
            val distance = radius * (0.329f + progress * 0.029f)
            val point = discCenter + Offset(cos(angle) * distance, sin(angle) * distance)
            if (step == 0) moveTo(point.x, point.y) else lineTo(point.x, point.y)
        }
    }

    fun DrawScope.drawFace() {
        drawCircle(Color(0xFF080A09), radius, discCenter)
        drawCircle(face, faceRadius, discCenter)

        for (groove in grooves) {
            drawCircle(
                Color.White.copy(alpha = groove.albedo), groove.radius, discCenter,
                style = Stroke(groove.width)
            )
            drawCircle(
                Color.Black.copy(alpha = 0.34f), groove.radius + groovePitch * 0.36f, discCenter,
                style = Stroke(groovePitch * 0.23f)
            )
        }

        // Smooth gaps between recorded tracks break up the fine groove texture.
        for (band in floatArrayOf(0.468f, 0.591f, 0.717f, 0.851f, 0.948f)) {
            drawCircle(
                Color.Black.copy(alpha = 0.36f), radius * band, discCenter,
                style = Stroke(groovePitch * 1.15f)
            )
        }

        drawPath(runOut, Color.White.copy(alpha = 0.055f), style = Stroke(groovePitch * 0.30f))
        drawCircle(
            Color.Black.copy(alpha = 0.65f), radius * 0.323f, discCenter,
            style = Stroke((radius * 0.006f).coerceAtLeast(0.8f))
        )
        for (sample in grain) {
            drawCircle(Color.White.copy(alpha = sample.alpha), sample.radius, sample.position)
        }
    }

    fun DrawScope.drawReflections() {
        rotate(-2.5f, discCenter) {
            clipRect(right = discCenter.x) {
                drawLightBand(strength = 0.48f)
            }
            clipRect(left = discCenter.x) {
                drawLightBand(strength = 1f)
            }
        }

        clipPath(rim) {
            drawCircle(edgeLight, radius, discCenter)
        }
        drawCircle(
            Color.Black.copy(alpha = 0.45f), radius * 0.986f, discCenter,
            style = Stroke((radius * 0.003f).coerceAtLeast(0.75f))
        )
        drawCircle(
            Color.White.copy(alpha = 0.08f), radius * 0.326f, discCenter,
            style = Stroke((radius * 0.0018f).coerceAtLeast(0.5f))
        )
    }

    private fun DrawScope.drawLightBand(strength: Float) {
        clipPath(recordedArea) {
            drawCircle(reflection, faceRadius, discCenter, alpha = 0.32f * strength)
            for (groove in grooves) {
                drawCircle(
                    reflection, groove.radius, discCenter, alpha = groove.reflection * strength,
                    style = Stroke(groove.width + groovePitch * 0.12f)
                )
            }
        }
        clipPath(runOutArea) {
            drawCircle(reflection, faceRadius, discCenter, alpha = 0.26f * strength)
        }
    }

    fun DrawScope.drawSpindle() {
        drawCircle(
            Color.Black.copy(alpha = 0.4f), radius * 0.016f,
            discCenter + Offset(0f, radius * 0.003f)
        )
        drawCircle(Color(0xFF070909), radius * 0.013f, discCenter)
        drawCircle(spindle, radius * 0.01f, discCenter)
    }

    private fun annulus(outer: Float, inner: Float) = Path().apply {
        fillType = PathFillType.EvenOdd
        addOval(androidx.compose.ui.geometry.Rect(discCenter, outer))
        addOval(androidx.compose.ui.geometry.Rect(discCenter, inner))
    }

    private data class Groove(val radius: Float, val width: Float, val albedo: Float, val reflection: Float)
    private data class Grain(val position: Offset, val radius: Float, val alpha: Float)
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
