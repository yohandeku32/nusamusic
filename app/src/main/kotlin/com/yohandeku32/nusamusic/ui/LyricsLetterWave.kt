package com.yohandeku32.nusamusic.ui

import java.text.BreakIterator
import java.util.Locale

/**
 * Every visible letter follows its own gentle crest, delayed by its measured
 * position within the word. Unlike a per-letter bounce, this is one single
 * pass: C2-smoothed rise, C2-smoothed fall, and no oscillator or loop.
 *
 * All values derive from Media3 playback milliseconds. Pause holds the pose;
 * seeking samples the correct pose immediately instead of replaying a tween.
 */
internal class LyricsLetterWave(units: List<LyricsHeldWordUnit>) {
    private data class Word(
        val start: Int,
        val end: Int,
        val leftDp: Float,
        val rightDp: Float
    )

    private val byUnit = arrayOfNulls<Word>(units.size)

    init {
        var index = 0
        while (index < units.size) {
            val first = index
            while (index + 1 < units.size) {
                val current = units[index]
                val next = units[index + 1]
                if (current.content.lastOrNull()?.isWhitespace() != false ||
                    next.content.firstOrNull()?.isWhitespace() != false ||
                    current.content.trim().any(Char::isWhitespace) ||
                    next.content.trim().any(Char::isWhitespace) ||
                    next.pathStartDp - current.pathEndDp > 2.5f
                ) break
                index++
            }
            val group = units.subList(first, index + 1)
            val start = group.minOf { it.startMs }
            val end = group.maxOf { it.endMs }
            if (end > start) {
                val word = Word(
                    start = start,
                    end = end,
                    leftDp = group.first().pathStartDp,
                    rightDp = group.last().pathEndDp
                )
                for (part in first..index) byUnit[part] = word
            }
            index++
        }
    }

    /** A completed letter never springs up again during the same word. */
    fun liftDp(unitIndex: Int, letterCenterDp: Float, nowMs: Int): Float {
        val word = byUnit.getOrNull(unitIndex) ?: return 0f
        val duration = (word.end.toLong() - word.start).toFloat()
        val fraction = ((letterCenterDp - word.leftDp) /
            (word.rightDp - word.leftDp).coerceAtLeast(1f)).coerceIn(0f, 1f)

        // Spread letter peaks over the word, but never rush through a 180ms
        // lyric like separate 30ms jumps. The tail can overlap the next word.
        val travelMs = (duration * 0.55f).coerceIn(240f, 640f)
        val onset = word.start.toDouble() + fraction * travelMs
        val elapsed = (nowMs - onset).toFloat()
        if (elapsed <= 0f || elapsed >= 410f) return 0f

        // Slow acceleration/deceleration has zero velocity at both ends.
        // 160ms up, 250ms down, max 1.55dp: softer, with enough movement to remain perceptible.
        val rise = heldWordEase(elapsed / 160f)
        val fall = 1f - heldWordEase((elapsed - 160f) / 250f)
        return -1.55f * rise * fall
    }
}

/**
 * Preserve Unicode grapheme clusters when separating letters. A base vowel
 * and combining accent must rise together, never as two detached glyphs.
 * Returned indices match UTF-16 offsets used by TextLayoutResult.
 */
internal fun lyricGraphemeRanges(text: String): List<IntRange> {
    val breaker = BreakIterator.getCharacterInstance(Locale.ROOT)
    breaker.setText(text)
    val ranges = ArrayList<IntRange>()
    var start = breaker.first()
    var end = breaker.next()
    while (end != BreakIterator.DONE) {
        if (end > start) ranges += start until end
        start = end
        end = breaker.next()
    }
    return ranges
}
