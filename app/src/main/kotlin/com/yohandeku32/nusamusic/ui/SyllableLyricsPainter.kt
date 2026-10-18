package com.yohandeku32.nusamusic.ui

import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Shader
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
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
    // Third, wide-radius halo reserved for held syllables. Prepared once,
    // not allocated for every draw frame or shimmer sample.
    private val outer = Array(65) { energy ->
        Shadow(Color.White.copy(alpha = 0.42f * energy / 64f), Offset.Zero, 14f * density)
    }
    // A thin bright contour is a safe alternative to shader-displacing
    // portions of a shaped glyph (previously caused visible broken strokes).
    private val edgeStrokes = Array(33) { step ->
        Stroke(width = (0.30f + 0.03f * step) * density)
    }
    fun broad(energy: Float, bloom: Float) = broad[(energy * 64f).roundToInt().coerceIn(0, 64)][(bloom * 8f).roundToInt().coerceIn(0, 8)]
    fun tight(energy: Float) = tight[(energy * 64f).roundToInt().coerceIn(0, 64)]
    fun outer(energy: Float) = outer[(energy * 64f).roundToInt().coerceIn(0, 64)]
    fun edgeStroke(energy: Float) = edgeStrokes[(energy * 32f).roundToInt().coerceIn(0, 32)]
}

