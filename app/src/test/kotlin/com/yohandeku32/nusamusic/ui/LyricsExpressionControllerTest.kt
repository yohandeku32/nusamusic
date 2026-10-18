package com.yohandeku32.nusamusic.ui

import kotlin.math.abs
import org.junit.Assert.*
import org.junit.Test

class LyricsExpressionControllerTest {
    @Test fun consecutiveNotesShareASoftOutgoingHaloWithoutEarlyHighlightOrGlow() {
        val profiles = lyricsAdaptiveExpressions(listOf(
            LyricsExpressionTiming(1000, 2000), LyricsExpressionTiming(2000, 5000)))
        val outgoing = LyricsExpressionController(1000, 2000, profiles[0])
        val incoming = LyricsExpressionController(2000, 5000, profiles[1])
        outgoing.update(2000)
        incoming.update(2000)
        assertTrue(outgoing.glow > 0.15f)
        assertEquals(0f, incoming.glow, 0f)
        incoming.update(2001)
        assertTrue(incoming.glow < 0.001f)
        outgoing.update(2160)
        assertEquals(0f, outgoing.glow, 0f)
        incoming.update(3500)
        assertTrue(incoming.glow > 0.85f)
    }

    @Test fun trueGapsReleaseInsideTheAuthoredWindowAndFastNotesNeverBloom() {
        val profiles = lyricsAdaptiveExpressions(listOf(
            LyricsExpressionTiming(1000, 2000), LyricsExpressionTiming(5000, 5180)))
        val outgoing = LyricsExpressionController(1000, 2000, profiles[0])
        outgoing.update(2000)
        assertEquals(0f, outgoing.glow, 0f)
        val fast = LyricsExpressionController(5000, 5180, profiles[1])
        for (now in 4900..5400 step 10) {
            fast.update(now)
            assertEquals(0f, fast.glow, 0f)
            assertEquals(0f, fast.wave, 0f)
        }
    }

    @Test fun glowHandoffsStayContinuousAt60_90_120Hz() {
        val timings = listOf(LyricsExpressionTiming(1000, 2000),
            LyricsExpressionTiming(2000, 4000), LyricsExpressionTiming(4000, 4350),
            LyricsExpressionTiming(4350, 9000))
        val profiles = lyricsAdaptiveExpressions(timings)
        for (hz in listOf(60, 90, 120)) {
            val controllers = timings.mapIndexed { i, t -> LyricsExpressionController(t.start, t.end, profiles[i]) }
            val previous = FloatArray(controllers.size)
            for (frame in 0..hz * 10) {
                val now = (frame * 1000.0 / hz).toInt()
                controllers.forEachIndexed { i, controller ->
                    controller.update(now)
                    assertTrue(controller.glow in 0f..1f)
                    assertTrue("Halo flicker at $hz Hz / $now ms", abs(controller.glow - previous[i]) < 0.1f)
                    if (now <= timings[i].start) assertEquals(0f, controller.glow, 0f)
                    previous[i] = controller.glow
                }
            }
        }
    }

    @Test fun samplingIsDeterministicAcrossPauseAndForwardBackwardSeeks() {
        val controller = LyricsExpressionController(1000, 4000,
            LyricsAdaptiveExpression(3000, 0.4f, 1f, 4000))
        controller.update(1400)
        val glow = controller.glow
        val wave = controller.wave
        repeat(120) {
            controller.update(1400)
            assertEquals(glow, controller.glow, 0f)
            assertEquals(wave, controller.wave, 0f)
        }
        controller.update(9000)
        assertEquals(0f, controller.glow, 0f)
        controller.update(0)
        assertEquals(0f, controller.glow, 0f)
        controller.update(1400)
        assertEquals(glow, controller.glow, 0f)
        assertEquals(wave, controller.wave, 0f)
    }
}
