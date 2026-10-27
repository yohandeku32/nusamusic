package com.yohandeku32.nusamusic.ui

import android.content.Context
import java.util.Locale
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.MoreHoriz
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import com.yohandeku32.nusamusic.R
import com.yohandeku32.nusamusic.data.AudioCodecLoader
import com.yohandeku32.nusamusic.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first

// The CD/Vinyl libraries stay unchanged. Immersive uses inexpensive static
// surfaces instead of live blur, per-row animations, shadows or scroll effects.
private val libraryBlack = Color(0xFF090A0D)
private val librarySurface = Color(0xFF1B1D22)
private val librarySecondary = Color(0xFF92949E)
private val libraryWhite = Color(0xFFF8F8FA)

private enum class ImmersiveSort(private val id: String, private val en: String) {
    TITLE("Judul A–Z", "Title A–Z"), TITLE_DESC("Judul Z–A", "Title Z–A"),
    ARTIST("Artis", "Artist"), ALBUM("Album", "Album"),
    NEWEST("Baru ditambahkan", "Recently added"), LONGEST("Durasi terpanjang", "Longest first");

    val label: String get() = libraryText(id, en)
}
private enum class FocusType { ARTIST, ALBUM }
private data class LibraryFocus(val type: FocusType, val name: String)

/**
 * Reopening the library should not re-sort thousands of tracks on the first
 * animated frame. Retain only the last base-order result; search and focused
 * views remain request-specific and are not cached.
 */
private object ImmersiveBaseOrderCache {
    private var source: List<Song>? = null
    private var order: ImmersiveSort? = null
    private var result: List<Song>? = null

    fun get(songs: List<Song>, sort: ImmersiveSort): List<Song>? =
        if (source === songs && order == sort) result else null

