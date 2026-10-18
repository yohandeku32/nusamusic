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
}
