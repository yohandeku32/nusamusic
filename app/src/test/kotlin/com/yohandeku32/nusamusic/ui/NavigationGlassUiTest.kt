package com.yohandeku32.nusamusic.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
class NavigationGlassUiTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val background = mutableStateOf(Color(0xFF832E36))
    private val reference = mutableStateOf(false)
    private val enabled = mutableStateOf(true)
    private val library = mutableStateOf(false)
    private val previousLocale = Locale.getDefault()
    private var settings = 0
    private var lyrics = 0
    private var discoveries = 0

    @Before fun english() { Locale.setDefault(Locale.ENGLISH) }
    @After fun restore() { Locale.setDefault(previousLocale) }

    private fun surface(light: Boolean = false) {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                Box(Modifier.size(240.dp, 80.dp).background(background.value).testTag("navigation-preview")) {
                    if (reference.value) Box(
                        Modifier.padding(12.dp).fillMaxSize().immersiveGlassPill(7.dp, light)
                    ) else ImmersiveNavigationSurface(
                        radius = 100.dp, lightMode = light, modifier = Modifier.padding(12.dp).fillMaxSize()
                    )
                }
            }
        }
        settle()
    }

    private fun player(light: Boolean = false, width: Int = 411, height: Int = 800, fontScale: Float = 1f, hasSong: Boolean = true) {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, fontScale)) {
                MaterialTheme(colorScheme = if (light) lightColorScheme() else darkColorScheme()) {
                    ImmersiveArtworkPlayer(
                        title = "Aruna", artist = "Hi-Res navigation preview", composerNames = "",
                        titleFontFamily = FontFamily.Serif, titleFontSize = 34f,
                        hasSong = hasSong, isPlaying = false, lightMode = light, isFavorite = false,
                        shuffleEnabled = false, repeatMode = 0, lyricsOpen = false, lyricsEnabled = true,
                        onPlayPause = {}, onPrevious = {}, onNext = {}, onShuffle = {}, onRepeat = {}, onFavorite = {},
                        onLyrics = { lyrics++ }, onDiscovery = { discoveries++ }, onLibrary = { library.value = true },
                        onHome = { library.value = false }, onLibraryClose = { library.value = false },
                        libraryOpen = library.value, libraryLightMode = light,
                        glassArtworkColor = background.value, liquidGlassEnabled = enabled.value,
                        libraryContent = { _, _ -> Box(Modifier.fillMaxSize().background(if (light) Color.White else Color.Black)) },
                        onShare = {}, onSettings = { settings++ },
                        artwork = { modifier, _, _ -> Box(modifier.background(background.value)) },
                        progress = {}, quality = {
                            Box(Modifier.immersiveGlassPill(7.dp, light).padding(horizontal = 9.dp, vertical = 4.dp)) {
                                Text("Hi-Res", color = if (light) Color(0xFF171A20) else Color.White,
                                    fontSize = 11.sp, lineHeight = 13.sp)
                            }
                        },
                        modifier = Modifier.size(width.dp, height.dp).testTag("navigation-preview")
                    )
                }
            }
        }
        settle()
    }

    private fun settle() {
        compose.mainClock.advanceTimeByFrame()
        compose.waitForIdle()
        compose.mainClock.advanceTimeBy(800)
        compose.waitForIdle()
    }

    private fun image(name: String? = null): Bitmap {
        compose.waitForIdle()
        val bounds = compose.onNodeWithTag("navigation-preview").getUnclippedBoundsInRoot()
        lateinit var bitmap: Bitmap
        compose.runOnIdle {
            bitmap = Bitmap.createBitmap((bounds.right - bounds.left).value.roundToInt(),
                (bounds.bottom - bounds.top).value.roundToInt(), Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.translate(-bounds.left.value, -bounds.top.value)
            compose.activity.findViewById<View>(android.R.id.content).draw(canvas)
            if (name != null) {
                val folder = File("build/reports/navigation-glass-preview").apply { mkdirs() }
                File(folder, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            }
        }
        return bitmap
    }

    private fun pixels(bitmap: Bitmap) = IntArray(bitmap.width * bitmap.height).also {
        bitmap.getPixels(it, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
    }

    private fun materialPixels(bitmap: Bitmap) = IntArray(120 * 32).also {
        bitmap.getPixels(it, 0, 120, 60, 24, 120, 32)
    }

    private fun matchesHiRes(light: Boolean) {
        surface(light)
        val navigation = image(if (light) "hires-light-navigation" else "hires-dark-navigation")
        compose.runOnIdle { reference.value = true }
        settle()
        val badge = image(if (light) "hires-light-reference" else "hires-dark-reference")
        assertArrayEquals("The shared material must match outside the different corner radii",
            materialPixels(badge), materialPixels(navigation))
    }

    @Test fun darkNavigationMatchesHiResMaterial() = matchesHiRes(false)
    @Test fun lightNavigationMatchesHiResMaterial() = matchesHiRes(true)

    @Test fun artworkChangesShowThroughWithoutAnExtraColourTween() {
        surface()
        compose.runOnIdle { background.value = Color(0xFF235DC2) }
        repeat(3) { compose.mainClock.advanceTimeByFrame(); compose.waitForIdle() }
        val immediate = image("hires-artwork-blue")
        val colour = Color(immediate.getPixel(120, 40))
        assertTrue(colour.blue - colour.red > 0.35f)
        settle()
        assertArrayEquals(pixels(immediate), pixels(image()))
        compose.runOnIdle { background.value = Color(0xFFB82732) }
        settle()
        image("hires-artwork-red").recycle()
    }

    @Test fun lightMaterialSupportsDarkInk() {
        background.value = Color.White
        surface(light = true)
        val plate = Color(image("hires-white-artwork").getPixel(120, 40))
        val ink = Color(0xFF171A20)
        assertTrue((plate.luminance() + 0.05f) / (ink.luminance() + 0.05f) >= 4.5f)
    }

    @Test fun nativePlayerKeepsActionsAndLibraryReturn() {
        player()
        image("hires-player-dark").recycle()
        compose.onNodeWithContentDescription("Settings").performClick()
        compose.onNodeWithContentDescription("Open lyrics").performClick()
        compose.onNodeWithContentDescription("Discover").performClick()
        assertEquals(1, settings)
        assertEquals(1, lyrics)
        assertEquals(1, discoveries)
        compose.onNodeWithContentDescription("Library").performClick()
        settle()
        compose.onNodeWithContentDescription("Back to Now Playing").performClick()
        settle()
        compose.onNodeWithContentDescription("Library").assertIsDisplayed()
    }

    @Test fun legacyGlassPreferenceCannotReintroduceTheOpaqueLayer() {
        background.value = Color.Black
        player(hasSong = false)
        val before = pixels(image("hires-player-empty"))
        compose.runOnIdle { enabled.value = false }
        settle()
        assertArrayEquals(before, pixels(image()))
    }

    @Test fun narrowLightPlayerWithLargeTextKeepsNavigationReachable() {
        background.value = Color(0xFFF8F9FB)
        player(light = true, width = 320, height = 568, fontScale = 1.5f)
        compose.onNodeWithTag("navigation-preview").performTouchInput { swipeUp() }
        settle()
        compose.onNodeWithContentDescription("Library").assertIsDisplayed()
        image("hires-player-light-large-text").recycle()
        compose.onNodeWithContentDescription("Settings").performClick()
        assertEquals(1, settings)
    }
}
