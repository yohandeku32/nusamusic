package com.yohandeku32.nusamusic.ui

import kotlin.math.abs

/** Depth of field for the VISIBLE position of a lyric row, in dp. */
internal data class LyricsFocusDepth(val blurDp: Float, val opacity: Float)

/**
 * The centre of the lyrics viewport is the visual focus plane.
 * This continuous, two-stage curve has no per-line index switches:
 * - in focus (within 24dp): sharp / opaque
 * - next visual row (~105dp): 1.2dp blur / 76% opacity
 * - distant visual row (~250dp): 2.7dp blur / 48% opacity
 *
 * Evaluated in a graphics layer from measured LazyList offsets, so manual
 * scrolling, active-line tracking and seek scrolling share one focus plane.
 * Only the row layer changes; the underlying syllable wave stays untouched.
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
