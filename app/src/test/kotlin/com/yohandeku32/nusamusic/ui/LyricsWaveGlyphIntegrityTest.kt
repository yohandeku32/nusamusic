package com.yohandeku32.nusamusic.ui

import kotlin.math.abs
import org.junit.Assert.*
import org.junit.Test

/** Regression for slow-song glyph tearing caused by clipped moving strips. */
class LyricsWaveGlyphIntegrityTest {
    @Test fun futureSyllablesStayAtRestWhileTheCurrentWordMovesAsOne() {
        val mask = LyricsTravelingWaveMask(listOf(
            LyricsVocalSpan(1000, 9000, 0f, 240f),
            LyricsVocalSpan(9000, 13000, 250f, 380f)
        ))
        val before = mask.front(999)
        assertEquals(0f, mask.glyphInfluence(before, 0f, 240f), 0f)
        val first = requireNotNull(mask.front(5000))
        assertEquals(1f, mask.glyphInfluence(first, 0f, 240f), 0.00001f)
        assertEquals(0f, mask.glyphInfluence(first, 250f, 380f), 0f)
    }

    @Test fun longVocalHasNoPerSliceTransformJumpsAt60_90_120Hz() {
        // A complete 240dp syllable uses just ONE transform, independently
        // of the number of letters. No clipping can cut through a stroke.
        val mask = LyricsTravelingWaveMask(
            listOf(LyricsVocalSpan(1000, 11000, 0f, 240f))
        )
        for (hz in listOf(60, 90, 120)) {
            val motion = LyricsFloatingMotion(horizontalDp = 120f)
            var previousLift = 0f
            var previousScale = 1f
            for (frame in 0..hz * 12) {
                val now = (frame * 1000.0 / hz).toInt()
                val influence = mask.glyphInfluence(mask.front(now), 0f, 240f)
                val lineIntensity = lyricsLineWaveIntensity(1000, 11000, now)
                motion.update(now, influence * lineIntensity)
                assertTrue(motion.scale in 1f..1.02f)
                assertTrue(abs(motion.liftDp) <= 2.15f)
                assertTrue("lift jumped at $hz Hz and $now ms",
                    abs(motion.liftDp - previousLift) < 0.16f)
                assertTrue("scale jumped at $hz Hz and $now ms",
                    abs(motion.scale - previousScale) < 0.002f)
                previousLift = motion.liftDp
                previousScale = motion.scale
            }
        }
    }

    @Test fun slowVocalContinuesFloatingDuringItsWholeSyllable() {
        val mask = LyricsTravelingWaveMask(
            listOf(LyricsVocalSpan(1000, 11000, 0f, 240f))
        )
        val motion = LyricsFloatingMotion(horizontalDp = 120f)
        val values = listOf(3000, 5000, 7000, 9000).map { now ->
            val influence = mask.glyphInfluence(mask.front(now), 0f, 240f)
            assertEquals(1f, influence, 0.0001f)
            motion.update(now, influence)
            motion.liftDp
        }
        assertTrue(values.distinct().size > 1)
        assertTrue(values.all { abs(it) <= 2.15f })
    }

    @Test fun beginningAndEndHaveSmoothFalloffWithoutAbruptSwitch() {
        val mask = LyricsTravelingWaveMask(
            listOf(LyricsVocalSpan(1000, 9000, 0f, 240f))
        )
        var previous = 0f
        for (now in 900..9350 step 10) {
            val level = mask.glyphInfluence(mask.front(now), 0f, 240f)
            assertTrue(level in 0f..1f)
            assertTrue("sudden mask change near $now",
                abs(level - previous) <= 0.055f)
            previous = level
        }
        assertEquals(0f, mask.glyphInfluence(mask.front(9500), 0f, 240f), 0f)
    }

    @Test fun pauseAndSeekingKeepTheOriginalV6Phase() {
        val mask = LyricsTravelingWaveMask(
            listOf(LyricsVocalSpan(1000, 11000, 0f, 240f))
        )
        val motion = LyricsFloatingMotion(horizontalDp = 120f)
        val now = 6000
        val influence = mask.glyphInfluence(mask.front(now), 0f, 240f)
        motion.update(now, influence)
        val originalLift = motion.liftDp
        val originalScale = motion.scale

        repeat(60) {
            motion.update(now, influence)
            assertEquals(originalLift, motion.liftDp, 0f)
            assertEquals(originalScale, motion.scale, 0f)
        }
        motion.update(8000, mask.glyphInfluence(mask.front(8000), 0f, 240f))
        motion.update(now, influence)
        assertEquals(originalLift, motion.liftDp, 0f)
        assertEquals(originalScale, motion.scale, 0f)
    }

    @Test fun previousWordReleasesWhileNextWordActivatesSmoothly() {
        val mask = LyricsTravelingWaveMask(listOf(
            LyricsVocalSpan(1000, 3000, 0f, 100f),
            LyricsVocalSpan(3000, 7000, 110f, 260f)
        ))
        val left = requireNotNull(mask.front(3000))
        assertEquals(0f, mask.glyphInfluence(left, 110f, 260f), 0f)
        assertTrue(mask.glyphInfluence(left, 0f, 100f) > 0f)
        val after = requireNotNull(mask.front(3300))
        assertTrue(mask.glyphInfluence(after, 110f, 260f) > 0f)
        assertTrue(mask.glyphInfluence(after, 0f, 100f) < 1f)
    }
}
