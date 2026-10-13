package com.yohandeku32.nusamusic.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate

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
    val progress = if (durationMs > 0L) {
        (positionMs.toFloat() / durationMs).coerceIn(0f, 1f)
    } else 0f

    // Park outside the disc. During playback, track its grooves but not the artwork.
    val targetAngle = if (isPlaying && hasSong) progress * (if (mountedOnDeck) 14f else 12f) else -11f
    val armAngle by animateFloatAsState(
        targetValue = targetAngle,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = if (isPlaying && hasSong) 35f else 90f
        ),
        label = "tonearmAngle"
    )

    Canvas(modifier) {
        val unit = size.minDimension
        val pivot = Offset(
            size.width * (if (mountedOnDeck) 0.86f else 0.88f),
            size.height * (if (mountedOnDeck) 0.175f else 0.145f)
        )
        fun point(x: Float, y: Float) = pivot + Offset(x * unit, y * unit)

        // Fixed, low-profile bearing: visibly smaller than the old assembly.
        drawCircle(Color.Black.copy(alpha = 0.12f), radius = unit * 0.046f, center = point(0.006f, 0.008f))
        drawCircle(Color(0xFF343A3C), radius = unit * 0.039f, center = pivot)
        drawCircle(
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFFF5F6F6), Color(0xFF858E91), Color(0xFFD9DDDF)),
                start = point(-0.035f, -0.035f),
                end = point(0.035f, 0.035f)
            ),
            radius = unit * 0.032f,
            center = pivot
        )

        rotate(degrees = armAngle, pivot = pivot) {
            // Compact counterweight and slim curved chrome tube.
            drawRoundRect(
                brush = Brush.horizontalGradient(listOf(Color(0xFF454A4C), Color(0xFFE8EBEC), Color(0xFF696F72))),
                topLeft = point(-0.027f, -0.108f),
                size = Size(unit * 0.054f, unit * 0.090f),
                cornerRadius = CornerRadius(unit * 0.010f)
            )
            drawLine(
                color = Color(0xFF262B2D),
                start = point(-0.027f, -0.048f),
                end = point(0.027f, -0.048f),
                strokeWidth = unit * 0.006f
            )

            val tube = Path().apply {
                moveTo(point(0f, 0.034f).x, point(0f, 0.034f).y)
                cubicTo(
                    point(-0.012f, 0.230f).x, point(-0.012f, 0.230f).y,
                    point(0.070f, 0.355f).x, point(0.070f, 0.355f).y,
                    point(-0.050f, 0.470f).x, point(-0.050f, 0.470f).y
                )
                cubicTo(
                    point(-0.095f, 0.515f).x, point(-0.095f, 0.515f).y,
                    point(-0.110f, 0.538f).x, point(-0.110f, 0.538f).y,
                    point(-0.132f, 0.547f).x, point(-0.132f, 0.547f).y
                )
            }
            drawPath(tube, Color.Black.copy(alpha = 0.13f), style = Stroke(width = unit * 0.019f, cap = StrokeCap.Round))
            drawPath(tube, Color(0xFF5D6569), style = Stroke(width = unit * 0.013f, cap = StrokeCap.Round))
            drawPath(
                tube,
                brush = Brush.linearGradient(
                    listOf(Color(0xFFFCFDFD), Color(0xFFB2BBC0), Color(0xFFFCFDFD)),
                    start = point(-0.12f, 0.35f),
                    end = point(0.065f, 0.35f)
                ),
                style = Stroke(width = unit * 0.008f, cap = StrokeCap.Round)
            )

            // Small angled headshell and stylus, clear of the central label.
            val cartridge = Path().apply {
                moveTo(point(-0.128f, 0.540f).x, point(-0.128f, 0.540f).y)
                lineTo(point(-0.106f, 0.565f).x, point(-0.106f, 0.565f).y)
                lineTo(point(-0.145f, 0.601f).x, point(-0.145f, 0.601f).y)
                lineTo(point(-0.171f, 0.580f).x, point(-0.171f, 0.580f).y)
                close()
            }
            drawPath(cartridge, Color(0xFF252A2C))
            drawLine(
                color = Color(0xFFC9D0D2),
                start = point(-0.146f, 0.581f),
                end = point(-0.170f, 0.615f),
                strokeWidth = unit * 0.006f,
                cap = StrokeCap.Round
            )
            drawCircle(Color(0xFF33383A), radius = unit * 0.004f, center = point(-0.170f, 0.615f))
        }

        // The hub stays fixed while only the arm rotates.
        drawCircle(Color(0xFF222729), radius = unit * 0.020f, center = pivot)
        drawCircle(Color(0xFFC6CDCF), radius = unit * 0.012f, center = pivot)
        drawCircle(Color(0xFF5F686C), radius = unit * 0.005f, center = pivot)
    }
}
