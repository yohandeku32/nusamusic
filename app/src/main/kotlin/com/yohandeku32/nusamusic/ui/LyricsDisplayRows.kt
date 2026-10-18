package com.yohandeku32.nusamusic.ui

import com.mocharealm.accompanist.lyrics.core.model.ISyncedLine
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine

internal data class LyricsDisplayRow(val main: ISyncedLine?, val sourceIndex: Int, val backing: List<KaraokeLine>)

/** One lazy item per main line: backing expands below it without moving the main anchor. */
internal class LyricsDisplayRows(lines: List<ISyncedLine>) {
    val rows: List<LyricsDisplayRow>
    val lineToRow = IntArray(lines.size)
    init {
        val mains = lines.indices.filter { !(lines[it] as? KaraokeLine)?.isAccompaniment.orFalse() }
        if (mains.isEmpty()) {
            rows = if (lines.isEmpty()) emptyList() else listOf(LyricsDisplayRow(null, 0, lines.filterIsInstance<KaraokeLine>()))
        } else {
            val backing = List(mains.size) { ArrayList<KaraokeLine>() }
            lines.forEachIndexed { index, line ->
                // A preceding main onset owns simultaneous and overlapping backing voices.
                var low = 0
                var high = mains.size
                while (low < high) {
                    val middle = (low + high) / 2
                    if (lines[mains[middle]].start <= line.start) low = middle + 1 else high = middle
                }
                val owner = if (line is KaraokeLine && line.isAccompaniment) (low - 1).coerceAtLeast(0)
                    else mains.binarySearch(index)
                lineToRow[index] = owner
                if (line is KaraokeLine && line.isAccompaniment) backing[owner] += line
            }
            rows = mains.mapIndexed { row, index -> LyricsDisplayRow(lines[index], index, backing[row]) }
        }
    }
    private fun Boolean?.orFalse() = this ?: false
}

/** Collapse the complete measured child, including its padding, to exactly zero. */
internal fun vocalLayoutHeight(fullHeight: Int, presence: Float): Int =
    (fullHeight * presence.coerceIn(0f, 1f)).toInt().coerceIn(0, fullHeight)