internal class SyllableLyricsPainter(
    layouts: List<List<SyllableLayout>>,
    private val density: Float,
    private val lineStart: Int,
    private val lineEnd: Int,
    expressions: List<LyricsAdaptiveExpression>? = null
) {
    private class Glyph(
        val layout: SyllableLayout,
        density: Float,
        val pathStartDp: Float,
        val expression: LyricsAdaptiveExpression
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
        val horizontalDp = (layout.position.x + width / 2f) / density
        val motion = LyricsFloatingMotion(horizontalDp)
        val controller = LyricsExpressionController(layout.syllable.start, layout.syllable.end, expression)
        // Separate sustained-vocal accent. Reuses the intact syllable's
        // measured text run so no individual character can break apart.
        val bubble = LyricsLongVocalMotion(
            layout.syllable.start, layout.syllable.end, horizontalDp, expression
        )
    }

    private val glyphs = buildList {
        val profiles = expressions ?: lyricsAdaptiveExpressions(layouts.flatten()
            .filter { it.syllable.content.isNotBlank() }
            .map { LyricsExpressionTiming(it.syllable.start, it.syllable.end) })
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
            val glyph = Glyph(layout, density, endPathDp + gap, profiles[size])
            add(glyph)
            endPathDp = glyph.pathEndDp
            previous = glyph
        }
    }

    // One pivot spans every rendered syllable and wrapped row. A final held
    // vowel can then carry its entire word/phrase instead of just its letters.
    private val closingPivot = if (glyphs.isEmpty()) Offset.Zero else Offset(
        (glyphs.minOf { it.layout.position.x } +
            glyphs.maxOf { it.layout.position.x + it.width }) / 2f,
        (glyphs.minOf { it.layout.position.y } +
            glyphs.maxOf { it.layout.position.y + it.layout.textLayoutResult.size.height }) / 2f
    )
    private val closingMotion = LyricsClosingVocalMotion(
        glyphs.map { LyricsExpressionTiming(it.layout.syllable.start, it.layout.syllable.end) },
        glyphs.map { it.expression }, closingPivot.x / density
    )

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
        closingMotion.update(waveTime)

        // Playback time alone drives the transform. No animation restarting,
        // no clipping through characters and no text measurement per frame.
        for (glyph in glyphs) {
            val influence = vocalMask.glyphInfluence(
                front, glyph.pathStartDp, glyph.pathEndDp
            )
            val localIntensity = lineStrength * influence
            glyph.controller.update(waveTime)
            // Keep the original traveling mask and 4.2s carrier. Duration only
            // attenuates their amplitude; its playback phase never restarts.
            glyph.motion.update(waveTime, localIntensity * glyph.controller.wave)
            glyph.bubble.update(waveTime, localIntensity)
        }

        withTransform({
            translate(closingMotion.offsetXDp * this@SyllableLyricsPainter.density,
                closingMotion.offsetYDp * this@SyllableLyricsPainter.density)
            scale(closingMotion.scaleX, closingMotion.scaleY, closingPivot)
        }) {
            for (pass in 0..1) for (glyph in glyphs) {
                val halo = pass == 0
                val unit = glyph.layout.syllable
                if (halo && (strength <= 0f || glyph.controller.glow <= 0f)) continue
                val progress = syllableProgress(unit, now)
                val glow = glyph.controller.glow * strength

                drawGlyph(glyph, halo, progress, glow, strength, now)
            }
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
        val localWeight = closingMotion.localWeight
        val shift = glyph.motion.liftDp
        val position = glyph.layout.position + Offset(
            glyph.bubble.offsetXDp * density * localWeight,
            (glyph.bubble.offsetYDp - shift) * density * localWeight
        )
        // The carrier stays continuous. The bubble adds only a small non-uniform
        // scale around the same baseline pivot; never relayout text.
        val scaleX = 1f + (glyph.motion.scale * glyph.bubble.scaleX - 1f) * localWeight
        val scaleY = 1f + (glyph.motion.scale * glyph.bubble.scaleY - 1f) * localWeight
        val width = glyph.width
        withTransform({
            scale(
                scaleX, scaleY,
                position + Offset(width / 2f, result.size.height * 0.8f)
            )
        }) {
            // TextLayoutResult reuses AndroidTextPaint: a null drawStyle retains
            // the last Stroke. Reset every ordinary pass explicitly to Fill.
            if (!halo) {
                drawText(result, color = Color.White.copy(alpha = 0.4f),
                    topLeft = position, shadow = Shadow.None, drawStyle = Fill)
            }
            if (strength <= 0f || progress <= 0f) return@withTransform
            val distance = (width + 2f * glyph.feather) * progress - glyph.feather
            val edge = if (glyph.rtl) width - distance else distance
            val direction = if (glyph.rtl) -1f else 1f
            glyph.fill.move(edge - direction * glyph.feather, edge + direction * glyph.feather)
            if (halo) {
                if (glow <= 0f) return@withTransform
                // The approved bright tight/mid halos still use the
                // original progressive gradient. A visual-only crescendo
                // lifts them up to ~1.5x, with the existing bubble breath.
                val crescendo = glyph.bubble.luminousCrescendo
                val bloom = (
                    syllableBloom(glyph.layout.syllable.start, glyph.layout.syllable.end, now) +
                        0.12f * glyph.bubble.glowBoost +
                        0.12f * crescendo
                ).coerceIn(0f, 1f)
                val breathingGlow = glow * (
                    1f + 0.12f * glyph.bubble.glowBoost + 0.38f * crescendo
                )
                if (crescendo > 0.005f) {
                    // Light spreads outward first; near letter contours
                    // remain precise because the original fill is still
                    // painted independently in the foreground pass.
                    drawText(result, brush = glyph.fill.brush, topLeft = position,
                        alpha = 0.10f * glow * crescendo,
                        shadow = glowPalette.outer(glow * crescendo), drawStyle = Fill)
                }
                drawText(result, brush = glyph.fill.brush, topLeft = position,
                    alpha = 0.22f * breathingGlow,
                    shadow = glowPalette.broad(breathingGlow, bloom), drawStyle = Fill)
                drawText(result, brush = glyph.fill.brush, topLeft = position,
                    alpha = 0.18f * breathingGlow,
                    shadow = glowPalette.tight(breathingGlow), drawStyle = Fill)

                val edge = glyph.bubble.edgeExpansion
                if (edge > 0.005f) {
                    // Soft, slightly enlarged outline BEHIND the sharp main
                    // text. No per-character clipping, path displacement,
                    // layout changes or duplicate opaque glyph interiors.
                    // Slow phase/position variation is supplied by the
                    // already-sampled audio-time envelope.
                    withTransform({
                        scale(
                            1f + 0.012f * edge, 1f + 0.016f * edge,
                            position + Offset(width / 2f, result.size.height * 0.8f)
                        )
                    }) {
                        drawText(result, brush = glyph.fill.brush,
                            topLeft = position, alpha = 0.18f * edge,
                            shadow = Shadow.None,
                            drawStyle = glowPalette.edgeStroke(edge))
                    }
                }
            } else {
                if (progress >= 1f) {
                    drawText(result, color = Color.White, topLeft = position,
                        alpha = strength * 0.85f, shadow = Shadow.None, drawStyle = Fill)
                } else {
                    drawText(result, brush = glyph.fill.brush, topLeft = position,
                        alpha = strength * 0.85f, shadow = Shadow.None, drawStyle = Fill)
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
                        alpha = 0.45f * glow, shadow = Shadow.None, drawStyle = Fill)
                }
            }
        }
    }
}

/** Testable duration-weighted halo; painters cache the same profile with their layouts. */
internal fun syllableGlowEnvelope(start: Int, end: Int, now: Int): Float {
    val profile = LyricsAdaptiveExpression(end.toLong() - start)
    return profile.glow * profile.envelope(start, end, now)
}
