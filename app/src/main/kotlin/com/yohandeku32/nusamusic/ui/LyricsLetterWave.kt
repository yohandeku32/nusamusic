package com.yohandeku32.nusamusic.ui

import java.text.BreakIterator
import java.util.Locale

internal const val LYRICS_DETAIL_MIN_MS = 650L

/**
 * A C2-smoothed crest travelling through character centers in authored word time.
 * Connected scripts use wordLiftDp to retain their contextual shaping.
 *
 * All values derive from Media3 playback milliseconds. Pause holds the pose;
 * seeking samples the correct pose immediately instead of replaying a tween.
 */
internal class LyricsLetterWave(units: List<LyricsHeldWordUnit>) {
    private data class Word(
        val start: Int,
        val end: Int,
        val leftDp: Float,
        val rightDp: Float,
        val amplitudeDp: Float,
        val travelMs: Float,
        var cycleMs: Float,
        val sustained: Boolean,
        var motionEndMs: Long = end.toLong()
    )

    private val byUnit = arrayOfNulls<Word>(units.size)

    /** Layout eligibility stays fixed while the playback clock advances. */
    fun hasMotion(unitIndex: Int): Boolean = byUnit.getOrNull(unitIndex) != null

    init {
        val groups = ArrayList<Word?>()
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
            val duration = end.toLong() - start
            var prepared: Word? = null
            if (duration > LYRICS_DETAIL_MIN_MS &&
                group.all { it.reliableHold } &&
                group.none { it.content.trim().any(Char::isWhitespace) }) {
                val travel = (duration * 0.45f).coerceIn(240f, 640f)
                val sustained = group.any { it.endMs.toLong() - it.startMs >= 1_300L }
                val word = Word(
                    start = start,
                    end = end,
                    leftDp = group.first().pathStartDp,
                    rightDp = group.last().pathEndDp,
                    amplitudeDp = 0.85f * heldWordEase((duration - LYRICS_DETAIL_MIN_MS) / 550f)
                        .coerceIn(0f, 1f),
                    travelMs = travel,
                    cycleMs = if (sustained) minOf(410f, duration - travel) else duration - travel,
                    sustained = sustained
                )
                for (part in first..index) byUnit[part] = word
                prepared = word
            }
            groups += prepared
            index++
        }
        for (index in 0 until groups.lastIndex) {
            val outgoing = groups[index] ?: continue
            val incoming = groups[index + 1] ?: continue
            if (outgoing.sustained || incoming.sustained) continue
            val gap = incoming.start.toLong() - outgoing.end
            if (gap !in 0L..120L) continue
            // Only motion overlaps. The next word's authored fill/onset is
            // unchanged, and rapid/inferred words break this connection.
            val handoffMs = gap + 180L
            outgoing.cycleMs += handoffMs.toFloat()
            outgoing.motionEndMs += handoffMs
        }
    }

    /** One offset for every shaped run in a word, including halo and fill. */
    fun wordLiftDp(unitIndex: Int, nowMs: Int): Float {
        val word = byUnit.getOrNull(unitIndex) ?: return 0f
        return liftDp(unitIndex, (word.leftDp + word.rightDp) * 0.5f, nowMs)
    }

    fun isAnimating(unitIndex: Int, nowMs: Int): Boolean {
        val word = byUnit.getOrNull(unitIndex) ?: return false
        return nowMs > word.start && nowMs.toLong() < word.motionEndMs &&
            nowMs.toDouble() < word.start.toDouble() + word.travelMs + word.cycleMs
    }

    /** A completed letter never springs up again during the same word. */
    fun liftDp(unitIndex: Int, letterCenterDp: Float, nowMs: Int): Float {
        val word = byUnit.getOrNull(unitIndex) ?: return 0f
        if (nowMs <= word.start || nowMs.toLong() >= word.motionEndMs) return 0f
        val fraction = ((letterCenterDp - word.leftDp) /
            (word.rightDp - word.leftDp).coerceAtLeast(1f)).coerceIn(0f, 1f)

        val onset = word.start.toDouble() + fraction * word.travelMs
        val elapsed = (nowMs - onset).toFloat()
        if (elapsed <= 0f || elapsed >= word.cycleMs) return 0f

        // A single smooth release may overlap the next nearby short pull.
        val attack = word.cycleMs * 0.39f
        val rise = heldWordEase(elapsed / attack)
        val fall = 1f - heldWordEase((elapsed - attack) / (word.cycleMs - attack))
        return -word.amplitudeDp * (rise * fall).coerceIn(0f, 1f)
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

/** Joining scripts and emoji sequences must retain their original shaped run. */
internal fun lyricIndependentLetterRanges(text: String): List<IntRange> {
    val supported = text.codePoints().allMatch { code ->
        val type = Character.getType(code)
        when {
            type == Character.NON_SPACING_MARK.toInt() ||
                type == Character.COMBINING_SPACING_MARK.toInt() -> true
            Character.isLetter(code) -> Character.UnicodeScript.of(code) in setOf(
                Character.UnicodeScript.LATIN, Character.UnicodeScript.GREEK,
                Character.UnicodeScript.CYRILLIC, Character.UnicodeScript.HAN,
                Character.UnicodeScript.HIRAGANA, Character.UnicodeScript.KATAKANA,
                Character.UnicodeScript.HANGUL
            )
            else -> Character.isDigit(code) || Character.isWhitespace(code) || type in setOf(
                Character.CONNECTOR_PUNCTUATION.toInt(), Character.DASH_PUNCTUATION.toInt(),
                Character.START_PUNCTUATION.toInt(), Character.END_PUNCTUATION.toInt(),
                Character.INITIAL_QUOTE_PUNCTUATION.toInt(), Character.FINAL_QUOTE_PUNCTUATION.toInt(),
                Character.OTHER_PUNCTUATION.toInt()
            )
        }
    }
    return if (supported) lyricGraphemeRanges(text) else emptyList()
}
