package com.yohandeku32.nusamusic.ui

import android.animation.ValueAnimator

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import java.io.File
import java.util.Locale
import kotlin.math.roundToInt
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.util.ReflectionHelpers
import org.robolectric.util.ReflectionHelpers.ClassParameter

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@OptIn(ExperimentalTestApi::class)
class NavigationGlassUiTest {
    private var motionScale = mutableFloatStateOf(1f)
    private val scale = object : MotionDurationScale { override val scaleFactor get() = motionScale.floatValue }
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>(effectContext = scale)
    private val palette = mutableStateOf(Color(0xFFB82732))
    private val background = mutableStateOf(Color(0xFF28343C))
    private val enabled = mutableStateOf(true)
    private val direct = mutableStateOf(false)
    private val library = mutableStateOf(false)
    private val blur = HazeState(initialBlurEnabled = false)
    private val previousLocale = Locale.getDefault()
    private var settings = 0
    private var lyrics = 0

    @Before fun english() { Locale.setDefault(Locale.ENGLISH) }
    @After fun restore() {
        Locale.setDefault(previousLocale)
        ReflectionHelpers.callStaticMethod<Unit>(ValueAnimator::class.java, "setDurationScale", ClassParameter.from(Float::class.javaPrimitiveType, 1f))
    }

    private fun surface(live: Boolean = false, light: Boolean = false, split: Boolean = false, record: Boolean = true) {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                Box(Modifier.size(240.dp, 80.dp).background(background.value).testTag("navigation-preview")) {
                    if (live && record) Box(Modifier.fillMaxSize().hazeSource(blur).background(background.value))
                    if (split) Row(Modifier.fillMaxSize()) {
                        Box(Modifier.weight(1f).fillMaxHeight().background(Color(0xFFBA3045)))
                        Box(Modifier.weight(1f).fillMaxHeight().background(Color(0xFF244ABF)))
                    }
                    if (direct.value) Box(Modifier.padding(12.dp).fillMaxSize().immersiveGlassPill(
                        emphasized = enabled.value, liquidGlass = enabled.value, lightMode = light,
                        hazeState = if (live && record) blur else null,
                        artworkTint = if (enabled.value) palette.value else null
                    )) else ContinuousLiquidGlassSurface(
                        radius = 100.dp, lightMode = light, liquidGlassEnabled = enabled.value,
                        hazeState = if (live) blur else null, hazeAlpha = 1f,
                        artworkTint = palette.value, modifier = Modifier.padding(12.dp).fillMaxSize()
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

    @Test fun fallbackKeepsBackdropColourAndVisibleDetail() {
        surface(split = true)
        val bitmap = image("fallback-spatial-colour")
        val left = Color(bitmap.getPixel(60, 40))
        val right = Color(bitmap.getPixel(180, 40))
        assertTrue(left.red - left.blue > 0.14f)
        assertTrue(right.blue - right.red > 0.14f)
        assertTrue("Backdrop detail must survive the material", left.red - right.red > 0.17f)
    }

    @Test fun platformFallbackFollowsNewArtworkWithoutASecondOpaqueBase() {
        surface(live = true)
        val red = image("haze-fallback-red")
        compose.runOnIdle { palette.value = Color(0xFF2159D0) }
        settle()
        val blue = image("haze-fallback-blue")
        assertFalse(pixels(red).contentEquals(pixels(blue)))
        compose.runOnIdle { direct.value = true }
        settle()
        assertArrayEquals("Full Haze reveal must not retain a second tinted base", pixels(blue), pixels(image()))
        compose.runOnIdle { direct.value = false; background.value = Color(0xFF2159D0) }
        settle()
        image("artwork-blue").recycle()
        compose.runOnIdle { palette.value = Color(0xFFB82732); background.value = palette.value }
        settle()
        image("artwork-red").recycle()
    }

    @Test fun missingBackdropKeepsTheArtworkFallbackVisible() {
        surface(live = true, record = false)
        val material = pixels(image("waiting-for-backdrop"))
        compose.runOnIdle { direct.value = true }
        settle()
        assertArrayEquals(material, pixels(image()))
    }

    @Test fun savedDisabledAppearanceDoesNotChangeWithTheArtwork() {
        enabled.value = false
        surface()
        val before = pixels(image())
        compose.runOnIdle { palette.value = Color(0xFF2159D0); direct.value = true }
        settle()
        assertArrayEquals(before, pixels(image("disabled-original")))
    }

    @Test fun whiteArtworkKeepsWhiteNavigationGlyphContrast() {
        palette.value = Color.White
        background.value = Color.White
        surface(live = true, light = true)
        val bitmap = image("light-white-artwork")
        val plate = Color(bitmap.getPixel(120, 40))
        assertTrue("White glyph needs at least 3:1 against its plate", 1.05f / (plate.luminance() + 0.05f) >= 3f)
    }

    @Test fun reducedMotionAppliesArtworkColourWithoutWaitingForTheTween() {
        surface()
        compose.runOnIdle {
            motionScale.floatValue = 0f
            ReflectionHelpers.callStaticMethod<Unit>(ValueAnimator::class.java, "setDurationScale", ClassParameter.from(Float::class.javaPrimitiveType, 0f))
            assertFalse(ValueAnimator.areAnimatorsEnabled())
            palette.value = Color(0xFF2159D0)
        }
        repeat(3) {
            compose.mainClock.advanceTimeByFrame()
            compose.waitForIdle()
        }
        val immediate = pixels(image("reduced-motion-blue"))
        settle()
        assertArrayEquals(immediate, pixels(image("reduced-motion-settled")))
    }

    @Test fun savedDisabledGlassKeepsPlayerNavigationActions() {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                MaterialTheme {
                    ImmersiveArtworkPlayer(
                        title = "Aruna", artist = "Navigation preview", composerNames = "", titleFontFamily = FontFamily.Serif,
                        titleFontSize = 34f, hasSong = true, isPlaying = false, isFavorite = false,
                        shuffleEnabled = false, repeatMode = 0, lyricsOpen = false, lyricsEnabled = true,
                        onPlayPause = {}, onPrevious = {}, onNext = {}, onShuffle = {}, onRepeat = {}, onFavorite = {},
                        onLyrics = { lyrics++ }, onDiscovery = {}, onLibrary = { library.value = true },
                        onHome = { library.value = false }, onLibraryClose = { library.value = false },
                        libraryOpen = library.value, glassArtworkColor = palette.value, liquidGlassEnabled = false,
                        libraryContent = { _, _ -> Box(Modifier.fillMaxSize().background(Color(0xFF090A0D))) },
                        onShare = {}, onSettings = { settings++ },
                        artwork = { modifier, _, _ -> Box(modifier.background(palette.value)) },
                        progress = {}, quality = {}, modifier = Modifier.size(411.dp, 800.dp).testTag("navigation-preview")
                    )
                }
            }
        }
        settle()
        val red = image("player-disabled-red")
        compose.runOnIdle { palette.value = Color(0xFF2159D0) }
        settle()
        val blue = image("player-disabled-blue")
        assertNotEquals(red.getPixel(240, 750), blue.getPixel(240, 750))
        compose.onNodeWithContentDescription("Settings").performClick()
        compose.onNodeWithContentDescription("Open lyrics").performClick()
        assertEquals(1, settings)
        assertEquals(1, lyrics)
        compose.onNodeWithContentDescription("Library").performClick()
        settle()
        compose.onNodeWithContentDescription("Back to Now Playing").performClick()
        settle()
        compose.onNodeWithContentDescription("Library").assertIsDisplayed()
        image("player-disabled-returned").recycle()
    }
}
