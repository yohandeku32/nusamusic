package com.yohandeku32.nusamusic.ui

import com.mocharealm.accompanist.lyrics.core.model.ISyncedLine
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine

/**
 * Timestamp-only line planning, independent from syllable activation. A single
 * scroll spring follows this continuous target, and blur samples the same
 * authored line cues. Backing voices cannot pull focus backwards or forwards.
 */
internal class LyricsVisualFocusController(private val lines: List<ISyncedLine>) {
    private val main = lines.indices.filter { (lines[it] as? KaraokeLine)?.isAccompaniment != true }
    private val sources = (if (main.isNotEmpty()) main else lines.indices.toList())
        .distinctBy { lines[it].start }.toIntArray()
    private val lead = FloatArray(sources.size) { i ->
        if (i == 0) 420f else minOf(420f,
            (lines[sources[i]].start.toLong() - lines[sources[i - 1]].start) * 0.35f).coerceAtLeast(1f)
    }
    private val lag = FloatArray(sources.size) { i ->
        val interval = sources.getOrNull(i + 1)?.let { lines[it].start.toLong() - lines[sources[i]].start }
        val gap = if (i == 0) 0L else
            (lines[sources[i]].start.toLong() - lines[sources[i - 1]].end).coerceAtLeast(0)
        // Countdown already follows the incoming row during a long break.
        // Complete its transition by onset so rejoining cannot pull backwards.
        minOf(180f, (interval ?: 720L) * 0.25f).coerceAtLeast(1f) *
            (1f - lyricEase(gap / 600f))
    }
    private val focusLead = FloatArray(lines.size) { 420f }
    private val focusRelease = FloatArray(lines.size) { 420f }
    private val response = FloatArray(sources.size) { i ->
        val interval = sources.getOrNull(i + 1)?.let { lines[it].start.toLong() - lines[sources[i]].start }
            ?: (lines[sources[i]].end.toLong() - lines[sources[i]].start)
        0.78f + 0.22f * lyricEase((interval - 250L) / 1800f)
    }

    init {
        val cueByStart = sources.indices.associateBy { lines[sources[it]].start }
        for (i in lines.indices) {
            val cue = cueByStart[lines[i].start] ?: continue
            focusLead[i] = (lead[cue] * (0.55f / 0.35f)).coerceIn(240f, 420f)
            val next = sources.getOrNull(cue + 1)
            focusRelease[i] = if (next == null) 420f else
                ((lines[next].start.toLong() - lines[i].start) * 0.6f).coerceIn(240f, 420f)
        }
    }

    var fromSource = -1
        private set
    var toSource = -1
        private set
    var blend = 0f
        private set
    var responsiveness = 1f
        private set

    fun update(playbackMs: Int) {
        if (sources.isEmpty()) return
        var low = 0
        var high = sources.size
        while (low < high) {
            val middle = (low + high) / 2
            if (lines[sources[middle]].start <= playbackMs) low = middle + 1 else high = middle
        }
        val current = (low - 1).coerceAtLeast(0)
        val incoming = when {
            current > 0 && playbackMs.toLong() < lines[sources[current]].start.toLong() + lag[current] -> current
            current + 1 < sources.size && playbackMs.toLong() > lines[sources[current + 1]].start.toLong() - lead[current + 1] -> current + 1
            else -> -1
        }
        if (incoming >= 0) {
            fromSource = sources[incoming - 1]
            toSource = sources[incoming]
            val transitionStart = lines[toSource].start.toLong() - lead[incoming]
            blend = lyricEase((playbackMs.toLong() - transitionStart) / (lead[incoming] + lag[incoming]))
            responsiveness = response[incoming - 1] +
                (response[incoming] - response[incoming - 1]) * blend
        } else {
            fromSource = sources[current]
            toSource = fromSource
            blend = 0f
            responsiveness = response[current]
        }
    }

    fun focusAmount(source: Int, playbackMs: Int): Float {
        val line = lines.getOrNull(source) ?: return 0f
        return lyricsMainFocusAmount(line.start, line.end, playbackMs, focusLead[source], focusRelease[source])
    }
}
