package com.yohandeku32.nusamusic.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import androidx.compose.ui.graphics.asAndroidBitmap
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

    @Test fun studioSkinRetainsOriginalChassisPixelsAndReusesDecodedLayers() = runBlocking {
        val assets = StudioBlackAssetLoader.load(compose.activity)
        assertSame(assets, StudioBlackAssetLoader.load(compose.activity))
        val source = BitmapFactory.decodeResource(compose.activity.resources,
            com.yohandeku32.nusamusic.R.raw.studio_black_reference,
            BitmapFactory.Options().apply { inScaled = false })
        val machine = assets.machine.asAndroidBitmap()
        assertEquals(499, machine.width)
        assertEquals(382, machine.height)
        assertEquals("The chassis is the original render, not a vector approximation",
            source.getPixel(727, 420), machine.getPixel(200, 330))
        assertEquals(0, android.graphics.Color.alpha(machine.getPixel(0, 100)))
        assertEquals(0xFF101112.toInt(), assets.record.asAndroidBitmap().getPixel(256, 256))
        source.recycle()
    }

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
        val artworkColor = java.util.concurrent.atomic.AtomicReference<Color?>()
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                MaterialTheme { VinylRecord(song.value, false, Modifier.size(240.dp).testTag("record"),
                    onArtworkColor = artworkColor::set) }
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
        compose.waitUntil(5000) { artworkColor.get()?.let { it.red > it.blue } == true }
        val oldColor = artworkColor.get()
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
            assertEquals("The tint follows the retained cover", oldColor, artworkColor.get())
            release.countDown()
            compose.waitUntil(5000) { ArtworkLoader.cached(second.uri, 512) != null }
            val updated = labelPixel()
            assertTrue("New cover replaces the retained cover", android.graphics.Color.blue(updated) >
                android.graphics.Color.red(updated) + 80)
            compose.waitUntil(5000) { artworkColor.get()?.let { it.blue > it.red } == true }
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
        val theme = mutableStateOf(VinylMachineTheme.CLASSIC)
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                MaterialTheme {
                    Box(Modifier.requiredSize(411.dp, 430.dp).background(VinylPaper).testTag("deck-preview"),
                        contentAlignment = Alignment.Center) {
                        VinylTurntableDeck(360.dp, false, true, true, position,
                            song.durationMs, 0,
                            TurntableLabels("Play", "Pause", "Previous", "Next", "Shuffle", "Shuffle on", "Shuffle off",
                                "Repeat", "Repeat off", "Repeat all", "Repeat one", "Seek", "Settings", "Playback options"),
                            {}, {}, { modifier -> VinylRecord(song, false, modifier.testTag("record-with-artwork"),
                                compactLabel = theme.value == VinylMachineTheme.STUDIO_BLACK) },
                            theme = theme.value)
                    }
                }
            }
        }
        compose.waitForIdle()
        val bounds = compose.onNodeWithTag("deck-preview").getUnclippedBoundsInRoot()
        compose.onNodeWithTag("record-with-artwork").assertIsDisplayed()
        for (selectedTheme in VinylMachineTheme.entries) {
            compose.runOnIdle { theme.value = selectedTheme }
            compose.waitForIdle()
            if (selectedTheme == VinylMachineTheme.STUDIO_BLACK) compose.waitUntil(10_000) {
                compose.onAllNodesWithTag("studio-black-raster").fetchSemanticsNodes().isNotEmpty()
            }
            val disc = compose.onNodeWithTag("record-with-artwork").fetchSemanticsNode().boundsInRoot
            val photo = if (selectedTheme == VinylMachineTheme.STUDIO_BLACK)
                compose.onNodeWithTag("studio-black-raster").fetchSemanticsNode().boundsInRoot else null
            compose.runOnIdle {
                val image = Bitmap.createBitmap(411, 430, Bitmap.Config.ARGB_8888)
                val output = Canvas(image)
                output.translate(-bounds.left.value, -bounds.top.value)
                compose.activity.findViewById<View>(android.R.id.content).draw(output)
                val dir = File("build/reports/vinyl-preview").apply { mkdirs() }
                val name = if (selectedTheme == VinylMachineTheme.CLASSIC) "silver-machine-artwork" else "studio-black-artwork"
                File(dir, "$name.png").outputStream().use {
                    image.compress(Bitmap.CompressFormat.PNG, 100, it)
                }
                val unit = photo?.let { it.width / 499f }
                val cx = if (photo != null) photo.left + 218.5f * unit!! - bounds.left.value
                    else (disc.left + disc.right) / 2 - bounds.left.value
                val cy = if (photo != null) photo.top + 148f * unit!! - bounds.top.value
                    else (disc.top + disc.bottom) / 2 - bounds.top.value
                val pixel = image.getPixel((cx + 10f).toInt(), (cy + 10f).toInt())
                assertTrue("The loaded cover remains pink inside the record label",
                    android.graphics.Color.red(pixel) > android.graphics.Color.green(pixel) + 40)
                fun brightness(x: Float, y: Float): Float {
                    var total = 0f
                    for (dx in -3..3) for (dy in -3..3) {
                        val color = image.getPixel(x.toInt() + dx, y.toInt() + dy)
                        total += (android.graphics.Color.red(color) + android.graphics.Color.green(color) +
                            android.graphics.Color.blue(color)) / 3f
                    }
                    return total / 49
                }
                val radial = (disc.right - disc.left) * 0.5f
                val verticalRadius = (disc.bottom - disc.top) * 0.5f
                if (selectedTheme == VinylMachineTheme.CLASSIC) {
                    assertTrue("Soft top light reveals the playing grooves",
                        brightness(cx, cy - verticalRadius * 0.8f) > brightness(cx - radial * 0.8f, cy) + 6)
                    assertTrue("The narrow center collar stays dark",
                        brightness(cx, cy - verticalRadius * 0.8f) > brightness(cx, cy - verticalRadius * 0.54f) + 6)
                } else {
                    val levels = (0..7).map { index ->
                        val angle = index * kotlin.math.PI.toFloat() / 4
                        brightness(cx + kotlin.math.cos(angle) * radial * 0.72f,
                            cy + kotlin.math.sin(angle) * verticalRadius * 0.72f)
                    }
                    assertTrue("Photographed record material retains its light variation", levels.max() - levels.min() > 8f)
                }
                image.recycle()
            }
        }
    }

    @Test fun recordArtworkAndSurfaceVisiblyRotateThenSettleWhenPaused() = verifyRotation(false)

    @Test fun studioRecordRotatesWithinItsPerspectivePlaneAndPauses() = verifyRotation(true)

    private fun verifyRotation(compactLabel: Boolean) = runBlocking {
        val cover = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(cover)
        canvas.drawColor(android.graphics.Color.rgb(210, 65, 100))
        canvas.drawRect(0f, 0f, 28f, 64f, Paint().apply { color = android.graphics.Color.CYAN })
        VinylPreviewMetadataRetriever.artwork = ByteArrayOutputStream().also {
            cover.compress(Bitmap.CompressFormat.PNG, 100, it)
        }.toByteArray()
        cover.recycle()
        val song = Song(992L, "Rotation fixture", "Fixture", "Fixture",
            "file:///vinyl-rotation-${System.nanoTime()}.mp3", 190_000L, 0L)
        assertNotNull(ArtworkLoader.load(compose.activity, song.uri, 512))
        val playing = mutableStateOf(false)
        val texture = if (compactLabel) StudioBlackAssetLoader.load(compose.activity).record else null
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f), LocalVinylRecordTexture provides texture) {
                MaterialTheme { VinylRecord(song, playing.value,
                    Modifier.size(240.dp).then(if (compactLabel) Modifier.studioRecordPerspective() else Modifier)
                        .testTag("spinning-record"), compactLabel = compactLabel) }
            }
        }
        fun pixels(name: String? = null): IntArray {
            val bounds = compose.onNodeWithTag("spinning-record").fetchSemanticsNode().boundsInRoot
            var result = IntArray(240 * 240)
            compose.runOnIdle {
                val bitmap = Bitmap.createBitmap(240, 240, Bitmap.Config.ARGB_8888)
                val output = Canvas(bitmap)
                output.translate(120f - (bounds.left + bounds.right) / 2,
                    120f - (bounds.top + bounds.bottom) / 2)
                compose.activity.findViewById<View>(android.R.id.content).draw(output)
                bitmap.getPixels(result, 0, 240, 0, 0, 240, 240)
                if (name != null) {
                    val dir = File("build/reports/vinyl-preview").apply { mkdirs() }
                    File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                }
                bitmap.recycle()
            }
            return result
        }
        compose.waitForIdle()
        compose.mainClock.autoAdvance = false
        compose.runOnIdle { playing.value = true }
        compose.mainClock.advanceTimeBy(1600)
        val prefix = if (compactLabel) "studio-record-rotation" else "record-rotation"
        val first = pixels("$prefix-a")
        compose.mainClock.advanceTimeBy(640)
        val second = pixels("$prefix-b")
        assertTrue("Artwork turns visibly", first.indices.count { first[it] != second[it] } > 1000)
        val movingSurface = first.indices.count { index ->
            val dx = index % 240 - 120
            val dy = (index / 240 - 120) / if (compactLabel) 0.84f else 1f
            val distanceSquared = dx * dx + dy * dy
            distanceSquared in (75f * 75f)..(112f * 112f) &&
                kotlin.math.abs(android.graphics.Color.red(first[index]) - android.graphics.Color.red(second[index])) >= 8
        }
        assertTrue("The black playing surface visibly rotates independently of the artwork", movingSurface > 300)
        compose.runOnIdle { playing.value = false }
        compose.mainClock.advanceTimeBy(1400)
        val paused = pixels()
        compose.mainClock.advanceTimeBy(640)
        assertArrayEquals("Paused record remains still after deceleration", paused, pixels())
        compose.runOnIdle { playing.value = true }
        compose.mainClock.advanceTimeBy(1600)
        assertFalse("Playback resumes the rotation", paused.contentEquals(pixels()))
    }
}
