package com.yohandeku32.nusamusic.ui

import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine
import com.yohandeku32.nusamusic.data.LyricsDecoder
import org.junit.Assert.*
import org.junit.Test

/** Synthetic embedded TTML fixtures; these assert timing/continuity, not device FPS or vocal loudness. */
class LyricsDynamicEngineScenariosTest {
    private fun metadata(durations: List<Int>, gap: Int = 0): String {
        var start = 1000
        return buildString {
            append("<tt><body><p begin=\"1000ms\" end=\"60000ms\">")
            durations.forEachIndexed { i, duration ->
                append("<span begin=\"${start}ms\" end=\"${start + duration}ms\">word$i </span>")
                start += duration + gap
            }
            append("</p></body></tt>")
        }
    }

    private fun checkScenario(durations: List<Int>, gap: Int = 0) {
        val decoded = LyricsDecoder.decodeWithTiming(metadata(durations, gap), 60000)
        val line = decoded.lyrics.lines.single() as KaraokeLine
        val profiles = lyricsAdaptiveLineExpressions(listOf(line), decoded.inferredEnds).getValue(line)
        val timings = line.syllables.map { LyricsExpressionTiming(it.start, it.end) }
        val controllers = timings.mapIndexed { i, t -> LyricsExpressionController(t.start, t.end, profiles[i]) }
        val mask = LyricsTravelingWaveMask(timings.mapIndexed { i, t ->
            LyricsVocalSpan(t.start, t.end, i * 60f, i * 60f + 50f)
        })
        val timeline = LyricsTimeline(decoded.lyrics.lines)
        val closing = LyricsClosingVocalMotion(timings, profiles, 150f)
        for (now in 0..line.end + 500 step 16) {
            closing.update(now)
            assertTrue(closing.scaleX in 1f..1.045f)
            controllers.forEachIndexed { i, controller ->
                controller.update(now)
                assertTrue(controller.glow.isFinite() && controller.glow in 0f..1f)
                val unit = line.syllables[i]
                assertEquals(((now.toLong() - unit.start).toFloat() / (unit.end.toLong() - unit.start))
                    .coerceIn(0f, 1f), unit.progress(now), 0.0001f)
                if (now <= unit.start) {
                    assertEquals(0f, controller.glow, 0f)
                    assertEquals(0f, mask.glyphInfluence(mask.front(now), i * 60f, i * 60f + 50f), 0f)
                }
                if (durations[i] < 250) assertEquals(0f, controller.glow, 0f)
            }
        }
        if (gap >= 3000) assertNotNull(timeline.frame(line.syllables.first().end + 1000).gap)
    }

    @Test fun rapFixtureKeepsRapidSyllablesMinimal() = checkScenario(List(12) { 180 })
    @Test fun popFixtureHasBalancedMediumExpression() = checkScenario(List(8) { 800 })
    @Test fun balladFixtureAllowsSlowBreathing() = checkScenario(List(5) { 1600 })
    @Test fun veryLongVocalFixtureRetainsFullClosingPhraseMotion() = checkScenario(listOf(400, 600, 9000))
    @Test fun mixedFixtureTransitionsBetweenFastMediumAndLongPhrases() =
        checkScenario(listOf(180, 180, 350, 350, 2400, 800, 180, 180, 4000))
    @Test fun instrumentalFixtureKeepsAuthoredSilence() = checkScenario(listOf(800, 2400, 800), 4000)

    @Test fun backingVoicesKeepTheirOwnExpressionAndUnionVocalGaps() {
        val decoded = LyricsDecoder.decodeWithTiming("""<tt xmlns:ttm="http://www.w3.org/ns/ttml#metadata"><body>
            <p begin="1s" end="5s"><span begin="1s" end="2s">Main</span>
            <span begin="1.5s" end="5s" ttm:role="x-bg"><span begin="1.5s" end="5s">Oh</span></span></p>
            <p begin="9s" end="11s"><span begin="9s" end="11s">Next</span></p>
            </body></tt>""", 15000)
        val backing = decoded.lyrics.lines.filterIsInstance<KaraokeLine>().first { it.isAccompaniment }
        val profiles = lyricsAdaptiveLineExpressions(listOf(backing), decoded.inferredEnds).getValue(backing)
        assertTrue(profiles.single().glow > 0.9f)
        assertNull(LyricsTimeline(decoded.lyrics.lines).frame(3000).gap)
        assertNotNull(LyricsTimeline(decoded.lyrics.lines).frame(6000).gap)
        val plan = LyricsVisualFocusController(decoded.lyrics.lines)
        plan.update(3000)
        assertEquals(0, plan.fromSource)
    }
}
