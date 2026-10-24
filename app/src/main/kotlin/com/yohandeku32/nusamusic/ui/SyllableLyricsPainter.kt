package com.yohandeku32.nusamusic.ui

import android.graphics.LinearGradient
import android.graphics.RadialGradient
import android.graphics.Matrix
import android.graphics.Shader
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.nativeCanvas
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

/**
 * Elliptical hill-shaped illumination for held notes. The crest follows the
 * word's measured text path in X and its slow mountain rise in Y, replacing
 * the former purely horizontal shimmer as the main sustained-vowel accent.
 * Shader and matrix are cached per syllable; no per-frame allocations.
 */
private class MovingLyricsMountainGlow {
    private val shader = RadialGradient(
        0f, 0f, 1f,
        intArrayOf(0xFFFFFFFF.toInt(), 0xBFFFFFFF.toInt(), 0x00FFFFFF),
        floatArrayOf(0f, 0.35f, 1f), Shader.TileMode.CLAMP
    )
    val brush = ShaderBrush(shader)
    private val matrix = Matrix()

    fun move(centerX: Float, centerY: Float, radiusX: Float, radiusY: Float) {
        matrix.setScale(radiusX.coerceAtLeast(1f), radiusY.coerceAtLeast(1f))
        matrix.postTranslate(centerX, centerY)
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
    // Restrained sustain-only lighting: avoid a broad white cloud obscuring
    // the real edges of neighbouring glyphs. Prebuilt for smooth 120 Hz draw.
    private val heldAura = Array(65) { energy ->
        Shadow(Color.White.copy(alpha = 0.48f * energy / 64f), Offset.Zero, 15f * density)
    }
    private val heldCore = Array(65) { energy ->
        Shadow(Color.White.copy(alpha = 0.62f * energy / 64f), Offset.Zero, 4f * density)
    }
    // A thin bright contour is a safe alternative to shader-displacing
    // portions of a shaped glyph (previously caused visible broken strokes).
    private val edgeStrokes = Array(33) { step ->
        Stroke(width = (0.30f + 0.03f * step) * density)
    }
    fun broad(energy: Float, bloom: Float) = broad[(energy * 64f).roundToInt().coerceIn(0, 64)][(bloom * 8f).roundToInt().coerceIn(0, 8)]
    fun tight(energy: Float) = tight[(energy * 64f).roundToInt().coerceIn(0, 64)]
    fun outer(energy: Float) = outer[(energy * 64f).roundToInt().coerceIn(0, 64)]
    fun heldAura(energy: Float) = heldAura[(energy * 64f).roundToInt().coerceIn(0, 64)]
    fun heldCore(energy: Float) = heldCore[(energy * 64f).roundToInt().coerceIn(0, 64)]
    fun edgeStroke(energy: Float) = edgeStrokes[(energy * 32f).roundToInt().coerceIn(0, 32)]
}

// Shadow tables are immutable; all visible rows share a small density cache.
private object LyricsGlowPaletteCache {
    private val palettes = LinkedHashMap<Int, LyricsGlowPalette>(4, 0.75f, true)
    fun forDensity(density: Float): LyricsGlowPalette = synchronized(palettes) {
        val key = (density * 100f).roundToInt()
        palettes[key] ?: LyricsGlowPalette(density).also {
            palettes[key] = it
            while (palettes.size > 4) palettes.remove(palettes.keys.first())
        }
    }
}

internal class SyllableLyricsPainter(
    layouts: List<List<SyllableLayout>>,
    private val density: Float,
    expressions: List<LyricsAdaptiveExpression>? = null,
    alignEnd: Boolean = false,
    viewportWidthPx: Float = 0f
) {
    private class Glyph(
        val layout: SyllableLayout,
        density: Float,
        val pathStartDp: Float,
        val expression: LyricsAdaptiveExpression,
        val rowShiftPx: Float
    ) {
        val position = layout.position + Offset(rowShiftPx, 0f)
        val width = layout.textLayoutResult.size.width.toFloat().coerceAtLeast(1f)
        val widthDp = width / density
        val rtl = layout.textLayoutResult.getParagraphDirection(0) == ResolvedTextDirection.Rtl
        val feather = minOf(5f * density, width * 0.2f).coerceAtLeast(0.5f)
        val fill = MovingLyricsGradient(false)
        val sheen = MovingLyricsGradient(true)
        val mountainGlow = MovingLyricsMountainGlow()
        val perspective by lazy(LazyThreadSafetyMode.NONE) { LyricsPerspectiveProjection() }
        val pathEndDp = pathStartDp + widthDp

        // Transform the complete measured syllable as ONE intact text run.
        // Per-22dp clipRect strips cut through letter strokes when neighbouring
        // slices have slightly different lift/scale, especially on slow vocals.
        // Keep the approved V6 horizontal phase at this run's visual centre.
        val horizontalDp = (position.x + width / 2f) / density
        // Word-local shape is sampled once per frame, reused by halo and fill.
        var wavePose = LyricsHeldWordPose()
        // Gentle movement of one intact rendered syllable at a time.
        var passingLiftDp = 0f
        var passingInfluence = 0f
        val controller = LyricsExpressionController(layout.syllable.start, layout.syllable.end, expression)
        // Separate sustained-vocal accent. Reuses the intact syllable's
        // measured text run so no individual character can break apart.
        val bubble = LyricsLongVocalMotion(
            layout.syllable.start, layout.syllable.end, horizontalDp, expression
        )
    }

    private val glyphs = buildList {
        // Use accurate authored fill immediately; phrase analysis arrives
        // off-main from SyncedLyricsView. Do not redo it for each visible row.
        val profiles = expressions ?: run {
            val neutral = LyricsAdaptiveExpression(0)
            List(layouts.sumOf { row -> row.count { it.syllable.content.isNotBlank() } }) {
                neutral
            }
        }
        var endPathDp = 0f
        var previous: Glyph? = null
        for (row in layouts) {
            // Right-align each *wrapped* duet row, not just the whole phrase.
            // Measure once while constructing the painter, never per frame.
            val rowRight = row.maxOfOrNull {
                it.position.x + it.textLayoutResult.size.width.toFloat()
            } ?: 0f
            val rowShiftPx = if (alignEnd)
                (viewportWidthPx - rowRight - 8f * density).coerceAtLeast(0f)
            else 0f
            for (layout in row) {
                if (layout.syllable.content.isBlank()) continue
                val gap = previous?.let { last ->
                    if (kotlin.math.abs(layout.position.y - last.layout.position.y) < 0.5f) {
                        ((layout.position.x - last.layout.position.x - last.width) / density)
                            .coerceAtLeast(0f)
                    } else {
                        // Wrapped rows continue along the same vocal path
                        // without restarting the wave on a new visual row.
                        12f
                    }
                } ?: 0f
                val glyph = Glyph(layout, density, endPathDp + gap, profiles[size], rowShiftPx)
                add(glyph)
                endPathDp = glyph.pathEndDp
                previous = glyph
            }
        }
    }

    // Every word is independent: only authored, reliably timed long notes
    // produce a mountain wave. Completed words remain completely stationary.
    private val heldWordUnits = glyphs.map { glyph ->
        LyricsHeldWordUnit(
            content = glyph.layout.syllable.content,
            startMs = glyph.layout.syllable.start,
            endMs = glyph.layout.syllable.end,
            pathStartDp = glyph.pathStartDp,
            pathEndDp = glyph.pathEndDp,
            reliableHold = glyph.expression.breathing > 0f
        )
    }
    private val heldWords = LyricsHeldWordWave(heldWordUnits)
    // Ordinary words breathe on their OWN authored timestamps.
    // Do not let a single sentence-wide playhead raise neighbouring words.
    private val wordPulse = LyricsTimedWordPulse(heldWordUnits)
    private val glowPalette = LyricsGlowPaletteCache.forDensity(density)

    /**
     * Long holds move only their OWN word. Each syllable is shaped and drawn
     * intact to avoid clipped letters or seams. Both glow and text share the
     * same mountain-wave transform sampled from the existing playback clock.
     */
    private val frameClock = LyricsFrameTimeSmoother()

    fun draw(scope: DrawScope, now: Int, focus: Float, waveTime: Int,
             playbackRevision: Int) = with(scope) {
        val frameTime = frameClock.sample(waveTime, playbackRevision)
        val strength = focus.coerceIn(0f, 1f)
 
        // Independent word-local envelopes replace the former sentence-wide
        // travelling front. The long held-vocal mountain remains separate.
        for ((index, glyph) in glyphs.withIndex()) {
            glyph.wavePose = heldWords.pose(
                index, frameTime, (glyph.pathStartDp + glyph.pathEndDp) * 0.5f
            )
            glyph.controller.update(frameTime)
            // One small, coherent lift per word (shared across its syllables).
            // Suppress this micro-wave under the existing long-held mountain.
            glyph.passingInfluence = wordPulse.influence(index, frameTime) *
                strength * (1f - 0.85f * glyph.wavePose.depth)
            glyph.passingLiftDp = lyricPassingLiftDp(glyph.passingInfluence)
            // Keep the old bubble as a LIGHT-only accent. Its independent
            // sinusoidal position/scale must never re-enlarge a letter while
            // the main 3D wave is smoothly shrinking back to baseline.
            glyph.bubble.update(frameTime, glyph.wavePose.intensity * 0.30f)
        }

        for (pass in 0..1) for (glyph in glyphs) {
            val halo = pass == 0
            val unit = glyph.layout.syllable
            if (halo && (strength <= 0f || glyph.controller.glow <= 0f)) continue
            val progress = syllableProgress(unit, frameTime)
            val glow = glyph.controller.glow * strength
            drawGlyph(glyph, halo, progress, glow, strength, frameTime)
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
        val pose = glyph.wavePose
        val position = glyph.position + Offset(
            pose.parallaxXDp * density,
            (pose.liftDp + glyph.passingLiftDp) * density
        )
        // A single intact measured text run tilts and rises with the local
        // mountain, never distorting glyphs with separately clipped slices.
        // Expand every syllable as part of ONE word-shaped surface:
        // measured text and kerning remain intact, with a shared scale and
        // shared pivot so no neighbouring syllable grows/shrinks alone.
        // The mountain travels through local lift and a restrained Y bulge.
        // A travelling, restrained forward swell follows the arched crest.
        // Horizontal magnification remains shared to protect word spacing.
        // Local vertical scale and camera depth make the leading syllables
        // approach the viewer before the trailing ones, without clipped glyphs.
        val forwardBulge = heldWordForwardBulge(pose.depth, pose.intensity)
        val scaleX = 1f + 0.105f * pose.depth
        val scaleY = forwardBulge.scaleY * (1f + 0.012f * glyph.passingInfluence)
        val width = glyph.width
        val pivot = Offset(
            x = if (pose.depth > 0f) heldWordPivotX(
                glyph.position.x, glyph.pathStartDp, pose.wordCenterDp, density
            ) else glyph.position.x + width / 2f,
            y = glyph.position.y + result.size.height * 0.8f
        )
        val native = drawContext.canvas.nativeCanvas
        val saved = if (pose.depth > 0.0001f) native.save() else -1
        try {
            if (saved >= 0) {
                glyph.perspective.apply(
                    native,
                    pivotX = pivot.x,
                    pivotY = pivot.y,
                    depth = forwardBulge.cameraDepth,
                    slope = pose.perspectiveSlope
                )
            }
            withTransform({
                // No separate per-syllable pivot, micro-bounce or stroke
                // splitting: one transform for the whole shaped word.
                scale(scaleX, scaleY, pivot)
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

                // Let the LONG held vowel shine more strongly while the
                // already-approved size/3D release remains untouched.
                // The extra aura is driven by exactly the same depth value,
                // so neither its onset nor its fade can pop at word boundaries.
                val heldLight = heldVocalGlowEnergy(pose.depth, glow)
                if (heldLight > 0f) {
                    // A compact soft halo follows the crest without moving
                    // the readable outline or overpowering adjacent words.
                    drawText(result, brush = glyph.fill.brush,
                        topLeft = position + Offset(0f, 1.2f * density * pose.depth),
                        alpha = 0.18f * heldLight,
                        shadow = glowPalette.heldAura(heldLight),
                        drawStyle = Fill)
                    glyph.mountainGlow.move(
                        centerX = (pose.crestDp - glyph.pathStartDp) * density,
                        centerY = result.size.height * 0.46f +
                            pose.liftDp * density * 0.11f,
                        radiusX = maxOf(width * 0.56f, 16f * density),
                        radiusY = maxOf(result.size.height * 0.82f, 12f * density)
                    )
                    // Sharper luminous crest follows the arched vocal path;
                    // the wider aura provides volume without whitening all
                    // upcoming words or introducing a per-syllable flash.
                    drawText(result, brush = glyph.mountainGlow.brush,
                        topLeft = position,
                        alpha = 0.36f * heldLight,
                        shadow = glowPalette.heldCore(heldLight),
                        drawStyle = Fill)
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
                // On held vocals, the curving mountain glow should lead;
                // keep only a faint conventional horizontal sheen.
                // A per-syllable shimmer restarted at every word and looked
                // like a tiny bump/loop. Fast and ordinary syllables now
                // receive ONLY monotonic karaoke fill. Long vocals use the
                // shared 3D mountain and a faint residual sheen.
                val shimmer = if (unit.end.toLong() - unit.start < 2_000L)
                    0f else lyricShimmer(unit.start, unit.end, now) *
                    (1f - 0.92f * pose.intensity)
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
        } finally {
            if (saved >= 0) native.restoreToCount(saved)
        }
    }
}

/**
 * Stronger sustained-only glow, sampled from the very same smooth forward
 * depth envelope as the 3D size. It stays visible during a long vowel even
 * as the original syllable halo gently recedes, then fades to exactly zero
 * at the authored end. No extra clock, thresholds or release keyframes.
 */
internal fun heldVocalGlowEnergy(depth: Float, syllableGlow: Float): Float =
    depth.coerceIn(0f, 1f) * (0.55f + 0.45f * syllableGlow.coerceIn(0f, 1f))

/** Testable duration-weighted halo; painters cache the same profile with their layouts. */
internal fun syllableGlowEnvelope(start: Int, end: Int, now: Int): Float {
    val profile = LyricsAdaptiveExpression(end.toLong() - start)
    return profile.glow * profile.envelope(start, end, now)
}
