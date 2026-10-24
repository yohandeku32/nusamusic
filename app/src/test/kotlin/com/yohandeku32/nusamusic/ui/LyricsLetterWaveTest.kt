package com.yohandeku32.nusamusic.ui

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs

class LyricsLetterWaveTest {
    @Test fun crestTravelsFromOneLetterToTheNextRatherThanLiftingTheWholeWord() {
        val wave = LyricsLetterWave(listOf(
            LyricsHeldWordUnit("melodi", 1000, 2000, 0f, 120f)
        ))
        val first = wave.liftDp(0, 12f, 1180)
        val last = wave.liftDp(0, 108f, 1180)
        assertTrue(first < -2.0f)
        assertEquals(0f, last, 0f)
        assertTrue(wave.liftDp(0, 108f, 1320) < -2.0f)
        assertTrue(wave.liftDp(0, 12f, 1320) > first)
    }

    @Test fun eachLetterRisesAndFallsWithoutFrameJumpsAt120Hz() {
        val wave = LyricsLetterWave(listOf(
            LyricsHeldWordUnit("aruna", 1000, 1280, 0f, 80f)
        ))
        var previous = 0f
        for (ms in 900..1800 step 8) {
            val lift = wave.liftDp(0, 40f, ms)
            assertTrue(lift in -2.35f..0f)
            assertTrue("Jump at $ms: $previous -> $lift", abs(lift - previous) < 0.29f)
            previous = lift
        }
        assertEquals(0f, wave.liftDp(0, 40f, 1800), 0f)
    }

    @Test fun adjacentWordsHaveIndependentTimingAndNoRecurringBounce() {
        val wave = LyricsLetterWave(listOf(
            LyricsHeldWordUnit("aku ", 1000, 1400, 0f, 45f),
            LyricsHeldWordUnit("suka", 1400, 1800, 48f, 97f)
        ))
        assertTrue(wave.liftDp(0, 20f, 1200) < 0f)
        assertEquals(0f, wave.liftDp(1, 65f, 1200), 0f)
        assertTrue(wave.liftDp(1, 65f, 1600) < 0f)
        assertEquals(0f, wave.liftDp(0, 20f, 2100), 0f)
        assertEquals(0f, wave.liftDp(1, 65f, 2200), 0f)
        repeat(30) {
            assertEquals(wave.liftDp(0, 20f, 1210),
                wave.liftDp(0, 20f, 1210), 0f)
        }
    }

    @Test fun combiningAccentAndSurrogatePairStayInOneGrapheme() {
        val accented = "a\u0301b"
        val accentParts = lyricGraphemeRanges(accented).map {
            accented.substring(it.first, it.last + 1)
        }
        assertEquals(listOf("a\u0301", "b"), accentParts)
        val emojiText = "A\uD83D\uDE00B"
        val emojiParts = lyricGraphemeRanges(emojiText).map {
            emojiText.substring(it.first, it.last + 1)
        }
        assertEquals(listOf("A", "\uD83D\uDE00", "B"), emojiParts)
    }
}
