package com.yohandeku32.nusamusic.model

import org.junit.Assert.*
import org.junit.Test

class LibraryContentStateTest {
    private fun state(
        songs: Boolean = false,
        access: Boolean = true,
        cached: Boolean = false,
        scanning: Boolean = false,
        failed: Boolean = false
    ) = LibraryContentState.resolve(songs, access, cached, scanning, failed)

    @Test fun firstLaunchStaysLoadingThroughCacheReadAndInitialScan() {
        val frames = listOf(state(), state(cached = true, scanning = true), state(songs = true, cached = true))
        assertEquals(listOf(LibraryContentState.LOADING, LibraryContentState.LOADING, LibraryContentState.CONTENT), frames)
        assertFalse(frames.contains(LibraryContentState.EMPTY))
    }

    @Test fun emptyAppearsOnlyAfterLoadingAndScanningComplete() {
        assertEquals(LibraryContentState.LOADING, state(scanning = true))
        assertEquals(LibraryContentState.LOADING, state(cached = true, scanning = true))
        assertEquals(LibraryContentState.EMPTY, state(cached = true))
    }

    @Test fun cachedSongsAppearImmediatelyWithoutWaitingForPlaybackRestore() {
        assertEquals(LibraryContentState.CONTENT, state(songs = true))
        assertEquals(LibraryContentState.CONTENT, state(songs = true, cached = true))
    }

    @Test fun refreshKeepsExistingCollectionVisible() {
        assertEquals(LibraryContentState.CONTENT, state(songs = true, cached = true, scanning = true))
        assertEquals(LibraryContentState.CONTENT, state(songs = true, cached = true, failed = true))
    }

    @Test fun deniedAccessShowsPermissionPromptInsteadOfAnEndlessLoader() {
        assertEquals(LibraryContentState.PERMISSION_REQUIRED, state(access = false))
        assertEquals(LibraryContentState.PERMISSION_REQUIRED, state(songs = true, access = false, cached = true))
    }

    @Test fun failedInitialScanDoesNotClaimThereIsNoMusic() {
        assertEquals(LibraryContentState.SCAN_FAILED, state(cached = true, failed = true))
    }

    @Test fun retryMovesFromFailureThroughLoadingToConfirmedEmpty() {
        assertEquals(LibraryContentState.LOADING, state(cached = true, scanning = true, failed = true))
        assertEquals(LibraryContentState.EMPTY, state(cached = true, failed = false))
    }
}
