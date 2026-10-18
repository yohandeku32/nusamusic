package com.yohandeku32.nusamusic.ui

import kotlin.math.abs
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine
import com.yohandeku32.nusamusic.data.LyricsDecoder
import org.junit.Assert.*
import org.junit.Test

class LyricsAdaptiveExpressionTest {
    @Test fun aPhraseCanCrossRowsWithoutChangingProgressOrTrustingAnInferredTail() {
        val decoded = LyricsDecoder.decodeWithTiming(
            "[00:00]<00:00>a <00:00.350>b<00:00.700>\n" +
                "[00:00.700]<00:00.700>c <00:01.050>hold<00:05.050>\n" +
                "[00:06]<00:06>tail", 30000)
        val lines = decoded.lyrics.lines.filterIsInstance<KaraokeLine>()
        val profiles = lyricsAdaptiveLineExpressions(lines, decoded.inferredEnds)
        assertTrue(profiles.getValue(lines[0]).first().phraseStrength < 1f)
        assertTrue(profiles.getValue(lines[1]).last().phraseStrength < 1f)
        assertTrue(profiles.getValue(lines[2]).single().glow <= 0.06f)
        assertEquals(0.5f, lines[1].syllables.last().progress(3050), 0.0001f)
        assertEquals(listOf(0, 700, 6000), lines.map { it.start })
    }

    @Test fun shortNotesOnlyHighlightAndLongHoldsGetTheStrongestExpression() {
        for (duration in listOf(0L, 40L, 180L, 249L, 250L)) {
            val p = LyricsAdaptiveExpression(duration)
            assertEquals(0f, p.glow, 0f)
            assertEquals(0f, p.wave, 0f)
            assertEquals(0f, p.breathing, 0f)
            assertEquals(0f, p.elastic, 0f)
            assertEquals(0f, p.vibration, 0f)
        }
        val durations = listOf(350L, 800L, 1400L, 2200L, 3200L)
        val profiles = durations.map { LyricsAdaptiveExpression(it) }
        assertTrue(profiles.zipWithNext().all { (a, b) -> a.glow < b.glow })
        assertTrue(profiles.first().glow < 0.02f)
        assertTrue(profiles[1].breathing < 0.1f)
        assertEquals(0f, profiles[2].vibration, 0f)
        assertEquals(1f, profiles.last().glow, 0f)
        assertEquals(1f, profiles.last().elastic, 0f)
    }

    @Test fun allDurationBandsInterpolateWithoutThresholdJumps() {
        for (boundary in listOf(250L, 600L, 700L, 1000L, 1800L, 2600L)) {
            val a = LyricsAdaptiveExpression(boundary - 1)
            val b = LyricsAdaptiveExpression(boundary + 1)
            assertTrue(abs(a.glow - b.glow) < 0.004f)
            assertTrue(abs(a.wave - b.wave) < 0.004f)
            assertTrue(abs(a.breathing - b.breathing) < 0.004f)
            assertTrue(abs(a.elastic - b.elastic) < 0.004f)
            assertTrue(abs(a.vibration - b.vibration) < 0.004f)
        }
    }

    @Test fun aRapidPhraseIsCalmerThanIsolatedNotesAndRecoversGraduallyOnAHold() {
        val timings = (0..5).map { LyricsExpressionTiming(it * 350, it * 350 + 350) } +
            LyricsExpressionTiming(2100, 6100)
        val profiles = lyricsAdaptiveExpressions(timings)
        assertTrue(profiles.take(6).all { it.phraseStrength <= 0.41f })
        val hold = profiles.last()
        assertTrue(hold.phraseAt(2100, 2100) < 0.5f)
        assertTrue(hold.phraseAt(2100, 2500) in 0.6f..0.8f)
        assertEquals(1f, hold.phraseAt(2100, 2900), 0f)
        val separated = lyricsAdaptiveExpressions(listOf(
            LyricsExpressionTiming(0, 350), LyricsExpressionTiming(1000, 1350),
            LyricsExpressionTiming(2000, 2350)))
        assertTrue(separated.all { it.phraseStrength == 1f })
    }

    @Test fun anInferredMetadataTailNeverBecomesALongVocalAccent() {
        val profiles = lyricsAdaptiveExpressions(listOf(
            LyricsExpressionTiming(0, 30000, reliableEnd = false),
            LyricsExpressionTiming(31000, 61000, reliableEnd = true)))
        val inferred = profiles.first()
        assertTrue(inferred.glow <= 0.06f)
        assertEquals(0f, inferred.breathing, 0f)
        assertEquals(0f, inferred.elastic, 0f)
        assertEquals(0f, inferred.vibration, 0f)
        val bubble = LyricsLongVocalMotion(0, 30000, 100f, inferred)
        bubble.update(10000, 1f)
        assertEquals(1f, bubble.scaleX, 0f)
        assertEquals(0f, bubble.luminousCrescendo, 0f)
        assertEquals(1f, profiles.last().glow, 0f)
    }

    @Test fun fastToSlowPhrasesStaySmoothAtEverySupportedRefreshRate() {
        val timings = (0..5).map { LyricsExpressionTiming(it * 350, it * 350 + 350) } +
            LyricsExpressionTiming(2100, 6100)
        val profiles = lyricsAdaptiveExpressions(timings)
        for (hz in listOf(60, 90, 120)) {
            var previous = 0f
            for (frame in 0..hz * 7) {
                val now = (frame * 1000.0 / hz).toInt()
                var glow = 0f
                for (i in timings.indices) {
                    val t = timings[i]
                    glow += profiles[i].glow * profiles[i].envelope(t.start, t.end, now) *
                        profiles[i].phraseAt(t.start, now)
                }
                assertTrue(glow in 0f..1f)
                assertTrue("Glow jumped at $hz Hz / $now ms", abs(glow - previous) < 0.09f)
                previous = glow
            }
        }
    }

    @Test fun pauseAndSeekReproduceTheSameAdaptivePose() {
        val p = LyricsAdaptiveExpression(4000, 0.4f)
        val motion = LyricsLongVocalMotion(1000, 5000, 100f, p)
        motion.update(1400, 1f)
        val pose = listOf(motion.scaleX, motion.scaleY, motion.offsetXDp, motion.luminousCrescendo)
        repeat(50) {
            motion.update(1400, 1f)
            assertEquals(pose, listOf(motion.scaleX, motion.scaleY, motion.offsetXDp, motion.luminousCrescendo))
        }
        motion.update(4500, 1f)
        motion.update(1400, 1f)
        assertEquals(pose, listOf(motion.scaleX, motion.scaleY, motion.offsetXDp, motion.luminousCrescendo))
    }
}
