package com.yohandeku32.nusamusic.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.style.ResolvedTextDirection
import androidx.compose.ui.unit.dp
import com.mocharealm.accompanist.lyrics.ui.composable.lyrics.SyllableLayout

/** Separate halo and crisp glyph passes; no clipping rectangles at the sung boundary. */
internal fun DrawScope.drawSyllableLyrics(
    layouts: List<List<SyllableLayout>>, now: Int, focus: Float, resetAt: Int
) {
    val strength = focus.coerceIn(0f, 1f)
    // All halos are behind all glyphs, including neighbouring syllables.
    for (pass in 0..1) layouts.forEach { row -> row.forEach syllable@ { layout ->
        val halo = pass == 0
        val unit = layout.syllable
        if (halo && (strength <= 0f || now <= unit.start || now >= unit.end)) return@syllable
        if (unit.content.isBlank()) return@syllable
        val result = layout.textLayoutResult
        val expression = syllableExpression(unit.start, unit.end, now, resetAt)
        val position = layout.position - Offset(0f, expression * 0.9.dp.toPx())
        val width = result.size.width.toFloat().coerceAtLeast(1f)
        val progress = syllableProgress(unit, now)
        val rtl = result.getParagraphDirection(0) == ResolvedTextDirection.Rtl
        val feather = minOf(5.dp.toPx(), width * 0.2f).coerceAtLeast(0.5f)
        val distance = (width + 2f * feather) * progress - feather
        val edge = if (rtl) width - distance else distance
        val direction = if (rtl) -1f else 1f
        val glow = syllableGlowEnvelope(unit.start, unit.end, now) *
            lyricBreathing(unit.start, unit.end, now) * strength
        withTransform({
            val scale = 1f + 0.012f * expression
            scale(scale, scale, position + Offset(width / 2f, result.size.height * 0.8f))
        }) {
            if (!halo) drawText(result, color = Color.White.copy(alpha = 0.4f), topLeft = position, shadow = Shadow.None)
            if (strength <= 0f || progress <= 0f) return@withTransform
            val fill = Brush.linearGradient(listOf(Color.White, Color.Transparent),
                Offset(edge - direction * feather, 0f), Offset(edge + direction * feather, 0f))
            if (halo) {
                if (glow <= 0f) return@withTransform
                val bloom = syllableBloom(unit.start, unit.end, now)
                // Visible broad atmosphere plus a tighter luminous core, followed by a sharp text pass.
                drawText(result, brush = fill, topLeft = position, alpha = 0.22f * glow,
                    shadow = Shadow(Color.White.copy(alpha = 0.55f * glow), Offset.Zero, (8f + 4f * bloom).dp.toPx()))
                drawText(result, brush = fill, topLeft = position, alpha = 0.18f * glow,
                    shadow = Shadow(Color.White.copy(alpha = 0.65f * glow), Offset.Zero, 3.dp.toPx()))
            } else {
                if (progress >= 1f) drawText(result, color = Color.White, topLeft = position, alpha = strength * 0.85f, shadow = Shadow.None)
                else drawText(result, brush = fill, topLeft = position, alpha = strength * 0.85f, shadow = Shadow.None)
                val shimmer = lyricShimmer(unit.start, unit.end, now)
                if (shimmer > 0f) {
                    val sungWidth = (width * progress - feather).coerceAtLeast(0f)
                    if (sungWidth < 1f) return@withTransform
                    val band = minOf(width * 0.3f + 4.dp.toPx(), sungWidth / 2f)
                    val swept = band + (sungWidth - 2f * band) * shimmer
                    val center = if (rtl) width - swept else swept
                    val sheen = Brush.linearGradient(listOf(Color.Transparent, Color.White, Color.Transparent),
                        Offset(center - band, 0f), Offset(center + band, 0f))
                    drawText(result, brush = sheen, topLeft = position, alpha = 0.45f * glow, shadow = Shadow.None)
                }
            }
        }
    } }
}

/** Authored attack/release; audio timestamps freeze all effects on pause. */
internal fun syllableGlowEnvelope(start: Int, end: Int, now: Int): Float {
    val duration = end.toLong() - start
    if (duration <= 0 || now <= start || now >= end) return 0f
    val ramp = minOf(120.0, duration / 2.0).coerceAtLeast(0.5)
    return lyricEase(((now.toLong() - start) / ramp).toFloat()) *
        lyricEase(((end.toLong() - now) / ramp).toFloat())
}
