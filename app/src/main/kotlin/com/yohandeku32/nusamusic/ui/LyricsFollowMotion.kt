package com.yohandeku32.nusamusic.ui

import kotlin.math.abs
import kotlin.math.ceil

/** High-damping scroll spring; its frame deltas are separate from the audio-time glyph effects. */
internal class LyricsFollowMotion {
    private var velocity = 0f
    fun reset() { velocity = 0f }
    fun step(distance: Float, deltaMs: Long): Float {
        if (deltaMs <= 0) return 0f
        val seconds = minOf(deltaMs, 350) / 1000f
        val steps = ceil(seconds * 120).toInt().coerceAtLeast(1)
        val dt = seconds / steps
        var error = distance
        var movement = 0f
        repeat(steps) {
            velocity += (196f * error - 2f * 0.92f * 14f * velocity) * dt
            val step = velocity * dt
            movement += step
            error -= step
        }
        if (abs(error) < 0.25f && abs(velocity) < 0.5f) { reset(); return distance }
        return movement
    }
}
