package com.yohandeku32.nusamusic.ui

import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.yohandeku32.nusamusic.data.AudioWaveform
import com.yohandeku32.nusamusic.model.Song
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class VinylWaveformStateTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val selected = mutableStateOf(song(1))
    private val visible = mutableStateOf(true)
    private class Request(val id: Long) {
        val result = CompletableDeferred<AudioWaveform?>()
        var cancelled = false
    }
    private val requests = mutableListOf<Request>()

    private fun show() {
        compose.setContent {
            val state = rememberVinylWaveform(selected.value, visible.value) { _, song ->
                val request = Request(song.id).also(requests::add)
                try { request.result.await() }
                catch (cancelled: CancellationException) { request.cancelled = true; throw cancelled }
            }
            Text("${state.data?.peaks?.first() ?: "none"}:${state.loading}")
        }
    }

    @Test fun trackSwitchClearsOldPeaksAndCancelledResultsCannotReplaceTheNewTrack() {
        show()
        compose.onNodeWithText("none:true").assertExists()
        compose.runOnIdle { assertEquals(1, requests.size); requests[0].result.complete(waveform(0.2f)) }
        compose.onNodeWithText("0.2:false").assertExists()
        compose.runOnIdle { selected.value = song(2) }
        compose.onNodeWithText("none:true").assertExists()
        compose.runOnIdle { assertEquals(2, requests.size) }
        compose.runOnIdle { selected.value = song(3) }
        compose.onNodeWithText("none:true").assertExists()
        compose.runOnIdle {
            assertEquals(3, requests.size)
            assertTrue(requests[1].cancelled)
            requests[1].result.complete(waveform(0.5f))
        }
        compose.onNodeWithText("none:true").assertExists()
        compose.runOnIdle { requests[2].result.complete(waveform(0.8f)) }
        compose.onNodeWithText("0.8:false").assertExists()
        assertEquals(listOf(1L, 2L, 3L), requests.map { it.id })
    }

    @Test fun hiddenPlayerDoesNotDecodeAndHidingCancelsPendingAnalysis() {
        visible.value = false
        show()
        compose.onNodeWithText("none:false").assertExists()
        assertTrue(requests.isEmpty())
        compose.runOnIdle { visible.value = true }
        compose.onNodeWithText("none:true").assertExists()
        compose.runOnIdle { assertEquals(1, requests.size) }
        compose.runOnIdle { visible.value = false }
        compose.onNodeWithText("none:false").assertExists()
        compose.runOnIdle { assertTrue(requests.single().cancelled) }
    }

    private fun waveform(level: Float) = AudioWaveform(4000, FloatArray(256) { level })
    private fun song(id: Long) = Song(id, "Test $id", "Fixture", "Fixture", "content://audio/$id", 4000, 0)
}
