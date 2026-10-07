package com.yohandeku32.nusamusic.player

import android.graphics.Bitmap
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Image
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * CD jewel-case player style.
 *
 * The CD itself rotates as a single physical layer while the transparent
 * jewel-case shell and hinge/latch details stay fixed, matching the physical
 * reference. Album artwork is reused as the CD label so no extra cover asset
 * is required.
 */
@Composable
fun CDCasePlayerStyle(
    artwork: Bitmap?,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val rotation = remember { mutableFloatStateOf(0f) }
    val rotationSpeed = remember { Animatable(0f) }

    LaunchedEffect(isPlaying) {
        rotationSpeed.animateTo(
            targetValue = if (isPlaying) 360f / 7.2f else 0f,
            animationSpec = tween(
                durationMillis = 1100,
                easing = FastOutSlowInEasing
            )
        )
    }

    LaunchedEffect(isPlaying) {
        var lastFrameNanos = 0L
        while (true) {
            if (isPlaying || rotationSpeed.value > 0.01f) {
                val now = androidx.compose.runtime.withFrameNanos { it }
                if (lastFrameNanos != 0L) {
                    val delta = ((now - lastFrameNanos).coerceAtMost(100_000_000L)) / 1_000_000_000f
                    rotation.floatValue = (rotation.floatValue + rotationSpeed.value * delta) % 360f
                }
                lastFrameNanos = now
            } else {
                lastFrameNanos = 0L
                kotlinx.coroutines.delay(120L)
            }
        }
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // Fixed shadow under the physical case.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    shadowElevation = 18.dp.toPx()
                    shape = RoundedCornerShape(14.dp)
                    clip = false
                }
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(14.dp))
            ) {
                val w = size.width
                val h = size.height
                val edge = minOf(w, h)
                val caseCorner = edge * 0.055f

                // White / clear rear shell.
                drawRoundRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.11f),
                            Color.White.copy(alpha = 0.025f),
                            Color.Transparent,
                            Color.White.copy(alpha = 0.08f)
                        )
                    ),
                    topLeft = androidx.compose.ui.geometry.Offset.Zero,
                    size = size,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(caseCorner, caseCorner)
                )

                // Interior black tray, intentionally only slightly darker
                // than the album so the CD reads as a physical object.
                drawRoundRect(
                    color = Color(0xFF080808).copy(alpha = 0.74f),
                    topLeft = androidx.compose.ui.geometry.Offset(
                        edge * 0.055f,
                        edge * 0.055f
                    ),
                    size = androidx.compose.ui.geometry.Size(
                        w - edge * 0.11f,
                        h - edge * 0.11f
                    ),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(
                        caseCorner * 0.60f,
                        caseCorner * 0.60f
                    )
                )

                // Left hinge spine.
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.16f),
                    topLeft = androidx.compose.ui.geometry.Offset(edge * 0.012f, edge * 0.14f),
                    size = androidx.compose.ui.geometry.Size(edge * 0.055f, h * 0.72f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f)
                )
                drawLine(
                    color = Color.White.copy(alpha = 0.30f),
                    start = androidx.compose.ui.geometry.Offset(edge * 0.028f, edge * 0.16f),
                    end = androidx.compose.ui.geometry.Offset(edge * 0.028f, h * 0.84f),
                    strokeWidth = 1.5f
                )

                // Lock/latch suggestion on left middle.
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.21f),
                    topLeft = androidx.compose.ui.geometry.Offset(0f, h * 0.39f),
                    size = androidx.compose.ui.geometry.Size(edge * 0.095f, h * 0.19f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(5f, 5f)
                )
                drawLine(
                    color = Color.White.copy(alpha = 0.33f),
                    start = androidx.compose.ui.geometry.Offset(edge * 0.035f, h * 0.405f),
                    end = androidx.compose.ui.geometry.Offset(edge * 0.035f, h * 0.575f),
                    strokeWidth = 1.3f
                )

                // Four clean shell seams.
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.17f),
                    topLeft = androidx.compose.ui.geometry.Offset(edge * 0.022f, edge * 0.022f),
                    size = androidx.compose.ui.geometry.Size(
                        w - edge * 0.044f,
                        h - edge * 0.044f
                    ),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(caseCorner * 0.78f, caseCorner * 0.78f),
                    style = Stroke(width = 1.15f)
                )

                drawLine(
                    color = Color.White.copy(alpha = 0.12f),
                    start = androidx.compose.ui.geometry.Offset(w * 0.10f, edge * 0.032f),
                    end = androidx.compose.ui.geometry.Offset(w * 0.89f, edge * 0.032f),
                    strokeWidth = 2.0f
                )
                drawLine(
                    color = Color.White.copy(alpha = 0.07f),
                    start = androidx.compose.ui.geometry.Offset(w * 0.11f, h - edge * 0.032f),
                    end = androidx.compose.ui.geometry.Offset(w * 0.92f, h - edge * 0.032f),
                    strokeWidth = 1.2f
                )
            }
        }

        // Only the CD rotates. The transparent case never rotates.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    rotationZ = rotation.floatValue
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val diameter = minOf(size.width, size.height) * 0.84f
                val radius = diameter / 2f
                val cx = size.width / 2f
                val cy = size.height / 2f
                val center = androidx.compose.ui.geometry.Offset(cx, cy)

                // Physical shadow directly under the disc.
                drawCircle(
                    color = Color.Black.copy(alpha = 0.23f),
                    radius = radius * 1.008f,
                    center = androidx.compose.ui.geometry.Offset(cx, cy + radius * 0.025f)
                )

                // Silver optical disc.
                drawCircle(
                    brush = Brush.radialGradient(
                        colorStops = arrayOf(
                            0f to Color(0xFFE9EAEC),
                            0.28f to Color(0xFFBFC2C6),
                            0.57f to Color(0xFF8D9196),
                            0.78f to Color(0xFFD7D9DC),
                            1f to Color(0xFF777B80)
                        ),
                        center = androidx.compose.ui.geometry.Offset(
                            cx - radius * 0.20f,
                            cy - radius * 0.18f
                        ),
                        radius = radius * 1.12f
                    ),
                    radius = radius,
                    center = center
                )

                // Optical rainbow / CD interference bands.
                drawArc(
                    color = Color(0xFFB6C6E8).copy(alpha = 0.36f),
                    startAngle = -72f,
                    sweepAngle = 60f,
                    useCenter = false,
                    topLeft = androidx.compose.ui.geometry.Offset(cx - radius * 0.94f, cy - radius * 0.94f),
                    size = androidx.compose.ui.geometry.Size(radius * 1.88f, radius * 1.88f),
                    style = Stroke(width = radius * 0.045f)
                )
                drawArc(
                    color = Color(0xFFD8A2BC).copy(alpha = 0.29f),
                    startAngle = 12f,
                    sweepAngle = 46f,
                    useCenter = false,
                    topLeft = androidx.compose.ui.geometry.Offset(cx - radius * 0.86f, cy - radius * 0.86f),
                    size = androidx.compose.ui.geometry.Size(radius * 1.72f, radius * 1.72f),
                    style = Stroke(width = radius * 0.035f)
                )
                drawArc(
                    color = Color(0xFFA8D8C2).copy(alpha = 0.24f),
                    startAngle = 112f,
                    sweepAngle = 52f,
                    useCenter = false,
                    topLeft = androidx.compose.ui.geometry.Offset(cx - radius * 0.76f, cy - radius * 0.76f),
                    size = androidx.compose.ui.geometry.Size(radius * 1.52f, radius * 1.52f),
                    style = Stroke(width = radius * 0.026f)
                )

                // Fine CD grooves / laser rings.
                for (i in 0..54) {
                    val rr = radius * (0.30f + i * 0.0121f)
                    if (rr < radius * 0.97f) {
                        drawCircle(
                            color = if (i % 2 == 0) {
                                Color.White.copy(alpha = 0.12f)
                            } else {
                                Color.Black.copy(alpha = 0.09f)
                            },
                            radius = rr,
                            center = center,
                            style = Stroke(width = if (i % 9 == 0) 0.95f else 0.42f)
                        )
                    }
                }

                // Album-art label. Kept small enough that a CD remains obvious.
                val labelRadius = radius * 0.295f
                drawCircle(
                    color = Color.Black.copy(alpha = 0.20f),
                    radius = labelRadius * 1.035f,
                    center = center
                )
            }

            if (artwork != null) {
                Image(
                    bitmap = artwork.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .fillMaxSize(0.62f)
                )
            }

            // Center label ring / spindle.
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                val radius = minOf(size.width, size.height) * 0.032f
                val center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f)
                drawCircle(
                    color = Color(0xFF1E1F20),
                    radius = radius * 1.75f,
                    center = center
                )
                drawCircle(
                    color = Color(0xFFE3E5E8),
                    radius = radius,
                    center = center,
                    style = Stroke(width = 1.2f)
                )
                drawCircle(
                    color = Color(0xFF27282A),
                    radius = radius * 0.46f,
                    center = center
                )
            }
        }

        // Final glass layer stays stationary above the rotating CD.
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(14.dp))
        ) {
            val w = size.width
            val h = size.height

            // Broad diagonal reflection like acrylic.
            val reflection = Path().apply {
                moveTo(w * 0.04f, h * 0.17f)
                lineTo(w * 0.45f, h * 0.035f)
                lineTo(w * 0.52f, h * 0.035f)
                lineTo(w * 0.11f, h * 0.22f)
                close()
            }
            drawPath(
                path = reflection,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.14f),
                        Color.White.copy(alpha = 0.015f),
                        Color.Transparent
                    )
                )
            )

            // Fine diagonal catch-light.
            drawLine(
                color = Color.White.copy(alpha = 0.10f),
                start = androidx.compose.ui.geometry.Offset(w * 0.08f, h * 0.77f),
                end = androidx.compose.ui.geometry.Offset(w * 0.42f, h * 0.93f),
                strokeWidth = 1.6f
            )

            // Tiny vertical glints at shell edges.
            drawLine(
                color = Color.White.copy(alpha = 0.19f),
                start = androidx.compose.ui.geometry.Offset(w * 0.03f, h * 0.26f),
                end = androidx.compose.ui.geometry.Offset(w * 0.03f, h * 0.43f),
                strokeWidth = 1.6f
            )
            drawLine(
                color = Color.White.copy(alpha = 0.16f),
                start = androidx.compose.ui.geometry.Offset(w * 0.97f, h * 0.18f),
                end = androidx.compose.ui.geometry.Offset(w * 0.97f, h * 0.39f),
                strokeWidth = 1.3f
            )
        }
    }
}
