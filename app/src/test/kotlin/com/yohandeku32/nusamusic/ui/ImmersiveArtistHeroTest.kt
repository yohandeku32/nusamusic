package com.yohandeku32.nusamusic.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.yohandeku32.nusamusic.data.ArtistLibrary
import com.yohandeku32.nusamusic.model.Song
import java.util.Locale
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp")
@GraphicsMode(GraphicsMode.Mode.LEGACY)
class ImmersiveArtistHeroTest {
    @get:Rule val compose = createComposeRule()
    private val previousLocale = Locale.getDefault()
    private val width = mutableStateOf(320.dp)
    private val plays = mutableListOf<Song>()
    private var backCount = 0

    @Before fun useEnglishLabels() { Locale.setDefault(Locale.ENGLISH) }
    @After fun restoreLocale() { Locale.setDefault(previousLocale) }

    private fun showPage(
        song: Song,
        fontScale: Float = 1f,
        overlay: Boolean = true,
        album: Boolean = false
    ) {
        val state = ImmersiveArtistPageState(song.artist).apply {
            if (album) selectedAlbumKey = ArtistLibrary.albums(listOf(song)).single().key
        }
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, fontScale)) {
                Box(Modifier.requiredWidth(width.value).fillMaxHeight()) {
                    ImmersiveArtistPage(
                        artistName = song.artist,
                        songs = listOf(song),
                        currentSong = song,
                        // Keep the real layout and callbacks without remote lookups.
                        isVisible = false,
                        state = state,
                        onBack = { backCount++ },
                        onPlay = { selected, queue ->
                            assertEquals(listOf(song), queue)
                            plays.add(selected)
                        },
                        artwork = { _, modifier -> Box(modifier.background(Color.White)) },
                        heroArtwork = { _, modifier ->
                            Box(modifier.background(Color.White).testTag("hero-art"))
                        },
                        overlayMode = overlay
                    )
                }
            }
        }
        compose.waitForIdle()
    }

    private fun assertTextFitsHero(text: String): TextLayoutResult {
        val results = mutableListOf<TextLayoutResult>()
        val node = compose.onNodeWithText(text)
        node.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(results) }
        val result = results.single()
        assertFalse("Text was clipped or ellipsized: $text", result.hasVisualOverflow)
        val textBounds = node.getUnclippedBoundsInRoot()
        val artworkBounds = compose.onNodeWithTag("hero-art").getUnclippedBoundsInRoot()
        assertTrue(textBounds.top >= artworkBounds.top)
        assertTrue(textBounds.bottom <= artworkBounds.bottom)
        assertTrue(textBounds.left >= artworkBounds.left)
        assertTrue(textBounds.right <= artworkBounds.right)
        return result
    }

    @Test fun shortArtistKeepsOverlayHeight() {
        showPage(song())
        assertEquals(360f, compose.onNodeWithTag("hero-art")
            .getUnclippedBoundsInRoot().height.value, 1f)
        assertTextFitsHero("Test Artist")
    }

    @Test fun shortArtistKeepsStandaloneHeightAndBackAction() {
        showPage(song(), overlay = false)
        assertEquals(450f, compose.onNodeWithTag("hero-art")
            .getUnclippedBoundsInRoot().height.value, 1f)
        compose.onNodeWithContentDescription("Back").performClick()
        compose.runOnIdle { assertEquals(1, backCount) }
    }

    @Test fun enlargedArtistTitleReflowsBeyondThreeLinesAtBothPhoneWidths() {
        val artist = "The Artist Whose Full Name Needs More Than Three Lines At Large Font Sizes"
        showPage(song(artist = artist), fontScale = 2f)
        for (phoneWidth in listOf(320.dp, 411.dp)) {
            compose.runOnIdle { width.value = phoneWidth }
            compose.waitForIdle()
            assertTrue(assertTextFitsHero(artist).lineCount > 3)
            assertTextFitsHero("ARTIST • YOUR LIBRARY")
            assertTextFitsHero("1 songs  ·  1 albums")
            assertTrue(compose.onNodeWithTag("hero-art")
                .getUnclippedBoundsInRoot().height.value > 360f)
        }
    }

    @Test fun enlargedAlbumTitleAndArtistMetadataFitAndPlaybackActionsStillWork() {
        val song = song(
            artist = "An Artist With A Long Name That Must Wrap In Album Metadata",
            album = "A Long Album Title That Remains Complete When The Listener Enlarges Their Text"
        )
        showPage(song, fontScale = 2f, album = true)
        assertTrue(assertTextFitsHero(song.album).lineCount > 3)
        assertTrue(assertTextFitsHero("${song.artist}  ·  1 songs").lineCount > 1)
        compose.onNodeWithText("Play").performScrollTo().performClick()
        compose.onNodeWithText("Shuffle").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(listOf(song, song), plays) }
    }

    @Test fun minimumScrimProtectsAllTextEvenOverWhiteArtwork() {
        val background = Color.Black.copy(alpha = ArtistHeroTextScrimAlpha)
            .compositeOver(Color.White)
        val title = Color(0xFFF7F8FA)
        val eyebrow = Color.White.copy(alpha = 0.8f).compositeOver(background)
        val metadata = Color.White.copy(alpha = 0.73f).compositeOver(background)
        fun contrast(foreground: Color) =
            (foreground.luminance() + 0.05f) / (background.luminance() + 0.05f)
        assertTrue(contrast(title) >= 3f)
        assertTrue(contrast(eyebrow) >= 4.5f)
        assertTrue(contrast(metadata) >= 4.5f)
    }

    private fun song(artist: String = "Test Artist", album: String = "Test Album") = Song(
        id = 1L, title = "Test Track", artist = artist, album = album,
        uri = "content://test/audio/1", durationMs = 180_000L, albumId = 1L
    )
}
