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
        0f to Color(0xFF131414),
        0.55f to Color(0xFF0D0E0E),
        1f to Color(0xFF090A0A),
        center = discCenter + Offset(-radius * 0.28f, -radius * 0.35f),
        radius = radius * 1.75f
    )
    private val random = Random(417)
    private val groovePitch = (radius * 0.0030f).coerceAtLeast(1f)
    private val grooveCount = (radius * 0.584f / groovePitch).toInt().coerceIn(56, 260)
    private val grooves = List(grooveCount) { index ->
        val progress = index.toFloat() / (grooveCount - 1)
        Groove(
            radius = radius * (0.402f + progress * 0.584f) + (random.nextFloat() - 0.5f) * groovePitch * 0.10f,
            width = groovePitch * (0.26f + random.nextFloat() * 0.16f),
            albedo = 0.022f + random.nextFloat() * 0.030f,
            reflection = (0.40f + random.nextFloat() * 0.52f) * (0.7f + progress * 0.3f)
        )
    }
    private val masteringBands = floatArrayOf(
        0.408f, 0.431f, 0.455f, 0.486f, 0.510f, 0.536f, 0.572f,
        0.593f, 0.631f, 0.659f, 0.694f, 0.718f, 0.750f, 0.783f,
        0.804f, 0.839f, 0.867f, 0.902f, 0.935f, 0.960f
    ).map { fraction ->
        MasteringBand(radius * fraction, groovePitch * (1.25f + random.nextFloat() * 1.8f))
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
    private val recordedArea = annulus(faceRadius, radius * 0.398f)
    private val runOutArea = annulus(radius * 0.398f, radius * 0.352f)
    private val rim = annulus(radius, radius * 0.991f)

    // Broad asymmetric sheen reveals the cut groove walls without a straight light stripe.
    private val reflection = Brush.radialGradient(
        0f to Color.White.copy(alpha = 0.14f),
        0.42f to Color.White.copy(alpha = 0.105f),
        0.72f to Color.White.copy(alpha = 0.035f),
        1f to Color.Transparent,
        center = discCenter + Offset(-radius * 0.38f, radius * 0.80f),
        radius = radius * 1.08f
    )
    private val counterLight = Brush.radialGradient(
        0f to Color.White.copy(alpha = 0.045f),
        0.5f to Color.White.copy(alpha = 0.020f),
        1f to Color.Transparent,
        center = discCenter + Offset(-radius * 0.75f, -radius * 0.70f),
        radius = radius * 1.15f
    )
    private val edgeLight = Brush.linearGradient(
        0f to Color.White.copy(alpha = 0.10f),
        0.4f to Color.White.copy(alpha = 0.025f),
        1f to Color.Black.copy(alpha = 0.55f),
        start = discCenter - Offset(radius, radius),
        end = discCenter + Offset(radius, radius)
    )
    // Directional, off-axis light grazing the microscopic groove walls.
    // This layer rotates with the vinyl: its highlight rolls across the surface
    // while the cast shadow under the record stays fixed to the turntable.
    private val movingSpecular = Brush.sweepGradient(
        0f to Color.Transparent,
        0.07f to Color.White.copy(alpha = 0.012f),
        0.15f to Color.White.copy(alpha = 0.085f),
        0.205f to Color.White.copy(alpha = 0.14f),
        0.245f to Color.White.copy(alpha = 0.025f),
        0.31f to Color.Transparent,
        0.51f to Color.Transparent,
        0.62f to Color.White.copy(alpha = 0.045f),
        0.69f to Color.White.copy(alpha = 0.080f),
        0.76f to Color.Transparent,
        1f to Color.Transparent,
        center = discCenter
    )
    private val grooveIridescence = Brush.sweepGradient(
        0f to Color(0xFF687986).copy(alpha = 0.015f),
        0.18f to Color.White.copy(alpha = 0.11f),
        0.24f to Color(0xFF9AA9B4).copy(alpha = 0.018f),
        0.48f to Color.Transparent,
        0.68f to Color.White.copy(alpha = 0.052f),
        0.83f to Color.Transparent,
        1f to Color(0xFF687986).copy(alpha = 0.015f),
        center = discCenter
    )
    // A visible steel pin and bevel, proportional to the record on every screen.
    private val spindleRadius = radius * 0.035f
    private val spindleShadowCenter = discCenter + Offset(spindleRadius * 0.22f, spindleRadius * 0.32f)
    private val spindleShadow = Brush.radialGradient(
        0f to Color.Black.copy(alpha = 0.65f),
        0.55f to Color.Black.copy(alpha = 0.40f),
        1f to Color.Transparent,
        center = spindleShadowCenter,
        radius = spindleRadius * 1.65f
    )
    private val spindleBevel = Brush.sweepGradient(
        0f to Color(0xFF59636B),
        0.18f to Color(0xFFDAE1E5),
        0.38f to Color(0xFF68747D),
        0.58f to Color(0xFFF5F7F8),
        0.78f to Color(0xFFABB5BC),
        1f to Color(0xFF59636B),
        center = discCenter
    )
    private val spindle = Brush.radialGradient(
        0f to Color(0xFFFAFCFD),
        0.30f to Color(0xFFDCE3E8),
        0.65f to Color(0xFF9EAAB4),
        1f to Color(0xFF45515D),
        center = discCenter - Offset(spindleRadius * 0.30f, spindleRadius * 0.35f),
        radius = spindleRadius * 1.45f
    )
    private val labelWell = Brush.radialGradient(
        0f to Color(0xFF090A0A),
        0.91f to Color(0xFF090A0A),
        0.97f to Color(0xFF050606),
        1f to Color(0xFF181919),
        center = discCenter,
        radius = radius * 0.352f
    )
    private val runOut = Path().apply {
        for (step in 0..240) {
            val progress = step / 240f
            val angle = progress * (4 * PI).toFloat()
            val distance = radius * (0.357f + progress * 0.036f)
            val point = discCenter + Offset(cos(angle) * distance, sin(angle) * distance)
            if (step == 0) moveTo(point.x, point.y) else lineTo(point.x, point.y)
        }
    }

    fun DrawScope.drawFace() {
        drawCircle(Color(0xFF070808), radius, discCenter)
        drawCircle(face, faceRadius, discCenter)

        for (groove in grooves) {
            drawCircle(
                Color.White.copy(alpha = groove.albedo), groove.radius, discCenter,
                style = Stroke(groove.width)
            )
            drawCircle(
                Color.Black.copy(alpha = 0.42f), groove.radius + groovePitch * 0.36f, discCenter,
                style = Stroke(groovePitch * 0.23f)
            )
        }

        // Uneven wider cuts sit between the closely spaced fine grooves.
        for (band in masteringBands) {
            drawCircle(
                Color.Black.copy(alpha = 0.46f), band.radius, discCenter,
                style = Stroke(band.width)
            )
            drawCircle(
                Color.White.copy(alpha = 0.035f), band.radius - band.width * 0.5f, discCenter,
                style = Stroke(groovePitch * 0.26f)
            )
        }

        drawPath(runOut, Color.White.copy(alpha = 0.055f), style = Stroke(groovePitch * 0.30f))
        for (sample in grain) {
            drawCircle(Color.White.copy(alpha = sample.alpha), sample.radius, sample.position)
        }
        drawCircle(labelWell, radius * 0.352f, discCenter)
    }

    fun DrawScope.drawReflections() {
        // Specular light should skim the groove walls, not cover the album label.
        // Both shaders are cached at this record size and rotate as one layer.
        clipPath(recordedArea) {
            drawCircle(movingSpecular, faceRadius, discCenter)
            for ((index, groove) in grooves.withIndex()) {
                if (index % 2 == 0) {
                    drawCircle(
                        grooveIridescence, groove.radius, discCenter,
                        style = Stroke((groove.width * 0.78f).coerceAtLeast(0.35f))
                    )
                }
            }
        }
        drawLightPool(reflection)
        drawLightPool(counterLight)

        clipPath(rim) {
            drawCircle(edgeLight, radius, discCenter)
        }
        drawCircle(
            Color.Black.copy(alpha = 0.45f), radius * 0.986f, discCenter,
            style = Stroke((radius * 0.003f).coerceAtLeast(0.75f))
        )
        drawCircle(
            Color.White.copy(alpha = 0.035f), radius * 0.352f, discCenter,
            style = Stroke((radius * 0.0018f).coerceAtLeast(0.5f))
        )
    }

    private fun DrawScope.drawLightPool(light: Brush) {
        clipPath(recordedArea) {
            drawCircle(light, faceRadius, discCenter, alpha = 0.52f)
            for (groove in grooves) {
                drawCircle(
                    light, groove.radius, discCenter, alpha = groove.reflection,
                    style = Stroke(groove.width + groovePitch * 0.12f)
                )
            }
            for (band in masteringBands) {
                drawCircle(
                    Color.Black.copy(alpha = 0.20f), band.radius, discCenter,
                    style = Stroke(band.width * 0.75f)
                )
                drawCircle(
                    light, band.radius - band.width * 0.5f, discCenter, alpha = 0.42f,
                    style = Stroke(groovePitch * 0.28f)
                )
            }
        }
        clipPath(runOutArea) {
            drawCircle(light, faceRadius, discCenter, alpha = 0.20f)
        }
    }

    fun DrawScope.drawSpindle() {
        drawCircle(spindleShadow, spindleRadius * 1.65f, spindleShadowCenter)
        // Dark contact ring and machined bevel separate the metal from any label colour.
        drawCircle(Color(0xFF151B20), spindleRadius * 1.18f, discCenter)
        drawCircle(spindleBevel, spindleRadius * 1.10f, discCenter)
        drawCircle(spindle, spindleRadius * 0.88f, discCenter)
        drawCircle(
            Color.White.copy(alpha = 0.32f), spindleRadius * 0.82f, discCenter,
            style = Stroke(spindleRadius * 0.055f)
        )
        drawCircle(
            Color.White.copy(alpha = 0.72f), spindleRadius * 0.18f,
            discCenter - Offset(spindleRadius * 0.30f, spindleRadius * 0.35f)
        )
    }

    private fun annulus(outer: Float, inner: Float) = Path().apply {
        fillType = PathFillType.EvenOdd
        addOval(androidx.compose.ui.geometry.Rect(discCenter, outer))
        addOval(androidx.compose.ui.geometry.Rect(discCenter, inner))
    }

    private data class Groove(val radius: Float, val width: Float, val albedo: Float, val reflection: Float)
    private data class MasteringBand(val radius: Float, val width: Float)
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
