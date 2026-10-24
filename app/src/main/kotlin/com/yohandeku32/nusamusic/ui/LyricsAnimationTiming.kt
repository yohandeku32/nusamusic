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
    fun reset(position: Int) { resetAt = position; revision++ }
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

/** A continuous 3.4-second glow breath; short units never cram in a full fast cycle. */
internal fun lyricBreathing(start: Int, end: Int, now: Int): Float {
    if (end <= start) return 1f
    val sample = now.coerceIn(start, end)
    return (0.94 + 0.06 * sin(sample.toDouble() * 2.0 * PI / 3400.0)).toFloat()
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

/** Collapse countdown space and return the following text to its normal anchor before vocals resume. */
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
    companion object {
        /** Preview begins 700ms before a backing vocal actually sings. */
        const val BACKING_PREVIEW_MS = 700
        private const val BACKING_RELEASE_MS = 380
    }

    /**
     * Visual pre-roll only. The lyric text is visible before its first
     * syllable, but karaoke fills still use the original authored time.
     * This is a continuous envelope through the exact vocal onset (1 -> 1)
     * and through gaps / final release, with no extra time shifting.
     */
    fun previewPresence(now: Int, resetAt: Int): Float {
        if (windows.isEmpty()) return 0f
        var low = 0
        var high = windows.size
        while (low < high) {
            val mid = (low + high) / 2
            if (windows[mid].first <= now) low = mid + 1 else high = mid
        }
        fun windowOpacity(window: Pair<Int, Int>): Float {
            val (start, end) = window
            if (now.toLong() < start.toLong() - BACKING_PREVIEW_MS ||
                now.toLong() >= end.toLong() + BACKING_RELEASE_MS ||
                (resetAt >= end && now >= end)) return 0f
            if (now < start) return lyricEase(
                ((now.toLong() - start.toLong() + BACKING_PREVIEW_MS) /
                    BACKING_PREVIEW_MS.toFloat()).coerceIn(0f, 1f)
            )
            if (now < end) return 1f
            return 1f - lyricEase(
                ((now.toLong() - end.toLong()) /
                    BACKING_RELEASE_MS.toFloat()).coerceIn(0f, 1f)
            )
        }
        return maxOf(
            windows.getOrNull(low - 2)?.let(::windowOpacity) ?: 0f,
            windows.getOrNull(low - 1)?.let(::windowOpacity) ?: 0f,
            windows.getOrNull(low)?.let(::windowOpacity) ?: 0f
        )
    }

    fun presence(now: Int, resetAt: Int): Float {
        var low = 0
        var high = windows.size
        while (low < high) {
            val middle = (low + high) / 2
            if (windows[middle].first <= now) low = middle + 1 else high = middle
        }
        // Soft 220-420ms attack and 380ms release for backing-vocal rows.
        // Adjacent authored syllables overlap naturally instead of popping.
        fun smooth(interval: Pair<Int, Int>): Float {
            val (start, end) = interval
            if (now < start || now.toLong() >= end.toLong() + 380L ||
                (resetAt >= end && now >= end)) return 0f
            val duration = (end.toLong() - start).toFloat()
            val attackMs = (duration * 0.40f).coerceIn(220f, 420f)
            val attack = if (resetAt >= start) 1f
                else lyricEase((now.toLong() - start) / attackMs)
            val release = if (now < end) 1f
                else 1f - lyricEase((now.toLong() - end) / 380f)
            return (attack * release).coerceIn(0f, 1f)
        }
        val current = windows.getOrNull(low - 1)?.let(::smooth) ?: 0f
        val prior = windows.getOrNull(low - 2)?.let(::smooth) ?: 0f
        return maxOf(current, prior)
    }
}

/** A subtle but visible continuous wave, including ordinary fast words. */
internal fun lyricPassingLiftDp(influence: Float): Float =
    -1.8f * lyricEase(influence.coerceIn(0f, 1f))

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
