package com.yohandeku32.nusamusic.ui

import kotlin.math.abs
import org.junit.Assert.*
import org.junit.Test

/** Regression tests for visual-only glow and elastic outline envelopes. */
class LyricsLongVocalCrescendoTest {
    @Test fun shortNotesAndUnheardSyllablesNeverGetExtraBloomOrOutline() {
        for (duration in listOf(0, 120, 650, 700)) {
            val accent = LyricsLongVocalMotion(1000, 1000 + duration, 100f)
            for (now in 850..1900 step 25) {
                accent.update(now, 1f)
                assertEquals(0f, accent.luminousCrescendo, 0f)
                assertEquals(0f, accent.edgeExpansion, 0f)
            }
        }
        val accent = LyricsLongVocalMotion(1000, 11000, 100f)
        accent.update(6400, 0f)
        assertEquals(0f, accent.luminousCrescendo, 0f)
        assertEquals(0f, accent.edgeExpansion, 0f)
    }

    @Test fun crescendoBuildsSlowlyThenFadesWithoutAHardCut() {
        val accent = LyricsLongVocalMotion(1000, 11000, 120f)
        accent.update(1000, 1f)
        assertEquals(0f, accent.luminousCrescendo, 0f)
        accent.update(1500, 1f)
        val early = accent.luminousCrescendo
        accent.update(6300, 1f)
        val middle = accent.luminousCrescendo
        accent.update(10100, 1f)
        val late = accent.luminousCrescendo
        assertTrue(early > 0f)
        assertTrue(middle > early + 0.3f)
        assertTrue(late > middle)
        accent.update(10900, 1f)
        assertTrue(accent.luminousCrescendo < middle)
        accent.update(11000, 1f)
        assertEquals(0f, accent.luminousCrescendo, 0f)
        assertEquals(0f, accent.edgeExpansion, 0f)
    }

    @Test fun visualCrescendoDoesNotImplyOrInspectAudioVolume() {
        // Deterministic from authored timestamps and local vocal influence;
        // no dependence on amplitude, waveform data or audio decoding.
        val first = LyricsLongVocalMotion(1000, 15000, 100f)
        val second = LyricsLongVocalMotion(1000, 15000, 100f)
        first.update(8000, 0.8f)
        second.update(8000, 0.8f)
        assertEquals(first.luminousCrescendo, second.luminousCrescendo, 0f)
        assertEquals(first.edgeExpansion, second.edgeExpansion, 0f)
        assertTrue(first.luminousCrescendo in 0f..1f)
        assertTrue(first.edgeExpansion in 0f..1f)
    }

    @Test fun edgeOutlineMovesContinuouslyAcrossHorizontalLocations() {
        val left = LyricsLongVocalMotion(1000, 15000, 100f)
        val right = LyricsLongVocalMotion(1000, 15000, 115f)
        left.update(9000, 1f)
        right.update(9000, 1f)
        assertTrue(abs(left.edgeExpansion - right.edgeExpansion) < 0.025f)
        assertNotEquals(left.edgeExpansion, right.edgeExpansion, 0.00001f)
        // Main text is still measured and painted once as an intact run.
        // No glyph slices are created by the crescendo envelope.
        assertTrue(left.scaleX in 1f..1.025f)
        assertTrue(right.scaleY in 1f..1.027f)
    }

    @Test fun glowAndElasticContourHaveSmoothFrameToFrameChanges() {
        for (hz in listOf(60, 90, 120)) {
            val accent = LyricsLongVocalMotion(1000, 11000, 120f)
            var previousGlow = 0f
            var previousEdge = 0f
            for (frame in 0..hz * 12) {
                val now = (frame * 1000.0 / hz).toInt()
                accent.update(now, 1f)
                assertTrue(accent.luminousCrescendo in 0f..1f)
                assertTrue(accent.edgeExpansion in 0f..1f)
                assertTrue("glow jumped at $hz Hz / $now ms",
                    abs(accent.luminousCrescendo - previousGlow) < 0.07f)
                assertTrue("edge jumped at $hz Hz / $now ms",
                    abs(accent.edgeExpansion - previousEdge) < 0.08f)
                previousGlow = accent.luminousCrescendo
                previousEdge = accent.edgeExpansion
            }
        }
    }

    @Test fun pauseAndSeekPreserveExactCrescendoPose() {
        val accent = LyricsLongVocalMotion(1000, 12000, 85f)
        accent.update(6200, 0.72f)
        val glow = accent.luminousCrescendo
        val edge = accent.edgeExpansion
        repeat(120) {
            accent.update(6200, 0.72f)
            assertEquals(glow, accent.luminousCrescendo, 0f)
            assertEquals(edge, accent.edgeExpansion, 0f)
        }
        accent.update(9800, 1f)
        accent.update(6200, 0.72f)
        assertEquals(glow, accent.luminousCrescendo, 0f)
        assertEquals(edge, accent.edgeExpansion, 0f)
    }

    @Test fun localWaveMaskRestrictsExtraLightToSungArea() {
        val mask = LyricsTravelingWaveMask(listOf(
            LyricsVocalSpan(1000, 9000, 0f, 150f),
            LyricsVocalSpan(9000, 16000, 165f, 300f)
        ))
        val current = LyricsLongVocalMotion(1000, 9000, 75f)
        val future = LyricsLongVocalMotion(9000, 16000, 235f)
        val now = 4900
        val front = mask.front(now)
        current.update(now, mask.glyphInfluence(front, 0f, 150f))
        future.update(now, mask.glyphInfluence(front, 165f, 300f))
        assertTrue(current.luminousCrescendo > 0f)
        assertEquals(0f, future.luminousCrescendo, 0f)
        assertEquals(0f, future.edgeExpansion, 0f)
    }
}
