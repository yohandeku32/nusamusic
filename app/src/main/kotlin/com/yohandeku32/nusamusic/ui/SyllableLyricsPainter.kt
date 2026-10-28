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
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.style.ResolvedTextDirection
import com.mocharealm.accompanist.lyrics.ui.composable.lyrics.SyllableLayout
import com.yohandeku32.nusamusic.data.LyricsUnitKey
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
    viewportWidthPx: Float = 0f,
    inferredEnds: Set<LyricsUnitKey> = emptySet(),
    textMeasurer: TextMeasurer,
    private val baseAlpha: Float = 0.4f,
    private val highlightAlpha: Float = 0.85f
) {
    private class Letter(
        val result: TextLayoutResult,
        val offset: Offset,
        val centerPathDp: Float
    ) {
        var pose = LyricsHeldWordPose()
        var shortLiftPx = 0f
    }

    private class Glyph(
        val index: Int,
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

        val horizontalDp = (position.x + width / 2f) / density
        var wavePose = LyricsHeldWordPose()
        var wordLiftPx = 0f
        var letters: List<Letter> = emptyList()
        val controller = LyricsExpressionController(layout.syllable.start, layout.syllable.end, expression)
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
                val glyph = Glyph(size, layout, density, endPathDp + gap, profiles[size], rowShiftPx)
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
            // Motion eligibility must not change when async phrase analysis arrives.
            reliableHold = LyricsUnitKey(glyph.layout.syllable.start,
                glyph.layout.syllable.content.trim()) !in inferredEnds
        )
    }
    private val heldWords = LyricsHeldWordWave(heldWordUnits)
    private val wordWave = LyricsLetterWave(heldWordUnits)
    private val glowPalette = LyricsGlowPaletteCache.forDensity(density)

    init {
        for (glyph in glyphs) {
            // Both short pulls and sustained vocals use complete graphemes.
            // Rapid/inferred words keep the cheaper intact-run drawing path.
            if ((!heldWords.hasHold(glyph.index) && !wordWave.hasMotion(glyph.index)) || glyph.rtl) continue
            val original = glyph.layout.textLayoutResult
            val text = glyph.layout.syllable.content
            if (original.layoutInput.text.text != text) continue
            val ranges = lyricIndependentLetterRanges(text)
            if (ranges.size !in 1..64) continue
            // Cache complete graphemes, not clipped strips of the word. Keep
            // these layouts at rest too, so entering a hold cannot swap fonts.
            val letters = ranges.mapNotNull { range ->
                val content = text.substring(range.first, range.last + 1)
                if (content.isBlank()) return@mapNotNull null
                val left = original.getHorizontalPosition(range.first, true)
                val right = original.getHorizontalPosition(range.last + 1, true)
                if (!left.isFinite() || !right.isFinite() || right <= left) return@mapNotNull null
                val measured = textMeasurer.measure(content, original.layoutInput.style,
                    softWrap = false)
                Letter(measured, Offset(left, original.firstBaseline - measured.firstBaseline),
                    glyph.pathStartDp + (left + right) / (2f * density))
            }
            if (letters.size == ranges.count { !text.substring(it.first, it.last + 1).isBlank() }) {
                glyph.letters = letters
            }
        }
    }

    private val frameClock = LyricsFrameTimeSmoother()
    private var companionAt = Int.MIN_VALUE
    private var companionRevision = Int.MIN_VALUE
    private var companionValue = LyricsCompanionPose.Rest

    /** Whole-line followers sample the source's measured wave, not translated word timings. */
    fun companionPose(waveTime: Int, playbackRevision: Int): LyricsCompanionPose {
        val now = frameClock.sample(waveTime, playbackRevision)
        if (now == companionAt && playbackRevision == companionRevision) return companionValue
        var lift = 0f
        var depth = 0f
        var glow = 0f
        for (glyph in glyphs) {
            val center = (glyph.pathStartDp + glyph.pathEndDp) * 0.5f
            val pose = heldWords.pose(glyph.index, now, center)
            val short = if (heldWords.hasHold(glyph.index))
                wordWave.wordLiftDp(glyph.index, now) else wordWave.liftDp(glyph.index, center, now)
            lift = minOf(lift, pose.liftDp * 0.70f + short)
            depth = maxOf(depth, pose.depth)
            glyph.controller.update(now)
            glow = maxOf(glow, glyph.controller.glow, heldVocalGlowEnergy(pose.depth, glyph.controller.glow))
        }
        companionAt = now
        companionRevision = playbackRevision
        companionValue = if (lift == 0f && depth == 0f && glow == 0f) LyricsCompanionPose.Rest
            else LyricsCompanionPose(lift, 1f + 0.055f * depth, glow)
        return companionValue
    }

    fun draw(scope: DrawScope, now: Int, focus: Float, waveTime: Int,
             playbackRevision: Int) = with(scope) {
        val frameTime = frameClock.sample(waveTime, playbackRevision)
        val strength = focus.coerceIn(0f, 1f)
 
        for ((index, glyph) in glyphs.withIndex()) {
            glyph.wavePose = heldWords.pose(
                index, frameTime, (glyph.pathStartDp + glyph.pathEndDp) * 0.5f
            )
            glyph.controller.update(frameTime)
            val amplitude = strength * (1f - 0.88f * glyph.wavePose.depth)
            val heldVocal = heldWords.hasHold(index)
            // A short run that cannot separate graphemes stays anchored.
            glyph.wordLiftPx = if (heldVocal)
                wordWave.wordLiftDp(index, frameTime) * density * amplitude else 0f
            for (letter in glyph.letters) {
                letter.pose = heldWords.letterPose(index, frameTime, letter.centerPathDp)
                letter.shortLiftPx = if (heldVocal) 0f else
                    wordWave.liftDp(index, letter.centerPathDp, frameTime) * density * strength
            }
            glyph.bubble.update(frameTime, glyph.wavePose.intensity * 0.30f)
        }

        for (pass in 0..1) for (glyph in glyphs) {
            val halo = pass == 0
            val unit = glyph.layout.syllable
            if (halo && (strength <= 0f || glyph.controller.glow <= 0f)) continue
            val progress = syllableProgress(unit, frameTime)
            val glow = glyph.controller.glow * strength
            if (glyph.letters.isEmpty()) {
                drawGlyph(glyph, halo, progress, glow, strength, frameTime)
            } else for (letter in glyph.letters) {
                drawGlyph(glyph, halo, progress, glow, strength, frameTime, letter)
            }
        }
    }

    private fun DrawScope.drawGlyph(
        glyph: Glyph,
        halo: Boolean,
        progress: Float,
        glow: Float,
        strength: Float,
        now: Int,
        letter: Letter? = null
    ) {
        val result = letter?.result ?: glyph.layout.textLayoutResult
        val pose = letter?.pose ?: glyph.wavePose
        val runOffset = letter?.offset ?: Offset.Zero
        val position = glyph.position + runOffset + Offset(
            pose.parallaxXDp * density,
            pose.liftDp * density * 0.70f + (letter?.shortLiftPx ?: glyph.wordLiftPx)
        )
        val forwardBulge = heldWordForwardBulge(pose.depth, pose.intensity)
        // Shared X expansion preserves advances; each character has its own
        // lift, Y stretch and camera plane along the travelling crest.
        val scaleX = 1f + 0.075f * glyph.wavePose.depth
        val scaleY = 1f + (forwardBulge.scaleY - 1f) * 0.70f
        val width = glyph.width
        val pivot = Offset(
            x = if (glyph.wavePose.depth > 0f) heldWordPivotX(
                glyph.position.x, glyph.pathStartDp, glyph.wavePose.wordCenterDp, density
            ) else glyph.position.x + width / 2f,
            y = glyph.position.y + glyph.layout.textLayoutResult.size.height * 0.8f
        )
        val native = drawContext.canvas.nativeCanvas
        val saved = if (pose.depth > 0.0001f) native.save() else -1
        try {
            if (saved >= 0) {
                glyph.perspective.apply(
                    native,
                    pivotX = if (letter == null) pivot.x else position.x + result.size.width * 0.5f,
                    pivotY = pivot.y,
                    depth = forwardBulge.cameraDepth,
                    slope = pose.perspectiveSlope
                )
            }
            withLyricsScale(scaleX, scaleY, pivot) {
            // TextLayoutResult reuses AndroidTextPaint: a null drawStyle retains
            // the last Stroke. Reset every ordinary pass explicitly to Fill.
            if (!halo) {
                drawText(result, color = Color.White.copy(alpha = baseAlpha),
                    topLeft = position, shadow = Shadow.None, drawStyle = Fill)
            }
            if (strength <= 0f || progress <= 0f) return@withLyricsScale
            val distance = (width + 2f * glyph.feather) * progress - glyph.feather
            val edge = if (glyph.rtl) width - distance else distance
            val direction = if (glyph.rtl) -1f else 1f
            glyph.fill.move(edge - direction * glyph.feather - runOffset.x,
                edge + direction * glyph.feather - runOffset.x)
            if (halo) {
                if (glow <= 0f) return@withLyricsScale
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
                        centerX = (pose.crestDp - glyph.pathStartDp) * density - runOffset.x,
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
                        alpha = strength * highlightAlpha, shadow = Shadow.None, drawStyle = Fill)
                } else {
                    drawText(result, brush = glyph.fill.brush, topLeft = position,
                        alpha = strength * highlightAlpha, shadow = Shadow.None, drawStyle = Fill)
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
                    glyph.sheen.move(center - band - runOffset.x, center + band - runOffset.x)
                    drawText(result, brush = glyph.sheen.brush, topLeft = position,
                        alpha = 0.45f * glow, shadow = Shadow.None, drawStyle = Fill)
                }
            }
        }
        } finally {
            if (saved >= 0) native.restoreToCount(saved)
        }
    }

    private inline fun DrawScope.withLyricsScale(
        scaleX: Float, scaleY: Float, pivot: Offset, draw: DrawScope.() -> Unit
    ) {
        if (scaleX == 1f && scaleY == 1f) draw()
        else withTransform({ scale(scaleX, scaleY, pivot) }) { draw() }
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
