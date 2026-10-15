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
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
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

        // Case-front decals remain fixed while the artwork and CD rotate underneath.
        // They belong to the clear lid, not to an album's artwork.
        Canvas(Modifier.matchParentSize()) {
            drawClearLid()
            drawSpindle(Offset(size.width * 0.55f, size.height * 0.5f))
            drawJewelCaseStickers()
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
 * Small archival paper decals adhered to the outside of the jewel-case lid.
 * Drawn as stationary vectors (no bitmap loading or per-track transitions),
 * with their own paper finish and ink so the rotating CD remains visible.
 */
private fun DrawScope.drawJewelCaseStickers() {
    val w = size.width
    val h = size.height
    val blue = Color(0xFF41698A)
    val paper = Color(0xFFE7EBE5)
    val ink = android.graphics.Color.rgb(50, 91, 130)
    val labelPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = ink
        typeface = android.graphics.Typeface.create("sans-serif-condensed", android.graphics.Typeface.BOLD)
    }

    // Text uses the same canvas as the decals so the printing stays aligned
    // when the smaller yellow price/date sticker is slightly rotated.
    fun printAt(text: String, x: Float, baseline: Float, heightFraction: Float) {
        labelPaint.textSize = h * heightFraction
        drawContext.canvas.nativeCanvas.drawText(text, x, baseline, labelPaint)
    }

    // Cream-and-blue archival sticker, similar to a vintage numbered CD seal.
    // Positioned left of the disc's metal hub rather than covering its centre.
    val bx = w * 0.235f
    val by = h * 0.105f
    val bw = w * 0.295f
    val bh = h * 0.385f
    val corner = CornerRadius(h * 0.027f)
    drawRoundRect(
        color = Color.Black.copy(alpha = 0.15f),
        topLeft = Offset(bx + 1.3.dp.toPx(), by + 1.7.dp.toPx()),
        size = Size(bw, bh),
        cornerRadius = corner
    )
    drawRoundRect(
        brush = Brush.verticalGradient(
            listOf(paper, Color(0xFFCDD9D7)),
            startY = by,
            endY = by + bh
        ),
        topLeft = Offset(bx, by),
        size = Size(bw, bh),
        cornerRadius = corner
    )
    drawRoundRect(
        color = blue.copy(alpha = 0.93f),
        topLeft = Offset(bx, by),
        size = Size(bw, bh),
        cornerRadius = corner,
        style = Stroke(1.1.dp.toPx())
    )

    val stamp = Offset(bx + bw * 0.78f, by + h * 0.067f)
    drawCircle(blue, h * 0.040f, stamp, style = Stroke(1.0.dp.toPx()))
    drawCircle(blue.copy(alpha = 0.48f), h * 0.034f, stamp, style = Stroke(0.55.dp.toPx()))
    printAt("13", stamp.x - h * 0.025f, stamp.y + h * 0.013f, 0.038f)
    printAt("B-SIDE", bx + bw * 0.072f, by + h * 0.059f, 0.032f)
    drawLine(blue.copy(alpha = 0.65f), Offset(bx + bw * 0.06f, by + h * 0.116f),
        Offset(bx + bw * 0.94f, by + h * 0.116f), 0.7.dp.toPx())
    printAt("NUSA", bx + bw * 0.072f, by + h * 0.173f, 0.057f)
    printAt("MUSIC ARCHIVE", bx + bw * 0.072f, by + h * 0.211f, 0.027f)
    printAt("S/N 7738700", bx + bw * 0.072f, by + h * 0.258f, 0.027f)
    drawLine(blue.copy(alpha = 0.54f), Offset(bx + bw * 0.06f, by + h * 0.280f),
        Offset(bx + bw * 0.94f, by + h * 0.280f), 0.6.dp.toPx())
    printAt("LIMITED", bx + bw * 0.072f, by + h * 0.327f, 0.043f)
    printAt("EDITION", bx + bw * 0.072f, by + h * 0.363f, 0.038f)

    // Slightly angled mustard inventory sticker beneath the big archival label.
    val yx = w * 0.645f
    val yy = h * 0.610f
    val yw = w * 0.285f
    val yh = h * 0.123f
    val ycorner = CornerRadius(h * 0.007f)
    val pivot = Offset(yx + yw / 2f, yy + yh / 2f)
    rotate(degrees = -9f, pivot = pivot) {
        drawRoundRect(
            Color.Black.copy(alpha = 0.13f),
            topLeft = Offset(yx + 1.dp.toPx(), yy + 1.4.dp.toPx()),
            size = Size(yw, yh),
            cornerRadius = ycorner
        )
        drawRoundRect(
            brush = Brush.linearGradient(
                listOf(Color(0xFFF2D968), Color(0xFFE2BF42)),
                start = Offset(yx, yy),
                end = Offset(yx + yw, yy + yh)
            ),
            topLeft = Offset(yx, yy),
            size = Size(yw, yh),
            cornerRadius = ycorner
        )
        drawLine(blue.copy(alpha = 0.65f), Offset(yx + yw * 0.07f, yy + yh * 0.53f),
            Offset(yx + yw * 0.90f, yy + yh * 0.53f), 0.5.dp.toPx())
        printAt("ARCHIVE", yx + yw * 0.07f, yy + yh * 0.40f, 0.032f)
        printAt("10-2026", yx + yw * 0.07f, yy + yh * 0.83f, 0.032f)
        val badge = Offset(yx + yw * 0.83f, yy + yh * 0.28f)
        drawCircle(blue, h * 0.019f, badge, style = Stroke(0.65.dp.toPx()))
        printAt("13", badge.x - h * 0.012f, badge.y + h * 0.006f, 0.020f)
    }
}
