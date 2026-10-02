package com.yohandeku32.nusamusic

import android.Manifest
import android.content.ComponentName
import android.content.pm.PackageManager
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
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
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
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
import com.yohandeku32.nusamusic.data.ArtistImageLoader
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
                    onShare = ::shareCurrentSong,
                    onRequestPermission = { permissionLauncher.launch(permission) }
                )
            }
        }

        window.decorView.post { hideStatusBar() }
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

    private fun shareCurrentSong(song: Song?) {
        if (song == null) return
        val shareText = "Listening to ${song.title} — ${song.artist}"
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        startActivity(Intent.createChooser(intent, "Share song"))
    }

    private fun hideStatusBar() {
        // Hide only the status bar. Keep Android's navigation bar visible.
        window.statusBarColor = android.graphics.Color.TRANSPARENT

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val insetsController = window.decorView.windowInsetsController ?: return
            insetsController.hide(WindowInsets.Type.statusBars())
            insetsController.systemBarsBehavior =
                WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility =
                window.decorView.systemUiVisibility or View.SYSTEM_UI_FLAG_FULLSCREEN
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
    onShare: (Song?) -> Unit,
    onRequestPermission: () -> Unit
) {
    var isFavorite by remember { mutableStateOf(false) }

    val filtered = songs

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(0.dp)
        ) {
            // The player occupies the first viewport. There is no list
            // rendering work here for the 400+ library entries below it.
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillParentMaxHeight()
                        .clip(
                            RoundedCornerShape(
                                bottomStart = 34.dp,
                                bottomEnd = 34.dp
                            )
                        )
                        .background(MaterialTheme.colorScheme.background),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    TopAppBar(
                        title = { },
                        navigationIcon = {
                            ArtistAvatar(
                                song = currentSong,
                                modifier = Modifier
                                    .padding(start = 18.dp)
                                    .size(40.dp)
                                    .clip(CircleShape)
                            )
                        },
                        actions = {
                            IconButton(onClick = {}) {
                                Icon(Icons.Default.MoreHoriz, contentDescription = "More")
                            }
                            IconButton(onClick = {}) {
                                Icon(Icons.Default.Settings, contentDescription = "Settings")
                            }
                        }
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(Modifier.height(2.dp))

                    VinylRecord(
                        song = currentSong,
                        isPlaying = isPlaying,
                        modifier = Modifier
                            .fillMaxWidth(0.88f)
                            .aspectRatio(1f)
                    )

                    Spacer(Modifier.height(24.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(82.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            currentSong?.title ?: "Choose a song",
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            fontSize = 28.sp,
                            lineHeight = 30.sp,
                            maxLines = 3,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 4.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }

                    Text(
                        currentSong?.artist ?: "Your local music library",
                        modifier = Modifier.fillMaxWidth(),
                        fontSize = 14.sp,
                        maxLines = 1,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(16.dp))

                    // Slight side margins keep the progress line visually compact,
                    // like the supplied reference.
                    SimpleProgressBar(
                        positionMs = positionMs,
                        durationMs = durationMs,
                        enabled = currentSong != null && durationMs > 0L,
                        onSeek = onSeek,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 0.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(formatTime(positionMs), fontSize = 12.sp)
                        Text(formatTime(durationMs), fontSize = 12.sp)
                    }

                    Spacer(Modifier.height(5.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        TransportPillButton(
                            icon = Icons.Default.SkipPrevious,
                            contentDescription = "Previous",
                            onClick = onPrevious,
                            enabled = currentSong != null
                        )

                        Spacer(Modifier.width(16.dp))

                        FilledIconButton(
                            onClick = if (currentSong == null) onRequestPermission else onTogglePlay,
                            modifier = Modifier.size(84.dp),
                            shape = CircleShape
                        ) {
                            Icon(
                                if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        Spacer(Modifier.width(16.dp))

                        TransportPillButton(
                            icon = Icons.Default.SkipNext,
                            contentDescription = "Next",
                            onClick = onNext,
                            enabled = currentSong != null
                        )
                    }

                    Spacer(Modifier.height(10.dp))

                    // Give the secondary actions a little more separation from
                    // the main transport controls.
                    Spacer(Modifier.height(12.dp))

                    Spacer(Modifier.height(18.dp))

                    // Bottom utility controls sit farther below the main transport.
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { },
                            modifier = Modifier.size(42.dp)
                        ) {
                            Icon(
                                Icons.Default.KeyboardArrowDown,
                                contentDescription = "Show songs",
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Spacer(Modifier.weight(1f))

                        IconButton(
                            onClick = { onShare(currentSong) },
                            enabled = currentSong != null,
                            modifier = Modifier.size(42.dp)
                        ) {
                            Icon(
                                Icons.Default.Share,
                                contentDescription = "Share song",
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        IconButton(
                            onClick = { isFavorite = !isFavorite },
                            enabled = currentSong != null,
                            modifier = Modifier.size(42.dp)
                        ) {
                            Icon(
                                if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                                modifier = Modifier.size(22.dp),
                                tint = if (isFavorite) {
                                    Color(0xFFC62828)
                                } else {
                                    MaterialTheme.colorScheme.onBackground
                                }
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                        if (!permissionGranted) {
                            Text(
                                "Give Nusa Music access to your audio files.",
                                color = Color(0xFF9D9D9D),
                                fontSize = 14.sp
                            )
                            Spacer(Modifier.height(12.dp))
                            FilledIconButton(onClick = onRequestPermission) {
                                Icon(Icons.Default.FolderOpen, contentDescription = "Allow music access")
                            }
                        } else if (filtered.isEmpty()) {
                            Text(
                                "No local music found",
                                color = Color(0xFF9D9D9D),
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                // Close the nested scrollable player content column.
                }

                // Black library content begins directly below the white player panel.
                // The player's rounded bottom corners create the only curve.
                item {
                    Spacer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(18.dp)
                            .background(Color.Black)
                    )
                }

            // Lazy rendering: with hundreds of songs, only visible rows are
            // composed. This is the main fix for the previous freeze/force-close.
            if (permissionGranted && filtered.isNotEmpty()) {
                items(
                    items = filtered,
                    key = { it.id },
                    contentType = { "song" }
                ) { song ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Black)
                            .padding(horizontal = 22.dp)
                    ) {
                        SongRow(
                            song = song,
                            selected = currentSong?.id == song.id,
                            onPlay = onPlay,
                            darkSurface = true
                        )
                    }
                }

                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(28.dp)
                            .background(Color.Black)
                    )
                }
            }
        }
    }
}

@Composable
private fun ArtistAvatar(song: Song?, modifier: Modifier = Modifier) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var artistBitmap by remember(song?.artist) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(song?.artist) {
        artistBitmap = ArtistImageLoader.load(
            context = context,
            artistName = song?.artist,
            maxSize = 256
        )
    }

    if (artistBitmap != null) {
        androidx.compose.foundation.Image(
            bitmap = artistBitmap!!.asImageBitmap(),
            contentDescription = song?.artist,
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    } else {
        // Album art remains the immediate fallback while the artist portrait
        // is being resolved in the background or when no match is found.
        ArtworkView(song = song, maxSizePx = 96, modifier = modifier)
    }
}

@Composable
private fun SimpleProgressBar(
    positionMs: Long,
    durationMs: Long,
    enabled: Boolean,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val fraction = if (durationMs > 0L) {
        (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    val primary = MaterialTheme.colorScheme.primary

    androidx.compose.foundation.Canvas(
        modifier = modifier
            .height(18.dp)
            .pointerInput(durationMs, enabled) {
                if (enabled && durationMs > 0L) {
                    detectTapGestures { offset ->
                        val tappedFraction =
                            (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                        onSeek((tappedFraction * durationMs).toLong())
                    }
                }
            }
    ) {
        val y = size.height / 2f
        val stroke = 3.4.dp.toPx()

        drawLine(
            color = Color(0xFFD0CDC6),
            start = androidx.compose.ui.geometry.Offset(0f, y),
            end = androidx.compose.ui.geometry.Offset(size.width, y),
            strokeWidth = stroke
        )

        drawLine(
            color = primary,
            start = androidx.compose.ui.geometry.Offset(0f, y),
            end = androidx.compose.ui.geometry.Offset(size.width * fraction, y),
            strokeWidth = stroke
        )

        drawCircle(
            color = primary,
            radius = 3.2.dp.toPx(),
            center = androidx.compose.ui.geometry.Offset(size.width * fraction, y)
        )
    }
}

@Composable
private fun TransportPillButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    enabled: Boolean
) {
    androidx.compose.material3.Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(width = 92.dp, height = 54.dp),
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 0.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun VinylRecord(song: Song?, isPlaying: Boolean, modifier: Modifier = Modifier) {
    // Preserve the physical angle when pausing and resuming playback.
    val rotation = remember { Animatable(0f) }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (isActive) {
                rotation.animateTo(
                    targetValue = rotation.value + 360f,
                    animationSpec = tween(
                        durationMillis = 6_500,
                        easing = LinearEasing
                    )
                )
            }
        }
    }

    Box(
        modifier = modifier.graphicsLayer {
            rotationZ = rotation.value
        },
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            Modifier
                .fillMaxSize()
                .clip(CircleShape)
        ) {
            val radius = size.minDimension / 2f

            // Base PVC: almost black, but with enough tonal depth to reveal
            // the pressed surface instead of looking like a flat black circle.
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF202020),
                        Color(0xFF0D0D0D),
                        Color(0xFF030303),
                        Color(0xFF000000)
                    ),
                    center = androidx.compose.ui.geometry.Offset(
                        size.width * 0.42f,
                        size.height * 0.37f
                    ),
                    radius = radius * 1.12f
                ),
                radius = radius
            )

            // Very subtle angular sheen from the molded PVC surface.
            drawCircle(
                brush = Brush.sweepGradient(
                    colorStops = arrayOf(
                        0.00f to Color.White.copy(alpha = 0.010f),
                        0.08f to Color.White.copy(alpha = 0.035f),
                        0.15f to Color.Transparent,
                        0.34f to Color.Transparent,
                        0.48f to Color.White.copy(alpha = 0.022f),
                        0.57f to Color.Transparent,
                        0.82f to Color.Transparent,
                        1.00f to Color.White.copy(alpha = 0.010f)
                    ),
                    center = center
                ),
                radius = radius
            )

            // Fine pressed grooves. Keeping them numerous and very low contrast
            // makes the surface read as physical vinyl rather than drawn rings.
            for (i in 0..245) {
                val t = i / 245f
                val grooveRadius = radius * (0.245f + t * 0.715f)
                val alpha = when {
                    i % 41 == 0 -> 0.080f
                    i % 13 == 0 -> 0.040f
                    i % 5 == 0 -> 0.020f
                    else -> 0.010f
                }

                drawCircle(
                    color = Color.White.copy(alpha = alpha),
                    radius = grooveRadius,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = if (i % 41 == 0) 0.75f else 0.35f
                    )
                )
            }

            // Small broken reflections across selected groove bands. Real
            // records rarely show perfectly uniform rings when light hits them.
            for (i in 0 until 42) {
                val startAngle = (i * 151f + (i % 4) * 9f) % 360f
                val sweepAngle = 10f + (i % 7) * 6f
                val arcRadius = radius * (0.34f + ((i * 23) % 53) / 100f)

                drawArc(
                    color = Color.White.copy(alpha = 0.012f + (i % 5) * 0.004f),
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = androidx.compose.ui.geometry.Offset(
                        center.x - arcRadius,
                        center.y - arcRadius
                    ),
                    size = androidx.compose.ui.geometry.Size(
                        arcRadius * 2f,
                        arcRadius * 2f
                    ),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1f)
                )
            }

            // Main soft specular band.
            drawArc(
                color = Color.White.copy(alpha = 0.085f),
                startAngle = -74f,
                sweepAngle = 24f,
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(
                    size.width * 0.065f,
                    size.height * 0.065f
                ),
                size = androidx.compose.ui.geometry.Size(
                    size.width * 0.87f,
                    size.height * 0.87f
                ),
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = 13f
                )
            )

            // Narrow secondary highlight gives the record a slightly curved,
            // glossy edge instead of a flat vector appearance.
            drawArc(
                color = Color.White.copy(alpha = 0.040f),
                startAngle = -58f,
                sweepAngle = 70f,
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(
                    size.width * 0.125f,
                    size.height * 0.125f
                ),
                size = androidx.compose.ui.geometry.Size(
                    size.width * 0.75f,
                    size.height * 0.75f
                ),
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = 4.5f
                )
            )

            // Very soft dark crescent opposite the highlight: this is what makes
            // the disc feel rounded under one directional light source.
            drawArc(
                color = Color.Black.copy(alpha = 0.34f),
                startAngle = 112f,
                sweepAngle = 92f,
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(
                    size.width * 0.06f,
                    size.height * 0.06f
                ),
                size = androidx.compose.ui.geometry.Size(
                    size.width * 0.88f,
                    size.height * 0.88f
                ),
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = 10f
                )
            )

            // Raised center well around the label.
            drawCircle(
                color = Color.Black.copy(alpha = 0.52f),
                radius = radius * 0.245f
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.035f),
                radius = radius * 0.255f,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.2f)
            )
            drawCircle(
                color = Color.Black.copy(alpha = 0.55f),
                radius = radius * 0.205f,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.0f)
            )

            // Pressed outer rim.
            drawCircle(
                color = Color.Black.copy(alpha = 0.78f),
                radius = radius * 0.988f,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.5f)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.035f),
                radius = radius * 0.955f,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.1f)
            )
        }

        // The album art becomes the physical center label.
        ArtworkView(
            song = song,
            maxSizePx = 512,
            modifier = Modifier
                .size(142.dp)
                .clip(CircleShape)
        )

        // Small spindle hole and metal center pin.
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(Color(0xFF080808))
        )
        Box(
            modifier = Modifier
                .size(5.dp)
                .clip(CircleShape)
                .background(Color(0xFFA8A8A8))
        )
    }
}

