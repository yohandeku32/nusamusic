package com.yohandeku32.nusamusic.ui

/**
 * All coordinates are pixels. The Now Playing row starts centered with a
 * 56dp Artist circle + 10dp gap + the navigation pill. Portal expansion
 * adds 48dp to the pill. Library keeps the entire group centered; Artist
 * hides its own circle and centers the pill alone.
 */
internal fun navigationPillLeftPx(
    originalLeft: Float,
    expansionPx: Float,
    profileWithGapPx: Float,
    reveal: Float,
    artistOnly: Boolean
): Float {
    val t = reveal.coerceIn(0f, 1f)
    val growth = expansionPx * t
    return originalLeft - growth / 2f -
        if (artistOnly) profileWithGapPx * t / 2f else 0f
}

internal fun navigationProfileLeftPx(
    originalLeft: Float,
    expansionPx: Float,
    reveal: Float
): Float = originalLeft - expansionPx * reveal.coerceIn(0f, 1f) / 2f
