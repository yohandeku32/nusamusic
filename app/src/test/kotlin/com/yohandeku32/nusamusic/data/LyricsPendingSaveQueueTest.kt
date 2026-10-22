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
}