@Composable
private fun ArtworkView(
    song: Song?,
    maxSizePx: Int = 512,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var bitmap by remember(song?.uri, maxSizePx) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(song?.uri, maxSizePx) {
        bitmap = song?.let {
            ArtworkLoader.load(
                context = context,
                uriString = it.uri,
                maxSize = maxSizePx
            )
        }
    }

    Box(
        modifier = modifier.background(
            Brush.linearGradient(
                listOf(
                    MaterialTheme.colorScheme.surfaceVariant,
                    MaterialTheme.colorScheme.surface
                )
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
            Icon(
                Icons.Default.MusicNote,
                contentDescription = null,
                modifier = Modifier.size(42.dp)
            )
        }
    }
}

@Composable
private fun SongRow(
    song: Song,
    selected: Boolean,
    onPlay: (Song) -> Unit,
    darkSurface: Boolean = false
) {
    val selectedBackground =
        if (darkSurface) Color(0xFF191919) else MaterialTheme.colorScheme.surfaceVariant

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) selectedBackground else Color.Transparent)
            .clickable { onPlay(song) }
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ArtworkView(
            song = song,
            maxSizePx = 128,
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(12.dp))
        )

        Spacer(Modifier.width(14.dp))

        Column(Modifier.weight(1f)) {
            Text(
                song.title,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (darkSurface) Color.White else MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(2.dp))
            Text(
                "${song.artist}  •  ${song.album}",
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                fontSize = 12.sp,
                color = if (darkSurface) Color(0xFF9D9D9D) else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        IconButton(onClick = { onPlay(song) }) {
            Icon(
                if (selected) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (selected) "Pause" else "Play",
                tint = if (darkSurface) Color.White else MaterialTheme.colorScheme.onBackground
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
