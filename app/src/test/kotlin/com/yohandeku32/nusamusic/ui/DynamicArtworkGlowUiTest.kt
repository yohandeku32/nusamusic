package com.yohandeku32.nusamusic.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import java.io.File
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.math.sqrt
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
class DynamicArtworkGlowUiTest {
    private val scale = object : MotionDurationScale { override val scaleFactor = 1f }
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>(effectContext = scale)
    private val enabled = mutableStateOf(false)
    private val frame = mutableStateOf<Bitmap?>(null)
    private val source = mutableStateOf<Bitmap?>(null)
    private val previousLocale = Locale.getDefault()

    @Before fun english() { Locale.setDefault(Locale.ENGLISH) }
    @After fun restore() { Locale.setDefault(previousLocale) }

    private fun light(right: Boolean): Bitmap = light(if (right) 1f else 0f)

    private fun light(progress: Float): Bitmap {
        val pixels = IntArray(96 * 96) { index ->
            val x = index % 96
            val y = index / 96
            val dx = (x - (18f + progress * 60f)) / 48f
            val dy = (y - 52f) / 54f
            val weight = (1f - sqrt(dx * dx + dy * dy)).coerceIn(0f, 1f)
            val r = (13f + weight * (45f + progress * 170f)).roundToInt()
            val g = (23f + weight * (170f - progress * 45f)).roundToInt()
            val b = (34f + weight * (180f - progress * 155f)).roundToInt()
            0xFF000000.toInt() or (r shl 16) or (g shl 8) or b
        }
        return Bitmap.createBitmap(pixels, 96, 96, Bitmap.Config.ARGB_8888)
    }

