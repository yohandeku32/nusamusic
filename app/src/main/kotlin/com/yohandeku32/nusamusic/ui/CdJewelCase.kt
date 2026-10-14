package com.yohandeku32.nusamusic.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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

// Fine scuffs molded into the outer clear case, not the spinning disc.
// Fractional coordinates let the marks scale to different screen widths.
private val caseHairlineScratches = arrayOf(
    floatArrayOf(0.17f, 0.14f, 0.27f, 0.12f),
    floatArrayOf(0.37f, 0.09f, 0.43f, 0.13f),
    floatArrayOf(0.78f, 0.12f, 0.87f, 0.10f),
    floatArrayOf(0.23f, 0.29f, 0.31f, 0.25f),
    floatArrayOf(0.73f, 0.29f, 0.84f, 0.34f),
    floatArrayOf(0.17f, 0.45f, 0.25f, 0.49f),
    floatArrayOf(0.82f, 0.47f, 0.91f, 0.44f),
    floatArrayOf(0.21f, 0.67f, 0.32f, 0.62f),
    floatArrayOf(0.68f, 0.74f, 0.77f, 0.76f),
    floatArrayOf(0.82f, 0.86f, 0.92f, 0.81f),
    floatArrayOf(0.36f, 0.89f, 0.43f, 0.86f),
    floatArrayOf(0.55f, 0.17f, 0.63f, 0.19f)
)

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
        disc(
            Modifier.offset(
                x = maxWidth * 0.55f - diameter / 2,
                y = (maxHeight - diameter) / 2
            ).size(diameter)
        )

        // The cover reflections, hinge and centre clip stay still during playback.
        Canvas(Modifier.matchParentSize()) {
            drawClearLid()
            drawSpindle(Offset(size.width * 0.55f, size.height * 0.5f))
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
    drawPath(reflection, Brush.linearGradient(listOf(Color.White.copy(alpha = 0.065f), Color.Transparent)))
    val lowerReflection = Path().apply {
        moveTo(w * 0.68f, h * 0.98f)
        lineTo(w * 0.95f, h * 0.58f)
        lineTo(w * 0.95f, h * 0.70f)
        lineTo(w * 0.78f, h * 0.98f)
        close()
    }
    drawPath(lowerReflection, glass.copy(alpha = 0.045f))

    // Lightly worn jewel case: short random-looking hairline marks on the
    // stationary transparent lid. Tiny dark offsets create etched depth while
    // keeping the rotating album artwork readable beneath the clear plastic.
    caseHairlineScratches.forEachIndexed { index, mark ->
        val start = Offset(w * mark[0], h * mark[1])
        val end = Offset(w * mark[2], h * mark[3])
        drawLine(
            color = glass.copy(alpha = if (index % 3 == 0) 0.30f else 0.18f),
            start = start,
            end = end,
            strokeWidth = edge * if (index % 4 == 0) 0.80f else 0.55f,
            cap = StrokeCap.Round
        )
        if (index % 4 == 0) {
            val shift = Offset(edge * 0.6f, edge * 0.8f)
            drawLine(
                Color.Black.copy(alpha = 0.11f),
                start + shift,
                end + shift,
                edge * 0.55f,
                StrokeCap.Round
            )
        }
    }
    drawArc(
        glass.copy(alpha = 0.14f),
        startAngle = 198f,
        sweepAngle = 19f,
        useCenter = false,
        topLeft = Offset(w * 0.28f, h * 0.18f),
        size = Size(w * 0.40f, h * 0.43f),
        style = Stroke(edge * 0.65f, cap = StrokeCap.Round)
    )
    drawArc(
        glass.copy(alpha = 0.13f),
        startAngle = 18f,
        sweepAngle = 14f,
        useCenter = false,
        topLeft = Offset(w * 0.55f, h * 0.51f),
        size = Size(w * 0.31f, h * 0.30f),
        style = Stroke(edge * 0.60f, cap = StrokeCap.Round)
    )

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
