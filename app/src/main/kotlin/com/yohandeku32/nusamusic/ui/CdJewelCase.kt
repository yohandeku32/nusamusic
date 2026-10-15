package com.yohandeku32.nusamusic.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Fixed clear shell and spindle; only the supplied disc rotates inside them. */
@Composable
internal fun CdJewelCase(
    modifier: Modifier = Modifier,
    disc: @Composable (Modifier) -> Unit
) {
    BoxWithConstraints(
        modifier.shadow(
            elevation = 12.dp,
            shape = RoundedCornerShape(3.dp),
            clip = false,
            ambientColor = Color.Black.copy(alpha = 0.4f),
            spotColor = Color.Black.copy(alpha = 0.55f)
        )
    ) {
        Canvas(Modifier.matchParentSize()) { drawClearTray() }

        val diameter = maxHeight * 0.92f
        val discX = maxWidth * 0.55f - diameter / 2
        val discY = (maxHeight - diameter) / 2
        // Only the disc content may slide during swaps. The parent clips discs
        // as they enter/leave the physical tray, while the static lid and
        // centre spindle remain outside this animation layer.
        Box(Modifier.matchParentSize().clipToBounds()) {
            disc(
                Modifier.offset(x = discX, y = discY).size(diameter)
            )
        }

        // The cover reflections, hinge and centre clip stay still during playback.
        Canvas(Modifier.matchParentSize()) {
            drawClearLid()
            drawSpindle(Offset(size.width * 0.55f, size.height * 0.5f))
            // The stationary shrink-wrap sits over the closed jewel case.
            drawPlasticWrap()
        }
    }
}

private fun DrawScope.drawClearTray() {
    val w = size.width
    val h = size.height
    val edge = 1.dp.toPx()
    val outline = Color(0xFFD7E6EB)
    val corner = CornerRadius(2.dp.toPx())

    // Low-opacity polycarbonate lets the dark player surface show through.
    drawRoundRect(
        brush = Brush.linearGradient(
            listOf(Color(0xFFBACBD3).copy(alpha = 0.14f), Color.White.copy(alpha = 0.025f), Color(0xFF8499A4).copy(alpha = 0.13f))
        ),
        cornerRadius = corner
    )
    drawRoundRect(outline.copy(alpha = 0.34f), cornerRadius = corner, style = Stroke(edge))
    drawRoundRect(
        outline.copy(alpha = 0.13f), Offset(edge * 3, edge * 3),
        Size(w - edge * 6, h - edge * 6), corner, style = Stroke(edge * 0.65f)
    )

    val centre = Offset(w * 0.55f, h * 0.5f)
    drawCircle(Color.Black.copy(alpha = 0.35f), h * 0.478f, centre, style = Stroke(h * 0.018f))
    drawCircle(outline.copy(alpha = 0.25f), h * 0.481f, centre, style = Stroke(edge * 0.8f))
    drawCircle(Color.White.copy(alpha = 0.10f), h * 0.493f, centre, style = Stroke(edge * 0.55f))

    // Narrow ribbed spine, with two moulded hinge bridges.
    drawRect(
        Brush.horizontalGradient(listOf(Color.White.copy(alpha = 0.11f), Color.Transparent, Color.White.copy(alpha = 0.08f))),
        Offset(w * 0.014f, h * 0.012f), Size(w * 0.075f, h * 0.976f)
    )
    for (rib in 0..13) {
        val x = w * (0.019f + rib * 0.0047f)
        drawLine(outline.copy(alpha = if (rib % 3 == 0) 0.27f else 0.12f), Offset(x, h * 0.024f), Offset(x, h * 0.976f), edge * 0.55f)
    }
    drawLine(Color.Black.copy(alpha = 0.6f), Offset(w * 0.094f, 0f), Offset(w * 0.094f, h), edge * 1.6f)
    drawLine(outline.copy(alpha = 0.32f), Offset(w * 0.099f, edge), Offset(w * 0.099f, h - edge), edge * 0.6f)
    listOf(0.22f, 0.78f).forEach { y ->
        drawRoundRect(outline.copy(alpha = 0.13f), Offset(w * 0.012f, h * y), Size(w * 0.082f, h * 0.023f), CornerRadius(edge))
        drawLine(outline.copy(alpha = 0.38f), Offset(w * 0.016f, h * y), Offset(w * 0.09f, h * y), edge * 0.7f)
    }

    // Semicircular retaining tabs in the four corners of the tray.
    listOf(0.24f, 0.85f).forEach { x ->
        listOf(false, true).forEach { bottom ->
            val tabSize = h * 0.10f
            val top = if (bottom) h - tabSize / 2 - edge * 3 else -tabSize / 2 + edge * 3
            drawArc(
                color = outline.copy(alpha = 0.26f),
                startAngle = if (bottom) 180f else 0f, sweepAngle = 180f,
                useCenter = false, topLeft = Offset(w * x - tabSize / 2, top),
                size = Size(tabSize, tabSize), style = Stroke(edge * 0.8f)
            )
        }
    }
}

