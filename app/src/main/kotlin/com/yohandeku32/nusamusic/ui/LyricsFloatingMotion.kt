package com.yohandeku32.nusamusic.ui

import kotlin.math.PI
import kotlin.math.sin

/** Reused per authored unit. One blended transform; phase comes exclusively from audio time. */
internal class LyricsFloatingMotion(private val start: Int, private val end: Int, private val ordinal: Int) {
    var scale = 1f
        private set
    var liftDp = 0f
        private set

    fun update(now: Int) {
        val duration = end.toLong() - start
        if (duration <= 0 || now <= start || now >= end) {
            scale = 1f; liftDp = 0f
            return
        }
        val elapsed = now.toLong() - start
        val remaining = end.toLong() - now
        val release = lyricEase(remaining / minOf(320f, duration * 0.3f).coerceAtLeast(1f))
        val expression = syllableExpression(start, end, now, Int.MIN_VALUE) * release
        val held = ((duration - 700) / 4300f).coerceIn(0f, 1f)
        // A shared slow carrier plus a small per-unit phase offset connects adjacent syllables.
        val phase = now.toDouble() * 2.0 * PI / 3600.0 + ordinal * 0.45
        val wave = sin(phase).toFloat()
        val breath = (0.5 + 0.5 * sin(phase - 0.6)).toFloat()
        val attack = lyricEase(elapsed / minOf(240f, duration * 0.3f).coerceAtLeast(1f))
        val floating = held * attack * release
        scale = (1f + 0.012f * expression + 0.012f * floating * breath).coerceIn(1f, 1.025f)
        liftDp = 0.9f * expression + 0.8f * floating * wave
    }
}
