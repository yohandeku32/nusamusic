package com.yohandeku32.nusamusic.ui

import com.yohandeku32.nusamusic.model.Song
import org.junit.Assert.*
import org.junit.Test

class CdDiscTransferStateTest {
    private fun song(id: Long) = Song(id, "Song $id", "Artist", "Album", "song:$id", 60_000, id)

    @Test fun replacingAnInsertionRejectsThePreviousAnimationsCallbacks() {
        val state = CdDiscTransferState()
        val old = state.begin(1, song(1))
        assertTrue(state.fly(old))
        val next = state.begin(2, song(2))
        state.finish(old)
        assertFalse(state.fly(old))
        assertEquals(next, state.request)
        assertEquals(CdDiscTransferPhase.OPENING, state.phase)
    }

    @Test fun reselectingTheSameSongStillHasAnIndependentInsertion() {
        val state = CdDiscTransferState()
        val old = state.begin(1, song(1))
        val next = state.begin(1, song(1))
        state.finish(old)
        assertFalse(state.fly(old))
        assertEquals(next, state.request)
        assertTrue(state.fly(next))
    }

    @Test fun anInsertionCanOnlyStartFlyingOnce() {
        val state = CdDiscTransferState()
        val item = state.begin(1, song(1))
        assertEquals(CdDiscTransferPhase.OPENING, state.phase)
        assertTrue(state.fly(item))
        assertEquals(CdDiscTransferPhase.FLYING, state.phase)
        assertFalse(state.fly(item))
        state.finish(item)
        assertNull(state.request)
        assertFalse(state.fly(item))
    }

    @Test fun cancellationReleasesThePlayerDuringEitherStage() {
        for (flying in listOf(false, true)) {
            val state = CdDiscTransferState()
            val item = state.begin(1, song(1))
            if (flying) state.fly(item)
            state.finish(item)
            assertNull(state.request)
            assertFalse(state.fly(item))
        }
    }

    @Test fun playbackIsReleasedOnlyOnceAfterFlightAndSeating() {
        val state = CdDiscTransferState()
        val item = state.begin(1, song(1), previousSongId = 10)
        assertNull(state.arrive(item))
        assertFalse(state.seat(item))
        state.fly(item)
        assertNull(state.arrive(item))
        assertTrue(state.seat(item))
        assertEquals(item.song, state.arrive(item))
        assertEquals(CdDiscTransferPhase.ARRIVED, state.phase)
        assertNull(state.arrive(item))
        // Retain the seated art while waiting for the actual player update.
        assertEquals(item, state.request)
        state.finish(item)
        assertNull(state.request)
    }

    @Test fun replacingASeatedDiscNeverReleasesTheOlderSong() {
        val state = CdDiscTransferState()
        val old = state.begin(1, song(1))
        state.fly(old)
        state.seat(old)
        val next = state.begin(2, song(2))
        assertNull(state.arrive(old))
        state.finish(old)
        assertEquals(next, state.request)
        assertNull(state.arrive(next))
    }

    @Test fun cancellingAfterSeatingDoesNotStartPlayback() {
        val state = CdDiscTransferState()
        val item = state.begin(1, song(1))
        state.fly(item)
        state.seat(item)
        state.finish(item)
        assertNull(state.arrive(item))
    }
}
