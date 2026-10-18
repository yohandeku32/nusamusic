package com.yohandeku32.nusamusic.ui

import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine
import com.yohandeku32.nusamusic.data.LyricsUnitKey

/** Visual weights only. Authored highlighting and the global wave phase never change. */
internal class LyricsAdaptiveExpression(
    durationMs: Long,
    val phraseStrength: Float = 1f,
    val upcomingHold: Float = 0f,
    val nextStartMs: Int? = null
) {
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
        val attack = minOf(320f + 140f * (1f - phraseStrength), available * 0.5f).coerceAtLeast(0.5f)
        val release = minOf(340f + 100f * breathing, available * 0.5f).coerceAtLeast(0.5f)
        return lyricEase((now.toLong() - start) / attack) *
            lyricEase((end.toLong() - now) / release)
    }

    /** Recover gently from a rapid phrase into a hold, sampled from the media clock. */
    fun phraseAt(start: Int, now: Int): Float {
        val elapsed = (now.toLong() - start).toFloat()
        val recovery = lyricEase(elapsed / 800f)
        // Look ahead only shapes the CURRENT note's bounded amplitude. It
        // never changes a future note's fill or starts its motion early.
        val approach = 0.18f * upcomingHold *
            lyricEase((elapsed / duration.coerceAtLeast(1f) - 0.45f) / 0.55f)
        return phraseStrength + (1f - phraseStrength) * (recovery + (1f - recovery) * approach)
    }

    /** Keep a soft outgoing halo across a nearby onset, restricted to sung text. */
    fun glowConnection(end: Int): Float {
        val next = nextStartMs ?: return 0f
        val gap = next.toLong() - end
        // Retain only soft light at the boundary; full bloom releases before
        // the note ends instead of being squeezed into a fast outgoing tail.
        return if (gap >= 0) 0.6f * (1f - lyricEase(gap / 240f)) else 0f
    }

    fun glowEnvelope(start: Int, end: Int, now: Int, connection: Float = glowConnection(end)): Float {
        val length = end.toLong() - start
        if (length <= 0 || now <= start) return 0f
        val releaseMs = minOf(160f, length * 0.4f).coerceAtLeast(1f)
        val outgoing = 1f - lyricEase((now.toLong() - end) / releaseMs)
        if (now >= end) return connection * outgoing
        val inside = envelope(start, end, now)
        val attackMs = minOf(320f + 140f * (1f - phraseStrength), length * 0.5f).coerceAtLeast(0.5f)
        val sustained = lyricEase((now.toLong() - start) / attackMs)
        return inside + (sustained - inside) * connection
    }
}

internal data class LyricsExpressionTiming(val start: Int, val end: Int, val reliableEnd: Boolean = true)

/** Prepared with measured layouts, never scanned or allocated on playback frames. */
internal fun lyricsAdaptiveExpressions(timings: List<LyricsExpressionTiming>): List<LyricsAdaptiveExpression> =
    LyricsPhraseAnalysis(timings).expressions

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
