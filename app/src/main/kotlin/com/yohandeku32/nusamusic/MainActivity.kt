package com.yohandeku32.nusamusic

import android.Manifest
import android.content.ComponentName
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.yohandeku32.nusamusic.data.MusicRepository
import com.yohandeku32.nusamusic.model.Song
import com.yohandeku32.nusamusic.playback.PlaybackService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private var controller: MediaController? = null
    private var songs by mutableStateOf<List<Song>>(emptyList())
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
        future.addListener({ controller = future.get() }, mainExecutor)

        setContent {
            NusaMusicTheme {
                NusaMusicApp(
                    songs = songs,
                    permissionGranted = permissionGranted,
                    onPlay = ::playSong,
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

    private fun playSong(song: Song) {
        controller?.let { c ->
            val item = MediaItem.Builder()
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
            c.setMediaItem(item)
            c.prepare()
            c.play()
        }
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
    permissionGranted: Boolean,
    onPlay: (Song) -> Unit,
    onRequestPermission: () -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    val filtered = remember(songs, query) {
        if (query.isBlank()) songs
        else songs.filter {
            it.title.contains(query, true) || it.artist.contains(query, true) || it.album.contains(query, true)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "NUSA MUSIC",
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.2.sp
                    )
                },
                actions = {
                    IconButton(onClick = { query = if (query.isBlank()) " " else "" }) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                listOf("Songs", "Albums", "Favorites").forEachIndexed { index, label ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = {
                            Icon(
                                when (index) {
                                    1 -> Icons.Default.Album
                                    2 -> Icons.Default.FavoriteBorder
                                    else -> Icons.Default.MusicNote
                                },
                                contentDescription = label
                            )
                        },
                        label = { Text(label) }
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (query.isNotBlank()) {
                OutlinedTextField(
                    value = query.trim(),
                    onValueChange = { query = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text("Search songs...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Close search")
                        }
                    },
                    singleLine = true
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(160.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.secondaryContainer
                            )
                        )
                    )
                    .padding(24.dp)
            ) {
                Column(Modifier.align(Alignment.CenterStart)) {
                    Text("YOUR LIBRARY", style = MaterialTheme.typography.labelLarge)
                    Text(
                        "${songs.size} songs",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        if (permissionGranted) "Music stored on this device"
                        else "Allow audio access to begin",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Icon(
                    Icons.Default.Headphones,
                    contentDescription = null,
                    modifier = Modifier
                        .size(76.dp)
                        .align(Alignment.CenterEnd),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            if (!permissionGranted) {
                Button(
                    onClick = onRequestPermission,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Icon(Icons.Default.FolderOpen, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Allow Music Access")
                }
            }

            Text(
                "Songs",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            if (filtered.isEmpty() && permissionGranted) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "No local music found",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(filtered, key = { it.id }) { song ->
                        SongRow(song, onPlay)
                    }
                }
            }
        }
    }
}

@Composable
private fun SongRow(song: Song, onPlay: (Song) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPlay(song) }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.MusicNote, contentDescription = null)
        }

        Spacer(Modifier.width(14.dp))

        Column(Modifier.weight(1f)) {
            Text(song.title, maxLines = 1, fontWeight = FontWeight.SemiBold)
            Text(
                "${song.artist} • ${song.album}",
                maxLines = 1,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
        }

        IconButton(onClick = { onPlay(song) }) {
            Icon(Icons.Default.PlayArrow, contentDescription = "Play")
        }
    }
}

@Composable
private fun NusaMusicTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            background = Color(0xFF0A0A0F),
            surface = Color(0xFF121218),
            primary = Color(0xFFB8A3FF),
            secondary = Color(0xFF8EE7DA)
        ),
        content = content
    )
}
