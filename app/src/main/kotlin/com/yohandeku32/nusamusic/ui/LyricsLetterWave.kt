package com.yohandeku32.nusamusic.ui

import java.text.BreakIterator
import java.util.Locale
import kotlin.math.abs

internal const val LYRICS_DETAIL_MIN_MS = 650L

/**
 * A C2-smoothed crest travelling through character centers in authored word time.
 * Connected scripts use wordLiftDp to retain their contextual shaping.
 *
 * All values derive from Media3 playback milliseconds. Pause holds the pose;
 * seeking samples the correct pose immediately instead of replaying a tween.
 */
internal class LyricsLetterWave(units: List<LyricsHeldWordUnit>, joinedGapDp: Float = 2.5f) {
    private data class Word(
        val start: Int,
        val end: Int,
        val leftDp: Float,
        val rightDp: Float,
        val amplitudeDp: Float,
        val travelMs: Float,
        val cycleMs: Float,
        val sustained: Boolean,
        var flow: Flow? = null
    )

    private class Flow(words: List<Word>) {
        val start = words.first().start
        val end = words.last().end
        private val radiusDp = (words.map { it.rightDp - it.leftDp }.average().toFloat() * 0.6f)
            .coerceIn(24f, 64f)
        private val amplitudeDp = words.map { it.amplitudeDp }.average().toFloat()
        private val path = LyricsWordFlow(buildList {
            for ((index, word) in words.withIndex()) {
                val previous = words.getOrNull(index - 1)
                val next = words.getOrNull(index + 1)
                val left = when {
                    previous == null -> word.leftDp
                    previous.end == word.start -> (previous.rightDp + word.leftDp) * 0.5f
                    else -> word.leftDp
                }
                val right = when {
                    next == null -> word.rightDp
                    word.end == next.start -> (word.rightDp + next.leftDp) * 0.5f
                    else -> word.rightDp
                }
                if (lastOrNull()?.first != word.start) add(word.start to left)
                add(word.end to right)
            }
        })

        fun isAnimating(word: Word, now: Int): Boolean = now > start && now < end &&
            now.toLong() > word.start.toLong() - 180L && now.toLong() < word.end.toLong() + 180L

        fun liftDp(word: Word, centerDp: Float, now: Int): Float {
            if (!isAnimating(word, now)) return 0f
            val hill = 1f - heldWordEase(abs(centerDp - path.sample(now)) / radiusDp)
            val arrival = heldWordEase((now.toLong() - word.start + 180L) / 180f)
            val release = 1f - heldWordEase((now.toLong() - word.end) / 180f)
            val enterFlow = heldWordEase((now.toLong() - start) / 180f)
            val exitFlow = 1f - heldWordEase((now.toLong() - end + 180L) / 180f)
            return -amplitudeDp * hill * arrival * release * enterFlow * exitFlow
        }
    }

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
                if (lyricUnitHasCjk(current.content) || lyricUnitHasCjk(next.content) ||
                    current.content.lastOrNull()?.isWhitespace() != false ||
                    next.content.firstOrNull()?.isWhitespace() != false ||
                    current.content.trim().any(Char::isWhitespace) ||
                    next.content.trim().any(Char::isWhitespace) ||
                    next.pathStartDp - current.pathEndDp > joinedGapDp
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
        var first = 0
        while (first < groups.size) {
            val initial = groups[first]
            if (initial == null || initial.sustained) { first++; continue }
            var last = first
            while (last + 1 < groups.size) {
                val outgoing = groups[last] ?: break
                val incoming = groups[last + 1] ?: break
                if (incoming.sustained || incoming.start.toLong() - outgoing.end !in 0L..120L) break
                last++
            }
            if (last > first) {
                val connected = groups.subList(first, last + 1).filterNotNull()
                val flow = Flow(connected)
                connected.forEach { it.flow = flow }
            }
            first = last + 1
        }
    }

