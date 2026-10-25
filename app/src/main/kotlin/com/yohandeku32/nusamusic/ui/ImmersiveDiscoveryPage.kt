package com.yohandeku32.nusamusic.ui

import android.content.SharedPreferences
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yohandeku32.nusamusic.data.AppleArtistPortraitLoader
import com.yohandeku32.nusamusic.data.DiscoveryAlbum
import com.yohandeku32.nusamusic.data.DiscoveryArtist
import com.yohandeku32.nusamusic.data.DiscoveryContent
import com.yohandeku32.nusamusic.data.DiscoveryLibrary
import com.yohandeku32.nusamusic.data.PlaybackHistory
import com.yohandeku32.nusamusic.model.Song
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val discoveryYellow = Color(0xFFF2D535)
private val discoveryMuted = Color(0xFFADADAD)
private fun discoveryText(id: String, en: String) =
    if (Locale.getDefault().language == "id") id else en

@Composable
internal fun ImmersiveDiscoveryPage(
    songs: List<Song>, currentSong: Song?, isPlaying: Boolean,
    listState: LazyListState, isVisible: Boolean, onBack: () -> Unit,
    onArtist: (String) -> Unit, onAlbum: (DiscoveryAlbum) -> Unit,
    onPlay: (Song, List<Song>) -> Unit, onTogglePlay: () -> Unit,
    onLibrary: () -> Unit, artwork: @Composable (Song, Modifier) -> Unit
) {
    val context = LocalContext.current
    val countsPrefs = remember(context) { PlaybackHistory.preferences(context) }
    val recentPrefs = remember(context) { PlaybackHistory.recentPreferences(context) }
    var revision by remember { mutableIntStateOf(0) }
    DisposableEffect(countsPrefs, recentPrefs) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> revision++ }
        countsPrefs.registerOnSharedPreferenceChangeListener(listener)
        recentPrefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            countsPrefs.unregisterOnSharedPreferenceChangeListener(listener)
            recentPrefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }
    val content by produceState<DiscoveryContent?>(null, songs, revision) {
        value = withContext(Dispatchers.Default) {
            DiscoveryLibrary.build(songs, PlaybackHistory.counts(countsPrefs),
                PlaybackHistory.counts(recentPrefs))
        }
    }
    BackHandler(enabled = isVisible, onBack = onBack)
    Box(Modifier.fillMaxSize().background(Color(0xFF101010))) {
        val data = content
        if (data == null) {
            CircularProgressIndicator(Modifier.align(Alignment.Center).size(24.dp), color = discoveryYellow)
        } else {
            DiscoveryFeed(data, songs, currentSong, isPlaying, listState,
                onArtist, onAlbum, onPlay, onTogglePlay, onLibrary, artwork)
        }
    }
}

