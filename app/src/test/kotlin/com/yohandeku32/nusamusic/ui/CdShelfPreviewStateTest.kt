package com.yohandeku32.nusamusic.ui

import com.yohandeku32.nusamusic.model.Song
import org.junit.Assert.*
import org.junit.Test

class CdShelfPreviewStateTest {
    private fun item(id: Long) = CdShelfSelection(
        id.toInt(), Song(id, "Song $id", "Artist", "Album", "song:$id", 60_000, id), 120f, 80f
    )

    @Test fun tappingACaseOnlyOpensAPreview() {
        val state = CdShelfPreviewState()
        val selected = item(1)
        assertTrue(state.show(selected))
        assertEquals(CdShelfPreviewPhase.OPENING, state.phase)
        assertNull(state.completeReturn(selected))
        assertFalse(state.beginReturn(selected, play = true))
    }

    @Test fun playIsDeliveredOnceAfterTheCaseReturns() {
        val state = CdShelfPreviewState()
        val selected = item(1)
        state.show(selected)
        state.ready(selected)
        assertTrue(state.beginReturn(selected, play = true))
        assertFalse(state.beginReturn(selected, play = true))
        assertEquals(selected.song, state.completeReturn(selected))
        assertNull(state.completeReturn(selected))
        assertNull(state.selection)
    }

    @Test fun closingOrBackingOutDoesNotPlayThePreviewedSong() {
        for (ready in listOf(false, true)) {
            val state = CdShelfPreviewState()
            val selected = item(1)
            state.show(selected)
            if (ready) state.ready(selected)
            assertTrue(state.beginReturn(selected, play = false))
            assertNull(state.completeReturn(selected))
            assertEquals(CdShelfPreviewPhase.CLOSED, state.phase)
        }
    }

    @Test fun anotherCaseCannotReplaceAnUnfinishedPreview() {
        val state = CdShelfPreviewState()
        state.show(item(1))
        assertFalse(state.show(item(2)))
        assertEquals(item(1), state.selection)
    }

    @Test fun removingASongCancelsEvenAConfirmedPendingPlay() {
        val state = CdShelfPreviewState()
        val selected = item(1)
        state.show(selected)
        state.ready(selected)
        state.beginReturn(selected, play = true)
        state.discard()
        assertNull(state.completeReturn(selected))
    }

    @Test fun callbacksFromAnOldPreviewCannotAffectTheNextOne() {
        val state = CdShelfPreviewState()
        val old = item(1)
        val next = item(2)
        state.show(old)
        state.discard()
        state.show(next)
        state.ready(old)
        assertEquals(CdShelfPreviewPhase.OPENING, state.phase)
        assertFalse(state.beginReturn(old, play = true))
        assertNull(state.completeReturn(old))
        assertEquals(next, state.selection)
    }
}
