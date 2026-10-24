package com.yohandeku32.nusamusic.data

/**
 * Walk the REAL playback order, using Media3 Timeline's shuffle-aware
 * getNextWindowIndex instead of incrementing library indices.
 *
 * A queue shorter than five tracks stops at the end (or after visiting all
 * distinct indices when repeat-all wraps); never warm the current song.
 */
internal fun upcomingPlaybackIndices(
    currentIndex: Int,
    itemCount: Int,
    limit: Int = 5,
    nextIndex: (Int) -> Int
): List<Int> {
    if (currentIndex !in 0 until itemCount || limit <= 0) return emptyList()
    val result = ArrayList<Int>(minOf(itemCount - 1, limit))
    val visited = hashSetOf(currentIndex)
    var cursor = currentIndex
    repeat(limit) {
        val next = nextIndex(cursor)
        if (next !in 0 until itemCount || !visited.add(next)) return result
        result.add(next)
        cursor = next
    }
    return result
}
