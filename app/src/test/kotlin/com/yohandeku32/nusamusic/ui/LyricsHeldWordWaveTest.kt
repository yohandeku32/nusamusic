package com.yohandeku32.nusamusic.ui

import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LyricsHeldWordWaveTest {
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
                    abs(pose.scaleY - before.scaleY) < 0.0005f)
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

    @Test fun shortEligibleHoldsGetSmallerMountainsThanLongHeldVowels() {
        val short = LyricsHeldWordWave(listOf(
            LyricsHeldWordUnit("short", 1000, 2250, 0f, 90f)
        ))
        val long = LyricsHeldWordWave(listOf(
            LyricsHeldWordUnit("long", 1000, 6000, 0f, 90f)
        ))
        val shortPeak = abs(short.pose(0, 1550, 45f).liftDp)
        val longPeak = abs(long.pose(0, 3200, 45f).liftDp)
        assertTrue(shortPeak > 0.1f)
        assertTrue(longPeak > shortPeak * 2f)
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
        assertTrue("opposing sides follow opposing arc tangents",
            atCrest.first().rotationDegrees * atCrest.last().rotationDegrees < 0f)
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

}
