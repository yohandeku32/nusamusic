package com.yohandeku32.nusamusic

import android.Manifest
import android.content.ComponentName
import android.content.pm.PackageManager
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import java.util.Locale
import kotlin.random.Random
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
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
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
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.zIndex
import androidx.compose.ui.platform.LocalDensity
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
import com.yohandeku32.nusamusic.data.ArtistBiographyLoader
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
                delay(if (isPlaying) 250L else 500L)
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
    var showSettings by remember { mutableStateOf(false) }
    var artistBiography by remember(currentSong?.artist) {
        mutableStateOf<com.yohandeku32.nusamusic.data.ArtistBiography?>(null)
    }
    var biographyLoading by remember(currentSong?.artist) {
        mutableStateOf(false)
    }

    val context = androidx.compose.ui.platform.LocalContext.current
    val uiPrefs = remember(context) {
        context.getSharedPreferences("ui_preferences", android.content.Context.MODE_PRIVATE)
    }
    var titleFontSize by remember {
        mutableStateOf(uiPrefs.getFloat("title_font_size", 34f))
    }
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

    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val libraryCenterOffset = remember(configuration.screenHeightDp, density) {
        with(density) {
            -((configuration.screenHeightDp.dp - 74.dp) / 2f).roundToPx()
        }
    }

    // Prepare the library position silently as soon as the current song
    // changes. There is intentionally NO scroll animation here.
    LaunchedEffect(currentSong?.id, filtered.size) {
        if (currentSong != null) {
            val index = filtered.indexOfFirst { it.id == currentSong.id }
            if (index >= 0) {
                libraryListState.scrollToItem(
                    index = index,
                    scrollOffset = libraryCenterOffset
                )
            }
        }
    }

    LaunchedEffect(currentSong?.artist) {
        artistBiography = null
        biographyLoading = currentSong?.artist?.isNotBlank() == true
        artistBiography = currentSong?.let {
            ArtistBiographyLoader.load(it)
        }
        biographyLoading = false
    }

    if (showSettings) {
        val settingsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        ModalBottomSheet(
            onDismissRequest = { showSettings = false },
            sheetState = settingsSheetState,
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.onBackground
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 24.dp)
            ) {
                Text(
                    "Settings",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(18.dp))

                Text(
                    "UKURAN JUDUL LAGU",
                    fontSize = 11.sp,
                    letterSpacing = 1.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${titleFontSize.toInt()} sp",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                androidx.compose.material3.Slider(
                    value = titleFontSize,
                    onValueChange = { titleFontSize = it },
                    onValueChangeFinished = {
                        uiPrefs.edit()
                            .putFloat("title_font_size", titleFontSize)
                            .apply()
                    },
                    valueRange = 24f..44f,
                    steps = 19
                )

                Text(
                    "Atur besar-kecil judul lagu.",
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(22.dp))

                Text(
                    "ABOUT NUSA MUSIC",
                    fontSize = 11.sp,
                    letterSpacing = 1.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))

                Text(
                    "Nusa Music adalah pemutar musik lokal Android yang dirancang dengan fokus pada pengalaman mendengarkan musik yang bersih dan sederhana.",
                    fontSize = 14.sp,
                    lineHeight = 21.sp
                )
                Spacer(Modifier.height(18.dp))

                Text(
                    "Developer",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "yohandeku32",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(Modifier.height(16.dp))

                Text(
                    "Project",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Nusa Music  •  Version 1.0",
                    fontSize = 15.sp
                )

                Spacer(Modifier.height(18.dp))

                Text(
                    "Technology",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Kotlin • Jetpack Compose • Material 3 • Media3",
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )

                Spacer(Modifier.height(22.dp))

                Text(
                    "Artist biographies are provided through Last.fm when configured.",
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
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
                                        colors = TopAppBarDefaults.topAppBarColors(
                                            containerColor = MaterialTheme.colorScheme.background,
                                            scrolledContainerColor = MaterialTheme.colorScheme.background
                                        ),
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
                                            IconButton(onClick = { showSettings = true }) {
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

                                            val titleSize = remember(titleWordCount, titleFontSize) {
                                                val scale = titleFontSize / 34f
                                                when {
                                                    titleWordCount <= 2 -> titleFontSize.sp
                                                    titleWordCount == 3 -> (31f * scale).sp
                                                    else -> (28f * scale).sp
                                                }
                                            }

                                            val titleLineHeight = remember(titleWordCount, titleFontSize) {
                                                val scale = titleFontSize / 34f
                                                when {
                                                    titleWordCount <= 2 -> (37f * scale).sp
                                                    titleWordCount == 3 -> (34f * scale).sp
                                                    else -> (31f * scale).sp
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
                                                icon = Icons.Default.FastRewind,
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
                                                icon = Icons.Default.FastForward,
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
                                                        // Open the artist biography section directly.
                                                        playerScrollState.animateScrollToItem(
                                                            index = 1,
                                                            scrollOffset = 0
                                                        )
                                                    }
                                                },
                                                modifier = Modifier.size(42.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.KeyboardArrowDown,
                                                    contentDescription = "Expand artist biography",
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

                            // Artist biography section.
                            item {
                                ArtistBiographySection(
                                    artistName = currentSong?.artist,
                                    biography = artistBiography,
                                    loading = biographyLoading
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
                            RecordCrateBackdrop(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .fillMaxWidth(0.94f)
                                    .fillMaxHeight(0.84f)
                            )

                            Text(
                                text = "DAFTAR LAGU",
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(top = 28.dp),
                                fontSize = 21.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.8.sp,
                                color = Color.White
                            )

                            LazyColumn(
                                state = libraryListState,
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(
                                    top = 112.dp,
                                    bottom = 160.dp
                                )
                            ) {
                                if (permissionGranted && filtered.isNotEmpty()) {
                                    itemsIndexed(
                                        items = filtered,
                                        key = { _, song -> song.id },
                                        contentType = { _, _ -> "song" }
                                    ) { index, song ->
                                        val itemInfo =
                                            libraryListState.layoutInfo.visibleItemsInfo
                                                .firstOrNull { it.index == index }
                                        val viewportCenter =
                                            (libraryListState.layoutInfo.viewportStartOffset +
                                                libraryListState.layoutInfo.viewportEndOffset) / 2f
                                        val itemCenter =
                                            itemInfo?.let {
                                                it.offset + it.size / 2f
                                            } ?: viewportCenter
                                        val distancePx = itemCenter - viewportCenter
                                        val maxStackDistancePx = with(density) { 300.dp.toPx() }
                                        val normalizedDistance =
                                            (distancePx / maxStackDistancePx).coerceIn(-1f, 1f)

                                        // The closest sleeve becomes the front record in the
                                        // crate. Neighboring sleeves remain visible as a stack.
                                        val distanceFromCenter =
                                            kotlin.math.abs(normalizedDistance)
                                        val centerProximity =
                                            1f - distanceFromCenter
                                        val selected = currentSong?.id == song.id

                                        val horizontalShift =
                                            -normalizedDistance * 8f
                                        val rotationY =
                                            -normalizedDistance * 10f
                                        val rotationZ =
                                            -normalizedDistance * 2.5f
                                        val scale =
                                            (0.84f + centerProximity * 0.13f +
                                                if (selected) 0.04f else 0f)
                                                .coerceIn(0.82f, 1.02f)
                                        val alpha =
                                            (0.60f + centerProximity * 0.40f)
                                                .coerceIn(0.55f, 1f)
                                        val z =
                                            centerProximity * 20f +
                                                if (selected) 10f else 0f

                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(74.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(230.dp)
                                                    .graphicsLayer {
                                                        translationX = with(density) {
                                                            horizontalShift.dp.toPx()
                                                        }
                                                        rotationY = rotationY
                                                        rotationZ = rotationZ
                                                        scaleX = scale
                                                        scaleY = scale
                                                        this.alpha = alpha
                                                        cameraDistance = 32f * density.density
                                                    }
                                                    .zIndex(z)
                                            ) {
                                                SongRow(
                                                    song = song,
                                                    selected = selected,
                                                    onPlay = onPlay,
                                                    darkSurface = true,
                                                    sleeveStack = true,
                                                    depth = z
                                                )
                                            }
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
private fun ArtistBiographySection(
    artistName: String?,
    biography: com.yohandeku32.nusamusic.data.ArtistBiography?,
    loading: Boolean
) {
    val displayArtist = artistName?.trim().orEmpty().ifBlank { "Unknown artist" }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black)
            .padding(horizontal = 28.dp, vertical = 24.dp)
    ) {
        if (loading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Loading artist biography…",
                    color = Color(0xFF9A9A9A),
                    fontSize = 14.sp
                )
            }
        } else if (biography == null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 72.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = displayArtist,
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    if (ArtistBiographyLoader.isConfigured()) {
                        "Biography not available"
                    } else {
                        "Add LASTFM_API_KEY to local.properties"
                    },
                    color = Color(0xFF8A8A8A),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            Text(
                "ABOUT THE ARTIST",
                color = Color(0xFF8E8E8E),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.6.sp
            )
            Spacer(Modifier.height(7.dp))
            Text(
                text = biography.artistName,
                color = Color.White,
                fontSize = 24.sp,
                lineHeight = 28.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(178.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF111111))
                    .padding(horizontal = 18.dp, vertical = 16.dp)
            ) {
                Text(
                    text = biography.text,
                    color = Color(0xFFE7E7E7),
                    fontSize = 15.sp,
                    lineHeight = 23.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 6,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
            Text(
                text = "Source: Last.fm (" + biography.sourceLanguage.uppercase() + ")",
                color = Color(0xFF777777),
                fontSize = 11.sp,
                modifier = Modifier.padding(start = 4.dp, top = 8.dp)
            )
        }
    }
}

@Composable
private fun AudioQualityPill(song: Song?) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var codecInfo by remember { mutableStateOf<AudioCodecInfo?>(null) }

    LaunchedEffect(song?.uri) {
        codecInfo = song?.let {
            AudioCodecLoader.load(
                context = context,
                uriString = it.uri
            )
        }
    }

    val info = codecInfo
    val isLossless = info?.codecName == "Apple Lossless" || info?.codecName == "FLAC"
    val isHiRes = isLossless && (info?.bitDepth ?: 16) >= 24
    val label = if (isHiRes) "Hi-Res Lossless" else "Lossless"

    androidx.compose.animation.AnimatedVisibility(
        visible = isLossless,
        enter = androidx.compose.animation.fadeIn(
            animationSpec = tween(durationMillis = 180)
        ),
        exit = androidx.compose.animation.fadeOut(
            animationSpec = tween(durationMillis = 180)
        )
    ) {
        Crossfade(
            targetState = isHiRes,
            animationSpec = tween(durationMillis = 180),
            label = "qualityBadgeCrossfade"
        ) { hiRes ->
            Box(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .clip(RoundedCornerShape(50))
                    .background(
                        color = if (hiRes) {
                            Color(0xFFB5A77C).copy(alpha = 0.42f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        }
                    )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    androidx.compose.foundation.Image(
                        painter = painterResource(id = R.drawable.apple_lossless_logo),
                        contentDescription = if (hiRes) "Hi-Res Lossless" else "Lossless",
                        contentScale = ContentScale.Fit,
                        colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(
                            if (hiRes) Color(0xFF3D3728) else MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.size(
                            width = 24.dp,
                            height = 13.dp
                        )
                    )

                    Spacer(Modifier.width(5.dp))

                    Text(
                        text = if (hiRes) "Hi-Res Lossless" else "Lossless",
                        fontSize = 10.sp,
                        lineHeight = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (hiRes) {
                            Color(0xFF3D3728)
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                }
            }
        }
    }

    if (isLossless) {
        Spacer(Modifier.height(4.dp))
    }
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
private fun ProgressStyleOption(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(46.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        },
        tonalElevation = 0.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                title,
                fontSize = 14.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
            )
        }
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
            .height(28.dp)
            .pointerInput(durationMs, enabled) {
                if (enabled && durationMs > 0L) {
                    detectTapGestures { offset ->
                        val tappedFraction =
                            (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                        onSeek((tappedFraction * durationMs).toLong())
                    }
                }
            }
            .pointerInput(durationMs, enabled) {
                if (enabled && durationMs > 0L) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val fraction =
                                (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                            onSeek((fraction * durationMs).toLong())
                        },
                        onDragEnd = {},
                        onDragCancel = {},
                        onDrag = { change, _ ->
                            change.consume()
                            val fraction =
                                (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                            onSeek((fraction * durationMs).toLong())
                        }
                    )
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
                modifier = Modifier
                    .size(28.dp)
                    .graphicsLayer {
                        scaleX = 1.05f
                        scaleY = 1.05f
                    }
            )
        }
    }
}

@Composable
private fun VinylRecord(song: Song?, isPlaying: Boolean, modifier: Modifier = Modifier) {
    // Preserve the physical angle when pausing and resuming playback.
    val rotation = remember { Animatable(0f) }

    // Deterministic surface texture so the disc keeps the same physical
    // micro-detail while it rotates.
    val grain = remember {
        val random = Random(417)
        List(720) {
            floatArrayOf(
                random.nextFloat(),
                random.nextFloat(),
                random.nextFloat(),
                random.nextFloat()
            )
        }
    }

    // A second deterministic set of marks gives the PVC a used, physical feel:
    // faint hairline scuffs, sleeve rubs and small circular scratches.
    val scratches = remember {
        val random = Random(918)
        List(54) {
            floatArrayOf(
                random.nextFloat() * 360f,
                4f + random.nextFloat() * 18f,
                0.35f + random.nextFloat() * 0.58f,
                0.15f + random.nextFloat() * 0.75f,
                0.30f + random.nextFloat() * 1.8f
            )
        }
    }

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
            val centerX = size.width / 2f
            val centerY = size.height / 2f
            val discCenter = androidx.compose.ui.geometry.Offset(centerX, centerY)

            // -------------------------------------------------------------
            // PHYSICAL THICKNESS / EDGE WALL
            // -------------------------------------------------------------
            // The offset dark layer creates a visible lower edge so the
            // record reads as a thin, real piece of PVC instead of a flat
            // black circle.
            val edgeOffset = (radius * 0.030f).coerceAtLeast(1.5f)

            drawCircle(
                color = Color(0xFF090909),
                center = androidx.compose.ui.geometry.Offset(
                    centerX,
                    centerY + edgeOffset
                ),
                radius = radius * 0.992f
            )

            drawArc(
                color = Color.Black.copy(alpha = 0.66f),
                startAngle = 12f,
                sweepAngle = 156f,
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(
                    centerX - radius * 0.992f,
                    centerY + edgeOffset - radius * 0.992f
                ),
                size = androidx.compose.ui.geometry.Size(
                    radius * 1.984f,
                    radius * 1.984f
                ),
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = radius * 0.032f
                )
            )

            drawArc(
                color = Color.White.copy(alpha = 0.075f),
                startAngle = 190f,
                sweepAngle = 145f,
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(
                    centerX - radius * 0.992f,
                    centerY + edgeOffset - radius * 0.992f
                ),
                size = androidx.compose.ui.geometry.Size(
                    radius * 1.984f,
                    radius * 1.984f
                ),
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = radius * 0.010f
                )
            )

            // Main pressed PVC face.
            drawCircle(
                brush = Brush.radialGradient(
                    colorStops = arrayOf(
                        0.00f to Color(0xFF323232),
                        0.24f to Color(0xFF1A1A1A),
                        0.52f to Color(0xFF090909),
                        0.78f to Color(0xFF020202),
                        1.00f to Color(0xFF000000)
                    ),
                    center = androidx.compose.ui.geometry.Offset(
                        size.width * 0.32f,
                        size.height * 0.27f
                    ),
                    radius = radius * 1.08f
                ),
                radius = radius
            )

            // Gentle reflected light across the lacquered PVC.
            drawCircle(
                brush = Brush.radialGradient(
                    colorStops = arrayOf(
                        0.00f to Color.White.copy(alpha = 0.085f),
                        0.22f to Color.White.copy(alpha = 0.042f),
                        0.52f to Color.White.copy(alpha = 0.012f),
                        1.00f to Color.Transparent
                    ),
                    center = androidx.compose.ui.geometry.Offset(
                        size.width * 0.27f,
                        size.height * 0.19f
                    ),
                    radius = radius * 0.90f
                ),
                radius = radius
            )

            // -------------------------------------------------------------
            // PRESSED GROOVES
            // -------------------------------------------------------------
            // Close, slightly irregular reflective rings imitate real
            // pressed grooves instead of a perfectly smooth digital disc.
            for (i in 0..178) {
                val t = i / 178f
                val grooveRadius = radius * (0.232f + t * 0.742f)
                val alpha = when {
                    i % 31 == 0 -> 0.082f
                    i % 13 == 0 -> 0.042f
                    i % 5 == 0 -> 0.019f
                    else -> 0.0075f
                }

                drawCircle(
                    color = Color.White.copy(alpha = alpha),
                    radius = grooveRadius,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = if (i % 31 == 0) 0.90f else 0.28f
                    )
                )

                // Dark companion line on alternating groove bands gives
                // the grooves actual depth.
                if (i % 7 == 0) {
                    drawCircle(
                        color = Color.Black.copy(alpha = 0.20f),
                        radius = grooveRadius + 0.55f,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = 0.65f
                        )
                    )
                }
            }

            // Inner run-out / label transition.
            drawCircle(
                color = Color.Black.copy(alpha = 0.35f),
                radius = radius * 0.305f,
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = 2.0f
                )
            )

            drawCircle(
                color = Color.White.copy(alpha = 0.050f),
                radius = radius * 0.321f,
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = 1.0f
                )
            )

            // -------------------------------------------------------------
            // PVC GRAIN
            // -------------------------------------------------------------
            for (sample in grain) {
                val x = sample[0] * size.width
                val y = sample[1] * size.height
                val dx = x - centerX
                val dy = y - centerY

                if (dx * dx + dy * dy <= radius * radius * 0.968f) {
                    val isLight = sample[3] > 0.46f
                    val alpha = if (isLight) {
                        0.014f + sample[2] * 0.022f
                    } else {
                        0.008f + sample[2] * 0.014f
                    }
                    val pointRadius = 0.20f + sample[2] * 0.50f

                    drawCircle(
                        color = if (isLight) {
                            Color.White.copy(alpha = alpha)
                        } else {
                            Color.Black.copy(alpha = alpha)
                        },
                        radius = pointRadius,
                        center = androidx.compose.ui.geometry.Offset(x, y)
                    )
                }
            }

            // -------------------------------------------------------------
            // REAL-WORLD SCRATCHES / SLEEVE RUB
            // -------------------------------------------------------------
            // Mostly radial/circular micro-scuffs. They are intentionally
            // faint: they should appear when the record catches light, not
            // make it look dirty.
            for (mark in scratches) {
                val angle = mark[0]
                val radiusFactor = mark[2]
                val sweep = mark[1]
                val lengthJitter = mark[3]
                val width = mark[4] * 0.32f

                val arcRadius = radius * radiusFactor
                val startAngle = angle
                val sweepAngle = sweep * (0.55f + lengthJitter * 0.55f)

                drawArc(
                    color = Color.White.copy(alpha = 0.010f + lengthJitter * 0.010f),
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = androidx.compose.ui.geometry.Offset(
                        centerX - arcRadius,
                        centerY - arcRadius
                    ),
                    size = androidx.compose.ui.geometry.Size(
                        arcRadius * 2f,
                        arcRadius * 2f
                    ),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = width.coerceAtLeast(0.45f)
                    )
                )
            }

            // A few very fine directional hairlines break the otherwise
            // mathematically perfect surface.
            for (i in 0 until 14) {
                val angle = Math.toRadians((i * 43f + 17f).toDouble())
                val inner = radius * (0.40f + (i % 4) * 0.075f)
                val outer = (inner + radius * (0.075f + (i % 5) * 0.022f))
                    .coerceAtMost(radius * 0.94f)

                val x1 = centerX + kotlin.math.cos(angle).toFloat() * inner
                val y1 = centerY + kotlin.math.sin(angle).toFloat() * inner
                val x2 = centerX + kotlin.math.cos(angle).toFloat() * outer
                val y2 = centerY + kotlin.math.sin(angle).toFloat() * outer

                drawLine(
                    color = Color.White.copy(alpha = 0.017f),
                    start = androidx.compose.ui.geometry.Offset(x1, y1),
                    end = androidx.compose.ui.geometry.Offset(x2, y2),
                    strokeWidth = 0.55f
                )
            }

            // Broken micro-reflections across the groove bands.
            for (i in 0 until 30) {
                val startAngle = (i * 137f + (i % 7) * 9f) % 360f
                val sweepAngle = 10f + (i % 8) * 7f
                val arcRadius = radius * (0.36f + ((i * 17) % 54) / 100f)

                drawArc(
                    color = Color.White.copy(alpha = 0.012f + (i % 5) * 0.004f),
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = androidx.compose.ui.geometry.Offset(
                        centerX - arcRadius,
                        centerY - arcRadius
                    ),
                    size = androidx.compose.ui.geometry.Size(
                        arcRadius * 2f,
                        arcRadius * 2f
                    ),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = 0.75f
                    )
                )
            }

            // Broad glossy highlight that travels with the rotating record.
            drawArc(
                color = Color.White.copy(alpha = 0.23f),
                startAngle = -80f,
                sweepAngle = 36f,
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(
                    size.width * 0.015f,
                    size.height * 0.015f
                ),
                size = androidx.compose.ui.geometry.Size(
                    size.width * 0.97f,
                    size.height * 0.97f
                ),
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = 9.5f
                )
            )

            drawArc(
                color = Color.White.copy(alpha = 0.115f),
                startAngle = -68f,
                sweepAngle = 62f,
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
                    width = 4.5f
                )
            )

            drawArc(
                color = Color.White.copy(alpha = 0.052f),
                startAngle = -52f,
                sweepAngle = 88f,
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(
                    size.width * 0.15f,
                    size.height * 0.15f
                ),
                size = androidx.compose.ui.geometry.Size(
                    size.width * 0.70f,
                    size.height * 0.70f
                ),
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = 2.3f
                )
            )

            // Thin specular streak.
            drawArc(
                color = Color.White.copy(alpha = 0.14f),
                startAngle = -72f,
                sweepAngle = 18f,
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(
                    size.width * 0.09f,
                    size.height * 0.09f
                ),
                size = androidx.compose.ui.geometry.Size(
                    size.width * 0.82f,
                    size.height * 0.82f
                ),
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = 1.7f
                )
            )

            // Dark falloff on the far side of the disc.
            drawArc(
                color = Color.Black.copy(alpha = 0.36f),
                startAngle = 108f,
                sweepAngle = 116f,
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(
                    size.width * 0.04f,
                    size.height * 0.04f
                ),
                size = androidx.compose.ui.geometry.Size(
                    size.width * 0.92f,
                    size.height * 0.92f
                ),
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = 10f
                )
            )

            // -------------------------------------------------------------
            // CENTER WELL / PRESSURE RING / OUTER BEVEL
            // -------------------------------------------------------------
            drawCircle(
                color = Color.Black.copy(alpha = 0.64f),
                radius = radius * 0.248f
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.067f),
                radius = radius * 0.259f,
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = 1.35f
                )
            )
            drawCircle(
                color = Color.Black.copy(alpha = 0.70f),
                radius = radius * 0.205f,
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = 1.15f
                )
            )

            // Strong outer bevel + fine top-edge catch light.
            drawCircle(
                color = Color.Black.copy(alpha = 0.92f),
                radius = radius * 0.989f,
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = 3.6f
                )
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.065f),
                radius = radius * 0.957f,
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = 1.15f
                )
            )

            // Tiny lower-right edge reflection reinforces the record's
            // thickness when it is rotating under the light.
            drawArc(
                color = Color.White.copy(alpha = 0.095f),
                startAngle = 18f,
                sweepAngle = 70f,
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(
                    centerX - radius * 0.973f,
                    centerY - radius * 0.973f
                ),
                size = androidx.compose.ui.geometry.Size(
                    radius * 1.946f,
                    radius * 1.946f
                ),
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = 2.0f
                )
            )
        }

        // Center label/artwork remains physically raised above the PVC face.
        ArtworkView(
            song = song,
            maxSizePx = 512,
            modifier = Modifier
                .size(142.dp)
                .clip(CircleShape)
        )

        // Metal spindle and hole.
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
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }

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
        Crossfade(
            targetState = bitmap,
            animationSpec = tween(durationMillis = 220),
            label = "artworkCrossfade"
        ) { targetBitmap ->
            if (targetBitmap != null) {
                androidx.compose.foundation.Image(
                    bitmap = targetBitmap.asImageBitmap(),
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
}

@Composable
private fun SongRow(
    song: Song,
    selected: Boolean,
    onPlay: (Song) -> Unit,
    darkSurface: Boolean = false,
    titleSize: Float = 14f,
    coverScale: Float = 1f,
    sleeveStack: Boolean = false,
    depth: Float = 0f
) {
    if (sleeveStack) {
        CrateSongSleeve(
            song = song,
            selected = selected,
            onPlay = onPlay,
            depth = depth
        )
        return
    }

    val selectedBackground =
        if (darkSurface) Color(0xFF1B1B1B) else MaterialTheme.colorScheme.surfaceVariant

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (selected) selectedBackground.copy(alpha = 0.96f)
                else Color(0xFF0D0D0D).copy(alpha = 0.72f)
            )
            .clickable { onPlay(song) }
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(78.dp * coverScale)
                .height(74.dp * coverScale)
        ) {
            Box(
                modifier = Modifier
                    .size(68.dp * coverScale)
                    .offset(
                        x = 6.dp + (depth * 0.22f).dp,
                        y = 4.dp
                    )
                    .clip(RoundedCornerShape(5.dp))
                    .background(Color(0xFF242424))
            )
            Box(
                modifier = Modifier
                    .size(70.dp * coverScale)
                    .offset(x = 3.dp, y = 2.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(Color(0xFF0B0B0B))
            )
            Box(
                modifier = Modifier
                    .size(70.dp * coverScale)
                    .clip(RoundedCornerShape(5.dp))
                    .background(Color(0xFF050505))
                    .padding(3.dp)
            ) {
                ArtworkView(
                    song = song,
                    maxSizePx = 192,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(2.dp))
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .offset(y = 3.dp)
                    .background(Color.White.copy(alpha = 0.18f))
            )
        }

        Spacer(Modifier.width(14.dp))

        Column(Modifier.weight(1f)) {
            Text(
                song.title,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                fontSize = titleSize.sp,
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

@Composable
private fun RecordCrateBackdrop(modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF3A2418),
                            Color(0xFF6A452B),
                            Color(0xFF2A1A12)
                        )
                    )
                )
                .border(
                    width = 1.5.dp,
                    color = Color(0xFF8A603A).copy(alpha = 0.75f),
                    shape = RoundedCornerShape(24.dp)
                )
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF17120F),
                            Color(0xFF0A0908),
                            Color(0xFF1B120D)
                        )
                    )
                )
        )

        Box(
            modifier = Modifier
                .width(16.dp)
                .fillMaxHeight()
                .align(Alignment.CenterStart)
                .clip(
                    RoundedCornerShape(
                        topStart = 20.dp,
                        bottomStart = 20.dp
                    )
                )
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color(0xFF7A5131),
                            Color(0xFF3A2418),
                            Color(0xFF1E140E)
                        )
                    )
                )
        )
        Box(
            modifier = Modifier
                .width(16.dp)
                .fillMaxHeight()
                .align(Alignment.CenterEnd)
                .clip(
                    RoundedCornerShape(
                        topEnd = 20.dp,
                        bottomEnd = 20.dp
                    )
                )
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color(0xFF1E140E),
                            Color(0xFF3A2418),
                            Color(0xFF7A5131)
                        )
                    )
                )
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .align(Alignment.BottomCenter)
                .clip(
                    RoundedCornerShape(
                        bottomStart = 22.dp,
                        bottomEnd = 22.dp
                    )
                )
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF70482D),
                            Color(0xFF422817),
                            Color(0xFF24170F)
                        )
                    )
                )
        )

        Box(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .height(3.dp)
                .align(Alignment.TopCenter)
                .offset(y = 18.dp)
                .clip(RoundedCornerShape(50))
                .background(Color(0xFFC08A52).copy(alpha = 0.45f))
        )
    }
}

