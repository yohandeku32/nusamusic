package com.yohandeku32.nusamusic.ui

internal data class MiuixPagerVisuals(
    val translationX: Float,
    val alpha: Float,
    val scrimAlpha: Float,
    val leadingCornerFraction: Float,
    val scale: Float
)

/**
 * Preserve Android's native page slide, with just 5% parallax on the page
 * being covered. No easing layered over the user's finger, no scaling,
 * fading, shadows or rounded-corner morphing between pages.
 */
internal fun miuixPagerVisuals(
    relativeDepth: Float, widthPx: Float, isRtl: Boolean
): MiuixPagerVisuals {
    val depth = relativeDepth.takeIf { it.isFinite() } ?: 0f
    val width = widthPx.takeIf { it.isFinite() && it > 0f } ?: 0f
    val covered = depth.coerceIn(0f, 1f)
    return MiuixPagerVisuals(
        translationX = covered * width * 0.05f * if (isRtl) -1f else 1f,
        alpha = 1f,
        scrimAlpha = 0f,
        leadingCornerFraction = 0f,
        scale = 1f
    )
}
