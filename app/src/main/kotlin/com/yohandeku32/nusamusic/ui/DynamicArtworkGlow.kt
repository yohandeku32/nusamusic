package com.yohandeku32.nusamusic.ui

import android.content.SharedPreferences
import android.graphics.Bitmap
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.MotionDurationScale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

internal object DynamicGlowPreference {
    private const val KEY = "immersive_dynamic_glow_enabled"
    fun read(preferences: SharedPreferences): Boolean = preferences.getBoolean(KEY, false)
    fun write(preferences: SharedPreferences, enabled: Boolean) {
        preferences.edit().putBoolean(KEY, enabled).apply()
    }
}

/** Preserve spatial light from the video; dark pixels contribute no black wash. */
internal fun artworkGlowPixels(pixels: IntArray, width: Int, height: Int): IntArray {
    val diffuse = blurArgb(pixels, width, height, radius = (minOf(width, height) / 5).coerceAtLeast(1), passes = 2)
    for (index in diffuse.indices) {
        val pixel = diffuse[index]
        val red = (pixel ushr 16) and 255
        val green = (pixel ushr 8) and 255
        val blue = pixel and 255
        val luminance = red * 0.2126f + green * 0.7152f + blue * 0.0722f
        val peak = maxOf(red, green, blue)
        val depth = (index / width).toFloat() / height
        val alpha = ((luminance * 0.65f + peak * 0.35f) * 0.55f * (1f - depth * 0.72f))
            .roundToInt().coerceIn(0, 140)
        diffuse[index] = (alpha shl 24) or (pixel and 0x00FFFFFF)
    }
    return diffuse
}

/** Sequential capture bounds readback/memory; cancellation owns a whole video session. */
internal suspend fun sampleArtworkGlow(
    position: () -> Long?,
    capture: () -> Bitmap?,
    publish: (Bitmap) -> Unit
) {
    val context = currentCoroutineContext()
    var lastPosition: Long? = null
    while (true) {
        val scale = context[MotionDurationScale]?.scaleFactor ?: 1f
        // Keep each image fade shorter than the sampling interval, including
        // devices configured for slower animations. Zero means no readback.
        if (scale <= 0f) return
        delay((200f * scale.coerceAtLeast(1f)).toLong())
        context.ensureActive()
        val now = position() ?: continue
        if (now == lastPosition) continue
        val frame = capture() ?: continue
        val light = try {
            withContext(Dispatchers.Default) { createArtworkGlow(frame) }
        } finally { frame.recycle() }
        context.ensureActive()
        publish(light)
        lastPosition = now
    }
}

internal fun createArtworkGlow(frame: Bitmap): Bitmap {
    val pixels = IntArray(frame.width * frame.height)
    frame.getPixels(pixels, 0, frame.width, 0, 0, frame.width, frame.height)
    return Bitmap.createBitmap(artworkGlowPixels(pixels, frame.width, frame.height),
        frame.width, frame.height, Bitmap.Config.ARGB_8888)
}

@Composable
internal fun DynamicArtworkGlow(bitmap: Bitmap?, modifier: Modifier = Modifier) {
    // Samples arrive >=200ms apart; the 160ms fade completes before the next
    // one, keeping two tiny images rather than stacking track-change fades.
    Crossfade(targetState = bitmap, animationSpec = tween(160),
        label = "dynamicArtworkLight", modifier = modifier) { light ->
        if (light != null) Image(light.asImageBitmap(), contentDescription = null,
            contentScale = ContentScale.FillBounds, modifier = Modifier.fillMaxSize())
    }
}

