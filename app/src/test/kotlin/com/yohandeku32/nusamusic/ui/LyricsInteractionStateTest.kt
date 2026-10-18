package com.yohandeku32.nusamusic.ui

import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.MutatorMutex
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class LyricsInteractionStateTest {
    @Test fun touchDownBlocksFollowingBeforeDragSlopAndSeekCannotOverrideTheFinger() {
        val state = LyricsInteractionState()
        assertTrue(state.canFollow)
        state.touchDown()
        assertFalse(state.canFollow)
        state.seek()
        assertFalse(state.canFollow)
        state.touchUp()
        assertFalse(state.canFollow)
        state.resume(state.generation)
        assertTrue(state.canFollow)
    }
    @Test fun staleIdleTimerCannotResumeANewerGestureOrFling() {
        val state = LyricsInteractionState()
        state.touchDown(); state.touchUp(); val old = state.generation
        state.touchDown(); state.drag(true); state.touchUp()
        state.resume(old)
        assertFalse(state.canFollow)
        state.drag(false); state.resume(old)
        assertFalse(state.canFollow)
        state.resume(state.generation)
        assertTrue(state.canFollow)
    }
    @Test fun tapSeekResumesOnlyAfterTheTouchEnds() {
        val state = LyricsInteractionState()
        state.touchDown(); state.touchUp(); state.seek()
        assertTrue(state.canFollow)
        state.drag(true); state.seek()
        assertFalse(state.canFollow)
    }
    @Test fun userMutationCancelsAutoAndBlocksACompetingDefaultMutation() = runBlocking {
        val mutex = MutatorMutex()
        val auto = launch(start = CoroutineStart.UNDISPATCHED) {
            mutex.mutate(MutatePriority.Default) { awaitCancellation() }
        }
        val release = CompletableDeferred<Unit>()
        val entered = CompletableDeferred<Unit>()
        val manual = launch(start = CoroutineStart.UNDISPATCHED) {
            mutex.mutate(MutatePriority.UserInput) { entered.complete(Unit); release.await() }
        }
        entered.await()
        auto.join()
        val competing = launch(start = CoroutineStart.UNDISPATCHED) {
            mutex.mutate(MutatePriority.Default) { fail("Auto scroll stole manual input") }
        }
        competing.join()
        assertTrue(auto.isCancelled)
        assertTrue(competing.isCancelled)
        release.complete(Unit)
        manual.join(); auto.join(); competing.join()
    }
    @Test fun aNewSeekMutationCancelsItsPreviousScrollOwner() = runBlocking {
        val mutex = MutatorMutex()
        val previous = launch(start = CoroutineStart.UNDISPATCHED) {
            mutex.mutate { awaitCancellation() }
        }
        var transitions = 0
        mutex.mutate { transitions++ }
        previous.join()
        assertTrue(previous.isCancelled)
        assertEquals(1, transitions)
    }

    @Test fun oldAutoCancellationCannotOverrideANewerTapSeek() {
        val state = LyricsInteractionState()
        val oldOwner = state.generation
        state.touchDown(); state.touchUp(); state.seek()
        state.cancelledAuto(oldOwner)
        assertTrue(state.canFollow)
        state.cancelledAuto(state.generation)
        assertFalse(state.canFollow)
    }
}
