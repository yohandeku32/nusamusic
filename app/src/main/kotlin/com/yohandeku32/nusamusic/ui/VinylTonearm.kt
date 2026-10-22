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

/**
 * Minimal brushed-silver tonearm, inspired by a real turntable on a pale surface.
 * The cartridge follows the record from its outer groove inward as the song plays.
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
    val progress = if (durationMs > 0L) {
        (positionMs.toFloat() / durationMs).coerceIn(0f, 1f)
    } else 0f

    // A restrained sweep into the inner grooves; pause keeps the current position.
    val targetAngle = if (hasSong) -1f + progress * 11f else -12f
    val armAngle by animateFloatAsState(
        targetValue = targetAngle,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = if (isPlaying && hasSong) 38f else 105f
        ),
        label = "tonearmAngle"
    )

    Canvas(modifier) {
        val unit = size.minDimension
        val pivot = Offset(
            size.width * (if (mountedOnDeck) 0.90f else 0.90f),
            size.height * (if (mountedOnDeck) 0.145f else 0.145f)
        )
        fun point(x: Float, y: Float) = pivot + Offset(x * unit, y * unit)
        fun Path.move(x: Float, y: Float) {
            val p = point(x, y)
            moveTo(p.x, p.y)
        }
        fun Path.line(x: Float, y: Float) {
            val p = point(x, y)
            lineTo(p.x, p.y)
        }
        fun Path.curve(
            x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float
        ) {
            val a = point(x1, y1)
            val b = point(x2, y2)
            val c = point(x3, y3)
            cubicTo(a.x, a.y, b.x, b.y, c.x, c.y)
        }
        fun armPath(dx: Float = 0f, dy: Float = 0f) = Path().apply {
            move(dx, 0.030f + dy)
            curve(
                -0.005f + dx, 0.180f + dy,
                -0.020f + dx, 0.344f + dy,
                -0.075f + dx, 0.461f + dy
            )
            curve(
                -0.091f + dx, 0.489f + dy,
                -0.112f + dx, 0.510f + dy,
                -0.140f + dx, 0.519f + dy
            )
        }

        // The bearing shadow stays fixed while the slender tonearm pivots.
        drawCircle(
            Color.Black.copy(alpha = 0.095f),
            radius = unit * 0.052f,
            center = pivot + Offset(unit * 0.011f, unit * 0.015f)
        )

        rotate(degrees = armAngle, pivot = pivot) {
            // Small machined counterweight, not a bulky black mechanical block.
            drawRoundRect(
                brush = Brush.horizontalGradient(
                    listOf(Color(0xFF9BA3A5), Color(0xFFFEFEFC), Color(0xFFC5CDCE))
                ),
                topLeft = point(-0.027f, -0.096f),
                size = Size(unit * 0.054f, unit * 0.093f),
                cornerRadius = CornerRadius(unit * 0.012f)
            )
            drawRoundRect(
                color = Color(0xFF9AA5A8).copy(alpha = 0.64f),
                topLeft = point(-0.026f, -0.093f),
                size = Size(unit * 0.005f, unit * 0.086f),
                cornerRadius = CornerRadius(unit * 0.002f)
            )

            // Realistic soft cast shadow falls to the right of the raised metal tube.
            drawPath(
                armPath(dx = 0.012f, dy = 0.013f),
                color = Color(0xFF33383A).copy(alpha = 0.14f),
                style = Stroke(width = unit * 0.020f, cap = StrokeCap.Round)
            )
            val tube = armPath()
            drawPath(
                tube,
                color = Color(0xFF899396),
                style = Stroke(width = unit * 0.020f, cap = StrokeCap.Round)
            )
            drawPath(
                tube,
                brush = Brush.linearGradient(
                    listOf(Color(0xFFF9FAF8), Color(0xFFDEE3E3), Color(0xFFB3BEC1)),
                    start = point(0f, 0f),
                    end = point(-0.14f, 0.55f)
                ),
                style = Stroke(width = unit * 0.014f, cap = StrokeCap.Round)
            )
            drawPath(
                armPath(dx = -0.003f),
                color = Color.White.copy(alpha = 0.68f),
                style = Stroke(width = unit * 0.0030f, cap = StrokeCap.Round)
            )

            // Reference-inspired off-white headshell and very fine dark stylus.
            val shellShadow = Path().apply {
                move(-0.146f, 0.511f)
                line(-0.181f, 0.535f)
                line(-0.160f, 0.556f)
                line(-0.123f, 0.532f)
                close()
            }
            drawPath(
                shellShadow,
                color = Color.Black.copy(alpha = 0.15f)
            )
            val shell = Path().apply {
                move(-0.151f, 0.501f)
                line(-0.188f, 0.528f)
                line(-0.166f, 0.547f)
                line(-0.128f, 0.523f)
                close()
            }
            drawPath(
                shell,
                brush = Brush.linearGradient(
                    listOf(Color(0xFFFFFFFF), Color(0xFFE5E8E7), Color(0xFFBFC8CA)),
                    start = point(-0.15f, 0.50f),
                    end = point(-0.17f, 0.55f)
                )
            )
            drawLine(
                Color(0xFF454D50),
                point(-0.172f, 0.543f),
                point(-0.185f, 0.560f),
                strokeWidth = unit * 0.0035f,
                cap = StrokeCap.Round
            )
            drawCircle(
                Color(0xFF697275),
                radius = unit * 0.0028f,
                center = point(-0.185f, 0.560f)
            )
        }

        // Low-profile silver pivot cover floats naturally above the pale backdrop.
        drawCircle(Color(0xFF8E999D), unit * 0.037f, pivot)
        drawCircle(
            brush = Brush.linearGradient(
                listOf(Color(0xFFFFFFFF), Color(0xFFE6EBEA), Color(0xFFAAB5B9)),
                start = point(-0.033f, -0.035f),
                end = point(0.033f, 0.035f)
            ),
            radius = unit * 0.031f,
            center = pivot
        )
        drawCircle(
            Color.White.copy(alpha = 0.67f),
            radius = unit * 0.010f,
            center = pivot + Offset(-unit * 0.006f, -unit * 0.007f)
        )
    }
}
