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
    private class Glyph(
        val layout: SyllableLayout,
        density: Float,
        val pathStartDp: Float
    ) {
        val width = layout.textLayoutResult.size.width.toFloat().coerceAtLeast(1f)
        val widthDp = width / density
        val rtl = layout.textLayoutResult.getParagraphDirection(0) == ResolvedTextDirection.Rtl
        val feather = minOf(5f * density, width * 0.2f).coerceAtLeast(0.5f)
        val fill = MovingLyricsGradient(false)
        val sheen = MovingLyricsGradient(true)
        val pathEndDp = pathStartDp + widthDp

        // Transform the complete measured syllable as ONE intact text run.
        // Per-22dp clipRect strips cut through letter strokes when neighbouring
        // slices have slightly different lift/scale, especially on slow vocals.
        // Keep the approved V6 horizontal phase at this run's visual centre.
        val motion = LyricsFloatingMotion((layout.position.x + width / 2f) / density)
    }

    private val glyphs = buildList {
        var endPathDp = 0f
        var previous: Glyph? = null
        for (layout in layouts.flatten()) {
            if (layout.syllable.content.isBlank()) continue
            val gap = previous?.let { last ->
                if (kotlin.math.abs(layout.position.y - last.layout.position.y) < 0.5f) {
                    ((layout.position.x - last.layout.position.x - last.width) / density)
                        .coerceAtLeast(0f)
                } else {
                    // Wrapped rows continue along the same vocal path rather
                    // than jumping to x=0 and restarting the mask.
                    12f
                }
            } ?: 0f
            val glyph = Glyph(layout, density, endPathDp + gap)
            add(glyph)
            endPathDp = glyph.pathEndDp
            previous = glyph
        }
    }

    private val vocalMask = LyricsTravelingWaveMask(glyphs.map { glyph ->
        LyricsVocalSpan(
            glyph.layout.syllable.start,
            glyph.layout.syllable.end,
            glyph.pathStartDp,
            glyph.pathEndDp
        )
    })
    private val glowPalette = LyricsGlowPalette(density)

    /**
     * Keep the V6 global phase and V7 traveling vocal front, but apply their
     * combined intensity uniformly to an entire syllable text run. Drawing one
     * intact glyph layout prevents seams, broken strokes, and doubled edges
     * at slice boundaries on long or slowly sung notes.
     *
     * Halo and foreground always use the SAME transform; highlighting,
     * shimmer, lyric timings, and scrolling are unchanged.
     */
    fun draw(scope: DrawScope, now: Int, focus: Float, waveTime: Int) = with(scope) {
        val strength = focus.coerceIn(0f, 1f)
        val lineStrength = lyricsLineWaveIntensity(lineStart, lineEnd, waveTime)
        val front = if (lineStrength > 0f) vocalMask.front(waveTime) else null

        // Playback time alone drives the transform. No animation restarting,
        // no clipping through characters and no text measurement per frame.
        for (glyph in glyphs) {
            val influence = vocalMask.glyphInfluence(
                front, glyph.pathStartDp, glyph.pathEndDp
            )
            glyph.motion.update(waveTime, lineStrength * influence)
        }

        for (pass in 0..1) for (glyph in glyphs) {
            val halo = pass == 0
            val unit = glyph.layout.syllable
            if (halo && (strength <= 0f || now <= unit.start || now >= unit.end)) continue
            val progress = syllableProgress(unit, now)
            val glow = syllableGlowEnvelope(unit.start, unit.end, now) *
                lyricBreathing(unit.start, unit.end, now) * strength

            drawGlyph(glyph, halo, progress, glow, strength, now)
        }
    }

    private fun DrawScope.drawGlyph(
        glyph: Glyph,
        halo: Boolean,
        progress: Float,
        glow: Float,
        strength: Float,
        now: Int
    ) {
        val result = glyph.layout.textLayoutResult
        val shift = glyph.motion.liftDp
        val scaleFactor = glyph.motion.scale
        val position = glyph.layout.position - Offset(0f, shift * density)
        val width = glyph.width
        withTransform({
            scale(
                scaleFactor, scaleFactor,
                position + Offset(width / 2f, result.size.height * 0.8f)
            )
        }) {
            if (!halo) {
                drawText(result, color = Color.White.copy(alpha = 0.4f),
                    topLeft = position, shadow = Shadow.None)
            }
            if (strength <= 0f || progress <= 0f) return@withTransform
            val distance = (width + 2f * glyph.feather) * progress - glyph.feather
            val edge = if (glyph.rtl) width - distance else distance
            val direction = if (glyph.rtl) -1f else 1f
            glyph.fill.move(edge - direction * glyph.feather, edge + direction * glyph.feather)
            if (halo) {
                if (glow <= 0f) return@withTransform
                val bloom = syllableBloom(
                    glyph.layout.syllable.start, glyph.layout.syllable.end, now
                )
                drawText(result, brush = glyph.fill.brush, topLeft = position,
                    alpha = 0.22f * glow, shadow = glowPalette.broad(glow, bloom))
                drawText(result, brush = glyph.fill.brush, topLeft = position,
                    alpha = 0.18f * glow, shadow = glowPalette.tight(glow))
            } else {
                if (progress >= 1f) {
                    drawText(result, color = Color.White, topLeft = position,
                        alpha = strength * 0.85f, shadow = Shadow.None)
                } else {
                    drawText(result, brush = glyph.fill.brush, topLeft = position,
                        alpha = strength * 0.85f, shadow = Shadow.None)
                }
                val unit = glyph.layout.syllable
                val shimmer = lyricShimmer(unit.start, unit.end, now)
                val sungWidth = (width * progress - glyph.feather).coerceAtLeast(0f)
                if (shimmer > 0f && sungWidth >= 1f) {
                    val band = minOf(width * 0.3f + 4f * density, sungWidth / 2f)
                    val swept = band + (sungWidth - 2f * band) * shimmer
                    val center = if (glyph.rtl) width - swept else swept
                    glyph.sheen.move(center - band, center + band)
                    drawText(result, brush = glyph.sheen.brush, topLeft = position,
                        alpha = 0.45f * glow, shadow = Shadow.None)
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