private fun DrawScope.drawClearLid() {
    val w = size.width
    val h = size.height
    val edge = 1.dp.toPx()
    val glass = Color(0xFFDEECF1)

    // Broad faint reflections across the cover, independent of the rotating label.
    val reflection = Path().apply {
        moveTo(w * 0.11f, h * 0.02f)
        lineTo(w * 0.63f, h * 0.02f)
        lineTo(w * 0.15f, h * 0.94f)
        lineTo(w * 0.11f, h * 0.94f)
        close()
    }
    drawPath(reflection, Brush.linearGradient(listOf(
        Color.White.copy(alpha = 0.090f),
        Color.White.copy(alpha = 0.020f),
        Color.Transparent
    )))
    val lowerReflection = Path().apply {
        moveTo(w * 0.68f, h * 0.98f)
        lineTo(w * 0.95f, h * 0.58f)
        lineTo(w * 0.95f, h * 0.70f)
        lineTo(w * 0.78f, h * 0.98f)
        close()
    }
    drawPath(lowerReflection, glass.copy(alpha = 0.065f))

    // An immaculate, clean polycarbonate lid. Tiny specular highlights
    // show its depth without placing damage marks over the album cover.
    val specular = Path().apply {
        moveTo(w * 0.16f, h * 0.024f)
        lineTo(w * 0.185f, h * 0.024f)
        lineTo(w * 0.115f, h * 0.28f)
        lineTo(w * 0.115f, h * 0.16f)
        close()
    }
    drawPath(specular, Brush.linearGradient(listOf(
        Color.White.copy(alpha = 0.16f),
        Color.White.copy(alpha = 0.015f)
    )))

    // Doubled edges give the thin clear plastic its thickness.
    drawLine(glass.copy(alpha = 0.58f), Offset(w * 0.11f, edge), Offset(w * 0.96f, edge), edge * 0.9f)
    drawLine(glass.copy(alpha = 0.17f), Offset(w * 0.11f, edge * 3), Offset(w * 0.96f, edge * 3), edge * 0.6f)
    drawLine(glass.copy(alpha = 0.3f), Offset(w - edge * 2, h * 0.025f), Offset(w - edge * 2, h * 0.97f), edge)
    drawLine(glass.copy(alpha = 0.25f), Offset(w * 0.11f, h - edge * 2), Offset(w * 0.96f, h - edge * 2), edge * 0.8f)
    drawLine(Color.Black.copy(alpha = 0.5f), Offset(w * 0.11f, h - edge * 4), Offset(w * 0.96f, h - edge * 4), edge)

    // Small raised catches at the opening edge, without adding controls.
    listOf(0.18f, 0.82f).forEach { y ->
        drawRoundRect(
            Color.Black.copy(alpha = 0.35f), Offset(w * 0.975f, h * y),
            Size(w * 0.022f, h * 0.075f), CornerRadius(edge)
        )
        drawRoundRect(
            glass.copy(alpha = 0.32f), Offset(w * 0.975f, h * y),
            Size(w * 0.022f, h * 0.075f), CornerRadius(edge), style = Stroke(edge * 0.65f)
        )
    }
}

private fun DrawScope.drawSpindle(centre: Offset) {
    val h = size.height
    val radius = h * 0.075f
    val edge = 1.dp.toPx()
    val glass = Color(0xFFD5E4E9)
    drawCircle(Color(0xFF172025).copy(alpha = 0.84f), radius, centre)
    drawCircle(glass.copy(alpha = 0.5f), radius, centre, style = Stroke(edge * 0.8f))
    drawCircle(glass.copy(alpha = 0.16f), radius * 0.83f, centre, style = Stroke(edge * 1.3f))
    for (tooth in 0 until 12) {
        val angle = tooth * PI / 6
        val direction = Offset(cos(angle).toFloat(), sin(angle).toFloat())
        drawLine(Color.Black.copy(alpha = 0.65f), centre + direction * (radius * 0.40f), centre + direction * (radius * 0.69f), h * 0.012f, StrokeCap.Round)
        drawLine(glass.copy(alpha = 0.48f), centre + direction * (radius * 0.42f), centre + direction * (radius * 0.65f), h * 0.006f, StrokeCap.Round)
    }
    drawCircle(Color(0xFF121719), radius * 0.32f, centre)
    drawCircle(glass.copy(alpha = 0.34f), radius * 0.32f, centre, style = Stroke(edge * 0.65f))
    drawArc(glass.copy(alpha = 0.65f), 205f, 95f, false,
        centre - Offset(radius, radius), Size(radius * 2, radius * 2), style = Stroke(edge))
}

/**
 * Stationary shrink-wrap film over a sealed jewel case.
 * Specular tension folds collect along the edges, leaving album art visible.
 * The plastic never rotates with the disc and has no animated shaders.
 */
