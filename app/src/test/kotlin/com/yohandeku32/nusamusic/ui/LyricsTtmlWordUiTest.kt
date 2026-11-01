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
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine
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
class LyricsTtmlWordUiTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val pretty = mutableStateOf(true)
    private val now = mutableIntStateOf(1700)
    private val motion = LyricsMotionState(0)
    private var height = 0

    private fun xml(): String {
        val syllables = listOf("men", "cin", "tai", "mu ").mapIndexed { index, text ->
            "<span begin=\"${1000 + index * 300}ms\" end=\"${1300 + index * 300}ms\">$text</span>"
        }.joinToString(if (pretty.value) "\n    " else "") +
            "<span begin=\"2.2s\" end=\"3s\">selalu</span>"
        return """<tt xmlns:m="http://www.w3.org/ns/ttml#metadata"><body><p begin="1s" end="3s">$syllables<span m:role="x-roman">$syllables</span><span m:role="x-translation">永远爱你</span></p></body></tt>"""
    }

    private fun show(width: Int, scale: Float) {
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, scale)) {
                val raw = xml()
                val decoded = remember(raw) { LyricsDecoder.decodeWithTiming(raw, 10_000) }
                val line = decoded.lyrics.lines.single() as KaraokeLine
                Column(Modifier.requiredWidth(width.dp).height(760.dp).background(Color(0xFF141820)).padding(16.dp, 32.dp)) {
                    CenteredKaraokeLine(line, { now.intValue }, motion,
                        TextStyle(fontSize = 34.sp, lineHeight = 44.sp, fontWeight = FontWeight.Bold),
                        onSeek = {}, onMainHeight = { height = it }, pronunciation = decoded.pronunciations[0])
                }
            }
        }
        compose.waitForIdle()
    }

    private fun bitmap(): Bitmap {
        lateinit var result: Bitmap
        compose.runOnIdle {
            val view = compose.activity.findViewById<View>(android.R.id.content)
            result = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888).also { view.draw(Canvas(it)) }
        }
        return result
    }

    private fun verify(width: Int, scale: Float) {
        show(width, scale)
        compose.onAllNodesWithContentDescription("mencintaimu selalu", useUnmergedTree = true).assertCountEquals(2)
        val first = bitmap()
        val firstHeight = height
        compose.runOnIdle { pretty.value = false }
        compose.waitForIdle()
        val compact = bitmap()
        assertTrue("Formatting must not create spaces, change wrapping or alter the animation", first.sameAs(compact))
        assertEquals(firstHeight, height)
        val file = File("build/reports/lyrics-companion-preview/joined-ttml-word-$width.png").apply { parentFile!!.mkdirs() }
        file.outputStream().use { assertTrue(first.compress(Bitmap.CompressFormat.PNG, 100, it)) }
        first.recycle(); compact.recycle()
    }

    @Test fun formattedMainAndLatinReadingRenderLikeTheOriginalJoinedWord() = verify(411, 1f)
    @Test fun formattingDoesNotCreateNewWordBreaksOnANarrowLargeTextScreen() = verify(320, 1.8f)
}
