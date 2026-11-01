package com.yohandeku32.nusamusic.ui

import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.Lifecycle
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LyricsFramePlaybackUiTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun theSharedClockFreezesOnPauseSeeksBothWaysAndStopsReadingInTheBackground() {
        val raw = AtomicLong(1500)
        val reads = AtomicInteger()
        val position = mutableLongStateOf(1500)
        val playing = mutableStateOf(true)
        val epoch = mutableIntStateOf(0)
        lateinit var cursor: LyricsPlaybackState
        compose.mainClock.autoAdvance = false
        compose.setContent {
            val playback = rememberLyricsFramePlayback("song", position,
                { reads.incrementAndGet(); raw.get() },
                { epoch.intValue }, playing.value, epoch.intValue)
            SideEffect { cursor = playback }
            Text(playback.position.longValue.toString())
        }
        compose.mainClock.advanceTimeBy(32)
        compose.waitForIdle()
        raw.set(1700)
        compose.mainClock.advanceTimeBy(32)
        compose.waitForIdle()
        assertEquals(1700L, cursor.position.longValue)
        compose.runOnIdle { playing.value = false }
        compose.mainClock.advanceTimeBy(32)
        compose.waitForIdle()
        val paused = cursor.position.longValue
        raw.set(2500)
        compose.mainClock.advanceTimeBy(250)
        compose.waitForIdle()
        assertEquals(paused, cursor.position.longValue)
        compose.runOnIdle { epoch.intValue++ }
        compose.waitForIdle()
        compose.mainClock.advanceTimeBy(32)
        compose.waitForIdle()
        compose.mainClock.advanceTimeBy(32)
        compose.waitForIdle()
        compose.runOnIdle { assertEquals(2500L, cursor.position.longValue) }
        raw.set(1200)
        compose.runOnIdle { epoch.intValue++ }
        compose.waitForIdle()
        compose.mainClock.advanceTimeBy(32)
        compose.waitForIdle()
        compose.mainClock.advanceTimeBy(32)
        compose.waitForIdle()
        assertEquals(1200L, cursor.position.longValue)
        compose.runOnIdle { playing.value = true }
        compose.mainClock.advanceTimeBy(32)
        compose.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        val stoppedReads = reads.get()
        compose.mainClock.advanceTimeBy(250)
        assertEquals(stoppedReads, reads.get())
    }
}
