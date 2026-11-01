package com.yohandeku32.nusamusic.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yohandeku32.nusamusic.model.Song

internal fun filterVinylCatalog(songs: List<Song>, query: String): List<Song> {
    val term = query.trim()
    return if (term.isEmpty()) songs else songs.filter {
        it.title.contains(term, ignoreCase = true) || it.artist.contains(term, ignoreCase = true)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VinylSongList(
    songs: List<Song>, state: LazyGridState, currentSongId: Long?, isPlaying: Boolean,
    backgroundColor: Color, playLabel: String, pauseLabel: String, unknownArtistLabel: String,
    onPlay: (Song) -> Unit, onRequestDelete: (Song) -> Unit, onTogglePlay: () -> Unit,
    artwork: @Composable (Song, Modifier) -> Unit, modifier: Modifier = Modifier,
    deleteLabel: String = "Delete", query: String = "", onQueryChange: (String) -> Unit = {},
    searchLabel: String = "Search title or artist", clearSearchLabel: String = "Clear search",
    noResultsLabel: String = "No matching songs", moreLabel: String = "Song options"
) {
    val fontScale = LocalDensity.current.fontScale
    val keyboard = LocalSoftwareKeyboardController.current
    val focus = LocalFocusManager.current
    val ink = MaterialTheme.colorScheme.onBackground
    val secondary = MaterialTheme.colorScheme.onSurfaceVariant
    BoxWithConstraints(modifier.background(backgroundColor).testTag("vinyl-catalog")) {
        val contentWidth = minOf(maxWidth, 920.dp)
        val columns = when {
            fontScale >= 1.6f -> if (contentWidth >= 640.dp) 2 else 1
            contentWidth < 300.dp -> 1
            contentWidth < 600.dp -> 2
            contentWidth < 800.dp -> 3
            else -> 4
        }
        Column(Modifier.widthIn(max = 920.dp).fillMaxSize().align(Alignment.TopCenter)) {
            OutlinedTextField(
                value = query, onValueChange = onQueryChange, singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { keyboard?.hide(); focus.clearFocus() }),
                placeholder = { Text(searchLabel, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = if (query.isNotEmpty()) ({
                    IconButton(onClick = { onQueryChange("") }) { Icon(Icons.Default.Close, clearSearchLabel) }
                }) else null,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = ink, unfocusedTextColor = ink,
                    focusedBorderColor = ink, unfocusedBorderColor = ink.copy(alpha = 0.12f),
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedPlaceholderColor = secondary, unfocusedPlaceholderColor = secondary),
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 32.dp, top = 8.dp, bottom = 16.dp)
                    .testTag("vinyl-catalog-search")
                    .semantics { contentDescription = searchLabel }
            )
            if (songs.isEmpty()) {
                Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Text(noResultsLabel, color = secondary, modifier = Modifier.padding(24.dp))
                }
            } else LazyVerticalGrid(
                columns = GridCells.Fixed(columns), state = state,
                contentPadding = PaddingValues(start = 16.dp, end = 32.dp, top = 2.dp, bottom = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.fillMaxWidth().weight(1f).testTag("vinyl-catalog-grid")
            ) {
                items(songs, key = { it.id }, contentType = { "vinyl-jacket" }) { song ->
                    val current = currentSongId == song.id
                    val playing = current && isPlaying
                    val artist = song.artist.ifBlank { unknownArtistLabel }
                    var menu by remember(song.id) { mutableStateOf(false) }
                    var focused by remember(song.id) { mutableStateOf(false) }
                    val shape = RoundedCornerShape(6.dp)
                    Column(Modifier.fillMaxWidth().clip(shape)
                        .border(if (current || focused) 1.5.dp else 0.dp,
                            if (current || focused) ink else Color.Transparent, shape)
                        .onFocusChanged { focused = it.isFocused }
                        .combinedClickable(role = Role.Button,
                            onClickLabel = if (playing) pauseLabel else playLabel,
                            onLongClickLabel = "$deleteLabel ${song.title}",
                            onClick = {
                                keyboard?.hide()
                                if (current) onTogglePlay() else onPlay(song)
                            },
                            onLongClick = { onRequestDelete(song) })
                        .semantics { selected = current; contentDescription = "${song.title}, $artist" }
                        .testTag("vinyl-sleeve-${song.id}").padding(6.dp)) {
                        BoxWithConstraints(Modifier.fillMaxWidth().aspectRatio(1f / 0.82f)) {
                            val side = maxWidth * 0.82f
                            Box(Modifier.size(side).align(Alignment.CenterEnd).drawWithCache {
                                val painter = VinylPainter(size)
                                onDrawBehind { with(painter) { drawFace(); drawReflections() } }
                            })
                            Box(Modifier.size(side).align(Alignment.CenterStart)
                                .shadow(3.dp, RoundedCornerShape(2.dp), clip = false)
                                .clip(RoundedCornerShape(2.dp))) {
                                artwork(song, Modifier.fillMaxSize())
                                Box(Modifier.matchParentSize().border(0.5.dp, Color.Black.copy(alpha = 0.14f), RoundedCornerShape(2.dp)))
                            }
                            if (current) Box(Modifier.size(32.dp).align(Alignment.BottomEnd)
                                .background(ink, CircleShape), contentAlignment = Alignment.Center) {
                                Icon(if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null,
                                    Modifier.size(19.dp), tint = backgroundColor)
                            }
                        }
                        Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.Top) {
                            Column(Modifier.weight(1f).padding(top = 4.dp)) {
                                Text(song.title, color = ink, fontSize = 15.sp, lineHeight = 20.sp,
                                    fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Text(artist, color = secondary, fontSize = 12.sp, lineHeight = 17.sp,
                                    maxLines = if (fontScale >= 1.6f) 2 else 1, overflow = TextOverflow.Ellipsis)
                            }
                            Box {
                                IconButton(onClick = { keyboard?.hide(); menu = true }, modifier = Modifier.size(44.dp)
                                    .testTag("vinyl-song-menu-${song.id}")) {
                                    Icon(Icons.Default.MoreHoriz, "$moreLabel ${song.title}", tint = secondary)
                                }
                                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                                    DropdownMenuItem(text = { Text(deleteLabel) }, onClick = {
                                        menu = false; onRequestDelete(song)
                                    })
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun VinylCatalogTrackInfo(
    title: String, artist: String, openLabel: String, onOpen: () -> Unit,
    artwork: @Composable (Modifier) -> Unit, modifier: Modifier = Modifier
) {
    Row(modifier.clip(RoundedCornerShape(8.dp)).clickable(role = Role.Button, onClickLabel = openLabel, onClick = onOpen)
        .semantics { contentDescription = "$openLabel: $title, $artist" }.padding(end = 6.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(36.dp).clip(RoundedCornerShape(3.dp))) { artwork(Modifier.fillMaxSize()) }
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 14.sp,
                lineHeight = 18.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Text(artist, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp,
                lineHeight = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