private fun DrawScope.drawPlasticWrap() {
    val w = size.width
    val h = size.height
    val hairline = 1.dp.toPx()

    // The film itself is transparent; only grazing reflections reveal it.
    drawRect(
        brush = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.027f),
                Color.Transparent,
                Color.White.copy(alpha = 0.015f)
            ),
            start = Offset(w * 0.10f, 0f),
            end = Offset(w * 0.94f, h)
        ),
        topLeft = Offset(w * 0.10f, 0f),
        size = Size(w * 0.89f, h)
    )

    // Slightly stretched film along the heat-sealed top and right edges.
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.20f),
                Color.White.copy(alpha = 0.075f),
                Color.Transparent
            ),
            startY = 0f,
            endY = h * 0.14f
        ),
        topLeft = Offset(w * 0.10f, 0f),
        size = Size(w * 0.88f, h * 0.14f)
    )
    drawRect(
        brush = Brush.horizontalGradient(
            colors = listOf(
                Color.Transparent,
                Color.White.copy(alpha = 0.07f),
                Color.White.copy(alpha = 0.16f)
            ),
            startX = w * 0.87f,
            endX = w
        ),
        topLeft = Offset(w * 0.87f, h * 0.035f),
        size = Size(w * 0.12f, h * 0.93f)
    )
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.08f)),
            startY = h * 0.91f,
            endY = h
        ),
        topLeft = Offset(w * 0.10f, h * 0.91f),
        size = Size(w * 0.88f, h * 0.085f)
    )

    // Semi-transparent broad creases, like cellophane pulled tight at corners.
    val topLeftFold = Path().apply {
        moveTo(w * 0.105f, h * 0.012f)
        lineTo(w * 0.245f, h * 0.010f)
        cubicTo(w * 0.19f, h * 0.03f, w * 0.18f, h * 0.075f, w * 0.27f, h * 0.14f)
        lineTo(w * 0.135f, h * 0.047f)
        close()
    }
    drawPath(
        topLeftFold,
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.17f),
                Color.White.copy(alpha = 0.055f),
                Color.Transparent
            ),
            start = Offset(w * 0.13f, h * 0.01f),
            end = Offset(w * 0.27f, h * 0.14f)
        )
    )
    val upperRightFold = Path().apply {
        moveTo(w * 0.73f, h * 0.012f)
        lineTo(w * 0.975f, h * 0.025f)
        lineTo(w * 0.944f, h * 0.32f)
        cubicTo(w * 0.915f, h * 0.19f, w * 0.91f, h * 0.09f, w * 0.73f, h * 0.012f)
        close()
    }
    drawPath(
        upperRightFold,
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.045f),
                Color.White.copy(alpha = 0.12f),
                Color.Transparent
            ),
            start = Offset(w * 0.82f, 0f),
            end = Offset(w * 0.97f, h * 0.33f)
        )
    )

    // Fine, uneven tension lines: highlights rather than scratches.
    fun crease(
        x0: Float, y0: Float,
        xc: Float, yc: Float,
        x1: Float, y1: Float,
        alpha: Float, width: Float
    ) {
        val path = Path().apply {
            moveTo(w * x0, h * y0)
            quadraticBezierTo(w * xc, h * yc, w * x1, h * y1)
        }
        drawPath(
            path,
            Color.White.copy(alpha = alpha),
            style = Stroke(width = width * hairline, cap = StrokeCap.Round)
        )
    }

    // Top-left cluster (over the case surface, away from the centre label).
    crease(0.115f, 0.014f, 0.145f, 0.024f, 0.17f, 0.083f, 0.28f, 1.8f)
    crease(0.135f, 0.009f, 0.185f, 0.047f, 0.225f, 0.107f, 0.19f, 1.25f)
    crease(0.195f, 0.012f, 0.23f, 0.045f, 0.29f, 0.078f, 0.13f, 0.85f)
    crease(0.245f, 0.006f, 0.30f, 0.035f, 0.35f, 0.062f, 0.11f, 0.85f)

    // Upper edge: slightly broken specular streaks from stretched film.
    crease(0.54f, 0.008f, 0.64f, 0.02f, 0.73f, 0.025f, 0.16f, 2.0f)
    crease(0.69f, 0.006f, 0.75f, 0.027f, 0.83f, 0.055f, 0.22f, 1.5f)
    crease(0.88f, 0.024f, 0.93f, 0.055f, 0.955f, 0.135f, 0.21f, 1.35f)

    // Right edge: elongated reflections typical of tightly wrapped cases.
    crease(0.975f, 0.13f, 0.95f, 0.23f, 0.94f, 0.40f, 0.30f, 2.1f)
    crease(0.987f, 0.20f, 0.965f, 0.31f, 0.97f, 0.43f, 0.17f, 1.0f)
    crease(0.98f, 0.72f, 0.95f, 0.79f, 0.91f, 0.85f, 0.11f, 0.9f)

    // Bottom corners: a few pulled folds, leaving the artwork unobscured.
    crease(0.15f, 0.965f, 0.22f, 0.94f, 0.28f, 0.89f, 0.14f, 1.15f)
    crease(0.50f, 0.987f, 0.55f, 0.963f, 0.605f, 0.93f, 0.18f, 1.2f)
    crease(0.64f, 0.99f, 0.70f, 0.959f, 0.75f, 0.92f, 0.21f, 1.4f)
    crease(0.92f, 0.983f, 0.86f, 0.954f, 0.81f, 0.917f, 0.15f, 1.1f)
}
