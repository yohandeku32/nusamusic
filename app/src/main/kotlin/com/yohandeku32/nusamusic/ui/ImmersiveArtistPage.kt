package com.yohandeku32.nusamusic.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.yohandeku32.nusamusic.data.*
import com.yohandeku32.nusamusic.model.Song
import java.util.Locale

private fun artistText(id: String, en: String) = if (Locale.getDefault().language == "id") id else en

@Composable
fun ImmersiveArtistPage(
    artistName: String?,
    songs: List<Song>,
    currentSong: Song?,
    biography: ArtistBiography?,
    biographyLoading: Boolean,
    isVisible: Boolean,
    state: ImmersiveArtistPageState,
    onVisible: () -> Unit,
    onBack: () -> Unit,
    onPlay: (Song, List<Song>) -> Unit,
    artwork: @Composable (Song, Modifier) -> Unit
) {
    val context = LocalContext.current
    val artist = ArtistNameUtils.firstArtist(artistName)
    val tracks = remember(songs, artist) { ArtistLibrary.tracks(songs, artist) }
    val albums = remember(tracks) { ArtistLibrary.albums(tracks) }
    val prefs = remember(context) { PlaybackHistory.preferences(context) }
    var counts by remember(prefs) { mutableStateOf(PlaybackHistory.counts(prefs)) }
    DisposableEffect(prefs) {
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
            counts = PlaybackHistory.counts(prefs)
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    val popular = remember(tracks, counts) { ArtistLibrary.mostPlayed(tracks, counts) }
    val portrait = state.portrait
    val portraitLoading = state.portraitLoading
    LaunchedEffect(state, isVisible) {
        if (isVisible) onVisible()
    }
    var selectedAlbumKey by state::selectedAlbumKey
    var bioExpanded by state::bioExpanded
    val selectedAlbum = albums.firstOrNull { it.key == selectedAlbumKey }
    val listState = state.listState
    val scope = rememberCoroutineScope()
    fun selectAlbum(key: String?) {
        selectedAlbumKey = key
        scope.launch { listState.scrollToItem(0) }
    }
    fun back() { if (selectedAlbumKey != null) selectAlbum(null) else onBack() }
    BackHandler(enabled = isVisible) { back() }

    MaterialTheme(colorScheme = darkColorScheme(background = Color.Black, surface = Color(0xFF151515), primary = Color.White)) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().background(Color.Black),
            contentPadding = PaddingValues(bottom = 40.dp)
        ) {
            item(key = "hero") {
                Box(Modifier.fillMaxWidth().height(410.dp)) {
                    if (selectedAlbum != null) {
                        artwork(selectedAlbum.tracks.first(), Modifier.fillMaxSize())
                    } else if (portrait != null) {
                        Image(
                            bitmap = portrait!!.asImageBitmap(),
                            contentDescription = artistText("Foto $artist dari Deezer", "$artist portrait from Deezer"),
                            contentScale = ContentScale.Crop,
                            colorFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }),
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF555555), Color(0xFF151515)))), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Person, null, tint = Color.White.copy(alpha = 0.2f), modifier = Modifier.size(160.dp))
                        }
                    }
                    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.18f), 0.4f to Color.Transparent,
                        0.7f to Color.Black.copy(alpha = 0.4f), 1f to Color.Black
                    )))
                    IconButton(
                        onClick = { back() },
                        modifier = Modifier.padding(16.dp).statusBarsPadding().clip(CircleShape).background(Color.Black.copy(alpha = 0.35f))
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, artistText("Kembali", "Back"), tint = Color.White)
                    }
                    Column(Modifier.align(Alignment.BottomStart).padding(horizontal = 24.dp, vertical = 16.dp)) {
                        Text(if (selectedAlbum == null) artistText("ARTIS • KOLEKSI ANDA", "ARTIST • YOUR LIBRARY") else artist,
                            color = Color(0xFFCCCCCC), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.5.sp)
                        Spacer(Modifier.height(10.dp))
                        Text(selectedAlbum?.title?.ifBlank { artistText("Album tanpa judul", "Untitled album") }
                            ?: artist.ifBlank { artistText("Belum ada artis", "No artist selected") },
                            color = Color.White, fontSize = 38.sp, lineHeight = 42.sp, fontWeight = FontWeight.Bold,
                            maxLines = 3, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.height(10.dp))
                        Text(if (selectedAlbum != null) artistText("${selectedAlbum.tracks.size} lagu lokal", "${selectedAlbum.tracks.size} local tracks")
                            else artistText("${tracks.size} lagu • ${albums.size} album lokal", "${tracks.size} tracks • ${albums.size} local albums"),
                            color = Color(0xFFAAAAAA), fontSize = 13.sp)
                    }
                }
            }
            if (selectedAlbum != null) {
                items(selectedAlbum.tracks, key = { it.uri }) { song ->
                    ArtistTrackRow(song, currentSong, null, { onPlay(song, selectedAlbum.tracks) }, artwork)
                }
            } else {
                if (tracks.isNotEmpty()) {
                    item(key = "play") {
                        Button(
                            onClick = { onPlay(tracks.first(), tracks) },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp).heightIn(min = 48.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, null)
                            Spacer(Modifier.width(6.dp))
                            Text(artistText("Putar koleksi artis", "Play artist library"), fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                item(key = "about") {
                    Column(Modifier.padding(24.dp)) {
                        ArtistSectionTitle(artistText("Tentang artis", "About the artist"))
                        Spacer(Modifier.height(12.dp))
                        Text(
                            when {
                                biographyLoading -> artistText("Memuat biografi…", "Loading biography…")
                                biography != null -> biography.text
                                else -> artistText("Biografi belum tersedia untuk artis ini.", "A biography is not available for this artist yet.")
                            },
                            color = Color(0xFFBBBBBB), fontSize = 14.sp, lineHeight = 22.sp,
                            maxLines = if (bioExpanded) Int.MAX_VALUE else 4, overflow = TextOverflow.Ellipsis
                        )
                        if (biography != null) {
                            TextButton(onClick = { bioExpanded = !bioExpanded }, contentPadding = PaddingValues(0.dp)) {
                                Text(if (bioExpanded) artistText("Lebih sedikit", "Show less") else artistText("Selengkapnya", "Read more"))
                            }
                            // Attribution is intentionally not a tap target.
                            // A dragged/partially visible neighboring Artist
                            // page must never launch Chrome over Now Playing.
                            Text(
                                "Source: Last.fm • ${biography.sourceLanguage.uppercase(Locale.ROOT)}",
                                color = Color(0xFF999999),
                                fontSize = 11.sp,
                                modifier = Modifier.padding(top = 7.dp, bottom = 10.dp)
                            )
                        }
                        if (portrait != null) Text(artistText("Foto artis: Deezer", "Artist portrait: Deezer"), color = Color(0xFF777777), fontSize = 11.sp)
                        else if (portraitLoading) Text(artistText("Memuat foto artis…", "Loading artist portrait…"), color = Color(0xFF777777), fontSize = 11.sp)
                    }
                }
                item(key = "popular-title") {
                    Column(Modifier.padding(horizontal = 24.dp)) {
                        ArtistSectionTitle(artistText("Sering diputar", "Most played"))
                        Text(artistText("Dari riwayat putar Anda", "From your listening history"), color = Color(0xFF888888), fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp, bottom = 12.dp))
                        if (popular.isEmpty()) Text(artistText("Lagu yang sering Anda dengarkan akan muncul di sini.", "Your most listened-to tracks will appear here."),
                            color = Color(0xFFAAAAAA), fontSize = 14.sp, modifier = Modifier.padding(bottom = 16.dp))
                    }
                }
                items(popular, key = { "popular:${it.uri}" }) { song ->
                    ArtistTrackRow(song, currentSong, counts[song.uri], { onPlay(song, popular) }, artwork)
                }
                item(key = "albums-title") {
                    Text(artistText("Album di koleksi Anda", "Albums in your library"), color = Color.White,
                        fontWeight = FontWeight.Bold, fontSize = 22.sp, modifier = Modifier.padding(24.dp))
                }
                items(albums.chunked(2), key = { "albums:${it.first().key}" }) { row ->
                    Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        row.forEach { album ->
                            Column(Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).clickable { selectAlbum(album.key) }.padding(bottom = 8.dp)) {
                                artwork(album.tracks.first(), Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(10.dp)))
                                Text(album.title.ifBlank { artistText("Album tanpa judul", "Untitled album") }, color = Color.White,
                                    fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(top = 10.dp))
                                Text(artistText("${album.tracks.size} lagu", "${album.tracks.size} tracks"), color = Color(0xFF888888), fontSize = 12.sp)
                            }
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
                if (tracks.isEmpty()) item(key = "empty") {
                    Text(artistText("Putar lagu dari koleksi lokal untuk melihat halaman artisnya.", "Play a song from your local library to see its artist page."),
                        color = Color(0xFFAAAAAA), fontSize = 14.sp, modifier = Modifier.padding(horizontal = 24.dp))
                }
            }
        }
    }
}

@Composable
private fun ArtistSectionTitle(title: String) {
    Text(title, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
}

@Composable
private fun ArtistTrackRow(song: Song, currentSong: Song?, count: Long?, onClick: () -> Unit, artwork: @Composable (Song, Modifier) -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 24.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
        artwork(song, Modifier.size(48.dp).clip(RoundedCornerShape(7.dp)))
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(song.title, color = Color.White, fontSize = 15.sp, fontWeight = if (song.uri == currentSong?.uri) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(count?.let { artistText("$it kali diputar", "$it plays") } ?: song.album, color = Color(0xFF888888), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Icon(if (song.uri == currentSong?.uri) Icons.Default.MusicNote else Icons.Default.PlayArrow,
            contentDescription = artistText("Putar ${song.title}", "Play ${song.title}"), tint = Color(0xFFCCCCCC), modifier = Modifier.size(20.dp))
    }
}
