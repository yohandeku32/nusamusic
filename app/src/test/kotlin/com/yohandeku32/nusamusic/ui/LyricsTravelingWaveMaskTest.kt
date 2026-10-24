package com.yohandeku32.nusamusic.ui

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs

class LyricsTravelingWaveMaskTest {
    private val spans = listOf(
        LyricsVocalSpan(1000, 2200, 0f, 100f),
        LyricsVocalSpan(2200, 3000, 110f, 180f),
        LyricsVocalSpan(3000, 7000, 190f, 350f)
    )
    private val mask = LyricsTravelingWaveMask(spans)

    @Test fun oneWaveMovesWholeWordsWithoutRestartAtSyllableBoundary() {
        val waves = LyricsTravelingWaveMask(listOf(
            LyricsVocalSpan(1000, 1270, 0f, 45f),
            LyricsVocalSpan(1270, 1510, 45f, 90f),
            LyricsVocalSpan(1510, 1810, 100f, 150f)
        ))
        val units = listOf(
            LyricsHeldWordUnit("ba", 1000, 1270, 0f, 20f),
            LyricsHeldWordUnit("ru ", 1270, 1510, 20f, 45f),
            LyricsHeldWordUnit("lagi", 1510, 1810, 100f, 150f)
        )
        val centers = lyricCoherentWordCenters(units)
        assertEquals(22.5f, centers[0], 0f)
        assertEquals(22.5f, centers[1], 0f)
        assertEquals(125f, centers[2], 0f)
        var previous = 0f
        for (time in 1000..2070 step 8) {
            val current = waves.coherentInfluence(waves.front(time), centers[0], time)
            assertTrue(current in 0f..1f)
            assertTrue("Jolt at ${time}ms", abs(current - previous) < 0.09f)
            previous = current
        }
        val before = waves.coherentInfluence(waves.front(1269), centers[0], 1269)
        val after = waves.coherentInfluence(waves.front(1270), centers[0], 1270)
        assertTrue(abs(before - after) < 0.04f)
        assertEquals(0f, waves.coherentInfluence(waves.front(950), centers[0], 950), 0f)
        assertTrue(waves.coherentInfluence(waves.front(1400), centers[0], 1400) > 0f)
        assertEquals(0f, waves.coherentInfluence(waves.front(2100), centers[0], 2100), 0f)
    }

    @Test fun ordinaryShortSyllablesReceiveASmoothPassingWave() {
        val fast = LyricsTravelingWaveMask(listOf(
            LyricsVocalSpan(1000, 1270, 0f, 45f),
            LyricsVocalSpan(1270, 1510, 45f, 90f)
        ))
        val values = (1000..1770 step 10).map { time ->
            fast.glyphInfluence(fast.front(time), 0f, 45f)
        }
        assertEquals(0f, values.first(), 0f)
        assertTrue(values.maxOrNull()!! > 0.7f)
        assertEquals(0f, values.last(), 0f)
        assertTrue(values.all { it in 0f..1f })
    }
    @Test fun nothingMovesBeforeFirstSyllableOrFarAheadOfVoice() {
        assertNull(mask.front(999))
        val active = requireNotNull(mask.front(1500))
        assertEquals(41.6667f, active.positionDp, 0.01f)
        assertEquals(0f, mask.influence(active, 110f, 125f), 0f)
        assertEquals(0f, mask.influence(active, 220f, 240f), 0f)
        assertTrue(mask.influence(active, 15f, 28f) > 0f)
    }

    @Test fun localMaskFollowsProgressWithinOneLongSyllable() {
        val held = LyricsTravelingWaveMask(
            listOf(LyricsVocalSpan(1000, 11000, 0f, 200f))
        )
        val first = requireNotNull(held.front(1500))
        val middle = requireNotNull(held.front(6000))
        val late = requireNotNull(held.front(10500))
        assertEquals(0f, held.influence(first, 75f, 90f), 0f)
        assertTrue(held.influence(middle, 90f, 108f) > 0.5f)
        assertEquals(0f, held.influence(middle, 175f, 195f), 0f)
        assertTrue(held.influence(late, 175f, 195f) > 0.5f)
        assertTrue(held.influence(late, 0f, 15f) < 0.01f)
    }

