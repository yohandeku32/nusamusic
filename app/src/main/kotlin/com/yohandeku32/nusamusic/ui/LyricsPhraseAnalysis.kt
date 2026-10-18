package com.yohandeku32.nusamusic.ui

/**
 * Bounded bidirectional context, prepared once from authored units. Continuous
 * duration/gap weights replace hard short-note run thresholds. Gaps and
 * overlapping voices break the chain instead of implying sustained vocals.
 */
internal class LyricsPhraseAnalysis(timings: List<LyricsExpressionTiming>) {
    private val durations = LongArray(timings.size) { i ->
        val unit = timings[i]
        val duration = (unit.end.toLong() - unit.start).coerceAtLeast(0)
        if (unit.reliableEnd) duration else minOf(duration, 600L)
    }
    private val fast = FloatArray(timings.size) { i ->
        1f - lyricEase((durations[i] - 250L) / 550f)
    }
    private val links = FloatArray((timings.size - 1).coerceAtLeast(0)) { i ->
        val gap = timings[i + 1].start.toLong() - timings[i].end
        if (gap < 0) 0f else 1f - lyricEase(gap / 450f)
    }

    val expressions = timings.indices.map { index ->
        var fastContext = 0f
        var upcomingHold = 0f
        for (direction in listOf(-1, 1)) {
            var proximity = 1f
            for (distance in 1..3) {
                val neighbour = index + direction * distance
                if (neighbour !in timings.indices) break
                val link = if (direction > 0) neighbour - 1 else neighbour
                proximity *= links[link]
                if (proximity <= 0f) break
                fastContext += fast[neighbour] * proximity
                if (direction > 0) {
                    val timeAhead = (timings[neighbour].start.toLong() - timings[index].end).coerceAtLeast(0)
                    val reach = 1f - lyricEase(timeAhead / 1200f)
                    val held = lyricEase((durations[neighbour] - 700L) / 1100f)
                    upcomingHold = maxOf(upcomingHold, held * proximity * reach)
                }
                proximity *= 0.7f
            }
        }
        val strength = 1f - 0.6f * lyricEase(fastContext / 2f)
        LyricsAdaptiveExpression(durations[index], strength, upcomingHold,
            timings.getOrNull(index + 1)?.start)
    }
}
