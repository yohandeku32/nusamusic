package com.yohandeku32.nusamusic.ui

import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LyricsHeldWordWaveTest {
    @Test fun heldWordCharactersHaveSeparateDepthAndTiltThroughoutThePull() {
        val wave = LyricsHeldWordWave(listOf(
            LyricsHeldWordUnit("kamu", 2000, 7000, 0f, 80f)
        ))
        val left = wave.letterPose(0, 3500, 10f)
        val right = wave.letterPose(0, 3500, 70f)
        assertTrue("Characters must not share a flat camera plane", left.depth - right.depth > 0.3f)
        assertTrue("Characters must also have independent camera tilt",
            abs(left.perspectiveSlope - right.perspectiveSlope) > 0.01f)
        val leftPeak = (2000..7000 step 10).maxBy { wave.letterPose(0, it, 10f).depth }
        val rightPeak = (2000..7000 step 10).maxBy { wave.letterPose(0, it, 70f).depth }
        assertTrue("The crest must reach later characters later", rightPeak - leftPeak > 400)
        assertTrue("Keep the sustained release", wave.letterPose(0, 5800, 70f).depth > 0.1f)
    }

    @Test fun everyCharacterReachesTheSameCrestInOrderWithoutASecondBounce() {
        val wave = LyricsHeldWordWave(listOf(
            LyricsHeldWordUnit("melody", 2000, 7000, 0f, 120f)
        ))
        var previousPeakTime = 0
        for (center in listOf(10f, 30f, 50f, 70f, 90f, 110f)) {
            val samples = (2000..7000 step 5).map { it to wave.letterPose(0, it, center).depth }
            val peak = samples.maxBy { it.second }
            assertTrue("Trailing characters must peak later", peak.first > previousPeakTime)
            assertTrue("The trailing characters must not lose their pull", peak.second > 0.999f)
            var falling = false
            var previous = 0f
            for ((time, depth) in samples) {
                if (depth < previous - 0.000001f) falling = true
                if (falling) assertTrue("A second crest appeared at $time", depth <= previous + 0.000001f)
                previous = depth
            }
            previousPeakTime = peak.first
        }
    }

    @Test fun characterWaveContinuesAcrossAuthoredSyllableBoundaries() {
        val single = LyricsHeldWordWave(listOf(
            LyricsHeldWordUnit("melody", 2000, 7000, 0f, 120f)
        ))
        val split = LyricsHeldWordWave(listOf(
            LyricsHeldWordUnit("me", 1600, 2000, 0f, 40f),
            LyricsHeldWordUnit("lo", 2000, 7000, 40f, 80f),
            LyricsHeldWordUnit("dy", 7000, 7100, 80f, 120f)
        ))
        for (time in 1600..7200 step 16) for ((index, center) in listOf(20f, 60f, 100f).withIndex()) {
            assertEquals("Syllable boundaries must not restart the wave",
                single.letterPose(0, time, center), split.letterPose(index, time, center))
        }
    }

    @Test fun characterProjectionIsSmoothAt60_90_120HzAndRestsAtAuthoredEnd() {
        val wave = LyricsHeldWordWave(listOf(
            LyricsHeldWordUnit("kamu", 2000, 7000, 0f, 80f)
        ))
        for (hz in listOf(60, 90, 120)) for (center in listOf(10f, 30f, 50f, 70f)) {
            var previous = LyricsHeldWordPose()
            for (frame in 0..hz * 8) {
                val now = (frame * 1000.0 / hz).toInt()
                val next = wave.letterPose(0, now, center)
                assertTrue(abs(next.liftDp - previous.liftDp) < 0.2f)
                assertTrue(abs(next.depth - previous.depth) < 0.025f)
                assertTrue(abs(next.perspectiveSlope - previous.perspectiveSlope) < 0.035f)
                previous = next
            }
            assertEquals(LyricsHeldWordPose(), previous)
        }
        for (time in listOf(2000, 7000, 8000)) {
            assertEquals(LyricsHeldWordPose(), wave.letterPose(0, time, 50f))
        }
        val paused = wave.letterPose(0, 4300, 50f)
        repeat(50) { assertEquals(paused, wave.letterPose(0, 4300, 50f)) }
        wave.letterPose(0, 6500, 50f)
        assertEquals(paused, wave.letterPose(0, 4300, 50f))
    }

    @Test fun fastInferredAndMultiwordUnitsDoNotGainCharacterDepth() {
        val units = listOf(
            LyricsHeldWordUnit("rap ", 0, 180, 0f, 34f),
            LyricsHeldWordUnit("inferred ", 180, 5000, 40f, 100f, reliableHold = false),
            LyricsHeldWordUnit("two words", 5000, 9000, 105f, 200f)
        )
        val wave = LyricsHeldWordWave(units)
        for (index in units.indices) for (time in 0..9000 step 16) {
            assertEquals(LyricsHeldWordPose(), wave.letterPose(index, time, 60f))
        }
    }

    private fun phrase(): LyricsHeldWordWave = LyricsHeldWordWave(listOf(
        LyricsHeldWordUnit("aku ", 1000, 1400, 0f, 52f),
        LyricsHeldWordUnit("cinta ", 1400, 2000, 58f, 134f),
        LyricsHeldWordUnit("kamu", 2000, 7000, 140f, 222f)
    ))

    @Test fun longKamuMovesWhileAkuAndCintaNeverMove() {
        val wave = phrase()
        for (time in 0..7400 step 80) {
            assertEquals(LyricsHeldWordPose(), wave.pose(0, time, 24f))
            assertEquals(LyricsHeldWordPose(), wave.pose(1, time, 96f))
        }
        val held = wave.pose(2, 4300, 182f)
        assertTrue(held.liftDp < -2f)
        assertTrue(held.scaleY > 1f)
        assertTrue(held.intensity > 0.5f)
        assertEquals(LyricsHeldWordPose(), wave.pose(2, 2000, 182f))
        assertEquals(LyricsHeldWordPose(), wave.pose(2, 7000, 182f))
    }

    @Test fun wordSplitAcrossSyllableUnitsFormsOneLocalArch() {
        val wave = LyricsHeldWordWave(listOf(
            LyricsHeldWordUnit("aku ", 1000, 1500, 0f, 50f),
            LyricsHeldWordUnit("ka", 1500, 1900, 55f, 81f),
            LyricsHeldWordUnit("mu", 1900, 6000, 81f, 119f),
            LyricsHeldWordUnit(" lagi", 6000, 6300, 125f, 183f)
        ))
        assertEquals(LyricsHeldWordPose(), wave.pose(0, 3700, 25f))
        assertEquals(LyricsHeldWordPose(), wave.pose(3, 3700, 160f))
        val ka = wave.pose(1, 3700, 68f)
        val mu = wave.pose(2, 3700, 100f)
        assertTrue("the short 'ka' participates in the held word", ka.liftDp < -0.5f)
        assertTrue("the held 'mu' participates in the same arch", mu.liftDp < -0.5f)
        assertTrue("adjacent syllables keep a coherent wave", abs(ka.liftDp - mu.liftDp) < 2.5f)
    }

    @Test fun rapidAndInferredUnitsStayStill() {
        val wave = LyricsHeldWordWave(listOf(
            LyricsHeldWordUnit("rap ", 0, 180, 0f, 34f),
            LyricsHeldWordUnit("next ", 180, 430, 40f, 80f),
            LyricsHeldWordUnit("unverified ", 430, 9000, 85f, 160f, reliableHold = false),
            LyricsHeldWordUnit("two words", 9000, 17000, 162f, 270f)
        ))
        for (i in 0..3) {
            val time = if (i < 2) 100 else if (i == 2) 4500 else 12000
            assertEquals(LyricsHeldWordPose(), wave.pose(i, time, 150f))
        }
    }

    @Test fun zeroToPeakAndBackIsContinuousAt60_90And120Hz() {
        val wave = phrase()
        for (hz in listOf(60, 90, 120)) {
            var before = LyricsHeldWordPose()
            var maxLift = 0f
            for (frame in 0..hz * 8) {
                val now = (frame * 1000.0 / hz).toInt()
                val pose = wave.pose(2, now, 182f)
                maxLift = maxOf(maxLift, abs(pose.liftDp))
                assertTrue("lift jump at $hz Hz, $now ms",
                    abs(pose.liftDp - before.liftDp) < 0.15f)
                assertTrue("scale jump at $hz Hz, $now ms",
                    abs(pose.scaleY - before.scaleY) < 0.0012f)
                assertTrue("tilt jump at $hz Hz, $now ms",
                    abs(pose.rotationDegrees - before.rotationDegrees) < 0.03f)
                before = pose
            }
            assertTrue("held word must visibly rise", maxLift > 2.5f)
        }
    }

    @Test fun heldWordSpendsItsEntireDurationRisingThenDescendingSlowly() {
        val wave = phrase()
        fun lift(at: Int) = abs(wave.pose(2, at, 182f).liftDp)

        // A 5-second held "kamu" no longer finishes its rise in 900ms.
        assertTrue("rise must still be underway after one second",
            lift(3000) < lift(3800))
        assertTrue("the crest needs time to develop",
            lift(3800) < lift(4200))

        // The descent occupies more than half the real held timestamp.
        assertTrue("still near the top shortly after the crest",
            lift(5000) > 2.6f)
        assertTrue("release must progress gently rather than a late cut",
            lift(5000) > lift(5500))
        assertTrue(lift(5500) > lift(6000))
        assertTrue(lift(6000) > lift(6500))
        assertTrue("return to baseline by the authored end", lift(6999) < 0.002f)
    }

    @Test fun theNextLyricStartsAfterThePreviousHeldWaveHasSettled() {
        val wave = LyricsHeldWordWave(listOf(
            LyricsHeldWordUnit("kamu ", 2000, 7000, 0f, 90f),
            LyricsHeldWordUnit("selamanya", 7000, 11500, 98f, 220f)
        ))
        val before = wave.pose(0, 6800, 45f)
        val after = wave.pose(0, 7000, 45f)
        assertTrue("previous word should already be near rest", abs(before.liftDp) < 0.05f)
        assertEquals(LyricsHeldWordPose(), after)
        assertEquals("next word must not pre-animate", LyricsHeldWordPose(),
            wave.pose(1, 6999, 150f))
        assertEquals(LyricsHeldWordPose(), wave.pose(1, 7000, 150f))
        assertTrue("new word rises softly from its own authored onset",
            abs(wave.pose(1, 7100, 150f).liftDp) < 0.02f)
        assertTrue("the next held word can reach its own crest",
            abs(wave.pose(1, 9100, 150f).liftDp) > 2f)
        assertEquals(LyricsHeldWordPose(), wave.pose(0, 9100, 45f))
    }

    @Test fun ordinaryWordsDoNotMakeSmallWavesButLongHoldsProjectForward() {
        val short = LyricsHeldWordWave(listOf(
            LyricsHeldWordUnit("short", 1000, 2250, 0f, 90f)
        ))
        val long = LyricsHeldWordWave(listOf(
            LyricsHeldWordUnit("long", 1000, 6000, 0f, 90f)
        ))
        assertEquals(LyricsHeldWordPose(), short.pose(0, 1550, 45f))
        val crest = long.pose(0, 3200, 45f)
        assertTrue(crest.liftDp < -2.5f)
        assertTrue("3D crest must protrude toward screen", crest.depth > 0.65f)
        assertTrue(crest.scaleY > 1.006f)
    }

    @Test fun pauseAndBackwardSeekReproduceExactlyTheSamePose() {
        val wave = phrase()
        val held = wave.pose(2, 4600, 182f)
        repeat(60) { assertEquals(held, wave.pose(2, 4600, 182f)) }
        wave.pose(2, 6800, 182f)
        assertEquals(held, wave.pose(2, 4600, 182f))
    }

    @Test fun longVowelFormsVisibleMountainAcrossFourAdjacentSyllables() {
        val wave = LyricsHeldWordWave(listOf(
            LyricsHeldWordUnit("se", 1000, 1250, 0f, 30f),
            LyricsHeldWordUnit("la", 1250, 1550, 30f, 60f),
            LyricsHeldWordUnit("ma", 1550, 1850, 60f, 90f),
            LyricsHeldWordUnit("nya", 1850, 7000, 90f, 120f)
        ))
        val atCrest = (0..3).map { index ->
            wave.pose(index, 4100, 15f + 30f * index)
        }
        // A broad central crest with lower left/right foothills is visibly
        // curved, unlike a flat translated word or a straight sheen.
        assertTrue("middle syllable rises more than left edge",
            atCrest[1].liftDp < atCrest[0].liftDp - 2f)
        assertTrue("middle syllable rises more than right edge",
            atCrest[2].liftDp < atCrest[3].liftDp - 2f)
        assertTrue("sustained lyric forms a clearly visible peak",
            minOf(atCrest[1].liftDp, atCrest[2].liftDp) < -7f)
        assertTrue("all syllables must have the same clean rotation",
            atCrest.all { it.rotationDegrees == 0f })
        assertTrue("all syllables must expand uniformly",
            atCrest.all { abs(it.depth - atCrest[0].depth) < 0.00001f })
        // The crest travels from left to right on authored playback time.
        val earlyLeft = wave.pose(0, 2800, 15f).liftDp
        val earlyRight = wave.pose(3, 2800, 105f).liftDp
        val lateLeft = wave.pose(0, 5650, 15f).liftDp
        val lateRight = wave.pose(3, 5650, 105f).liftDp
        assertTrue(earlyLeft < earlyRight)
        assertTrue(lateRight < lateLeft)
        assertTrue("luminous crest travels through the word, not along a flat stripe",
            wave.pose(3, 5650, 105f).crestDp > wave.pose(3, 2800, 105f).crestDp)
    }

    @Test fun mountainDoesNotDisturbShortRapOrContinuePastHold() {
        val wave = LyricsHeldWordWave(listOf(
            LyricsHeldWordUnit("quick ", 500, 800, 0f, 40f),
            LyricsHeldWordUnit("held", 800, 5800, 46f, 116f)
        ))
        assertEquals(LyricsHeldWordPose(), wave.pose(0, 3200, 20f))
        assertEquals(LyricsHeldWordPose(), wave.pose(1, 800, 80f))
        assertEquals(LyricsHeldWordPose(), wave.pose(1, 5800, 80f))
        val middle = wave.pose(1, 3200, 80f)
        repeat(10) { assertEquals(middle, wave.pose(1, 3200, 80f)) }
    }

    @Test fun fastAdjacentWordsNeverDevelopPulsingDepthOrParallax() {
        val words = LyricsHeldWordWave(listOf(
            LyricsHeldWordUnit("hello ", 1000, 1480, 0f, 65f),
            LyricsHeldWordUnit("there ", 1480, 2100, 70f, 130f),
            LyricsHeldWordUnit("again", 2100, 2990, 140f, 206f)
        ))
        for (time in 0..3600 step 8) {
            for (index in 0..2) {
                val pose = words.pose(index, time, index * 70f + 30f)
                assertEquals(LyricsHeldWordPose(), pose)
            }
        }
    }

    @Test fun forwardProjectionIsFrameStableAndReversibleWithSeek() {
        val wave = phrase()
        for (hz in listOf(60, 90, 120)) {
            var previousDepth = 0f
            var previousParallax = 0f
            for (i in 0..hz * 8) {
                val now = (i * 1000.0 / hz).toInt()
                val pose = wave.pose(2, now, 172f)
                assertTrue(pose.depth in 0f..1f)
                assertTrue("foreground depth jumped at $hz Hz",
                    abs(pose.depth - previousDepth) < 0.02f)
                assertTrue("horizontal parallax jumped at $hz Hz",
                    abs(pose.parallaxXDp - previousParallax) < 0.033f)
                previousDepth = pose.depth
                previousParallax = pose.parallaxXDp
            }
        }
        val saved = wave.pose(2, 4400, 172f)
        wave.pose(2, 6500, 172f)
        assertEquals(saved, wave.pose(2, 4400, 172f))
        assertEquals(0f, wave.pose(2, 7000, 172f).depth, 0f)
    }

    @Test fun typicalOnePointEightSecondSustainGetsVisibleForwardDepth() {
        val word = LyricsHeldWordWave(listOf(
            LyricsHeldWordUnit("loooove", 2_000, 3_800, 0f, 90f)
        ))
        val middle = word.pose(0, 2_760, 45f)
        // Previously a double duration gate at >=2s produced ZERO motion.
        // The 1.8-second vowel should now visibly protrude instead.
        assertTrue("normal sustained vowels must be visually eligible", middle.depth > 0.45f)
        assertTrue(middle.scaleY > 1.004f)
        assertEquals(LyricsHeldWordPose(), word.pose(0, 2_000, 45f))
        assertEquals(LyricsHeldWordPose(), word.pose(0, 3_800, 45f))
    }

    @Test fun forwardWaveKeepsOnePerspectiveAcrossSyllables() {
        val word = LyricsHeldWordWave(listOf(
            LyricsHeldWordUnit("ca", 500, 800, 0f, 30f),
            LyricsHeldWordUnit("hu", 800, 1100, 30f, 60f),
            LyricsHeldWordUnit("aaa", 1100, 5200, 60f, 90f)
        ))
        val left = word.pose(0, 3_150, 15f)
        val right = word.pose(2, 3_150, 75f)
        assertTrue(left.depth > 0.05f)
        assertTrue(right.depth > 0.05f)
        assertEquals("3D perspective must be shared by the whole word",
            left.perspectiveSlope, right.perspectiveSlope, 0f)
        assertEquals("one word means one scale for all syllables",
            left.depth, right.depth, 0f)
        assertEquals(left.wordCenterDp, right.wordCenterDp, 0f)
    }

    @Test fun threeDimensionalLetterSizeShrinksSmoothlyWithoutGrowingAgainAtEveryRefreshRate() {
        val wave = phrase()
        // 5-second held "kamu": 42% attack, 58% uninterrupted release.
        // Each word region must obey the SAME monotonically decreasing
        // authored-time size envelope, even while the mountain crest moves.
        for (hz in listOf(60, 90, 120)) {
            for (center in listOf(145f, 163f, 182f, 203f, 220f)) {
                val peak = 4_100
                var previous = wave.pose(2, peak, center).depth
                assertTrue("3D projection must remain visible at its crest", previous > 0.65f)
                for (frame in 1..(hz * 3)) {
                    val now = minOf(7_000, peak + (frame * 1000.0 / hz).toInt())
                    val pose = wave.pose(2, now, center)
                    assertTrue("size went backwards at $hz Hz, $now ms",
                        pose.depth <= previous + 0.00001f)
                    assertTrue("size reduction skipped a visible frame at $hz Hz, $now ms",
                        previous - pose.depth < 0.018f)
                    // Both face scale and 3D camera use pose.depth, so a
                    // strictly decaying depth cannot create a sudden grow-back.
                    assertEquals(1f + 0.055f * pose.depth, pose.scaleY, 0.000001f)
                    previous = pose.depth
                }
                assertEquals(0f, wave.pose(2, 7_000, center).depth, 0f)
            }
        }
    }

    @Test fun heldWordProjectionNeverPopsAtPeakOrLastFrame() {
        val wave = phrase()
        val beforePeak = wave.pose(2, 4_099, 182f)
        val peak = wave.pose(2, 4_100, 182f)
        val afterPeak = wave.pose(2, 4_101, 182f)
        assertTrue(abs(beforePeak.depth - peak.depth) < 0.00005f)
        assertTrue(abs(afterPeak.depth - peak.depth) < 0.00005f)

        val beforeEnd = wave.pose(2, 6_999, 182f)
        val finished = wave.pose(2, 7_000, 182f)
        assertTrue(beforeEnd.depth < 0.00001f)
        assertEquals(0f, finished.depth, 0f)
        assertEquals(1f, finished.scaleY, 0f)
        assertEquals(0f, finished.parallaxXDp, 0f)
        assertEquals(1f, heldWordSizeRelease(0f), 0f)
        assertEquals(0f, heldWordSizeRelease(1f), 0f)
    }

    @Test fun adjacentHeldSyllablesShrinkIndependentlyOfMovingLightCrest() {
        val wave = LyricsHeldWordWave(listOf(
            LyricsHeldWordUnit("se", 1_000, 1_250, 0f, 30f),
            LyricsHeldWordUnit("la", 1_250, 1_550, 30f, 60f),
            LyricsHeldWordUnit("ma", 1_550, 1_850, 60f, 90f),
            LyricsHeldWordUnit("nya", 1_850, 7_000, 90f, 120f)
        ))
        for (i in 0..3) {
            val center = 15f + 30f * i
            var previous = wave.pose(i, 4_100, center).depth
            for (now in 4_116..7_000 step 16) {
                val next = wave.pose(i, now, center).depth
                assertTrue("syllable $i popped larger at $now ms", next <= previous + 0.00001f)
                previous = next
            }
        }
    }

    @Test fun heldWaveCurvesUpWithoutHorizontallyDisturbingLetterSpacing() {
        val wave = LyricsHeldWordWave(listOf(
            LyricsHeldWordUnit("se", 1_000, 1_280, 0f, 28f),
            LyricsHeldWordUnit("la", 1_280, 1_610, 28f, 56f),
            LyricsHeldWordUnit("ma", 1_610, 1_900, 56f, 84f),
            LyricsHeldWordUnit("nya", 1_900, 7_000, 84f, 112f)
        ))
        for (hz in listOf(60, 90, 120)) {
            for (frame in 0..hz * 8) {
                val now = (frame * 1000f / hz).toInt()
                for (index in 0..3) {
                    val pose = wave.pose(index, now, 14f + 28f * index)
                    assertEquals("no sideways drift means no syllable crowding",
                        0f, pose.parallaxXDp, 0f)
                    assertTrue("wave must not stretch glyph heights",
                        pose.scaleY in 1f..1.056f)
                    assertTrue("perspective roll should stay understated",
                        pose.rotationDegrees == 0f)
                    assertTrue("the curve should travel upwards, never bounce down",
                        pose.liftDp in -8.11f..0f)
                }
            }
        }
        val duringHold = (0..3).map { index ->
            wave.pose(index, 4_200, 14f + 28f * index).liftDp
        }
        assertTrue("a visible rounded hill rises in the middle",
            minOf(duringHold[1], duringHold[2]) < -6.2f)
        assertTrue(duringHold[1] < duringHold[0] - 1.5f)
        assertTrue(duringHold[2] < duringHold[3] - 1.5f)
    }

    @Test fun everySyllableSharesOneExpansionAndOneWordCenterAt120Hz() {
        val wave = LyricsHeldWordWave(listOf(
            LyricsHeldWordUnit("se", 1_000, 1_300, 0f, 30f),
            LyricsHeldWordUnit("la", 1_300, 1_600, 30f, 60f),
            LyricsHeldWordUnit("ma", 1_600, 1_850, 60f, 90f),
            LyricsHeldWordUnit("nya", 1_850, 7_000, 90f, 120f)
        ))
        for (hz in listOf(60, 90, 120)) {
            for (frame in 0..hz * 8) {
                val t = (frame * 1000.0 / hz).toInt()
                val samples = (0..3).map { i -> wave.pose(i, t, 15f + i * 30f) }
                for (pose in samples) {
                    assertEquals(samples[0].depth, pose.depth, 0f)
                    assertEquals(samples[0].scaleY, pose.scaleY, 0f)
                    assertEquals(samples[0].wordCenterDp, pose.wordCenterDp, 0f)
                    assertEquals(0f, pose.rotationDegrees, 0f)
                }
            }
        }
        val held = (0..3).map { i -> wave.pose(i, 4_100, 15f + i * 30f) }
        assertTrue(held.all { it.depth > 0.9f })
        assertTrue(held.all { it.scaleY > 1.05f })
        // Only the vertical hill differs between syllables, preserving
        // one stable typographic size across the entire expanding word.
        assertTrue(held[1].liftDp < held[0].liftDp - 2f)
        assertTrue(held[2].liftDp < held[3].liftDp - 2f)
    }

    @Test fun everyMeasuredRunUsesExactlyTheSameScreenPixelPivot() {
        for (density in listOf(1f, 2f, 3.25f)) {
            val startsDp = listOf(0f, 30f, 60f, 90f)
            val initialX = 24f
            val reference = heldWordPivotX(initialX, 0f, 60f, density)
            for (pathStart in startsDp) {
                val glyphX = initialX + pathStart * density
                assertEquals(reference,
                    heldWordPivotX(glyphX, pathStart, 60f, density), 0.00001f)
            }
        }
    }

    @Test fun fullWordExpansionReturnsToBaseBeforeNextLyricWithoutEndPop() {
        val wave = LyricsHeldWordWave(listOf(
            LyricsHeldWordUnit("long ", 2_000, 7_000, 0f, 90f),
            LyricsHeldWordUnit("next", 7_000, 7_500, 96f, 160f)
        ))
        for (hz in listOf(60, 90, 120)) {
            var previous = wave.pose(0, 4_100, 45f).depth
            for (frame in 1..hz * 3) {
                val time = minOf(7_000, 4_100 + (frame * 1000.0 / hz).toInt())
                val pose = wave.pose(0, time, 45f)
                assertTrue("shrink must be monotonic and never restart at " + time,
                    pose.depth <= previous + 0.000001f)
                assertTrue("must not change more than 1.8% in one frame",
                    previous - pose.depth < 0.018f)
                previous = pose.depth
            }
        }
        assertTrue(wave.pose(0, 6_950, 45f).depth < 0.01f)
        assertTrue(wave.pose(0, 6_999, 45f).depth < 0.00001f)
        assertEquals(LyricsHeldWordPose(), wave.pose(0, 7_000, 45f))
        assertEquals(LyricsHeldWordPose(), wave.pose(1, 7_100, 120f))
    }

    @Test fun travellingForwardBulgeFollowsHeldMountainWithoutChangingHorizontalTypography() {
        val wave = LyricsHeldWordWave(listOf(
            LyricsHeldWordUnit("se", 1000, 1300, 0f, 30f),
            LyricsHeldWordUnit("la", 1300, 1600, 30f, 60f),
            LyricsHeldWordUnit("ma", 1600, 1900, 60f, 90f),
            LyricsHeldWordUnit("nya", 1900, 7000, 90f, 120f)
        ))
        fun bulge(index: Int, at: Int) = wave.pose(index, at, 15f + 30f * index).let {
            heldWordForwardBulge(it.depth, it.intensity)
        }
        // The bulge starts on the left and travels to the right, while the
        // unmodified word-depth remains shared by all measured text runs.
        assertTrue(bulge(0, 2800).scaleY > bulge(3, 2800).scaleY)
        assertTrue(bulge(3, 5650).scaleY > bulge(0, 5650).scaleY)
        assertEquals(wave.pose(0, 4200, 15f).depth, wave.pose(3, 4200, 105f).depth, 0f)
        assertEquals(HeldWordForwardBulge(1f, 0f), bulge(0, 1000))
        assertEquals(HeldWordForwardBulge(1f, 0f), bulge(3, 7000))

        // No position-dependent horizontal scale or per-character slices.
        // At 120 Hz the projection and release do not jump or restart.
        for (hz in listOf(60, 90, 120)) {
            var previous = bulge(2, 0)
            for (frame in 1..hz * 8) {
                val at = (frame * 1000.0 / hz).toInt()
                val current = bulge(2, at)
                assertTrue("vertical growth jumped at $hz Hz, $at ms",
                    abs(current.scaleY - previous.scaleY) < 0.0025f)
                assertTrue("camera projection jumped at $hz Hz, $at ms",
                    abs(current.cameraDepth - previous.cameraDepth) < 0.025f)
                assertTrue("forward growth exceeds restrained limits",
                    current.scaleY in 1f..1.088f)
                previous = current
            }
        }
    }

}
