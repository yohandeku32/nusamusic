package com.yohandeku32.nusamusic.ui

import org.junit.Assert.*
import org.junit.Test

class LyricsCjkHoldIsolationTest {
    @Test fun heldChineseOrJapaneseUnitCannotAnimateEitherNeighbor() {
        for (text in listOf(listOf("你", "好", "吗"), listOf("あ", "い", "う"), listOf("カ", "キ", "ク"))) {
            for (gap in listOf(2.5f, Float.POSITIVE_INFINITY)) {
                val units = listOf(
                    LyricsHeldWordUnit(text[0], 500, 1000, 0f, 34f),
                    LyricsHeldWordUnit(text[1], 1000, 4000, 34f, 68f),
                    LyricsHeldWordUnit(text[2], 4000, 4500, 68f, 102f)
                )
                val wave = LyricsHeldWordWave(units, gap)
                assertTrue(wave.hasHold(1))
                assertFalse("A preceding CJK unit must not inherit a later hold: $text", wave.hasHold(0))
                assertFalse("A following CJK unit must not inherit an earlier hold: $text", wave.hasHold(2))
                assertTrue(wave.pose(1, 2260, 51f).depth > 0.9f)
                for (time in 500..4500 step 25) for (index in listOf(0, 2)) {
                    val center = (units[index].pathStartDp + units[index].pathEndDp) * 0.5f
                    assertEquals(LyricsHeldWordPose(), wave.pose(index, time, center))
                    assertEquals(LyricsHeldWordPose(), wave.letterPose(index, time, center))
                }
            }
        }
    }

    @Test fun adjacentCjkHoldsUseTheirOwnIntervalsRatherThanTheLongestInThePhrase() {
        val units = listOf(LyricsHeldWordUnit("你", 1000, 3000, 0f, 34f),
            LyricsHeldWordUnit("好", 3000, 6000, 34f, 68f))
        val wave = LyricsHeldWordWave(units, Float.POSITIVE_INFINITY)
        assertTrue(wave.pose(0, 1840, 17f).depth > 0.5f)
        assertEquals(LyricsHeldWordPose(), wave.pose(1, 1840, 51f))
        assertEquals(LyricsHeldWordPose(), wave.pose(0, 4260, 17f))
        assertTrue(wave.pose(1, 4260, 51f).depth > 0.9f)
        val shortWave = LyricsLetterWave(units, Float.POSITIVE_INFINITY)
        assertEquals(0f, shortWave.wordLiftDp(0, 4260), 0f)
        assertEquals(0f, shortWave.wordLiftDp(1, 1840), 0f)
    }

    @Test fun aMultiKanjiAuthoredUnitKeepsItsInternalCharacterAnimation() {
        val wave = LyricsHeldWordWave(listOf(
            LyricsHeldWordUnit("世界", 1000, 4000, 0f, 68f),
            LyricsHeldWordUnit("へ", 4000, 4400, 68f, 102f)
        ), Float.POSITIVE_INFINITY)
        assertTrue(wave.hasHold(0))
        assertTrue(wave.letterPose(0, 2260, 17f).depth > 0f)
        assertTrue(wave.letterPose(0, 2260, 51f).depth > 0f)
        assertFalse(wave.hasHold(1))
    }
}
