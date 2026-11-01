package com.yohandeku32.nusamusic.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.yohandeku32.nusamusic.model.PlayerPresentationMode
import java.io.File
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
class VinylThemeSettingsTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private fun show(width: Int = 411, scale: Float = 1f) {
        compose.setContent {
            var mode by remember { mutableStateOf(PlayerPresentationMode.VINYL) }
            CompositionLocalProvider(LocalDensity provides Density(1f, scale)) {
                MaterialTheme(colorScheme = darkColorScheme()) {
                    Box(Modifier.requiredSize(width.dp, 914.dp)) {
                        PlayerSettingsContent(lyricsFontSize = 20f, onLyricsFontSizeChange = {},
                            onLyricsFontSizeChangeFinished = {}, hasLyricsSong = false,
                            lyricsLoading = false, onRetryLyrics = {}, onlineLyricsEnabled = true,
                            onOnlineLyricsChange = {}, titleFontSize = 34f, onTitleFontSizeChange = {},
                            onTitleFontSizeChangeFinished = {}, titleFontName = "Default",
                            titleFontFamily = FontFamily.Serif, customTitleFontName = null,
                            hasCustomTitleFont = false, onTitleFontSelected = {}, onChooseCustomTitleFont = {},
                            onResetCustomTitleFont = {}, playerPresentationMode = mode,
                            onPresentationSelected = { mode = it }, libraryLightMode = false,
                            onLibraryLightModeChange = {}, animatedArtworkEnabled = false,
                            roundedCarouselCorners = false, onRoundedCarouselCornersChange = {},
                            onAnimatedArtworkChange = {}, artworkSourceOrder = "apple_first",
                            onArtworkSourceOrderChange = {}, realisticControls = false,
                            onRealisticControlsChange = {}, crossfadeEnabled = false,
                            onCrossfadeChange = {}, crossfadeDurationSeconds = 3f,
                            onCrossfadeDurationChange = {}, onCrossfadeDurationChangeFinished = {},
                            musicFolders = emptyList(), isScanningMusic = false, onSelectMusicFolder = {},
                            onRemoveMusicFolder = {}, onScanMusic = {}, onDismiss = {})
                    }
                }
            }
        }
    }

    @Test fun vinylShowsNoMachineThemeSelector() {
        show()
        compose.onNodeWithText("Vinyl").assertExists()
        compose.onNodeWithText("Studio Black").assertDoesNotExist()
        compose.onNodeWithText("Vinyl machine theme").assertDoesNotExist()
        compose.onNodeWithText("CD").performClick()
        compose.onNodeWithText("Vinyl").performClick()
        compose.onNodeWithText("Studio Black").assertDoesNotExist()
    }

    @Test fun compactLargeTextKeepsVinylModeAvailable() {
        show(320, 2f)
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Vinyl"))
        compose.onNodeWithText("Vinyl").assertExists()
        compose.onNodeWithText("Studio Black").assertDoesNotExist()
    }
}
