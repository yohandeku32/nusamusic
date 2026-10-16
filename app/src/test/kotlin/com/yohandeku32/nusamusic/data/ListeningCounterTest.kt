package com.yohandeku32.nusamusic.data

import org.junit.Assert.*
import org.junit.Test

class ListeningCounterTest {
    @Test fun countsOnceAfterThirtySecondsOfActualListening() {
        val counter = ListeningCounter()
        assertNull(counter.sample("a", true, 0, 180_000))
        assertNull(counter.sample("a", true, 29_999, 180_000))
        assertEquals("a", counter.sample("a", true, 30_000, 180_000))
        assertNull(counter.sample("a", true, 90_000, 180_000))
    }

    @Test fun pausesDoNotAddListeningTime() {
        val counter = ListeningCounter()
        counter.sample("a", true, 0, 180_000)
        counter.sample("a", false, 10_000, 180_000)
        assertNull(counter.sample("a", true, 100_000, 180_000))
        assertNull(counter.sample("a", true, 119_999, 180_000))
        assertEquals("a", counter.sample("a", true, 120_000, 180_000))
    }

    @Test fun shortTracksUseHalfDurationAndRepeatCanCountAgain() {
        val counter = ListeningCounter()
        counter.sample("a", true, 0, 10_000)
        assertEquals("a", counter.sample("a", true, 5_000, 10_000))
        counter.reset()
        counter.sample("a", true, 10_000, 10_000)
        assertEquals("a", counter.sample("a", true, 15_000, 10_000))
    }

    @Test fun skippingTracksDoesNotCarryTimeToNextTrack() {
        val counter = ListeningCounter()
        counter.sample("a", true, 0, 180_000)
        assertNull(counter.sample("b", true, 29_000, 180_000))
        assertNull(counter.sample("b", true, 58_999, 180_000))
        assertEquals("b", counter.sample("b", true, 59_000, 180_000))
    }
}
