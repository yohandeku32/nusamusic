package com.yohandeku32.nusamusic.ui

import org.junit.Assert.assertEquals
import org.junit.Test

/** A short seek must never be mistaken for a backwards clock sample. */
class LyricsFrameTimeSmootherTest {
    @Test fun ignoresOnlyTinyBackwardsClockJitterWithinAnEpoch() {
        val clock = LyricsFrameTimeSmoother()
        assertEquals(1200, clock.sample(1200, 0))
        assertEquals(1210, clock.sample(1210, 0))
        assertEquals(1210, clock.sample(1190, 0)) // 20ms drift
        assertEquals(1220, clock.sample(1220, 0))
    }

    @Test fun explicitSeekReanchorsImmediatelyEvenWhenUnder24Milliseconds() {
        val clock = LyricsFrameTimeSmoother()
        assertEquals(5000, clock.sample(5000, 0))
        assertEquals(4995, clock.sample(4995, 1)) // Five-millisecond deliberate seek
        assertEquals(4996, clock.sample(4996, 1))
    }

    @Test fun largerBackwardsStepIsNotSuppressed() {
        val clock = LyricsFrameTimeSmoother()
        assertEquals(8500, clock.sample(8500, 0))
        assertEquals(8470, clock.sample(8470, 0))
        assertEquals(1000, clock.sample(1000, 1))
    }

    @Test fun pausedClockAndMultipleSeekRevisionsStayDeterministic() {
        val clock = LyricsFrameTimeSmoother()
        assertEquals(3000, clock.sample(3000, 0))
        repeat(60) { assertEquals(3000, clock.sample(3000, 0)) }
        assertEquals(1400, clock.sample(1400, 1))
        assertEquals(1400, clock.sample(1400, 2))
    }
}