@Composable
internal fun DiscoveryFeed(
    data: DiscoveryContent, songs: List<Song>, currentSong: Song?, isPlaying: Boolean,
    listState: LazyListState, onArtist: (String) -> Unit, onAlbum: (DiscoveryAlbum) -> Unit,
    onPlay: (Song, List<Song>) -> Unit, onTogglePlay: () -> Unit,
    onLibrary: () -> Unit, artwork: @Composable (Song, Modifier) -> Unit,
    artistArtwork: @Composable (DiscoveryArtist, Modifier) -> Unit = { artist, modifier ->
        DiscoveryArtistArtwork(artist, modifier, artwork)
    }
) {
    LazyColumn(
        state = listState, modifier = Modifier.fillMaxSize().testTag("discovery-feed"),
        contentPadding = PaddingValues(top = 24.dp, bottom = 156.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item(key = "discover-heading") {
            Text("Discover", Modifier.padding(horizontal = 22.dp), color = Color.White,
                fontSize = 30.sp, lineHeight = 36.sp, fontWeight = FontWeight.Bold)
        }
        if (songs.isEmpty()) item(key = "discover-empty") {
            Column(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 40.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(discoveryText("Belum ada lagu", "No songs yet"), color = Color.White,
                    fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                Text(discoveryText("Tambahkan musik melalui Library untuk mulai menjelajah.",
                    "Add music through Library to start exploring."), color = discoveryMuted,
                    fontSize = 15.sp, lineHeight = 22.sp)
                Text(discoveryText("Buka Library", "Open Library"), color = discoveryYellow,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.heightIn(min = 48.dp).clickable(role = Role.Button, onClick = onLibrary)
                        .padding(vertical = 14.dp))
            }
        }
        if (data.albums.isNotEmpty()) item(key = "discover-albums") {
            LazyRow(contentPadding = PaddingValues(horizontal = 22.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                items(data.albums, key = { it.key }) { album ->
                    Column(Modifier.width(154.dp).testTag("discovery-album:${album.key}").clip(RoundedCornerShape(5.dp))
                        .clickable(role = Role.Button, onClickLabel = discoveryText("Buka album", "Open album")) {
                            onAlbum(album)
                        }) {
                        artwork(album.tracks.first(), Modifier.fillMaxWidth().aspectRatio(1f)
                            .clip(RoundedCornerShape(5.dp)).background(Color(0xFF222222)))
                        Text(album.title, Modifier.padding(top = 9.dp), color = Color.White,
                            fontSize = 14.sp, fontWeight = FontWeight.Medium,
                            maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(album.artist, Modifier.padding(top = 3.dp), color = discoveryMuted,
                            fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
        if (data.artists.isNotEmpty()) item(key = "discover-artists") {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                DiscoverySectionTitle(if (data.hasListeningHistory)
                    discoveryText("Artis populer", "Popular artists")
                    else discoveryText("Artis di koleksimu", "Your artists"))
                LazyRow(contentPadding = PaddingValues(horizontal = 22.dp),
                    horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    items(data.artists, key = { it.key }) { artist ->
                        Column(Modifier.width(96.dp).testTag("discovery-artist:${artist.key}").clip(RoundedCornerShape(8.dp))
                            .clickable(role = Role.Button, onClickLabel = discoveryText("Buka artis", "Open artist")) {
                                onArtist(artist.name)
                            }, horizontalAlignment = Alignment.CenterHorizontally) {
                            artistArtwork(artist, Modifier.size(96.dp).clip(CircleShape)
                                .background(Color(0xFF252525)))
                            Text(artist.name, Modifier.padding(top = 10.dp), color = Color.White,
                                fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        }
                    }
                }
            }
        }
        if (songs.isNotEmpty()) {
            item(key = "discover-recent-heading") {
                DiscoverySectionTitle(discoveryText("Baru diputar", "Recently played"))
            }
            if (data.recent.isEmpty()) item(key = "discover-recent-empty") {
                Text(discoveryText("Riwayat akan muncul setelah Anda mendengarkan lagu.",
                    "Your listening history will appear here."), color = discoveryMuted,
                    fontSize = 14.sp, lineHeight = 21.sp,
                    modifier = Modifier.padding(horizontal = 22.dp))
            }
            items(data.recent, key = { "recent:" + it.uri }, contentType = { "recent-track" }) { song ->
                val active = song.uri == currentSong?.uri
                val play = { if (active) onTogglePlay() else onPlay(song, data.recent) }
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(6.dp)).clickable(role = Role.Button, onClick = play)
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    artwork(song, Modifier.size(48.dp).clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF252525)))
                    Column(Modifier.weight(1f).padding(horizontal = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(song.title, color = if (active) discoveryYellow else Color.White,
                            fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(song.artist, color = discoveryMuted, fontSize = 12.sp,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    IconButton(onClick = play, modifier = Modifier.size(48.dp)) {
                        Icon(if (active && isPlaying) NusaPlayerIcons.Pause else NusaPlayerIcons.Play,
                            if (active && isPlaying) discoveryText("Jeda", "Pause") else discoveryText("Putar", "Play"),
                            tint = discoveryYellow, modifier = Modifier.size(21.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun DiscoverySectionTitle(text: String) {
    Text(text, Modifier.padding(horizontal = 22.dp), color = Color.White,
        fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold)
}

@Composable
private fun DiscoveryArtistArtwork(
    artist: DiscoveryArtist, modifier: Modifier, artwork: @Composable (Song, Modifier) -> Unit
) {
    val context = LocalContext.current
    var portrait by remember(artist.key) { mutableStateOf(AppleArtistPortraitLoader.cached(artist.name)) }
    LaunchedEffect(artist.key) {
        if (portrait == null) portrait = AppleArtistPortraitLoader.warm(context, artist.name)
    }
    val image = portrait
    if (image != null) Image(image.asImageBitmap(), null, modifier, contentScale = ContentScale.Crop)
    else artwork(artist.tracks.first(), modifier)
}
