package com.yohandeku32.nusamusic.ui

import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine
import com.yohandeku32.nusamusic.data.LyricsUnitKey

/** Visual weights only. Authored highlighting and the global wave phase never change. */
internal class LyricsAdaptiveExpression(durationMs: Long, val phraseStrength: Float = 1f) {
    private val duration = durationMs.coerceAtLeast(0).toFloat()
    val glow = curve(0f, 0.06f, 0.34f, 0.78f, 1f)
    val wave = curve(0f, 0.10f, 0.45f, 0.85f, 1f)
    val breathing = lyricEase((duration - 700f) / 1100f)
    val elastic = lyricEase((duration - 1000f) / 1600f)
    val vibration = lyricEase((duration - 1800f) / 800f)

    private fun curve(a: Float, b: Float, c: Float, d: Float, e: Float): Float = when {
        duration <= 250f -> a
        duration < 600f -> a + (b - a) * lyricEase((duration - 250f) / 350f)
        duration < 1000f -> b + (c - b) * lyricEase((duration - 600f) / 400f)
        duration < 1800f -> c + (d - c) * lyricEase((duration - 1000f) / 800f)
        else -> d + (e - d) * lyricEase((duration - 1800f) / 800f)
    }

    /** No compressed full cycle on fast notes; attack/release consume the short window. */
    fun envelope(start: Int, end: Int, now: Int): Float {
        val length = end.toLong() - start
        if (length <= 0 || now <= start || now >= end) return 0f
        val available = length.toFloat()
        val attack = minOf(320f, available * 0.5f).coerceAtLeast(0.5f)
        val release = minOf(340f, available * 0.5f).coerceAtLeast(0.5f)
        return lyricEase((now.toLong() - start) / attack) *
            lyricEase((end.toLong() - now) / release)
    }

    /** Recover gently from a rapid phrase into a hold, sampled from the media clock. */
    fun phraseAt(start: Int, now: Int): Float = phraseStrength +
        (1f - phraseStrength) * lyricEase((now.toLong() - start) / 800f)
}

internal data class LyricsExpressionTiming(val start: Int, val end: Int, val reliableEnd: Boolean = true)

/** Prepared with measured layouts, never scanned or allocated on playback frames. */
internal fun lyricsAdaptiveExpressions(timings: List<LyricsExpressionTiming>): List<LyricsAdaptiveExpression> {
    val durations = timings.map { timing ->
        (timing.end.toLong() - timing.start).coerceAtLeast(0).let {
            // A container/song end is insufficient evidence of a sustained vowel.
            if (timing.reliableEnd) it else minOf(it, 600L)
        }
    }
    val strengths = FloatArray(timings.size) { 1f }
    var index = 0
    while (index < timings.size) {
        if (durations[index] >= 600L) { index++; continue }
        val first = index
        var last = first
        while (last + 1 < timings.size && durations[last + 1] < 600L &&
            timings[last + 1].start.toLong() - timings[last].end in 0L..160L
        ) last++
        val count = last - first + 1
        if (count >= 3) {
            val damping = 1f - 0.6f * lyricEase((count - 2) / 4f)
            for (i in first..last) strengths[i] = minOf(strengths[i], damping)
            if (last + 1 < timings.size &&
                timings[last + 1].start.toLong() - timings[last].end in 0L..160L
            ) strengths[last + 1] = minOf(strengths[last + 1], damping)
        }
        index = last + 1
    }
    return timings.indices.map { LyricsAdaptiveExpression(durations[it], strengths[it]) }
}

/** Main phrases may span rows; backing voices are prepared separately by the caller. */
internal fun lyricsAdaptiveLineExpressions(
    lines: List<KaraokeLine>, inferredEnds: Set<LyricsUnitKey>
): Map<KaraokeLine, List<LyricsAdaptiveExpression>> {
    val units = lines.flatMap { line -> line.syllables.filter { it.content.isNotBlank() } }
    val profiles = lyricsAdaptiveExpressions(units.map {
        LyricsExpressionTiming(it.start, it.end, LyricsUnitKey(it.start, it.content.trim()) !in inferredEnds)
    })
    var offset = 0
    return lines.associateWith { line ->
        val size = line.syllables.count { it.content.isNotBlank() }
        profiles.subList(offset, offset + size).also { offset += size }
    }
}
