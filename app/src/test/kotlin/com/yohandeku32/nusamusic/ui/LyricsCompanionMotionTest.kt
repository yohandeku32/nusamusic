package com.yohandeku32.nusamusic.ui

import com.yohandeku32.nusamusic.data.LyricsPronunciation
import com.yohandeku32.nusamusic.data.LyricsPronunciationWord
import org.junit.Assert.*
import org.junit.Test

class LyricsCompanionMotionTest {
    @Test fun completePronunciationPreservesTextSpacesAndAuthoredIntervals() {
        val line = pronunciationKaraokeLine(LyricsPronunciation("wan an", listOf(
            LyricsPronunciationWord(0..2, 1_000, 1_300), LyricsPronunciationWord(4..5, 1_350, 3_000)
        )))!!
        assertEquals("wan an", line.syllables.joinToString("") { it.content })
        assertEquals(listOf(1_000, 1_350), line.syllables.map { it.start })
        assertEquals(listOf(1_300, 3_000), line.syllables.map { it.end })
        assertEquals(3_000, line.end)
    }

    @Test fun untimedWordsKeepTheFallbackInsteadOfInheritingAnotherWordsTiming() {
        assertNull(pronunciationKaraokeLine(LyricsPronunciation("wan wan", listOf(LyricsPronunciationWord(4..6, 2_000, 3_000)))))
        assertNull(pronunciationKaraokeLine(LyricsPronunciation("wan an", listOf(LyricsPronunciationWord(0..2, 1_000, 2_000)))))
        assertNull(pronunciationKaraokeLine(LyricsPronunciation("wan an")))
    }

    @Test fun malformedViewRangesDoNotReachThePainter() {
        for (range in listOf(-1..2, 0..50, 2..1))
            assertNull(pronunciationKaraokeLine(LyricsPronunciation("wan", listOf(LyricsPronunciationWord(range, 1_000, 2_000)))))
    }

    @Test fun rapidSyllablesRetainTheirShortDurations() {
        val line = pronunciationKaraokeLine(LyricsPronunciation("wan an", listOf(
            LyricsPronunciationWord(0..2, 1_000, 1_250), LyricsPronunciationWord(4..5, 1_250, 1_500)
        )))!!
        assertEquals(listOf(250, 250), line.syllables.map { it.end - it.start })
        assertNull(pronunciationKaraokeLine(LyricsPronunciation("wan", listOf(LyricsPronunciationWord(0..2, 1_000, 1_000)))))
    }
}
