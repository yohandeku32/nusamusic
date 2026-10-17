package com.yohandeku32.nusamusic.ui

import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeAlignment
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeSyllable
import com.mocharealm.accompanist.lyrics.core.model.synced.SyncedLine
import org.junit.Assert.*
import org.junit.Test

class SyllableTimingTest {
    @Test fun authoredSyllablesAndSilencesRemainUnchanged() {
        val line = KaraokeLine(listOf(KaraokeSyllable("hel", 1_000, 2_000), KaraokeSyllable("lo", 4_000, 5_000)),
            "halo", true, KaraokeAlignment.End, 1_000, 5_000)
        assertSame(line, SyllableTiming.forDisplay(line))
        assertEquals(1f, syllableProgress(line.syllables[0], 3_000), 0f)
        assertEquals(0f, syllableProgress(line.syllables[1], 3_000), 0f)
    }
    @Test fun overlappingSyllablesHaveIndependentProgress() {
        val first = KaraokeSyllable("one", 1_000, 3_000)
        val second = KaraokeSyllable("two", 2_000, 4_000)
        assertEquals(0.75f, syllableProgress(first, 2_500), 0.001f)
        assertEquals(0.25f, syllableProgress(second, 2_500), 0.001f)
    }
    @Test fun progressResetsOnBackwardSeekAndHasSafeBoundaries() {
        val syllable = KaraokeSyllable("word", 1_000, 2_000)
        assertEquals(0f, syllableProgress(syllable, 999), 0f)
        assertEquals(0f, syllableProgress(syllable, 1_000), 0f)
        assertEquals(1f, syllableProgress(syllable, 2_000), 0f)
        assertEquals(0.5f, syllableProgress(syllable, 1_500), 0f)
        assertEquals(0f, syllableProgress(syllable, 0), 0f)
    }
    @Test fun zeroDurationAndExtremeTimestampsNeverProduceNonFiniteProgress() {
        assertEquals(1f, syllableProgress(KaraokeSyllable("zero", 100, 100), 100), 0f)
        assertEquals(1f, syllableProgress(KaraokeSyllable("last", Int.MAX_VALUE - 1, Int.MAX_VALUE), Int.MAX_VALUE), 0f)
    }
    @Test fun lineOnlyFallbackPreservesTextTranslationAndBounds() {
        val original = SyncedLine("  Hello, world!  ", "Halo dunia", 1_000, 5_000)
        val display = SyllableTiming.forDisplay(original)
        assertEquals(original.content, display.syllables.joinToString("") { it.content })
        assertEquals(original.translation, display.translation)
        assertEquals(1_000, display.syllables.first().start)
        assertEquals(5_000, display.syllables.last().end)
        assertTrue(display.syllables.all { it.end > it.start })
        assertEquals(2, display.syllables.size)
    }
    @Test fun estimatedWordsCannotExtendIntoAnInstrumentalGap() {
        val display = SyllableTiming.forDisplay(SyncedLine("One two three", null, 2_000, 5_000))
        assertTrue(display.syllables.all { it.start >= 2_000 && it.end <= 5_000 })
        assertTrue(display.syllables.all { syllableProgress(it, 10_000) == 1f })
    }
    @Test fun unicodeClustersAndEmojiArePreserved() {
        val text = "cafe\u0301 👨‍👩‍👧‍👦 中文 한글 か\u3099"
        val display = SyllableTiming.forDisplay(SyncedLine(text, null, 0, 10_000))
        assertEquals(text, display.syllables.joinToString("") { it.content })
        assertTrue(display.syllables.any { it.content.trim() == "cafe\u0301" })
        assertTrue(display.syllables.any { it.content.trim() == "👨‍👩‍👧‍👦" })
        assertTrue(display.syllables.any { it.content.trim() == "か\u3099" })
        assertTrue(display.syllables.any { it.content == "中" })
    }
    @Test fun shortLinesStillHavePositiveBoundedUnitDurations() {
        val display = SyllableTiming.forDisplay(SyncedLine("A B C D", null, 10, 11))
        assertTrue(display.syllables.all { it.start == 10 && it.end == 11 })
    }
    @Test fun singleTimedSentenceUsesItsAuthoredBoundsForWordEstimates() {
        val line = KaraokeLine(listOf(KaraokeSyllable("Hello world", 1_000, 4_000)),
            "Halo", true, KaraokeAlignment.End, 1_000, 4_000)
        val display = SyllableTiming.forDisplay(line)
        assertEquals(2, display.syllables.size)
        assertEquals(line.translation, display.translation)
        assertEquals(line.alignment, display.alignment)
        assertTrue(display.isAccompaniment)
        assertEquals(line.start, display.syllables.first().start)
        assertEquals(line.end, display.syllables.last().end)
    }
}
