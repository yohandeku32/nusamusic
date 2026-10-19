package com.yohandeku32.nusamusic.ui

internal data class MiuixPagerVisuals(
    val translationX: Float,
    val alpha: Float,
    val scrimAlpha: Float,
    val leadingCornerFraction: Float,
    val scale: Float
)

/** Compensate the native full-page translation to produce quarter-page parallax. */
internal fun miuixPagerVisuals(relativeDepth: Float, widthPx: Float, isRtl: Boolean): MiuixPagerVisuals {
    val depth = relativeDepth.takeIf { it.isFinite() } ?: 0f
    val width = widthPx.takeIf { it.isFinite() && it > 0f } ?: 0f
    val covered = depth.coerceIn(0f, 1f)
    // Quintic smoothstep avoids a rigid linear slide without overshooting.
    val t = covered * covered * covered *
        (covered * (covered * 6f - 15f) + 10f)
    return MiuixPagerVisuals(
        // The covered page drifts just 15% of the viewport, producing depth.
        translationX = t * width * 0.85f * if (isRtl) -1f else 1f,
        alpha = 1f - t * 0.025f,
        scrimAlpha = t * 0.16f,
        leadingCornerFraction = (-depth).coerceIn(0f, 1f),
        scale = 1f - t * 0.038f
    )
}
