package com.yohandeku32.nusamusic.ui

import kotlin.math.abs

/** Blur and opacity applied together by a lyric graphics layer. */
internal data class LyricsFocusDepth(val blurDp: Float, val opacity: Float)

/**
 * Retained backing-vocal profile: the viewport centre is the focus plane.
 * This continuous, two-stage curve has no per-line index switches:
 * - in focus (within 24dp): sharp / opaque
 * - next visual row (~105dp): 1.2dp blur / 76% opacity
 * - distant visual row (~250dp): 2.7dp blur / 48% opacity
 *
 * Evaluated in a graphics layer from measured LazyList offsets, so manual
 * scrolling, active-line tracking and seek scrolling share one focus plane.
 * Kept independent from the stronger main-text profile below.
 */
internal fun lyricsFocusDepth(distanceDp: Float): LyricsFocusDepth {
    val distance = abs(distanceDp)
    if (!distance.isFinite()) return LyricsFocusDepth(0f, 1f)
    if (distance <= 24f) return LyricsFocusDepth(0f, 1f)
    if (distance <= 105f) {
        val fraction = lyricEase((distance - 24f) / 81f)
        return LyricsFocusDepth(
            blurDp = 1.2f * fraction,
            opacity = 1f - 0.24f * fraction
        )
    }
    val fraction = lyricEase((distance - 105f) / 145f)
    return LyricsFocusDepth(
        blurDp = 1.2f + 1.5f * fraction,
        opacity = 0.76f - 0.28f * fraction
    )
}

/** Main text only: stronger depth, with the existing backing-vocal curve above retained. */
internal fun lyricsMainFocusDepth(
    distanceDp: Float, start: Int, end: Int, now: Int,
    focusAmount: Float = lyricsMainFocusAmount(start, end, now)
): LyricsFocusDepth {
    // Finish the incoming focus pull BEFORE the first active syllable. All
    // overlapping active lines stay completely sharp, even during browsing.
    // Sample the existing playback clock: pause holds focus, and seek never
    // restarts a stale animator or replays a focus transition from another row.
    val focus = focusAmount.coerceIn(0f, 1f)
    if (focus >= 1f) return LyricsFocusDepth(0f, 1f)
    val distance = abs(distanceDp).takeIf { it.isFinite() } ?: 0f
    val nearToFar = lyricEase((distance - 105f) / 105f)
    val farToFarthest = lyricEase((distance - 210f) / 105f)
    val blur = 2.5f + 2f * nearToFar + farToFarthest
    val opacity = 0.70f - 0.22f * nearToFar - 0.13f * farToFarthest
    return LyricsFocusDepth(blur * (1f - focus), opacity + (1f - opacity) * focus)
}

/** One playback-time smoothstep for blur AND opacity; the default retains the 420ms profile. */
internal fun lyricsMainFocusAmount(
    start: Int, end: Int, now: Int, leadMs: Float = 420f, releaseMs: Float = 420f
): Float {
    if (end <= start) return 0f
    return when {
        now < start -> 1f - lyricEase((start.toLong() - now) / leadMs.coerceAtLeast(1f))
        now < end -> 1f
        else -> 1f - lyricEase((now.toLong() - end) / releaseMs.coerceAtLeast(1f))
    }
}
