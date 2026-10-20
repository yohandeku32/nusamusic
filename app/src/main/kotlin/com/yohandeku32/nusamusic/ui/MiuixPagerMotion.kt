package com.yohandeku32.nusamusic.ui

import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.round

/**
 * Navigation-card parameters only. The real page movement MUST remain owned
 * by HorizontalPager; compensating its translation detaches visual positions
 * from Compose hit targets and can send a Now Playing tap to an Artist page.
 *
 * Pages use their native physical X positions, remain fully opaque, and never
 * scale. Only the front card gets rounded leading corners and a subtle shadow
 * as it slides over the previous page.
 */
internal data class MiuixCardVisuals(
    val cornerRadiusDp: Float,
    val shadowElevationDp: Float,
    val edgeShadeAlpha: Float
)

private val idleCardVisuals = MiuixCardVisuals(0f, 0f, 0f)

/** Increasing page indices form the navigation stack (Artist < Player < Library).
 * The higher page always sits above the lower page, on either swipe direction.
 * Keeping this order constant avoids input-layer swaps during slow dragging.
 */
internal fun miuixPageZIndex(page: Int): Float = page.toFloat()

internal fun miuixCardVisuals(
    page: Int, pagerPosition: Float, scrolling: Boolean
): MiuixCardVisuals {
    if (!scrolling || !pagerPosition.isFinite() || page <= 0) return idleCardVisuals

    // For 0 < position < 1, Player (1) is the front card. For
    // 1 < position < 2, Library (2) is the front card, both directions.
    val frontPage = ceil(pagerPosition.toDouble()).toInt().coerceAtLeast(1)
    if (page != frontPage) return idleCardVisuals

    val distanceFromRest = abs(pagerPosition - round(pagerPosition))
        .coerceIn(0f, 0.5f)
    // Ramp the visual accent only during the first 9% of travel; a drag
    // reversed before touch-slop returns to an undisturbed flat screen.
    val fraction = (distanceFromRest / 0.09f).coerceIn(0f, 1f)
    val eased = fraction * fraction * (3f - 2f * fraction)
    return MiuixCardVisuals(
        cornerRadiusDp = 16f,
        shadowElevationDp = 9f * eased,
        edgeShadeAlpha = 0.19f * eased
    )
}

internal fun miuixPageAcceptsInput(
    page: Int, settledPage: Int, currentPage: Int,
    offsetFraction: Float, scrolling: Boolean
): Boolean = !scrolling && page == settledPage && page == currentPage &&
    offsetFraction.isFinite() && kotlin.math.abs(offsetFraction) < 0.001f