    fun put(songs: List<Song>, sort: ImmersiveSort, sorted: List<Song>) {
        source = songs
        order = sort
        result = sorted
    }
}

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
        ImmersiveSort.TITLE -> matches.sortedBy { it.title.lowercase(Locale.ROOT) }
        ImmersiveSort.TITLE_DESC -> matches.sortedByDescending { it.title.lowercase(Locale.ROOT) }
        ImmersiveSort.ARTIST -> matches.sortedWith(
            compareBy<Song> { it.artist.lowercase(Locale.ROOT) }.thenBy { it.title.lowercase(Locale.ROOT) })
        ImmersiveSort.ALBUM -> matches.sortedWith(
            compareBy<Song> { it.album.lowercase(Locale.ROOT) }.thenBy { it.title.lowercase(Locale.ROOT) })
        ImmersiveSort.NEWEST -> matches.sortedWith(
            compareByDescending<Song> { it.dateAddedMs }.thenBy { it.title.lowercase(Locale.ROOT) })
        ImmersiveSort.LONGEST -> matches.sortedByDescending { it.durationMs }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ImmersiveMusicLibrary(
    songs: List<Song>,
    currentSong: Song?,
    isPlaying: Boolean,
    isVisible: Boolean,
    lightMode: Boolean = false,
    hasMusicAccess: Boolean,
    isScanning: Boolean,
    emptyMessage: String,
    onReturnToPlayer: () -> Unit,
    onPlay: (Song, List<Song>) -> Unit,
    onPlayNext: (Song) -> Unit,
    onShare: (Song) -> Unit,
    onDeleteSong: (Song) -> Unit,
    onTogglePlay: () -> Unit,
    onScanMusic: () -> Unit,
    onRequestPermission: () -> Unit,
    // The third argument gates uncached cover IO until the portal settles;
    // the fourth spreads first-screen loads across frames instead of a burst.
    artwork: @Composable (Song, Modifier, Boolean, Int) -> Unit,
    bottomContentPadding: Dp = 24.dp,
    modifier: Modifier = Modifier,
    onChooseMusicFolder: (() -> Unit)? = null
) {
    val palette = immersiveColors(lightMode)
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
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
    var searchDialogOpen by remember { mutableStateOf(false) }
    var deleteCandidate by remember { mutableStateOf<Song?>(null) }
    val dismissKeyboard: () -> Unit = {
        keyboard?.hide()
        focusManager.clearFocus(force = true)
        searchDialogOpen = false
    }
    // The first Library frame is intentionally not "settled" yet.
    // Do not request keyboard hide / force-clear focus on that entrance
    // frame; that work can trigger an unnecessary Android UI traversal.
    var wasSettledVisible by remember { mutableStateOf(false) }
    LaunchedEffect(isVisible) {
        if (isVisible) {
            wasSettledVisible = true
        } else if (wasSettledVisible) {
            dismissKeyboard()
            wasSettledVisible = false
        }
    }
    var visibleSongs by remember(songs) {
        mutableStateOf(ImmersiveBaseOrderCache.get(songs, sort) ?: songs)
    }
    val currentlyVisible by rememberUpdatedState(isVisible)
    val listState = rememberLazyListState()
    val density = LocalDensity.current
    val imeInsets = WindowInsets.ime
    // Observe only visibility (not every pixel of the IME animation).
    val imeVisible by remember(imeInsets, density) {
        derivedStateOf { imeInsets.getBottom(density) > 0 }
    }
    val canReadMetadata by remember(listState, imeInsets, density) {
        derivedStateOf {
            !listState.isScrollInProgress && !searchDialogOpen &&
                imeInsets.getBottom(density) == 0
        }
    }
    // Single decoded painter reused by every row (the logo is an 89 KB PNG).
    val hiResPainter = painterResource(id = R.drawable.hi_res_audio_logo)

    // No all-tracks MediaExtractor sweep when Library opens. Native decoding
    // thousands of files competes with drawing the first frames and the live
    // blur. Each idle, visible row below requests only its own badge.
    // Verified Hi-Res badges stay cached by AudioCodecLoader.

    // Debounce text locally in the input composable; sort only settled queries
    // off-main to avoid competing with system keyboard animation.
    LaunchedEffect(songs, search, focus, sort) {
        val result = withContext(Dispatchers.Default) {
            findImmersiveSongs(songs, search, focus, sort)
        }
        if (search.isBlank() && focus == null) {
            // Background sort once; the next Library open gets the prepared
            // order immediately instead of a second sort and list reset.
            ImmersiveBaseOrderCache.put(songs, sort, result)
        }
        // A mid-transition list replacement causes its LazyColumn to
        // remeasure and fights the portal animation. Publish once settled.
        snapshotFlow { currentlyVisible }.first { it }
        visibleSongs = result
        if (listState.firstVisibleItemIndex != 0 || listState.firstVisibleItemScrollOffset != 0) {
            listState.scrollToItem(0)
        }
    }
    BackHandler(enabled = focus != null) {
        focus = null
        search = ""
    }

    // New window: only text rendering and search filtering are active while
    // Gboard animates. Existing player, blur, album art and metadata jobs are
    // not recomposed by IME input.
    if (searchDialogOpen && isVisible) {
        ImmersiveSearchDialog(
            songs = songs,
            initialQuery = search,
            lightMode = lightMode,
            onDismiss = { query ->
                searchDialogOpen = false
                search = query
            },
            onPlay = onPlay
        )
    }

    MaterialTheme(
        colorScheme = if (lightMode) lightColorScheme(
            background = palette.background,
            surface = palette.surface,
            surfaceVariant = palette.surfaceVariant,
            surfaceContainer = palette.surface,
            surfaceContainerHigh = palette.surface,
            surfaceTint = palette.surface,
            onBackground = palette.primaryText,
            onSurface = palette.primaryText,
            onSurfaceVariant = palette.secondaryText,
            primary = palette.primaryText,
            onPrimary = palette.background,
            error = palette.destructiveText
        ) else darkColorScheme(
            background = palette.background,
            surface = palette.surface,
            surfaceVariant = palette.surfaceVariant,
            surfaceContainer = palette.surface,
            surfaceContainerHigh = palette.surface,
            surfaceTint = palette.surface,
            onBackground = palette.primaryText,
            onSurface = palette.primaryText,
            onSurfaceVariant = palette.secondaryText,
            primary = palette.primaryText,
            onPrimary = palette.background,
            error = palette.destructiveText
        )
    ) {
    deleteCandidate?.let { candidate ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { deleteCandidate = null },
            title = { Text(libraryText("Hapus lagu secara permanen?", "Delete song permanently?")) },
            text = {
                Text(libraryText(
                    "“${candidate.title}” akan dihapus dari penyimpanan perangkat, bukan hanya dari Aruna. Tindakan ini tidak dapat dibatalkan.",
                    "“${candidate.title}” will be deleted from device storage, not just removed from Aruna. This cannot be undone."
                ))
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    deleteCandidate = null
                    onDeleteSong(candidate)
                }) { Text(libraryText("Hapus permanen", "Delete permanently"), color = palette.destructiveText) }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { deleteCandidate = null }) {
                    Text(libraryText("Batal", "Cancel"))
                }
            }
        )
    }
    Box(
        modifier = modifier.fillMaxSize().background(palette.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)

    ) {
        // Fixed layout height: Android IME overlays the lower song rows,
        // rather than forcing the entire animated player/pager to relayout
        // on every keyboard-animation frame. Search stays near the top.
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().height(58.dp).padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    if (focus != null) { focus = null; search = "" }
                    else onReturnToPlayer()
                }) {
                    Icon(Icons.Rounded.ArrowBack, libraryText("Kembali", "Back"), tint = palette.primaryText)
                }
                Text(
                    libraryText("Perpustakaan", "Library"), color = palette.primaryText, fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(start = 8.dp)
                )
                IconButton(onClick = onScanMusic, enabled = !isScanning) {
                    Icon(Icons.Rounded.Refresh, libraryText("Perbarui koleksi", "Refresh library"), tint = palette.primaryText)
                }
                Box {
                    IconButton(onClick = { sortOpen = true }) {
                        Icon(Icons.Rounded.Sort, libraryText("Urutkan lagu", "Sort songs"), tint = palette.primaryText)
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
                        focus?.name ?: libraryText("Lagu", "Songs"), color = palette.primaryText,
                        fontSize = 29.sp, fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-0.8).sp, lineHeight = 36.sp,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(5.dp))
                    Text(
                        when (focus?.type) {
                            FocusType.ALBUM -> libraryText("Album", "ALBUM")
                            FocusType.ARTIST -> libraryText("Artis", "ARTIST")
                            null -> libraryText("Di perangkat ini", "On this device")
                        },
                        color = palette.secondaryText, fontSize = 12.sp
                    )
                }
                if (visibleSongs.isNotEmpty()) {
                    Button(
                        onClick = { onPlay(visibleSongs.first(), visibleSongs) },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = palette.primaryText, contentColor = palette.background),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                        modifier = Modifier.height(40.dp)
                    ) {
                        Icon(Icons.Rounded.PlayArrow, null, modifier = Modifier.size(19.dp))
                        Spacer(Modifier.width(3.dp))
                        Text(libraryText("Putar semua", "Play all"), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // A lightweight, separate dialog window handles Gboard.
            // The heavy Library pager and all artwork stay at a fixed size.
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp)
                    .height(48.dp).clip(RoundedCornerShape(15.dp))
                    .background(palette.surface)
                    .clickable { searchDialogOpen = true }
                    .padding(horizontal = 15.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.Search, null, tint = palette.secondaryText,
                    modifier = Modifier.size(19.dp))
                Spacer(Modifier.width(10.dp))
                Text(
                    search.ifBlank { libraryText("Cari lagu, artis, album", "Search songs, artists, albums") },
                    color = if (search.isBlank()) palette.secondaryText else palette.primaryText,
                    fontSize = 13.sp, maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (search.isNotBlank()) {
                    IconButton(onClick = { search = "" }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Rounded.Close, libraryText("Hapus pencarian", "Clear search"), tint = palette.secondaryText,
                            modifier = Modifier.size(17.dp))
                    }
                }
            }
            Row(
                Modifier.fillMaxWidth().padding(start = 24.dp, end = 22.dp, top = 19.dp, bottom = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    librarySongCount(visibleSongs.size),
                    color = palette.secondaryText, fontSize = 12.sp, fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                Text(sort.label, color = palette.secondaryText, fontSize = 11.sp)
            }

            PullToRefreshBox(
                isRefreshing = isScanning, onRefresh = onScanMusic,
                modifier = Modifier.weight(1f)
            ) {
                if (!hasMusicAccess || (songs.isEmpty() && !isScanning)) {
                    Column(
                        Modifier.fillMaxSize().padding(bottom = bottomContentPadding.coerceAtLeast(24.dp))
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 32.dp, vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            if (!hasMusicAccess) libraryText("Izinkan akses ke musik Anda", "Allow access to your music") else emptyMessage,
                            color = palette.primaryText, fontWeight = FontWeight.SemiBold,
                            fontSize = 17.sp, textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(14.dp))
                        if (onChooseMusicFolder != null) {
                            MusicImportActions(onChooseMusicFolder, onRequestPermission)
                            if (hasMusicAccess) androidx.compose.material3.TextButton(onClick = onScanMusic) {
                                Text(libraryText("Pindai lagi", "Scan again"))
                            }
                        } else {
                            OutlinedButton(onClick = if (!hasMusicAccess) onRequestPermission else onScanMusic) {
                                Text(if (!hasMusicAccess) libraryText("Izinkan akses", "Allow access")
                                    else libraryText("Pindai lagi", "Scan again"), color = palette.primaryText)
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        state = listState, modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 12.dp, end = 12.dp, top = 2.dp,
                            bottom = bottomContentPadding.coerceAtLeast(24.dp))
                    ) {
                        itemsIndexed(
                            items = visibleSongs,
                            key = { _, song -> song.id },
                            contentType = { _, _ -> "immersive-track" }
                        ) { index, song ->
                            ImmersiveTrackRow(
                                song = song, active = currentSong?.id == song.id,
                                lightMode = lightMode,
                                // The native extractor must not compete with
                                // the first Library animation frames.
                                loadMetadata = isVisible && canReadMetadata && !searchDialogOpen,
                                hiResPainter = hiResPainter,
                                artwork = { track, imageModifier ->
                                    artwork(track, imageModifier, isVisible, index)
                                },
                                onPlay = { onPlay(song, visibleSongs) },
                                onPlayNext = { onPlayNext(song) },
                                onShare = { onShare(song) },
                                onDelete = { deleteCandidate = song },
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
                                Text(libraryText("Tidak ada lagu yang cocok", "No matching songs"), color = palette.secondaryText,
                                    textAlign = TextAlign.Center, fontSize = 14.sp,
                                    modifier = Modifier.fillMaxWidth().padding(top = 70.dp))
                            }
                        }
                    }
                }
            }
        }


    }
    } // Immersive-only Material theme
}


