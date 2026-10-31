package com.yohandeku32.nusamusic.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun VinylLyricsSection(
    heading: String,
    viewportHeight: Dp,
    modifier: Modifier = Modifier,
    lyrics: @Composable (Modifier) -> Unit
) {
    Column(
        modifier.fillMaxWidth().height(viewportHeight / 2).background(Color.Black).navigationBarsPadding()
            .padding(horizontal = 28.dp, vertical = 20.dp)
    ) {
        Text(heading, color = Color(0xFFAAAAAA), fontSize = 12.sp,
            letterSpacing = 1.6.sp, modifier = Modifier.semantics { heading() })
        Spacer(Modifier.height(16.dp))
        lyrics(Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(22.dp))
            .background(Color(0xFF111111)))
    }
}
