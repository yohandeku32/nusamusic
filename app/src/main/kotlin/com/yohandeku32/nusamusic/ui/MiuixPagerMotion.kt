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
 * MIUIX-style reveal with most of the transition spent moving, not fading.
 *
 * The native pager still owns drag and fling physics. Compensate only 42%
 * of its translation, so the page glides 58% of the viewport under the finger.
 * Keep both pages opaque through the middle 60% of the gesture: unlike a
 * full-screen crossfade, this avoids blending two heavy artwork/video scenes
 * on every frame. Fade only at the edges to conceal resting neighbours.
 *
 * Depth is limited to a 1% GPU transform. No overscroll, scrim, dynamic
 * corner mask, live blur or extra bitmap decoding while swiping.
 */
internal fun miuixPagerVisuals(
    relativeDepth: Float, widthPx: Float, isRtl: Boolean
): MiuixPagerVisuals {
    val depth = relativeDepth.takeIf { it.isFinite() } ?: 0f
    val width = widthPx.takeIf { it.isFinite() && it > 0f } ?: 0f
    val normalizedDepth = depth.coerceIn(-1f, 1f)
    val distance = abs(normalizedDepth)
    val easedDistance = distance * distance * (3f - 2f * distance)
    // Edge-only dissolve. At mid-swipe BOTH surfaces are opaque so the
    // incoming surface covers the outgoing one without costly pixel blending.
    val reveal = ((1f - distance) / 0.20f).coerceIn(0f, 1f)
    val opacity = reveal * reveal * (3f - 2f * reveal)
    return MiuixPagerVisuals(
        translationX = normalizedDepth * width * 0.42f * if (isRtl) -1f else 1f,
        alpha = opacity,
        scrimAlpha = 0f,
        leadingCornerFraction = 0f,
        scale = 1f - 0.010f * easedDistance
    )
}

internal fun miuixPageAcceptsInput(
    page: Int, settledPage: Int, currentPage: Int,
    offsetFraction: Float, scrolling: Boolean
): Boolean = !scrolling && page == settledPage && page == currentPage &&
    offsetFraction.isFinite() && kotlin.math.abs(offsetFraction) < 0.001f

/** During a swipe the incoming layer is on top; at rest the settled page owns
 * the visible surface. Hit testing is separately guarded by pagerPageInput. */
internal fun miuixPageZIndex(page: Int, settledPage: Int, scrolling: Boolean): Float =
    if (scrolling) {
        if (page == settledPage) 0f else 1f
    } else {
        if (page == settledPage) 1f else 0f
    }
