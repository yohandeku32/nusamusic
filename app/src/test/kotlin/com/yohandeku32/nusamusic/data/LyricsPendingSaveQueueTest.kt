package com.yohandeku32.nusamusic.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Robolectric's AndroidInterceptors cannot initialize against the JDK used
 * by current CI, so verify the no-write-while-active rule as pure Kotlin.
 * SAF storage and process-restoration remain device acceptance tests.
 */
class LyricsPendingSaveQueueTest {
    @Test fun currentSongIsNeverEligibleForInPlaceMetadataRewrite() {
        assertFalse(mayCommitQueuedLyrics(songId = 123L, activeSongId = 123L))
        assertTrue(mayCommitQueuedLyrics(songId = 123L, activeSongId = 456L))
        assertTrue(mayCommitQueuedLyrics(songId = 123L, activeSongId = null))
    }

    @Test fun returningToPreviouslyQueuedSongPreventsItsWrite() {
        assertTrue(mayCommitQueuedLyrics(10L, 11L))
        assertFalse(mayCommitQueuedLyrics(10L, 10L))
        assertTrue(mayCommitQueuedLyrics(10L, 12L))
    }
    @Test fun newQueueNeverOverwritesExternallyEditedM4a() {
        assertFalse(queuedLyricsSourceChanged("same-m4a", "same-m4a", 10_000L, 11_000L))
        assertTrue(queuedLyricsSourceChanged("old-tag-bytes", "new-tag-bytes", 10_000L, 11_000L))
        // If a provider can't report a revision we should not invent one.
        assertFalse(queuedLyricsSourceChanged("old", null, 10_000L, 15_000L))
    }

    @Test fun legacyQueueUsesAudioModificationTimeForSafeFallback() {
        assertFalse(queuedLyricsSourceChanged(null, "current", 10_000L, 9_000L))
        assertFalse(queuedLyricsSourceChanged(null, "current", 10_000L, 10_800L))
        assertTrue(queuedLyricsSourceChanged(null, "current", 10_000L, 12_000L))
    }

}
