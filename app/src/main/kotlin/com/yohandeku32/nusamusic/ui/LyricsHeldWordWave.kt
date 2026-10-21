package com.yohandeku32.nusamusic.ui

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

/** Only timestamped, reliably held vocals can animate a word. */
internal data class LyricsHeldWordUnit(
    val content: String,
    val startMs: Int,
    val endMs: Int,
    val pathStartDp: Float,
    val pathEndDp: Float,
    val reliableHold: Boolean = true
)

internal data class LyricsHeldWordPose(
    val liftDp: Float = 0f,
    val scaleY: Float = 1f,
    val rotationDegrees: Float = 0f,
    val intensity: Float = 0f
)

/**
 * Builds an independent local wave per held word from the AUTHOR'S syllable
 * timestamps. Earlier words never inherit a held final vowel's transform.
 *
 * Each measured text run stays intact; no 20px slices, glyph clipping,
 * per-frame animation job, relayout or per-character timing inference.
 * A slow bell-shaped crest travels across the measured word as the vowel
 * progresses. The amplitude eases in and out over the authored hold.
 */
internal class LyricsHeldWordWave(units: List<LyricsHeldWordUnit>) {
    private data class Word(
        val left: Float,
        val right: Float,
        val holdStartMs: Int,
        val holdEndMs: Int
    )

    private val words: Array<Word?> = arrayOfNulls(units.size)

    init {
        var index = 0
        while (index < units.size) {
            val first = index
            var last = first
            // The embedded metadata defines syllables, not word boundaries.
            // Whitespace in text OR a measured inter-run gap separates words.
            // A unit containing multiple words is not a safe wave target.
            while (last + 1 < units.size &&
                !units[last].content.lastOrNull().let { it?.isWhitespace() ?: true } &&
                !units[last + 1].content.firstOrNull().let { it?.isWhitespace() ?: true } &&
                units[last + 1].pathStartDp - units[last].pathEndDp <= 2.5f &&
                !units[last].content.trim().any { it.isWhitespace() } &&
                !units[last + 1].content.trim().any { it.isWhitespace() }
            ) last++

            val group = units.subList(first, last + 1)
            val includesMultipleWords = group.any {
                it.content.trim().any(Char::isWhitespace)
            }
            val held = if (includesMultipleWords) null else group
                .filter { it.reliableHold && it.endMs.toLong() - it.startMs >= 1_200L }
                .maxByOrNull { it.endMs.toLong() - it.startMs }
            if (held != null) {
                val wave = Word(
                    left = group.first().pathStartDp,
                    right = group.last().pathEndDp,
                    holdStartMs = held.startMs,
                    holdEndMs = held.endMs
                )
                for (unitIndex in first..last) words[unitIndex] = wave
            }
            index = last + 1
        }
    }

    fun pose(index: Int, playbackMs: Int, centerDp: Float): LyricsHeldWordPose {
        val word = words.getOrNull(index) ?: return LyricsHeldWordPose()
        val duration = (word.holdEndMs.toLong() - word.holdStartMs).toFloat()
        if (duration <= 0f || playbackMs <= word.holdStartMs ||
            playbackMs >= word.holdEndMs) return LyricsHeldWordPose()

        val elapsed = (playbackMs.toLong() - word.holdStartMs).toFloat()
        // The envelope is sampled directly from audio time: pause freezes it,
        // seek reanchors immediately, and no independent cycle resets.
        val riseMs = minOf(900f, duration * 0.30f).coerceAtLeast(1f)
        val fallMs = minOf(850f, duration * 0.27f).coerceAtLeast(1f)
        val envelope = lyricEase(elapsed / riseMs) *
            lyricEase((duration - elapsed) / fallMs)
        val progress = (elapsed / duration).coerceIn(0f, 1f)

        // A slow, broad mountain: its crest moves from left to right with the
        // held syllable, without turning every letter into a separate wave.
        val center = ((centerDp - word.left) /
            (word.right - word.left).coerceAtLeast(1f)).coerceIn(0f, 1f)
        val crest = 0.32f + 0.36f * lyricEase(progress)
        val distance = abs(center - crest)
        val mountain = 1f - lyricEase(distance / 0.75f)
        // Gentle breathing rather than a short, repetitive vibration.
        val breathing = (0.88 + 0.12 *
            sin(playbackMs.toDouble() * 2.0 * PI / 3_800.0)).toFloat()
        val strength = (envelope * mountain * breathing).coerceIn(0f, 1f)

        // <5dp total lift; a mild tilt conveys a moving arch while retaining
        // complete glyph shapes, kerning and diacritics.
        val slope = ((center - crest) / 0.75f).coerceIn(-1f, 1f)
        return LyricsHeldWordPose(
            liftDp = -4.5f * strength,
            scaleY = 1f + 0.012f * strength,
            rotationDegrees = 0.65f * strength * slope,
            intensity = strength
        )
    }
}
