package com.yohandeku32.nusamusic.ui

import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import kotlin.math.roundToInt

internal object PlayerScalePreference {
    private const val KEY = "immersive_player_scale"
    const val MIN = 1f
    const val MAX = 1.25f

    fun normalize(value: Float): Float =
        if (value.isFinite()) (value.coerceIn(MIN, MAX) * 20).roundToInt() / 20f else MIN

    fun read(preferences: SharedPreferences): Float = normalize(
        runCatching { preferences.getFloat(KEY, MIN) }.getOrDefault(MIN)
    )

    fun write(preferences: SharedPreferences, value: Float) {
        preferences.edit().putFloat(KEY, normalize(value)).apply()
    }
}

internal val LocalPlayerBaseDensity = staticCompositionLocalOf<Density?> { null }

/** Reflow the player at its selected size while preserving system font scaling. */
@Composable
internal fun PlayerScaleHost(scale: Float, content: @Composable () -> Unit) {
    val base = LocalDensity.current
    val normalized = PlayerScalePreference.normalize(scale)
    val scaled = remember(base, normalized) { Density(base.density * normalized, base.fontScale) }
    CompositionLocalProvider(LocalPlayerBaseDensity provides base, LocalDensity provides scaled, content = content)
}

@Composable
internal fun currentPlayerScale(): Float =
    LocalPlayerBaseDensity.current?.let { LocalDensity.current.density / it.density } ?: 1f

/** Shared navigation returns to screen density as a full-screen page opens. */
@Composable
internal fun PlayerNavigationScaleHost(
    playerDensity: Density,
    screenDensity: Density,
    reveal: () -> Float,
    content: @Composable () -> Unit
) {
    val progress = reveal().coerceIn(0f, 1f)
    val density = remember(playerDensity, screenDensity, progress) {
        Density(playerDensity.density + (screenDensity.density - playerDensity.density) * progress,
            screenDensity.fontScale)
    }
    CompositionLocalProvider(LocalDensity provides density, content = content)
}
