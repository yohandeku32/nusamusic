package com.yohandeku32.nusamusic.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.yohandeku32.nusamusic.data.EmbeddedLyricsSelector
import com.yohandeku32.nusamusic.data.SongCredits
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
class LyricsCreditsUiTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val xml = """<tt xmlns="http://www.w3.org/ns/ttml"><head><metadata>
        <iTunesMetadata xmlns="http://music.apple.com/lyric-ttml-internal"><songwriters>
        <songwriter>Writer 世界</songwriter><songwriter>Second Writer</songwriter>
        </songwriters></iTunesMetadata></metadata></head><body><p begin="1s" end="3s">Halo</p></body></tt>"""

    private fun show(credits: SongCredits, width: Int = 411, fontScale: Float = 1f) {
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, fontScale)) {
                MaterialTheme(colorScheme = darkColorScheme()) {
                    Box(Modifier.requiredSize(width.dp, 800.dp).background(Color(0xFF141820))) {
                        ImmersiveArtworkPlayer(title = "Credit preview", artist = "Test song",
                            composerNames = credits.composers.ifEmpty { credits.songwriters }.joinToString(", "),
                            composerRole = if (credits.composers.isEmpty() && credits.songwriters.isNotEmpty())
                                "SONGWRITERS" else "COMPOSER",
                            titleFontFamily = FontFamily.SansSerif, titleFontSize = 34f, hasSong = true,
                            isPlaying = false, isFavorite = false, shuffleEnabled = false, repeatMode = 0,
                            lyricsOpen = true, lyricsEnabled = true, onPlayPause = {}, onPrevious = {}, onNext = {},
                            onShuffle = {}, onRepeat = {}, onFavorite = {}, onLyrics = {}, onDiscovery = {},
                            onLibrary = {}, onShare = {}, onSettings = {},
                            artwork = { modifier, _, _ -> Box(modifier) }, progress = {}, quality = {},
                            lyrics = { modifier -> Column(modifier.padding(horizontal = 16.dp)) { LyricsCredits(credits) } })
                    }
                }
            }
        }
        compose.mainClock.advanceTimeBy(1000)
        compose.waitForIdle()
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

    @Test fun ttmlSongwritersReplaceUnknownWithoutClaimingTheyAreComposersOrLyricists() {
        val credits = EmbeddedLyricsSelector.select(listOf(xml), 10_000).stored.credits
        show(credits)
        compose.onNodeWithText("SONGWRITERS").assertIsDisplayed()
        compose.onNodeWithText("Written by").assertIsDisplayed()
        compose.onAllNodesWithText("Writer 世界, Second Writer").assertCountEquals(2)
        compose.onNodeWithText("Unknown composer").assertDoesNotExist()
        compose.onNodeWithText("Composed by").assertDoesNotExist()
        compose.onNodeWithText("Lyrics by").assertDoesNotExist()
        preview("ttml-songwriter-fallback")
    }

    @Test fun taggedComposerRemainsTheHeaderWhileSongwriterCreditsKeepTheirRole() {
        val credits = EmbeddedLyricsSelector.select(listOf(xml), 10_000, SongCredits(listOf("Tagged composer"))).stored.credits
        show(credits)
        compose.onNodeWithText("COMPOSER").assertIsDisplayed()
        compose.onAllNodesWithText("Tagged composer").assertCountEquals(2)
        compose.onNodeWithText("Composed by").assertIsDisplayed()
        compose.onNodeWithText("Written by").assertIsDisplayed()
        compose.onNodeWithText("SONGWRITERS").assertDoesNotExist()
    }

    @Test fun missingCreditsRetainTheExistingUnknownState() {
        show(SongCredits())
        compose.onNodeWithText("Unknown composer").assertIsDisplayed()
        compose.onNodeWithText("Written by").assertDoesNotExist()
    }

    @Test fun fallbackHeaderStaysInsideANarrowLargeTextViewport() {
        show(EmbeddedLyricsSelector.select(listOf(xml), 10_000).stored.credits, width = 320, fontScale = 1.8f)
        val header = compose.onAllNodesWithText("Writer 世界, Second Writer")[0]
        header.assertIsDisplayed()
        val bounds = header.getUnclippedBoundsInRoot()
        assertTrue(bounds.left >= 0.dp && bounds.right <= 320.dp)
        preview("ttml-songwriter-narrow-large-text")
    }
}
