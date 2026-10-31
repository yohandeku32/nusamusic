package com.yohandeku32.nusamusic.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.yohandeku32.nusamusic.model.Song
import java.io.File
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w800dp-h914dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@OptIn(ExperimentalTestApi::class)
class VinylSongListTest {
    private val motion = object : MotionDurationScale { override var scaleFactor by mutableFloatStateOf(1f) }
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>(effectContext = motion)
    private val songs = mutableStateOf((1L..30L).map {
        Song(it, "Record $it", "Sleeve fixture $it", "Fixture", "content://fixture/$it", 4000, it)
    })
    private val current = mutableStateOf<Long?>(2L)
    private var plays = 0
    private var toggles = 0
    private var delete: Long? = null
    private lateinit var list: LazyListState
    private lateinit var inputMode: InputModeManager

    private fun show(width: Int = 411, height: Int = 800, font: Float = 1f) {
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, font)) {
                MaterialTheme(colorScheme = darkColorScheme()) {
                    inputMode = LocalInputModeManager.current
                    list = rememberLazyListState()
                    Column(Modifier.requiredSize(width.dp, height.dp).background(Color(0xFF111111))) {
                        Text("Record collection (fixture)", Modifier.padding(24.dp), color = Color.White)
                        VinylSongList(songs.value, list, current.value, true, Color(0xFF111111),
                            "Play", "Pause", "Unknown artist", { plays++; current.value = it.id },
                            { delete = it.id }, { toggles++ },
                            artwork = { song, modifier ->
                                val palette = listOf(Color(0xFF714C85), Color(0xFF1B7E83), Color(0xFFC37B3A),
                                    Color(0xFFB94C54), Color(0xFF41659C))
                                Box(modifier.background(Brush.linearGradient(listOf(
                                    palette[(song.id % palette.size).toInt()], Color(0xFF202424))))) {
                                    Text("COVER FIXTURE ${song.id}", Modifier.padding(top = 180.dp, start = 18.dp),
                                        color = Color.White)
                                }
                            }, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
        compose.waitForIdle()
    }

    private fun snapshot(name: String, width: Int = 411, height: Int = 800) {
        compose.runOnIdle {
            val image = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            compose.activity.findViewById<View>(android.R.id.content).draw(Canvas(image))
            val directory = File("build/reports/vinyl-preview").apply { mkdirs() }
            File(directory, "$name.png").outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
            image.recycle()
        }
    }

    @Test fun stackedSleevesExposeTitlesAndTapUsesTheRightSong() {
        show()
        val first = compose.onNodeWithTag("vinyl-sleeve-1").getUnclippedBoundsInRoot()
        val second = compose.onNodeWithTag("vinyl-sleeve-2").getUnclippedBoundsInRoot()
        assertTrue("Sleeves overlap like record jackets", second.top < first.bottom)
        assertTrue("Each sleeve leaves a readable header exposed", second.top - first.top >= 60.dp)
        snapshot("record-crate-phone")
        compose.onNodeWithTag("vinyl-sleeve-1").performTouchInput {
            click(androidx.compose.ui.geometry.Offset(width / 2f, 24f))
        }
        assertEquals(1L, current.value)
        assertEquals(1, plays)
        compose.onNodeWithTag("vinyl-sleeve-1").assertIsSelected().performTouchInput {
            click(androidx.compose.ui.geometry.Offset(width / 2f, 24f))
        }
        assertEquals(1, toggles)
        compose.onNodeWithTag("vinyl-sleeve-1")
            .performSemanticsAction(SemanticsActions.OnLongClick) { it() }
        assertEquals(1L, delete)
    }

    @Test fun browsingAndFilteringDoNotStartPlaybackAndLastSleeveIsReachable() {
        show()
        compose.onNodeWithTag("vinyl-sleeves").performScrollToIndex(29)
        compose.onNodeWithTag("vinyl-sleeve-30").assertIsDisplayed()
        assertEquals("Browsing alone never starts playback", 0, plays)
        compose.onNodeWithTag("vinyl-sleeve-30").performClick()
        assertEquals(30L, current.value)
        assertEquals(1, plays)
        compose.runOnIdle { songs.value = songs.value.filter { it.id % 2L == 0L }.reversed() }
        compose.onNodeWithTag("vinyl-sleeves").performScrollToIndex(0)
        compose.onNodeWithTag("vinyl-sleeve-30").assertIsSelected()
        assertEquals("Scroll and filter preserve playback", 1, plays)
    }

    @Test fun largeTextUsesSeparatedCoversAndKeyboardRemainsUsable() {
        show(width = 320, height = 720, font = 2f)
        val first = compose.onNodeWithTag("vinyl-sleeve-1").getUnclippedBoundsInRoot()
        val second = compose.onNodeWithTag("vinyl-sleeve-2").getUnclippedBoundsInRoot()
        assertTrue("Large text removes overlap", first.bottom <= second.top)
        compose.runOnIdle { inputMode.requestInputMode(InputMode.Keyboard) }
        compose.onNodeWithTag("vinyl-sleeve-1").performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        compose.onNodeWithTag("vinyl-sleeve-1").performKeyInput { pressKey(Key.Enter) }
        assertEquals(1, plays)
        val titleLayout = mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
        compose.onNodeWithText("Record 1", useUnmergedTree = true)
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(titleLayout) }
        snapshot("record-crate-large-text", 320, 720)
        assertFalse("The short selected title must not be ellipsized: ${titleLayout.single().size}",
            titleLayout.single().isLineEllipsized(titleLayout.single().lineCount - 1))
    }

    @Test fun wideAndReducedMotionKeepCoversBoundedAndAligned() {
        show(width = 800)
        compose.runOnIdle { motion.scaleFactor = 0f }
        val first = compose.onNodeWithTag("vinyl-sleeve-1").getUnclippedBoundsInRoot()
        val second = compose.onNodeWithTag("vinyl-sleeve-2").getUnclippedBoundsInRoot()
        assertEquals(420f, (first.right - first.left).value, 1f)
        assertEquals(190f, first.left.value, 1f)
        assertEquals(first.left, second.left)
        snapshot("record-crate-wide-reduced-motion", 800, 800)
    }
}
