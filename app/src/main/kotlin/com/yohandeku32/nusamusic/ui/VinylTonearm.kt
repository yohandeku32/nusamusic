package com.yohandeku32.nusamusic.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate

@Composable
fun VinylTonearm(
    isPlaying: Boolean,
    hasSong: Boolean,
    positionMsState: State<Long>,
    durationMs: Long,
    modifier: Modifier = Modifier
) {
    // The tonearm is drawn entirely with Compose Canvas.
    // This keeps the proportions locked to one coordinate system, so the
    // artwork cannot be stretched/squashed by Image content scaling.
    val positionMs by positionMsState
    val trackProgress = if (durationMs > 0L) {
        (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    // The stylus is physically mounted down-right from the pivot in the
    // Canvas artwork. A small angle puts the stylus at the outer groove;
    // increasing the angle moves it inward toward the label without crossing
    // onto the album artwork.
    val targetAngle = if (hasSong) {
        // Start just a little farther outward than the previous position,
        // while keeping the same smooth inward tracking range.
        2.5f + trackProgress * 13f
    } else {
        0f
    }

    val armAngle by animateFloatAsState(
        targetValue = targetAngle,
        animationSpec = if (isPlaying && hasSong) {
            androidx.compose.animation.core.spring(
                dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy,
                stiffness = 180f
            )
        } else {
            androidx.compose.animation.core.spring(
                dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy,
                stiffness = 220f
            )
        },
        label = "tonearmAngle"
    )

    Canvas(modifier = modifier) {
        // Reference artwork is 297 × 732. Scale uniformly from that exact
        // coordinate system, then anchor the transparent design area to the
        // top-right just like the original artwork.
        val designScale = minOf(size.width, size.height) * 0.92f / 732f
        val designWidth = 297f * designScale

        // Keep the pivot clearly in the white area to the right of the vinyl.
        // The slight right shift is intentional; a small part of the artwork
        // may sit beyond the canvas edge, matching a real turntable layout.
        val pivotShift = size.width * 0.115f
        val left = size.width - designWidth + pivotShift
        val top = -size.height * 0.085f

        fun x(value: Float): Float = left + value * designScale
        fun y(value: Float): Float = top + value * designScale
        fun point(px: Float, py: Float) =
            androidx.compose.ui.geometry.Offset(x(px), y(py))

        val pivot = point(151f, 171f)

        rotate(
            degrees = armAngle,
            pivot = pivot
        ) {
            val softShadow = Color.Black.copy(alpha = 0.12f)

            // A soft radial shadow grounds the pivot against the turntable
            // surface before the metal bearing is drawn over it.
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.16f),
                        Color.Black.copy(alpha = 0.08f),
                        Color.Transparent
                    ),
                    center = androidx.compose.ui.geometry.Offset(
                        x(153.5f),
                        y(176f)
                    ),
                    radius = 58f * designScale
                ),
                radius = 58f * designScale,
                center = androidx.compose.ui.geometry.Offset(
                    x(153.5f),
                    y(176f)
                )
            )

            // Pivot contact shadow: several soft offset layers make the
            // bearing look seated above the white turntable surface.
            drawCircle(
                color = Color.Black.copy(alpha = 0.08f),
                radius = 53f * designScale,
                center = androidx.compose.ui.geometry.Offset(
                    x(151f) + 3.5f * designScale,
                    y(171f) + 4.5f * designScale
                )
            )
            drawCircle(
                color = Color.Black.copy(alpha = 0.11f),
                radius = 48f * designScale,
                center = androidx.compose.ui.geometry.Offset(
                    x(151f) + 2.2f * designScale,
                    y(171f) + 3.0f * designScale
                )
            )
            drawCircle(
                color = Color.Black.copy(alpha = 0.13f),
                radius = 43f * designScale,
                center = androidx.compose.ui.geometry.Offset(
                    x(151f) + 1.3f * designScale,
                    y(171f) + 2.0f * designScale
                )
            )

            // Soft contact shadow under the metal arm and cartridge.
            val shadowPath = androidx.compose.ui.graphics.Path().apply {
                moveTo(x(145f), y(249f))
                cubicTo(
                    x(145f), y(346f),
                    x(191f), y(475f),
                    x(129f), y(585f)
                )
            }
            drawPath(
                path = shadowPath,
                color = softShadow.copy(alpha = 0.08f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = 22f * designScale,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                )
            )
            drawPath(
                path = shadowPath,
                color = softShadow.copy(alpha = 0.14f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = 12f * designScale,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                )
            )

            // Upper spindle / counterweight stem.
            drawRoundRect(
                color = Color(0xFF161616),
                topLeft = point(139f, 18f),
                size = androidx.compose.ui.geometry.Size(
                    23f * designScale,
                    63f * designScale
                ),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(
                    5f * designScale,
                    5f * designScale
                )
            )
            drawRoundRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFE7EBED),
                        Color(0xFF8B9093),
                        Color(0xFFF8F9F9),
                        Color(0xFF6E7477)
                    ),
                    start = point(139f, 0f),
                    end = point(162f, 0f)
                ),
                topLeft = point(143f, 17f),
                size = androidx.compose.ui.geometry.Size(
                    15f * designScale,
                    58f * designScale
                ),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(
                    3f * designScale,
                    3f * designScale
                )
            )
            drawRoundRect(
                color = Color(0xFF111111),
                topLeft = point(147f, 20f),
                size = androidx.compose.ui.geometry.Size(
                    5f * designScale,
                    50f * designScale
                ),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(
                    2f * designScale,
                    2f * designScale
                )
            )

            // Brushed-metal counterweight housing.
            val housingPath = androidx.compose.ui.graphics.Path().apply {
                moveTo(x(111f), y(51f))
                cubicTo(x(119f), y(46f), x(167f), y(46f), x(174f), y(52f))
                lineTo(x(170f), y(103f))
                cubicTo(x(166f), y(113f), x(119f), y(113f), x(115f), y(103f))
                close()
            }
            drawPath(
                path = housingPath,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFF2F4F5),
                        Color(0xFFB7BEC2),
                        Color(0xFFF9FAFA),
                        Color(0xFF8F979A)
                    ),
                    start = point(110f, 55f),
                    end = point(176f, 55f)
                )
            )
            drawPath(
                path = housingPath,
                color = Color.Black.copy(alpha = 0.18f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = 1.7f * designScale
                )
            )
            drawRect(
                color = Color(0xFF34383A),
                topLeft = point(129f, 52f),
                size = androidx.compose.ui.geometry.Size(
                    8f * designScale,
                    53f * designScale
                )
            )
            drawRect(
                color = Color.White.copy(alpha = 0.55f),
                topLeft = point(151f, 52f),
                size = androidx.compose.ui.geometry.Size(
                    6f * designScale,
                    48f * designScale
                )
            )

            // Bearing collar.
            drawRoundRect(
                color = Color(0xFF9FA5A7),
                topLeft = point(128f, 100f),
                size = androidx.compose.ui.geometry.Size(
                    46f * designScale,
                    28f * designScale
                ),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(
                    6f * designScale,
                    6f * designScale
                )
            )
            drawRect(
                color = Color(0xFF4C5052),
                topLeft = point(133f, 100f),
                size = androidx.compose.ui.geometry.Size(
                    31f * designScale,
                    4f * designScale
                )
            )
            drawLine(
                color = Color.White.copy(alpha = 0.7f),
                start = point(134f, 107f),
                end = point(165f, 107f),
                strokeWidth = 1.4f * designScale
            )

            // Main black bearing wheel.
            drawCircle(
                color = Color(0xFF080808),
                radius = 48f * designScale,
                center = point(151f, 171f)
            )
            drawCircle(
                color = Color(0xFF1D1D1D),
                radius = 43f * designScale,
                center = point(151f, 171f)
            )
            drawCircle(
                color = Color(0xFFBFC4C6),
                radius = 31f * designScale,
                center = point(151f, 171f)
            )
            drawCircle(
                color = Color(0xFF111111),
                radius = 25f * designScale,
                center = point(151f, 171f)
            )
            drawCircle(
                color = Color(0xFFE4E6E7),
                radius = 18f * designScale,
                center = point(151f, 171f)
            )
            drawCircle(
                color = Color(0xFF2A2A2A),
                radius = 12f * designScale,
                center = point(151f, 171f)
            )
            drawCircle(
                color = Color(0xFFD7DADB),
                radius = 8f * designScale,
                center = point(151f, 171f)
            )

            // Small hardware details around the bearing.
            drawCircle(
                color = Color(0xFFCCB26B),
                radius = 7f * designScale,
                center = point(99f, 168f)
            )
            drawCircle(
                color = Color(0xFFE2CF91),
                radius = 4f * designScale,
                center = point(99f, 168f)
            )
            drawCircle(
                color = Color(0xFFCCB26B),
                radius = 7f * designScale,
                center = point(202f, 192f)
            )
            drawCircle(
                color = Color(0xFFE2CF91),
                radius = 4f * designScale,
                center = point(202f, 192f)
            )
            drawCircle(
                color = Color(0xFF7F8587),
                radius = 3.2f * designScale,
                center = point(123f, 113f)
            )
            drawCircle(
                color = Color(0xFF7F8587),
                radius = 3.2f * designScale,
                center = point(162f, 114f)
            )

            // Black mounting block below the bearing.
            drawRoundRect(
                color = Color(0xFF101010),
                topLeft = point(137f, 214f),
                size = androidx.compose.ui.geometry.Size(
                    29f * designScale,
                    52f * designScale
                ),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(
                    2f * designScale,
                    2f * designScale
                )
            )
            drawRect(
                color = Color(0xFF191919),
                topLeft = point(140f, 214f),
                size = androidx.compose.ui.geometry.Size(
                    23f * designScale,
                    50f * designScale
                )
            )

            // Long polished S-shaped tonearm.
            val armPath = androidx.compose.ui.graphics.Path().apply {
                moveTo(x(150f), y(255f))
                cubicTo(
                    x(148f), y(328f),
                    x(175f), y(445f),
                    x(176f), y(495f)
                )
                cubicTo(
                    x(177f), y(540f),
                    x(151f), y(565f),
                    x(130f), y(596f)
                )
            }

            // Dark outer edge.
            drawPath(
                path = armPath,
                color = Color(0xFF5A5E60),
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = 14f * designScale,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round,
                    join = androidx.compose.ui.graphics.StrokeJoin.Round
                )
            )

            // Main chrome tube.
            drawPath(
                path = armPath,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF63696B),
                        Color(0xFFF7F8F8),
                        Color(0xFFB8BEC0),
                        Color(0xFFFDFDFD),
                        Color(0xFF666B6D)
                    ),
                    start = point(128f, 255f),
                    end = point(191f, 540f)
                ),
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = 10.5f * designScale,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round,
                    join = androidx.compose.ui.graphics.StrokeJoin.Round
                )
            )
            drawPath(
                path = armPath,
                color = Color.White.copy(alpha = 0.72f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = 1.9f * designScale,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                )
            )

            // Cartridge neck.
            drawRoundRect(
                color = Color(0xFF2B2F30),
                topLeft = point(119f, 573f),
                size = androidx.compose.ui.geometry.Size(
                    31f * designScale,
                    26f * designScale
                ),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(
                    4f * designScale,
                    4f * designScale
                )
            )

            // White cartridge body, angled down-left.
            val cartridgePath = androidx.compose.ui.graphics.Path().apply {
                moveTo(x(128f), y(588f))
                lineTo(x(164f), y(603f))
                lineTo(x(111f), y(703f))
                lineTo(x(75f), y(686f))
                close()
            }
            drawPath(
                path = cartridgePath,
                color = Color(0xFFE9ECEC)
            )
            drawPath(
                path = cartridgePath,
                color = Color(0xFF2B2E2F),
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = 3f * designScale
                )
            )

            // Cartridge face / beveled edge.
            val cartridgeEdge = androidx.compose.ui.graphics.Path().apply {
                moveTo(x(75f), y(686f))
                lineTo(x(111f), y(703f))
                lineTo(x(106f), y(714f))
                lineTo(x(69f), y(697f))
                close()
            }
            drawPath(
                path = cartridgeEdge,
                color = Color(0xFF222526)
            )

            // Vent / screw dots on the cartridge face.
            val holeRows = listOf(
                listOf(100f, 621f, 93f, 635f, 86f, 649f, 79f, 663f),
                listOf(110f, 625f, 103f, 639f, 96f, 653f, 89f, 667f),
                listOf(120f, 629f, 113f, 643f, 106f, 657f, 99f, 671f)
            )
            holeRows.forEach { row ->
                var i = 0
                while (i < row.size) {
                    drawCircle(
                        color = Color(0xFF252829),
                        radius = 3.1f * designScale,
                        center = point(row[i], row[i + 1])
                    )
                    i += 2
                }
            }

            // Stylus tip.
            drawLine(
                color = Color(0xFF7B8082),
                start = point(130f, 704f),
                end = point(173f, 721f),
                strokeWidth = 5f * designScale,
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )
            drawLine(
                color = Color(0xFFD5D8D9),
                start = point(130f, 704f),
                end = point(173f, 721f),
                strokeWidth = 2.2f * designScale,
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )
            drawCircle(
                color = Color(0xFF505456),
                radius = 3.2f * designScale,
                center = point(173f, 721f)
            )
        }
    }
}
