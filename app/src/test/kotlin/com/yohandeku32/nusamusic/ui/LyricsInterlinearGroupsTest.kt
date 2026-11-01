package com.yohandeku32.nusamusic.ui

import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeAlignment
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeSyllable
import com.yohandeku32.nusamusic.data.LyricsPronunciation
import com.yohandeku32.nusamusic.data.LyricsPronunciationWord
import org.junit.Assert.*
import org.junit.Test

class LyricsInterlinearGroupsTest {
    private fun line(vararg words: Triple<String, Int, Int>): KaraokeLine {
        val units = words.map { KaraokeSyllable(it.first, it.second, it.third) }
        return KaraokeLine(units, null, false, KaraokeAlignment.Unspecified, units.first().start, units.last().end)
    }

    private fun reading(vararg words: Triple<String, Int, Int>): LyricsPronunciation {
        var cursor = 0
        return LyricsPronunciation(words.joinToString(" ") { it.first }, words.map {
            val range = cursor until cursor + it.first.length
            cursor += it.first.length + 1
            LyricsPronunciationWord(range, it.second, it.third)
        })
    }

    @Test fun chineseCharactersMatchTheirOwnTimedPinyin() {
        val source = line(Triple("镜", 1_000, 2_000), Triple("子", 2_000, 3_000), Triple("里", 3_000, 4_000))
        val phonetic = reading(Triple("jing", 1_000, 2_000), Triple("zi", 2_000, 3_000), Triple("li", 3_000, 4_000))
        assertEquals(listOf(LyricsReadingGroup(0..0, 0..0), LyricsReadingGroup(1..1, 1..1), LyricsReadingGroup(2..2, 2..2)), interlinearReadingGroups(source, phonetic))
    }

    @Test fun japaneseReadingCanBelongToTwoKanjiTogether() {
        val source = line(Triple("世", 1_000, 2_000), Triple("界", 2_000, 3_000), Triple("へ", 3_000, 4_000))
        val phonetic = reading(Triple("sekai", 1_000, 3_000), Triple("e", 3_000, 4_000))
        assertEquals(listOf(LyricsReadingGroup(0..1, 0..0), LyricsReadingGroup(2..2, 1..1)), interlinearReadingGroups(source, phonetic))
    }

    @Test fun severalReadingSpansCanBelongToOneKanjiGroup() {
        val source = line(Triple("世界", 1_000, 3_000))
        val phonetic = reading(Triple("se", 1_000, 2_000), Triple("kai", 2_000, 3_000))
        assertEquals(listOf(LyricsReadingGroup(0..0, 0..1)), interlinearReadingGroups(source, phonetic))
    }

    @Test fun repeatedPinyinUsesTimeInsteadOfTheFirstMatchingText() {
        val source = line(Triple("明", 1_000, 2_000), Triple("明", 2_000, 3_000))
        val phonetic = reading(Triple("ming", 1_000, 2_000), Triple("ming", 2_000, 3_000))
        assertEquals(listOf(LyricsReadingGroup(0..0, 0..0), LyricsReadingGroup(1..1, 1..1)), interlinearReadingGroups(source, phonetic))
    }

    @Test fun unannotatedEnglishTailKeepsItsOwnCell() {
        val source = line(Triple("明 ", 1_000, 2_000), Triple("Oh", 2_000, 3_000))
        assertEquals(listOf(LyricsReadingGroup(0..0, 0..0), LyricsReadingGroup(1..1)),
            interlinearReadingGroups(source, reading(Triple("ming", 1_000, 2_000))))
    }

    @Test fun mismatchedAndMissingTimingKeepTheSentenceFallback() {
        val source = line(Triple("明", 1_000, 2_000))
        assertNull(interlinearReadingGroups(source, reading(Triple("ming", 1_010, 2_000))))
        assertNull(interlinearReadingGroups(source, LyricsPronunciation("ming")))
        assertNull(interlinearReadingGroups(source, reading(Triple("ming", 1_000, 2_100))))
    }

    @Test fun overlappingSourceIntervalsAreNotGuessed() {
        val source = line(Triple("明", 1_000, 2_000), Triple("天", 1_500, 3_000))
        assertNull(interlinearReadingGroups(source, reading(Triple("ming", 1_000, 2_000), Triple("tian", 2_000, 3_000))))
    }

    @Test fun latinOnlyLyricsKeepTheirExistingCompanionRow() {
        val source = line(Triple("Hello", 1_000, 2_000))
        assertNull(interlinearReadingGroups(source, reading(Triple("halo", 1_000, 2_000))))
    }
}