    private fun show(width: Int = 411, lightMode: Boolean = false) {
        val cover = light(false)
        source.value = cover
        val base = Bitmap.createBitmap(96, 96, Bitmap.Config.ARGB_8888).apply { eraseColor(0xFF243844.toInt()) }
        compose.mainClock.autoAdvance = false
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                MaterialTheme {
                    Box(Modifier.requiredWidth(width.dp).height(760.dp).testTag("glow-player")) {
                        ImmersiveArtworkPlayer(
                            title = "Cahaya di Ruang Musik", artist = "Aruna • pratinjau pengujian", composerNames = "",
                            titleFontFamily = FontFamily.Default, titleFontSize = 30f, modifier = Modifier.fillMaxSize(),
                            hasSong = true, isPlaying = true, lightMode = lightMode,
                            isFavorite = false, shuffleEnabled = false, repeatMode = 0,
                            lyricsOpen = false, lyricsEnabled = true,
                            onPlayPause = {}, onPrevious = {}, onNext = {}, onShuffle = {},
                            onRepeat = {}, onFavorite = {}, onLyrics = {}, onDiscovery = {},
                            onLibrary = {}, onShare = {}, onSettings = {}, liquidGlassEnabled = false,
                            artwork = { modifier, height, fraction ->
                                ImmersiveArtworkScene(source.value, base, Color.Black, height,
                                    lightMode = lightMode, modifier = modifier, heroWidthFraction = fraction,
                                    animateChanges = false,
                                    dynamicGlow = if (enabled.value) frame.value else null)
                            }, progress = { modifier ->
                                Text("0:42                                      3:24", modifier,
                                    color = if (lightMode) Color.Black else Color.White)
                            }, quality = {}, contentInsets = WindowInsets(0, 0, 0, 0)
                        )
                    }
                }
            }
        }
        compose.mainClock.advanceTimeBy(400)
        compose.waitForIdle()
    }

    private fun image(name: String? = null): Bitmap {
        compose.waitForIdle()
        val bounds = compose.onNodeWithTag("glow-player").getUnclippedBoundsInRoot()
        lateinit var bitmap: Bitmap
        compose.runOnIdle {
            bitmap = Bitmap.createBitmap((bounds.right - bounds.left).value.roundToInt(),
                (bounds.bottom - bounds.top).value.roundToInt(), Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.translate(-bounds.left.value, -bounds.top.value)
            compose.activity.findViewById<View>(android.R.id.content).draw(canvas)
            if (name != null) {
                val dir = File("build/reports/artwork-glow-preview").apply { mkdirs() }
                File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            }
        }
        return bitmap
    }

    private fun pixels(bitmap: Bitmap): IntArray = IntArray(bitmap.width * bitmap.height).also {
        bitmap.getPixels(it, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
    }

    private fun settle() {
        compose.mainClock.advanceTimeByFrame()
        compose.waitForIdle()
        compose.mainClock.advanceTimeBy(240)
        compose.waitForIdle()
    }

    @Test fun disabledMatchesOriginalAndTurningOffRestoresIt() {
        show()
        val before = image("dark-off")
        compose.runOnIdle { frame.value = createArtworkGlow(light(true)) }
        compose.mainClock.advanceTimeBy(220)
        assertArrayEquals(pixels(before), pixels(image()))
        compose.runOnIdle { enabled.value = true }
        settle()
        val on = image("dark-on")
        assertFalse(pixels(before).contentEquals(pixels(on)))
        compose.runOnIdle { enabled.value = false }
        settle()
        assertArrayEquals(pixels(before), pixels(image("dark-restored")))
    }

    @Test fun diffuseLayerRendersAboveItsBase() {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                Box(Modifier.size(320.dp).background(Color(0xFF243844)).testTag("glow-player")) {
                    DynamicArtworkGlow(frame.value, Modifier.fillMaxSize())
                }
            }
        }
        compose.mainClock.advanceTimeBy(200)
        val before = pixels(image())
        compose.runOnIdle { frame.value = createArtworkGlow(light(true)) }
        settle()
        assertFalse(before.contentEquals(pixels(image("isolated-glow"))))
    }

    @Test fun movingSourceProducesAContinuousNativePlayerPreview() {
        show()
        compose.runOnIdle { enabled.value = true; frame.value = createArtworkGlow(light(false)) }
        settle()
        val left = image("motion-000")
        for (sample in 1..8) {
            compose.runOnIdle {
                val next = light(sample / 8f)
                source.value = next
                frame.value = createArtworkGlow(next)
            }
            for (step in 1..5) {
                compose.mainClock.advanceTimeBy(40)
                compose.waitForIdle()
                image("motion-%03d".format((sample - 1) * 5 + step)).recycle()
            }
        }
        fun lowerPixels(bitmap: Bitmap) = IntArray(bitmap.width * 150).also {
            bitmap.getPixels(it, 0, bitmap.width, 0, 550, bitmap.width, 150)
        }
        assertFalse(lowerPixels(left).contentEquals(lowerPixels(image())))
    }

    @Test fun narrowLightPlayerRetainsControlsWithGlowEnabled() {
        show(width = 320, lightMode = true)
        compose.runOnIdle { enabled.value = true; frame.value = createArtworkGlow(light(true)) }
        compose.mainClock.advanceTimeBy(240)
        compose.onNodeWithText("Cahaya di Ruang Musik").assertIsDisplayed()
        image("light-narrow-on").recycle()
    }

    @Test fun toggleWorksAndExplainsWhenArtworkIsDisabled() {
        val artwork = mutableStateOf(true)
        compose.setContent {
            MaterialTheme { Column(Modifier.width(320.dp)) {
                DynamicGlowSetting(enabled.value, artwork.value) { enabled.value = it }
            } }
        }
        compose.onNode(isToggleable()).assertIsOff().performClick().assertIsOn()
        compose.runOnIdle { artwork.value = false }
        compose.onNodeWithText("Turn on Animated artwork to see this effect.").assertIsDisplayed()
        compose.onNode(isToggleable()).assertIsOn().performClick().assertIsOff()
    }
}