@Composable
private fun CrateSongSleeve(
    song: Song,
    selected: Boolean,
    onPlay: (Song) -> Unit,
    depth: Float
) {
    val rearOffset = (depth * 0.12f).coerceIn(0f, 3f).dp

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(230.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Box(
            modifier = Modifier
                .size(178.dp)
                .offset(x = 92.dp, y = 28.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF3A3A3A),
                            Color(0xFF161616),
                            Color(0xFF030303)
                        )
                    )
                )
        ) {
            for (ring in 1..7) {
                Box(
                    modifier = Modifier
                        .size((92 + ring * 10).dp)
                        .align(Alignment.Center)
                        .clip(CircleShape)
                        .border(
                            width = 0.8.dp,
                            color = Color.White.copy(alpha = 0.045f)
                        )
                )
            }
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .align(Alignment.Center)
                    .clip(CircleShape)
                    .background(Color(0xFF090909))
            )
        }

        Box(
            modifier = Modifier
                .size(205.dp)
                .offset(x = rearOffset, y = 8.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF111111))
        )
        Box(
            modifier = Modifier
                .size(211.dp)
                .offset(x = (-7).dp, y = 4.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF2A211A))
        )

        Box(
            modifier = Modifier
                .size(216.dp)
                .clip(RoundedCornerShape(8.dp))
                .border(
                    width = if (selected) 2.dp else 1.dp,
                    color = if (selected) {
                        Color.White.copy(alpha = 0.88f)
                    } else {
                        Color(0xFF6A4C35).copy(alpha = 0.70f)
                    },
                    shape = RoundedCornerShape(8.dp)
                )
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF080808),
                            Color(0xFF141414),
                            Color(0xFF050505)
                        )
                    )
                )
                .padding(5.dp)
                .clickable { onPlay(song) }
        ) {
            ArtworkView(
                song = song,
                maxSizePx = 512,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(4.dp))
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .align(Alignment.TopCenter)
                    .background(Color.White.copy(alpha = 0.18f))
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.03f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.16f)
                            )
                        )
                    )
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