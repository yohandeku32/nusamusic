package com.yohandeku32.nusamusic.ui

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Sort
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yohandeku32.nusamusic.data.AudioCodecLoader
import com.yohandeku32.nusamusic.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

// The CD/Vinyl libraries stay unchanged. Immersive uses inexpensive static
// surfaces instead of live blur, per-row animations, shadows or scroll effects.
private val libraryBlack = Color(0xFF090A0D)
private val librarySurface = Color(0xFF1B1D22)
private val librarySecondary = Color(0xFF92949E)
private val libraryWhite = Color(0xFFF8F8FA)

private enum class ImmersiveSort(val label: String) {
    TITLE("Title A–Z"), TITLE_DESC("Title Z–A"), ARTIST("Artist"),
    ALBUM("Album"), NEWEST("Recently added"), LONGEST("Longest first")
}
private enum class FocusType { ARTIST, ALBUM }
private data class LibraryFocus(val type: FocusType, val name: String)

private fun findImmersiveSongs(
    songs: List<Song>, search: String, focus: LibraryFocus?, sort: ImmersiveSort
): List<Song> {
    val term = search.trim()
    val matches = songs.filter { song ->
        val inFocus = when (focus?.type) {
            FocusType.ARTIST -> song.artist.equals(focus.name, ignoreCase = true)
            FocusType.ALBUM -> song.album.equals(focus.name, ignoreCase = true)
            null -> true
        }
        inFocus && (term.isEmpty() ||
            song.title.contains(term, ignoreCase = true) ||
            song.artist.contains(term, ignoreCase = true) ||
            song.album.contains(term, ignoreCase = true))
    }
    return when (sort) {
        ImmersiveSort.TITLE -> matches.sortedBy { it.title.lowercase() }
        ImmersiveSort.TITLE_DESC -> matches.sortedByDescending { it.title.lowercase() }
        ImmersiveSort.ARTIST -> matches.sortedWith(
            compareBy<Song> { it.artist.lowercase() }.thenBy { it.title.lowercase() })
        ImmersiveSort.ALBUM -> matches.sortedWith(
            compareBy<Song> { it.album.lowercase() }.thenBy { it.title.lowercase() })
        ImmersiveSort.NEWEST -> matches.sortedWith(
            compareByDescending<Song> { it.dateAddedMs }.thenBy { it.title.lowercase() })
        ImmersiveSort.LONGEST -> matches.sortedByDescending { it.durationMs }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ImmersiveMusicLibrary(
    songs: List<Song>,
    currentSong: Song?,
    isPlaying: Boolean,
    hasMusicAccess: Boolean,
    isScanning: Boolean,
    emptyMessage: String,
    onReturnToPlayer: () -> Unit,
    onPlay: (Song, List<Song>) -> Unit,
    onPlayNext: (Song) -> Unit,
    onShare: (Song) -> Unit,
    onTogglePlay: () -> Unit,
    onScanMusic: () -> Unit,
    onRequestPermission: () -> Unit,
    artwork: @Composable (Song, Modifier) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs = remember(context) {
        context.getSharedPreferences("ui_preferences", Context.MODE_PRIVATE)
    }
    var search by rememberSaveable { mutableStateOf("") }
    var focus by remember { mutableStateOf<LibraryFocus?>(null) }
    var sort by rememberSaveable {
        mutableStateOf(runCatching {
            ImmersiveSort.valueOf(prefs.getString("immersive_library_sort", "TITLE") ?: "TITLE")
        }.getOrDefault(ImmersiveSort.TITLE))
    }
    var sortOpen by remember { mutableStateOf(false) }
    var visibleSongs by remember(songs) { mutableStateOf(songs) }
    val listState = rememberLazyListState()
    val loadMetadata by remember(listState) {
        derivedStateOf { !listState.isScrollInProgress }
    }

    // Debounce search. Sorting/filtering run off the UI thread and are cancelled
    // when the query changes, so fast typing cannot queue up list rebuilds.
    LaunchedEffect(songs, search, focus, sort) {
        if (search.isNotBlank()) delay(90)
        val result = withContext(Dispatchers.Default) {
            findImmersiveSongs(songs, search, focus, sort)
        }
        visibleSongs = result
        listState.scrollToItem(0)
    }
    BackHandler(enabled = focus != null) {
        focus = null
        search = ""
    }

    Box(
        modifier = modifier.fillMaxSize().background(libraryBlack)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().height(58.dp).padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    if (focus != null) { focus = null; search = "" }
                    else onReturnToPlayer()
                }) {
                    Icon(Icons.Rounded.ArrowBack, "Back", tint = libraryWhite)
                }
                Text(
                    "LIBRARY", color = librarySecondary, fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold, letterSpacing = 2.sp,
                    modifier = Modifier.weight(1f).padding(start = 8.dp)
                )
                IconButton(onClick = onScanMusic, enabled = !isScanning) {
                    Icon(Icons.Rounded.Refresh, "Refresh library", tint = libraryWhite)
                }
                Box {
                    IconButton(onClick = { sortOpen = true }) {
                        Icon(Icons.Rounded.Sort, "Sort songs", tint = libraryWhite)
                    }
                    DropdownMenu(expanded = sortOpen, onDismissRequest = { sortOpen = false }) {
                        ImmersiveSort.entries.forEach { option ->
                            DropdownMenuItem(
                                text = {
                                    Text(option.label, fontWeight =
                                        if (sort == option) FontWeight.SemiBold else FontWeight.Normal)
                                },
                                trailingIcon = {
                                    if (sort == option) Icon(Icons.Rounded.Check, null)
                                },
                                onClick = {
                                    sort = option
                                    prefs.edit().putString("immersive_library_sort", option.name).apply()
                                    sortOpen = false
                                }
                            )
                        }
                    }
                }
            }

            Row(
                Modifier.fillMaxWidth().padding(start = 24.dp, end = 20.dp, top = 12.dp, bottom = 18.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        focus?.name ?: "Your Music", color = libraryWhite,
                        fontSize = 31.sp, fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.8).sp, lineHeight = 36.sp,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(5.dp))
                    Text(
                        when (focus?.type) {
                            FocusType.ALBUM -> "ALBUM"
                            FocusType.ARTIST -> "ARTIST"
                            null -> "\${songs.size} songs on this device"
                        },
                        color = librarySecondary, fontSize = 12.sp
                    )
                }
                if (visibleSongs.isNotEmpty()) {
                    Button(
                        onClick = { onPlay(visibleSongs.first(), visibleSongs) },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = libraryWhite, contentColor = libraryBlack),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                        modifier = Modifier.height(40.dp)
                    ) {
                        Icon(Icons.Rounded.PlayArrow, null, modifier = Modifier.size(19.dp))
                        Spacer(Modifier.width(3.dp))
                        Text("Play all", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            BasicTextField(
                value = search, onValueChange = { search = it }, singleLine = true,
                textStyle = TextStyle(fontSize = 14.sp, color = libraryWhite),
                cursorBrush = SolidColor(libraryWhite),
                keyboardOptions = KeyboardOptions(
                    imeAction = androidx.compose.ui.text.input.ImeAction.Search),
                decorationBox = { field ->
                    Row(
                        Modifier.fillMaxSize().padding(horizontal = 15.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.Search, null, tint = librarySecondary,
                            modifier = Modifier.size(19.dp))
                        Spacer(Modifier.width(10.dp))
                        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                            if (search.isEmpty()) {
                                Text("Search songs, artists, albums",
                                    color = librarySecondary, fontSize = 13.sp)
                            }
                            field()
                        }
                        if (search.isNotEmpty()) {
                            IconButton(onClick = { search = "" }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Rounded.Close, "Clear search",
                                    tint = librarySecondary, modifier = Modifier.size(17.dp))
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
                    .height(48.dp).clip(RoundedCornerShape(15.dp))
                    .background(librarySurface)
            )
            Row(
                Modifier.fillMaxWidth().padding(start = 24.dp, end = 22.dp, top = 19.dp, bottom = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (visibleSongs.size == 1) "1 song" else "\${visibleSongs.size} songs",
                    color = librarySecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                Text(sort.label, color = librarySecondary, fontSize = 11.sp)
            }

            PullToRefreshBox(
                isRefreshing = isScanning, onRefresh = onScanMusic,
                modifier = Modifier.weight(1f)
            ) {
                if (!hasMusicAccess || (songs.isEmpty() && !isScanning)) {
                    Column(
                        Modifier.fillMaxSize().padding(horizontal = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            if (!hasMusicAccess) "Allow access to your music" else emptyMessage,
                            color = libraryWhite, fontWeight = FontWeight.SemiBold,
                            fontSize = 17.sp, textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(14.dp))
                        OutlinedButton(onClick = if (!hasMusicAccess) onRequestPermission else onScanMusic) {
                            Text(if (!hasMusicAccess) "Allow access" else "Scan again", color = libraryWhite)
                        }
                    }
                } else {
                    LazyColumn(
                        state = listState, modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 12.dp, end = 12.dp, top = 2.dp,
                            bottom = if (currentSong != null) 114.dp else 24.dp)
                    ) {
                        items(
                            items = visibleSongs,
                            key = { it.id },
                            contentType = { "immersive-track" }
                        ) { song ->
                            ImmersiveTrackRow(
                                song = song, active = currentSong?.id == song.id,
                                loadMetadata = loadMetadata, artwork = artwork,
                                onPlay = { onPlay(song, visibleSongs) },
                                onPlayNext = { onPlayNext(song) },
                                onShare = { onShare(song) },
                                onAlbum = {
                                    if (song.album.isNotBlank()) {
                                        focus = LibraryFocus(FocusType.ALBUM, song.album)
                                        search = ""
                                    }
                                },
                                onArtist = {
                                    if (song.artist.isNotBlank()) {
                                        focus = LibraryFocus(FocusType.ARTIST, song.artist)
                                        search = ""
                                    }
                                }
                            )
                        }
                        if (visibleSongs.isEmpty()) {
                            item(key = "empty-search", contentType = "empty") {
                                Text("No matching songs", color = librarySecondary,
                                    textAlign = TextAlign.Center, fontSize = 14.sp,
                                    modifier = Modifier.fillMaxWidth().padding(top = 70.dp))
                            }
                        }
                    }
                }
            }
        }

        if (currentSong != null) {
            Row(
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 14.dp)
                    .clip(RoundedCornerShape(20.dp)).background(librarySurface)
                    .border(0.7.dp, Color.White.copy(alpha = 0.09f), RoundedCornerShape(20.dp))
                    .clickable(onClick = onReturnToPlayer)
                    .padding(start = 9.dp, end = 5.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                artwork(currentSong, Modifier.size(43.dp).clip(RoundedCornerShape(12.dp)))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(currentSong.title, color = libraryWhite, fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold, maxLines = 1,
                        overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(2.dp))
                    Text(currentSong.artist.ifBlank { "Unknown artist" },
                        color = librarySecondary, fontSize = 11.sp,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                IconButton(onClick = onTogglePlay, modifier = Modifier.size(46.dp)) {
                    Icon(if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        if (isPlaying) "Pause" else "Play", tint = libraryWhite,
                        modifier = Modifier.size(24.dp))
                }
            }
        }
    }
}

@Composable
private fun ImmersiveTrackRow(
    song: Song, active: Boolean, loadMetadata: Boolean,
    artwork: @Composable (Song, Modifier) -> Unit,
    onPlay: () -> Unit, onPlayNext: () -> Unit, onShare: () -> Unit,
    onAlbum: () -> Unit, onArtist: () -> Unit
) {
    val context = LocalContext.current
    var codec by remember(song.uri) { mutableStateOf(AudioCodecLoader.cached(song.uri)) }
    var menuOpen by remember { mutableStateOf(false) }

    LaunchedEffect(song.uri, loadMetadata) {
        if (codec == null && loadMetadata) {
            delay(180)
            codec = AudioCodecLoader.load(context, song.uri)
        }
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
            .clip(RoundedCornerShape(15.dp))
            .background(if (active) Color.White.copy(alpha = 0.075f) else Color.Transparent)
            .clickable(onClick = onPlay)
            .padding(start = 8.dp, end = 4.dp, top = 7.dp, bottom = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        artwork(song, Modifier.size(52.dp).clip(RoundedCornerShape(12.dp)))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(song.title, color = libraryWhite, fontSize = 14.5.sp,
                lineHeight = 19.sp, fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (active) {
                    Icon(Icons.Rounded.GraphicEq, "Now playing", tint = libraryWhite,
                        modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(5.dp))
                }
                Text(song.artist.ifBlank { "Unknown artist" },
                    color = librarySecondary, fontSize = 11.5.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false))
                if (codec?.isHiRes == true) {
                    Spacer(Modifier.width(8.dp))
                    Text("HI-RES", color = librarySecondary, fontSize = 8.sp,
                        fontWeight = FontWeight.Bold, letterSpacing = 0.4.sp)
                }
            }
        }
        Spacer(Modifier.width(7.dp))
        Text(librarySongDuration(song.durationMs), color = librarySecondary, fontSize = 10.5.sp)
        Box {
            IconButton(onClick = { menuOpen = true }, modifier = Modifier.size(38.dp)) {
                Icon(Icons.Rounded.MoreHoriz, "More options for \${song.title}",
                    tint = librarySecondary, modifier = Modifier.size(21.dp))
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(text = { Text("Play next") }, onClick = {
                    menuOpen = false; onPlayNext()
                })
                if (song.album.isNotBlank()) {
                    DropdownMenuItem(text = { Text("View album") }, onClick = {
                        menuOpen = false; onAlbum()
                    })
                }
                if (song.artist.isNotBlank()) {
                    DropdownMenuItem(text = { Text("View artist") }, onClick = {
                        menuOpen = false; onArtist()
                    })
                }
                DropdownMenuItem(text = { Text("Share") }, onClick = {
                    menuOpen = false; onShare()
                })
            }
        }
    }
}

private fun librarySongDuration(durationMs: Long): String {
    val seconds = (durationMs / 1000L).coerceAtLeast(0L)
    return if (seconds >= 3600L) {
        "%d:%02d:%02d".format(seconds / 3600L, (seconds % 3600L) / 60L, seconds % 60L)
    } else {
        "%d:%02d".format(seconds / 60L, seconds % 60L)
    }
}
