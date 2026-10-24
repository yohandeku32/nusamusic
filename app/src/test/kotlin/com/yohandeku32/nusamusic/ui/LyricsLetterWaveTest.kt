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
        assertTrue(first < -0.4f)
        assertEquals(0f, last, 0f)
        assertTrue(wave.liftDp(0, 108f, 1560) < -0.4f)
        assertTrue(wave.liftDp(0, 12f, 1660) > first)
    }

    @Test fun eachLetterRisesAndFallsWithoutFrameJumpsAt120Hz() {
        val wave = LyricsLetterWave(listOf(
            LyricsHeldWordUnit("aruna", 1000, 2000, 0f, 80f)
        ))
        var previous = 0f
        var maximumLift = 0f
        for (ms in 900..2200 step 8) {
            val lift = wave.liftDp(0, 40f, ms)
            assertTrue(lift in -0.85f..0f)
            maximumLift = minOf(maximumLift, lift)
            assertTrue("Jump at $ms: $previous -> $lift", abs(lift - previous) < 0.29f)
            previous = lift
        }
        assertTrue(maximumLift < -0.4f)
        assertEquals(0f, wave.liftDp(0, 40f, 2200), 0f)
    }

    @Test fun adjacentWordsHaveIndependentTimingAndNoRecurringBounce() {
        val wave = LyricsLetterWave(listOf(
            LyricsHeldWordUnit("aku ", 1000, 1900, 0f, 45f),
            LyricsHeldWordUnit("suka", 1900, 2900, 48f, 97f)
        ))
        assertTrue(wave.liftDp(0, 20f, 1200) < 0f)
        assertEquals(0f, wave.liftDp(1, 65f, 1200), 0f)
        assertTrue(wave.liftDp(1, 65f, 2200) < 0f)
        assertEquals(0f, wave.liftDp(0, 20f, 2100), 0f)
        assertEquals(0f, wave.liftDp(1, 65f, 3000), 0f)
        repeat(30) {
            assertEquals(wave.liftDp(0, 20f, 1210),
                wave.liftDp(0, 20f, 1210), 0f)
        }
    }

    @Test fun rapidWordsSkipLetterAnimationIncludingItsOldOutgoingTail() {
        for (duration in listOf(80, 180, 280, 450, 650)) {
            val wave = LyricsLetterWave(listOf(
                LyricsHeldWordUnit("cepat ", 1000, 1000 + duration, 0f, 80f)
            ))
            for (now in 900..2200 step 8) {
                assertFalse(wave.isAnimating(0, now))
                for (center in listOf(5f, 40f, 75f)) {
                    assertEquals(0f, wave.liftDp(0, center, now), 0f)
                }
            }
        }
    }

    @Test fun durationGateIsSoftAndLastLetterFinishesBeforeTheNextWord() {
        var previous = 0f
        for (duration in 650..1300 step 10) {
            val wave = LyricsLetterWave(listOf(
                LyricsHeldWordUnit("pelan ", 1000, 1000 + duration, 0f, 80f)
            ))
            val peak = (1000..2300 step 4).maxOf { now -> -wave.liftDp(0, 40f, now) }
            assertTrue(peak <= 0.85f)
            assertTrue(abs(peak - previous) < 0.04f)
            previous = peak
            for (now in 1000 + duration..2000 + duration step 8) {
                assertFalse(wave.isAnimating(0, now))
                assertEquals(0f, wave.liftDp(0, 80f, now), 0f)
            }
        }
    }

    @Test fun wholeLineTimingDoesNotInventMotionForItsFastWords() {
        val wave = LyricsLetterWave(listOf(
            LyricsHeldWordUnit("kata cepat dalam satu baris", 1000, 5000, 0f, 240f)
        ))
        for (now in 900..5500 step 8) {
            assertFalse(wave.isAnimating(0, now))
            assertEquals(0f, wave.liftDp(0, 120f, now), 0f)
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
