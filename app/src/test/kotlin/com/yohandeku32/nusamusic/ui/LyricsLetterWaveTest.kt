package com.yohandeku32.nusamusic.ui

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs

class LyricsLetterWaveTest {
    @Test fun independentLettersKeepAccentsWhileJoiningScriptsAndEmojiStayShaped() {
        val text = "a\u0301ku"
        assertEquals(listOf("a\u0301", "k", "u"), lyricIndependentLetterRanges(text).map {
            text.substring(it.first, it.last + 1)
        })
        for (content in listOf("سلام", "नमस्ते", "A👩‍🎤B", "កម្ពុជា", "สวัสดี")) {
            assertTrue("Keep contextual shaping for $content", lyricIndependentLetterRanges(content).isEmpty())
        }
        for (content in listOf("Victory", "café", "Δέλτα", "Привет", "日本語", "한글")) {
            assertTrue(lyricIndependentLetterRanges(content).isNotEmpty())
        }
    }

    @Test fun cachedCharacterEligibilityDoesNotToggleDuringPlayback() {
        val wave = LyricsHeldWordWave(listOf(
            LyricsHeldWordUnit("hold ", 1000, 7000, 0f, 80f),
            LyricsHeldWordUnit("rap ", 7000, 7200, 85f, 120f),
            LyricsHeldWordUnit("inferred", 7200, 12000, 125f, 220f, reliableHold = false)
        ))
        assertTrue(wave.hasHold(0))
        assertFalse(wave.hasHold(1))
        assertFalse(wave.hasHold(2))
        for (now in listOf(0, 1300, 4000, 8000, 2000)) {
            wave.letterPose(0, now, 10f)
            assertTrue(wave.hasHold(0))
        }
    }

    @Test fun shapedRunsInOneWordShareSmoothMotionAt60_90_120Hz() {
        val wave = LyricsLetterWave(listOf(
            LyricsHeldWordUnit("mélo", 1000, 1400, 0f, 60f),
            LyricsHeldWordUnit("die", 1400, 2000, 60f, 120f)
        ))
        for (hz in listOf(60, 90, 120)) {
            var previous = 0f
            var peak = 0f
            for (frame in 0..hz * 3) {
                val now = (frame * 1000.0 / hz).toInt()
                val first = wave.wordLiftDp(0, now)
                val second = wave.wordLiftDp(1, now)
                assertEquals("Joined runs separated at $now", first, second, 0f)
                assertTrue("Motion snapped at $hz Hz / $now", abs(first - previous) < 0.14f)
                peak = maxOf(peak, -first)
                previous = first
            }
            assertTrue("Motion must not be disabled for medium words", peak > 0.4f)
            assertEquals(0f, previous, 0f)
        }
    }

    @Test fun longHeldWordsRetainTheirSecondaryCrestAndSustainedPull() {
        val units = listOf(
            LyricsHeldWordUnit("ka", 1000, 1300, 0f, 30f),
            LyricsHeldWordUnit("mu", 1300, 7000, 30f, 90f)
        )
        val shortWave = LyricsLetterWave(units)
        val heldWave = LyricsHeldWordWave(units)
        var heldPeak = 0f
        var secondaryPeak = 0f
        for (now in 900..7500 step 8) {
            val first = shortWave.wordLiftDp(0, now)
            assertEquals(first, shortWave.wordLiftDp(1, now), 0f)
            secondaryPeak = maxOf(secondaryPeak, -first)
            heldPeak = maxOf(heldPeak, heldWave.pose(1, now, 60f).depth)
        }
        assertTrue("Preserve the existing secondary crest on long words", secondaryPeak > 0.8f)
        assertTrue("The authored held-vocal wave must remain", heldPeak > 0.9f)
        assertEquals(0f, shortWave.wordLiftDp(0, 7000), 0f)
        assertEquals(LyricsHeldWordPose(), heldWave.pose(1, 7000, 60f))
    }

    @Test fun inferredEndNeverInventsWordMotion() {
        val wave = LyricsLetterWave(listOf(
            LyricsHeldWordUnit("held", 1000, 5000, 0f, 90f, reliableHold = false)
        ))
        for (now in 900..5500 step 8) {
            assertFalse(wave.isAnimating(0, now))
            assertEquals(0f, wave.wordLiftDp(0, now), 0f)
        }
    }

    @Test fun wordMotionFreezesOnPauseAndResamplesAfterSeek() {
        val wave = LyricsLetterWave(listOf(
            LyricsHeldWordUnit("réflexion", 1000, 2100, 0f, 150f)
        ))
        val pose = wave.wordLiftDp(0, 1350)
        assertTrue(pose < 0f)
        repeat(120) { assertEquals(pose, wave.wordLiftDp(0, 1350), 0f) }
        assertEquals(0f, wave.wordLiftDp(0, 9000), 0f)
        assertEquals(pose, wave.wordLiftDp(0, 1350), 0f)
    }

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
