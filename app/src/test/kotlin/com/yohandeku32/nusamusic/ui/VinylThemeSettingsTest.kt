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
    private val preferences get() = compose.activity.getSharedPreferences("vinyl-theme-test", Context.MODE_PRIVATE)

    @Test fun missingUnknownAndWrongTypeKeepClassicWithoutChangingTheSavedPlayerMode() {
        preferences.edit().clear().putString("player_presentation_mode", "vinyl").commit()
        assertEquals(VinylMachineTheme.CLASSIC, VinylThemePreference.read(preferences))
        assertFalse(preferences.contains("vinyl_machine_theme"))
        preferences.edit().putString("vinyl_machine_theme", "future-theme").commit()
        assertEquals(VinylMachineTheme.CLASSIC, VinylThemePreference.read(preferences))
        preferences.edit().putInt("vinyl_machine_theme", 42).commit()
        assertEquals(VinylMachineTheme.CLASSIC, VinylThemePreference.read(preferences))
        VinylThemePreference.write(preferences, VinylMachineTheme.STUDIO_BLACK)
        assertEquals(VinylMachineTheme.STUDIO_BLACK, VinylThemePreference.read(preferences))
        assertEquals("vinyl", preferences.getString("player_presentation_mode", null))
        VinylThemePreference.write(preferences, VinylMachineTheme.CLASSIC)
        assertEquals(VinylMachineTheme.CLASSIC, VinylThemePreference.read(preferences))
    }

    private fun show(width: Int = 411, scale: Float = 1f) {
        preferences.edit().clear().commit()
        compose.setContent {
            var theme by remember { mutableStateOf(VinylThemePreference.read(preferences)) }
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
                            onRemoveMusicFolder = {}, onScanMusic = {}, onDismiss = {},
                            vinylMachineTheme = theme, onVinylMachineThemeChange = {
                                theme = it; VinylThemePreference.write(preferences, it)
                            })
                    }
                }
            }
        }
    }

    @Test fun studioBlackIsSecondSelectionPersistsAndClassicRemainsAvailable() {
        show()
        compose.onNodeWithText("Classic").assertIsSelected()
        compose.onNodeWithText("Studio Black").performClick().assertIsSelected()
        assertEquals(VinylMachineTheme.STUDIO_BLACK, VinylThemePreference.read(preferences))
        compose.runOnIdle {
            val view = compose.activity.findViewById<View>(android.R.id.content)
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            view.draw(Canvas(bitmap))
            val file = File("build/reports/vinyl-preview/settings-studio-black.png").apply { parentFile!!.mkdirs() }
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
        }
        compose.onNodeWithText("CD").performClick()
        compose.onNodeWithText("Studio Black").assertDoesNotExist()
        compose.onNodeWithText("Vinyl").performClick()
        compose.onNodeWithText("Studio Black").assertIsSelected()
        compose.onNodeWithText("Classic").performClick().assertIsSelected()
        assertEquals(VinylMachineTheme.CLASSIC, VinylThemePreference.read(preferences))
    }

    @Test fun compactLargeTextKeepsBothThemeChoicesOperable() {
        show(320, 2f)
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Studio Black"))
        compose.onNodeWithText("Studio Black").performClick().assertIsSelected()
        compose.onNodeWithText("Classic").performClick().assertIsSelected()
    }
}
