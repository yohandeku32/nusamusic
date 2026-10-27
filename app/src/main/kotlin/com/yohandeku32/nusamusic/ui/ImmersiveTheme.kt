package com.yohandeku32.nusamusic.ui

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/** Immersive-only appearance, independent of CD/Vinyl and device theme. */
@Immutable
internal data class ImmersiveColors(
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val primaryText: Color,
    val secondaryText: Color,
    val activeRow: Color,
    val destructiveText: Color
)

internal fun immersiveColors(light: Boolean): ImmersiveColors =
    if (light) ImmersiveColors(
        background = Color(0xFFF8F9FB),
        surface = Color(0xFFFFFFFF),
        surfaceVariant = Color(0xFFEAECEF),
        primaryText = Color(0xFF171A20),
        secondaryText = Color(0xFF676C78),
        activeRow = Color(0xFFE8ECF2),
        destructiveText = Color(0xFFB3261E)
    ) else ImmersiveColors(
        background = Color(0xFF090A0D),
        surface = Color(0xFF1B1D22),
        surfaceVariant = Color(0xFF25262D),
        primaryText = Color(0xFFF8F8FA),
        secondaryText = Color(0xFF92949E),
        activeRow = Color.White.copy(alpha = 0.075f),
        destructiveText = Color(0xFFFFB4AB)
    )
