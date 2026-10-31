package com.yohandeku32.nusamusic.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Looper
import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.yohandeku32.nusamusic.data.LyricsRepository
import com.yohandeku32.nusamusic.data.Mp3EmbeddedLyricsWriter
import com.yohandeku32.nusamusic.model.Song
import java.io.File
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h914dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class VinylLyricsSectionTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private var sought = 0L

    private fun fixture(withLyrics: Boolean): Song {
        val original = File(compose.activity.cacheDir, "vinyl-source.mp3")
        original.writeBytes(byteArrayOf(0xFF.toByte(), 0xFB.toByte()) + ByteArray(126))
        val output = if (withLyrics) File(compose.activity.cacheDir, "vinyl-lyrics.mp3").also {
            Mp3EmbeddedLyricsWriter.write(original, it,
                "[00:01.00]First lyric fixture\n[00:06.00]Second lyric fixture",
                "First lyric fixture\nSecond lyric fixture")
        } else original
        return Song(940L, "Vinyl lyric fixture", "Test fixture", "Test fixture",
            Uri.fromFile(output).toString(), 10_000L, 0L)
    }

    private fun show(song: Song, positionMs: Long = 2_000L): ImmersiveLyricsState {
        val state = ImmersiveLyricsState(LyricsRepository(compose.activity))
        val position = mutableStateOf(positionMs)
        compose.mainClock.autoAdvance = false
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                MaterialTheme {
                    val scroll = rememberLazyListState()
                    LaunchedEffect(Unit) { scroll.scrollToItem(1) }
                    LazyColumn(Modifier.requiredSize(411.dp, 914.dp).background(Color.Black).testTag("lyrics-screen"), state = scroll) {
                        item { Box(Modifier.fillMaxWidth().height(914.dp)) }
                        item {
                            VinylLyricsSection("LYRICS", 914.dp, Modifier.testTag("lyrics-section")) { modifier ->
                                ImmersiveLyricsPanel(song, position, { position.value }, { 0 },
                                    false, 0, song.durationMs, state, { sought = it }, 28f,
                                    onlineLyricsEnabled = false, modifier = modifier.testTag("lyrics-panel"),
                                    onQueueLyrics = { _, _ -> }, isLyricsQueued = { false }, topAligned = true)
                            }
                        }
                    }
                }
            }
        }
        try {
            compose.waitUntil(10_000L) {
                shadowOf(Looper.getMainLooper()).idle()
                compose.mainClock.advanceTimeByFrame()
                state.forSong(song).localReadComplete && !state.forSong(song).loading
            }
        } catch (failure: ComposeTimeoutException) {
            val entry = state.forSong(song)
            throw AssertionError("Lyric state: local=${entry.localReadComplete}, loading=${entry.loading}, generation=${entry.generation}, data=${entry.data?.stored?.content}, job=${entry.job}", failure)
        }
        compose.mainClock.advanceTimeBy(400L)
        compose.waitForIdle()
        return state
    }

    @Test fun embeddedTimedLyricsRenderAndSeekFromTheVinylSection() {
        val song = fixture(withLyrics = true)
        val state = show(song)
        assertNotNull(state.forSong(song).data?.synced)
        compose.onNodeWithText("LYRICS").assertIsDisplayed()
        val screen = compose.onNodeWithTag("lyrics-screen").getUnclippedBoundsInRoot()
        val section = compose.onNodeWithTag("lyrics-section").getUnclippedBoundsInRoot()
        val panel = compose.onNodeWithTag("lyrics-panel").getUnclippedBoundsInRoot()
        val first = compose.onNodeWithText("First lyric fixture").getUnclippedBoundsInRoot()
        assertEquals(457f, (section.bottom - section.top).value, 1f)
        assertEquals(screen.top.value + 457f, section.top.value, 1f)
        assertTrue("Active lyrics start near the panel top", first.top.value - panel.top.value in 0f..60f)
        compose.onNodeWithText("First lyric fixture").assertIsDisplayed().performClick()
        assertEquals(1_000L, sought)
        compose.onNodeWithText("ABOUT THE ARTIST").assertDoesNotExist()
        compose.runOnIdle {
            val view = compose.activity.findViewById<View>(android.R.id.content)
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            view.draw(Canvas(bitmap))
            val directory = File("build/reports/vinyl-preview").apply { mkdirs() }
            File(directory, "lyrics-section.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
        }
    }

    @Test fun missingLyricsShowAnUnavailableStateAndWorkingRetry() {
        val song = fixture(withLyrics = false)
        val state = show(song)
        compose.onNodeWithText("No synchronized lyrics found").assertIsDisplayed()
        val generation = state.forSong(song).generation
        compose.onNodeWithText("Try again").assertIsEnabled().performClick()
        compose.waitUntil(5_000L) {
            shadowOf(Looper.getMainLooper()).idle()
            compose.mainClock.advanceTimeByFrame()
            state.forSong(song).generation > generation
        }
        assertEquals(0L, sought)
    }

    @Test fun laterActiveLineAlsoFollowsNearThePanelTop() {
        val song = fixture(withLyrics = true)
        show(song, positionMs = 7_000L)
        val panel = compose.onNodeWithTag("lyrics-panel").getUnclippedBoundsInRoot()
        val line = compose.onNodeWithText("Second lyric fixture").getUnclippedBoundsInRoot()
        assertTrue("Later active lines use the same top follow region", line.top.value - panel.top.value in 0f..60f)
        compose.onNodeWithText("Second lyric fixture").assertIsDisplayed().performClick()
        assertEquals(6_000L, sought)
    }
}
