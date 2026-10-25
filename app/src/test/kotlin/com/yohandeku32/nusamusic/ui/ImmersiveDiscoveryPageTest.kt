package com.yohandeku32.nusamusic.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.yohandeku32.nusamusic.data.DiscoveryLibrary
import com.yohandeku32.nusamusic.model.Song
import java.io.File
import java.util.Locale
import kotlin.math.roundToInt
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ImmersiveDiscoveryPageTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val previousLocale = Locale.getDefault()
    private val one = Song(1, "Song one", "Artist A", "Collection one", "content://music/1", 180000, 1, 100)
    private val two = Song(2, "Song two", "Artist B", "Collection two", "content://music/2", 180000, 2, 200)
    private val songs = listOf(one, two)
    private var selectedArtist: String? = null
    private var selectedAlbum: String? = null
    private var played: Song? = null
    private var pauses = 0
    private var libraries = 0

    @Before fun english() { Locale.setDefault(Locale.ENGLISH) }
    @After fun restore() { Locale.setDefault(previousLocale) }

    private fun show(width: Int = 411, scale: Float = 1f, empty: Boolean = false, history: Boolean = true, active: Boolean = false) {
        val tracks = if (empty) emptyList() else songs
        val data = DiscoveryLibrary.build(tracks, if (history) mapOf(one.uri to 3L) else emptyMap(),
            if (history) mapOf(one.uri to 1000L) else emptyMap())
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, scale)) {
                Box(Modifier.requiredWidth(width.dp).fillMaxHeight().background(Color(0xFF101010))) {
                    DiscoveryFeed(data, tracks, if (active) one else null, active,
                        rememberLazyListState(), onArtist = { selectedArtist = it },
                        onAlbum = { selectedAlbum = it.title },
                        onPlay = { song, queue -> assertEquals(data.recent, queue); played = song },
                        onTogglePlay = { pauses++ }, onLibrary = { libraries++ },
                        artwork = { song, modifier -> Box(modifier.background(
                            if (song.id == 1L) Color(0xFF84644B) else Color(0xFF546D76))) },
                        artistArtwork = { _, modifier -> Box(modifier.background(Color(0xFF69604F))) })
                }
            }
        }
        compose.waitForIdle()
    }

    private fun screenshot(name: String) {
        val bounds = compose.onNodeWithTag("discovery-feed").getUnclippedBoundsInRoot()
        compose.runOnIdle {
            val view = compose.activity.findViewById<View>(android.R.id.content)
            val bitmap = Bitmap.createBitmap(bounds.width.value.roundToInt(),
                bounds.height.value.roundToInt(), Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.translate(-bounds.left.value, -bounds.top.value)
            view.draw(canvas)
            val dir = File("build/reports/discovery-preview").apply { mkdirs() }
            File(dir, "$name.png").outputStream().use {
                assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it))
            }
            bitmap.recycle()
        }
    }

    @Test fun albumArtistAndRecentTrackHaveWorkingActions() {
        show()
        compose.onNodeWithTag("discovery-artist:artist a").performClick()
        assertEquals("Artist A", selectedArtist)
        compose.onNodeWithTag("discovery-album:artist b\u0000collection two").performClick()
        assertEquals("Collection two", selectedAlbum)
        compose.onNodeWithText("Song one").performScrollTo().performClick()
        assertEquals(one, played)
    }

    @Test fun currentRecentTrackPausesInsteadOfReplacingTheQueue() {
        show(active = true)
        compose.onNodeWithContentDescription("Pause").performScrollTo().performClick()
        assertEquals(1, pauses)
        assertNull(played)
    }

    @Test fun emptyHistoryIsExplainedWithoutInventedRecentTracks() {
        show(history = false)
        compose.onNodeWithText("Your artists").assertExists()
        compose.onNodeWithText("Your listening history will appear here.").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Song one").assertDoesNotExist()
    }

    @Test fun emptyLibraryOffersWorkingLibraryNavigation() {
        show(empty = true)
        compose.onNodeWithText("No songs yet").assertIsDisplayed()
        compose.onNodeWithText("Open Library").performClick()
        assertEquals(1, libraries)
    }

    @Test fun portraitLayoutMatchesDiscoveryHierarchy() {
        show()
        compose.onNodeWithText("Discover").assertIsDisplayed()
        compose.onNodeWithText("Popular artists").assertIsDisplayed()
        compose.onNodeWithText("Recently played").assertIsDisplayed()
        screenshot("discovery-portrait")
    }

    @Test fun narrowScreenAndLargeTypeKeepRecentPlaybackReachable() {
        show(width = 320, scale = 2f)
        compose.onNodeWithText("Song one").performScrollTo()
        compose.onNodeWithContentDescription("Play").performClick()
        assertEquals(one, played)
        val bounds = compose.onNodeWithText("Song one").getUnclippedBoundsInRoot()
        assertTrue(bounds.left >= 0.dp && bounds.right <= 320.dp)
        screenshot("discovery-large-text")
    }
}
