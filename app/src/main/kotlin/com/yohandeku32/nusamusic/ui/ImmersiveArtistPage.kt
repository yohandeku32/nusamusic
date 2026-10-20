package com.yohandeku32.nusamusic.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yohandeku32.nusamusic.data.ArtistLibrary
import com.yohandeku32.nusamusic.data.ArtistNameUtils
import com.yohandeku32.nusamusic.data.PlaybackHistory
import com.yohandeku32.nusamusic.model.Song
import java.util.Locale
import kotlinx.coroutines.launch

private val artistBase = Color(0xFF090A0D)
private val artistSurface = Color(0xFF17191F)
private val artistInk = Color(0xFFF7F8FA)
private val artistMuted = Color(0xFF999EAA)

private fun artistText(id: String, en: String): String =
    if (Locale.getDefault().language == "id") id else en

/**
 * Apple Music-inspired artist profile using only the local user's songs.
 * It never downloads Deezer portraits or waits for Last.fm biographies.
 * Local album artwork is deliberately not labeled an official artist portrait.
 */
@Composable
fun ImmersiveArtistPage(
    artistName: String?,
    songs: List<Song>,
    currentSong: Song?,
    isVisible: Boolean,
    state: ImmersiveArtistPageState,
    onBack: () -> Unit,
    onPlay: (Song, List<Song>) -> Unit,
    artwork: @Composable (Song, Modifier) -> Unit
) {
    val artist = ArtistNameUtils.firstArtist(artistName)
    val displayName = artist.ifBlank { artistText("Artis", "Artist") }
    val tracks = remember(songs, artist) { ArtistLibrary.tracks(songs, artist) }
    val albums = remember(tracks) { ArtistLibrary.albums(tracks) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember(context) { PlaybackHistory.preferences(context) }
    var counts by remember(prefs) { mutableStateOf(PlaybackHistory.counts(prefs)) }
    DisposableEffect(prefs) {
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
            counts = PlaybackHistory.counts(prefs)
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    val played = remember(tracks, counts) { ArtistLibrary.mostPlayed(tracks, counts) }
    val popular = remember(tracks, played) {
        (played + tracks.filterNot { s -> played.any { it.uri == s.uri } }).take(5)
    }
    var selectedAlbumKey by state::selectedAlbumKey
    val selectedAlbum = albums.firstOrNull { it.key == selectedAlbumKey }
    val listState = state.listState
    val scope = rememberCoroutineScope()
    val heroSong = selectedAlbum?.tracks?.firstOrNull()
        ?: currentSong?.takeIf {
            ArtistNameUtils.cacheKey(it.artist) == ArtistNameUtils.cacheKey(artist)
        } ?: popular.firstOrNull() ?: tracks.firstOrNull()

    fun showAlbum(key: String?) {
        selectedAlbumKey = key
        scope.launch { listState.scrollToItem(0) }
    }
    fun navigateBack() {
        if (selectedAlbumKey != null) showAlbum(null) else onBack()
    }
    BackHandler(enabled = isVisible) { navigateBack() }

    // The library Light switch must not recolor the main player or Artist.
    MaterialTheme(
        colorScheme = darkColorScheme(
            background = artistBase, surface = artistSurface,
            surfaceVariant = Color(0xFF22242B), primary = artistInk,
            onPrimary = artistBase, onBackground = artistInk,
            onSurface = artistInk, onSurfaceVariant = artistMuted
        )
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().background(artistBase),
            contentPadding = PaddingValues(bottom = 54.dp)
        ) {
            item(key = "artist-hero") {
                Box(
                    Modifier.fillMaxWidth().height(450.dp)
                        .background(artistSurface)
                ) {
                    if (heroSong != null) {
                        artwork(heroSong, Modifier.fillMaxSize())
                    } else {
                        Box(
                            Modifier.fillMaxSize().background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF343C4C), Color(0xFF151720), artistBase)
                                )
                            ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.MusicNote, null,
                                tint = Color.White.copy(alpha = 0.12f),
                                modifier = Modifier.size(130.dp)
                            )
                        }
                    }
                    Box(
                        Modifier.matchParentSize().background(
                            Brush.verticalGradient(
                                0f to Color.Black.copy(alpha = 0.22f),
                                0.30f to Color.Transparent,
                                0.59f to Color.Black.copy(alpha = 0.28f),
                                0.86f to artistBase.copy(alpha = 0.86f),
                                1f to artistBase
                            )
                        )
                    )
                    IconButton(
                        onClick = { navigateBack() },
                        modifier = Modifier.statusBarsPadding()
                            .padding(start = 16.dp, top = 10.dp)
                            .size(46.dp).immersiveGlassPill(100.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = artistText("Kembali", "Back"),
                            tint = Color.White
                        )
                    }
                    Column(
                        Modifier.align(Alignment.BottomStart).fillMaxWidth()
                            .padding(horizontal = 24.dp).padding(bottom = 22.dp)
                    ) {
                        Text(
                            if (selectedAlbum == null)
                                artistText("ARTIS • KOLEKSI LOKAL", "ARTIST • YOUR LIBRARY")
                            else artistText("ALBUM • KOLEKSI LOKAL", "ALBUM • YOUR LIBRARY"),
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 11.sp, letterSpacing = 1.4.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            selectedAlbum?.title?.ifBlank {
                                artistText("Album tanpa judul", "Untitled album")
                            } ?: displayName,
                            color = artistInk,
                            fontSize = if (selectedAlbum == null) 42.sp else 37.sp,
                            lineHeight = if (selectedAlbum == null) 46.sp else 41.sp,
                            fontWeight = FontWeight.Bold, maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            if (selectedAlbum == null)
                                artistText(
                                    tracks.size.toString() + " lagu  ·  " + albums.size + " album",
                                    tracks.size.toString() + " songs  ·  " + albums.size + " albums"
                                )
                            else artistText(
                                displayName + "  ·  " + selectedAlbum.tracks.size + " lagu",
                                displayName + "  ·  " + selectedAlbum.tracks.size + " songs"
                            ),
                            color = Color.White.copy(alpha = 0.73f),
                            fontSize = 13.sp, fontWeight = FontWeight.Medium,
                            maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            if (tracks.isNotEmpty()) {
                item(key = "artist-actions") {
                    val playable = selectedAlbum?.tracks ?: tracks
                    Row(
                        Modifier.fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .padding(top = 8.dp, bottom = 26.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ArtistActionButton(
                            label = artistText("Putar", "Play"),
                            icon = Icons.Default.PlayArrow,
                            primary = true,
                            modifier = Modifier.weight(1f),
                            onClick = { onPlay(playable.first(), playable) }
                        )
                        ArtistActionButton(
                            label = artistText("Acak", "Shuffle"),
                            icon = Icons.Default.Shuffle,
                            primary = false,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                val shuffled = playable.shuffled()
                                if (shuffled.isNotEmpty()) onPlay(shuffled.first(), shuffled)
                            }
                        )
                    }
                }
            }

            if (selectedAlbum != null) {
                item(key = "album-title") {
                    ArtistSectionHeader(
                        artistText("Daftar Lagu", "Tracklist"),
                        artistText(
                            selectedAlbum.tracks.size.toString() + " lagu",
                            selectedAlbum.tracks.size.toString() + " tracks"
                        )
                    )
                }
                items(selectedAlbum.tracks, key = { "track:" + it.uri }) { song ->
                    ArtistMusicRow(song, currentSong, song.album, artwork) {
                        onPlay(song, selectedAlbum.tracks)
                    }
                }
            } else {
                if (popular.isNotEmpty()) {
                    item(key = "popular-heading") {
                        ArtistSectionHeader(
                            artistText("Lagu Populer", "Top Songs"),
                            if (played.isNotEmpty())
                                artistText("Sering kamu putar", "From your listening history")
                            else artistText("Dari koleksimu", "From your library")
                        )
                    }
                    items(popular, key = { "popular:" + it.uri }) { song ->
                        ArtistMusicRow(song, currentSong, song.album, artwork) {
                            onPlay(song, tracks)
                        }
                    }
                }

                if (albums.isNotEmpty()) {
                    item(key = "albums-title") {
                        Spacer(Modifier.height(18.dp))
                        ArtistSectionHeader(
                            artistText("Album", "Albums"),
                            artistText("Di perpustakaanmu", "In your library")
                        )
                    }
                    items(albums.chunked(2), key = { "albums:" + it.first().key }) { row ->
                        Row(
                            Modifier.fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(15.dp)
                        ) {
                            row.forEach { album ->
                                Column(
                                    Modifier.weight(1f)
                                        .clickable { showAlbum(album.key) }
                                        .padding(bottom = 8.dp)
                                ) {
                                    Box(
                                        Modifier.fillMaxWidth().aspectRatio(1f)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(artistSurface)
                                    ) {
                                        artwork(album.tracks.first(), Modifier.fillMaxSize())
                                    }
                                    Spacer(Modifier.height(10.dp))
                                    Text(
                                        album.title.ifBlank {
                                            artistText("Album tanpa judul", "Untitled album")
                                        },
                                        color = artistInk, fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp, maxLines = 2,
                                        lineHeight = 18.sp, overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(Modifier.height(3.dp))
                                    Text(
                                        artistText(
                                            album.tracks.size.toString() + " lagu",
                                            album.tracks.size.toString() + " tracks"
                                        ),
                                        color = artistMuted, fontSize = 12.sp
                                    )
                                }
                            }
                            if (row.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }

                if (tracks.isNotEmpty()) {
                    item(key = "footer") {
                        Column(
                            Modifier.fillMaxWidth()
                                .padding(horizontal = 24.dp)
                                .padding(top = 20.dp, bottom = 24.dp)
                        ) {
                            HorizontalDivider(
                                color = Color.White.copy(alpha = 0.08f),
                                thickness = 1.dp
                            )
                            Spacer(Modifier.height(18.dp))
                            Text(
                                artistText(
                                    "Musik dari koleksi perangkatmu",
                                    "Music from your device library"
                                ),
                                color = artistMuted, fontSize = 12.sp
                            )
                        }
                    }
                }
            }
            if (tracks.isEmpty()) {
                item(key = "empty") {
                    Column(
                        Modifier.fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 26.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Album, null,
                            modifier = Modifier.size(35.dp), tint = artistMuted
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            artistText(
                                "Lagu artis ini belum ada di koleksimu.",
                                "This artist has no songs in your library."
                            ),
                            color = artistMuted, fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ArtistActionButton(
    label: String,
    icon: ImageVector,
    primary: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(50.dp),
        shape = RoundedCornerShape(17.dp),
        color = if (primary) artistInk else Color.White.copy(alpha = 0.09f),
        border = if (primary) null else androidx.compose.foundation.BorderStroke(
            1.dp, Color.White.copy(alpha = 0.10f)
        )
    ) {
        Row(
            Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon, null,
                tint = if (primary) artistBase else artistInk,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(7.dp))
            Text(
                label,
                color = if (primary) artistBase else artistInk,
                fontWeight = FontWeight.SemiBold, fontSize = 15.sp
            )
        }
    }
}

@Composable
private fun ArtistSectionHeader(title: String, subtitle: String) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 13.dp)
    ) {
        Text(
            title, fontWeight = FontWeight.Bold, color = artistInk,
            fontSize = 23.sp, letterSpacing = (-0.4).sp
        )
        Spacer(Modifier.height(3.dp))
        Text(subtitle, fontSize = 12.sp, color = artistMuted)
    }
}

@Composable
private fun ArtistMusicRow(
    song: Song,
    currentSong: Song?,
    detail: String,
    artwork: @Composable (Song, Modifier) -> Unit,
    onClick: () -> Unit
) {
    val selected = song.uri == currentSong?.uri
    Row(
        Modifier.fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (selected) Color.White.copy(alpha = 0.065f) else Color.Transparent
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(54.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(artistSurface)
        ) { artwork(song, Modifier.fillMaxSize()) }
        Column(
            Modifier.weight(1f).padding(start = 13.dp, end = 8.dp)
        ) {
            Text(
                song.title,
                color = artistInk,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                fontSize = 14.5.sp, maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Text(
                detail.ifBlank { artistText("Lagu lokal", "Local track") },
                color = artistMuted, fontSize = 12.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
        Icon(
            if (selected) Icons.Default.MusicNote else Icons.Default.PlayArrow,
            contentDescription = artistText("Putar lagu", "Play song"),
            tint = if (selected) artistInk else artistMuted,
            modifier = Modifier.size(21.dp)
        )
        Spacer(Modifier.width(7.dp))
    }
}
