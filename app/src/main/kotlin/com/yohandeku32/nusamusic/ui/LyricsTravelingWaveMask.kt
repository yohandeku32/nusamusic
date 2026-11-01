package com.yohandeku32.nusamusic.ui

import kotlin.math.abs

/**
 * Measured vocal path in lyric-reading order. Positions are calculated once
 * from the existing SyllableLayout glyph bounds, not from assumed word widths.
 * The mask follows the sung portion of the text, independently of the V6 wave.
 */
internal data class LyricsVocalSpan(
    val startMs: Int,
    val endMs: Int,
    val startDp: Float,
    val endDp: Float
)

internal data class LyricsVocalFront(
    val positionDp: Float,
    val intensity: Float
)

internal class LyricsTravelingWaveMask(spans: List<LyricsVocalSpan>) {
    private val timed = spans
        .filter { it.endMs > it.startMs && it.endDp > it.startDp }
        .sortedWith(compareBy<LyricsVocalSpan> { it.startMs }.thenBy { it.endMs })

    // One shared entrance for a continuous phrase, not one per syllable.
    private val phraseStarts = buildList {
        var farthestEnd = Long.MIN_VALUE
        for (span in timed) {
            if (farthestEnd == Long.MIN_VALUE ||
                span.startMs.toLong() - farthestEnd > 260L) add(span.startMs)
            farthestEnd = maxOf(farthestEnd, span.endMs.toLong())
        }
    }

    /**
     * C1-smooth wave on a whole SHAPED WORD. All its syllables share a center
     * and rise together; the crest moves continuously across adjacent words.
     * It never restarts per letter, per syllable, or at a normal word boundary.
     */
    fun coherentInfluence(front: LyricsVocalFront?, wordCenterDp: Float, playbackMs: Int): Float {
        if (front == null || front.intensity <= 0f) return 0f
        val onset = phraseStarts.lastOrNull { it <= playbackMs } ?: return 0f
        val entrance = lyricEase((playbackMs.toLong() - onset) / 260f)
        val delta = wordCenterDp - front.positionDp
        // Soft, wide leading shoulder and slightly longer trailing descent.
        val rangeDp = if (delta >= 0f) 60f else 92f
        val distance = (abs(delta) / rangeDp).coerceIn(0f, 1f)
        return (entrance * front.intensity * (1f - lyricEase(distance)))
            .coerceIn(0f, 1f)
    }

    /**
     * One playhead for the whole row. The timestamp is the same media clock
     * that drives syllableProgress(), not a time since syllable activation.
     * Holding / seeking changes the sampled position but never the wave phase.
     */
    fun front(playbackMs: Int): LyricsVocalFront? {
        var active: LyricsVocalSpan? = null
        var previous: LyricsVocalSpan? = null

        for (span in timed) {
            if (span.startMs > playbackMs) break
            if (playbackMs < span.endMs) {
                // Latest authored onset wins when vocal windows overlap.
                active = span
            } else if (previous == null || span.endMs >= previous.endMs) {
                previous = span
            }
        }

        active?.let { span ->
            val elapsed = playbackMs.toLong() - span.startMs
            val progress = (elapsed.toFloat() /
                (span.endMs.toLong() - span.startMs).toFloat()).coerceIn(0f, 1f)
            // At a contiguous syllable boundary the font contains actual
            // inter-word whitespace. Ease the playhead through that measured
            // gap instead of jumping from one word's end to the next start.
            // The phase itself remains entirely independent of this offset.
            val contiguous = previous?.takeIf {
                span.startMs.toLong() - it.endMs.toLong() in 0L..120L &&
                    it.endDp <= span.startDp
            }
            val startPosition = contiguous?.let {
                val bridge = lyricEase(elapsed.toFloat() / 100f)
                it.endDp + (span.startDp - it.endDp) * bridge
            } ?: span.startDp
            return LyricsVocalFront(
                positionDp = startPosition + (span.endDp - span.startDp) * progress,
                intensity = 1f
            )
        }

        // Hold the front briefly after a note, so its last visible pixels
        // relax to baseline instead of snapping off in a vocal gap.
        previous?.let { span ->
            val afterMs = playbackMs.toLong() - span.endMs
            if (afterMs in 0L..260L) {
                return LyricsVocalFront(
                    positionDp = span.endDp,
                    intensity = 1f - lyricEase(afterMs / 260f)
                )
            }
        }
        return null
    }

