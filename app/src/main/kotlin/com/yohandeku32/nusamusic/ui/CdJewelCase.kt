package com.yohandeku32.nusamusic.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import java.util.Locale

/** Fixed clear shell and spindle; only the supplied disc rotates inside them. */
@Composable
internal fun CdJewelCase(
    modifier: Modifier = Modifier,
    artistName: String? = null,
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
        // Render the static plastic lid, reflections and detailed paper grain
        // into an independent layer. Rotating frames can then reuse that layer
        // instead of submitting the sticker's hundreds of tiny draw operations.
        // Changing songs still invalidates the artist text as normal.
        CdClearLid(Modifier.matchParentSize(), artistName, showSpindle = true)
    }
}

/** The player and carousel share the same clear plastic and archival decals. */
@Composable
internal fun CdClearLid(
    modifier: Modifier = Modifier,
    artistName: String? = null,
    showSpindle: Boolean = false,
    showStickers: Boolean = true
) {
    // This optional personal-use font stays local; public builds use the fallback.
    val context = LocalContext.current.applicationContext
    val stickerFont = remember(context, showStickers) {
        if (!showStickers) null else try {
            android.graphics.Typeface.createFromAsset(context.assets, "fonts/Aerosoldier_PERSONAL_USE_ONLY.otf")
        } catch (_: RuntimeException) {
            null
        }
    }
    Canvas(modifier.graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }) {
        drawClearLid()
        drawClearCaseReflections()
        if (showSpindle) drawSpindle(Offset(size.width * 0.55f, size.height * 0.5f))
        if (showStickers) drawJewelCaseStickers(artistName, stickerFont)
    }
}

