package com.yohandeku32.nusamusic.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
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
@Config(sdk = [34], qualifiers = "w411dp-h891dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SaltLyricsSettingsUiTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val mini = mutableStateOf(false)
    private val online = mutableStateOf(true)
    private val loading = mutableStateOf(false)
    private var retries = 0

    private fun show(width: Int = 411, scale: Float = 1f, dark: Boolean = true) {
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, scale)) {
                MaterialTheme(colorScheme = if (dark) darkColorScheme() else lightColorScheme()) {
                    Surface(Modifier.requiredSize(width.dp, 820.dp), color = MaterialTheme.colorScheme.background,
                        contentColor = MaterialTheme.colorScheme.onBackground) {
                        PlayerSettingsContent(lyricsFontSize = 34f, onLyricsFontSizeChange = {},
                            onLyricsFontSizeChangeFinished = {}, hasLyricsSong = true,
                            lyricsLoading = loading.value, onRetryLyrics = { retries++ },
                            onlineLyricsEnabled = online.value, onOnlineLyricsChange = { online.value = it },
                            titleFontSize = 34f, onTitleFontSizeChange = {}, onTitleFontSizeChangeFinished = {},
                            titleFontName = "Default", titleFontFamily = FontFamily.SansSerif,
                            customTitleFontName = null, hasCustomTitleFont = false, onTitleFontSelected = {},
                            onChooseCustomTitleFont = {}, onResetCustomTitleFont = {},
                            playerPresentationMode = PlayerPresentationMode.IMMERSIVE_ARTWORK,
                            onPresentationSelected = {}, libraryLightMode = !dark, onLibraryLightModeChange = {},
                            animatedArtworkEnabled = false, roundedCarouselCorners = false,
                            onRoundedCarouselCornersChange = {}, onAnimatedArtworkChange = {},
                            artworkSourceOrder = "apple_first", onArtworkSourceOrderChange = {},
                            realisticControls = false, onRealisticControlsChange = {},
                            crossfadeEnabled = false, onCrossfadeChange = {}, crossfadeDurationSeconds = 3f,
                            onCrossfadeDurationChange = {}, onCrossfadeDurationChangeFinished = {},
                            musicFolders = emptyList(), isScanningMusic = false, onSelectMusicFolder = {},
                            onRemoveMusicFolder = {}, onScanMusic = {}, onDismiss = {},
                            miniLyricsEnabled = mini.value, onMiniLyricsChange = { mini.value = it })
                    }
                }
            }
        }
    }

    private fun preview(name: String) {
        compose.runOnIdle {
            val view = compose.activity.findViewById<View>(android.R.id.content)
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            view.draw(Canvas(bitmap))
            val file = File("build/reports/lyrics-companion-preview/$name.png").apply { parentFile!!.mkdirs() }
            file.outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
            bitmap.recycle()
        }
    }

    private fun reveal(text: String): SemanticsNodeInteraction {
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText(text))
        return compose.onNodeWithText(text).performScrollTo()
    }

    @Test fun saltSwitchesUseExistingStateAndReloadRespectsTheLoadingGuard() {
        show()
        reveal("Mini lyrics").performClick()
        assertTrue(mini.value)
        reveal("Online lyrics").performClick()
        assertFalse(online.value)
        reveal("Reload lyrics").performClick()
        assertEquals(1, retries)
        compose.runOnIdle { loading.value = true }
        compose.onNodeWithText("Reload lyrics").assertIsNotEnabled()
        preview("saltui-lyrics-settings-dark")
    }

    @Test fun lightNarrowLargeTextRetainsBothSwitchTargets() {
        show(width = 320, scale = 1.8f, dark = false)
        reveal("Mini lyrics").assertIsDisplayed().performClick()
        assertTrue(mini.value)
        preview("saltui-lyrics-settings-large-text")
        reveal("Online lyrics").assertIsDisplayed().performClick()
        assertFalse(online.value)
    }
}