    @Test fun adjacentSyllablesShareAContinuousTravelingFront() {
        val before = requireNotNull(mask.front(2199))
        val at = requireNotNull(mask.front(2200))
        assertTrue(abs(before.positionDp - at.positionDp) <= 10.2f)
        val trailingAtBoundary = mask.influence(at, 85f, 100f)
        val trailingBefore = mask.influence(before, 85f, 100f)
        assertTrue(abs(trailingAtBoundary - trailingBefore) < 0.06f)
        // The next syllable itself must not move until its front arrives.
        assertEquals(0f, mask.influence(at, 110f, 125f), 0f)
    }

    @Test fun completedTextReturnsSmoothlyToBaseline() {
        val wave = LyricsTravelingWaveMask(listOf(LyricsVocalSpan(1000, 2000, 0f, 100f)))
        val end = requireNotNull(wave.front(2000))
        val easingOut = requireNotNull(wave.front(2130))
        val nearlyDone = requireNotNull(wave.front(2250))
        assertTrue(mask.influence(null, 50f, 65f) == 0f)
        assertTrue(wave.influence(end, 90f, 100f) > wave.influence(easingOut, 90f, 100f))
        assertTrue(wave.influence(easingOut, 90f, 100f) > wave.influence(nearlyDone, 90f, 100f))
        assertNull(wave.front(2261))
    }

    @Test fun vocalGapDoesNotLeaveWaveRunningForever() {
        val front = requireNotNull(mask.front(3001))
        assertTrue(front.positionDp in 180f..190.1f)
        assertNull(mask.front(7300))
    }

    @Test fun shortAndInvalidSyllablesAreHandledSafely() {
        val wave = LyricsTravelingWaveMask(
            listOf(
                LyricsVocalSpan(5000, 5000, 0f, 200f),
                LyricsVocalSpan(5000, 5100, 200f, 200f),
                LyricsVocalSpan(5100, 5200, 220f, 260f)
            )
        )
        assertNull(wave.front(5050))
        assertNotNull(wave.front(5150))
    }

    @Test fun playbackPauseAndSeekDoNotResetWavePhase() {
        val motion = LyricsFloatingMotion(100f)
        val t = 2600
        val front = requireNotNull(mask.front(t))
        val intensity = mask.influence(front, 125f, 145f)
        motion.update(t, intensity)
        val offset = motion.liftDp
        val scale = motion.scale
        repeat(120) {
            motion.update(t, intensity)
            assertEquals(offset, motion.liftDp, 0f)
            assertEquals(scale, motion.scale, 0f)
        }
        motion.update(5900, mask.influence(mask.front(5900), 260f, 275f))
        motion.update(t, intensity)
        assertEquals(offset, motion.liftDp, 0f)
        assertEquals(scale, motion.scale, 0f)
    }

    @Test fun motionRemainsBoundedAt60_90_120HzNearWordBoundary() {
        for (hz in listOf(60, 90, 120)) {
            val motion = LyricsFloatingMotion(120f)
            var lastLift = 0f
            var lastScale = 1f
            for (frame in 0..hz * 3) {
                val now = 1700 + (frame * 1000.0 / hz).toInt()
                val local = mask.influence(mask.front(now), 115f, 130f)
                motion.update(now, local * lyricsLineWaveIntensity(1000, 7000, now))
                assertTrue(motion.scale in 1f..1.02f)
                assertTrue(abs(motion.liftDp) <= 2.15f)
                assertTrue("wave jumped: Hz=$hz time=$now", abs(motion.liftDp - lastLift) < 0.24f)
                assertTrue(abs(motion.scale - lastScale) < 0.004f)
                lastLift = motion.liftDp
                lastScale = motion.scale
            }
        }
    }
}