    /** One offset for every shaped run in a word, including halo and fill. */
    fun wordLiftDp(unitIndex: Int, nowMs: Int): Float {
        val word = byUnit.getOrNull(unitIndex) ?: return 0f
        return liftDp(unitIndex, (word.leftDp + word.rightDp) * 0.5f, nowMs)
    }

    fun isAnimating(unitIndex: Int, nowMs: Int): Boolean {
        val word = byUnit.getOrNull(unitIndex) ?: return false
        word.flow?.let { return it.isAnimating(word, nowMs) }
        return nowMs > word.start && nowMs < word.end &&
            nowMs.toDouble() < word.start.toDouble() + word.travelMs + word.cycleMs
    }

    /** A completed letter never springs up again during the same word. */
    fun liftDp(unitIndex: Int, letterCenterDp: Float, nowMs: Int): Float {
        val word = byUnit.getOrNull(unitIndex) ?: return 0f
        word.flow?.let { return it.liftDp(word, letterCenterDp, nowMs) }
        if (nowMs <= word.start || nowMs >= word.end) return 0f
        val fraction = ((letterCenterDp - word.leftDp) /
            (word.rightDp - word.leftDp).coerceAtLeast(1f)).coerceIn(0f, 1f)

        val onset = word.start.toDouble() + fraction * word.travelMs
        val elapsed = (nowMs - onset).toFloat()
        if (elapsed <= 0f || elapsed >= word.cycleMs) return 0f

        // Isolated words keep their existing character crest.
        val attack = word.cycleMs * 0.39f
        val rise = heldWordEase(elapsed / attack)
        val fall = 1f - heldWordEase((elapsed - attack) / (word.cycleMs - attack))
        return -word.amplitudeDp * (rise * fall).coerceIn(0f, 1f)
    }
}

/** A monotone text-path cursor with shared velocity and zero acceleration at word joins. */
internal class LyricsWordFlow(points: List<Pair<Int, Float>>) {
    private val times = points.map { it.first }.toIntArray()
    private val positions = points.map { it.second }.toFloatArray()
    private val durations = FloatArray(points.size - 1) { (times[it + 1].toLong() - times[it]).toFloat() }
    private val slopes = FloatArray(points.size)
    private val c3 = FloatArray(points.size - 1)
    private val c4 = FloatArray(points.size - 1)
    private val c5 = FloatArray(points.size - 1)
    private var sampledTime = 0
    private var hasSample = false
    private var sampledPosition = positions.first()

    init {
        require(points.size >= 2 && durations.all { it > 0f })
        for (index in 1 until points.lastIndex) {
            val before = (positions[index] - positions[index - 1]) / durations[index - 1]
            val after = (positions[index + 1] - positions[index]) / durations[index]
            slopes[index] = minOf(before, after).coerceAtLeast(0f)
        }
        for (index in durations.indices) {
            val distance = positions[index + 1] - positions[index]
            val first = slopes[index] * durations[index]
            val last = slopes[index + 1] * durations[index]
            c3[index] = 10f * distance - 6f * first - 4f * last
            c4[index] = -15f * distance + 8f * first + 7f * last
            c5[index] = 6f * distance - 3f * first - 3f * last
        }
    }

    fun sample(now: Int): Float {
        if (hasSample && sampledTime == now) return sampledPosition
        val value = when {
            now <= times.first() -> positions.first()
            now >= times.last() -> positions.last()
            else -> {
                var low = 0
                var high = times.lastIndex
                while (low + 1 < high) {
                    val middle = (low + high) / 2
                    if (times[middle] <= now) low = middle else high = middle
                }
                val t = (now.toLong() - times[low]) / durations[low]
                positions[low] + slopes[low] * durations[low] * t +
                    t * t * t * (c3[low] + t * (c4[low] + t * c5[low]))
            }
        }
        sampledTime = now
        hasSample = true
        sampledPosition = value
        return value
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
