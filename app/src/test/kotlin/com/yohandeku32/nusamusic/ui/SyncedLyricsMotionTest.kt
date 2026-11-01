package com.yohandeku32.nusamusic.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.core.model.synced.SyncedLine
import java.io.File
import kotlin.math.abs
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
class SyncedLyricsMotionTest {
    private val durationScale = object : MotionDurationScale {
        var factor = 1f
        override val scaleFactor: Float get() = factor
    }
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>(effectContext = durationScale)
    private val text = listOf("Musik mengisi ruangan", "Suara yang terus berjalan",
        "Kalimat ini mulai mengalir", "Baris berikutnya menyusul",
        "Lalu berhenti dengan lembut", "Kita dengarkan sampai akhir")
    private val lyrics = SyncedLyrics(text.mapIndexed { index, words ->
        SyncedLine(words, null, index * 2_000, (index + 1) * 2_000)
    })
    private val position = mutableLongStateOf(4_500)
    private val motion = LyricsMotionState(0)
    private val duetRight = mutableStateOf(emptySet<Int>())
    private lateinit var list: LazyListState
    private var sought: Int? = null

    private fun show(width: Int = 411, scale: Float = 1f) {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, scale)) {
                list = rememberLazyListState()
                Box(Modifier.requiredWidth(width.dp).height(660.dp).background(Color(0xFF141820))) {
                    SyncedLyricsView(lyrics, position, true, list, motion,
                        onSeek = { line ->
                            sought = line.start
                            position.longValue = line.start.toLong()
                            motion.reset(line.start)
                        }, fontSize = 30f, modifier = Modifier.fillMaxSize().testTag("lyrics-motion"),
                        duetRightLines = duetRight.value)
                }
            }
        }
        compose.mainClock.advanceTimeBy(160)
        compose.waitForIdle()
    }

    private fun audio(now: Long) {
        compose.runOnIdle { position.longValue = now }
        compose.mainClock.advanceTimeBy(20)
        compose.waitForIdle()
    }

    private fun top(index: Int) = compose.onNodeWithText(text[index]).getUnclippedBoundsInRoot().top.value

    private fun frame(index: Int) {
        val bounds = compose.onNodeWithTag("lyrics-motion").getUnclippedBoundsInRoot()
        compose.runOnIdle {
            val bitmap = Bitmap.createBitmap((bounds.right - bounds.left).value.roundToInt(),
                (bounds.bottom - bounds.top).value.roundToInt(), Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.translate(-bounds.left.value, -bounds.top.value)
            compose.activity.findViewById<View>(android.R.id.content).draw(canvas)
            val dir = File("build/reports/lyrics-motion-preview").apply { mkdirs() }
            File(dir, "frame-%03d.png".format(index)).outputStream().use {
                assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it))
            }
            bitmap.recycle()
        }
    }

    @Test fun sentencesFollowWithDifferentTravelAndPauseFreezesTheRenderedPose() {
        show()
        var largestGap = Float.NEGATIVE_INFINITY
        var smallestGap = Float.POSITIVE_INFINITY
        var firstTop = 0f
        var lastTop = 0f
        for (step in 0..85) {
            audio(5_500L + step * 20L)
            val activeTop = top(2)
            val nextTop = top(3)
            val gap = nextTop - activeTop
            largestGap = maxOf(largestGap, gap)
            smallestGap = minOf(smallestGap, gap)
            if (step == 0) firstTop = activeTop
            lastTop = activeTop
            if (step % 2 == 0) frame(step / 2)
            if (step == 32) {
                compose.mainClock.advanceTimeBy(100)
                compose.waitForIdle()
                val held = top(3)
                compose.mainClock.advanceTimeBy(400)
                compose.waitForIdle()
                assertEquals("Pause must also hold a transition in progress", held, top(3), 0.1f)
            }
        }
        assertTrue("The lyric actually advances", lastTop < firstTop - 12f)
        assertTrue("Adjacent sentences need distinct movement", largestGap - smallestGap > 0.75f)
        compose.mainClock.advanceTimeBy(100)
        compose.waitForIdle()
        val paused = top(3)
        compose.mainClock.advanceTimeBy(600)
        compose.waitForIdle()
        assertEquals(paused, top(3), 0.1f)
    }

    @Test fun aLyricTapSeeksAndManualBrowsingKeepsControl() {
        show()
        compose.onNodeWithText(text[3]).performClick()
        compose.mainClock.advanceTimeBy(160)
        assertEquals(6_000, sought)
        assertEquals(6_000L, position.longValue)
        compose.onNodeWithTag("lyrics-motion").performTouchInput { swipeUp(durationMillis = 200) }
        compose.mainClock.advanceTimeBy(600)
        compose.waitForIdle()
        val before = list.firstVisibleItemIndex to list.firstVisibleItemScrollOffset
        audio(6_400)
        compose.mainClock.advanceTimeBy(200)
        compose.waitForIdle()
        assertEquals(before, list.firstVisibleItemIndex to list.firstVisibleItemScrollOffset)
    }

    @Test fun reducedMotionRemovesSentenceOffsetsEvenDuringAPausedTransition() {
        show()
        val baselineGap = top(3) - top(2)
        for (now in 5_500L..6_140L step 20L) audio(now)
        compose.mainClock.advanceTimeBy(100)
        compose.waitForIdle()
        assertTrue(abs((top(3) - top(2)) - baselineGap) > 0.75f)
        compose.runOnIdle { durationScale.factor = 0f }
        compose.mainClock.advanceTimeBy(160)
        compose.waitForIdle()
        assertEquals(baselineGap, top(3) - top(2), 0.1f)
    }

    @Test fun refreshingDuetMetadataKeepsSentenceMotionConnected() {
        show()
        compose.runOnIdle { duetRight.value = setOf(5) }
        compose.mainClock.advanceTimeBy(160)
        var smallest = Float.POSITIVE_INFINITY
        var largest = Float.NEGATIVE_INFINITY
        for (now in 5_500L..6_700L step 20L) {
            audio(now)
            val gap = top(3) - top(2)
            smallest = minOf(smallest, gap)
            largest = maxOf(largest, gap)
        }
        assertTrue(largest - smallest > 0.75f)
    }

    @Test fun wrappedSentencesRemainReadableOnNarrowLargeTextLayout() {
        show(width = 320, scale = 1.5f)
        for (now in 4_520L..5_400L step 20L) audio(now)
        val bounds = compose.onNodeWithText(text[2]).getUnclippedBoundsInRoot()
        assertTrue(bounds.left >= 0.dp && bounds.right <= 320.dp)
        assertTrue(abs(bounds.top.value) < 660f)
        compose.onNodeWithText(text[2]).assertIsDisplayed().performClick()
        assertEquals(4_000, sought)
    }
}
