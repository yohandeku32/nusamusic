package com.yohandeku32.nusamusic.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yohandeku32.nusamusic.data.SongCredits

@Composable
internal fun LyricsCredits(credits: SongCredits, modifier: Modifier = Modifier) {
    if (credits.isEmpty) return
    Column(modifier.fillMaxWidth().padding(top = 24.dp, bottom = 12.dp)) {
        if (credits.composers.isNotEmpty()) CreditRole("Composed by", credits.composers)
        if (credits.lyricists.isNotEmpty()) CreditRole("Lyrics by", credits.lyricists)
    }
}

@Composable
private fun CreditRole(label: String, names: List<String>) {
    Column(Modifier.padding(bottom = 12.dp)) {
        Text(label, color = Color.White.copy(alpha = 0.55f), fontSize = 12.sp)
        Text(names.joinToString(", "), Modifier.padding(top = 4.dp),
            color = Color.White.copy(alpha = 0.85f), fontSize = 16.sp, lineHeight = 22.sp,
            fontWeight = FontWeight.SemiBold)
    }
}
