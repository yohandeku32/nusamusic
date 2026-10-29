package com.yohandeku32.nusamusic.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import com.moriafly.salt.ui.SaltColors
import com.moriafly.salt.ui.SaltDynamicColors
import com.moriafly.salt.ui.SaltTheme
import com.moriafly.salt.ui.saltConfigs
import com.moriafly.salt.ui.saltDimens

/** Salt components inherit Aruna's current palette; the existing Settings section owns insets. */
@Composable
internal fun ArunaSaltTheme(content: @Composable () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val palette = remember(colors) { SaltColors(
        highlight = colors.primary, text = colors.onSurface, subText = colors.onSurfaceVariant,
        background = colors.background, subBackground = colors.surface, popup = colors.surface,
        stroke = colors.outlineVariant, onHighlight = colors.onPrimary
    ) }
    val configs = remember(colors) { saltConfigs(isDarkTheme = colors.surface.luminance() < 0.5f) }
    val dimensions = remember { saltDimens(item = 64.dp, padding = 0.dp) }
    SaltTheme(configs = configs, dynamicColors = SaltDynamicColors(palette, palette),
        dimens = dimensions, content = content)
}
