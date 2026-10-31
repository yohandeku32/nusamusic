package com.yohandeku32.nusamusic.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.media.MediaMetadataRetriever
import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.yohandeku32.nusamusic.VinylRecord
import com.yohandeku32.nusamusic.data.ArtworkLoader
import com.yohandeku32.nusamusic.model.Song
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.shadows.ShadowMediaMetadataRetriever

@Implements(MediaMetadataRetriever::class)
class VinylPreviewMetadataRetriever : ShadowMediaMetadataRetriever() {
    @Implementation protected fun getEmbeddedPicture(): ByteArray? {
        entered?.countDown()
        gate?.let { check(it.await(20, TimeUnit.SECONDS)) }
        return artwork
    }
    companion object {
        @Volatile var artwork: ByteArray? = null
        @Volatile var gate: CountDownLatch? = null
        @Volatile var entered: CountDownLatch? = null
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h914dp", shadows = [VinylPreviewMetadataRetriever::class])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class VinylDeckArtworkTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun changingTracksKeepsTheOldCoverUntilTheNewCoverIsReady() = runBlocking {
        fun cover(color: Int): ByteArray {
            val bitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(color)
            return ByteArrayOutputStream().also { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                .toByteArray().also { bitmap.recycle() }
        }
        val first = Song(1, "First fixture", "Fixture", "Fixture", "file:///first-${System.nanoTime()}.mp3", 4000, 0)
        val second = first.copy(id = 2, uri = "file:///second-${System.nanoTime()}.mp3")
        VinylPreviewMetadataRetriever.artwork = cover(android.graphics.Color.RED)
        assertNotNull(ArtworkLoader.load(compose.activity, first.uri, 512))
        val song = mutableStateOf(first)
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                MaterialTheme { VinylRecord(song.value, false, Modifier.size(240.dp).testTag("record")) }
            }
        }
        fun labelPixel(): Int {
            val bounds = compose.onNodeWithTag("record").getUnclippedBoundsInRoot()
            var pixel = 0
            compose.runOnIdle {
                val view = compose.activity.findViewById<View>(android.R.id.content)
                val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
                view.draw(Canvas(bitmap))
                pixel = bitmap.getPixel(bounds.left.value.toInt() + 145, bounds.top.value.toInt() + 120)
                bitmap.recycle()
            }
            return pixel
        }
        val old = labelPixel()
        assertTrue(android.graphics.Color.red(old) > android.graphics.Color.blue(old) + 80)
        val release = CountDownLatch(1)
        VinylPreviewMetadataRetriever.artwork = cover(android.graphics.Color.BLUE)
        VinylPreviewMetadataRetriever.entered = CountDownLatch(1)
        VinylPreviewMetadataRetriever.gate = release
        try {
            compose.runOnIdle { song.value = second }
            compose.waitForIdle()
            compose.waitUntil(5000) { VinylPreviewMetadataRetriever.entered!!.count == 0L }
            assertEquals("The prior cover stays visible during a blocked decode", old, labelPixel())
            release.countDown()
            compose.waitUntil(5000) { ArtworkLoader.cached(second.uri, 512) != null }
            val updated = labelPixel()
            assertTrue("New cover replaces the retained cover", android.graphics.Color.blue(updated) >
                android.graphics.Color.red(updated) + 80)
        } finally {
            release.countDown()
            VinylPreviewMetadataRetriever.gate = null
            VinylPreviewMetadataRetriever.entered = null
        }
    }

    @Test fun theDeckKeepsRealArtworkLoadingAndRendersItsLabelInsideTheRecord() = runBlocking {
        val cover = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(cover)
        canvas.drawColor(android.graphics.Color.rgb(238, 119, 146))
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.rgb(150, 42, 75)
            style = Paint.Style.STROKE; strokeWidth = 9f
        }
        for (i in 0..5) canvas.drawArc(-140f + i * 49, 65f + i * 19,
            340f + i * 49, 450f + i * 19, 205f, 190f, false, paint)
        paint.apply { color = android.graphics.Color.rgb(42, 25, 32); style = Paint.Style.FILL
            textSize = 34f; textAlign = Paint.Align.CENTER; isFakeBoldText = true }
        canvas.drawText("TEST COVER", 256f, 130f, paint)
        VinylPreviewMetadataRetriever.artwork = ByteArrayOutputStream().also {
            cover.compress(Bitmap.CompressFormat.PNG, 100, it)
        }.toByteArray()
        cover.recycle()
        val song = Song(991L, "Artwork test fixture", "Fixture artist", "Fixture album",
            "file:///vinyl-artwork-${System.nanoTime()}.mp3", 190_000L, 0L)
        assertNotNull(ArtworkLoader.load(compose.activity, song.uri, 512))
        val position = mutableStateOf(0L)
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                MaterialTheme {
                    Box(Modifier.requiredSize(411.dp, 430.dp).background(VinylPaper).testTag("deck-preview"),
                        contentAlignment = Alignment.Center) {
                        VinylTurntableDeck(360.dp, false, true, true, position,
                            song.durationMs, 0,
                            TurntableLabels("Play", "Pause", "Previous", "Next", "Shuffle", "Shuffle on", "Shuffle off",
                                "Repeat", "Repeat off", "Repeat all", "Repeat one", "Seek", "Settings", "Playback options"),
                            {}, {}, { modifier -> VinylRecord(song, false, modifier.testTag("record-with-artwork")) })
                    }
                }
            }
        }
        compose.waitForIdle()
        val bounds = compose.onNodeWithTag("deck-preview").getUnclippedBoundsInRoot()
        compose.onNodeWithTag("record-with-artwork").assertIsDisplayed()
        compose.runOnIdle {
            val image = Bitmap.createBitmap(411, 430, Bitmap.Config.ARGB_8888)
            val output = Canvas(image)
            output.translate(-bounds.left.value, -bounds.top.value)
            compose.activity.findViewById<View>(android.R.id.content).draw(output)
            val pixel = image.getPixel(240, 215)
            assertTrue("The loaded cover remains pink inside the record label",
                android.graphics.Color.red(pixel) > android.graphics.Color.green(pixel) + 40)
            val dir = File("build/reports/vinyl-preview").apply { mkdirs() }
            File(dir, "silver-machine-artwork.png").outputStream().use {
                image.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
            image.recycle()
        }
    }
}
