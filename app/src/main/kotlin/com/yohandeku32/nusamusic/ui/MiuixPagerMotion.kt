package com.yohandeku32.nusamusic.ui

import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.round

/**
 * The pager itself provides the 36dp overlap and native movement, while this
 * model controls only the front card's clip/shadow. Nothing moves hit regions
 * with a graphicsLayer translation, crossfade or scaling transform.
 */
internal data class MiuixCardVisuals(
    val cornerRadiusDp: Float,
    val shadowElevationDp: Float,
    val edgeShadeAlpha: Float
)

private val idleCardVisuals = MiuixCardVisuals(0f, 0f, 0f)

// Artist = 0, Now Playing = 1, Library = 2.
// Fixed stack order works for both swipes and gesture reversals.
internal fun miuixPageZIndex(page: Int): Float = page.toFloat()

internal fun miuixCardVisuals(
    page: Int, pagerPosition: Float, scrolling: Boolean
): MiuixCardVisuals {
    if (!scrolling || !pagerPosition.isFinite() || page <= 0)
        return idleCardVisuals

    val frontPage = ceil(pagerPosition.toDouble()).toInt().coerceAtLeast(1)
    if (page != frontPage) return idleCardVisuals

    val distanceFromRest = abs(pagerPosition - round(pagerPosition))
        .coerceIn(0f, 0.5f)
    val progress = (distanceFromRest / 0.085f).coerceIn(0f, 1f)
    val eased = progress * progress * (3f - 2f * progress)
    return MiuixCardVisuals(
        cornerRadiusDp = 26f,
        shadowElevationDp = 10f * eased,
        edgeShadeAlpha = 0.16f * eased
    )
}

internal fun miuixPageAcceptsInput(
    page: Int, settledPage: Int, currentPage: Int,
    offsetFraction: Float, scrolling: Boolean
): Boolean = !scrolling && page == settledPage && page == currentPage &&
    offsetFraction.isFinite() && abs(offsetFraction) < 0.001f
