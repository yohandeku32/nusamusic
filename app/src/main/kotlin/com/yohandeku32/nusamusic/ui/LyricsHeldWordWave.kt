package com.yohandeku32.nusamusic.ui

import kotlin.math.abs

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
/** C2-continuous rise/fall; exactly zero slope at the start, crest and end. */
internal fun heldWordEase(value: Float): Float {
    val t = value.coerceIn(0f, 1f)
    return t * t * t * (t * (6f * t - 15f) + 10f)
}

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
        val progress = (elapsed / duration).coerceIn(0f, 1f)

        // Spend the WHOLE authored hold on one unhurried rise and descent,
        // rather than attacking in 900ms and dropping in the last 850ms.
        // The peak arrives around 42%; the remaining 58% is a slow release.
        // Quintic easing makes acceleration, slope and the final return to
        // baseline continuous, including the final frame before the next lyric.
        val peakAt = 0.42f
        val envelope = if (progress <= peakAt) {
            heldWordEase(progress / peakAt)
        } else {
            1f - heldWordEase((progress - peakAt) / (1f - peakAt))
        }

        // One broad mountain moving from left to right through the authored
        // word. Adjacent syllables feel like a connected arch, not independent
        // quick bounces. Do not split or clip the font's shaped text run.
        val center = ((centerDp - word.left) /
            (word.right - word.left).coerceAtLeast(1f)).coerceIn(0f, 1f)
        val crest = 0.30f + 0.40f * heldWordEase(progress)
        val distance = abs(center - crest)
        val mountain = 1f - heldWordEase(distance / 0.82f)

        // A 1.2-second unit cannot carry a full 4.5dp mountain gracefully.
        // Longer actual holds grow into the full expression continuously.
        val holdWeight = 0.32f + 0.68f *
            heldWordEase((duration - 1_200f) / 1_600f)
        val strength = (envelope * mountain * holdWeight).coerceIn(0f, 1f)

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
