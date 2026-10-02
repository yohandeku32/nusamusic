package com.yohandeku32.nusamusic

import android.Manifest
import android.content.ComponentName
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.yohandeku32.nusamusic.data.ArtworkLoader
import com.yohandeku32.nusamusic.data.MusicRepository
import com.yohandeku32.nusamusic.model.Song
import com.yohandeku32.nusamusic.playback.PlaybackService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private var controller: MediaController? = null
    private var songs by mutableStateOf<List<Song>>(emptyList())
    private var currentSong by mutableStateOf<Song?>(null)
    private var isPlaying by mutableStateOf(false)
    private var positionMs by mutableLongStateOf(0L)
    private var durationMs by mutableLongStateOf(0L)
    private var permissionGranted by mutableStateOf(false)

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            permissionGranted = granted
            if (granted) loadSongs()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val permission = if (Build.VERSION.SDK_INT >= 33) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        permissionGranted = checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
        if (permissionGranted) loadSongs() else permissionLauncher.launch(permission)

        val token = SessionToken(this, ComponentName(this, PlaybackService::class.java))
        val future = MediaController.Builder(this, token).buildAsync()
        future.addListener({
            val c = future.get()
            controller = c
            c.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(playing: Boolean) {
                    isPlaying = playing
                }

                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    val id = mediaItem?.mediaId?.toLongOrNull()
                    currentSong = songs.firstOrNull { it.id == id } ?: currentSong
                }
            })
        }, mainExecutor)

        lifecycleScope.launch {
            while (isActive) {
                controller?.let { c ->
                    positionMs = c.currentPosition.coerceAtLeast(0L)
                    durationMs = c.duration.coerceAtLeast(0L)
                    isPlaying = c.isPlaying
                }
                delay(400)
            }
        }

        setContent {
            NusaMusicTheme {
                NusaMusicApp(
                    songs = songs,
                    currentSong = currentSong,
                    isPlaying = isPlaying,
                    positionMs = positionMs,
                    durationMs = durationMs,
                    permissionGranted = permissionGranted,
                    onPlay = ::playSong,
                    onTogglePlay = ::togglePlay,
                    onNext = ::nextSong,
                    onPrevious = ::previousSong,
                    onSeek = ::seekTo,
                    onRequestPermission = { permissionLauncher.launch(permission) }
                )
            }
        }
    }

    private fun loadSongs() {
        lifecycleScope.launch {
            songs = withContext(Dispatchers.IO) {
                MusicRepository(this@MainActivity).loadSongs()
            }
        }
    }

    private fun mediaItemFor(song: Song): MediaItem =
        MediaItem.Builder()
            .setMediaId(song.id.toString())
            .setUri(song.uri)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(song.title)
                    .setArtist(song.artist)
                    .setAlbumTitle(song.album)
                    .build()
            )
            .build()

    private fun playSong(song: Song) {
        controller?.let { c ->
            val index = songs.indexOfFirst { it.id == song.id }
            if (index >= 0) {
                c.setMediaItems(songs.map(::mediaItemFor), index, 0L)
                c.prepare()
                c.play()
                currentSong = song
                isPlaying = true
            }
        }
    }

    private fun togglePlay() {
        controller?.let {
            if (it.isPlaying) it.pause() else it.play()
        }
    }

    private fun nextSong() {
        controller?.let { it.seekToNextMediaItem(); it.play() }
    }

    private fun previousSong() {
        controller?.let {
            if (it.currentPosition > 5_000L) it.seekTo(0L) else it.seekToPreviousMediaItem()
        }
    }

    private fun seekTo(value: Long) {
        controller?.seekTo(value)
    }

    override fun onDestroy() {
        controller?.release()
        controller = null
        super.onDestroy()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NusaMusicApp(
    songs: List<Song>,
    currentSong: Song?,
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    permissionGranted: Boolean,
    onPlay: (Song) -> Unit,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onRequestPermission: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    var showSearch by remember { mutableStateOf(false) }

    val filtered = remember(songs, query) {
        if (query.isBlank()) songs
        else songs.filter {
            it.title.contains(query, true) ||
                it.artist.contains(query, true) ||
                it.album.contains(query, true)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "NUSA MUSIC",
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                },
                navigationIcon = {
                    Icon(
                        Icons.Default.MusicNote,
                        contentDescription = null,
                        modifier = Modifier.padding(start = 16.dp).size(24.dp)
                    )
                },
                actions = {
                    IconButton(onClick = { showSearch = !showSearch }) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            if (showSearch) {
                item {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        placeholder = { Text("Search songs, artists, albums") },
                        singleLine = true
                    )
                }
            }

            item {
                Spacer(Modifier.height(12.dp))
                Text(
                    "NOW PLAYING",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(18.dp))
                VinylRecord(
                    song = currentSong,
                    isPlaying = isPlaying,
                    modifier = Modifier.size(310.dp)
                )
                Spacer(Modifier.height(22.dp))
                Text(
                    currentSong?.title ?: "Choose a song",
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 30.sp,
                    lineHeight = 32.sp,
                    modifier = Modifier.padding(horizontal = 24.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    currentSong?.artist ?: "Your local music library",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(20.dp))
                Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
                    val safeDuration = durationMs.coerceAtLeast(0L)
                    val safePosition = positionMs.coerceIn(0L, safeDuration.coerceAtLeast(1L))
                    Slider(
                        value = if (safeDuration > 0) safePosition.toFloat() / safeDuration.toFloat() else 0f,
                        onValueChange = { fraction ->
                            if (safeDuration > 0) onSeek((fraction * safeDuration).toLong())
                        },
                        enabled = currentSong != null && safeDuration > 0
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(formatTime(positionMs), fontSize = 12.sp)
                        Text(formatTime(durationMs), fontSize = 12.sp)
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    IconButton(onClick = onPrevious, enabled = currentSong != null) {
                        Icon(Icons.Default.SkipPrevious, contentDescription = "Previous", modifier = Modifier.size(30.dp))
                    }
                    FilledIconButton(
                        onClick = if (currentSong == null) onRequestPermission else onTogglePlay,
                        modifier = Modifier.size(76.dp),
                        shape = CircleShape
                    ) {
                        Icon(
                            if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            modifier = Modifier.size(34.dp)
                        )
                    }
                    IconButton(onClick = onNext, enabled = currentSong != null) {
                        Icon(Icons.Default.SkipNext, contentDescription = "Next", modifier = Modifier.size(30.dp))
                    }
                }
                Spacer(Modifier.height(28.dp))
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "YOUR SONGS",
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        modifier = Modifier.weight(1f)
                    )
                    if (songs.isNotEmpty()) {
                        Icon(Icons.Default.FavoriteBorder, contentDescription = "Favorites")
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            if (!permissionGranted) {
                item {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                        Text("Give Nusa Music access to your audio files.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(12.dp))
                        FilledIconButton(onClick = onRequestPermission) {
                            Icon(Icons.Default.FolderOpen, contentDescription = "Allow music access")
                        }
                    }
                }
            } else if (filtered.isEmpty()) {
                item {
                    Text("No local music found", modifier = Modifier.padding(32.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                items(filtered, key = { it.id }) { song ->
                    SongRow(
                        song = song,
                        selected = currentSong?.id == song.id,
                        onPlay = onPlay
                    )
                }
            }
        }
    }
}

@Composable
private fun VinylRecord(song: Song?, isPlaying: Boolean, modifier: Modifier = Modifier) {
    val rotation = remember { androidx.compose.animation.core.Animatable(0f) }
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (true) {
                val start = rotation.value
                rotation.animateTo(start + 360f, androidx.compose.animation.core.tween(durationMillis = 6_000))
                rotation.snapTo(rotation.value % 360f)
            }
        }
    }

    Box(
        modifier = modifier.graphicsLayer { rotationZ = rotation.value },
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize().clip(CircleShape)) {
            drawCircle(Color(0xFF090909))
            val maxRadius = size.minDimension / 2f
            for (i in 1..24) {
                val radius = maxRadius * (0.16f + (i * 0.033f))
                drawCircle(
                    Color.White.copy(alpha = 0.045f),
                    radius = radius,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.1f)
                )
            }
            drawCircle(
                Color.White.copy(alpha = 0.08f),
                radius = maxRadius * 0.98f,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
            )
        }

        ArtworkView(
            song = song,
            modifier = Modifier.size(142.dp).clip(CircleShape)
        )
        Box(
            modifier = Modifier.size(18.dp).clip(CircleShape).background(Color(0xFF0B0B0B))
        )
        Box(
            modifier = Modifier.size(5.dp).clip(CircleShape).background(Color(0xFF8A8A8A))
        )
    }
}

@Composable
private fun ArtworkView(song: Song?, modifier: Modifier = Modifier) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var bitmap by remember(song?.uri) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(song?.uri) {
        bitmap = song?.let { ArtworkLoader.load(context, it.uri) }
    }

    Box(
        modifier = modifier.background(
            Brush.linearGradient(
                listOf(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.surface)
            )
        ),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            androidx.compose.foundation.Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = song?.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(Icons.Default.MusicNote, contentDescription = null, modifier = Modifier.size(42.dp))
        }
    }
}

@Composable
private fun SongRow(song: Song, selected: Boolean, onPlay: (Song) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(if (selected) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent)
            .clickable { onPlay(song) }
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ArtworkView(song = song, modifier = Modifier.size(58.dp).clip(RoundedCornerShape(13.dp)))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(song.title, maxLines = 1, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)
            Spacer(Modifier.height(2.dp))
            Text(
                "${song.artist}  •  ${song.album}",
                maxLines = 1,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = { onPlay(song) }) {
            Icon(
                if (selected) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (selected) "Pause" else "Play"
            )
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000L).coerceAtLeast(0L)
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return "%d:%02d".format(minutes, seconds)
}

@Composable
private fun NusaMusicTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val scheme = if (dark) {
        darkColorScheme(
            background = Color(0xFF050505),
            surface = Color(0xFF101010),
            surfaceVariant = Color(0xFF1C1C1C),
            primary = Color(0xFFF4F1EA)
        )
    } else {
        lightColorScheme(
            background = Color(0xFFF2F0EB),
            surface = Color(0xFFF7F5F0),
            surfaceVariant = Color(0xFFE2E0DA),
            primary = Color(0xFF111111)
        )
    }
    MaterialTheme(colorScheme = scheme, content = content)
}
