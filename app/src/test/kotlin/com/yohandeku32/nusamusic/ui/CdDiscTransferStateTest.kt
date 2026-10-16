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

    @Test fun returnCannotInterruptAnInsertionOrStartTwice() {
        val state = CdDiscTransferState()
        val insertion = state.begin(1, song(1))
        assertNull(state.beginReturn(1, song(1)))
        state.finish(insertion)
        assertNotNull(state.beginReturn(1, song(1)))
        assertNull(state.beginReturn(1, song(1)))
    }

    @Test fun caseClosesOnlyAfterTheReturningDiscReachesTheTray() {
        val state = CdDiscTransferState()
        val item = state.beginReturn(1, song(1))!!
        assertFalse(state.close(item))
        assertNull(state.storedSongId)
        assertTrue(state.returnFlight(item))
        assertFalse(state.returnFlight(item))
        assertTrue(state.close(item))
        assertNull(state.arrive(item))
        state.finish(item)
        assertEquals(1L, state.storedSongId)
        assertNull(state.beginReturn(1, song(1)))
    }

    @Test fun reinsertingTheStoredSongResumesInsteadOfRestartingIt() {
        val state = CdDiscTransferState()
        val returned = state.beginReturn(1, song(1))!!
        state.returnFlight(returned)
        state.close(returned)
        state.finish(returned)
        val insertion = state.begin(1, song(1), previousSongId = 1)
        assertTrue(insertion.resumePlayback)
        state.fly(insertion)
        state.seat(insertion)
        assertEquals(song(1), state.arrive(insertion))
        assertNull(state.storedSongId)
    }

    @Test fun anOldReturnCannotCloseAReplacementSelection() {
        val state = CdDiscTransferState()
        val returned = state.beginReturn(1, song(1))!!
        state.returnFlight(returned)
        val insertion = state.begin(2, song(2), previousSongId = 1)
        assertFalse(state.close(returned))
        state.finish(returned)
        assertEquals(insertion, state.request)
        assertNull(state.storedSongId)
        assertFalse(insertion.resumePlayback)
    }

    @Test fun externalPlaybackRestoresTheStoredDiscButPausingKeepsItStored() {
        val state = CdDiscTransferState()
        val returned = state.beginReturn(1, song(1))!!
        state.returnFlight(returned)
        state.close(returned)
        state.finish(returned)
        state.syncPlayer(1, playing = false)
        assertEquals(1L, state.storedSongId)
        state.syncPlayer(1, playing = true)
        assertNull(state.storedSongId)
    }

    @Test fun cancellingAReturnBeforeArrivalDoesNotStoreTheDisc() {
        val state = CdDiscTransferState()
        val returned = state.beginReturn(1, song(1))!!
        state.returnFlight(returned)
        state.finish(returned)
        assertFalse(state.close(returned))
        assertNull(state.storedSongId)
    }
}
