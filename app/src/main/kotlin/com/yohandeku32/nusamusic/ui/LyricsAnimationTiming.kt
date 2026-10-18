package com.yohandeku32.nusamusic.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import com.mocharealm.accompanist.lyrics.core.model.ISyncedLine
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/** Reset boundaries suppress onset/fade replay when opening, seeking, or returning from background. */
internal class LyricsMotionState(initial: Int) {
    var resetAt by mutableIntStateOf(initial)
        private set
    var revision by mutableIntStateOf(0)
        private set
    private var previous = initial
    fun reset(position: Int) { previous = position; resetAt = position; revision++ }
    fun update(position: Int) {
        if (position < previous || position.toLong() - previous > 350) reset(position)
        previous = position
    }
}

internal fun lyricEase(value: Float): Float {
    val t = value.coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}

internal fun lyricPresence(start: Int, end: Int, now: Int, resetAt: Int): Float {
    if (now < start || end <= start || (resetAt >= end && now >= end)) return 0f
    val elapsed = now.toLong() - start
    val remaining = end.toLong() - now
    if (remaining <= 0) return 1f - lyricEase((-remaining / 160f))
    return if (resetAt >= start) 1f else lyricEase(elapsed / minOf(120f, (end.toLong() - start) / 2f).coerceAtLeast(1f))
}

/** Breathing is slow and bounded; its period is derived from the authored/next-boundary interval. */
internal fun lyricBreathing(start: Int, end: Int, now: Int): Float {
    val duration = (end.toLong() - start).coerceAtLeast(1)
    val elapsed = (now.toLong() - start).coerceIn(0, duration)
    val cycles = (duration / 2600.0).coerceIn(1.0, 4.0)
    return (0.88 + 0.12 * sin(2.0 * PI * elapsed / duration * cycles)).toFloat()
}

internal fun lyricShimmer(start: Int, end: Int, now: Int): Float {
    val duration = end.toLong() - start
    if (duration < 900 || now <= start || now >= end) return 0f
    val phase = ((now.toLong() - start).toDouble() / duration).coerceIn(0.0, 1.0)
    return phase.toFloat()
}

internal fun countdownOpacity(gap: LyricsGap, now: Int): Float {
    if (now < gap.start || now >= gap.end) return 0f
    return lyricEase((now.toLong() - gap.start) / 120f) *
        lyricEase((gap.end.toLong() - now) / 160f) * countdownFollowOffset(gap, now)
}

/** Pre-position the already measured next line while the countdown fades, with no spacer row. */
internal fun countdownFollowOffset(gap: LyricsGap, now: Int): Float =
    1f - lyricEase((now.toLong() - (gap.end.toLong() - 440)) / 440f)

/** Union real timed units, preserving authored silence inside a backing/translated line. */
internal class LyricsVocalWindows(line: ISyncedLine) {
    private val windows = buildList<Pair<Int, Int>> {
        val intervals = if (line is KaraokeLine) line.syllables.filter { it.content.isNotBlank() }
            .map { it.start to it.end } else listOf(line.start to line.end)
        for ((start, end) in intervals.sortedBy { it.first }) {
            if (end <= start) continue
            val last = lastOrNull()
            if (last != null && start <= last.second) { removeAt(lastIndex); add(last.first to maxOf(last.second, end)) }
            else add(start to end)
        }
    }
    fun presence(now: Int, resetAt: Int): Float {
        var low = 0
        var high = windows.size
        while (low < high) {
            val middle = (low + high) / 2
            if (windows[middle].first <= now) low = middle + 1 else high = middle
        }
        val interval = windows.getOrNull(low - 1) ?: return 0f
        return lyricPresence(interval.first, interval.second, now, resetAt)
    }
}

/** Critically damped spring response evaluated from audio time: no wall-clock animator or repeated bounce. */
internal fun syllableExpression(start: Int, end: Int, now: Int, resetAt: Int): Float {
    if (end <= start || now < start || (resetAt >= end && now >= end)) return 0f
    val duration = end.toLong() - start
    fun response(t: Float): Float {
        val x = t.coerceIn(0f, 1f) * 7f
        return ((1f - (1f + x) * exp(-x)) / (1f - 8f * exp(-7f))).coerceIn(0f, 1f)
    }
    val attack = if (resetAt >= start) 1f else response((now.toLong() - start) / minOf(180f, duration * 0.4f).coerceAtLeast(1f))
    val release = if (now < end) 1f else 1f - response((now.toLong() - end) / 160f)
    return attack * release
}

/** A long held note gradually broadens the halo instead of restarting an onset effect. */
internal fun syllableBloom(start: Int, end: Int, now: Int): Float {
    val duration = end.toLong() - start
    if (duration <= 0) return 0f
    return lyricEase(((now.toLong() - start).toFloat() / duration).coerceIn(0f, 1f))
}
