package com.yohandeku32.nusamusic.ui

import kotlin.math.PI
import kotlin.math.sin

/**
 * Pure audio-time motion: drawing the same timestamp always produces the same
 * transform. No repeating animation job, frame-time integrator, or spring can
 * restart when Compose recomposes, the user pauses, or the player seeks.
 *
 * One small onset lift remains at each authored syllable boundary. Sustained
 * vowels float on a much slower continuous carrier shared across syllables.
 */
internal class LyricsFloatingMotion(
    private val start: Int,
    private val end: Int,
    private val ordinal: Int
) {
    var scale = 1f
        private set
    var liftDp = 0f
        private set

    fun update(now: Int) {
        val duration = end.toLong() - start
        if (duration <= 0L || now <= start || now >= end) {
            scale = 1f
            liftDp = 0f
            return
        }

        val elapsed = (now.toLong() - start).toFloat()
        val remaining = (end.toLong() - now).toFloat()
        val durationFloat = duration.toFloat()

        // Gentle enter/leave envelopes prevent a held note from snapping to
        // or from baseline. The sustain envelope modulates amplitude, not speed.
        val attackMs = minOf(520f, durationFloat * 0.42f).coerceAtLeast(1f)
        val releaseMs = minOf(440f, durationFloat * 0.32f).coerceAtLeast(1f)
        val sustain = lyricEase(elapsed / attackMs) *
            lyricEase(remaining / releaseMs)
        val heldNote = lyricEase((durationFloat - 500f) / 1300f)
        val floatAmount = sustain * (0.3f + 0.7f * heldNote)

        // A single subtle onset pulse, not a repeatedly bouncing syllable.
        val onsetMs = minOf(300f, durationFloat * 0.45f).coerceAtLeast(1f)
        val onset = if (elapsed < onsetMs) {
            val pulse = sin(PI * (elapsed / onsetMs)).toFloat()
            pulse * pulse
        } else 0f

        // Continuous ~3.4-second period in audio time. Adjacent syllables
        // share a carrier; their slight phase difference gives a flowing look.
        val phase = now.toDouble() * 2.0 * PI / 3400.0 + ordinal * 0.28
        val wave = sin(phase).toFloat()
        val breath = (0.5 + 0.5 * sin(phase - 0.65)).toFloat()

        // Never move the row's layout box: offsets stay inside DrawScope.
        scale = 1f + 0.003f * onset + 0.0108f * floatAmount * breath
        liftDp = -0.4f * onset + 1.05f * floatAmount * wave
    }
}
