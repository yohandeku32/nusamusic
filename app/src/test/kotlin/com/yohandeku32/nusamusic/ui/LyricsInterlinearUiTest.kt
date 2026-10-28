package com.yohandeku32.nusamusic.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine
import com.mocharealm.accompanist.lyrics.ui.composable.lyrics.measureSyllablesAndDetermineAnimation
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
class LyricsInterlinearUiTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val now = mutableIntStateOf(4_500)
    private val motion = LyricsMotionState(0)
    private var plan: LyricsInterlinearLayout? = null
    private var sought: Int? = null
    private var height = 0
    private val original = "镜子里面只剩自己呢"
    private val phonetic = "jing zi li mian zhi sheng zi ji ne"

    private fun chineseXml(): String {
        val readings = phonetic.split(' ')
        val body = original.mapIndexed { i, char -> "<span begin=\"${i + 1}s\" end=\"${i + 2}s\">$char</span>" }.joinToString("")
        val reading = readings.mapIndexed { i, text -> "<span begin=\"${i + 1}s\" end=\"${i + 2}s\">$text</span>" }.joinToString("")
        return """<tt xmlns:m="http://www.w3.org/ns/ttml#metadata"><body><p begin="1s" end="10s">$body<span m:role="x-roman">$reading</span></p></body></tt>"""
    }

    private fun show(width: Int = 411, scale: Float = 1f, right: Boolean = false, xml: String = chineseXml()) {
        val decoded = LyricsDecoder.decodeWithTiming(xml, 15_000)
        val line = decoded.lyrics.lines.single() as KaraokeLine
        val reading = decoded.pronunciations.getValue(0)
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, scale)) {
                val measurer = rememberTextMeasurer()
                val style = TextStyle(fontSize = 34.sp, lineHeight = 44.sp, fontWeight = FontWeight.Bold)
                val units = measureSyllablesAndDetermineAnimation(line.syllables, measurer, style, true,
                    measurer.measure(" ", style).size.width.toFloat())
                val layout = measureInterlinearLyrics(line, reading, units, style, lyricsReadingStyle(style, right), measurer,
                    (width - 64).toFloat(), 1f, right)
                SideEffect { plan = layout }
                Column(Modifier.requiredWidth(width.dp).height(760.dp).background(Color(0xFF141820)).padding(16.dp, 32.dp)) {
                    CenteredKaraokeLine(line, { now.intValue }, motion, style,
                        onSeek = { sought = it.start }, onMainHeight = { height = it }, alignEnd = right,
                        pronunciation = reading)
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
        val bitmap = bitmap()
        val file = File("build/reports/lyrics-companion-preview/$name.png").apply { requireNotNull(parentFile).mkdirs() }
        file.outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
        bitmap.recycle()
    }

    private fun assertAligned(layout: LyricsInterlinearLayout, width: Float) {
        val main = layout.sourceRows.flatten()
        val reading = layout.readingRows.flatten()
        for (cell in layout.cells) {
            assertTrue(cell.left >= -0.1f && cell.left + cell.width <= width + 0.1f)
            val ids = cell.reading ?: continue
            val first = main[cell.source.first]
            val last = main[cell.source.last]
            val mainRight = last.position.x + last.textLayoutResult.getHorizontalPosition(last.syllable.content.trimEnd().length, true)
            val mainCenter = (first.position.x + mainRight) * 0.5f
            val readingFirst = reading[ids.first]
            val readingLast = reading[ids.last]
            val readingRight = readingLast.position.x + readingLast.textLayoutResult.getHorizontalPosition(readingLast.syllable.content.trimEnd().length, true)
            val readingCenter = (readingFirst.position.x + readingRight) * 0.5f * layout.readingScale
            assertEquals("Reading must be centered under its source group", mainCenter, readingCenter, 0.6f)
            assertTrue(readingFirst.position.y * layout.readingScale > first.position.y)
        }
    }

    @Test fun chinesePinyinCentersUnderEachCharacterAndCanSeek() {
        show()
        assertEquals(9, requireNotNull(plan).cells.size)
        assertAligned(requireNotNull(plan), 347f)
        compose.onNodeWithContentDescription(phonetic, useUnmergedTree = true).assertIsDisplayed().performTouchInput { click() }
        assertEquals(1_000, sought)
        preview("interlinear-chinese")
    }

    @Test fun japaneseRomajiCentersUnderItsWholeKanjiGroup() {
        val xml = """<tt xmlns:m="http://www.w3.org/ns/ttml#metadata"><body><p begin="1s" end="4s"><span begin="1s" end="2s">世</span><span begin="2s" end="3s">界</span><span begin="3s" end="4s">へ</span><span m:role="x-roman"><span begin="1s" end="3s">sekai</span><span begin="3s" end="4s">e</span></span></p></body></tt>"""
        show(xml = xml)
        assertEquals(0..1, requireNotNull(plan).cells.first().source)
        assertAligned(requireNotNull(plan), 347f)
        preview("interlinear-japanese")
    }

    @Test fun narrowLargeTextWrapsEachCharacterTogetherWithItsReading() {
        show(width = 320, scale = 1.8f)
        val layout = requireNotNull(plan)
        assertTrue(layout.sourceRows.size >= 2)
        assertEquals(layout.sourceRows.size, layout.readingRows.size)
        assertAligned(layout, 256f)
        for (row in layout.cells.groupBy { it.row }.values) {
            for ((left, right) in row.zipWithNext()) assertTrue(left.left + left.width <= right.left)
        }
        preview("interlinear-narrow-large-text")
    }

    @Test fun duetAlignmentMovesBothRowsTogether() {
        show(width = 320, right = true)
        val layout = requireNotNull(plan)
        assertAligned(layout, 256f)
        for (row in layout.cells.groupBy { it.row }.values)
            assertEquals(256f, row.last().left + row.last().width, 0.1f)
        preview("interlinear-duet-right")
    }

    @Test fun pairedLayoutFreezesOnPauseAndRestoresItsPoseAfterSeek() {
        show()
        compose.runOnIdle { now.intValue = 1_500; motion.reset(1_500) }
        compose.waitForIdle()
        val beforeHeight = height
        val first = bitmap()
        compose.mainClock.advanceTimeBy(200)
        compose.waitForIdle()
        val paused = bitmap()
        assertTrue(first.sameAs(paused))
        compose.runOnIdle { now.intValue = 7_000 }
        compose.waitForIdle()
        compose.runOnIdle { now.intValue = 1_500; motion.reset(1_500) }
        compose.waitForIdle()
        val restored = bitmap()
        assertTrue(first.sameAs(restored))
        assertEquals(beforeHeight, height)
        first.recycle(); paused.recycle(); restored.recycle()
    }
}
