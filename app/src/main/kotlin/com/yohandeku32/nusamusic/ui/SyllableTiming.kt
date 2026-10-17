package com.yohandeku32.nusamusic.ui

import com.mocharealm.accompanist.lyrics.core.model.ISyncedLine
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeAlignment
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeSyllable
import com.mocharealm.accompanist.lyrics.core.model.synced.SyncedLine
import java.text.BreakIterator
import java.lang.Character.UnicodeScript
import java.util.Locale

/** Authored syllable boundaries are authoritative; line-only timing gets a visual estimate. */
internal object SyllableTiming {
    fun forDisplay(line: ISyncedLine): KaraokeLine = when (line) {
        is KaraokeLine -> {
            val unit = line.syllables.singleOrNull()
            if (unit == null || words(unit.content).size <= 1) line
            else line.copy(syllables = estimate(unit.content, unit.start, unit.end))
        }
        is SyncedLine -> KaraokeLine(estimate(line.content, line.start, line.end), line.translation,
            false, KaraokeAlignment.Unspecified, line.start, line.end)
        else -> error("Unsupported lyric line")
    }

    private fun estimate(text: String, start: Int, end: Int): List<KaraokeSyllable> {
        val pieces = words(text).ifEmpty { listOf(text) }
        val weights = pieces.map { piece -> piece.codePoints().filter { Character.isLetterOrDigit(it) }.count().coerceAtLeast(1) }
        val total = weights.sum()
        val begin = start.coerceIn(0, Int.MAX_VALUE - 1)
        val finish = end.coerceAtLeast(begin + 1)
        val duration = finish.toLong() - begin
        var weight = 0L
        return pieces.mapIndexed { index, piece ->
            val from = (begin + duration * weight / total).toInt().coerceAtMost(finish - 1)
            weight += weights[index]
            val to = (begin + duration * weight / total).toInt().coerceAtLeast(from + 1)
            KaraokeSyllable(piece, from, to)
        }
    }

    /** Preserve spaces/punctuation and Unicode clusters; CJK syllabic characters stand alone. */
    private fun words(text: String): List<String> {
        val result = mutableListOf<String>()
        val matches = Regex("\\S+\\s*").findAll(text).toList()
        matches.forEach { match ->
            val core = match.value.trimEnd()
            val iterator = BreakIterator.getCharacterInstance(Locale.ROOT).apply { setText(core) }
            val word = StringBuilder()
            fun flush() { if (word.isNotEmpty()) { result += word.toString(); word.clear() } }
            var from = iterator.first()
            var to = iterator.next()
            while (to != BreakIterator.DONE) {
                val cluster = core.substring(from, to)
                when (UnicodeScript.of(cluster.codePointAt(0))) {
                    UnicodeScript.HAN, UnicodeScript.HIRAGANA, UnicodeScript.KATAKANA, UnicodeScript.HANGUL -> {
                        flush()
                        result += cluster
                    }
                    else -> word.append(cluster)
                }
                from = to
                to = iterator.next()
            }
            flush()
            if (result.isNotEmpty()) result[result.lastIndex] += match.value.substring(core.length)
        }
        if (result.isNotEmpty()) result[0] = text.substring(0, matches.first().range.first) + result[0]
        return result
    }
}

internal fun syllableProgress(syllable: KaraokeSyllable, now: Int): Float = when {
    now < syllable.start -> 0f
    now >= syllable.end -> 1f
    else -> ((now.toLong() - syllable.start).toFloat() / (syllable.end.toLong() - syllable.start).coerceAtLeast(1)).coerceIn(0f, 1f)
}
