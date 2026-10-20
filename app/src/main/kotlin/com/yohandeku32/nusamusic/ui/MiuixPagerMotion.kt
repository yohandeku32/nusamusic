package com.yohandeku32.nusamusic.ui

import kotlin.math.abs

internal data class MiuixPagerVisuals(
    val translationX: Float,
    val alpha: Float,
    val scrimAlpha: Float,
    val leadingCornerFraction: Float,
    val scale: Float
)

/**
 * Soft reveal instead of a horizontal slide.
 *
 * HorizontalPager still owns touch handling and accessibility, but we cancel
 * 97% of its automatic page translation. Pages then drift only ~3% of the
 * screen width as their content softly dissolves into each other.
 *
 * Distance-squared opacity keeps the combined layers bright at the midpoint,
 * preventing the noticeable dim flash of a conventional two-way 50% fade.
 * No overshoot, zoom, corner morph, scrims or extra per-frame blur work.
 */
internal fun miuixPagerVisuals(
    relativeDepth: Float, widthPx: Float, isRtl: Boolean
): MiuixPagerVisuals {
    val depth = relativeDepth.takeIf { it.isFinite() } ?: 0f
    val width = widthPx.takeIf { it.isFinite() && it > 0f } ?: 0f
    val normalizedDepth = depth.coerceIn(-1f, 1f)
    val distance = abs(normalizedDepth)
    val opacity = (1f - distance * distance).coerceIn(0f, 1f)
    return MiuixPagerVisuals(
        translationX = normalizedDepth * width * 0.97f * if (isRtl) -1f else 1f,
        alpha = opacity,
        scrimAlpha = 0f,
        leadingCornerFraction = 0f,
        scale = 1f
    )
}
