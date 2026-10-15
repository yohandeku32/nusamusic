package com.yohandeku32.nusamusic.ui

import kotlin.math.abs

internal const val CD_CAROUSEL_VISIBLE_COUNT = 5

/** Start well inside a virtual list so two neighbours exist on either side. */
internal fun initialCdCarouselIndex(songIndex: Int, songCount: Int): Int {
    require(songCount > 0 && songIndex in 0 until songCount)
    if (songCount < CD_CAROUSEL_VISIBLE_COUNT) return songIndex
    return nearestCdCarouselIndex(songIndex, songCount, Int.MAX_VALUE / 2)
}

/** Find the nearest copy of a song without traversing/decode-loading whole cycles. */
internal fun nearestCdCarouselIndex(songIndex: Int, songCount: Int, aroundIndex: Int): Int {
    require(songCount > 0 && songIndex in 0 until songCount)
    if (songCount < CD_CAROUSEL_VISIBLE_COUNT) return songIndex
    val reference = aroundIndex.toLong().coerceIn(0L, Int.MAX_VALUE.toLong() - 1L)
    val base = reference - reference % songCount + songIndex
    return listOf(base - songCount, base, base + songCount)
        .filter { it in 0L until Int.MAX_VALUE.toLong() }
        .minBy { abs(it - reference) }
        .toInt()
}
