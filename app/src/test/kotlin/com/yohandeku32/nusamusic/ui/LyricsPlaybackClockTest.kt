package com.yohandeku32.nusamusic.ui

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableLongStateOf
import org.junit.Assert.*
import org.junit.Test

class LyricsPlaybackClockTest {
    @Test fun callbackRetainedDuringOffscreenLoadStartsFollowingOnOpen() {
        val position = mutableLongStateOf(1_000)
        val clock = LyricsPlaybackClock(position, false)
        val callbackRetainedByRenderer = clock.read
        val observed = derivedStateOf { callbackRetainedByRenderer() }
        position.longValue = 2_000
        assertEquals(1_000, observed.value)
        clock.update(position, true)
        assertEquals(2_000, observed.value)
        position.longValue = 3_000
        assertEquals(3_000, observed.value)
        assertSame(callbackRetainedByRenderer, clock.read)
    }
    @Test fun closingFreezesDrawingAndReopeningResumesTheSameCallback() {
        val position = mutableLongStateOf(1_000)
        val clock = LyricsPlaybackClock(position, true)
        val read = clock.read
        position.longValue = 2_000
        clock.update(position, false)
        position.longValue = 8_000
        assertEquals(2_000, read())
        clock.update(position, true)
        assertEquals(8_000, read())
    }
    @Test fun restoredPlaybackStateCanReplaceThePositionSource() {
        val old = mutableLongStateOf(0)
        val clock = LyricsPlaybackClock(old, true)
        val read = clock.read
        val restored = mutableLongStateOf(45_000)
        clock.update(restored, true)
        old.longValue = 1_000
        assertEquals(45_000, read())
        restored.longValue = 46_000
        assertEquals(46_000, read())
    }
    @Test fun backwardSeekIsImmediate() {
        val position = mutableLongStateOf(50_000)
        val clock = LyricsPlaybackClock(position, true)
        assertEquals(50_000, clock.read())
        position.longValue = 3_000
        assertEquals(3_000, clock.read())
    }
    @Test fun invalidPlaybackBoundsNeverOverflow() {
        val position = mutableLongStateOf(-1)
        val clock = LyricsPlaybackClock(position, true)
        assertEquals(0, clock.read())
        position.longValue = Long.MAX_VALUE
        assertEquals(Int.MAX_VALUE, clock.read())
    }
}
