package com.yohandeku32.nusamusic.ui

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.MotionDurationScale
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DynamicArtworkGlowTest {
    @Test fun preferenceDefaultsOffAndSurvivesReopening() {
        val context = RuntimeEnvironment.getApplication()
        val prefs = context.getSharedPreferences("dynamic-glow-test", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        assertFalse(DynamicGlowPreference.read(prefs))
        DynamicGlowPreference.write(prefs, true)
        assertTrue(DynamicGlowPreference.read(context.getSharedPreferences("dynamic-glow-test", Context.MODE_PRIVATE)))
        DynamicGlowPreference.write(prefs, false)
        assertFalse(DynamicGlowPreference.read(prefs))
    }

    @Test fun darkFramesContributeNoBlackWash() {
        assertTrue(artworkGlowPixels(IntArray(96 * 96) { 0xFF000000.toInt() }, 96, 96)
            .all { it == 0 })
    }

    @Test fun neutralFramesRemainNeutralAndControlsReceiveLessLight() {
        val pixels = artworkGlowPixels(IntArray(96 * 96) { 0xFFCCCCCC.toInt() }, 96, 96)
        assertTrue(pixels.all { ((it ushr 16) and 255) == ((it ushr 8) and 255) &&
            ((it ushr 8) and 255) == (it and 255) && (it ushr 24) <= 140 })
        assertTrue((pixels.first() ushr 24) > (pixels.last() ushr 24) * 2)
    }

    @Test fun lightTracksTheSourcePositionAndHue() {
        fun sample(left: Boolean) = artworkGlowPixels(IntArray(96 * 64) { i ->
            if ((i % 96 < 32) == left && (left || i % 96 >= 64)) 0xFFFF3C00.toInt()
            else 0xFF000000.toInt()
        }, 96, 64)
        val left = sample(true)
        val right = sample(false)
        fun centre(pixels: IntArray): Double {
            val weight = pixels.sumOf { (it ushr 24).toDouble() }
            return pixels.indices.sumOf { (it % 96) * (pixels[it] ushr 24).toDouble() } / weight
        }
        assertTrue(centre(right) - centre(left) > 30)
        assertTrue(left.all { ((it ushr 16) and 255) >= ((it ushr 8) and 255) && (it and 255) == 0 })
    }

    @Test fun pausedOrUnchangedVideoDoesNotKeepCapturing() = runBlocking {
        var position: Long? = null
        var captures = 0
        var published = 0
        val job = launch {
            sampleArtworkGlow({ position }, {
                captures++
                Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888)
            }, { published++ })
        }
        delay(240)
        assertEquals(0, captures)
        position = 10
        withTimeout(3_000) { while (published == 0) delay(10) }
        delay(450)
        assertEquals(1, captures)
        position = 20
        withTimeout(3_000) { while (published < 2) delay(10) }
        job.cancelAndJoin()
        val stopped = captures
        delay(240)
        assertEquals(stopped, captures)
    }

    @Test fun captureFailureRetriesAndCancellationCannotPublishAnOldFrame() = runBlocking {
        var captures = 0
        var published = 0
        lateinit var frame: Bitmap
        lateinit var job: Job
        job = launch {
            sampleArtworkGlow({ 10L }, {
                captures++
                if (captures == 1) null else {
                    frame = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888)
                    job.cancel()
                    frame
                }
            }, { published++ })
        }
        withTimeout(3_000) { job.join() }
        assertEquals(2, captures)
        assertEquals(0, published)
        assertTrue(frame.isRecycled)
    }

    @Test fun zeroMotionScaleDoesNotCapture() = runBlocking {
        val scale = object : MotionDurationScale { override val scaleFactor = 0f }
        var captures = 0
        withContext(scale) {
            sampleArtworkGlow({ 1L }, { captures++; null }, { fail("No dynamic frame expected") })
        }
        assertEquals(0, captures)
    }
}