internal fun DrawScope.drawClearTray() {
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

internal fun DrawScope.drawClearLid() {
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


/**
 * Subtle, stationary reflection in the transparent polycarbonate case.
 * Broad low-alpha reflections and short specular edge highlights imply depth,
 * rather than scratches, creases or plastic shrink-wrap.
 */
internal fun DrawScope.drawClearCaseReflections() {
    val w = size.width
    val h = size.height
    val glass = Color(0xFFEAF7FF)
    val edge = 1.dp.toPx()

    // Diffuse diagonal highlight reflecting from the smooth outer lid.
    val reflectedWindow = Path().apply {
        moveTo(w * 0.36f, h * 0.012f)
        lineTo(w * 0.61f, h * 0.012f)
        cubicTo(
            w * 0.55f, h * 0.16f,
            w * 0.40f, h * 0.44f,
            w * 0.31f, h * 0.64f
        )
        lineTo(w * 0.26f, h * 0.68f)
        close()
    }
    drawPath(
        reflectedWindow,
        brush = Brush.linearGradient(
            colors = listOf(
                glass.copy(alpha = 0.14f),
                glass.copy(alpha = 0.045f),
                Color.Transparent
            ),
            start = Offset(w * 0.44f, h * 0.04f),
            end = Offset(w * 0.29f, h * 0.65f)
        )
    )

    // Curved reflection on the other side creates a glassy sense of depth.
    val rightGlare = Path().apply {
        moveTo(w * 0.93f, h * 0.09f)
        quadraticBezierTo(w * 0.967f, h * 0.31f, w * 0.947f, h * 0.54f)
        quadraticBezierTo(w * 0.927f, h * 0.65f, w * 0.919f, h * 0.77f)
        lineTo(w * 0.968f, h * 0.77f)
        quadraticBezierTo(w * 0.985f, h * 0.48f, w * 0.978f, h * 0.24f)
        close()
    }
    drawPath(
        rightGlare,
        brush = Brush.horizontalGradient(
            colors = listOf(Color.Transparent, glass.copy(alpha = 0.105f)),
            startX = w * 0.919f,
            endX = w * 0.985f
        )
    )

    // Thin polished glints at the folded lid edges, not plastic-wrap lines.
    drawLine(
        glass.copy(alpha = 0.27f),
        Offset(w * 0.39f, h * 0.008f),
        Offset(w * 0.66f, h * 0.008f),
        strokeWidth = edge * 0.65f,
        cap = StrokeCap.Round
    )
    drawLine(
        glass.copy(alpha = 0.21f),
        Offset(w * 0.966f, h * 0.12f),
        Offset(w * 0.966f, h * 0.38f),
        strokeWidth = edge * 0.62f,
        cap = StrokeCap.Round
    )
    drawLine(
        glass.copy(alpha = 0.17f),
        Offset(w * 0.69f, h * 0.987f),
        Offset(w * 0.90f, h * 0.987f),
        strokeWidth = edge * 0.52f,
        cap = StrokeCap.Round
    )
}

internal fun DrawScope.drawSpindle(centre: Offset) {
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
private fun DrawScope.drawJewelCaseStickers(
    artistName: String?,
    stickerTypeface: android.graphics.Typeface?
) {
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
    // Kept small in the upper-left corner of the clear lid.
    val bx = w * 0.155f
    val by = h * 0.080f
    val bw = w * 0.295f
    val bh = h * 0.385f
    // Scale paper, print, borders and stamp together without shifting the lid.
    scale(scaleX = 0.82f, scaleY = 0.82f, pivot = Offset(bx, by)) {
        // Slightly frayed paper silhouette, with a tiny tear on the right edge.
        val paperEdge = Path().apply {
            moveTo(bx + bw * 0.045f, by)
            lineTo(bx + bw * 0.943f, by)
            quadraticBezierTo(bx + bw, by, bx + bw, by + bh * 0.047f)
            lineTo(bx + bw, by + bh * 0.804f)
            lineTo(bx + bw * 0.968f, by + bh * 0.838f)
            lineTo(bx + bw * 0.988f, by + bh * 0.855f)
            lineTo(bx + bw * 0.952f, by + bh * 0.881f)
            lineTo(bx + bw * 0.979f, by + bh * 0.907f)
            lineTo(bx + bw * 0.949f, by + bh * 0.932f)
            lineTo(bx + bw * 0.970f, by + bh * 0.963f)
            quadraticBezierTo(bx + bw * 0.963f, by + bh, bx + bw * 0.923f, by + bh)
            lineTo(bx + bw * 0.056f, by + bh)
            quadraticBezierTo(bx, by + bh, bx, by + bh * 0.952f)
            lineTo(bx, by + bh * 0.050f)
            quadraticBezierTo(bx, by, bx + bw * 0.045f, by)
            close()
        }
        translate(left = 1.2.dp.toPx(), top = 1.3.dp.toPx()) {
            drawPath(paperEdge, Color.Black.copy(alpha = 0.13f))
        }
        drawPath(
            paperEdge,
            brush = Brush.verticalGradient(
                listOf(Color(0xFFEEEDE6), paper, Color(0xFFD3DAD4)),
                startY = by,
                endY = by + bh
            )
        )
        clipPath(paperEdge) {
            drawStickerPaperTexture(
                left = bx, top = by, width = bw, height = bh,
                darkFiber = Color(0xFF8E9690),
                lightFiber = Color(0xFFFFFAEE),
                stain = Color(0xFF947E63),
                seed = 117, grainCount = 240
            )
        }
        drawPath(paperEdge, blue.copy(alpha = 0.58f), style = Stroke(0.65.dp.toPx()))
        // A bright exposed fiber at the small notch, not a plastic-wrap crease.
        drawLine(
            Color.White.copy(alpha = 0.56f),
            Offset(bx + bw * 0.968f, by + bh * 0.838f),
            Offset(bx + bw * 0.952f, by + bh * 0.881f),
            0.55.dp.toPx()
        )
    
        val stamp = Offset(bx + bw * 0.78f, by + h * 0.067f)
        drawCircle(blue, h * 0.040f, stamp, style = Stroke(1.0.dp.toPx()))
        drawCircle(blue.copy(alpha = 0.48f), h * 0.034f, stamp, style = Stroke(0.55.dp.toPx()))
        printAt("13", stamp.x - h * 0.025f, stamp.y + h * 0.013f, 0.038f)
        printAt("B-SIDE", bx + bw * 0.072f, by + h * 0.059f, 0.032f)
        drawLine(blue.copy(alpha = 0.65f), Offset(bx + bw * 0.06f, by + h * 0.116f),
            Offset(bx + bw * 0.94f, by + h * 0.116f), 0.7.dp.toPx())
        // Unlike the archival typesetting, the artist name is handwritten in marker.
        drawArtistMarker(artistName, stickerTypeface, bx + bw * 0.072f, by + h * 0.175f, bw * 0.86f)
        printAt("MUSIC ARCHIVE", bx + bw * 0.072f, by + h * 0.211f, 0.027f)
        printAt("S/N 7738700", bx + bw * 0.072f, by + h * 0.258f, 0.027f)
        drawLine(blue.copy(alpha = 0.54f), Offset(bx + bw * 0.06f, by + h * 0.280f),
            Offset(bx + bw * 0.94f, by + h * 0.280f), 0.6.dp.toPx())
        printAt("LIMITED", bx + bw * 0.072f, by + h * 0.327f, 0.043f)
        printAt("EDITION", bx + bw * 0.072f, by + h * 0.363f, 0.038f)
        // A few faded ink pinholes sit over the print, like aged paper stock.
        clipPath(paperEdge) {
            drawStickerInkWear(bx, by, bw, bh, seed = 509, fleckCount = 24)
        }
    }

    // Smaller angled mustard inventory sticker at the lower-right corner.
    val yx = w * 0.715f
    val yy = h * 0.790f
    val yw = w * 0.285f
    val yh = h * 0.123f
    scale(scaleX = 0.80f, scaleY = 0.80f, pivot = Offset(yx, yy)) {
        val pivot = Offset(yx + yw / 2f, yy + yh / 2f)
        rotate(degrees = -9f, pivot = pivot) {
            // Unlike the blue label's right-edge nicks, this adhesive date
            // sticker has a small irregular piece missing at its upper-left.
            // The rest of its edges stay mostly smooth, like peeled paper.
            val yellowEdge = Path().apply {
                moveTo(yx + yw * 0.205f, yy)
                lineTo(yx + yw * 0.93f, yy)
                quadraticBezierTo(yx + yw, yy, yx + yw, yy + yh * 0.10f)
                lineTo(yx + yw, yy + yh * 0.925f)
                quadraticBezierTo(yx + yw, yy + yh, yx + yw * 0.928f, yy + yh)
                lineTo(yx + yw * 0.055f, yy + yh)
                quadraticBezierTo(yx, yy + yh, yx, yy + yh * 0.93f)
                lineTo(yx, yy + yh * 0.39f)
                lineTo(yx + yw * 0.046f, yy + yh * 0.344f)
                lineTo(yx + yw * 0.085f, yy + yh * 0.285f)
                lineTo(yx + yw * 0.063f, yy + yh * 0.236f)
                lineTo(yx + yw * 0.145f, yy + yh * 0.161f)
                lineTo(yx + yw * 0.098f, yy + yh * 0.082f)
                close()
            }
            translate(left = 1.dp.toPx(), top = 1.3.dp.toPx()) {
                drawPath(yellowEdge, Color.Black.copy(alpha = 0.12f))
            }
            drawPath(
                yellowEdge,
                brush = Brush.linearGradient(
                    listOf(Color(0xFFF3DC7A), Color(0xFFE1BE45)),
                    start = Offset(yx, yy),
                    end = Offset(yx + yw, yy + yh)
                )
            )
            clipPath(yellowEdge) {
                drawStickerPaperTexture(
                    left = yx, top = yy, width = yw, height = yh,
                    darkFiber = Color(0xFF92733D),
                    lightFiber = Color(0xFFFFF4CA),
                    stain = Color(0xFFAA7633),
                    seed = 931, grainCount = 125
                )
            }
            drawPath(
                yellowEdge,
                color = Color(0xFFB8953B).copy(alpha = 0.34f),
                style = Stroke(0.55.dp.toPx())
            )
            // A small turned-up paper tip exposes paler adhesive at the torn
            // corner; short fibers suggest a different type of wear.
            val peeledTip = Path().apply {
                moveTo(yx + yw * 0.205f, yy)
                lineTo(yx + yw * 0.145f, yy + yh * 0.161f)
                lineTo(yx + yw * 0.270f, yy + yh * 0.125f)
                close()
            }
            drawPath(
                peeledTip,
                brush = Brush.linearGradient(
                    listOf(Color(0xFFFFF1BE), Color(0xFFD7B65F).copy(alpha = 0.86f)),
                    start = Offset(yx + yw * 0.18f, yy),
                    end = Offset(yx + yw * 0.24f, yy + yh * 0.18f)
                )
            )
            drawLine(
                Color(0xFF9A7C39).copy(alpha = 0.30f),
                Offset(yx + yw * 0.145f, yy + yh * 0.161f),
                Offset(yx + yw * 0.270f, yy + yh * 0.125f),
                0.55.dp.toPx()
            )
            for (fiber in 0..3) {
                val t = fiber / 4f
                drawLine(
                    Color(0xFFFFE9B1).copy(alpha = 0.56f),
                    Offset(yx + yw * (0.018f + t * 0.06f), yy + yh * (0.370f - t * 0.11f)),
                    Offset(yx + yw * (0.034f + t * 0.06f), yy + yh * (0.400f - t * 0.11f)),
                    0.45.dp.toPx()
                )
            }
            drawLine(blue.copy(alpha = 0.65f), Offset(yx + yw * 0.07f, yy + yh * 0.53f),
                Offset(yx + yw * 0.90f, yy + yh * 0.53f), 0.5.dp.toPx())
            printAt("ARCHIVE", yx + yw * 0.07f, yy + yh * 0.40f, 0.032f)
            printAt("10-2026", yx + yw * 0.07f, yy + yh * 0.83f, 0.032f)
            val badge = Offset(yx + yw * 0.83f, yy + yh * 0.28f)
            drawCircle(blue, h * 0.019f, badge, style = Stroke(0.65.dp.toPx()))
            printAt("13", badge.x - h * 0.012f, badge.y + h * 0.006f, 0.020f)
            clipPath(yellowEdge) {
                drawStickerInkWear(yx, yy, yw, yh, seed = 1601, fleckCount = 14)
            }
        }
    }
}

/**
 * Stable seeded randomness: paper wear should never move or flicker.
 * No texture bitmap, network requests, animation or extra assets.
 */
private class PaperNoise(seed: Int) {
    private var value = seed

    fun next(): Float {
        value = value * 1664525 + 1013904223
        return ((value ushr 8) and 0x00FFFFFF) / 16777216f
    }
}

/**
 * Several different scales of paper stock: cloudy dye, soft pulp grain, and
 * thin uneven fibers. Avoids the visible uniform hatch/fleck pattern.
 * Call only inside a clipPath matching the sticker's torn-paper outline.
 */
private fun DrawScope.drawStickerPaperTexture(
    left: Float,
    top: Float,
    width: Float,
    height: Float,
    darkFiber: Color,
    lightFiber: Color,
    stain: Color,
    seed: Int,
    grainCount: Int
) {
    val noise = PaperNoise(seed)

    // Slight yellowing / absorptive dye variation, with very soft edges.
    repeat(5) {
        val center = Offset(
            left + width * (0.05f + noise.next() * 0.90f),
            top + height * (0.08f + noise.next() * 0.84f)
        )
        val radius = width * (0.13f + noise.next() * 0.19f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    stain.copy(alpha = 0.024f + noise.next() * 0.038f),
                    Color.Transparent
                ),
                center = center,
                radius = radius
            ),
            radius = radius,
            center = center
        )
    }

    // Grain is tiny and irregular: different radii, tones and concentrations.
    repeat(grainCount) {
        val point = Offset(left + width * noise.next(), top + height * noise.next())
        val fiberTone = if (noise.next() < 0.64f) darkFiber else lightFiber
        val opacity = 0.034f + noise.next() * 0.10f
        drawCircle(
            color = fiberTone.copy(alpha = opacity),
            radius = (0.15f + noise.next() * 0.33f).dp.toPx(),
            center = point
        )
    }

    // Curving short threads suggest cellulose, not repeated straight scratches.
    repeat(grainCount / 6) {
        val x = left + width * noise.next()
        val y = top + height * noise.next()
        val angle = noise.next() * (2f * PI.toFloat())
        val length = width * (0.009f + noise.next() * 0.039f)
        val dx = cos(angle) * length
        val dy = sin(angle) * length
        val bend = (noise.next() - 0.5f) * height * 0.012f
        val fiber = Path().apply {
            moveTo(x, y)
            quadraticBezierTo(
                x + dx * 0.48f - dy * 0.15f,
                y + dy * 0.48f + bend,
                x + dx,
                y + dy
            )
        }
        drawPath(
            path = fiber,
            color = (if (noise.next() < 0.6f) darkFiber else lightFiber)
                .copy(alpha = 0.055f + noise.next() * 0.085f),
            style = Stroke(
                width = (0.24f + noise.next() * 0.24f).dp.toPx(),
                cap = StrokeCap.Round
            )
        )
    }
}

/** Infrequent pale voids in printed ink, as on older matte adhesive paper. */
private fun DrawScope.drawStickerInkWear(
    left: Float,
    top: Float,
    width: Float,
    height: Float,
    seed: Int,
    fleckCount: Int
) {
    val noise = PaperNoise(seed)
    repeat(fleckCount) {
        drawCircle(
            color = Color(0xFFFFF8E8).copy(alpha = 0.065f + noise.next() * 0.11f),
            radius = (0.18f + noise.next() * 0.29f).dp.toPx(),
            center = Offset(
                left + width * noise.next(),
                top + height * noise.next()
            )
        )
    }
}

/**
 * Marker-style artist name on the cream paper sticker.
 * Uses Android's built-in casual handwriting face, gently slanted and
 * inked twice for a subtle felt-tip appearance without bundling font assets.
 */
private fun DrawScope.drawArtistMarker(
    artistName: String?,
    stickerTypeface: android.graphics.Typeface?,
    x: Float,
    baseline: Float,
    availableWidth: Float
) {
    val h = size.height
    val artist = artistName
        ?.trim()
        ?.replace(Regex("\\s+"), " ")
        ?.uppercase(Locale.ROOT)
        ?.takeIf { it.isNotEmpty() }
        ?: "ARTIST"
    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        typeface = stickerTypeface
            ?: android.graphics.Typeface.create("casual", android.graphics.Typeface.BOLD)
        color = android.graphics.Color.BLACK
        // Use the exact Aerosoldier glyph outlines without synthetic slant.
        textSkewX = if (stickerTypeface == null) -0.075f else 0f
        isSubpixelText = true
        style = android.graphics.Paint.Style.FILL_AND_STROKE
        strokeJoin = android.graphics.Paint.Join.ROUND
        strokeCap = android.graphics.Paint.Cap.ROUND
        // Aerosoldier is already expressive; avoid artificial thick outlines.
        strokeWidth = h * (if (stickerTypeface == null) 0.0017f else 0.0006f)
        textSize = h * 0.050f
    }

    // Uppercase labels; abbreviate longer artist names to initials if needed.
    // If the artist's full name does not fit at natural marker size,
    // write the first letter of each word instead (e.g. Bunga Citra Lestari -> BCL).
    // Single long names are still reduced to fit, without inventing initials.
    val words = artist.split(" ").filter { it.isNotEmpty() }
    val displayName = if (words.size > 1 && paint.measureText(artist) > availableWidth) {
        words.joinToString(separator = "") { it.first().uppercaseChar().toString() }
    } else {
        artist
    }

    // Retain the natural handwritten size where possible. Extra-long initials
    // or single-word names shrink gently, with ellipsis only as a last resort.
    val measured = paint.measureText(displayName).coerceAtLeast(1f)
    paint.textSize = (paint.textSize * availableWidth / measured)
        .coerceIn(h * 0.025f, h * 0.050f)
    var display = displayName
    if (paint.measureText(display) > availableWidth) {
        while (display.length > 1 && paint.measureText("$display…") > availableWidth) {
            display = display.dropLast(1)
        }
        display += "…"
    }

    val canvas = drawContext.canvas.nativeCanvas
    val saved = canvas.save()
    try {
        canvas.rotate(-2.0f, x, baseline)
        // Faint ink bleed along the paper fibers; the main stroke stays sharp.
        paint.alpha = 44
        canvas.drawText(display, x + h * 0.0011f, baseline + h * 0.0016f, paint)
        paint.alpha = 232
        canvas.drawText(display, x, baseline, paint)
    } finally {
        canvas.restoreToCount(saved)
    }
}
