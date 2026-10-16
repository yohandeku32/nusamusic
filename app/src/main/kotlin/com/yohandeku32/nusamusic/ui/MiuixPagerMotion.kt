package com.yohandeku32.nusamusic.ui

internal data class MiuixPagerVisuals(
    val translationX: Float,
    val alpha: Float,
    val scrimAlpha: Float,
    val leadingCornerFraction: Float
)

/** Compensate the native full-page translation to produce quarter-page parallax. */
internal fun miuixPagerVisuals(relativeDepth: Float, widthPx: Float, isRtl: Boolean): MiuixPagerVisuals {
    val depth = relativeDepth.takeIf { it.isFinite() } ?: 0f
    val width = widthPx.takeIf { it.isFinite() && it > 0f } ?: 0f
    val covered = depth.coerceIn(0f, 1f)
    return MiuixPagerVisuals(
        translationX = covered * width * 0.75f * if (isRtl) -1f else 1f,
        alpha = 1f - covered * 0.1f,
        scrimAlpha = covered * 0.5f,
        leadingCornerFraction = (-depth).coerceIn(0f, 1f)
    )
}
