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

    // Follow the grooves slowly; the stylus parks outside the disc.
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
        fun pos(x: Float, y: Float) = pivot + Offset(x * unit, y * unit)
        fun Path.move(x: Float, y: Float) {
            val p = pos(x, y)
            moveTo(p.x, p.y)
        }
        fun Path.line(x: Float, y: Float) {
            val p = pos(x, y)
            lineTo(p.x, p.y)
        }
        fun Path.curve(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float) {
            val a = pos(x1, y1)
            val b = pos(x2, y2)
            val c = pos(x3, y3)
            cubicTo(a.x, a.y, b.x, b.y, c.x, c.y)
        }

        // Subtle bearing mount and cueing lever, both fixed to the deck.
        drawCircle(Color.Black.copy(alpha = 0.09f), unit * 0.063f, pos(0.005f, 0.005f))
        drawCircle(Color(0xFFB6BEC1), unit * 0.054f, pivot)
        val rest = Path().apply {
            move(-0.155f, 0.115f)
            curve(-0.107f, 0.151f, -0.063f, 0.160f, -0.010f, 0.142f)
        }
        drawPath(rest, Color(0xFF252B2E), style = Stroke(unit * 0.013f, cap = StrokeCap.Round))
        drawLine(
            Color(0xFF879094), pos(0.017f, 0.145f), pos(0.147f, 0.145f),
            strokeWidth = unit * 0.006f, cap = StrokeCap.Round
        )
        drawLine(
            Color(0xFF252B2E), pos(0.111f, 0.145f), pos(0.151f, 0.145f),
            strokeWidth = unit * 0.008f, cap = StrokeCap.Round
        )

        rotate(degrees = armAngle, pivot = pivot) {
            // Brushed silver cylindrical counterweight and short black stem.
            drawRoundRect(
                color = Color(0xFF171C1E),
                topLeft = pos(-0.018f, -0.067f),
                size = Size(unit * 0.036f, unit * 0.080f),
                cornerRadius = CornerRadius(unit * 0.004f)
            )
            drawRoundRect(
                brush = Brush.horizontalGradient(
                    listOf(Color(0xFF667075), Color(0xFFF7FAFB), Color(0xFFADB5B9), Color(0xFF4C565B))
                ),
                topLeft = pos(-0.055f, -0.128f),
                size = Size(unit * 0.110f, unit * 0.064f),
                cornerRadius = CornerRadius(unit * 0.008f)
            )
            drawCircle(Color(0xFF323B3F), unit * 0.005f, pos(0f, -0.093f))

            // Straight black tube with a restrained bend, like the reference.
            val tube = Path().apply {
                move(0f, 0.042f)
                curve(0.001f, 0.215f, -0.011f, 0.413f, -0.043f, 0.563f)
            }
            drawPath(tube, Color.Black.copy(alpha = 0.16f), style = Stroke(unit * 0.025f, cap = StrokeCap.Round))
            drawPath(tube, Color(0xFF161B1D), style = Stroke(unit * 0.018f, cap = StrokeCap.Round))
            drawPath(
                tube,
                Color(0xFF4D565A).copy(alpha = 0.75f),
                style = Stroke(unit * 0.003f, cap = StrokeCap.Round)
            )

            // Small red tube keeper.
            drawRoundRect(
                color = Color(0xFFB13B35),
                topLeft = pos(-0.027f, 0.382f),
                size = Size(unit * 0.034f, unit * 0.028f),
                cornerRadius = CornerRadius(unit * 0.003f)
            )
            drawLine(
                Color(0xFF171B1D), pos(-0.010f, 0.382f), pos(-0.012f, 0.410f),
                strokeWidth = unit * 0.019f, cap = StrokeCap.Round
            )

            // Narrow headshell clamp with two metal fasteners.
            drawLine(
                Color(0xFF25292B), pos(-0.081f, 0.561f), pos(-0.004f, 0.566f),
                strokeWidth = unit * 0.014f, cap = StrokeCap.Round
            )
            drawCircle(Color(0xFFE0E5E6), unit * 0.005f, pos(-0.077f, 0.561f))
            drawCircle(Color(0xFFE0E5E6), unit * 0.005f, pos(-0.010f, 0.566f))

            // Angled black cartridge with a fine needle at the outer grooves.
            val headshell = Path().apply {
                move(-0.054f, 0.567f)
                line(-0.020f, 0.582f)
                line(-0.101f, 0.656f)
                line(-0.145f, 0.638f)
                close()
            }
            drawPath(headshell, Color(0xFF222729))
            drawLine(
                Color(0xFF697174), pos(-0.096f, 0.651f), pos(-0.137f, 0.635f),
                strokeWidth = unit * 0.003f
            )
            drawCircle(Color(0xFFEDF0F1), unit * 0.005f, pos(-0.113f, 0.629f))
            drawLine(
                Color(0xFF171B1D), pos(-0.143f, 0.639f), pos(-0.170f, 0.650f),
                strokeWidth = unit * 0.006f, cap = StrokeCap.Round
            )
            drawCircle(Color(0xFFCED3D5), unit * 0.003f, pos(-0.170f, 0.650f))
        }

        // The silver circular pivot face remains stationary while the arm swings.
        drawCircle(Color(0xFF697277), unit * 0.050f, pivot)
        drawCircle(
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFFFFFFFF), Color(0xFFE4E9EB), Color(0xFFABB5BA)),
                start = pos(-0.042f, -0.042f),
                end = pos(0.042f, 0.042f)
            ),
            radius = unit * 0.044f,
            center = pivot
        )
        drawCircle(Color(0xFFB0B8BC).copy(alpha = 0.65f), unit * 0.044f, pivot, style = Stroke(unit * 0.0015f))
    }
}
