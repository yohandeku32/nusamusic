package com.yohandeku32.nusamusic.ui

import kotlin.math.PI
import kotlin.math.sin

/**
 * Extra expression only for sustained syllables. This is deliberately separate
 * from LyricsFloatingMotion: its 4.2s traveling carrier and vocal mask remain
 * unchanged. The entire shaped syllable receives ONE transform, not clipped
 * per-letter strips (which previously produced torn glyphs).
 *
 * Pure playback-time sampling freezes on pause and reproduces the same pose
 * after a seek. Horizontal placement adds a gentle phase offset to adjacent
 * syllables without shifting their layout or creating an individual clock.
 */
internal class LyricsLongVocalMotion(
    private val startMs: Int,
    private val endMs: Int,
    private val horizontalDp: Float,
    private val expression: LyricsAdaptiveExpression = LyricsAdaptiveExpression(endMs.toLong() - startMs)
) {
    var scaleX = 1f
        private set
    var scaleY = 1f
        private set
    var offsetXDp = 0f
        private set
    var offsetYDp = 0f
        private set
    var glowBoost = 0f
        private set

    /** Strength of the visual crescendo, independent from actual loudness. */
    var luminousCrescendo = 0f
        private set

    /** Soft contour-only expansion; the filled text remains sharp. */
    var edgeExpansion = 0f
        private set

    fun update(playbackMs: Int, localInfluence: Float) {
        val duration = endMs.toLong() - startMs.toLong()
        if (duration <= 700L || playbackMs <= startMs || playbackMs >= endMs ||
            localInfluence <= 0f
        ) {
            reset()
            return
        }

        // Blend expression by evidenced vocal duration. Short rapid notes
        // retain clean highlighting; no artificial vibrato metadata assumed.
        val eligible = expression.breathing
        val elapsed = (playbackMs.toLong() - startMs).toFloat()
        val envelope = expression.envelope(startMs, endMs, playbackMs)
        val energy = (eligible * envelope * expression.phraseAt(startMs, playbackMs) * localInfluence.coerceIn(0f, 1f))
            .coerceIn(0f, 1f)

        if (energy <= 0f) {
            reset()
            return
        }

        // One global slow ~2.85s bubble clock; glyph x-position only offsets
        // its phase slightly, so neighbouring words respond organically.
        val phase = playbackMs.toDouble() * 2.0 * PI / 2850.0 -
            horizontalDp.toDouble() * 0.0045
        val breathX = 0.5 + 0.5 * sin(phase - 0.65)
        val breathY = 0.5 + 0.5 * sin(phase - 0.4)
        scaleX = (1.0 + 0.052 * energy * breathX).toFloat()
        scaleY = (1.0 + 0.056 * energy * breathY).toFloat()

        // Continuous 1.0–1.4Hz resonances, under 0.32dp combined.
        // They move the complete glyph run as a unit, never its sub-pixels
        // independently. No random values, oscillator jobs or springs.
        val t = playbackMs.toDouble()
        val x = horizontalDp.toDouble()
        val vibrationEnergy = energy * expression.vibration
        offsetXDp = (0.19 * vibrationEnergy *
            sin(t * 2.0 * PI / 1030.0 + x * 0.009)).toFloat()
        offsetYDp = (vibrationEnergy * (
            0.22 * sin(t * 2.0 * PI / 940.0 + x * 0.008) +
                0.09 * sin(t * 2.0 * PI / 710.0 + x * 0.003 - 0.8)
        )).toFloat()

        // The original progressive highlight controls which letters glow.
        // This small breathing factor only nudges its existing halo strength.
        glowBoost = (energy * breathY.toFloat()).coerceIn(0f, 1f)

        // A visual crescendo only: authored syllable duration supplies timing,
        // NOT any inferred information about vocal volume or real vibrato.
        // Reach full expression late in a held note, then the existing
        // attack/release envelope carries both contour and bloom to zero.
        val progress = (elapsed / duration.toFloat()).coerceIn(0f, 1f)
        val rising = 0.22f + 0.78f * lyricEase(progress / 0.73f)
        val crescendo = (energy * expression.elastic * rising).coerceIn(0f, 1f)
        val slowLight = 0.82 + 0.18 * breathY
        luminousCrescendo = (crescendo * slowLight.toFloat()).coerceIn(0f, 1f)

        // A second slow, globally aligned ripple affects the outline only.
        // The face of the text remains one intact shaped glyph run; no
        // independently animated per-character slices or shader allocations.
        val edgePhase = t * 2.0 * PI / 3250.0 - x * 0.005
        val edgePulse = 0.78 + 0.22 * (0.5 + 0.5 * sin(edgePhase))
        edgeExpansion = (crescendo * edgePulse.toFloat()).coerceIn(0f, 1f)
    }

    private fun reset() {
        scaleX = 1f
        scaleY = 1f
        offsetXDp = 0f
        offsetYDp = 0f
        glowBoost = 0f
        luminousCrescendo = 0f
        edgeExpansion = 0f
    }
}
