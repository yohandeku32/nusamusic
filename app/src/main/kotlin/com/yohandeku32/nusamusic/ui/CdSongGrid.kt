package com.yohandeku32.nusamusic.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yohandeku32.nusamusic.model.Song

@Composable
internal fun CdSongGrid(
    songs: List<Song>,
    currentSongId: Long?,
    onPlay: (Song) -> Unit,
    artwork: @Composable (Song?, Modifier) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier,
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 140.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        items(count = songs.size, key = { songs[it].id }, contentType = { "cd-song" }) { index ->
            val song = songs[index]
            Column(Modifier.fillMaxWidth().clickable(onClickLabel = "Putar ${song.title}") { onPlay(song) }) {
                Box(Modifier.fillMaxWidth().aspectRatio(1.12f)) {
                    CdJewelCase(
                        modifier = Modifier.matchParentSize(),
                        showStickers = false
                    ) { discModifier ->
                        CdDiscSurface(song, artwork, discModifier, shadowElevation = 0.dp)
                    }
                    CdHiResBadge(
                        song,
                        Modifier.align(androidx.compose.ui.Alignment.BottomEnd)
                            .offset(x = (-8).dp, y = (-8).dp)
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    song.title,
                    color = if (song.id == currentSongId) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    song.artist,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
