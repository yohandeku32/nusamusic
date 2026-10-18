package com.yohandeku32.nusamusic.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.style.ResolvedTextDirection
import androidx.compose.ui.unit.dp
import com.mocharealm.accompanist.lyrics.ui.composable.lyrics.SyllableLayout

/** Feather the glyph fill itself: a hard clip also cuts the glow into visible rectangles. */
internal fun DrawScope.drawSyllableLyrics(
    layouts: List<List<SyllableLayout>>, now: Int, focus: Float, resetAt: Int
) {
    val strength = focus.coerceIn(0f, 1f)
    layouts.forEach { row -> row.forEach syllable@ { layout ->
        if (layout.syllable.content.isBlank()) return@syllable
        val result = layout.textLayoutResult
        val onset = lyricOnset(layout.syllable.start, layout.syllable.end, now, resetAt)
        val position = layout.position - Offset(0f, onset * 1.2.dp.toPx())
        // Keep the same base luminance when focus changes; fading a second parent alpha
        // used to darken unsung text abruptly on entry and flash it bright on exit.
        drawText(result, color = Color.White.copy(alpha = 0.4f), topLeft = position, shadow = Shadow.None)
        if (strength <= 0f) return@syllable
        val progress = syllableProgress(layout.syllable, now)
        if (progress <= 0f) return@syllable
        val glow = syllableGlowEnvelope(layout.syllable.start, layout.syllable.end, now) *
            lyricBreathing(layout.syllable.start, layout.syllable.end, now)
        val shadow = if (glow > 0f) Shadow(
            Color.White.copy(alpha = 0.14f * glow), Offset.Zero, 4.dp.toPx()
        ) else Shadow.None
        if (progress >= 1f) {
            drawText(result, color = Color.White, topLeft = position, alpha = strength, shadow = shadow)
        } else {
            val width = result.size.width.toFloat().coerceAtLeast(1f)
            val feather = minOf(5.dp.toPx(), width * 0.2f).coerceAtLeast(0.5f)
            val distance = (width + 2f * feather) * progress - feather
            val rtl = result.getParagraphDirection(0) == ResolvedTextDirection.Rtl
            val edge = if (rtl) width - distance else distance
            val direction = if (rtl) -1f else 1f
            val fill = Brush.linearGradient(
                colors = listOf(Color.White, Color.Transparent),
                start = Offset(edge - direction * feather, 0f),
                end = Offset(edge + direction * feather, 0f)
            )
            // No clipRect: the soft halo can extend naturally beyond the glyph bounds.
            drawText(result, brush = fill, topLeft = position, alpha = strength, shadow = shadow)
        }
        val shimmer = lyricShimmer(layout.syllable.start, layout.syllable.end, now)
        if (shimmer > 0f) {
            val width = result.size.width.toFloat().coerceAtLeast(1f)
            val rtl = result.getParagraphDirection(0) == ResolvedTextDirection.Rtl
            val center = width * (if (rtl) 1f - shimmer else shimmer)
            val band = width * 0.35f + 4.dp.toPx()
            val sheen = Brush.linearGradient(
                colors = listOf(Color.Transparent, Color.White, Color.Transparent),
                start = Offset(center - band, 0f), end = Offset(center + band, 0f))
            drawText(result, brush = sheen, topLeft = position, alpha = 0.07f * glow * strength, shadow = Shadow.None)
        }
    } }
}

/** Audio-time envelope stays frozen on pause and reaches zero at both authored bounds. */
internal fun syllableGlowEnvelope(start: Int, end: Int, now: Int): Float {
    val duration = end.toLong() - start
    if (duration <= 0 || now <= start || now >= end) return 0f
    val ramp = minOf(120.0, duration / 2.0).coerceAtLeast(0.5)
    fun smooth(value: Double): Float {
        val x = value.coerceIn(0.0, 1.0).toFloat()
        return x * x * (3f - 2f * x)
    }
    return smooth((now.toLong() - start) / ramp) * smooth((end.toLong() - now) / ramp)
}
