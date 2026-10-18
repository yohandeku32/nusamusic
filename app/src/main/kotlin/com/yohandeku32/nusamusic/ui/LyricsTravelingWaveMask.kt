package com.yohandeku32.nusamusic.ui

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
            val progress = ((playbackMs.toLong() - span.startMs).toFloat() /
                (span.endMs.toLong() - span.startMs).toFloat()).coerceIn(0f, 1f)
            return LyricsVocalFront(
                positionDp = span.startDp + (span.endDp - span.startDp) * progress,
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

        val centre = (sliceStartDp + sliceEndDp) / 2f
        val position = front.positionDp
        val spatial = if (position < centre) {
            lyricEase((position - sliceStartDp) / (centre - sliceStartDp).coerceAtLeast(0.001f))
        } else {
            1f - lyricEase((position - centre) / 100f)
        }
        return (front.intensity * spatial).coerceIn(0f, 1f)
    }
}
