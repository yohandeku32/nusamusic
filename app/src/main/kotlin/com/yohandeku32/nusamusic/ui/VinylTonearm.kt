package com.yohandeku32.nusamusic.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.*
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
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate

@Composable
fun VinylTonearm(
    isPlaying: Boolean,
    hasSong: Boolean,
    positionMsState: State<Long>,
    durationMs: Long,
    modifier: Modifier = Modifier
) {
    val position by positionMsState
    val progress = if (durationMs > 0L) (position.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
    val angle by animateFloatAsState(
        if (hasSong) 1f + progress * 8f else -15f,
        animationSpec = if (android.animation.ValueAnimator.areAnimatorsEnabled())
            spring(dampingRatio = 1f, stiffness = if (isPlaying) 180f else 220f) else snap(),
        label = "tonearmAngle"
    )
    val tube = remember {
        Path().apply {
            moveTo(317f, 50f)
            lineTo(298f, 156f)
            lineTo(286f, 224f)
            cubicTo(282f, 250f, 272f, 264f, 244f, 289f)
        }
    }
    val cartridge = remember {
        Path().apply {
            moveTo(239f, 281f); lineTo(252f, 292f)
            lineTo(234f, 311f); lineTo(221f, 300f); close()
        }
    }
    val plate = remember {
        Path().apply {
            moveTo(239f, 286f); lineTo(247f, 293f)
            lineTo(233f, 307f); lineTo(226f, 300f); close()
        }
    }
    val metal = remember {
        Brush.horizontalGradient(listOf(Color(0xFF41484D), Color(0xFFE5E9EB),
            Color(0xFF969FA5), Color(0xFFF3F5F5), Color(0xFF59646D)), 220f, 321f)
    }
    val weight = remember {
        Brush.horizontalGradient(listOf(Color(0xFF484F55), Color(0xFFDDE1E3),
            Color(0xFF969FA6), Color(0xFF4B5258)), 312f, 336f)
    }
    Canvas(modifier) {
        // One 360-unit mechanical drawing keeps the pivot, tube and cartridge concentric.
        scale(size.minDimension / 360f, pivot = Offset.Zero) {
            val pivot = Offset(317f, 50f)
            drawCircle(Color.Black.copy(alpha = 0.13f), 29f, pivot + Offset(2f, 3f))
            drawCircle(Color(0xFF59636A), 26f, pivot)
            drawCircle(Color(0xFF252D34), 23f, pivot)
            drawCircle(Color(0xFFB5BDC3), 15f, pivot)
            drawCircle(Color(0xFF4B5660), 12f, pivot)
            drawCircle(Color(0xFF6D7881), 8f, pivot)
            drawCircle(Color.White.copy(alpha = 0.24f), 14f, pivot, style = Stroke(1f))

            rotate(angle, pivot) {
                drawLine(Color(0xFF4B554D), Offset(319f, 35f), Offset(325f, 1f), 7f, StrokeCap.Round)
                rotate(10f, Offset(326f, 8f)) {
                    drawRoundRect(Color.Black.copy(alpha = 0.12f), Offset(315f, 0f), Size(26f, 22f), CornerRadius(2f))
                    drawRoundRect(weight, Offset(312f, -2f), Size(26f, 20f), CornerRadius(2f))
                    drawLine(Color(0xFFE6EAE2), Offset(315f, -1f), Offset(315f, 16f), 2f)
                    drawRoundRect(Color(0xFF434C45), Offset(320f, -10f), Size(9f, 9f), CornerRadius(1f))
                    drawLine(Color(0xFFB4BDB2), Offset(322f, -9f), Offset(322f, -3f), 1f)
                }
                translate(2f, 3f) { drawPath(tube, Color.Black.copy(alpha = 0.22f), style = Stroke(9f, cap = StrokeCap.Round)) }
                drawPath(tube, Color(0xFF434F46), style = Stroke(7f, cap = StrokeCap.Round))
                drawPath(tube, metal, style = Stroke(4.5f, cap = StrokeCap.Round))
                translate(-0.8f, -0.5f) { drawPath(tube, Color(0xFFE7EBE2).copy(alpha = 0.72f), style = Stroke(1f, cap = StrokeCap.Round)) }
                drawCircle(Color(0xFF9EA99E), 5f, pivot)
                drawCircle(Color(0xFF576358), 2f, pivot)

                translate(1f, 2f) { drawPath(cartridge, Color.Black.copy(alpha = 0.30f)) }
                drawPath(cartridge, Color(0xFF2C3742))
                drawPath(plate, Brush.linearGradient(listOf(Color(0xFFDFE4E7), Color(0xFF8D9AA4)),
                    Offset(228f, 290f), Offset(242f, 307f)))
                drawCircle(Color(0xFF394A3C), 1.8f, Offset(239f, 293f))
                drawCircle(Color(0xFF394A3C), 1.8f, Offset(233f, 300f))
                drawLine(Color(0xFF303F32), Offset(228f, 308f), Offset(224f, 313f), 2f, StrokeCap.Round)
                drawCircle(Color(0xFFE2E7DD), 1.2f, Offset(224f, 313f))
            }
        }
    }
}
