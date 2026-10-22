package com.yohandeku32.nusamusic.ui

import kotlin.math.abs
import kotlin.math.ceil

/** Lift only the first vocal anchor by 12.5% of the viewport, including after seeking back. */
internal fun lyricsFollowAnchor(
    viewportStart: Int, viewportEnd: Int, firstVocal: Boolean,
    mainHeight: Float = 0f, minimumPadding: Float = 0f
): Float {
    val height = (viewportEnd.toLong() - viewportStart).coerceAtLeast(0).toFloat()
    val offset = if (firstVocal) maxOf(height * 0.375f, mainHeight / 2f + minimumPadding)
        else height * 0.5f
    return viewportStart + offset
}

/** High-damping scroll spring; its frame deltas are separate from the audio-time glyph effects. */
internal class LyricsFollowMotion {
    private var velocity = 0f
    fun reset() { velocity = 0f }
    fun step(distance: Float, deltaMs: Long, responsiveness: Float = 1f): Float {
        if (deltaMs <= 0) return 0f
        val seconds = minOf(deltaMs, 350) / 1000f
        val steps = ceil(seconds * 120).toInt().coerceAtLeast(1)
        val dt = seconds / steps
        val frequency = 14f * responsiveness.coerceIn(0.78f, 1f)
        var error = distance
        var movement = 0f
        repeat(steps) {
            velocity += (frequency * frequency * error - 2f * 0.92f * frequency * velocity) * dt
            val step = velocity * dt
            movement += step
            error -= step
        }
        if (abs(error) < 0.25f && abs(velocity) < 0.5f) { reset(); return distance }
        return movement
    }
}

/**
 * A settled lyric list must not briefly reverse while audio time is moving
 * forward. The spring may otherwise correct a one-pixel overshoot by scrolling
 * backwards, which looks like an extra karaoke loop at 90/120 Hz.
 *
 * New instance per follow session (seek/manual-resume): backward rejoining
 * is intentionally allowed until the actual playback row is centered.
 */
internal class LyricsFollowDirectionGuard {
    private var settled = false

    fun markSettled() {
        settled = true
    }

    fun isReverseCorrection(error: Float): Boolean = settled && error < -0.5f

    fun step(requested: Float, error: Float): Float {
        if (abs(error) <= 0.5f) {
            settled = true
            return 0f
        }
        if (isReverseCorrection(error)) return 0f
        // The spring carries velocity from the preceding frame. Never let
        // that stale velocity move beyond or away from the current anchor.
        if (requested * error <= 0f) return 0f
        return requested.coerceIn(minOf(0f, error), maxOf(0f, error))
    }
}
