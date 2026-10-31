package com.yohandeku32.nusamusic.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.yohandeku32.nusamusic.VinylRecord
import java.io.File
import kotlin.math.roundToInt
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h914dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@OptIn(ExperimentalTestApi::class)
class VinylPlayerUiTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private var plays = 0
    private var previous = 0
    private var next = 0
    private var settings = 0
    private var lyricsOpens = 0
    private var shares = 0
    private var favorites = 0
    private var qualityClicks = 0
    private var sought = 0L
    private val playing = mutableStateOf(false)
    private val shuffled = mutableStateOf(false)
    private val repeated = mutableStateOf(0)
    private val title = mutableStateOf("The Emptiness Machine")
    private val qualityVisible = mutableStateOf(true)
    private val qualityLabel = mutableStateOf("Hi-Res")
    private lateinit var inputModeManager: InputModeManager

    private fun show(width: Int = 411, height: Int = 914, fontScale: Float = 1f, hasSong: Boolean = true,
        safeBottom: Int = 0, titleSize: Float = 34f) {
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, fontScale)) {
                inputModeManager = LocalInputModeManager.current
                MaterialTheme(colorScheme = darkColorScheme()) {
                    LazyColumn(Modifier.requiredSize(width.dp, (height - safeBottom).dp).testTag("vinyl-preview")) {
                        item {
                            TurntablePlayer(
                                title = title.value, artist = "LINKIN PARK",
                                titleFontFamily = FontFamily.Serif, titleFontSize = titleSize,
                                viewportHeight = (height - safeBottom).dp, isPlaying = playing.value,
                                isVisible = true, hasSong = hasSong,
                                positionMsState = remember { mutableStateOf(75_000L) }, durationMs = 190_000L,
                                shuffleEnabled = shuffled.value, repeatMode = repeated.value,
                                labels = TurntableLabels("Play", "Pause", "Previous", "Next",
                                    "Shuffle", "Shuffle on", "Shuffle off", "Repeat", "Repeat off",
                                    "Repeat all", "Repeat one", "Seek", "Settings", "Playback options"),
                                onPlayPause = { plays++; playing.value = !playing.value },
                                onPrevious = { previous++ }, onNext = { next++ }, onSeek = { sought = it },
                                onShuffle = { shuffled.value = !shuffled.value },
                                onRepeat = { repeated.value = (repeated.value + 1) % 3 },
                                onSettings = { settings++ },
                                avatar = { Box(it.testTag("artist-profile").background(Color.Black, CircleShape)) },
                                record = { VinylRecord(song = null, isPlaying = false, modifier = it.testTag("vinyl-record")) },
                                quality = {
                                    if (qualityVisible.value && hasSong) VinylQualityBadge(
                                        qualityLabel.value, true, true, { qualityClicks++ }, Modifier.testTag("quality")
                                    )
                                },
                                footer = {
                                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                                        IconButton(onClick = { lyricsOpens++ }) { Icon(Icons.Default.KeyboardArrowDown, "Lyrics") }
                                        Spacer(Modifier.weight(1f))
                                        IconButton(onClick = { shares++ }) { Icon(Icons.Default.Share, "Share") }
                                        IconButton(onClick = { favorites++ }) { Icon(Icons.Default.FavoriteBorder, "Favorite") }
                                    }
                                },
                                emptyState = {
                                    if (!hasSong) Text("Choose a song")
                                    Box(Modifier.height(1.dp).testTag("player-bottom"))
                                }
                            )
                        }
                    }
                }
            }
        }
        compose.waitForIdle()
    }

    private fun snapshot(name: String) {
        val bounds = compose.onNodeWithTag("vinyl-preview").getUnclippedBoundsInRoot()
        compose.runOnIdle {
            val bitmap = Bitmap.createBitmap((bounds.right - bounds.left).value.roundToInt(),
                (bounds.bottom - bounds.top).value.roundToInt(), Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.translate(-bounds.left.value, -bounds.top.value)
            compose.activity.findViewById<View>(android.R.id.content).draw(canvas)
            val dir = File("build/reports/vinyl-preview").apply { mkdirs() }
            File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }

    @Test fun transportMenusSeekAndFooterUsePlaybackCallbacks() {
        show()
        snapshot("phone-411")
        compose.onNodeWithContentDescription("Play").performClick()
        compose.onNodeWithContentDescription("Pause").assertExists().performClick()
        compose.onNodeWithContentDescription("Previous").performClick()
        compose.onNodeWithContentDescription("Next").performClick()
        compose.onNodeWithContentDescription("Settings").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Playback options").performClick()
        compose.onNodeWithText("Shuffle off").performClick()
        assertTrue(shuffled.value)
        compose.onNodeWithContentDescription("Playback options").performClick()
        compose.onNodeWithText("Repeat off").performClick()
        assertEquals(1, repeated.value)
        compose.onNodeWithContentDescription("Seek").performSemanticsAction(SemanticsActions.SetProgress) { it(0.5f) }
        assertEquals(95_000L, sought)
        compose.onNodeWithContentDescription("Lyrics").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Share").performClick()
        compose.onNodeWithContentDescription("Favorite").performClick()
        assertEquals(listOf(2, 1, 1, 1, 1, 1, 1), listOf(plays, previous, next, settings, lyricsOpens, shares, favorites))
    }

    @Test fun compactPhoneWithLargeTextCanScrollToEveryControl() {
        show(width = 320, height = 640, fontScale = 2f)
        compose.onNodeWithContentDescription("Play").performScrollTo().assertIsDisplayed().performClick()
        compose.onNodeWithContentDescription("Favorite").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("quality").performScrollTo().assertIsDisplayed()
        snapshot("compact-320-font-200")
    }

    @Test fun emptyPlayerDisablesTransportAndSeekButKeepsPlayAndSettings() {
        show(hasSong = false)
        compose.onNodeWithContentDescription("Previous").assertIsNotEnabled()
        compose.onNodeWithContentDescription("Next").assertIsNotEnabled()
        compose.onNodeWithContentDescription("Seek").assertIsNotEnabled()
        compose.onNodeWithContentDescription("Play").assertIsEnabled()
        compose.onNodeWithContentDescription("Settings").assertIsEnabled()
        compose.onNodeWithContentDescription("Turntable: Repeat").assertIsNotEnabled()
        compose.onNodeWithText("Choose a song").performScrollTo().assertIsDisplayed()
    }

    @Test fun changingFromShortToLongTitlesKeepsTransportAndQualityInPlace() {
        title.value = "Home"
        show()
        val beforePlay = compose.onNodeWithContentDescription("Play").getUnclippedBoundsInRoot()
        val beforeQuality = compose.onNodeWithTag("quality").getUnclippedBoundsInRoot()
        val beforeRecord = compose.onNodeWithTag("vinyl-record").getUnclippedBoundsInRoot()
        val beforeProfile = compose.onNodeWithTag("artist-profile").getUnclippedBoundsInRoot()
        compose.runOnIdle { title.value = "A very long song title that wraps onto several different lines" }
        val afterPlay = compose.onNodeWithContentDescription("Play").getUnclippedBoundsInRoot()
        val afterQuality = compose.onNodeWithTag("quality").getUnclippedBoundsInRoot()
        assertEquals(beforePlay.top.value, afterPlay.top.value, 0.5f)
        assertEquals(beforeQuality.top.value, afterQuality.top.value, 0.5f)
        assertEquals(beforeRecord, compose.onNodeWithTag("vinyl-record").getUnclippedBoundsInRoot())
        assertEquals(beforeProfile, compose.onNodeWithTag("artist-profile").getUnclippedBoundsInRoot())
        compose.runOnIdle { title.value = "Home" }
        assertEquals(beforeRecord, compose.onNodeWithTag("vinyl-record").getUnclippedBoundsInRoot())
        assertEquals(beforeProfile, compose.onNodeWithTag("artist-profile").getUnclippedBoundsInRoot())
    }

    @Test fun qualityAvailabilityAndLabelDoNotMoveTheNextSection() {
        show()
        val before = compose.onNodeWithTag("player-bottom").getUnclippedBoundsInRoot()
        compose.runOnIdle { qualityVisible.value = false }
        val missing = compose.onNodeWithTag("player-bottom").getUnclippedBoundsInRoot()
        assertEquals(before.top.value, missing.top.value, 0.5f)
        compose.runOnIdle { qualityVisible.value = true; qualityLabel.value = "Lossless" }
        val restored = compose.onNodeWithTag("player-bottom").getUnclippedBoundsInRoot()
        assertEquals(before.top.value, restored.top.value, 0.5f)
    }

    @Test fun compactRecordAndProfileStayFixedAcrossRepeatedTitleChanges() {
        title.value = "Home"
        show(width = 360, height = 640, safeBottom = 24, titleSize = 44f)
        val record = compose.onNodeWithTag("vinyl-record").getUnclippedBoundsInRoot()
        val profile = compose.onNodeWithTag("artist-profile").getUnclippedBoundsInRoot()
        for (text in listOf("A very long song title that wraps onto several lines", "Go", "The Emptiness Machine", "Home")) {
            compose.runOnIdle { title.value = text }
            assertEquals(record, compose.onNodeWithTag("vinyl-record").getUnclippedBoundsInRoot())
            assertEquals(profile, compose.onNodeWithTag("artist-profile").getUnclippedBoundsInRoot())
            assertEveryControlFits()
        }
    }

    @Test fun smallQualityFaceRetainsAFullTouchTargetAndMetadataAction() {
        show()
        compose.onNodeWithTag("quality").performScrollTo().performClick()
        assertEquals(1, qualityClicks)
        val face = compose.onNodeWithTag("vinyl-quality-face", useUnmergedTree = true).getUnclippedBoundsInRoot()
        val target = compose.onNodeWithTag("quality").getUnclippedBoundsInRoot()
        assertEquals(24f, (face.bottom - face.top).value, 0.5f)
        assertTrue((target.bottom - target.top).value >= 44f)
        snapshot("phone-compact-badge")
    }

    @Test fun shortPhoneShowsEveryControlAndQualityWithoutScrolling() {
        show(width = 360, height = 640)
        assertEveryControlFits()
        snapshot("short-phone-no-scroll")
    }

    private fun assertEveryControlFits() {
        val viewport = compose.onNodeWithTag("vinyl-preview").getUnclippedBoundsInRoot()
        for (description in listOf("Settings", "Seek", "Previous", "Play", "Next", "Lyrics", "Share", "Favorite",
                "Turntable: Play", "Turntable: Repeat")) {
            val bounds = compose.onNodeWithContentDescription(description).getUnclippedBoundsInRoot()
            assertTrue("$description must fit above the screen bottom", bounds.bottom <= viewport.bottom)
            assertTrue("$description must fit below the screen top", bounds.top >= viewport.top)
        }
        val badge = compose.onNodeWithTag("quality").getUnclippedBoundsInRoot()
        assertTrue("Full Hi-Res touch target must be visible without scrolling", badge.bottom <= viewport.bottom)
    }

    @Test fun machineSwitchesOperatePlaybackAndExposeRepeatState() {
        show()
        compose.onNodeWithContentDescription("Turntable: Play").performClick()
        compose.onNodeWithContentDescription("Pause").assertExists()
        compose.onNodeWithContentDescription("Turntable: Pause").performClick()
        assertEquals(2, plays)
        val repeat = compose.onNodeWithContentDescription("Turntable: Repeat")
        repeat.assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Repeat off"))
        repeat.performClick()
        repeat.assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Repeat one"))
        repeat.performClick()
        repeat.assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Repeat all"))
        repeat.performClick()
        repeat.assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Repeat off"))
        snapshot("silver-machine-controls")
    }

    @Test fun machineControlsRetainTouchAreasAndKeyboardActivationOnShortScreens() {
        show(width = 360, height = 640, safeBottom = 24, titleSize = 44f)
        val deck = compose.onNodeWithTag("vinyl-deck").getUnclippedBoundsInRoot()
        val progress = compose.onNodeWithTag("vinyl-progress").getUnclippedBoundsInRoot()
        val record = compose.onNodeWithTag("vinyl-record").getUnclippedBoundsInRoot()
        assertEquals(progress.left.value, deck.left.value, 0.5f)
        assertEquals(progress.right.value, deck.right.value, 0.5f)
        assertEquals((record.right - record.left).value, (record.bottom - record.top).value, 0.5f)
        assertTrue("A short viewport uses a wider chassis while keeping the record circular",
            (deck.right - deck.left) > (deck.bottom - deck.top))
        for (description in listOf("Turntable: Play", "Turntable: Repeat")) {
            val bounds = compose.onNodeWithContentDescription(description).getUnclippedBoundsInRoot()
            assertTrue((bounds.right - bounds.left).value >= 44f)
            assertTrue((bounds.bottom - bounds.top).value >= 44f)
            assertTrue(bounds.left >= deck.left && bounds.right <= deck.right)
            assertTrue(bounds.top >= deck.top && bounds.bottom <= deck.bottom)
        }
        compose.runOnIdle { inputModeManager.requestInputMode(InputMode.Keyboard) }
        compose.onNodeWithContentDescription("Turntable: Repeat")
            .performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        compose.onNodeWithContentDescription("Turntable: Repeat").assertIsFocused()
        compose.onNodeWithContentDescription("Turntable: Repeat").performKeyInput { pressKey(Key.Enter) }
        assertEquals(1, repeated.value)
    }

    @Test fun enlargedMachineAndLeftAlignedMetadataShareTheProgressBarEdges() {
        show()
        val progress = compose.onNodeWithTag("vinyl-progress").getUnclippedBoundsInRoot()
        val deck = compose.onNodeWithTag("vinyl-deck").getUnclippedBoundsInRoot()
        assertEquals(progress.left.value, deck.left.value, 0.5f)
        assertEquals(progress.right.value, deck.right.value, 0.5f)
        assertTrue("The normal phone chassis grows beyond the previous 349 dp", (deck.right - deck.left).value > 360f)
        for (text in listOf(title.value, "LINKIN PARK")) {
            val node = compose.onNodeWithText(text)
            val bounds = node.getUnclippedBoundsInRoot()
            val layout = mutableListOf<TextLayoutResult>()
            node.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layout) }
            assertEquals(progress.left.value, bounds.left.value, 0.5f)
            assertEquals("Visible English text starts at the shared left edge", 0f, layout.single().getLineLeft(0), 0.5f)
        }
        snapshot("aligned-machine-metadata")
    }

    @Test fun rectangularQualityFaceSitsUnderTheArtistAtTheSameLeftEdge() {
        show()
        val artist = compose.onNodeWithText("LINKIN PARK").getUnclippedBoundsInRoot()
        val face = compose.onNodeWithTag("vinyl-quality-face", useUnmergedTree = true).getUnclippedBoundsInRoot()
        val progress = compose.onNodeWithTag("vinyl-progress").getUnclippedBoundsInRoot()
        assertEquals(artist.left.value, face.left.value, 0.5f)
        assertTrue(face.top >= artist.bottom && face.bottom <= progress.top)
        assertTrue("The badge stays close below its artist", (face.top - artist.bottom).value <= 16f)
        compose.runOnIdle {
            val view = compose.activity.findViewById<View>(android.R.id.content)
            val image = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            view.draw(Canvas(image))
            assertEquals("A square corner is filled, rather than clipped into a capsule", 0xFFDAD4BF.toInt(),
                image.getPixel(face.left.value.roundToInt() + 1, face.top.value.roundToInt() + 1))
            assertEquals("The visible progress line starts at the same edge as the chassis", 0xFF171717.toInt(),
                image.getPixel(progress.left.value.roundToInt() + 1, progress.top.value.roundToInt() + 24))
            image.recycle()
        }
    }

    @Test fun longTitleAndLargerTitlePreferenceFitAboveTheNavigationArea() {
        title.value = "A long title that must leave the lower playback controls visible"
        show(width = 360, height = 640, safeBottom = 24, titleSize = 44f)
        assertEveryControlFits()
        snapshot("short-phone-safe-area-large-title")
    }

    @Test fun oneLineTitleDoesNotLeaveAnEmptySecondLineAboveTheArtist() {
        title.value = "Home"
        show(width = 360, height = 740)
        val titleNode = compose.onNodeWithText("Home")
        val results = mutableListOf<TextLayoutResult>()
        titleNode.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(results) }
        val titleBounds = titleNode.getUnclippedBoundsInRoot()
        val artistBounds = compose.onNodeWithText("LINKIN PARK").getUnclippedBoundsInRoot()
        val visibleTitleBottom = titleBounds.top.value + results.single().getLineBottom(0)
        assertTrue("A short title must sit close to its artist", artistBounds.top.value - visibleTitleBottom <= 12f)
        snapshot("one-line-title-no-scroll")
    }

    @Test
    @Config(sdk = [34], qualifiers = "w800dp-h914dp")
    fun seekSupportsKeyboardAndWidePlayerRetainsCenteredRecord() {
        show(width = 800, height = 914)
        val record = compose.onNodeWithTag("vinyl-record").getUnclippedBoundsInRoot()
        assertTrue("The record fits its remaining height and keeps the 380 dp cap",
            (record.right - record.left).value in 180f..380f)
        assertEquals(400f, ((record.left + record.right) / 2f).value, 1f)
        compose.runOnIdle { title.value = "A very long song title that wraps onto several different lines" }
        assertEquals(record, compose.onNodeWithTag("vinyl-record").getUnclippedBoundsInRoot())
        compose.onNodeWithContentDescription("Seek").performScrollTo()
            .performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        compose.onNodeWithContentDescription("Seek").performKeyInput { pressKey(Key.DirectionRight) }
        assertTrue(sought > 75_000L)
        snapshot("wide-800")
    }
}
