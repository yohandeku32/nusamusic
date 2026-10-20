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
 * MIUIX-style layered glide, not a stationary cross-fade or a full-page slide.
 *
 * The underlying HorizontalPager still owns drag/fling physics and accessibility.
 * Cancel 76% of its native translation, leaving 24% real physical movement
 * under the finger. A restrained 1.6% depth scale makes each page feel layered.
 * Incoming content draws above the departing page while the gesture is active.
 *
 * The eased opacity keeps the midpoint bright and both endpoints gentle;
 * no black scrim, corner morph, overshoot or additional artwork blur work.
 */
internal fun miuixPagerVisuals(
    relativeDepth: Float, widthPx: Float, isRtl: Boolean
): MiuixPagerVisuals {
    val depth = relativeDepth.takeIf { it.isFinite() } ?: 0f
    val width = widthPx.takeIf { it.isFinite() && it > 0f } ?: 0f
    val normalizedDepth = depth.coerceIn(-1f, 1f)
    val distance = abs(normalizedDepth)
    // Zero slope at both ends avoids a sudden appearance/disappearance when
    // the gesture starts or the settling spring reaches its destination.
    val easedDistance = distance * distance * (3f - 2f * distance)
    val opacity = (1f - easedDistance * easedDistance).coerceIn(0f, 1f)
    return MiuixPagerVisuals(
        translationX = normalizedDepth * width * 0.76f * if (isRtl) -1f else 1f,
        alpha = opacity,
        scrimAlpha = 0f,
        leadingCornerFraction = 0f,
        scale = 1f - 0.016f * easedDistance
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
