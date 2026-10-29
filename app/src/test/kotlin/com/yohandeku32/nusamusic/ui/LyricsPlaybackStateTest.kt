package com.yohandeku32.nusamusic.ui

import org.junit.Assert.*
import org.junit.Test

class LyricsPlaybackStateTest {
    @Test fun longFrameStallsAndSpeedChangesDoNotBecomeFakeSeeks() {
        val playback = LyricsPlaybackState(0, 4)
        for (now in listOf(16L, 32L, 500L, 1200L, 3500L)) playback.publish(now, 4)
        assertEquals(3500L, playback.position.longValue)
        assertEquals(0, playback.motion.revision)
        assertEquals(0, playback.motion.resetAt)
    }
    @Test fun seekResetsExactlyOnceEvenWhenManyFramesReadTheSameEvent() {
        val playback = LyricsPlaybackState(5000, 0)
        playback.publish(1000, 1)
        repeat(120) { playback.publish(1000 + it.toLong(), 1) }
        assertEquals(1, playback.motion.revision)
        assertEquals(1000, playback.motion.resetAt)
        assertEquals(1119L, playback.position.longValue)
    }
    @Test fun rapidForwardAndBackwardSeeksUseOnlyLatestAuthoredPosition() {
        val playback = LyricsPlaybackState(0, 0)
        for ((epoch, now) in listOf(1 to 7000L, 2 to 1000L, 3 to 8500L, 4 to 0L)) playback.publish(now, epoch)
        assertEquals(4, playback.motion.revision)
        assertEquals(0, playback.motion.resetAt)
        assertEquals(0L, playback.position.longValue)
    }
    @Test fun pausedUpdatesAndAChangedSongDoNotInheritPriorMotion() {
        val first = LyricsPlaybackState(4500, 7)
        repeat(120) { first.publish(4500, 7) }
        assertEquals(0, first.motion.revision)
        first.publish(1000, 8)
        val next = LyricsPlaybackState(0, 8)
        assertEquals(0L, next.position.longValue)
        assertEquals(0, next.motion.revision)
        assertEquals(0, next.motion.resetAt)
    }
    @Test fun invalidBoundsAreClampedBeforePublishingAndResetting() {
        val playback = LyricsPlaybackState(-100, 0)
        assertEquals(0L, playback.position.longValue)
        playback.publish(Long.MAX_VALUE, 1)
        assertEquals(Int.MAX_VALUE.toLong(), playback.position.longValue)
        assertEquals(Int.MAX_VALUE, playback.motion.resetAt)
    }
    @Test fun media3ClockCorrectionsCannotMoveTheEntireLyricsTimelineBackwards() {
        val playback = LyricsPlaybackState(1100, 7)
        for (sample in listOf(1120L, 1140L, 1030L, 1099L, 1130L, 1180L, 1168L)) {
            playback.publish(sample, 7)
            assertTrue("same epoch must not rewind lyrics", playback.position.longValue >= 1100L)
        }
        assertEquals(1180L, playback.position.longValue)
        assertEquals(0, playback.motion.revision)
        // A user-requested seek reanchors immediately, including a small one.
        playback.publish(1175L, 8)
        assertEquals(1175L, playback.position.longValue)
        assertEquals(1, playback.motion.revision)
        playback.publish(1172L, 8)
        assertEquals(1175L, playback.position.longValue)
        playback.publish(500L, 9)
        assertEquals(500L, playback.position.longValue)
        assertEquals(2, playback.motion.revision)
    }

    @Test fun sameEpochJitterDoesNotReTriggerBoundaryAfterLineHasAdvanced() {
        val playback = LyricsPlaybackState(1950, 0)
        val samples = listOf(1970L, 1994L, 2010L, 1988L, 1990L, 2030L, 1975L, 2050L)
        val advanced = ArrayList<Boolean>()
        for (sample in samples) {
            playback.publish(sample, 0)
            advanced.add(playback.position.longValue >= 2000L)
        }
        assertEquals(listOf(false, false, true, true, true, true, true, true), advanced)
    }

}
