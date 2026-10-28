package com.yohandeku32.nusamusic.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
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
class ImmersiveMusicLibraryTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val previousLocale = Locale.getDefault()
    private val songs = (1L..40L).map { index ->
        Song(index, "Track %02d".format(Locale.ROOT, index), "Artist $index", "Album $index",
            "content://music/$index", 180_000L, index)
    }
    private var played: Song? = null
    private var next: Song? = null
    private var shared: Song? = null
    private var deleted: Song? = null
    private var refreshes = 0
    private var returns = 0
    private var folderImports = 0
    private var permissionRequests = 0

    @Before fun english() { Locale.setDefault(Locale.ENGLISH) }
    @After fun restoreLocale() { Locale.setDefault(previousLocale) }

    @Composable
    private fun library(light: Boolean, bottom: Dp = 24.dp, visible: Boolean = true,
        empty: Boolean = false, access: Boolean = true, importEnabled: Boolean = false,
        activeSong: Song? = songs.first()) {
        ImmersiveMusicLibrary(
            songs = if (empty) emptyList() else songs, currentSong = activeSong, isPlaying = true,
            isVisible = visible, lightMode = light, hasMusicAccess = access,
            isScanning = false, emptyMessage = "No music yet",
            onReturnToPlayer = { returns++ },
            onPlay = { song, queue -> assertTrue(queue.contains(song)); played = song },
            onPlayNext = { next = it }, onShare = { shared = it },
            onDeleteSong = { deleted = it }, onTogglePlay = {},
            onScanMusic = { refreshes++ }, onRequestPermission = { permissionRequests++ },
            onChooseMusicFolder = if (importEnabled) ({ folderImports++ }) else null,
            artwork = { _, modifier, _, _ -> Box(modifier.background(Color(0xFF84644B))) },
            bottomContentPadding = bottom, modifier = Modifier.testTag("library-test")
        )
    }

    private fun show(light: Boolean = true, width: Int = 411, scale: Float = 1f, overlay: Boolean = false,
        empty: Boolean = false, access: Boolean = true, importEnabled: Boolean = false,
        activeSong: Song? = songs.first()) {
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, scale)) {
                MaterialTheme(colorScheme = if (light) darkColorScheme() else lightColorScheme()) {
                    Box(Modifier.requiredWidth(width.dp).fillMaxHeight()) {
                        if (!overlay) library(light, empty = empty, access = access,
                            importEnabled = importEnabled, activeSong = activeSong) else {
                            ImmersiveArtworkPlayer(
                                title = "Track 01", artist = "Artist 1", composerNames = "",
                                titleFontFamily = FontFamily.Default, titleFontSize = 32f,
                                hasSong = true, isPlaying = true, lightMode = false,
                                isFavorite = false, shuffleEnabled = false, repeatMode = 0,
                                lyricsOpen = false, lyricsEnabled = true,
                                onPlayPause = {}, onPrevious = {}, onNext = {}, onShuffle = {},
                                onRepeat = {}, onFavorite = {}, onLyrics = {}, onDiscovery = {},
                                libraryOpen = true, libraryLightMode = light, liquidGlassEnabled = false,
                                libraryContent = { settled, padding ->
                                    library(light, padding, settled, empty, access, importEnabled, activeSong)
                                },
                                onLibrary = {}, onShare = {}, onSettings = {},
                                artwork = { modifier, _, _ -> Box(modifier.background(Color.DarkGray)) },
                                progress = {}, quality = {}, contentInsets = WindowInsets(0, 0, 0, 0)
                            )
                        }
                    }
                }
            }
        }
        compose.waitForIdle()
    }

    private fun menu(track: String = "Track 01") =
        compose.onNodeWithContentDescription("More options for $track").performScrollTo().performClick()

    private fun assertDialogTextColor(light: Boolean) {
        val layouts = mutableListOf<TextLayoutResult>()
        compose.onNodeWithText("Delete song permanently?").performSemanticsAction(
            SemanticsActions.GetTextLayoutResult
        ) { it(layouts) }
        assertEquals(immersiveColors(light).primaryText, layouts.single().layoutInput.style.color)
    }

    private fun screenshot(name: String) {
        val bounds = compose.onNodeWithTag("library-test").getUnclippedBoundsInRoot()
        compose.runOnIdle {
            val view = compose.activity.findViewById<View>(android.R.id.content)
            val bitmap = Bitmap.createBitmap((bounds.right - bounds.left).value.roundToInt(),
                (bounds.bottom - bounds.top).value.roundToInt(), Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.translate(-bounds.left.value, -bounds.top.value)
            view.draw(canvas)
            val dir = File("build/reports/library-preview").apply { mkdirs() }
            File(dir, "$name.png").outputStream().use {
                assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it))
            }
            bitmap.recycle()
        }
    }

    @Test fun lightLibraryConfirmationKeepsItsThemeUnderDarkParentAndCancels() {
        show(light = true)
        menu()
        compose.onNodeWithText("Delete permanently").performClick()
        assertDialogTextColor(light = true)
        compose.onNodeWithText("“Track 01” will be deleted from device storage, not just removed from Aruna. This cannot be undone.").assertIsDisplayed()
        compose.onNodeWithText("Cancel").performClick()
        assertNull(deleted)
        compose.onNodeWithText("Delete song permanently?").assertDoesNotExist()
    }

    @Test fun darkLibraryConfirmationKeepsItsThemeUnderLightParentAndConfirmsOnlyOnce() {
        show(light = false)
        menu()
        compose.onNodeWithText("Delete permanently").performClick()
        assertNull(deleted)
        assertDialogTextColor(light = false)
        compose.onNodeWithText("Delete permanently").performClick()
        assertEquals(songs.first(), deleted)
        compose.onNodeWithText("Delete song permanently?").assertDoesNotExist()
    }

    @Test fun indonesianLibraryLocalizesSortingSearchMenusAndWarning() {
        Locale.setDefault(Locale.forLanguageTag("id-ID"))
        show()
        compose.onNodeWithText("Perpustakaan").assertIsDisplayed()
        compose.onNodeWithText("40 lagu").assertIsDisplayed()
        compose.onNodeWithText("Putar semua").assertIsDisplayed()
        compose.onNodeWithContentDescription("Opsi lainnya untuk Track 01").performClick()
        compose.onNodeWithText("Hapus permanen").performClick()
        compose.onNodeWithText("Hapus lagu secara permanen?").assertIsDisplayed()
        compose.onNodeWithText("Batal").performClick()
        compose.onNodeWithContentDescription("Urutkan lagu").performClick()
        compose.onNodeWithText("Judul Z–A").performClick()
        compose.onNodeWithText("Judul Z–A").assertIsDisplayed()
        compose.onNodeWithText("Cari lagu, artis, album").performClick()
        compose.onNode(hasSetTextAction()).performTextInput("does not exist")
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Tidak ada lagu yang cocok").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("0 lagu").assertIsDisplayed()
        compose.onNodeWithContentDescription("Tutup pencarian").performClick()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("Tidak ada lagu yang cocok").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Tidak ada lagu yang cocok").assertIsDisplayed()
        compose.onNodeWithContentDescription("Hapus pencarian").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithText("40 lagu").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("40 lagu").assertIsDisplayed()
        compose.onNodeWithText("Cari lagu, artis, album").performClick()
        compose.onNode(hasSetTextAction()).performTextInput("Track 02")
        compose.waitUntil(5_000) { compose.onAllNodesWithText("1 lagu").fetchSemanticsNodes().isNotEmpty() }
        compose.onNode(hasText("Track 02") and hasText("Artist 2") and hasAnyAncestor(isDialog())).performClick()
        assertEquals(songs[1], played)
        assertEquals("1 lagu", librarySongCount(1))
    }

    @Test fun playbackQueueShareRefreshAndBackKeepTheirCallbacks() {
        show()
        compose.onNodeWithText("Play all").performClick()
        assertEquals(songs.first(), played)
        menu()
        compose.onNodeWithText("Play next").performClick()
        assertEquals(songs.first(), next)
        menu()
        compose.onNodeWithText("Share").performClick()
        assertEquals(songs.first(), shared)
        compose.onNodeWithContentDescription("Refresh library").performClick()
        assertEquals(1, refreshes)
        menu()
        compose.onNodeWithText("View album").performClick()
        compose.onNodeWithText("Album 1").assertIsDisplayed()
        compose.onNodeWithText("1 song").assertIsDisplayed()
        compose.onNodeWithContentDescription("Back").performClick()
        assertEquals(0, returns)
        menu()
        compose.onNodeWithText("View artist").performClick()
        compose.onAllNodesWithText("Artist 1").onFirst().assertIsDisplayed()
        compose.onNodeWithText("Track 01").performClick()
        assertEquals(songs.first(), played)
        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithContentDescription("Back").performClick()
        assertEquals(1, returns)
        assertEquals("1 song", librarySongCount(1))
    }

    private fun assertLastTrackAboveNavigation(name: String) {
        compose.onNode(hasScrollToIndexAction()).performScrollToIndex(songs.lastIndex)
        val action = compose.onNodeWithContentDescription("More options for Track 40")
        val navigation = compose.onNodeWithContentDescription("Back to Now Playing")
        assertTrue(action.getUnclippedBoundsInRoot().bottom <= navigation.getUnclippedBoundsInRoot().top)
        screenshot(name)
        action.performClick()
        compose.onNodeWithText("Play next").performClick()
        assertEquals(songs.last(), next)
    }

    @Test fun floatingLocateJumpsToPlayingSongWithoutReplayingIt() {
        show(activeSong = songs.last())
        compose.onNodeWithTag("locate-now-playing").assertIsDisplayed()
        compose.onNodeWithContentDescription("Locate now playing song").assertIsDisplayed()
        compose.onNodeWithTag("locate-now-playing").performClick()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("Track 40").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Track 40").assertIsDisplayed()
        compose.onNodeWithTag("locate-now-playing").assertDoesNotExist()
        assertNull("Locate should scroll, not restart playback", played)
    }

    @Test fun floatingLocateClearsFilteringWhenPlayingSongIsExcluded() {
        show(activeSong = songs.last())
        compose.onNodeWithText("Search songs, artists, albums").performClick()
        compose.onNode(hasSetTextAction()).performTextInput("Track 02")
        compose.onNodeWithContentDescription("Close search").performClick()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("1 song").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("locate-now-playing").assertIsDisplayed().performClick()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("40 songs").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Track 40").assertIsDisplayed()
        compose.onNodeWithTag("locate-now-playing").assertDoesNotExist()
        assertNull(played)
    }

    @Test fun floatingLocateIsAboveNavigationAtLargeFontAndAbsentWithoutPlayback() {
        show(light = false, width = 320, scale = 1.5f, overlay = true,
            activeSong = songs.last())
        compose.waitUntil(5_000) {
            compose.onAllNodesWithTag("locate-now-playing").fetchSemanticsNodes().isNotEmpty()
        }
        val locator = compose.onNodeWithTag("locate-now-playing").getUnclippedBoundsInRoot()
        val navigation = compose.onNodeWithContentDescription("Back to Now Playing")
            .getUnclippedBoundsInRoot()
        assertTrue("Floating control must clear navigation", locator.bottom <= navigation.top)
    }

    @Test fun floatingLocateDoesNotAppearWithoutAnActiveSong() {
        show(activeSong = null)
        compose.onNodeWithTag("locate-now-playing").assertDoesNotExist()
    }

    @Test fun finalSongRemainsAboveRealOverlayNavigation() {
        show(overlay = true)
        screenshot("library-light-overview")
        assertLastTrackAboveNavigation("library-light-last-track")
    }

    @Test fun narrowDarkLibraryWithLargeTextKeepsFinalSongActionReachable() {
        show(light = false, width = 320, scale = 1.5f, overlay = true)
        assertLastTrackAboveNavigation("library-dark-narrow-large-text")
    }

    @Test fun pullingTheImmersiveLibraryRequestsAScan() {
        show(light = false, overlay = true)
        compose.onNode(hasScrollToIndexAction()).performScrollToIndex(0)
        compose.onNode(hasScrollToIndexAction()).performTouchInput {
            swipeDown(startY = 20f, endY = 350f, durationMillis = 600)
        }
        compose.waitForIdle()
        assertEquals(1, refreshes)
    }

    @Test fun immersiveFirstRunRetainsFolderAndDeviceRecoveryWithoutAccess() {
        show(light = false, overlay = true, empty = true, access = false, importEnabled = true)
        assertEquals(0, folderImports + permissionRequests)
        compose.onNodeWithText("Choose a music folder").performScrollTo().performClick()
        assertEquals(1, folderImports)
        assertEquals(0, permissionRequests)
        compose.onNodeWithText("Find music on this device").performScrollTo().performClick()
        assertEquals(1, permissionRequests)
        screenshot("library-first-run-import")
    }

    @Test fun emptyImmersiveLibraryCanChooseAnotherFolderOrRescanWithLargeText() {
        show(light = true, width = 320, scale = 1.5f, overlay = true,
            empty = true, access = true, importEnabled = true)
        compose.onNodeWithText("Choose a music folder").performScrollTo().performClick()
        assertEquals(1, folderImports)
        compose.onNodeWithText("Scan again").performScrollTo().performClick()
        assertEquals(1, refreshes)
        val action = compose.onNodeWithText("Scan again").getUnclippedBoundsInRoot()
        val navigation = compose.onNodeWithContentDescription("Back to Now Playing").getUnclippedBoundsInRoot()
        assertTrue(action.bottom <= navigation.top)
        screenshot("library-first-run-large-text")
    }

    @Test fun destructiveTextHasNormalTextContrastInBothLibraryThemes() {
        for (light in listOf(true, false)) {
            val palette = immersiveColors(light)
            val lighter = maxOf(palette.destructiveText.luminance(), palette.surface.luminance())
            val darker = minOf(palette.destructiveText.luminance(), palette.surface.luminance())
            assertTrue((lighter + 0.05f) / (darker + 0.05f) >= 4.5f)
        }
    }
}
