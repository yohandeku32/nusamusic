package com.yohandeku32.nusamusic.ui

import kotlin.math.PI
import kotlin.math.sin

/**
 * ONE carrier shared by all glyphs in all lines. Only playback position and
 * measured horizontal coordinates control its phase. Syllable boundaries
 * never restart the wave, and pausing simply holds the current sample.
 *
 * A wavelength longer than a typical lyric row gives neighbouring glyphs
 * close phases while the full row still bends like a soft ribbon.
 */
internal class LyricsFloatingMotion(private val horizontalDp: Float) {
    var scale = 1f
        private set
    var liftDp = 0f
        private set

    fun update(playbackMs: Int, lineIntensity: Float) {
        val intensity = lineIntensity.coerceIn(0f, 1f)
        if (intensity <= 0f) {
            scale = 1f
            liftDp = 0f
            return
        }

        // Global 4.2-second clock; position changes phase spatially instead of
        // giving each word or syllable its own independent animation.
        val timePhase = playbackMs.toDouble() * 2.0 * PI / 4200.0
        val spatialPhase = horizontalDp.toDouble() * 2.0 * PI / 900.0
        val phase = timePhase - spatialPhase

        liftDp = (2.15 * intensity * sin(phase)).toFloat()
        val breathing = 0.5 + 0.5 * sin(phase - 0.65)
        scale = (1.0 + 0.0175 * intensity * breathing).toFloat()
    }
}

/**
 * Smooth crossfade of floating intensity around an authored line boundary.
 * At a contiguous boundary (previous.end == next.start), the outgoing and
 * incoming line envelopes each equal 0.5: neither wave changes phase.
 *
 * This is intentionally independent of active syllable and resetAt state.
 * Syllable highlighting/glow still use their original timestamps.
 */
internal fun lyricsLineWaveIntensity(start: Int, end: Int, now: Int): Float {
    if (end <= start) return 0f
    val transitionMs = minOf(400f, (end.toLong() - start).toFloat())
        .coerceAtLeast(1f)
    val half = transitionMs / 2f
    val fadeIn = lyricEase((now.toLong() - start + half) / transitionMs)
    val fadeOut = lyricEase((end.toLong() - now + half) / transitionMs)
    return fadeIn * fadeOut
}
