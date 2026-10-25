package com.yohandeku32.nusamusic.ui

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs

class LyricsLetterWaveTest {
    @Test fun shortWordConnectionsLeaveNeighboringLongVocalMotionUnchanged() {
        val long = LyricsHeldWordUnit("panjang ", 1900, 6900, 105f, 205f)
        val solo = LyricsLetterWave(listOf(long))
        val phrase = LyricsLetterWave(listOf(
            LyricsHeldWordUnit("aku ", 1000, 1900, 0f, 100f), long,
            LyricsHeldWordUnit("lagi", 6900, 7800, 210f, 310f)
        ))
        for (time in 900..8000 step 8) for (center in listOf(115f, 155f, 195f)) {
            assertEquals(solo.liftDp(0, center, time), phrase.liftDp(1, center, time), 0f)
            assertEquals(solo.isAnimating(0, time), phrase.isAnimating(1, time))
        }
    }

    @Test fun nearbyShortWordsShareAContinuousCrestWithBoundedVisualAnticipation() {
        for (gap in listOf(0, 60, 120)) {
            val nextStart = 1900 + gap
            val wave = LyricsLetterWave(listOf(
                LyricsHeldWordUnit("aku ", 1000, 1900, 0f, 100f),
                LyricsHeldWordUnit("suka", nextStart, nextStart + 900, 105f, 205f)
            ))
            assertTrue("The outgoing crest must keep releasing at the boundary",
                wave.liftDp(0, 90f, nextStart) < -0.01f)
            for (time in nextStart..nextStart + 90 step 5) {
                val outgoing = wave.liftDp(0, 90f, time)
                val incoming = wave.liftDp(1, 115f, time)
                assertTrue("Both words became still during the handoff at $time",
                    outgoing < -0.00001f || incoming < -0.00001f)
            }
            assertEquals(0f, wave.liftDp(1, 115f, nextStart - 180), 0f)
            assertTrue(wave.liftDp(1, 115f, nextStart) < -0.01f)
            assertEquals(0f, wave.liftDp(0, 100f, nextStart + 180), 0f)
        }
    }

    @Test fun bothWordsUseTheSameMovingSurfaceAtTheirSharedBoundary() {
        val wave = LyricsLetterWave(listOf(
            LyricsHeldWordUnit("aku ", 1000, 1900, 0f, 100f),
            LyricsHeldWordUnit("suka", 1900, 2800, 105f, 205f)
        ))
        val sharedCenter = 102.5f
        val outgoing = wave.liftDp(0, sharedCenter, 1900)
        val incoming = wave.liftDp(1, sharedCenter, 1900)
        assertTrue("The flow must not reset at the next word", incoming < -0.1f)
        assertEquals(outgoing, incoming, 0.00001f)
        assertEquals(wave.liftDp(0, sharedCenter, 1899),
            wave.liftDp(1, sharedCenter, 1901), 0.001f)
    }

    @Test fun flowCursorKeepsVelocityAndAccelerationAcrossMixedWordDurations() {
        val flow = LyricsWordFlow(listOf(1000 to -40f, 1900 to 102.5f,
            2020 to 110f, 3000 to 215f, 4200 to 380f))
        for (join in listOf(1900, 2020, 3000)) {
            val leftVelocity = flow.sample(join) - flow.sample(join - 1)
            val rightVelocity = flow.sample(join + 1) - flow.sample(join)
            assertTrue("Do not stop and restart at $join", minOf(leftVelocity, rightVelocity) > 0.02f)
            assertEquals(leftVelocity, rightVelocity, 0.001f)
            val leftAcceleration = flow.sample(join) - 2f * flow.sample(join - 1) + flow.sample(join - 2)
            val rightAcceleration = flow.sample(join + 2) - 2f * flow.sample(join + 1) + flow.sample(join)
            assertEquals(leftAcceleration, rightAcceleration, 0.001f)
        }
        var previous = flow.sample(900)
        for (time in 900..4300 step 4) {
            val current = flow.sample(time)
            assertTrue("The crest reversed at $time", current >= previous - 0.0001f)
            previous = current
        }
        val paused = flow.sample(1950)
        repeat(30) { assertEquals(paused, flow.sample(1950), 0f) }
        flow.sample(4000)
        assertEquals(paused, flow.sample(1950), 0f)
    }

    @Test fun aConnectedCharacterRisesOnlyOnceAsTheCrestPasses() {
        val wave = LyricsLetterWave(listOf(
            LyricsHeldWordUnit("aku ", 1000, 1900, 0f, 100f),
            LyricsHeldWordUnit("suka ", 1900, 2800, 105f, 205f),
            LyricsHeldWordUnit("musik", 2800, 3700, 210f, 310f)
        ))
        for (hz in listOf(60, 90, 120)) for ((index, center) in listOf(90f, 115f, 220f).withIndex()) {
            var previous = 0f
            var peak = 0f
            var releasing = false
            for (frame in 0..hz * 4) {
                val time = (frame * 1000.0 / hz).toInt()
                val current = wave.liftDp(index, center, time)
                if (current > previous + 0.00001f) releasing = true
                if (releasing) assertTrue("A second kick appeared at $time", current >= previous - 0.00001f)
                assertTrue(abs(current - previous) < 0.1f)
                peak = minOf(peak, current)
                previous = current
            }
            assertTrue(peak < -0.1f)
            assertEquals(0f, previous, 0f)
        }
    }

