package com.yohandeku32.nusamusic

import android.Manifest
import android.content.ComponentName
import android.content.pm.PackageManager
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import java.util.Locale
import android.os.Bundle
import android.content.SharedPreferences
import kotlin.math.abs
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.WindowCompat
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.yohandeku32.nusamusic.data.ArtworkLoader
import com.yohandeku32.nusamusic.data.AudioCodecInfo
import com.yohandeku32.nusamusic.data.AudioCodecLoader
import com.yohandeku32.nusamusic.data.ArtistImageLoader
import com.yohandeku32.nusamusic.data.LyricLine
import com.yohandeku32.nusamusic.data.LyricsLoader
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
    private var shuffleEnabled by mutableStateOf(false)
    private var repeatMode by mutableIntStateOf(Player.REPEAT_MODE_OFF)

    private val playbackPrefs: SharedPreferences by lazy {
        getSharedPreferences("playback_state", MODE_PRIVATE)
    }
    private var lastPersistedPosition = -1L

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            permissionGranted = granted
            if (granted) loadSongs()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Draw the white player surface underneath the hidden status-bar area,
        // while keeping the Android navigation bar visible.
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Android 15/16 edge-to-edge ignores a custom navigation-bar color
        // for gesture navigation. The correct solution is to make the system
        // navigation area transparent and let the scrolling content draw behind it.
        val isDarkMode =
            (resources.configuration.uiMode and
                android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
                android.content.res.Configuration.UI_MODE_NIGHT_YES
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
            window.navigationBarDividerColor = android.graphics.Color.TRANSPARENT
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility =
                window.decorView.systemUiVisibility or
                    if (isDarkMode) 0 else View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        }

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
                    persistPlaybackState(c)
                }

                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    val id = mediaItem?.mediaId?.toLongOrNull()
                    currentSong = songs.firstOrNull { it.id == id } ?: currentSong
                    persistPlaybackState(c, force = true)
                }

                override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                    shuffleEnabled = shuffleModeEnabled
                    persistPlaybackState(c, force = true)
                }

                override fun onRepeatModeChanged(repeatModeValue: Int) {
                    repeatMode = repeatModeValue
                    persistPlaybackState(c, force = true)
                }
            })

            shuffleEnabled = c.shuffleModeEnabled
            repeatMode = c.repeatMode
            syncCurrentSong(c)
            restorePlaybackStateIfNeeded(c)
        }, mainExecutor)

        lifecycleScope.launch {
            while (isActive) {
                controller?.let { c ->
                    positionMs = c.currentPosition.coerceAtLeast(0L)
                    durationMs = c.duration.coerceAtLeast(0L)
                    isPlaying = c.isPlaying
                    syncCurrentSong(c)

                    // Persist the position periodically so a process restart
                    // can return to the same song and approximate position.
                    if (
                        c.currentMediaItem != null &&
                        (lastPersistedPosition < 0L ||
                            kotlin.math.abs(c.currentPosition - lastPersistedPosition) >= 3_000L)
                    ) {
                        persistPlaybackState(c)
                    }
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
                    onToggleShuffle = ::toggleShuffle,
                    onToggleRepeat = ::toggleRepeat,
                    shuffleEnabled = shuffleEnabled,
                    repeatMode = repeatMode,
                    onRequestPermission = { permissionLauncher.launch(permission) }
                )
            }
        }

        window.decorView.post { hideStatusBar() }
    }

    private fun syncCurrentSong(c: MediaController) {
        val id = c.currentMediaItem?.mediaId?.toLongOrNull() ?: return
        songs.firstOrNull { it.id == id }?.let { currentSong = it }
    }

    private fun persistPlaybackState(
        c: MediaController,
        force: Boolean = false
    ) {
        val mediaId = c.currentMediaItem?.mediaId ?: return
        val currentPosition = c.currentPosition.coerceAtLeast(0L)

        if (!force &&
            lastPersistedPosition >= 0L &&
            kotlin.math.abs(currentPosition - lastPersistedPosition) < 3_000L
        ) {
            return
        }

        playbackPrefs.edit()
            .putString("media_id", mediaId)
            .putLong("position_ms", currentPosition)
            .putBoolean("is_playing", c.isPlaying)
            .putBoolean("shuffle_enabled", c.shuffleModeEnabled)
            .putInt("repeat_mode", c.repeatMode)
            .apply()

        lastPersistedPosition = currentPosition
    }

    private fun restorePlaybackStateIfNeeded(c: MediaController) {
        if (songs.isEmpty() || c.currentMediaItem != null) return

        val mediaId = playbackPrefs.getString("media_id", null) ?: return
        val index = songs.indexOfFirst { it.id.toString() == mediaId }
        if (index < 0) return

        val savedPosition = playbackPrefs.getLong("position_ms", 0L).coerceAtLeast(0L)
        val savedPlaying = playbackPrefs.getBoolean("is_playing", false)
        val savedShuffle = playbackPrefs.getBoolean("shuffle_enabled", false)
        val savedRepeat = playbackPrefs.getInt("repeat_mode", Player.REPEAT_MODE_OFF)

        c.setMediaItems(songs.map(::mediaItemFor), index, savedPosition)
        c.shuffleModeEnabled = savedShuffle
        c.repeatMode = savedRepeat
        c.prepare()

        currentSong = songs[index]
        shuffleEnabled = savedShuffle
        repeatMode = savedRepeat

        if (savedPlaying) {
            c.play()
            isPlaying = true
        } else {
            isPlaying = false
        }
    }

    private fun toggleShuffle() {
        controller?.let { c ->
            c.shuffleModeEnabled = !c.shuffleModeEnabled
            shuffleEnabled = c.shuffleModeEnabled
            persistPlaybackState(c, force = true)
        }
    }

    private fun toggleRepeat() {
        controller?.let { c ->
            val next = when (c.repeatMode) {
                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                else -> Player.REPEAT_MODE_OFF
            }
            c.repeatMode = next
            repeatMode = next
            persistPlaybackState(c, force = true)
        }
    }

    private fun loadSongs() {
        lifecycleScope.launch {
            songs = withContext(Dispatchers.IO) {
                MusicRepository(this@MainActivity).loadSongs()
            }

            controller?.let { c ->
                syncCurrentSong(c)
                restorePlaybackStateIfNeeded(c)
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
        controller?.let { persistPlaybackState(it, force = true) }
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
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    shuffleEnabled: Boolean,
    repeatMode: Int,
    onRequestPermission: () -> Unit
) {
    var isFavorite by remember { mutableStateOf(false) }
    var embeddedLyrics by remember(currentSong?.uri) {
        mutableStateOf<com.yohandeku32.nusamusic.data.LyricsResult?>(null)
    }
    var lyricsLoading by remember(currentSong?.uri) { mutableStateOf(false) }

    val context = androidx.compose.ui.platform.LocalContext.current
    val filtered = songs

    val pagerState = androidx.compose.foundation.pager.rememberPagerState(
        initialPage = 0,
        pageCount = { 2 }
    )
    val playerScrollState =
        androidx.compose.foundation.lazy.rememberLazyListState()
    val libraryListState =
        androidx.compose.foundation.lazy.rememberLazyListState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(currentSong?.uri) {
        embeddedLyrics = null
        lyricsLoading = currentSong != null
        embeddedLyrics = currentSong?.let {
            LyricsLoader.load(
                context = context,
                song = it
            )
        }
        lyricsLoading = false
    }

    val lyricLines = embeddedLyrics?.lines.orEmpty()

    val activeLyricIndex by remember(lyricLines) {
        derivedStateOf {
            findActiveLyricIndex(lyricLines, positionMs)
        }
    }

    // Keep the active lyric in view only after the user has entered the lyrics
    // section. During a manual drag, the user's scroll always wins.
    LaunchedEffect(activeLyricIndex, lyricLines.size) {
        if (
            activeLyricIndex >= 0 &&
            lyricLines.isNotEmpty() &&
            playerScrollState.firstVisibleItemIndex >= 1 &&
            !playerScrollState.isScrollInProgress
        ) {
            playerScrollState.animateScrollToItem(
                index = activeLyricIndex + 2,
                scrollOffset = -110
            )
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0)
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            androidx.compose.foundation.pager.HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                beyondViewportPageCount = 0
            ) { page ->
                when (page) {
                    0 -> {
                        LazyColumn(
                            state = playerScrollState,
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black),
                            contentPadding = PaddingValues(0.dp)
                        ) {
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
                                    Spacer(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(24.dp)
                                    )

                                    TopAppBar(
                                        title = { },
                                        windowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
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
                                                Icon(
                                                    Icons.Default.MoreHoriz,
                                                    contentDescription = "More"
                                                )
                                            }
                                            IconButton(onClick = {}) {
                                                Icon(
                                                    Icons.Default.Settings,
                                                    contentDescription = "Settings"
                                                )
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
                                            val titleText =
                                                currentSong?.title ?: "Choose a song"

                                            val titleWordCount = remember(titleText) {
                                                titleText.trim()
                                                    .split(Regex("\\s+"))
                                                    .count { it.isNotBlank() }
                                            }

                                            val titleSize = remember(titleWordCount) {
                                                when {
                                                    titleWordCount <= 2 -> 35.sp
                                                    titleWordCount == 3 -> 31.sp
                                                    else -> 28.sp
                                                }
                                            }

                                            val titleLineHeight = remember(titleWordCount) {
                                                when {
                                                    titleWordCount <= 2 -> 37.sp
                                                    titleWordCount == 3 -> 33.sp
                                                    else -> 30.sp
                                                }
                                            }

                                            Text(
                                                titleText,
                                                fontFamily = FontFamily.Serif,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = titleSize,
                                                lineHeight = titleLineHeight,
                                                maxLines = 3,
                                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                                modifier = Modifier.padding(horizontal = 4.dp),
                                                textAlign = TextAlign.Center
                                            )
                                        }

                                        Text(
                                            currentSong?.artist ?: "Your local music library",
                                            modifier = Modifier.fillMaxWidth(),
                                            fontSize = 14.sp,
                                            maxLines = 1,
                                            textAlign = TextAlign.Center,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        Spacer(Modifier.height(16.dp))

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
                                                .padding(horizontal = 12.dp),
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
                                                onClick = if (currentSong == null) {
                                                    onRequestPermission
                                                } else {
                                                    onTogglePlay
                                                },
                                                modifier = Modifier.size(84.dp),
                                                shape = CircleShape
                                            ) {
                                                Icon(
                                                    if (isPlaying) {
                                                        Icons.Default.Pause
                                                    } else {
                                                        Icons.Default.PlayArrow
                                                    },
                                                    contentDescription = if (isPlaying) {
                                                        "Pause"
                                                    } else {
                                                        "Play"
                                                    },
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
                                        Spacer(Modifier.height(12.dp))
                                        Spacer(Modifier.height(18.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            IconButton(
                                                onClick = {
                                                    scope.launch {
                                                        pagerState.animateScrollToPage(1)
                                                    }
                                                },
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
                                                    if (isFavorite) {
                                                        Icons.Default.Favorite
                                                    } else {
                                                        Icons.Default.FavoriteBorder
                                                    },
                                                    contentDescription = if (isFavorite) {
                                                        "Favorite"
                                                    } else {
                                                        "Add to favorites"
                                                    },
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
                                        AudioQualityPill(song = currentSong)

                                        if (!permissionGranted) {
                                            Text(
                                                "Give Nusa Music access to your audio files.",
                                                color = Color(0xFF9D9D9D),
                                                fontSize = 14.sp
                                            )
                                            Spacer(Modifier.height(12.dp))
                                            FilledIconButton(onClick = onRequestPermission) {
                                                Icon(
                                                    Icons.Default.FolderOpen,
                                                    contentDescription = "Allow music access"
                                                )
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
                            }

                            // The player and the lyrics are one continuous
                            // vertical scroll. This makes a slow upward drag
                            // move the player itself with the finger.
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.Black)
                                ) {
                                    Spacer(Modifier.height(12.dp))

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(188.dp)
                                            .clip(
                                                RoundedCornerShape(
                                                    bottomStart = 34.dp,
                                                    bottomEnd = 34.dp
                                                )
                                            )
                                            .background(MaterialTheme.colorScheme.background)
                                            .padding(horizontal = 16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        IconButton(
                                            onClick = {
                                                scope.launch {
                                                    playerScrollState.animateScrollToItem(0)
                                                }
                                            }
                                        ) {
                                            Icon(
                                                Icons.Default.KeyboardArrowDown,
                                                contentDescription = "Back to player",
                                                modifier = Modifier.size(28.dp)
                                            )
                                        }

                                        Spacer(Modifier.weight(1f))

                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = currentSong?.title ?: "Lyrics",
                                                fontFamily = FontFamily.Serif,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 27.sp,
                                                lineHeight = 29.sp,
                                                maxLines = 2,
                                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                                textAlign = TextAlign.Center
                                            )
                                            Text(
                                                text = currentSong?.artist ?: "",
                                                fontSize = 12.sp,
                                                maxLines = 1,
                                                textAlign = TextAlign.Center,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        Spacer(Modifier.weight(1f))
                                        Spacer(Modifier.width(48.dp))
                                    }
                                }
                            }

                            if (lyricsLoading) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(240.dp)
                                            .background(Color.Black),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "Loading lyrics…",
                                            color = Color(0xFF8A8A8A),
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                            } else if (lyricLines.isEmpty()) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(240.dp)
                                            .background(Color.Black),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "No lyrics found",
                                            color = Color(0xFF8A8A8A),
                                            fontSize = 15.sp
                                        )
                                    }
                                }
                            } else {
                                itemsIndexed(
                                    items = lyricLines,
                                    key = { _, line -> "\${line.startMs}-\${line.text}" },
                                    contentType = { _, _ -> "lyric" }
                                ) { index, line ->
                                    val distance = if (activeLyricIndex >= 0) {
                                        abs(index - activeLyricIndex)
                                    } else {
                                        99
                                    }

                                    val alpha = when {
                                        index == activeLyricIndex -> 1f
                                        distance == 1 -> 0.72f
                                        distance == 2 -> 0.46f
                                        else -> 0.24f
                                    }

                                    Text(
                                        text = line.text,
                                        color = Color.White.copy(alpha = alpha),
                                        fontSize = if (index == activeLyricIndex) 18.sp else 17.sp,
                                        fontWeight = if (index == activeLyricIndex) {
                                            FontWeight.Medium
                                        } else {
                                            FontWeight.Normal
                                        },
                                        lineHeight = 24.sp,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color.Black)
                                            .padding(
                                                horizontal = 38.dp,
                                                vertical = 6.dp
                                            )
                                    )
                                }
                            }

                            item {
                                Spacer(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(120.dp)
                                        .background(Color.Black)
                                )
                            }
                        }
                    }

                    1 -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black)
                        ) {
                            LazyColumn(
                                state = libraryListState,
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(top = 10.dp, bottom = 120.dp)
                            ) {
                                if (permissionGranted && filtered.isNotEmpty()) {
                                    items(
                                        items = filtered,
                                        key = { it.id },
                                        contentType = { "song" }
                                    ) { song ->
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
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
                                } else if (!permissionGranted) {
                                    item {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 80.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                "Give Nusa Music access to your audio files.",
                                                color = Color(0xFF9D9D9D),
                                                fontSize = 14.sp
                                            )
                                            Spacer(Modifier.height(12.dp))
                                            FilledIconButton(onClick = onRequestPermission) {
                                                Icon(
                                                    Icons.Default.FolderOpen,
                                                    contentDescription = "Allow music access"
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    item {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 80.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                "No local music found",
                                                color = Color(0xFF9D9D9D),
                                                fontSize = 14.sp
                                            )
                                        }
                                    }
                                }
                            }

                            val showFloatingControls by remember {
                                derivedStateOf {
                                    libraryListState.firstVisibleItemIndex >= 3
                                }
                            }
                            val showBackToPlayer by remember {
                                derivedStateOf {
                                    libraryListState.firstVisibleItemIndex >= 5
                                }
                            }

                            val floatingAlpha by animateFloatAsState(
                                targetValue = if (showFloatingControls) 1f else 0f,
                                animationSpec = tween(320),
                                label = "libraryFloatingAlpha"
                            )
                            val floatingOffset by animateFloatAsState(
                                targetValue = if (showFloatingControls) 0f else 28f,
                                animationSpec = tween(360),
                                label = "libraryFloatingOffset"
                            )

                            Surface(
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                tonalElevation = 3.dp,
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 18.dp)
                                    .graphicsLayer {
                                        alpha = floatingAlpha
                                        translationY = floatingOffset
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val shuffleActiveColor by animateColorAsState(
                                        targetValue = if (shuffleEnabled) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            Color.Transparent
                                        },
                                        animationSpec = tween(180),
                                        label = "shuffleBackground"
                                    )
                                    val shuffleIconColor by animateColorAsState(
                                        targetValue = if (shuffleEnabled) {
                                            MaterialTheme.colorScheme.onPrimary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        animationSpec = tween(180),
                                        label = "shuffleIcon"
                                    )
                                    val repeatActiveColor by animateColorAsState(
                                        targetValue = if (repeatMode != Player.REPEAT_MODE_OFF) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            Color.Transparent
                                        },
                                        animationSpec = tween(180),
                                        label = "repeatBackground"
                                    )
                                    val repeatIconColor by animateColorAsState(
                                        targetValue = if (repeatMode != Player.REPEAT_MODE_OFF) {
                                            MaterialTheme.colorScheme.onPrimary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        animationSpec = tween(180),
                                        label = "repeatIcon"
                                    )

                                    val shuffleScale by animateFloatAsState(
                                        targetValue = if (shuffleEnabled) 1.08f else 1f,
                                        animationSpec = tween(180),
                                        label = "shuffleScale"
                                    )
                                    val repeatScale by animateFloatAsState(
                                        targetValue = if (repeatMode != Player.REPEAT_MODE_OFF) 1.08f else 1f,
                                        animationSpec = tween(180),
                                        label = "repeatScale"
                                    )

                                    androidx.compose.material3.Surface(
                                        onClick = onToggleShuffle,
                                        enabled = showFloatingControls,
                                        shape = CircleShape,
                                        color = shuffleActiveColor,
                                        tonalElevation = 0.dp,
                                        modifier = Modifier.size(42.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                Icons.Default.Shuffle,
                                                contentDescription = if (shuffleEnabled) "Shuffle on" else "Shuffle off",
                                                tint = shuffleIconColor,
                                                modifier = Modifier
                                                    .size(20.dp)
                                                    .graphicsLayer {
                                                        scaleX = shuffleScale
                                                        scaleY = shuffleScale
                                                    }
                                            )
                                        }
                                    }

                                    Spacer(Modifier.width(2.dp))

                                    androidx.compose.material3.Surface(
                                        onClick = onToggleRepeat,
                                        enabled = showFloatingControls,
                                        shape = CircleShape,
                                        color = repeatActiveColor,
                                        tonalElevation = 0.dp,
                                        modifier = Modifier.size(42.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                if (repeatMode == Player.REPEAT_MODE_ONE) {
                                                    Icons.Default.RepeatOne
                                                } else {
                                                    Icons.Default.Repeat
                                                },
                                                contentDescription = when (repeatMode) {
                                                    Player.REPEAT_MODE_ONE -> "Repeat one"
                                                    Player.REPEAT_MODE_ALL -> "Repeat all"
                                                    else -> "Repeat off"
                                                },
                                                tint = repeatIconColor,
                                                modifier = Modifier
                                                    .size(20.dp)
                                                    .graphicsLayer {
                                                        scaleX = repeatScale
                                                        scaleY = repeatScale
                                                    }
                                            )
                                        }
                                    }
                                }
                            }

                            val backButtonAlpha by animateFloatAsState(
                                targetValue = if (showBackToPlayer) 1f else 0f,
                                animationSpec = tween(300),
                                label = "libraryBackAlpha"
                            )
                            val backButtonOffset by animateFloatAsState(
                                targetValue = if (showBackToPlayer) 0f else 26f,
                                animationSpec = tween(340),
                                label = "libraryBackOffset"
                            )

                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(end = 16.dp, bottom = 18.dp)
                                    .graphicsLayer {
                                        alpha = backButtonAlpha
                                        translationY = backButtonOffset
                                    }
                            ) {
                                FilledIconButton(
                                    enabled = showBackToPlayer,
                                    onClick = {
                                        scope.launch {
                                            pagerState.animateScrollToPage(0)
                                        }
                                    },
                                    modifier = Modifier.size(46.dp),
                                    shape = CircleShape
                                ) {
                                    Icon(
                                        Icons.Default.KeyboardArrowUp,
                                        contentDescription = "Back to player",
                                        modifier = Modifier.size(25.dp)
                                    )
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
private fun LyricsScreen(
    song: Song?,
    positionMs: Long,
    durationMs: Long,
    isFavorite: Boolean,
    sheetProgress: Float,
    onBack: () -> Unit,
    onShare: (Song?) -> Unit,
    onProgressChange: (Float) -> Unit,
    maxHeightPx: Float
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var result by remember(song?.uri) { mutableStateOf<com.yohandeku32.nusamusic.data.LyricsResult?>(null) }
    var loading by remember(song?.uri) { mutableStateOf(false) }
    val lyricListState = androidx.compose.foundation.lazy.rememberLazyListState()

    LaunchedEffect(song?.uri) {
        result = null
        loading = song != null
        result = song?.let { LyricsLoader.load(context, it) }
        loading = false
    }

    val lines = result?.lines.orEmpty()
    val activeIndex by remember(lines) {
        derivedStateOf {
            findActiveLyricIndex(lines, positionMs)
        }
    }

    LaunchedEffect(activeIndex, lines.size) {
        if (activeIndex >= 0 && activeIndex < lines.size) {
            lyricListState.animateScrollToItem(
                index = activeIndex.coerceAtLeast(0),
                scrollOffset = -120
            )
        }
    }

    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .lyricsVerticalDrag(
                progress = { sheetProgress },
                maxHeightPx = maxHeightPx,
                onProgressChange = onProgressChange,
                onDragStopped = { velocity ->
                    if (velocity > 900f) onBack()
                }
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(188.dp)
                    .clip(
                        RoundedCornerShape(
                            bottomStart = 34.dp,
                            bottomEnd = 34.dp
                        )
                    )
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Default.KeyboardArrowDown,
                            contentDescription = "Back to player",
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(Modifier.weight(1f))

                    IconButton(
                        onClick = { onShare(song) },
                        enabled = song != null
                    ) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = "Share song",
                            modifier = Modifier.size(21.dp)
                        )
                    }

                    IconButton(
                        onClick = { },
                        enabled = song != null
                    ) {
                        Icon(
                            if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            modifier = Modifier.size(22.dp),
                            tint = if (isFavorite) Color(0xFFC62828) else MaterialTheme.colorScheme.onBackground
                        )
                    }
                }

                Spacer(Modifier.height(7.dp))

                Text(
                    text = song?.title ?: "Lyrics",
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 27.sp,
                    lineHeight = 29.sp,
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                Text(
                    text = song?.artist ?: "",
                    fontSize = 12.sp,
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (loading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Loading lyrics…",
                        color = Color(0xFF8A8A8A),
                        fontSize = 14.sp
                    )
                }
            } else if (lines.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No lyrics found",
                        color = Color(0xFF8A8A8A),
                        fontSize = 15.sp
                    )
                }
            } else {
                androidx.compose.foundation.lazy.LazyColumn(
                    state = lyricListState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(
                        start = 38.dp,
                        end = 38.dp,
                        top = 34.dp,
                        bottom = 150.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(
                        items = lines,
                        key = { _, line -> "${line.startMs}-${line.text}" }
                    ) { index, line ->
                        val distance = if (activeIndex >= 0) {
                            abs(index - activeIndex)
                        } else {
                            99
                        }

                        val alpha = when {
                            index == activeIndex -> 1f
                            distance == 1 -> 0.72f
                            distance == 2 -> 0.46f
                            else -> 0.24f
                        }

                        Text(
                            text = line.text,
                            color = Color.White.copy(alpha = alpha),
                            fontSize = if (index == activeIndex) 18.sp else 17.sp,
                            fontWeight = if (index == activeIndex) {
                                FontWeight.Medium
                            } else {
                                FontWeight.Normal
                            },
                            lineHeight = 24.sp,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

        }
    }
}

@Composable
private fun Modifier.lyricsVerticalDrag(
    progress: () -> Float,
    maxHeightPx: Float,
    onProgressChange: (Float) -> Unit,
    onDragStopped: suspend kotlinx.coroutines.CoroutineScope.(Float) -> Unit
): Modifier {
    if (maxHeightPx <= 0f) return this

    return draggable(
        orientation = Orientation.Vertical,
        state = rememberDraggableState { delta ->
            val next = (progress() - delta / maxHeightPx).coerceIn(0f, 1f)
            onProgressChange(next)
        },
        onDragStopped = onDragStopped,
        startDragImmediately = false
    )
}

private fun findActiveLyricIndex(
    lines: List<LyricLine>,
    positionMs: Long
): Int {
    var low = 0
    var high = lines.lastIndex
    var answer = -1

    while (low <= high) {
        val mid = (low + high) ushr 1
        if (lines[mid].startMs <= positionMs) {
            answer = mid
            low = mid + 1
        } else {
            high = mid - 1
        }
    }

    return answer
}




@Composable
private fun AudioQualityPill(song: Song?) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var codecInfo by remember(song?.uri) { mutableStateOf<AudioCodecInfo?>(null) }

    LaunchedEffect(song?.uri) {
        codecInfo = song?.let {
            AudioCodecLoader.load(
                context = context,
                uriString = it.uri
            )
        }
    }

    val info = codecInfo ?: return

    // Only ALAC/FLAC receive an Apple-style Lossless badge.
    // 24-bit and above is Hi-Res Lossless; anything below 24-bit is Lossless.
    val isLossless = info.codecName == "Apple Lossless" || info.codecName == "FLAC"
    if (!isLossless) return

    val label = if ((info.bitDepth ?: 16) >= 24) {
        "Hi-Res Lossless"
    } else {
        "Lossless"
    }

    androidx.compose.material3.Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 0.dp,
        modifier = Modifier.padding(top = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            androidx.compose.foundation.Image(
                painter = painterResource(id = R.drawable.apple_lossless_logo),
                contentDescription = label,
                contentScale = ContentScale.Fit,
                colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(
                    MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.size(
                    width = 24.dp,
                    height = 13.dp
                )
            )

            Spacer(Modifier.width(5.dp))

            Text(
                text = label,
                fontSize = 10.sp,
                lineHeight = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }

    Spacer(Modifier.height(4.dp))
}

@Composable
private fun ArtistAvatar(song: Song?, modifier: Modifier = Modifier) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var artistBitmap by remember(song?.artist) { mutableStateOf<Bitmap?>(null) }
    val grayscaleMatrix = remember {
        ColorMatrix().apply { setToSaturation(0f) }
    }
    val grayscaleFilter = remember(grayscaleMatrix) {
        ColorFilter.colorMatrix(grayscaleMatrix)
    }

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
            colorFilter = grayscaleFilter,
            modifier = modifier
        )
    } else {
        // Keep the avatar monochrome even while the artist portrait is being
        // resolved and when album art is used as the fallback.
        ArtworkView(
            song = song,
            maxSizePx = 96,
            modifier = modifier,
            monochrome = true
        )
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

            // Real-record style lighting: deep black PVC with a soft off-axis
            // hotspot so the disc has a rounded, glossy surface.
            drawCircle(
                brush = Brush.radialGradient(
                    colorStops = arrayOf(
                        0.00f to Color(0xFF262626),
                        0.34f to Color(0xFF111111),
                        0.72f to Color(0xFF040404),
                        1.00f to Color(0xFF000000)
                    ),
                    center = androidx.compose.ui.geometry.Offset(
                        size.width * 0.34f,
                        size.height * 0.30f
                    ),
                    radius = radius * 1.05f
                ),
                radius = radius
            )

            // Low-contrast molded-groove field. Fewer strokes keep scrolling and
            // rotation smooth while the surface still reads as pressed vinyl.
            for (i in 0..118) {
                val t = i / 118f
                val grooveRadius = radius * (0.245f + t * 0.715f)
                val alpha = when {
                    i % 31 == 0 -> 0.070f
                    i % 9 == 0 -> 0.030f
                    i % 3 == 0 -> 0.015f
                    else -> 0.008f
                }

                drawCircle(
                    color = Color.White.copy(alpha = alpha),
                    radius = grooveRadius,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = if (i % 31 == 0) 0.85f else 0.32f
                    )
                )
            }

            // Micro-reflections across the groove bands. These are deliberately
            // broken up so the highlight does not look like perfect CAD rings.
            for (i in 0 until 24) {
                val startAngle = (i * 149f + (i % 5) * 7f) % 360f
                val sweepAngle = 14f + (i % 6) * 8f
                val arcRadius = radius * (0.39f + ((i * 19) % 48) / 100f)

                drawArc(
                    color = Color.White.copy(alpha = 0.014f + (i % 4) * 0.004f),
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
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 0.85f)
                )
            }

            // Large curved specular reflection. The reference record has a
            // visible glossy streak following the circular groove direction.
            drawArc(
                color = Color.White.copy(alpha = 0.135f),
                startAngle = -76f,
                sweepAngle = 34f,
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(
                    size.width * 0.02f,
                    size.height * 0.02f
                ),
                size = androidx.compose.ui.geometry.Size(
                    size.width * 0.96f,
                    size.height * 0.96f
                ),
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = 8.5f
                )
            )

            drawArc(
                color = Color.White.copy(alpha = 0.075f),
                startAngle = -66f,
                sweepAngle = 56f,
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(
                    size.width * 0.075f,
                    size.height * 0.075f
                ),
                size = androidx.compose.ui.geometry.Size(
                    size.width * 0.85f,
                    size.height * 0.85f
                ),
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = 4.0f
                )
            )

            drawArc(
                color = Color.White.copy(alpha = 0.035f),
                startAngle = -54f,
                sweepAngle = 82f,
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(
                    size.width * 0.16f,
                    size.height * 0.16f
                ),
                size = androidx.compose.ui.geometry.Size(
                    size.width * 0.68f,
                    size.height * 0.68f
                ),
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = 2.2f
                )
            )

            // Subtle dark falloff on the opposite side gives the record depth
            // instead of a uniformly lit black circle.
            drawArc(
                color = Color.Black.copy(alpha = 0.30f),
                startAngle = 108f,
                sweepAngle = 110f,
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(
                    size.width * 0.045f,
                    size.height * 0.045f
                ),
                size = androidx.compose.ui.geometry.Size(
                    size.width * 0.91f,
                    size.height * 0.91f
                ),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 9f)
            )

            // Center well and slight inner lip around the printed label.
            drawCircle(
                color = Color.Black.copy(alpha = 0.56f),
                radius = radius * 0.248f
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.050f),
                radius = radius * 0.258f,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.2f)
            )
            drawCircle(
                color = Color.Black.copy(alpha = 0.60f),
                radius = radius * 0.206f,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.0f)
            )

            // Realistic pressed outer edge.
            drawCircle(
                color = Color.Black.copy(alpha = 0.88f),
                radius = radius * 0.989f,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.2f)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.050f),
                radius = radius * 0.958f,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.0f)
            )
        }

        ArtworkView(
            song = song,
            maxSizePx = 512,
            modifier = Modifier
                .size(142.dp)
                .clip(CircleShape)
        )

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
    modifier: Modifier = Modifier,
    monochrome: Boolean = false
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
                colorFilter = if (monochrome) {
                    ColorFilter.colorMatrix(
                        ColorMatrix().apply { setToSaturation(0f) }
                    )
                } else {
                    null
                },
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
