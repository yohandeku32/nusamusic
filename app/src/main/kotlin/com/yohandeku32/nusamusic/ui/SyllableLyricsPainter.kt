package com.yohandeku32.nusamusic.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.style.ResolvedTextDirection
import androidx.compose.ui.unit.dp
import com.mocharealm.accompanist.lyrics.ui.composable.lyrics.SyllableLayout
import kotlin.math.PI
import kotlin.math.sin

/** Every authored unit has its own sweep; a row-wide gradient cannot honor syllable gaps. */
internal fun DrawScope.drawSyllableLyrics(layouts: List<List<SyllableLayout>>, now: Int, focused: Boolean) {
    layouts.forEach { row -> row.forEach syllable@ { layout ->
        if (layout.syllable.content.isBlank()) return@syllable
        val progress = syllableProgress(layout.syllable, now)
        val singing = focused && now >= layout.syllable.start && now < layout.syllable.end
        val lift = if (singing) sin(progress * PI).toFloat() * 1.5.dp.toPx() else 0f
        val position = layout.position - Offset(0f, lift)
        drawText(layout.textLayoutResult, color = Color.White.copy(alpha = 0.24f), topLeft = position)
        if (progress > 0f) {
            val rtl = layout.textLayoutResult.getParagraphDirection(0) == ResolvedTextDirection.Rtl
            val width = layout.textLayoutResult.size.width.toFloat()
            val left = if (rtl) position.x + width * (1f - progress) else position.x
            val right = if (rtl) position.x + width else position.x + width * progress
            clipRect(left = left, right = right, top = position.y - 8.dp.toPx(),
                bottom = position.y + layout.textLayoutResult.size.height + 8.dp.toPx()) {
                drawText(layout.textLayoutResult, color = Color.White, topLeft = position,
                    shadow = if (singing) Shadow(Color.White.copy(alpha = 0.25f), Offset.Zero, 5.dp.toPx()) else null)
            }
        }
    } }
}
