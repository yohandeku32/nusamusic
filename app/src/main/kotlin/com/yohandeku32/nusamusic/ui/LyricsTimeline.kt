package com.yohandeku32.nusamusic.ui

import com.mocharealm.accompanist.lyrics.core.model.ISyncedLine
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine

internal data class LyricsGap(val start: Int, val end: Int, val nextLine: Int)
internal data class LyricsFrame(val anchor: Int, val active: Set<Int>, val gap: LyricsGap? = null)

/** Start-sorted lyrics can have overlapping, non-monotonic end times (duets/backgrounds). */
internal class LyricsTimeline(private val lines: List<ISyncedLine>) {
    private val starts = IntArray(lines.size) { lines[it].start }
    private val ends = IntArray(lines.size) { lines[it].end }
    private val maxEnds = IntArray((lines.size * 4).coerceAtLeast(1))
    private val main = lines.indices.filter { (lines[it] as? KaraokeLine)?.isAccompaniment != true }.toIntArray()
    private val anchors = if (main.isNotEmpty()) main else IntArray(lines.size) { it }
    // Union all voices so an overlapping/background singer never produces a false break.
    val gaps: List<LyricsGap> = buildList {
        val intervals = lines.flatMapIndexed { index, line ->
            if (line is KaraokeLine) line.syllables.filter { it.content.isNotBlank() }
                .map { Triple(it.start, it.end, index) }
            else listOf(Triple(line.start, line.end, index))
        }.sortedBy { it.first }
        var coveredUntil = 0
        for ((start, end, index) in intervals) {
            if (start.toLong() - coveredUntil >= 3000) add(LyricsGap(coveredUntil, start, index))
            coveredUntil = maxOf(coveredUntil, end)
        }
    }

    init {
        require(starts.indices.drop(1).all { starts[it - 1] <= starts[it] })
        if (lines.isNotEmpty()) build(1, 0, lines.lastIndex)
    }

    private fun build(node: Int, left: Int, right: Int): Int {
        maxEnds[node] = if (left == right) ends[left] else {
            val middle = (left + right) / 2
            maxOf(build(node * 2, left, middle), build(node * 2 + 1, middle + 1, right))
        }
        return maxEnds[node]
    }

    fun frame(position: Int): LyricsFrame {
        if (lines.isEmpty()) return LyricsFrame(-1, emptySet())
        val active = linkedSetOf<Int>()
        fun query(node: Int, left: Int, right: Int) {
            if (starts[left] > position || maxEnds[node] <= position) return
            if (left == right) { active += left; return }
            val middle = (left + right) / 2
            query(node * 2, left, middle)
            query(node * 2 + 1, middle + 1, right)
        }
        query(1, 0, lines.lastIndex)
        val activeMain = active.filter { (lines[it] as? KaraokeLine)?.isAccompaniment != true }
        val latestStart = activeMain.lastOrNull()?.let { starts[it] }
        val anchor = activeMain.firstOrNull { starts[it] == latestStart } ?: run {
            val candidates = anchors
            var low = 0
            var high = candidates.size
            while (low < high) {
                val middle = (low + high) / 2
                if (starts[candidates[middle]] <= position) low = middle + 1 else high = middle
            }
            val latest = starts[candidates[(low - 1).coerceAtLeast(0)]]
            // Keep the same first authored identity after a simultaneous duet finishes.
            var first = 0
            var last = low.coerceAtLeast(1)
            while (first < last) {
                val middle = (first + last) / 2
                if (starts[candidates[middle]] < latest) first = middle + 1 else last = middle
            }
            candidates[first]
        }
        var low = 0
        var high = gaps.size
        while (low < high) {
            val middle = (low + high) / 2
            if (gaps[middle].start <= position) low = middle + 1 else high = middle
        }
        val gap = gaps.getOrNull(low - 1)?.takeIf { position < it.end }
        return LyricsFrame(anchor, active, gap)
    }
}
