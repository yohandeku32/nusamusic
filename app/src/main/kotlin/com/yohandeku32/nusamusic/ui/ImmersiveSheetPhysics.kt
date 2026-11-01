package com.yohandeku32.nusamusic.ui

/**
 * Place the edge-to-edge Lyrics sheet just below the safe top inset.
 * Settings is now in the bottom pill, so no measured top-right control is
 * required. The offset is device-specific and never a screen-height ratio.
 */
internal fun immersiveLyricsSheetTopPx(
    safeTopInsetPx: Int,
    viewportHeightPx: Int,
    clearancePx: Int
): Int {
    val viewport = viewportHeightPx.coerceAtLeast(0)
    return (safeTopInsetPx.coerceAtLeast(0).toLong() +
        clearancePx.coerceAtLeast(0).toLong()).coerceIn(0L, viewport.toLong()).toInt()
}

/**
 * Positive velocity is downward (Android pixels/second).
 * A decisive downward fling dismisses even when only slightly dragged.
 * A decisive upward flick snaps back; release position handles slow drags.
 */
internal fun shouldDismissImmersiveLyricsSheet(
    openedToClosedFraction: Float,
    releaseVelocityPxPerSecond: Float,
    flingThresholdPxPerSecond: Float
): Boolean {
    if (releaseVelocityPxPerSecond < -flingThresholdPxPerSecond) return false
    if (releaseVelocityPxPerSecond > flingThresholdPxPerSecond) return true
    return openedToClosedFraction.coerceIn(0f, 1f) >= 0.28f
}
