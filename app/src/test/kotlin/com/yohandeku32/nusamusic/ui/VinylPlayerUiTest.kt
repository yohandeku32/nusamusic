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
import androidx.compose.ui.MotionDurationScale
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
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import com.yohandeku32.nusamusic.VinylRecord
import com.yohandeku32.nusamusic.data.AudioWaveform
import com.yohandeku32.nusamusic.data.PcmWaveformAccumulator
import com.yohandeku32.nusamusic.data.WaveformPcm
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
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
    private val animationScale = object : MotionDurationScale {
        override var scaleFactor by mutableFloatStateOf(1f)
    }
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>(effectContext = animationScale)
    private var plays = 0
    private var previous = 0
    private var next = 0
    private var settings = 0
    private var songListOpens = 0
    private var lyricsOpens = 0
    private var shares = 0
    private var favorites = 0
    private var qualityClicks = 0
    private var sought = 0L
    private val playing = mutableStateOf(false)
    private val visible = mutableStateOf(true)
    private val position = mutableStateOf(75_000L)
    private val shuffled = mutableStateOf(false)
    private val repeated = mutableStateOf(0)
    private val title = mutableStateOf("The Emptiness Machine")
    private val songLoaded = mutableStateOf(true)
    private val qualityVisible = mutableStateOf(true)
    private val qualityLabel = mutableStateOf("Hi-Res")
    private val artworkColor = mutableStateOf<Color?>(null)
    private val artworkPalette = mutableStateOf<VinylArtworkPalette?>(null)
    private lateinit var inputModeManager: InputModeManager

    private fun show(width: Int = 411, height: Int = 914, fontScale: Float = 1f, hasSong: Boolean = true,
        safeBottom: Int = 0, titleSize: Float = 34f, waveform: AudioWaveform? = null,
        waveformLoading: Boolean = false) {
        position.value = if (waveform == null) 75_000L else waveform.durationMs * 2 / 5
        songLoaded.value = hasSong
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, fontScale)) {
                inputModeManager = LocalInputModeManager.current
                MaterialTheme(colorScheme = darkColorScheme()) {
                    LazyColumn(Modifier.requiredSize(width.dp, (height - safeBottom).dp).testTag("vinyl-preview")) {
                        item {
                            TurntablePlayer(
                                title = title.value, artist = if (waveform == null) "LINKIN PARK" else "Audio test fixture",
                                titleFontFamily = FontFamily.Serif, titleFontSize = titleSize,
                                viewportHeight = (height - safeBottom).dp, isPlaying = playing.value,
                                isVisible = visible.value, hasSong = songLoaded.value,
                                positionMsState = position,
                                durationMs = waveform?.durationMs ?: 190_000L,
                                waveform = waveform, isWaveformLoading = waveformLoading,
                                artworkColor = artworkColor.value,
                                artworkPalette = artworkPalette.value,
                                shuffleEnabled = shuffled.value, repeatMode = repeated.value,
                                labels = TurntableLabels("Play", "Pause", "Previous", "Next",
                                    "Shuffle", "Shuffle on", "Shuffle off", "Repeat", "Repeat off",
                                    "Repeat all", "Repeat one", "Seek", "Settings", "Playback options"),
                                onPlayPause = { plays++; playing.value = !playing.value },
                                onPrevious = { previous++ }, onNext = { next++ }, onSeek = { sought = it },
                                onShuffle = { shuffled.value = !shuffled.value },
                                onRepeat = { repeated.value = (repeated.value + 1) % 3 },
                                onSettings = { settings++ },
                                onOpenSongs = { songListOpens++ },
                                avatar = { Box(it.testTag("artist-profile").background(Color.Black, CircleShape)) },
                                record = { VinylRecord(song = null, isPlaying = false, modifier = it.testTag("vinyl-record")) },
                                quality = {
                                    if (qualityVisible.value && songLoaded.value) VinylQualityBadge(
                                        qualityLabel.value, true, { qualityClicks++ }, Modifier.testTag("quality")
                                    )
                                },
                                footer = {
                                    Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
                                        IconButton(onClick = { lyricsOpens++ }) { Icon(Icons.Default.KeyboardArrowDown, "Lyrics") }
                                        Spacer(Modifier.weight(1f))
                                        IconButton(onClick = { shares++ }) { Icon(Icons.Default.Share, "Share") }
                                        IconButton(onClick = { favorites++ }) { Icon(Icons.Default.FavoriteBorder, "Favorite") }
                                    }
                                },
                                emptyState = {
                                    if (!songLoaded.value) Text("Choose a song")
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

    @Test fun coverGradientColorsOnlyTheLowerPanelWithoutMovingControls() {
        artworkColor.value = Color(0xFF8852AC)
        show()
        val before = compose.onNodeWithContentDescription("Play").getUnclippedBoundsInRoot()
        fun backgroundPixel(y: Int): Color {
            var pixel = Color.Transparent
            compose.runOnIdle {
                val bitmap = Bitmap.createBitmap(411, 914, Bitmap.Config.ARGB_8888)
                compose.activity.findViewById<View>(android.R.id.content).draw(Canvas(bitmap))
                pixel = Color(bitmap.getPixel(4, y))
                bitmap.recycle()
            }
            return pixel
        }
        assertEquals(VinylPaper, backgroundPixel(4))
        val purple = backgroundPixel(900)
        assertTrue(purple.blue > purple.green)
        snapshot("artwork-gradient-purple")
        compose.runOnIdle { artworkColor.value = Color(0xFF328145) }
        compose.mainClock.advanceTimeBy(600)
        compose.waitForIdle()
        val green = backgroundPixel(900)
        assertTrue(green.green > green.red)
        assertEquals(before, compose.onNodeWithContentDescription("Play").getUnclippedBoundsInRoot())
        snapshot("artwork-gradient-green")
        compose.runOnIdle { artworkColor.value = null }
        compose.mainClock.advanceTimeBy(600)
        compose.waitForIdle()
        assertEquals(VinylPaper, backgroundPixel(900))
    }

    @Test fun twoToneArtworkBlendsSoftlyAtBottomWithoutShiftingControls() {
        show()
        val originalPlay = compose.onNodeWithContentDescription("Play").getUnclippedBoundsInRoot()
        fun pixelAt(x: Int, y: Int): Color {
            var result = Color.Transparent
            compose.runOnIdle {
                val bitmap = Bitmap.createBitmap(411, 914, Bitmap.Config.ARGB_8888)
                compose.activity.findViewById<View>(android.R.id.content).draw(Canvas(bitmap))
                result = Color(bitmap.getPixel(x, y))
                bitmap.recycle()
            }
            return result
        }

        compose.runOnIdle {
            artworkPalette.value = VinylArtworkPalette(Color(0xFFDB562E), Color(0xFF2D60C5))
        }
        compose.mainClock.advanceTimeBy(750)
        compose.waitForIdle()

        assertEquals("The upper Vinyl surface remains neutral", VinylPaper, pixelAt(4, 4))
        val left = pixelAt(4, 898)
        val right = pixelAt(405, 898)
        assertTrue("The lower-left side preserves the dominant warm hue", left.red > left.blue)
        assertTrue("The lower-right side carries the real blue companion",
            right.blue > left.blue + 0.025f)
        assertEquals("Album palette animation must not alter playback positions",
            originalPlay, compose.onNodeWithContentDescription("Play").getUnclippedBoundsInRoot())

        compose.runOnIdle { artworkPalette.value = null }
        compose.mainClock.advanceTimeBy(750)
        compose.waitForIdle()
        assertEquals("The empty palette restores the original neutral paper", VinylPaper, pixelAt(4, 898))
    }

    @Test fun classicMechanicalControlsRemainOperable() {
        show()
        compose.onNodeWithContentDescription("Turntable: Play").performClick()
        assertEquals(1, plays)
        compose.onNodeWithContentDescription("Turntable: Repeat").performClick()
        assertEquals(1, repeated.value)
        snapshot("classic-mechanical-controls")
    }

    @Test fun classicFitsCompactPhoneWithLargeText() {
        show(width = 320, height = 640, fontScale = 2f)
        compose.onNodeWithContentDescription("Turntable: Play").performScrollTo().performClick()
        assertEquals(1, plays)
        compose.onNodeWithContentDescription("Turntable: Repeat").performScrollTo().performClick()
        assertEquals(1, repeated.value)
        compose.onNodeWithContentDescription("Favorite").performScrollTo().assertIsDisplayed()
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

    private fun pcmFixture(): AudioWaveform {
        val pcm = ByteBuffer.allocate(800).order(ByteOrder.LITTLE_ENDIAN)
        repeat(400) { frame ->
            val amplitude = when (frame / 100) { 0 -> 0.25f; 1 -> 1f; 2 -> 0f; else -> 0.75f }
            pcm.putShort((amplitude * 32767 * if (frame % 2 == 0) 1 else -1).toInt().toShort())
        }
        pcm.flip()
        val accumulator = PcmWaveformAccumulator(4_000_000)
        accumulator.add(pcm, 0, 100, 1, WaveformPcm.SIGNED_16)
        return requireNotNull(accumulator.finish())
    }

    private fun progressPixels(knownBounds: DpRect? = null): IntArray {
        val bounds = knownBounds ?: compose.onNodeWithTag("vinyl-progress").getUnclippedBoundsInRoot()
        var result = intArrayOf()
        compose.runOnUiThread {
            val view = compose.activity.findViewById<View>(android.R.id.content)
            val image = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            view.draw(Canvas(image))
            val width = (bounds.right - bounds.left).value.roundToInt()
            result = IntArray(width * 48)
            image.getPixels(result, 0, width, bounds.left.value.roundToInt(), bounds.top.value.roundToInt(), width, 48)
            image.recycle()
        }
        return result
    }

    private fun excursion(pixels: IntArray): Int {
        val width = pixels.size / 48
        return pixels.indices.filter { (pixels[it] shr 16 and 255) < 210 }
            .maxOfOrNull { kotlin.math.abs(it / width - 24) } ?: 0
    }

    @Test fun layeredCurvesUseCurrentSourceAmplitudeAndRetainSeeking() {
        title.value = "Waveform fixture"
        show(waveform = pcmFixture())
        val seek = compose.onNodeWithContentDescription("Seek")
        seek.assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Audio waveform"))
        val loud = progressPixels()
        assertTrue("Thin curves leave open space rather than a filled envelope",
            loud.count { (it shr 16 and 255) < 210 } < (loud.size / 48) * 10)
        assertTrue("The layered curves extend above and below the baseline", excursion(loud) >= 10)
        snapshot("animated-wave-player-fixture")
        compose.runOnIdle { position.value = 400 }
        val quiet = progressPixels()
        assertTrue("Quiet source audio has a smaller wave", excursion(quiet) < excursion(loud))
        compose.runOnIdle { position.value = 2400 }
        assertTrue("Silent source audio stays flat", excursion(progressPixels()) <= 2)
        seek.performSemanticsAction(SemanticsActions.SetProgress) { it(0.75f) }
        assertEquals(3_000L, sought)
        seek.performTouchInput { click(androidx.compose.ui.geometry.Offset(width * 0.25f, height / 2f)) }
        assertTrue("Touch seeking uses the waveform's time axis", sought in 800L..1200L)
        assertEveryControlFits()
    }

    @Test fun waveMovesDuringPlaybackAndFreezesWhenPausedOrHidden() {
        show(waveform = pcmFixture())
        compose.mainClock.autoAdvance = false
        compose.runOnIdle { playing.value = true }
        compose.mainClock.advanceTimeBy(32)
        val first = progressPixels()
        compose.mainClock.advanceTimeBy(640)
        assertFalse("Playing changes curve geometry", first.contentEquals(progressPixels()))
        snapshot("animated-wave-playing")
        compose.runOnIdle { playing.value = false }
        compose.mainClock.advanceTimeBy(32)
        val paused = progressPixels()
        compose.mainClock.advanceTimeBy(640)
        assertArrayEquals("Pause freezes all three curves", paused, progressPixels())
        compose.runOnIdle { playing.value = true; visible.value = false }
        compose.mainClock.advanceTimeBy(32)
        val hidden = progressPixels()
        compose.mainClock.advanceTimeBy(640)
        assertArrayEquals("Hidden player does not animate", hidden, progressPixels())
        compose.runOnIdle { visible.value = true }
        compose.mainClock.advanceTimeBy(32)
        val backgroundBounds = compose.onNodeWithTag("vinyl-progress").getUnclippedBoundsInRoot()
        compose.activityRule.scenario.moveToState(androidx.lifecycle.Lifecycle.State.STARTED)
        val background = progressPixels(backgroundBounds)
        compose.mainClock.advanceTimeBy(640)
        assertArrayEquals("Background activity does not animate", background, progressPixels(backgroundBounds))
        compose.activityRule.scenario.moveToState(androidx.lifecycle.Lifecycle.State.RESUMED)
    }

    @Test fun waveAppearsBeforeAudioAnalysisAndRespectsReducedMotion() {
        show(waveformLoading = true)
        assertTrue("First render has curves without waiting for decoded PCM", excursion(progressPixels()) >= 8)
        compose.mainClock.autoAdvance = false
        compose.runOnIdle { playing.value = true }
        compose.mainClock.advanceTimeBy(32)
        val first = progressPixels()
        compose.mainClock.advanceTimeBy(640)
        assertFalse("Playback animates even while analysis is pending", first.contentEquals(progressPixels()))
        compose.runOnIdle { animationScale.scaleFactor = 0f }
        compose.mainClock.advanceTimeBy(32)
        val reduced = progressPixels()
        compose.mainClock.advanceTimeBy(640)
        assertArrayEquals("Disabled system animations freeze the curves", reduced, progressPixels())
    }

    @Test fun wavePacketTravelsWithProgressAndLeavesThePassedTrackStraight() {
        title.value = "Traveling wave fixture"
        show(waveform = AudioWaveform(4000, FloatArray(256) { 1f }))
        fun height(pixels: IntArray, left: Float, right: Float): Int {
            val width = pixels.size / 48
            return pixels.indices.filter { it % width in (width * left).toInt() until (width * right).toInt() &&
                (pixels[it] shr 16 and 255) < 210 }.maxOfOrNull { kotlin.math.abs(it / width - 24) } ?: 0
        }
        compose.runOnIdle { position.value = 400 }
        val early = progressPixels()
        assertTrue("At 10% the packet is near the left end", height(early, 0f, 0.25f) >= 8)
        assertTrue("Track ahead of the packet stays straight", height(early, 0.3f, 1f) <= 2)
        compose.runOnIdle { position.value = 3600 }
        val late = progressPixels()
        assertTrue("At 90% the packet reaches the right end", height(late, 0.75f, 1f) >= 8)
        assertTrue("The passed track returns to a straight line", height(late, 0f, 0.7f) <= 2)
        assertTrue("Played ink advances with the packet", late.count { (it shr 16 and 255) < 70 } >
            early.count { (it shr 16 and 255) < 70 } * 2)
        snapshot("traveling-wave-progress")
        compose.runOnIdle { position.value = 4000 }
        assertTrue("Completed playback leaves the whole track straight", excursion(progressPixels()) <= 2)
    }

    @Test fun waveformLoadingKeepsSeekAvailableAndUsesTheExistingLayout() {
        show(waveformLoading = true)
        compose.onNodeWithContentDescription("Seek")
            .assertIsEnabled()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Reading audio waveform"))
            .performSemanticsAction(SemanticsActions.SetProgress) { it(0.5f) }
        assertEquals(95_000L, sought)
        assertEveryControlFits()
    }

    @Test fun outlinedTransportFitsCompactWidthAndOpensTheSongList() {
        show(width = 320, height = 640)
        val controls = listOf("Repeat", "Previous", "Play", "Next", "Songs").map {
            compose.onNodeWithContentDescription(it).getUnclippedBoundsInRoot()
        }
        controls.forEach { assertTrue((it.right - it.left).value >= 44f && (it.bottom - it.top).value >= 44f) }
        controls.zipWithNext().forEach { (left, right) -> assertTrue(left.right <= right.left) }
        val repeat = compose.onNodeWithContentDescription("Repeat")
        repeat.performClick()
        repeat.assertIsSelected().assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Repeat one"))
        snapshot("outlined-transport-active-compact")
        repeat.performClick()
        repeat.assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Repeat all"))
        repeat.performClick()
        repeat.assertIsNotSelected().assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Repeat off"))
        compose.runOnIdle { inputModeManager.requestInputMode(InputMode.Keyboard) }
        repeat.performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        repeat.performKeyInput { pressKey(Key.Enter) }
        assertEquals(1, repeated.value)
        compose.onNodeWithContentDescription("Songs").performClick()
        assertEquals(1, songListOpens)
        assertEveryControlFits()
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
        compose.onNodeWithContentDescription("Songs").assertIsEnabled()
        compose.onNodeWithContentDescription("Repeat").assertIsNotEnabled()
        compose.onNodeWithContentDescription("Play").assertIsEnabled()
        compose.onNodeWithContentDescription("Settings").assertIsEnabled()
        compose.onNodeWithContentDescription("Turntable: Repeat").assertIsNotEnabled()
        compose.onNodeWithText("Choose a song").performScrollTo().assertIsDisplayed()
    }

    @Test fun restoringLastSongDoesNotResizeVinylOrShiftPlaybackOnCompactPhone() {
        show(width = 360, height = 740, hasSong = false)
        val initialDeck = compose.onNodeWithTag("vinyl-deck").getUnclippedBoundsInRoot()
        val initialRecord = compose.onNodeWithTag("vinyl-record").getUnclippedBoundsInRoot()
        val initialTransport = compose.onNodeWithTag("vinyl-transport").getUnclippedBoundsInRoot()
        val initialProgress = compose.onNodeWithTag("vinyl-progress").getUnclippedBoundsInRoot()

        // Simulate async saved-song / library restore after the very first frame.
        compose.runOnIdle { songLoaded.value = true }
        compose.waitForIdle()

        assertEquals("Machine bounds must not jump when the first song loads",
            initialDeck, compose.onNodeWithTag("vinyl-deck").getUnclippedBoundsInRoot())
        assertEquals("Record must not scale after startup",
            initialRecord, compose.onNodeWithTag("vinyl-record").getUnclippedBoundsInRoot())
        assertEquals("Playback buttons must not shift after song restore",
            initialTransport, compose.onNodeWithTag("vinyl-transport").getUnclippedBoundsInRoot())
        assertEquals("Waveform area stays in the same position",
            initialProgress, compose.onNodeWithTag("vinyl-progress").getUnclippedBoundsInRoot())
        compose.runOnIdle { songLoaded.value = false }
        compose.waitForIdle()
        assertEquals("Returning to the empty state keeps the original chassis size",
            initialDeck, compose.onNodeWithTag("vinyl-deck").getUnclippedBoundsInRoot())
    }

    @Test fun restoringLastSongDoesNotResizeVinylOnRegularPhone() {
        show(width = 411, height = 914, hasSong = false)
        val initialDeck = compose.onNodeWithTag("vinyl-deck").getUnclippedBoundsInRoot()
        val initialRecord = compose.onNodeWithTag("vinyl-record").getUnclippedBoundsInRoot()
        compose.runOnIdle { songLoaded.value = true }
        compose.waitForIdle()
        assertEquals(initialDeck, compose.onNodeWithTag("vinyl-deck").getUnclippedBoundsInRoot())
        assertEquals(initialRecord, compose.onNodeWithTag("vinyl-record").getUnclippedBoundsInRoot())
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
        assertEquals(18f, (face.bottom - face.top).value, 0.5f)
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
        for (description in listOf("Settings", "Seek", "Repeat", "Previous", "Play", "Next", "Songs", "Lyrics", "Share", "Favorite",
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
        snapshot("vinyl-red-led-playing")
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
        assertEquals("Vinyl stays horizontally centered", ((progress.left + progress.right) / 2f).value,
            ((deck.left + deck.right) / 2f).value, 0.5f)
        assertEquals("Vinyl chassis scales down by only 4%", (progress.right - progress.left).value * 0.96f,
            (deck.right - deck.left).value, 0.5f)
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

    @Test fun machineAndLeftAlignedMetadataShareTheOriginalProgressMargins() {
        show()
        val progress = compose.onNodeWithTag("vinyl-progress").getUnclippedBoundsInRoot()
        val deck = compose.onNodeWithTag("vinyl-deck").getUnclippedBoundsInRoot()
        val transport = compose.onNodeWithTag("vinyl-transport").getUnclippedBoundsInRoot()
        assertEquals("Vinyl remains centered over the progress", ((progress.left + progress.right) / 2f).value,
            ((deck.left + deck.right) / 2f).value, 0.5f)
        assertEquals("Vinyl is only 4% smaller", (progress.right - progress.left).value * 0.96f,
            (deck.right - deck.left).value, 0.5f)
        assertEquals("Playback controls must share the same left gutter", progress.left.value, transport.left.value, 0.5f)
        assertEquals("Playback controls must share the same right gutter", progress.right.value, transport.right.value, 0.5f)
        assertEquals("Keep the 36 dp progress and metadata gutter", 36f, progress.left.value, 0.5f)
        assertEquals(375f, progress.right.value, 0.5f)
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

    @Test fun translucentTextQualityLabelSitsUnderTheArtist() {
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
            val background = image.getPixel(face.left.value.roundToInt() + 2, face.top.value.roundToInt() + 9)
            assertTrue("The label has a low-opacity neutral background",
                android.graphics.Color.red(background) in 205..220 &&
                    android.graphics.Color.green(background) in 205..220 &&
                    android.graphics.Color.blue(background) in 200..215)
            assertTrue("The wave starts at the same edge as the chassis",
                (4..44).any { y ->
                    (image.getPixel(progress.left.value.roundToInt() + 1, progress.top.value.roundToInt() + y) shr 16 and 255) < 210
                })
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
        val progress = compose.onNodeWithTag("vinyl-progress").getUnclippedBoundsInRoot()
        val transport = compose.onNodeWithTag("vinyl-transport").getUnclippedBoundsInRoot()
        val deck = compose.onNodeWithTag("vinyl-deck").getUnclippedBoundsInRoot()
        assertEquals("Wide-screen Vinyl stays centered", ((progress.left + progress.right) / 2f).value,
            ((deck.left + deck.right) / 2f).value, 0.5f)
        assertEquals("Wide-screen Vinyl scales down by 4%", (progress.right - progress.left).value * 0.96f,
            (deck.right - deck.left).value, 0.5f)
        assertEquals("Wide-screen transport matches waveform's left gutter", progress.left.value, transport.left.value, 0.5f)
        assertEquals("Wide-screen transport matches waveform's right gutter", progress.right.value, transport.right.value, 0.5f)
        assertEquals("Both use 512 dp max width minus the 36 dp gutters", 440f, (transport.right - transport.left).value, 1f)
        compose.runOnIdle { title.value = "A very long song title that wraps onto several different lines" }
        assertEquals(record, compose.onNodeWithTag("vinyl-record").getUnclippedBoundsInRoot())
        compose.onNodeWithContentDescription("Seek").performScrollTo()
            .performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        compose.onNodeWithContentDescription("Seek").performKeyInput { pressKey(Key.DirectionRight) }
        assertTrue(sought > 75_000L)
        snapshot("wide-800")
    }
}
