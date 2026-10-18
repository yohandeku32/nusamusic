package com.yohandeku32.nusamusic.ui

import kotlin.math.abs
import org.junit.Assert.*
import org.junit.Test

class LyricsPhraseAnalysisTest {
    private fun phrase(durations: List<Int>, gap: Int = 0): List<LyricsExpressionTiming> {
        var start = 1000
        return durations.map { duration ->
            LyricsExpressionTiming(start, start + duration).also { start += duration + gap }
        }
    }

    @Test fun rapPopAndBalladHaveDifferentContextWithoutChangingDurationWeights() {
        val rap = lyricsAdaptiveExpressions(phrase(List(8) { 180 }))
        val pop = lyricsAdaptiveExpressions(phrase(List(8) { 800 }))
        val ballad = lyricsAdaptiveExpressions(phrase(List(8) { 2400 }))
        assertTrue(rap.all { it.glow == 0f && it.wave == 0f && it.phraseStrength < 0.5f })
        assertTrue(pop.all { it.glow > 0.06f && it.glow < 0.34f })
        assertTrue(ballad.all { it.glow > 0.9f && it.phraseStrength == 1f })
    }

    @Test fun neighbouringDurationsCrossOldThresholdsWithoutChangingTheWholePhraseAbruptly() {
        for (boundary in listOf(250, 600, 700, 1000, 1800)) {
            val a = lyricsAdaptiveExpressions(phrase(listOf(350, 350, boundary - 1, 350, 3000)))
            val b = lyricsAdaptiveExpressions(phrase(listOf(350, 350, boundary + 1, 350, 3000)))
            for (i in a.indices) {
                assertTrue(abs(a[i].phraseStrength - b[i].phraseStrength) < 0.01f)
                assertTrue(abs(a[i].upcomingHold - b[i].upcomingHold) < 0.01f)
            }
        }
    }

    @Test fun lookAheadPreparesTheCurrentNoteButNeverActivatesTheUpcomingHold() {
        val timings = phrase(listOf(350, 350, 350, 4000))
        val profiles = lyricsAdaptiveExpressions(timings)
        val current = profiles[2]
        assertTrue(current.upcomingHold > 0.9f)
        val withoutLookAhead = LyricsAdaptiveExpression(350, current.phraseStrength)
        val now = timings[2].end - 1
        assertTrue(current.phraseAt(timings[2].start, now) >
            withoutLookAhead.phraseAt(timings[2].start, now))
        val future = LyricsExpressionController(timings[3].start, timings[3].end, profiles[3])
        future.update(now)
        assertEquals(0f, future.glow, 0f)
        val mask = LyricsTravelingWaveMask(timings.mapIndexed { i, t ->
            LyricsVocalSpan(t.start, t.end, i * 60f, i * 60f + 50f)
        })
        assertEquals(0f, mask.glyphInfluence(mask.front(now), 180f, 230f), 0f)
    }

    @Test fun phraseInfluenceFadesContinuouslyAcrossPausesAndStopsAtInstrumentalGaps() {
        for (gap in listOf(160, 240, 449)) {
            val a = lyricsAdaptiveExpressions(phrase(List(6) { 350 }, gap - 1))
            val b = lyricsAdaptiveExpressions(phrase(List(6) { 350 }, gap + 1))
            assertTrue(abs(a[2].phraseStrength - b[2].phraseStrength) < 0.01f)
        }
        val separated = lyricsAdaptiveExpressions(phrase(listOf(350, 350, 4000), 3000))
        assertTrue(separated.all { it.phraseStrength == 1f && it.upcomingHold == 0f })
    }

    @Test fun overlappingVoicesAndInferredMetadataCannotSupplyFalsePhraseEvidence() {
        val overlap = lyricsAdaptiveExpressions(listOf(
            LyricsExpressionTiming(1000, 5000), LyricsExpressionTiming(1500, 1800)))
        assertTrue(overlap.all { it.phraseStrength == 1f })
        val inferred = lyricsAdaptiveExpressions(listOf(
            LyricsExpressionTiming(1000, 1350), LyricsExpressionTiming(1350, 20000, false)))
        assertEquals(0f, inferred.first().upcomingHold, 0f)
        assertEquals(0f, inferred.last().breathing, 0f)
        assertTrue(inferred.last().glow <= 0.06f)
    }
}
