package com.yohandeku32.nusamusic.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.yohandeku32.nusamusic.data.EmbeddedLyricsSelector
import java.io.File
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@OptIn(ExperimentalTestApi::class)
class MiniLyricsUiTest {
    private val durationScale = object : MotionDurationScale {
        val amount = mutableFloatStateOf(1f)
        override val scaleFactor get() = amount.floatValue
    }
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>(effectContext = durationScale)
    private val now = mutableLongStateOf(2200L)
    private val motion = LyricsMotionState(0)
    private val enabled = mutableStateOf(true)
    private val full = mutableStateOf(false)
    private val xml = """<tt><body><p begin="1s" end="5s"><span begin="1s" end="4s">mencintaimu </span><span begin="4s" end="5s">selalu</span></p><p begin="5s" end="7s"><span begin="5s" end="7s">baris berikutnya</span></p></body></tt>"""
    private val data = mutableStateOf(EmbeddedLyricsSelector.select(listOf(xml), 10_000))

    private fun show(width: Int = 411, fontScale: Float = 1f, light: Boolean = false) {
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, fontScale)) {
                MaterialTheme {
                    Box(Modifier.requiredSize(width.dp, 800.dp).background(if (light) Color(0xFFF0F1F3) else Color(0xFF141820))) {
                        ImmersiveArtworkPlayer(title = "Mini lyric preview", artist = "Aruna", composerNames = "",
                            titleFontFamily = FontFamily.SansSerif, titleFontSize = 34f, hasSong = true,
                            isPlaying = false, isFavorite = false, shuffleEnabled = false, repeatMode = 0,
                            lyricsOpen = full.value, lyricsEnabled = true, lightMode = light,
                            onPlayPause = {}, onPrevious = {}, onNext = {}, onShuffle = {}, onRepeat = {},
                            onFavorite = {}, onLyrics = { full.value = !full.value }, onDiscovery = {},
                            onLibrary = {}, onShare = {}, onSettings = {}, artwork = { modifier, _, _ ->
                                Box(modifier.background(if (light) Color(0xFFF0F1F3) else Color(0xFF25363F)))
                            },
                            progress = {}, quality = {}, modifier = Modifier.fillMaxSize(),
                            miniLyrics = if (enabled.value) ({ mode -> MiniLyricsLine(data.value, now, motion, mode) }) else null,
                            lyrics = { modifier -> Text("Full lyrics", modifier) })
                    }
                }
            }
        }
        compose.waitForIdle()
    }

    private fun bitmap(name: String? = null): Bitmap {
        lateinit var bitmap: Bitmap
        compose.runOnIdle {
            val view = compose.activity.findViewById<View>(android.R.id.content)
            bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888).also { view.draw(Canvas(it)) }
            if (name != null) {
                val file = File("build/reports/lyrics-companion-preview/$name.png").apply { parentFile!!.mkdirs() }
                file.outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
            }
        }
        return bitmap
    }

    @Test fun oneAnimatedLineSitsAboveTheTitleAndRestoresPixelsOnPauseAndSeek() {
        show()
        val line = compose.onNodeWithContentDescription("mencintaimu selalu", useUnmergedTree = true)
        line.assertIsDisplayed()
        val bounds = line.getUnclippedBoundsInRoot()
        val title = compose.onNodeWithText("Mini lyric preview").getUnclippedBoundsInRoot()
        assertTrue(bounds.bottom <= title.top)
        val first = bitmap("mini-lyrics-phone")
        compose.mainClock.advanceTimeBy(250)
        assertTrue(first.sameAs(bitmap()))
        compose.runOnIdle { now.longValue = 3100 }
        compose.waitForIdle()
        assertFalse("The held word must retain the main lyric's animated geometry", first.sameAs(bitmap()))
        compose.runOnIdle { now.longValue = 2200; motion.reset(2200) }
        compose.waitForIdle()
        assertTrue(first.sameAs(bitmap()))
        assertEquals(title, compose.onNodeWithText("Mini lyric preview").getUnclippedBoundsInRoot())
        first.recycle()
    }

    @Test fun offFullLyricsAndMissingTimingRemoveTheMiniContent() {
        show()
        compose.runOnIdle { full.value = true }
        compose.waitForIdle()
        compose.onNodeWithContentDescription("mencintaimu selalu", useUnmergedTree = true).assertDoesNotExist()
        compose.runOnIdle { full.value = false; enabled.value = false }
        compose.waitForIdle()
        compose.onNodeWithContentDescription("mencintaimu selalu", useUnmergedTree = true).assertDoesNotExist()
        compose.runOnIdle { enabled.value = true; data.value = EmbeddedLyricsSelector.select(listOf("Plain lyrics"), 10_000) }
        compose.waitForIdle()
        compose.onNodeWithContentDescription("Plain lyrics", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test fun aLongLineStaysInOneBandAndPansInsteadOfWrapping() {
        val long = (1..12).map { "kata$it " }
        val body = long.mapIndexed { index, word -> "<span begin=\"${index + 1}s\" end=\"${index + 2}s\">$word</span>" }.joinToString("")
        data.value = EmbeddedLyricsSelector.select(listOf("<tt><body><p begin=\"1s\" end=\"13s\">$body</p></body></tt>"), 14_000)
        show(width = 320, fontScale = 1.8f)
        val text = long.joinToString("").trim()
        val node = compose.onNodeWithContentDescription(text, useUnmergedTree = true)
        node.assertIsDisplayed()
        val before = node.getUnclippedBoundsInRoot()
        val first = bitmap()
        compose.runOnIdle { now.longValue = 11_500; motion.reset(11_500) }
        compose.waitForIdle()
        assertEquals(before, node.getUnclippedBoundsInRoot())
        assertTrue(before.bottom - before.top <= 60.dp)
        assertFalse(first.sameAs(bitmap("mini-lyrics-long-large-text")))
        first.recycle()
    }

    @Test fun lightModeUsesDarkReadableInkAndReducedMotionKeepsTheLine() {
        show(light = true)
        val animated = bitmap("mini-lyrics-light")
        compose.runOnIdle { durationScale.amount.floatValue = 0f }
        compose.waitForIdle()
        compose.onNodeWithContentDescription("mencintaimu selalu", useUnmergedTree = true).assertIsDisplayed()
        assertFalse(animated.sameAs(bitmap("mini-lyrics-reduced-motion")))
        animated.recycle()
    }

    @Test fun settingsToggleAndStoredChoiceSurviveAReopen() {
        val preferences = compose.activity.getSharedPreferences("mini-lyrics-test", Context.MODE_PRIVATE)
        preferences.edit().clear().commit()
        assertFalse(MiniLyricsPreference.read(preferences))
        enabled.value = false
        compose.setContent { MaterialTheme { MiniLyricsSetting(enabled.value) {
            enabled.value = it; MiniLyricsPreference.write(preferences, it)
        } } }
        compose.onNode(isToggleable()).assertIsOff().performClick().assertIsOn()
        assertTrue(MiniLyricsPreference.read(preferences))
        compose.onNode(isToggleable()).performClick().assertIsOff()
        assertFalse(MiniLyricsPreference.read(preferences))
        preferences.edit().putString("mini_lyrics_enabled", "invalid").commit()
        assertFalse(MiniLyricsPreference.read(preferences))
    }

    @Test fun turningTheMiniOffRestoresTheOriginalPlayerPixels() {
        enabled.value = false
        show()
        val first = bitmap()
        compose.runOnIdle { enabled.value = true }
        compose.waitForIdle()
        compose.onNodeWithContentDescription("mencintaimu selalu", useUnmergedTree = true).assertIsDisplayed()
        compose.runOnIdle { enabled.value = false }
        compose.waitForIdle()
        compose.mainClock.advanceTimeBy(300)
        compose.waitForIdle()
        assertTrue(first.sameAs(bitmap()))
        first.recycle()
    }

    @Test fun aNewLineAndTrackReplaceTheOldTextWhileLineSyncedTextRemainsOneLine() {
        show()
        compose.runOnIdle { now.longValue = 5400; motion.reset(5400) }
        compose.waitForIdle()
        compose.onNodeWithContentDescription("baris berikutnya", useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithContentDescription("mencintaimu selalu", useUnmergedTree = true).assertDoesNotExist()
        compose.runOnIdle {
            data.value = EmbeddedLyricsSelector.select(listOf("[00:01]Line timed lyrics\n[00:03]Next line"), 8000)
            now.longValue = 2200; motion.reset(2200)
        }
        compose.waitForIdle()
        compose.onNodeWithContentDescription("Line timed lyrics", useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithContentDescription("baris berikutnya", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test fun lineChangesEaseInWithPlaybackTimeAndFreezeWhilePaused() {
        data.value = EmbeddedLyricsSelector.select(listOf("[00:01]First line\n[00:03]Next line\n[00:05]Last line"), 8000)
        now.longValue = 3030
        show()
        val entering = bitmap("mini-line-entering")
        compose.mainClock.advanceTimeBy(500)
        assertTrue("Pause must freeze the transition as well as the word animation", entering.sameAs(bitmap()))
        compose.runOnIdle { now.longValue = 3240 }
        compose.waitForIdle()
        val settled = bitmap("mini-line-settled")
        assertFalse("Changing a lyric line must ease in instead of appearing at full opacity instantly", entering.sameAs(settled))
        compose.runOnIdle { now.longValue = 3030; motion.reset(3030) }
        compose.waitForIdle()
        assertTrue("Seeking must restore the exact transition pose", entering.sameAs(bitmap()))
        entering.recycle(); settled.recycle()
    }
}