    @Test fun shortWordReleaseUsesTheAuthoredWindowInsteadOfStoppingMidWord() {
        val wave = LyricsLetterWave(listOf(
            LyricsHeldWordUnit("melodi", 1000, 2200, 0f, 100f)
        ))
        assertTrue("Keep the trailing character moving near the authored end",
            wave.liftDp(0, 95f, 2050) < -0.01f)
        assertEquals(0f, wave.liftDp(0, 95f, 2200), 0f)
    }

    @Test fun restsRapidAndUnreliableWordsBreakTheShortWordConnection() {
        for (next in listOf(
            LyricsHeldWordUnit("rest", 2021, 2921, 105f, 205f),
            LyricsHeldWordUnit("rapid", 1900, 2150, 105f, 205f),
            LyricsHeldWordUnit("unknown", 1900, 2800, 105f, 205f, reliableHold = false),
            LyricsHeldWordUnit("long", 1900, 6900, 105f, 205f)
        )) {
            val wave = LyricsLetterWave(listOf(
                LyricsHeldWordUnit("aku ", 1000, 1900, 0f, 100f), next
            ))
            for (time in 1900..2300 step 8) {
                assertEquals("Do not drag motion across ${next.content}",
                    0f, wave.liftDp(0, 95f, time), 0f)
            }
        }
    }

    @Test fun connectedShortWordsRemainSmoothAt60_90_120HzAndDeterministicOnSeek() {
        val wave = LyricsLetterWave(listOf(
            LyricsHeldWordUnit("aku ", 1000, 1900, 0f, 100f),
            LyricsHeldWordUnit("suka", 1900, 2800, 105f, 205f)
        ))
        for (hz in listOf(60, 90, 120)) for ((index, center) in listOf(90f, 115f).withIndex()) {
            var previous = 0f
            for (frame in 0..hz * 3) {
                val time = (frame * 1000.0 / hz).toInt()
                val current = wave.liftDp(index, center, time)
                assertTrue("Handoff jumped at $hz Hz / $time", abs(current - previous) < 0.12f)
                previous = current
            }
            val boundary = wave.liftDp(index, center, 1950)
            repeat(30) { assertEquals(boundary, wave.liftDp(index, center, 1950), 0f) }
            wave.liftDp(index, center, 2700)
            assertEquals(boundary, wave.liftDp(index, center, 1950), 0f)
        }
    }

    @Test fun shortPullsAreEligibleForCharacterDrawingBeforeTheLongHoldThreshold() {
        val units = listOf(
            LyricsHeldWordUnit("cepat ", 0, 200, 0f, 60f),
            LyricsHeldWordUnit("melodi ", 200, 1200, 65f, 155f),
            LyricsHeldWordUnit("unknown", 1200, 2300, 160f, 250f, reliableHold = false)
        )
        val wave = LyricsLetterWave(units)
        val held = LyricsHeldWordWave(units)
        assertFalse(held.hasHold(1))
        assertTrue("Short pulls need separate character layouts", wave.hasMotion(1))
        assertFalse(wave.hasMotion(0))
        assertFalse(wave.hasMotion(2))
        for (now in listOf(0, 400, 900, 1600, 400)) {
            wave.liftDp(1, 80f, now)
            assertTrue("Never swap glyph layouts when a short pull starts/ends", wave.hasMotion(1))
        }
    }

    @Test fun shortPullCharactersPeakInOrderAndReturnSmoothlyAt60_90_120Hz() {
        for (duration in listOf(700, 900, 1200)) {
            val wave = LyricsLetterWave(listOf(
                LyricsHeldWordUnit("aruna", 1000, 1000 + duration, 0f, 100f)
            ))
            for (hz in listOf(60, 90, 120)) {
                var previousPeakTime = 0
                for (center in listOf(10f, 30f, 50f, 70f, 90f)) {
                    var previous = 0f
                    var peak = 0f
                    var peakTime = 0
                    var falling = false
                    for (frame in 0..hz * 3) {
                        val time = (frame * 1000.0 / hz).toInt()
                        val lift = wave.liftDp(0, center, time)
                        assertTrue("Letter jumped at $duration ms / $hz Hz / $time",
                            abs(lift - previous) < 0.18f)
                        if (lift < peak) { peak = lift; peakTime = time }
                        if (lift > previous + 0.000001f) falling = true
                        if (falling) assertTrue("Short pull bounced twice", lift >= previous - 0.000001f)
                        previous = lift
                    }
                    assertTrue("Each character must move", peak < 0f)
                    assertTrue("The crest must travel across the word", peakTime > previousPeakTime)
                    assertEquals(0f, previous, 0f)
                    previousPeakTime = peakTime
                }
            }
        }
    }

    @Test fun shortCharacterWaveDoesNotRestartAtTtmlSyllableBoundaries() {
        val whole = LyricsLetterWave(listOf(
            LyricsHeldWordUnit("melodi", 1000, 2000, 0f, 120f)
        ))
        val split = LyricsLetterWave(listOf(
            LyricsHeldWordUnit("me", 1000, 1250, 0f, 40f),
            LyricsHeldWordUnit("lo", 1250, 1600, 40f, 80f),
            LyricsHeldWordUnit("di", 1600, 2000, 80f, 120f)
        ))
        for (time in 900..2200 step 8) for ((index, center) in listOf(20f, 60f, 100f).withIndex()) {
            assertTrue(split.hasMotion(index))
            assertEquals(whole.liftDp(0, center, time), split.liftDp(index, center, time), 0f)
        }
    }

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
