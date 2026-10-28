package com.yohandeku32.nusamusic.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine
import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.core.model.synced.SyncedLine
import com.yohandeku32.nusamusic.data.LyricsDecoder
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
class LyricsCompanionUiTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val now = mutableIntStateOf(0)
    private val motion = LyricsMotionState(0)
    private var sought: Int? = null
    private var measured = 0
    private lateinit var inputModeManager: InputModeManager
    private val translation = "Selamat malam, semoga tidurmu tenang sampai pagi."
    private val pronunciation = "wan an yuan ni jin ye hao meng"
    private val xml = """
        <tt xmlns="http://www.w3.org/ns/ttml" xmlns:m="http://www.w3.org/ns/ttml#metadata" xmlns:a="http://music.apple.com/lyric-ttml-internal">
        <head><metadata><a:transliterations><a:transliteration><a:text for="L1">
            <span begin="1s" end="2s">wan</span><span begin="2s" end="3s">an</span><span begin="3s" end="4s">yuan</span><span begin="4s" end="5s">ni</span><span begin="5s" end="6s">jin</span><span begin="6s" end="7s">ye</span><span begin="7s" end="8s">hao</span><span begin="8s" end="9s">meng</span>
        </a:text></a:transliteration></a:transliterations></metadata></head>
        <body><p a:key="L1" begin="1s" end="9s"><span begin="1s" end="2s">晚</span><span begin="2s" end="3s">安</span><span begin="3s" end="4s">愿</span><span begin="4s" end="5s">你</span><span begin="5s" end="6s">今</span><span begin="6s" end="7s">夜</span><span begin="7s" end="8s">好</span><span begin="8s" end="9s">梦</span><span m:role="x-translation">$translation</span></p></body></tt>
    """.trimIndent()

    private fun show(width: Int = 411, fontScale: Float = 1f, right: Boolean = false, lineTimed: Boolean = false) {
        val decoded = LyricsDecoder.decodeWithTiming(xml, 10_000)
        val line = decoded.lyrics.lines.single() as KaraokeLine
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, fontScale)) {
                inputModeManager = LocalInputModeManager.current
                Column(Modifier.requiredWidth(width.dp).height(760.dp).background(Color(0xFF141820))
                    .padding(horizontal = 16.dp, vertical = 28.dp).testTag("companion-preview")) {
                    val style = TextStyle(fontSize = 30.sp, lineHeight = 40.sp, fontWeight = FontWeight.Bold)
                    if (lineTimed) LineSyncedLyricsRow(SyncedLine("Good night", translation, 1_000, 9_000),
                        { now.intValue }, motion, style, onMainHeight = { measured = it }, alignEnd = right) { sought = 1_000 }
                    else CenteredKaraokeLine(line, { now.intValue }, motion, style,
                        onSeek = { sought = it.start }, onMainHeight = { measured = it }, alignEnd = right,
                        pronunciation = decoded.pronunciations[0])
                }
            }
        }
        compose.waitForIdle()
    }

    private fun bitmap(): Bitmap {
        var result: Bitmap? = null
        compose.runOnIdle {
            val view = compose.activity.findViewById<View>(android.R.id.content)
            result = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888).also { view.draw(Canvas(it)) }
        }
        return requireNotNull(result)
    }

    private fun preview(name: String) {
        val image = bitmap()
        val dir = File("build/reports/lyrics-companion-preview").apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { assertTrue(image.compress(Bitmap.CompressFormat.PNG, 100, it)) }
        image.recycle()
    }

    private fun peakRed(image: Bitmap, bounds: androidx.compose.ui.unit.DpRect): Int {
        var peak = 0
        for (y in bounds.top.value.toInt() until bounds.bottom.value.toInt()) {
            for (x in bounds.left.value.toInt() until bounds.right.value.toInt()) {
                peak = maxOf(peak, android.graphics.Color.red(image.getPixel(x, y)))
            }
        }
        return peak
    }

    @Test fun companionsAreVisibleBeforePlaybackAndBothTapTheAuthoredStart() {
        show()
        compose.onNodeWithContentDescription(pronunciation, useUnmergedTree = true).assertIsDisplayed()
            .performTouchInput { click() }
        assertEquals(1_000, sought)
        sought = null
        compose.onNodeWithText(translation, useUnmergedTree = true).assertIsDisplayed().performTouchInput { click() }
        assertEquals(1_000, sought)
        val translated = compose.onNodeWithText(translation, useUnmergedTree = true).getUnclippedBoundsInRoot()
        val spoken = compose.onNodeWithContentDescription(pronunciation, useUnmergedTree = true).getUnclippedBoundsInRoot()
        assertTrue(translated.top >= spoken.bottom)
        assertTrue(measured >= translated.bottom.value - 28f)
        preview("phone-before-playback")
    }

    @Test fun lineSyncedTranslationIsDisplayedAndKeyboardCanSeek() {
        show(lineTimed = true)
        compose.onNodeWithText(translation, useUnmergedTree = true).assertIsDisplayed()
        compose.runOnIdle { inputModeManager.requestInputMode(InputMode.Keyboard) }
        compose.onRoot().performKeyInput { pressKey(Key.Tab) }
        compose.waitForIdle()
        compose.onNode(isFocused()).performKeyInput { pressKey(Key.Enter) }
        assertEquals(1_000, sought)
        preview("line-synced-translation")
    }

    @Test fun pronunciationAndTranslationWrapAt320dpWithLargeSystemText() {
        show(width = 320, fontScale = 1.8f)
        for (node in listOf(
            compose.onNodeWithText(translation, useUnmergedTree = true),
            compose.onNodeWithContentDescription(pronunciation, useUnmergedTree = true)
        )) {
            node.assertIsDisplayed()
            val bounds = node.getUnclippedBoundsInRoot()
            assertTrue(bounds.left >= 32.dp && bounds.right <= 288.dp)
            assertTrue(bounds.bottom < 760.dp)
        }
        preview("narrow-large-text")
    }

    @Test fun rightAlignedDuetCompanionsRemainBelowTheirMainLine() {
        show(right = true)
        compose.onNodeWithText(translation, useUnmergedTree = true).assertIsDisplayed().performTouchInput { click() }
        assertEquals(1_000, sought)
        preview("duet-right")
    }

    @Test fun timedPronunciationRespondsToPlaybackAndPausingKeepsTheSamePixels() {
        show()
        compose.runOnIdle { now.intValue = 1_700 }
        compose.waitForIdle()
        val first = bitmap()
        compose.mainClock.advanceTimeBy(200)
        compose.waitForIdle()
        val paused = bitmap()
        assertTrue(first.sameAs(paused))
        compose.runOnIdle { now.intValue = 4_700 }
        compose.waitForIdle()
        val later = bitmap()
        assertFalse(first.sameAs(later))
        preview("phone-during-playback")
        first.recycle(); paused.recycle(); later.recycle()
    }

    @Test fun pronunciationHighlightIsClearlyBrighterThanUpcomingWords() {
        show()
        val bounds = compose.onNodeWithContentDescription(pronunciation, useUnmergedTree = true)
            .getUnclippedBoundsInRoot()
        val before = bitmap()
        compose.runOnIdle { now.intValue = 2_200 }
        compose.waitForIdle()
        val active = bitmap()
        val difference = peakRed(active, bounds) - peakRed(before, bounds)
        assertTrue("Pronunciation highlight must increase rendered brightness by at least 90/255; was $difference", difference >= 90)
        preview("strong-pronunciation-highlight")
        before.recycle(); active.recycle()
    }

    @Test fun translationBrightensWithItsLineWithoutChangingItsLayout() {
        show(lineTimed = true)
        val node = compose.onNodeWithText(translation, useUnmergedTree = true)
        val bounds = node.getUnclippedBoundsInRoot()
        val before = bitmap()
        compose.runOnIdle { now.intValue = 2_000 }
        compose.waitForIdle()
        val active = bitmap()
        assertEquals(bounds, node.getUnclippedBoundsInRoot())
        val difference = peakRed(active, bounds) - peakRed(before, bounds)
        assertTrue("Active translation must increase rendered brightness by at least 65/255; was $difference", difference >= 65)
        preview("strong-translation-highlight")
        compose.runOnIdle { now.intValue = 10_000 }
        compose.waitForIdle()
        val released = bitmap()
        assertEquals(peakRed(before, bounds), peakRed(released, bounds))
        before.recycle(); active.recycle(); released.recycle()
    }

    @Test fun viewportReceivesDecodedCompanionsAndKeepsFollowingAndSeeking() {
        compose.mainClock.autoAdvance = false
        val decoded = LyricsDecoder.decodeWithTiming(xml, 15_000)
        val allLines = SyncedLyrics(listOf(
            SyncedLine("Musik mengisi malam", "Music fills the evening", 0, 900),
            decoded.lyrics.lines.single(),
            SyncedLine("Nada berikutnya menunggu", "The next melody is waiting", 9_500, 12_000),
            SyncedLine("Dengarkan sampai akhir", "Listen until the end", 12_000, 15_000)
        ))
        val position = mutableLongStateOf(4_700)
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                Box(Modifier.requiredWidth(411.dp).height(760.dp).background(Color(0xFF141820))) {
                    SyncedLyricsView(allLines, position, true, rememberLazyListState(), motion,
                        onSeek = { sought = it.start }, fontSize = 30f,
                        pronunciations = mapOf(1 to decoded.pronunciations.getValue(0)))
                }
            }
        }
        compose.mainClock.advanceTimeBy(160)
        compose.waitForIdle()
        compose.onNodeWithContentDescription(pronunciation, useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithText(translation, useUnmergedTree = true).assertIsDisplayed().performTouchInput { click() }
        assertEquals(1_000, sought)
        preview("lyrics-viewport")
    }
}
