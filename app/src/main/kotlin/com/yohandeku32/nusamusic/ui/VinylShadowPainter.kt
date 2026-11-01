package com.yohandeku32.nusamusic.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope

/** Stationary cast and contact shadows, drawn outside the record's layout bounds. */
internal class VinylShadowPainter(size: Size, softness: Float, offset: Float) {
    private val radius = size.minDimension / 2f
    private val discCenter = Offset(size.width / 2f, size.height / 2f)
    private val shadowCenter = discCenter + Offset(0f, offset)
    private val shadowRadius = radius + softness
    private val castShadow = Brush.radialGradient(
        0f to Color.Black.copy(alpha = 0.24f),
        ((radius - softness * 0.60f) / shadowRadius).coerceIn(0f, 1f) to Color.Black.copy(alpha = 0.24f),
        radius / shadowRadius to Color.Black.copy(alpha = 0.13f),
        (radius + softness * 0.45f) / shadowRadius to Color.Black.copy(alpha = 0.035f),
        1f to Color.Transparent,
        center = shadowCenter,
        radius = shadowRadius
    )
    private val contactCenter = discCenter + Offset(0f, offset * 0.35f)
    private val contactRadius = radius + softness * 0.38f
    private val contactShadow = Brush.radialGradient(
        0f to Color.Black.copy(alpha = 0.16f),
        radius * 0.96f / contactRadius to Color.Black.copy(alpha = 0.16f),
        1f to Color.Transparent,
        center = contactCenter,
        radius = contactRadius
    )
    private val lowerEdgeCenter = discCenter + Offset(0f, offset * 0.25f)

    fun DrawScope.drawShadow() {
        drawCircle(castShadow, shadowRadius, shadowCenter)
        drawCircle(contactShadow, contactRadius, contactCenter)
        drawCircle(Color(0xFF060707), radius, lowerEdgeCenter)
    }
}
