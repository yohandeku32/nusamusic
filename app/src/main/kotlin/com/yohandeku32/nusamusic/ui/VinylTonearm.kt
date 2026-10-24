package com.yohandeku32.nusamusic.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.sqrt

/**
 * Pinterest-reference straight white tonearm.
 * A narrow white tube with a continuous satin/soft-shadow finish extends
 * diagonally from the right-hand bearing down to the vinyl grooves.
 * No decorative elbow, curved arm, dark beam or oversized cartridge.
 */
@Composable
fun VinylTonearm(
    isPlaying: Boolean,
    hasSong: Boolean,
    positionMsState: State<Long>,
    durationMs: Long,
    mountedOnDeck: Boolean = false,
    modifier: Modifier = Modifier
) {
    val positionMs by positionMsState
    val fraction = if (durationMs > 0L) {
        (positionMs.toFloat() / durationMs).coerceIn(0f, 1f)
    } else 0f

    // The stylus glides slowly inward, preserving its position on pause.
    val armAngle by animateFloatAsState(
        targetValue = if (hasSong) fraction * 8f else -7f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = if (isPlaying && hasSong) 38f else 100f
        ),
        label = "referenceWhiteTonearm"
    )

    Canvas(modifier) {
        val unit = size.minDimension
        if (unit <= 0f) return@Canvas

        // The disc center is around 26.5% of viewport width in the
        // reference-matched turntable layout. Position the needle on its groove,
        // near the right-hand upper quadrant of the record.
        val pivot = Offset(size.width * 0.91f, size.height * 0.095f)
        val shaftStart = pivot + Offset(-unit * 0.006f, unit * 0.025f)
        val stylusContact = Offset(size.width * 0.705f, size.height * 0.582f)

        val dx = stylusContact.x - shaftStart.x
        val dy = stylusContact.y - shaftStart.y
        val length = sqrt(dx * dx + dy * dy).coerceAtLeast(1f)
        val direction = Offset(dx / length, dy / length)
        val perpendicular = Offset(-direction.y, direction.x)

        val shaftEnd = stylusContact - direction * (unit * 0.048f)
        val cartridgeStart = stylusContact - direction * (unit * 0.057f)
        val cartridgeEnd = stylusContact + direction * (unit * 0.010f)
        val shadowOffset = Offset(unit * 0.009f, unit * 0.010f)

        rotate(degrees = armAngle, pivot = pivot) {
            // Continuous soft contact shadow: no thick dark outline.
            drawLine(
                color = Color(0xFF737779).copy(alpha = 0.07f),
                start = shaftStart + shadowOffset,
                end = cartridgeEnd + shadowOffset,
                strokeWidth = unit * 0.033f,
                cap = StrokeCap.Round
            )
            drawLine(
                color = Color(0xFF656B6C).copy(alpha = 0.12f),
                start = shaftStart + shadowOffset * 0.46f,
                end = shaftEnd + shadowOffset * 0.46f,
                strokeWidth = unit * 0.019f,
                cap = StrokeCap.Round
            )

            // One perfectly straight slender white body. Three aligned
            // strokes create a brushed-white metallic tube, not a bend.
            drawLine(
                color = Color(0xFFA9B1B2),
                start = shaftStart,
                end = shaftEnd,
                strokeWidth = unit * 0.0165f,
                cap = StrokeCap.Round
            )
            drawLine(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFFDFDFC),
                        Color(0xFFF3F5F4),
                        Color(0xFFD6DFDF)
                    ),
                    start = shaftStart + perpendicular * (unit * 0.008f),
                    end = shaftStart - perpendicular * (unit * 0.009f)
                ),
                start = shaftStart,
                end = shaftEnd,
                strokeWidth = unit * 0.014f,
                cap = StrokeCap.Round
            )
            drawLine(
                color = Color.White.copy(alpha = 0.90f),
                start = shaftStart - perpendicular * (unit * 0.0035f),
                end = shaftEnd - perpendicular * (unit * 0.0035f),
                strokeWidth = unit * 0.003f,
                cap = StrokeCap.Round
            )

            // A small tapered ivory headshell continues the straight tube.
            // Keep its scale close to the white stylus housing in the photo.
            val shell = Path().apply {
                val leftBack = cartridgeStart + perpendicular * (unit * 0.015f)
                val rightBack = cartridgeStart - perpendicular * (unit * 0.015f)
                val rightTip = cartridgeEnd - perpendicular * (unit * 0.011f)
                val leftTip = cartridgeEnd + perpendicular * (unit * 0.011f)
                moveTo(leftBack.x, leftBack.y)
                lineTo(rightBack.x, rightBack.y)
                lineTo(rightTip.x, rightTip.y)
                lineTo(leftTip.x, leftTip.y)
                close()
            }
            drawPath(
                path = shell,
                brush = Brush.linearGradient(
                    listOf(Color(0xFFFFFFFF), Color(0xFFF6F7F5), Color(0xFFC8D0D0)),
                    start = cartridgeEnd - perpendicular * (unit * 0.015f),
                    end = cartridgeEnd + perpendicular * (unit * 0.015f)
                )
            )
            drawPath(
                path = shell,
                color = Color(0xFF899496).copy(alpha = 0.32f),
                style = Stroke(width = unit * 0.0017f)
            )

            // Extremely fine needle and small shadow make the vinyl contact
            // legible without looking like a large black stylus.
            val needleRoot = cartridgeEnd
            val needleTip = stylusContact + direction * (unit * 0.021f)
            drawLine(
                color = Color(0xFF4D5658),
                start = needleRoot,
                end = needleTip,
                strokeWidth = unit * 0.0026f,
                cap = StrokeCap.Round
            )
            drawCircle(
                color = Color(0xFF61696B),
                radius = unit * 0.0021f,
                center = needleTip
            )

            // Small, clean white extension behind the pivot.
            drawLine(
                color = Color(0xFFAAB3B4),
                start = shaftStart - direction * (unit * 0.055f),
                end = shaftStart,
                strokeWidth = unit * 0.019f,
                cap = StrokeCap.Round
            )
            drawLine(
                color = Color(0xFFF9FAF8),
                start = shaftStart - direction * (unit * 0.055f),
                end = shaftStart,
                strokeWidth = unit * 0.014f,
                cap = StrokeCap.Round
            )
        }

        // Fixed low-profile satin white pivot; no black bracket or bulky mount.
        drawCircle(
            color = Color(0xFF969FA1).copy(alpha = 0.19f),
            radius = unit * 0.038f,
            center = pivot + Offset(unit * 0.007f, unit * 0.009f)
        )
        drawCircle(
            brush = Brush.linearGradient(
                listOf(Color(0xFFFDFEFC), Color(0xFFDDE3E2), Color(0xFF9EA9AB)),
                start = pivot - Offset(unit * 0.030f, unit * 0.030f),
                end = pivot + Offset(unit * 0.030f, unit * 0.030f)
            ),
            radius = unit * 0.027f,
            center = pivot
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.80f),
            radius = unit * 0.010f,
            center = pivot - Offset(unit * 0.006f, unit * 0.006f)
        )
    }
}
