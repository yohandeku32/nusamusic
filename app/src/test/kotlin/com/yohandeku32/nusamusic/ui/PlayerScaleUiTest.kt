package com.yohandeku32.nusamusic.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Density
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
@OptIn(ExperimentalTestApi::class)
class PlayerScaleUiTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val scale = mutableFloatStateOf(1f)
    private val hosted = mutableStateOf(true)
    private val library = mutableStateOf(false)
    private var profileDensity = 0f
    private var settings = 0
    private var plays = 0
    private val previousLocale = Locale.getDefault()
    private val songs = (1L..40L).map { Song(it, "Scale track $it", "Artist", "Album", "content://scale/$it", 180000L, it) }
    private val cover = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888).apply {
        eraseColor(android.graphics.Color.rgb(22, 47, 81))
        for (x in 80..119) for (y in 0..199) setPixel(x, y, android.graphics.Color.rgb(240, 30, 30))
    }
    private val blur = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888).apply {
        eraseColor(android.graphics.Color.rgb(22, 47, 81))
    }

    @Before fun english() { Locale.setDefault(Locale.ENGLISH) }
    @After fun restore() { Locale.setDefault(previousLocale) }

    private fun show(width: Int = 411, height: Int = 800, fontScale: Float = 1f) {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, fontScale)) {
                MaterialTheme(colorScheme = darkColorScheme()) {
                    Box(Modifier.size(width.dp, height.dp).testTag("scale-preview")) {
                        val content: @Composable () -> Unit = {
                            ImmersiveArtworkPlayer(
                                title = if (fontScale > 1f) "A long song title for the enlarged player" else "Aruna",
                                artist = "Player scale preview", composerNames = "", titleFontFamily = FontFamily.Serif,
                                titleFontSize = 34f, hasSong = true, isPlaying = false, isFavorite = false,
                                shuffleEnabled = false, repeatMode = 0, lyricsOpen = false, lyricsEnabled = true,
                                onPlayPause = { plays++ }, onPrevious = {}, onNext = {}, onShuffle = {}, onRepeat = {},
                                onFavorite = {}, onLyrics = {}, onDiscovery = {}, onLibrary = { library.value = true },
                                libraryOpen = library.value, onLibraryClose = { library.value = false },
                                onHome = { library.value = false }, onShare = {}, onSettings = { settings++ },
                                libraryContent = { settled, padding ->
                                    val currentDensity = LocalDensity.current.density
                                    SideEffect { profileDensity = currentDensity }
                                    ImmersiveMusicLibrary(
                                        songs = songs, currentSong = songs.first(), isPlaying = false,
                                        isVisible = false, lightMode = false, hasMusicAccess = true, isScanning = false,
                                        emptyMessage = "", onReturnToPlayer = { library.value = false },
                                        onPlay = { _, _ -> }, onPlayNext = {}, onShare = {}, onDeleteSong = {},
                                        onTogglePlay = {}, onScanMusic = {}, onRequestPermission = {},
                                        artwork = { _, modifier, _, _ -> Box(modifier.background(Color(0xFF20354C))) },
                                        bottomContentPadding = padding
                                    )
                                },
                                artwork = { modifier, hero, fraction ->
                                    ImmersiveArtworkScene(cover, blur, Color(0xFF162F51), hero,
                                        modifier = modifier, heroWidthFraction = fraction, animateChanges = false)
                                }, progress = {}, quality = {}, modifier = Modifier.fillMaxSize(),
                                contentInsets = WindowInsets(0, 24, 0, 32)
                            )
                        }
                        if (hosted.value) PlayerScaleHost(scale.floatValue, content) else content()
                    }
                }
            }
        }
        settle()
    }

    private fun settle() {
        compose.mainClock.advanceTimeByFrame()
        compose.waitForIdle()
        compose.mainClock.advanceTimeBy(1000)
        compose.waitForIdle()
    }

    private fun image(name: String): Bitmap {
        compose.waitForIdle()
        val bounds = compose.onNodeWithTag("scale-preview").getUnclippedBoundsInRoot()
        lateinit var bitmap: Bitmap
        compose.runOnIdle {
            bitmap = Bitmap.createBitmap((bounds.right - bounds.left).value.roundToInt(),
                (bounds.bottom - bounds.top).value.roundToInt(), Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.translate(-bounds.left.value, -bounds.top.value)
            compose.activity.findViewById<View>(android.R.id.content).draw(canvas)
            val folder = File("build/reports/player-scale-preview").apply { mkdirs() }
            File(folder, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
        return bitmap
    }

    private fun pixels(bitmap: Bitmap) = IntArray(bitmap.width * bitmap.height).also {
        bitmap.getPixels(it, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
    }

    @Test fun originalScaleMatchesTheUnwrappedPlayer() {
        hosted.value = false
        show()
        val original = pixels(image("player-original"))
        compose.runOnIdle { hosted.value = true }
        settle()
        assertArrayEquals(original, pixels(image("player-100")))
    }

    @Test fun maximumScaleEnlargesArtworkAndControlsWithRealTapTargets() {
        show()
        val before = image("player-size-100")
        val normal = compose.onNodeWithContentDescription("Settings").fetchSemanticsNode().boundsInRoot
        compose.runOnIdle { scale.floatValue = 1.25f }
        settle()
        val after = image("player-size-125")
        val enlarged = compose.onNodeWithContentDescription("Settings").fetchSemanticsNode().boundsInRoot
        assertEquals(normal.width * 1.25f, enlarged.width, 1f)
        fun redSpan(bitmap: Bitmap) = (0 until bitmap.width).count {
            val colour = Color(bitmap.getPixel(it, 40))
            colour.red > 0.7f && colour.green < 0.3f && colour.blue < 0.3f
        }
        assertTrue("Artwork is magnified inside its responsive frame", redSpan(after) > redSpan(before))
        compose.onNodeWithContentDescription("Settings").performTouchInput { click(center) }
        assertEquals(1, settings)
    }

    @Test fun narrowEnlargedPlayerKeepsControlsInsideSafeAreasWithLargeText() {
        scale.floatValue = 1.25f
        show(width = 320, height = 568, fontScale = 1.5f)
        repeat(2) { compose.onNodeWithTag("scale-preview").performTouchInput { swipeUp() }; settle() }
        val viewport = compose.onNodeWithTag("scale-preview").fetchSemanticsNode().boundsInRoot
        for (label in listOf("Shuffle off", "Previous", "Play", "Next", "Repeat off", "Library", "Settings")) {
            val node = compose.onNodeWithContentDescription(label).assertIsDisplayed()
            val bounds = node.fetchSemanticsNode().boundsInRoot
            assertTrue("$label fits horizontally", bounds.left >= viewport.left - 1f && bounds.right <= viewport.right + 1f)
            assertTrue("$label clears the system bar", bounds.bottom <= viewport.bottom - 31f)
        }
        compose.onNodeWithContentDescription("Play").performTouchInput { click(center) }
        assertEquals(1, plays)
        image("player-125-narrow-large-text").recycle()
    }

    @Test
    @Config(qualifiers = "w800dp-h400dp")
    fun landscapeEnlargementKeepsNavigationReachable() {
        scale.floatValue = 1.25f
        show(width = 800, height = 400)
        compose.onNodeWithTag("scale-preview").performTouchInput {
            swipe(Offset(700f, 340f), Offset(700f, 60f))
        }
        settle()
        compose.onNodeWithContentDescription("Settings").assertIsDisplayed().performTouchInput { click(center) }
        assertEquals(1, settings)
        image("player-125-landscape").recycle()
    }

    @Test fun libraryReturnsToScreenDensityAndItsLastRowClearsNavigation() {
        scale.floatValue = 1.25f
        show(width = 320)
        compose.onNodeWithTag("scale-preview").performTouchInput { swipeUp() }
        settle()
        compose.onNodeWithContentDescription("Library").performClick()
        settle()
        assertEquals(1f, profileDensity, 0f)
        compose.onNode(hasScrollToIndexAction()).performScrollToIndex(songs.lastIndex)
        val menu = compose.onNodeWithContentDescription("More options for Scale track 40").assertIsDisplayed()
        val home = compose.onNodeWithContentDescription("Back to Now Playing").assertIsDisplayed()
        assertTrue(menu.fetchSemanticsNode().boundsInRoot.bottom <= home.fetchSemanticsNode().boundsInRoot.top)
        val settingsBounds = compose.onNodeWithContentDescription("Settings").fetchSemanticsNode().boundsInRoot
        assertTrue(settingsBounds.right <= compose.onNodeWithTag("scale-preview").fetchSemanticsNode().boundsInRoot.right)
        image("library-from-player-125").recycle()
        home.performTouchInput { click(center) }
        settle()
        compose.onNodeWithContentDescription("Library").assertIsDisplayed()
    }

    @Test fun settingSupportsKeyboardPersistenceAndReset() {
        Locale.setDefault(Locale.forLanguageTag("id-ID"))
        val preferences = compose.activity.getSharedPreferences("scale-ui-test", Context.MODE_PRIVATE)
        preferences.edit().clear().commit()
        compose.setContent {
            var value by remember { mutableFloatStateOf(PlayerScalePreference.read(preferences)) }
            CompositionLocalProvider(LocalDensity provides Density(1f, 1.5f)) {
            MaterialTheme(colorScheme = darkColorScheme()) { Surface {
            Column(Modifier.width(280.dp).padding(16.dp).testTag("scale-preview")) {
                PlayerScaleSetting(value) { value = it; PlayerScalePreference.write(preferences, it) }
            } } }
            }
        }
        val slider = compose.onNodeWithContentDescription("Skala player utama")
        slider.performSemanticsAction(SemanticsActions.SetProgress) { it(1.15f) }
        assertEquals(1.15f, PlayerScalePreference.read(preferences), 0f)
        slider.performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        slider.performKeyInput { pressKey(Key.DirectionRight) }
        assertEquals(1.2f, PlayerScalePreference.read(preferences), 0.001f)
        slider.assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "120%"))
        image("player-scale-setting").recycle()
        compose.onNodeWithText("Atur ulang").performClick()
        assertEquals(1f, PlayerScalePreference.read(preferences), 0f)
    }
}