    /**
     * Asymmetric, C1-smooth spatial falloff: zero *ahead* of the highlight;
     * strongest just behind the vocal front; trails off over ~100 dp.
     * Sampling a measured visual slice, instead of its entire word, leaves
     * un-sung characters in long syllables completely stationary.
     */
    fun influence(
        front: LyricsVocalFront?,
        sliceStartDp: Float,
        sliceEndDp: Float
    ): Float {
        if (front == null || front.intensity <= 0f ||
            sliceEndDp <= sliceStartDp ||
            front.positionDp <= sliceStartDp
        ) return 0f

        // Ease the influence over a short physical distance. Ramping across
        // half a narrow strip would compress an entire attack into one frame
        // when a syllable is fast, causing a visible twitch at 60 Hz.
        val peak = sliceStartDp + 18f
        val position = front.positionDp
        val spatial = if (position < peak) {
            lyricEase((position - sliceStartDp) / 18f)
        } else {
            1f - lyricEase((position - peak) / 100f)
        }
        return (front.intensity * spatial).coerceIn(0f, 1f)
    }
    /**
     * One smooth influence value per intact rendered syllable.
     *
     * The V7 playhead still travels with the sung text, but individual
     * characters inside the same shaped text run never get conflicting
     * translations or clipping edges. The wave appears gently at the
     * syllable's start, stays alive through a held vowel, then recedes
     * after the vocal front passes its end. Future syllables stay still.
     *
     * Only this mask changes; the global V6 sine carrier is untouched.
     */
    fun glyphInfluence(
        front: LyricsVocalFront?,
        glyphStartDp: Float,
        glyphEndDp: Float
    ): Float {
        if (front == null || front.intensity <= 0f ||
            glyphEndDp <= glyphStartDp ||
            front.positionDp <= glyphStartDp
        ) return 0f

        val position = front.positionDp
        val attack = lyricEase((position - glyphStartDp) / 18f)
        val release = 1f - lyricEase((position - glyphEndDp) / 100f)
        return (front.intensity * attack * release).coerceIn(0f, 1f)
    }

}

/** Same pivot for all the shaped syllables making up a word. */
internal fun lyricCoherentWordCenters(units: List<LyricsHeldWordUnit>): FloatArray {
    val centers = FloatArray(units.size)
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
                next.pathStartDp - current.pathEndDp > 2.5f) break
            index++
        }
        val center = (units[first].pathStartDp + units[index].pathEndDp) * 0.5f
        for (part in first..index) centers[part] = center
        index++
    }
    return centers
}


/**
 * Independent, shallow motion for each authored word. It never uses the
 * sentence-wide travelling front: neighbouring words only move when their
 * own timed vocal window starts. Syllables belonging to the SAME word share
 * one envelope, protecting letter spacing and preventing a staccato bounce.
 *
 * The lift rises smoothly and releases over 200 ms after the word ends.
 * Playback time is the sole clock, so pause/seek cannot restart an animator.
 */
internal class LyricsTimedWordPulse(units: List<LyricsHeldWordUnit>) {
    private data class Word(val startMs: Int, val endMs: Int)
    private val byUnit = arrayOfNulls<Word>(units.size)

    init {
        var i = 0
        while (i < units.size) {
            val first = i
            while (i + 1 < units.size) {
                val current = units[i]
                val next = units[i + 1]
                if (current.content.lastOrNull()?.isWhitespace() != false ||
                    next.content.firstOrNull()?.isWhitespace() != false ||
                    current.content.trim().any(Char::isWhitespace) ||
                    next.content.trim().any(Char::isWhitespace) ||
                    next.pathStartDp - current.pathEndDp > 2.5f
                ) break
                i++
            }
            val group = units.subList(first, i + 1)
            val start = group.minOf { it.startMs }
            val end = group.maxOf { it.endMs }
            if (end > start) {
                val word = Word(start, end)
                for (part in first..i) byUnit[part] = word
            }
            i++
        }
    }

    fun influence(unitIndex: Int, playbackMs: Int): Float {
        val word = byUnit.getOrNull(unitIndex) ?: return 0f
        if (playbackMs <= word.startMs ||
            playbackMs.toLong() >= word.endMs.toLong() + 200L) return 0f
        val duration = (word.endMs.toLong() - word.startMs).toFloat()
        val riseMs = (duration * 0.55f).coerceIn(95f, 180f)
        val rise = heldWordEase((playbackMs.toLong() - word.startMs) / riseMs)
        val release = if (playbackMs < word.endMs) 1f
            else 1f - heldWordEase((playbackMs.toLong() - word.endMs) / 200f)
        return (rise * release).coerceIn(0f, 1f)
    }
}
