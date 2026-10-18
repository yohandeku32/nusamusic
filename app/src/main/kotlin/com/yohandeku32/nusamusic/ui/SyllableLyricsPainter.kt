package com.yohandeku32.nusamusic.ui

import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Shader
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.style.ResolvedTextDirection
import com.mocharealm.accompanist.lyrics.ui.composable.lyrics.SyllableLayout
import kotlin.math.roundToInt

/** Cache gradient objects and matrices with the measured glyph, not with its active state. */
private class MovingLyricsGradient(sheen: Boolean) {
    private val shader = LinearGradient(0f, 0f, 1f, 0f,
        if (sheen) intArrayOf(0x00FFFFFF, -1, 0x00FFFFFF) else intArrayOf(-1, 0x00FFFFFF),
        null, Shader.TileMode.CLAMP)
    val brush = ShaderBrush(shader)
    private val matrix = Matrix()
    fun move(start: Float, end: Float) {
        matrix.setScale(end - start, 1f)
        matrix.postTranslate(start, 0f)
        shader.setLocalMatrix(matrix)
    }
}

/** Quantised soft shadows avoid constructing shadow objects during a frame. */
private class LyricsGlowPalette(density: Float) {
    private val broad = Array(65) { energy -> Array(9) { bloom ->
        Shadow(Color.White.copy(alpha = 0.55f * energy / 64f), Offset.Zero, (8f + bloom * 0.5f) * density)
    } }
    private val tight = Array(65) { energy ->
        Shadow(Color.White.copy(alpha = 0.65f * energy / 64f), Offset.Zero, 3f * density)
    }
    fun broad(energy: Float, bloom: Float) = broad[(energy * 64f).roundToInt().coerceIn(0, 64)][(bloom * 8f).roundToInt().coerceIn(0, 8)]
    fun tight(energy: Float) = tight[(energy * 64f).roundToInt().coerceIn(0, 64)]
}

internal class SyllableLyricsPainter(
    layouts: List<List<SyllableLayout>>,
    private val density: Float,
    private val lineStart: Int,
    private val lineEnd: Int
) {
    private class Glyph(val layout: SyllableLayout, density: Float) {
        val width = layout.textLayoutResult.size.width.toFloat().coerceAtLeast(1f)
        val rtl = layout.textLayoutResult.getParagraphDirection(0) == ResolvedTextDirection.Rtl
        val feather = minOf(5f * density, width * 0.2f).coerceAtLeast(0.5f)
        val fill = MovingLyricsGradient(false)
        val sheen = MovingLyricsGradient(true)
        // Horizontal glyph centre (dp) gives the spatial phase of the
        // shared line-wide carrier. No per-syllable start/end oscillator.
        val motion = LyricsFloatingMotion((layout.position.x + width / 2f) / density)
    }
    private val glyphs = layouts.flatten().mapNotNull { layout ->
        if (layout.syllable.content.isBlank()) null else Glyph(layout, density)
    }
    private val glowPalette = LyricsGlowPalette(density)

    /** Both passes share one transform and stable shader objects; halos stay behind all glyphs. */
    fun draw(scope: DrawScope, now: Int, focus: Float, waveTime: Int) = with(scope) {
        val strength = focus.coerceIn(0f, 1f)
        // Evaluated once for the entire row. Only highlights and glows
        // depend on each syllable's own timestamps.
        val waveStrength = lyricsLineWaveIntensity(lineStart, lineEnd, waveTime)
        for (pass in 0..1) for (glyph in glyphs) {
            val halo = pass == 0
            val layout = glyph.layout
            val unit = layout.syllable
            // Compute the audio-time transform once for both halo and fill passes.
            if (pass == 0) glyph.motion.update(waveTime, waveStrength)
            if (halo && (strength <= 0f || now <= unit.start || now >= unit.end)) continue
            val result = layout.textLayoutResult
            val position = layout.position - Offset(0f, glyph.motion.liftDp * density)
            val width = glyph.width
            val progress = syllableProgress(unit, now)
            val glow = syllableGlowEnvelope(unit.start, unit.end, now) * lyricBreathing(unit.start, unit.end, now) * strength
            withTransform({
                scale(glyph.motion.scale, glyph.motion.scale, position + Offset(width / 2f, result.size.height * 0.8f))
            }) {
                if (!halo) drawText(result, color = Color.White.copy(alpha = 0.4f), topLeft = position, shadow = Shadow.None)
                if (strength <= 0f || progress <= 0f) return@withTransform
                val distance = (width + 2f * glyph.feather) * progress - glyph.feather
                val edge = if (glyph.rtl) width - distance else distance
                val direction = if (glyph.rtl) -1f else 1f
                glyph.fill.move(edge - direction * glyph.feather, edge + direction * glyph.feather)
                if (halo) {
                    if (glow <= 0f) return@withTransform
                    val bloom = syllableBloom(unit.start, unit.end, now)
                    drawText(result, brush = glyph.fill.brush, topLeft = position, alpha = 0.22f * glow,
                        shadow = glowPalette.broad(glow, bloom))
                    drawText(result, brush = glyph.fill.brush, topLeft = position, alpha = 0.18f * glow,
                        shadow = glowPalette.tight(glow))
                } else {
                    if (progress >= 1f) drawText(result, color = Color.White, topLeft = position, alpha = strength * 0.85f, shadow = Shadow.None)
                    else drawText(result, brush = glyph.fill.brush, topLeft = position, alpha = strength * 0.85f, shadow = Shadow.None)
                    val shimmer = lyricShimmer(unit.start, unit.end, now)
                    val sungWidth = (width * progress - glyph.feather).coerceAtLeast(0f)
                    if (shimmer > 0f && sungWidth >= 1f) {
                        val band = minOf(width * 0.3f + 4f * density, sungWidth / 2f)
                        val swept = band + (sungWidth - 2f * band) * shimmer
                        val center = if (glyph.rtl) width - swept else swept
                        glyph.sheen.move(center - band, center + band)
                        drawText(result, brush = glyph.sheen.brush, topLeft = position, alpha = 0.45f * glow, shadow = Shadow.None)
                    }
                }
            }
        }
    }
}

/** Authored attack/release; audio timestamps freeze all effects on pause. */
internal fun syllableGlowEnvelope(start: Int, end: Int, now: Int): Float {
    val duration = end.toLong() - start
    if (duration <= 0 || now <= start || now >= end) return 0f
    val ramp = minOf(120.0, duration / 2.0).coerceAtLeast(0.5)
    return lyricEase(((now.toLong() - start) / ramp).toFloat()) *
        lyricEase(((end.toLong() - now) / ramp).toFloat())
}
