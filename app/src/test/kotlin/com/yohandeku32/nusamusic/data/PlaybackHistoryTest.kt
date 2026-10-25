package com.yohandeku32.nusamusic.data

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PlaybackHistoryTest {
    @Test fun beginningPlaybackUpdatesRecencyWithoutCountingAnEntireListen() {
        val context = RuntimeEnvironment.getApplication()
        val counts = PlaybackHistory.preferences(context)
        counts.edit().clear().commit()
        PlaybackHistory.recordRecent(context, "content://song/started")
        assertTrue(PlaybackHistory.counts(counts).isEmpty())
        assertTrue(PlaybackHistory.recentPreferences(context).getLong("content://song/started", 0) > 0)
    }

    @Test fun recordingRecentTimeDoesNotPollutePlayCounts() {
        val context = RuntimeEnvironment.getApplication()
        val counts = PlaybackHistory.preferences(context)
        val recent = PlaybackHistory.recentPreferences(context)
        counts.edit().clear().commit()
        recent.edit().clear().commit()
        PlaybackHistory.record(context, "content://song/1")
        PlaybackHistory.record(context, "content://song/1")
        assertEquals(mapOf("content://song/1" to 2L), PlaybackHistory.counts(counts))
        assertTrue(recent.getLong("content://song/1", 0) > 0L)
    }

    @Test fun boundedRecentHistoryKeepsNewTrackAndLeavesLifetimeCountsIntact() {
        val context = RuntimeEnvironment.getApplication()
        val recent = PlaybackHistory.recentPreferences(context)
        recent.edit().clear().commit()
        val edit = recent.edit()
        repeat(200) { edit.putLong("content://song/$it", it + 1L) }
        edit.commit()
        PlaybackHistory.record(context, "content://song/new")
        assertEquals(200, recent.all.size)
        assertFalse(recent.contains("content://song/0"))
        assertTrue(recent.contains("content://song/new"))
    }
}