@Composable
private fun ImmersiveTrackRow(
    song: Song, active: Boolean, loadMetadata: Boolean,
    lightMode: Boolean,
    hiResPainter: Painter,
    artwork: @Composable (Song, Modifier) -> Unit,
    onPlay: () -> Unit, onPlayNext: () -> Unit, onShare: () -> Unit,
    onDelete: () -> Unit,
    onAlbum: () -> Unit, onArtist: () -> Unit
) {
    val palette = immersiveColors(lightMode)
    val context = LocalContext.current
    // This per-song snapshot lookup makes the badge appear as soon as native
    // metadata finishes, even if the row stays visible throughout a fling.
    val isHiRes = AudioCodecLoader.isHiResCached(song.uri)
    var menuOpen by remember { mutableStateOf(false) }

    LaunchedEffect(song.uri, loadMetadata) {
        if (loadMetadata && !isHiRes) {
            // Once scheduled, the app-scoped metadata lookup outlives this
            // recycled row. Revisiting the song never repeats native reads.
            AudioCodecLoader.requestHiResBadgeOnce(context, song.uri)
        }
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
            .clip(RoundedCornerShape(15.dp))
            .background(if (active) palette.activeRow else Color.Transparent)
            .clickable(onClick = onPlay)
            .padding(start = 8.dp, end = 4.dp, top = 7.dp, bottom = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        artwork(song, Modifier.size(52.dp).clip(RoundedCornerShape(12.dp)))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(song.title, color = palette.primaryText, fontSize = 14.5.sp,
                lineHeight = 19.sp, fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (active) {
                    Icon(Icons.Rounded.GraphicEq, libraryText("Lagu saat ini", "Now playing"), tint = palette.primaryText,
                        modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(5.dp))
                }
                Text(song.artist.ifBlank { libraryText("Artis tidak diketahui", "Unknown artist") },
                    color = palette.secondaryText, fontSize = 11.5.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false))
                if (isHiRes) {
                    Spacer(Modifier.width(8.dp))
                    Image(
                        painter = hiResPainter,
                        contentDescription = "Hi-Res Audio",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
        Spacer(Modifier.width(7.dp))
        Text(librarySongDuration(song.durationMs), color = palette.secondaryText, fontSize = 10.5.sp)
        Box {
            IconButton(onClick = { menuOpen = true }, modifier = Modifier.size(38.dp)) {
                Icon(Icons.Rounded.MoreHoriz, libraryText("Opsi lainnya untuk ${song.title}", "More options for ${song.title}"),
                    tint = palette.secondaryText, modifier = Modifier.size(21.dp))
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(text = { Text(libraryText("Putar berikutnya", "Play next")) }, onClick = {
                    menuOpen = false; onPlayNext()
                })
                if (song.album.isNotBlank()) {
                    DropdownMenuItem(text = { Text(libraryText("Lihat album", "View album")) }, onClick = {
                        menuOpen = false; onAlbum()
                    })
                }
                if (song.artist.isNotBlank()) {
                    DropdownMenuItem(text = { Text(libraryText("Lihat artis", "View artist")) }, onClick = {
                        menuOpen = false; onArtist()
                    })
                }
                DropdownMenuItem(text = { Text(libraryText("Bagikan", "Share")) }, onClick = {
                    menuOpen = false; onShare()
                })
                androidx.compose.material3.HorizontalDivider()
                DropdownMenuItem(
                    text = { Text(libraryText("Hapus permanen", "Delete permanently"), color = palette.destructiveText) },
                    onClick = { menuOpen = false; onDelete() }
                )
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
