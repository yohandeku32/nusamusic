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
    /** Projected motion toward the viewer. Whole glyph runs stay intact. */
    val depth: Float = 0f,
    val parallaxXDp: Float = 0f,
    /** Signed slope of the projected surface; mirrored across the crest. */
    val perspectiveSlope: Float = 0f,
    val rotationDegrees: Float = 0f,
    val intensity: Float = 0f,
    /** Measured word-path location of the moving mountain's luminous crest. */
    val crestDp: Float = 0f
)

/**
 * Builds an independent local wave per held word from the AUTHOR'S syllable
 * timestamps. Earlier words never inherit a held final vowel's transform.
 *
 * Each measured text run stays intact; no 20px slices, glyph clipping,
 * per-frame animation job, relayout or per-character timing inference.
 * A visible, rounded mountain crest rises, crosses the measured word,
 * and falls over the authored hold. Adjacent syllables trace the same arc
 * with different heights and tangents; rapid words stay flat. The entire
 * shaped syllable remains intact to protect kerning and diacritics.
 */
/** C2-continuous rise/fall; exactly zero slope at the start, crest and end. */
internal fun heldWordEase(value: Float): Float {
    val t = value.coerceIn(0f, 1f)
    return t * t * t * (t * (6f * t - 15f) + 10f)
}

/** Smoothstep is gentler through the middle than quintic on the size release. */
internal fun heldWordSizeRelease(value: Float): Float {
    val t = value.coerceIn(0f, 1f)
    return 1f - t * t * (3f - 2f * t)
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
                .filter { it.reliableHold && it.endMs.toLong() - it.startMs >= 1_300L }
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

        // The old glow looked like a straight horizontal sweep: its crest
        // crossed only 40% of a word and the whole word lifted under 4.5dp.
        // The new crest traverses 64%, giving every syllable a DIFFERENT
        // height. An arch now reads as a mountain rather than a flat wave.
        val center = ((centerDp - word.left) /
            (word.right - word.left).coerceAtLeast(1f)).coerceIn(0f, 1f)
        val crest = 0.18f + 0.64f * heldWordEase(progress)
        val distance = abs(center - crest)
        val mountain = 1f - heldWordEase(distance / 0.65f)

        // The previous implementation required a 2,000ms individual unit
        // and multiplied TWO duration gates together. Most real sustained
        // syllables never reached visible depth. Engage reliably from a
        // 1,300ms authored hold; only long notes reach the full projection.
        val holdWeight = heldWordEase((duration - 1_000f) / 1_500f)
        val strength = (envelope * mountain * holdWeight).coerceIn(0f, 1f)

        // Spatial crest still travels for the arch and perspective tilt.
        // The FONT SIZE must not follow the passing crest: previously the
        // mountain multiplied depth, so each glyph rapidly contracted as
        // the crest passed it, even when the singer kept holding the note.
        // A spatially fixed depth profile and a single authored-time size
        // envelope give a truly continuous big -> small return.
        val tangent = ((center - crest) / 0.65f).coerceIn(-1f, 1f)
        val sizeEnvelope = if (progress <= peakAt) {
            heldWordEase(progress / peakAt)
        } else {
            // An extended, critically smooth 58% release. No frame jobs,
            // bounce, hard clamp, or discontinuity at the vocal's end.
            heldWordSizeRelease((progress - peakAt) / (1f - peakAt))
        }
        val centeredDistance = abs(center - 0.5f)
        val depthShape = 0.72f + 0.28f *
            (1f - heldWordEase(centeredDistance / 0.65f))
        val depth = (holdWeight * sizeEnvelope * depthShape).coerceIn(0f, 1f)
        return LyricsHeldWordPose(
            liftDp = -7.5f * strength,
            scaleY = 1f + 0.065f * depth,
            depth = depth,
            parallaxXDp = -2.7f * tangent * depth,
            perspectiveSlope = tangent,
            rotationDegrees = -2.2f * tangent * strength,
            intensity = strength,
            crestDp = word.left + (word.right - word.left) * crest
        )
    }
}
