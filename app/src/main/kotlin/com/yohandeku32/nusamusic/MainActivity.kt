package com.yohandeku32.nusamusic

import android.Manifest
import android.database.ContentObserver
import android.content.ComponentName
import android.content.pm.PackageManager
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.MediaStore
import java.util.Locale
import kotlin.random.Random
import android.os.Bundle
import android.content.SharedPreferences
import org.json.JSONArray
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
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Sort
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
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.yohandeku32.nusamusic.data.ArtistNameUtils
import com.yohandeku32.nusamusic.data.MusicRepository
import com.yohandeku32.nusamusic.model.Song
import com.yohandeku32.nusamusic.playback.PlaybackService
import com.decent.usbaudio.UsbAudioPermissionHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private fun nusaText(
    indonesian: String,
    english: String
): String {
    return if (Locale.getDefault().language.equals("id", ignoreCase = true)) {
        indonesian
    } else {
        english
    }
}

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
    private val libraryPrefs: SharedPreferences by lazy {
        getSharedPreferences("library_preferences", MODE_PRIVATE)
    }
    private var lastPersistedPosition = -1L
    private var selectedMusicFolders by mutableStateOf<List<String>>(emptyList())
    private var isScanningMusic by mutableStateOf(false)
    private var automaticMusicScanJob: Job? = null
    private var libraryCacheLoaded = false
    private var scanAfterCacheLoad = false

    private val musicContentObserver = object : ContentObserver(
        Handler(Looper.getMainLooper())
    ) {
        override fun onChange(selfChange: Boolean, uri: Uri?) {
            super.onChange(selfChange, uri)

            if (!permissionGranted) return

            automaticMusicScanJob?.cancel()
            automaticMusicScanJob = lifecycleScope.launch {
                // Android may emit several MediaStore changes while a new
                // file is being indexed. Debounce them into one refresh.
                delay(900L)
                refreshSongs()
            }
        }
    }

    private val musicFolderPicker =
        registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            if (uri == null) return@registerForActivityResult

            runCatching {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }

            val uriString = uri.toString()
            val updatedFolders = (selectedMusicFolders + uriString).distinct()
            selectedMusicFolders = updatedFolders
            persistSelectedMusicFolders(updatedFolders)
            scanMusic()
        }

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            permissionGranted = granted
            if (granted) {
                if (libraryCacheLoaded) {
                    if (!libraryPrefs.contains("songs_cache_json")) {
                        scanMusic()
                    }
                } else {
                    scanAfterCacheLoad = true
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Claim a connected USB DAC as early as possible so the dedicated
        // Hi-Res USB audio engine can take control of the device.
        UsbAudioPermissionHelper.handleIntent(applicationContext, intent)

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

        selectedMusicFolders = libraryPrefs
            .getStringSet("music_folder_uris", emptySet())
            ?.toList()
            .orEmpty()

        permissionGranted = checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED

        contentResolver.registerContentObserver(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            true,
            musicContentObserver
        )

        loadCachedSongs()
        if (!permissionGranted) {
            permissionLauncher.launch(permission)
        }

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
                    isScanningMusic = isScanningMusic,
                    selectedMusicFolders = selectedMusicFolders,
                    onScanMusic = ::scanMusic,
                    onSelectMusicFolder = ::openMusicFolderPicker,
                    onRemoveMusicFolder = ::removeMusicFolder,
                    onRequestPermission = { permissionLauncher.launch(permission) }
                )
            }
        }

        window.decorView.post { hideStatusBar() }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        UsbAudioPermissionHelper.handleIntent(applicationContext, intent)
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

    private fun loadCachedSongs() {
        lifecycleScope.launch {
            val cachedSongs = withContext(Dispatchers.IO) {
                readCachedSongs()
            }

            if (cachedSongs != null) {
                songs = cachedSongs
            }

            libraryCacheLoaded = true

            controller?.let { c ->
                syncCurrentSong(c)
                restorePlaybackStateIfNeeded(c)
            }

            if (cachedSongs == null &&
                (permissionGranted || selectedMusicFolders.isNotEmpty())
            ) {
                scanAfterCacheLoad = false
                scanMusic()
            } else if (scanAfterCacheLoad && permissionGranted && cachedSongs == null) {
                scanAfterCacheLoad = false
                scanMusic()
            } else {
                scanAfterCacheLoad = false
            }
        }
    }

    private fun readCachedSongs(): List<Song>? {
        val raw = libraryPrefs.getString("songs_cache_json", null) ?: return null

        return runCatching {
            val array = JSONArray(raw)
            buildList(array.length()) {
                for (index in 0 until array.length()) {
                    val item = array.getJSONObject(index)
                    add(
                        Song(
                            id = item.getLong("id"),
                            title = item.getString("title"),
                            artist = item.getString("artist"),
                            album = item.getString("album"),
                            uri = item.getString("uri"),
                            durationMs = item.getLong("durationMs"),
                            albumId = item.getLong("albumId")
                        )
                    )
                }
            }
        }.getOrElse {
            libraryPrefs.edit().remove("songs_cache_json").apply()
            null
        }
    }

    private fun persistCachedSongs(songsToCache: List<Song>) {
        val array = JSONArray()

        songsToCache.forEach { song ->
            array.put(
                org.json.JSONObject().apply {
                    put("id", song.id)
                    put("title", song.title)
                    put("artist", song.artist)
                    put("album", song.album)
                    put("uri", song.uri)
                    put("durationMs", song.durationMs)
                    put("albumId", song.albumId)
                }
            )
        }

        libraryPrefs.edit()
            .putString("songs_cache_json", array.toString())
            .apply()
    }

    private suspend fun refreshSongs(): Int {
        val previousIds = songs.asSequence()
            .map { it.id }
            .toSet()

        val refreshedSongs = withContext(Dispatchers.IO) {
            MusicRepository(this@MainActivity).loadSongs(
                extraFolderUris = selectedMusicFolders.toSet()
            )
        }

        songs = refreshedSongs
        persistCachedSongs(refreshedSongs)

        val newSongs = if (previousIds.isEmpty()) {
            emptyList()
        } else {
            refreshedSongs.filter { it.id !in previousIds }
        }

        controller?.let { c ->
            syncCurrentSong(c)
            restorePlaybackStateIfNeeded(c)

            // Never clear or rebuild the current Media3 queue during a scan.
            // Newly discovered tracks are appended only, so the current
            // playback/playlist position remains intact.
            if (newSongs.isNotEmpty() && c.mediaItemCount > 0) {
                val queuedIds = HashSet<Long>(c.mediaItemCount)
                for (index in 0 until c.mediaItemCount) {
                    c.getMediaItemAt(index).mediaId.toLongOrNull()?.let {
                        queuedIds += it
                    }
                }

                val queueAdditions = newSongs.filter { it.id !in queuedIds }
                if (queueAdditions.isNotEmpty()) {
                    c.addMediaItems(queueAdditions.map(::mediaItemFor))
                }
            }
        }

        return newSongs.size
    }

    private fun persistSelectedMusicFolders(folders: List<String>) {
        libraryPrefs.edit()
            .putStringSet("music_folder_uris", folders.toSet())
            .apply()
    }

    private fun openMusicFolderPicker() {
        musicFolderPicker.launch(null)
    }

    private fun removeMusicFolder(uriString: String) {
        val updatedFolders = selectedMusicFolders.filterNot { it == uriString }
        if (updatedFolders.size == selectedMusicFolders.size) return

        runCatching {
            contentResolver.releasePersistableUriPermission(
                Uri.parse(uriString),
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }

        selectedMusicFolders = updatedFolders
        persistSelectedMusicFolders(updatedFolders)
        scanMusic()
    }

    private fun scanMusic() {
        if (isScanningMusic) return
        if (!permissionGranted && selectedMusicFolders.isEmpty()) {
            return
        }

        automaticMusicScanJob?.cancel()
        isScanningMusic = true

        lifecycleScope.launch {
            val result = runCatching { refreshSongs() }
            isScanningMusic = false

            val message = result.fold(
                onSuccess = { addedCount ->
                    if (addedCount > 0) {
                        if (addedCount == 1) {
                            nusaText("1 lagu baru ditemukan", "1 new song found")
                        } else {
                            nusaText("$addedCount lagu baru ditemukan", "$addedCount new songs found")
                        }
                    } else {
                        nusaText("Perpustakaan musik sudah diperbarui", "Music library updated")
                    }
                },
                onFailure = {
                    nusaText("Pemindaian musik gagal", "Music scan failed")
                }
            )

            android.widget.Toast.makeText(
                this@MainActivity,
                message,
                android.widget.Toast.LENGTH_SHORT
            ).show()
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
        startActivity(Intent.createChooser(
                intent,
                nusaText("Bagikan lagu", "Share song")
            ))
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
        automaticMusicScanJob?.cancel()
        automaticMusicScanJob = null
        runCatching {
            contentResolver.unregisterContentObserver(musicContentObserver)
        }

        controller?.let { persistPlaybackState(it, force = true) }
        controller?.release()
        controller = null
        super.onDestroy()
    }
}

private fun displayMusicFolderName(uriString: String): String {
    val treeUri = runCatching { Uri.parse(uriString) }.getOrNull()
        ?: return nusaText("Folder musik", "Music folder")

    val documentId = runCatching {
        DocumentsContract.getTreeDocumentId(treeUri)
    }.getOrNull().orEmpty()

    val displayName = documentId
        .substringAfterLast(':', documentId)
        .substringAfterLast('/')
        .takeIf { it.isNotBlank() }
        ?.let { Uri.decode(it) }

    return displayName ?: "Folder musik"
}

@Composable
private fun ChatGptStyleShareIcon(
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified
) {
    val resolvedColor =
        if (color == Color.Unspecified) {
            androidx.compose.material3.LocalContentColor.current
        } else {
            color
        }

    Canvas(modifier = modifier) {
        val strokeWidth = size.minDimension * 0.095f
        val left = size.width * 0.18f
        val right = size.width * 0.82f
        val trayY = size.height * 0.79f

        // Open share tray.
        drawLine(
            color = resolvedColor,
            start = androidx.compose.ui.geometry.Offset(left, trayY),
            end = androidx.compose.ui.geometry.Offset(right, trayY),
            strokeWidth = strokeWidth,
            cap = androidx.compose.ui.graphics.StrokeCap.Round
        )
        drawLine(
            color = resolvedColor,
            start = androidx.compose.ui.geometry.Offset(left, trayY),
            end = androidx.compose.ui.geometry.Offset(left, size.height * 0.61f),
            strokeWidth = strokeWidth,
            cap = androidx.compose.ui.graphics.StrokeCap.Round
        )
        drawLine(
            color = resolvedColor,
            start = androidx.compose.ui.geometry.Offset(right, trayY),
            end = androidx.compose.ui.geometry.Offset(right, size.height * 0.61f),
            strokeWidth = strokeWidth,
            cap = androidx.compose.ui.graphics.StrokeCap.Round
        )

        // Upward arrow used in the current ChatGPT-style share control.
        val centerX = size.width * 0.50f
        val topY = size.height * 0.17f
        val shaftBottomY = size.height * 0.61f
        val arrowWingY = size.height * 0.35f
        val wingX = size.width * 0.16f

        drawLine(
            color = resolvedColor,
            start = androidx.compose.ui.geometry.Offset(centerX, shaftBottomY),
            end = androidx.compose.ui.geometry.Offset(centerX, topY),
            strokeWidth = strokeWidth,
            cap = androidx.compose.ui.graphics.StrokeCap.Round
        )
        drawLine(
            color = resolvedColor,
            start = androidx.compose.ui.geometry.Offset(centerX, topY),
            end = androidx.compose.ui.geometry.Offset(centerX - wingX, arrowWingY),
            strokeWidth = strokeWidth,
            cap = androidx.compose.ui.graphics.StrokeCap.Round
        )
        drawLine(
            color = resolvedColor,
            start = androidx.compose.ui.geometry.Offset(centerX, topY),
            end = androidx.compose.ui.geometry.Offset(centerX + wingX, arrowWingY),
            strokeWidth = strokeWidth,
            cap = androidx.compose.ui.graphics.StrokeCap.Round
        )
    }
}

@Composable
private fun BiographyArrowIcon(
    modifier: Modifier = Modifier
) {
    val color = androidx.compose.material3.LocalContentColor.current

    Canvas(modifier = modifier) {
        val stroke = size.minDimension * 0.105f
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        val half = size.width * 0.22f

        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(centerX - half, centerY - half * 0.35f),
            end = androidx.compose.ui.geometry.Offset(centerX, centerY + half * 0.65f),
            strokeWidth = stroke,
            cap = androidx.compose.ui.graphics.StrokeCap.Round
        )
        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(centerX, centerY + half * 0.65f),
            end = androidx.compose.ui.geometry.Offset(centerX + half, centerY - half * 0.35f),
            strokeWidth = stroke,
            cap = androidx.compose.ui.graphics.StrokeCap.Round
        )
    }
}

@Composable
private fun NusaFavoriteIcon(
    selected: Boolean,
    modifier: Modifier = Modifier
) {
    val color = if (selected) {
        Color(0xFFFF4F6D)
    } else {
        androidx.compose.material3.LocalContentColor.current
    }

    Canvas(modifier = modifier) {
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(size.width * 0.50f, size.height * 0.87f)
            cubicTo(
                size.width * 0.43f,
                size.height * 0.80f,
                size.width * 0.12f,
                size.height * 0.61f,
                size.width * 0.12f,
                size.height * 0.35f
            )
            cubicTo(
                size.width * 0.12f,
                size.height * 0.15f,
                size.width * 0.34f,
                size.height * 0.08f,
                size.width * 0.50f,
                size.height * 0.28f
            )
            cubicTo(
                size.width * 0.66f,
                size.height * 0.08f,
                size.width * 0.88f,
                size.height * 0.15f,
                size.width * 0.88f,
                size.height * 0.35f
            )
            cubicTo(
                size.width * 0.88f,
                size.height * 0.61f,
                size.width * 0.57f,
                size.height * 0.80f,
                size.width * 0.50f,
                size.height * 0.87f
            )
            close()
        }

        if (selected) {
            drawPath(
                path = path,
                color = color
            )
        } else {
            drawPath(
                path = path,
                color = color,
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = size.minDimension * 0.075f,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round,
                    join = androidx.compose.ui.graphics.StrokeJoin.Round
                )
            )
        }
    }
}

private enum class LibrarySortOption(
    val indonesianLabel: String,
    val englishLabel: String
) {
    TITLE_ASC("Judul A–Z", "Title A–Z"),
    TITLE_DESC("Judul Z–A", "Title Z–A"),
    ARTIST_ASC("Artis A–Z", "Artist A–Z"),
    ALBUM_ASC("Album A–Z", "Album A–Z"),
    DURATION_ASC("Durasi terpendek", "Shortest duration"),
    DURATION_DESC("Durasi terpanjang", "Longest duration");

    val label: String
        get() = nusaText(indonesianLabel, englishLabel)
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
    isScanningMusic: Boolean,
    selectedMusicFolders: List<String>,
    onScanMusic: () -> Unit,
    onSelectMusicFolder: () -> Unit,
    onRemoveMusicFolder: (String) -> Unit,
    onRequestPermission: () -> Unit
) {
    var isFavorite by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var biographyExpanded by remember { mutableStateOf(false) }
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
    var sortMenuExpanded by remember { mutableStateOf(false) }
    var librarySortOption by remember { mutableStateOf(LibrarySortOption.TITLE_ASC) }

    val filtered = remember(
        songs,
        librarySortOption
    ) {
        when (librarySortOption) {
            LibrarySortOption.TITLE_ASC ->
                songs.sortedBy { it.title.lowercase(Locale.ROOT) }

            LibrarySortOption.TITLE_DESC ->
                songs.sortedByDescending { it.title.lowercase(Locale.ROOT) }

            LibrarySortOption.ARTIST_ASC ->
                songs.sortedBy {
                    it.artist.lowercase(Locale.ROOT)
                }

            LibrarySortOption.ALBUM_ASC ->
                songs.sortedBy {
                    it.album.lowercase(Locale.ROOT)
                }

            LibrarySortOption.DURATION_ASC ->
                songs.sortedBy { it.durationMs }

            LibrarySortOption.DURATION_DESC ->
                songs.sortedByDescending { it.durationMs }
        }
    }

    val alphabet = remember {
        ('A'..'Z').toList()
    }

    val alphabetTargets = remember(filtered) {
        alphabet.associateWith { letter ->
            filtered.indexOfFirst { song ->
                song.title.trim()
                    .firstOrNull()
                    ?.uppercaseChar() == letter
            }
        }
    }

    val pagerState = androidx.compose.foundation.pager.rememberPagerState(
        initialPage = 0,
        pageCount = { 2 }
    )
    val playerScrollState =
        androidx.compose.foundation.lazy.rememberLazyListState()
    val libraryListState =
        androidx.compose.foundation.lazy.grid.rememberLazyGridState()
    val scope = rememberCoroutineScope()
    val density = androidx.compose.ui.platform.LocalDensity.current



    // Center the currently playing song only after the pager has settled.
    // Because every grid card has a fixed height, the target offset is
    // deterministic and can be applied in one operation without a second
    // corrective scroll.
    LaunchedEffect(pagerState) {
        androidx.compose.runtime.snapshotFlow { pagerState.settledPage }
            .collect { settledPage ->
                if (settledPage == 1 && currentSong != null) {
                    val index = filtered.indexOfFirst { it.id == currentSong.id }

                    if (index >= 0) {
                        kotlinx.coroutines.yield()

                        val viewportHeight =
                            libraryListState.layoutInfo.viewportEndOffset -
                                libraryListState.layoutInfo.viewportStartOffset
                        val cardHeightPx =
                            with(density) { 194.dp.roundToPx() }
                        val centerOffset =
                            -((viewportHeight - cardHeightPx) / 2).coerceAtLeast(0)

                        libraryListState.scrollToItem(
                            index = index,
                            scrollOffset = centerOffset
                        )
                    }
                }
            }
    }

    LaunchedEffect(currentSong?.artist) {
        artistBiography = null
        biographyLoading =
            ArtistNameUtils.firstArtist(currentSong?.artist).isNotBlank()
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
                    nusaText("Pengaturan", "Settings"),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(18.dp))

                Text(
                    nusaText("UKURAN JUDUL LAGU", "SONG TITLE SIZE"),
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
                    nusaText("Atur besar-kecil judul lagu.", "Adjust the song title size."),
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(22.dp))

                Text(
                    nusaText("PERPUSTAKAAN MUSIK", "MUSIC LIBRARY"),
                    fontSize = 11.sp,
                    letterSpacing = 1.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))

                Surface(
                    onClick = onSelectMusicFolder,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    tonalElevation = 0.dp
                ) {
                    Row(
                        modifier = Modifier.padding(
                            horizontal = 16.dp,
                            vertical = 14.dp
                        ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.FolderOpen,
                            contentDescription = nusaText("Pilih folder musik", "Choose music folder")
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                nusaText("Pilih folder musik", "Choose music folder"),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                nusaText("Berikan akses ke folder lokal yang ingin dipindai.", "Grant access to a local folder to scan."),
                                fontSize = 12.sp,
                                lineHeight = 17.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (selectedMusicFolders.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))

                    selectedMusicFolders.forEach { folderUri ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp),
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                            tonalElevation = 0.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(
                                    start = 14.dp,
                                    end = 6.dp,
                                    top = 8.dp,
                                    bottom = 8.dp
                                ),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.FolderOpen,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    displayMusicFolderName(folderUri),
                                    modifier = Modifier.weight(1f),
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                                IconButton(
                                    onClick = { onRemoveMusicFolder(folderUri) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = nusaText("Hapus folder", "Remove folder")
                                    )
                                }
                            }
                        }
                    }
                }

                Surface(
                    onClick = onScanMusic,
                    enabled = !isScanningMusic,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = if (isScanningMusic) {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    contentColor = if (isScanningMusic) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onPrimary
                    },
                    tonalElevation = 0.dp
                ) {
                    Row(
                        modifier = Modifier.padding(
                            horizontal = 16.dp,
                            vertical = 14.dp
                        ),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = nusaText("Scan musik", "Scan music")
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (isScanningMusic) {
                                nusaText("Memindai musik…", "Scanning music…")
                            } else {
                                nusaText("Scan musik sekarang", "Scan music now")
                            },
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(Modifier.height(22.dp))

                Text(
                    nusaText("TENTANG NUSA", "ABOUT NUSA"),
                    fontSize = 11.sp,
                    letterSpacing = 1.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))

                Text(
                    nusaText("Nusa adalah pemutar musik lokal Android yang dirancang dengan fokus pada pengalaman mendengarkan musik yang bersih dan sederhana.", "Nusa is a local Android music player focused on a clean and simple listening experience."),
                    fontSize = 14.sp,
                    lineHeight = 21.sp
                )
                Spacer(Modifier.height(18.dp))

                Text(
                    nusaText("Pengembang", "Developer"),
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
                    nusaText("Proyek", "Project"),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    nusaText("Nusa  •  Versi 1.0", "Nusa  •  Version 1.0"),
                    fontSize = 15.sp
                )

                Spacer(Modifier.height(18.dp))

                Text(
                    nusaText("Teknologi", "Technology"),
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
                    nusaText("Biografi artis disediakan melalui Last.fm jika sudah dikonfigurasi.", "Artist biographies are provided through Last.fm when configured."),
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
                beyondViewportPageCount = 1
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
                                                    contentDescription = nusaText("Lainnya", "More")
                                                )
                                            }
                                            IconButton(onClick = { showSettings = true }) {
                                                Icon(
                                                    Icons.Default.Settings,
                                                    contentDescription = nusaText("Pengaturan", "Settings")
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
                                            positionMs = positionMs,
                                            durationMs = durationMs,
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
                                                currentSong?.title ?: nusaText("Pilih lagu", "Choose a song")

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
                                            currentSong?.artist ?: nusaText("Perpustakaan musik lokal Anda", "Your local music library"),
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
                                                icon = Icons.Rounded.FastRewind,
                                                contentDescription = nusaText("Sebelumnya", "Previous"),
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
                                                        Icons.Rounded.Pause
                                                    } else {
                                                        Icons.Rounded.PlayArrow
                                                    },
                                                    contentDescription = if (isPlaying) {
                                                        nusaText("Jeda", "Pause")
                                                    } else {
                                                        nusaText("Putar", "Play")
                                                    },
                                                    modifier = Modifier.size(40.dp)
                                                )
                                            }

                                            Spacer(Modifier.width(16.dp))

                                            TransportPillButton(
                                                icon = Icons.Rounded.FastForward,
                                                contentDescription = nusaText("Berikutnya", "Next"),
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
                                            val biographyArrowRotation by animateFloatAsState(
                                                targetValue = if (biographyExpanded) 180f else 0f,
                                                animationSpec = tween(
                                                    durationMillis = 380,
                                                    easing = androidx.compose.animation.core.FastOutSlowInEasing
                                                ),
                                                label = "biographyArrowRotation"
                                            )

                                            IconButton(
                                                onClick = {
                                                    biographyExpanded = !biographyExpanded
                                                    scope.launch {
                                                        if (biographyExpanded) {
                                                            val viewportHeight =
                                                                playerScrollState.layoutInfo.viewportEndOffset -
                                                                    playerScrollState.layoutInfo.viewportStartOffset

                                                            playerScrollState.animateScrollBy(
                                                                value = viewportHeight.toFloat(),
                                                                animationSpec = androidx.compose.animation.core.tween(
                                                                    durationMillis = 520,
                                                                    easing = androidx.compose.animation.core.FastOutSlowInEasing
                                                                )
                                                            )
                                                        } else {
                                                            playerScrollState.animateScrollToItem(
                                                                index = 0,
                                                                scrollOffset = 0
                                                            )
                                                        }
                                                    }
                                                },
                                                modifier = Modifier.size(42.dp)
                                            ) {
                                                BiographyArrowIcon(
                                                    modifier = Modifier
                                                        .size(25.dp)
                                                        .graphicsLayer {
                                                            rotationZ = biographyArrowRotation
                                                        }
                                                )
                                            }

                                            Spacer(Modifier.weight(1f))

                                            IconButton(
                                                onClick = { onShare(currentSong) },
                                                enabled = currentSong != null,
                                                modifier = Modifier.size(42.dp)
                                            ) {
                                                ChatGptStyleShareIcon(
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            }

                                            IconButton(
                                                onClick = { isFavorite = !isFavorite },
                                                enabled = currentSong != null,
                                                modifier = Modifier.size(42.dp)
                                            ) {
                                                NusaFavoriteIcon(
                                                    selected = isFavorite,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            }
                                        }

                                        Spacer(Modifier.height(8.dp))
                                        AudioQualityPill(song = currentSong)

                                        if (!permissionGranted) {
                                            Text(
                                                nusaText("Izinkan Nusa mengakses file audio Anda.", "Give Nusa access to your audio files."),
                                                color = Color(0xFF9D9D9D),
                                                fontSize = 14.sp
                                            )
                                            Spacer(Modifier.height(12.dp))
                                            FilledIconButton(onClick = onRequestPermission) {
                                                Icon(
                                                    Icons.Default.FolderOpen,
                                                    contentDescription = nusaText("Izinkan akses musik", "Allow music access")
                                                )
                                            }
                                        } else if (filtered.isEmpty()) {
                                            Text(
                                                nusaText("Tidak ada musik lokal", "No local music found"),
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
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color(0xFF18191B),
                                            Color(0xFF0D0E10),
                                            Color(0xFF070708)
                                        )
                                    )
                                )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(76.dp)
                                    .background(Color.Black.copy(alpha = 0.18f))
                                    .padding(horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = {
                                        scope.launch {
                                            pagerState.animateScrollToPage(0)
                                        }
                                    }
                                ) {
                                    Icon(
                                        Icons.Default.ArrowBack,
                                        contentDescription = nusaText("Kembali ke pemutar", "Back to player"),
                                        tint = Color.White
                                    )
                                }

                                Text(
                                    text = nusaText("Daftar Lagu", "Songs"),
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.Center,
                                    fontSize = 21.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )

                                Box {
                                    IconButton(
                                        onClick = { sortMenuExpanded = true }
                                    ) {
                                            Icon(
                                                Icons.Default.Sort,
                                                contentDescription = nusaText("Urutkan lagu", "Sort songs"),
                                                tint = Color.White
                                            )
                                        }

                                        DropdownMenu(
                                        expanded = sortMenuExpanded,
                                        onDismissRequest = {
                                            sortMenuExpanded = false
                                        }
                                    ) {
                                        LibrarySortOption.entries.forEach { option ->
                                            DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        text = option.label,
                                                        fontWeight = if (
                                                            option == librarySortOption
                                                        ) {
                                                            FontWeight.Bold
                                                        } else {
                                                            FontWeight.Normal
                                                        }
                                                    )
                                                },
                                                onClick = {
                                                    librarySortOption = option
                                                    sortMenuExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                state = libraryListState,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(top = 76.dp),
                                contentPadding = PaddingValues(
                                    top = 10.dp,
                                    start = 6.dp,
                                    end = 6.dp,
                                    bottom = 140.dp
                                ),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if ((permissionGranted || selectedMusicFolders.isNotEmpty()) && filtered.isNotEmpty()) {
                                    items(
                                        items = filtered,
                                        key = { song -> song.id },
                                        span = { GridItemSpan(1) },
                                        contentType = { "library-song" }
                                    ) { song ->
                                        LibrarySongRow(
                                            song = song,
                                            selected = currentSong?.id == song.id,
                                            onPlay = onPlay
                                        )
                                    }
                                } else if (!permissionGranted && selectedMusicFolders.isEmpty()) {
                                    item(span = { GridItemSpan(maxLineSpan) }) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 80.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                nusaText("Izinkan Nusa mengakses file audio Anda.", "Give Nusa access to your audio files."),
                                                color = Color(0xFF9D9D9D),
                                                fontSize = 14.sp
                                            )
                                            Spacer(Modifier.height(12.dp))
                                            FilledIconButton(onClick = onRequestPermission) {
                                                Icon(
                                                    Icons.Default.FolderOpen,
                                                    contentDescription = nusaText("Izinkan akses musik", "Allow music access")
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    item(span = { GridItemSpan(maxLineSpan) }) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 80.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                nusaText("Tidak ada musik lokal", "No local music found"),
                                                color = Color(0xFF9D9D9D),
                                                fontSize = 14.sp
                                            )
                                        }
                                    }
                                }
                            }

                            var alphabetScrollJob by remember {
                                mutableStateOf<Job?>(null)
                            }

                            fun requestAlphabetScroll(targetIndex: Int) {
                                if (targetIndex < 0) return

                                alphabetScrollJob?.cancel()
                                alphabetScrollJob = scope.launch {
                                    libraryListState.animateScrollToItem(
                                        index = targetIndex,
                                        scrollOffset = 0
                                    )
                                }
                            }

                            val alphabetIndexModifier = Modifier
                                .fillMaxHeight()
                                .padding(
                                    top = 88.dp,
                                    bottom = 122.dp,
                                    end = 2.dp
                                )
                                .width(22.dp)
                                .pointerInput(alphabetTargets, filtered) {
                                    var lastDragTarget = -1

                                    detectDragGestures(
                                        onDragStart = { offset ->
                                            val slotHeight =
                                                size.height / alphabet.size.toFloat()
                                            val slot = (offset.y / slotHeight)
                                                .toInt()
                                                .coerceIn(0, alphabet.lastIndex)
                                            val targetIndex =
                                                alphabetTargets[alphabet[slot]] ?: -1

                                            if (targetIndex >= 0) {
                                                lastDragTarget = targetIndex
                                                requestAlphabetScroll(targetIndex)
                                            }
                                        },
                                        onDrag = { change, _ ->
                                            change.consume()

                                            val slotHeight =
                                                size.height / alphabet.size.toFloat()
                                            val slot = (change.position.y / slotHeight)
                                                .toInt()
                                                .coerceIn(0, alphabet.lastIndex)
                                            val targetIndex =
                                                alphabetTargets[alphabet[slot]] ?: -1

                                            if (
                                                targetIndex >= 0 &&
                                                targetIndex != lastDragTarget
                                            ) {
                                                lastDragTarget = targetIndex
                                                requestAlphabetScroll(targetIndex)
                                            }
                                        },
                                        onDragEnd = {
                                            lastDragTarget = -1
                                        },
                                        onDragCancel = {
                                            lastDragTarget = -1
                                        }
                                    )
                                }

                            Box(
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Column(
                                    modifier = alphabetIndexModifier.align(Alignment.CenterEnd),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    alphabet.forEach { letter ->
                                    val targetIndex = alphabetTargets[letter] ?: -1
                                    val available = targetIndex >= 0

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f)
                                            .clickable(enabled = available) {
                                                requestAlphabetScroll(targetIndex)
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = letter.toString(),
                                            fontSize = 9.sp,
                                            fontWeight = if (available) {
                                                FontWeight.Bold
                                            } else {
                                                FontWeight.Normal
                                            },
                                            color = if (available) {
                                                Color.White.copy(alpha = 0.78f)
                                            } else {
                                                Color.White.copy(alpha = 0.18f)
                                            }
                                        )
                                    }
                                    }
                                }
                            }

                            val showFloatingControls by remember {
                                derivedStateOf {
                                    libraryListState.firstVisibleItemIndex >= 4
                                }
                            }
                            val showBackToPlayer by remember {
                                derivedStateOf {
                                    libraryListState.firstVisibleItemIndex >= 8
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

                            Box(
                                modifier = Modifier.fillMaxSize()
                            ) {
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

                                    Box(
                                        modifier = Modifier
                                            .size(46.dp)
                                            .clip(CircleShape)
                                            .background(shuffleActiveColor)
                                            .clickable(onClick = onToggleShuffle),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Shuffle,
                                            contentDescription = nusaText("Acak", "Shuffle"),
                                            tint = shuffleIconColor
                                        )
                                    }

                                    val repeatActive = repeatMode != Player.REPEAT_MODE_OFF
                                    val repeatActiveColor by animateColorAsState(
                                        targetValue = if (repeatActive) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            Color.Transparent
                                        },
                                        animationSpec = tween(180),
                                        label = "repeatBackground"
                                    )
                                    val repeatIconColor by animateColorAsState(
                                        targetValue = if (repeatActive) {
                                            MaterialTheme.colorScheme.onPrimary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        animationSpec = tween(180),
                                        label = "repeatIcon"
                                    )

                                    Box(
                                        modifier = Modifier
                                            .size(46.dp)
                                            .clip(CircleShape)
                                            .background(repeatActiveColor)
                                            .clickable(onClick = onToggleRepeat),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            if (repeatMode == Player.REPEAT_MODE_ONE) {
                                                Icons.Default.RepeatOne
                                            } else {
                                                Icons.Default.Repeat
                                            },
                                            contentDescription = nusaText("Ulangi", "Repeat"),
                                            tint = repeatIconColor
                                        )
                                    }
                                }
                            }
                            }

                            val backAlpha by animateFloatAsState(
                                targetValue = if (showBackToPlayer) 1f else 0f,
                                animationSpec = tween(280),
                                label = "backPlayerAlpha"
                            )
                            val backOffset by animateFloatAsState(
                                targetValue = if (showBackToPlayer) 0f else 26f,
                                animationSpec = tween(320),
                                label = "backPlayerOffset"
                            )

                            Box(
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    tonalElevation = 3.dp,
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(top = 82.dp, end = 14.dp)
                                        .graphicsLayer {
                                            alpha = backAlpha
                                            translationY = backOffset
                                        }
                                        .clickable {
                                            scope.launch {
                                                pagerState.animateScrollToPage(0)
                                            }
                                        }
                                ) {
                                Row(
                                    modifier = Modifier.padding(
                                        horizontal = 12.dp,
                                        vertical = 8.dp
                                    ),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.KeyboardArrowDown,
                                        contentDescription = nusaText("Kembali ke pemutar", "Back to player"),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        nusaText("Pemutar", "Player"),
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
}

@Composable
private fun ArtistBiographySection(
    artistName: String?,
    biography: com.yohandeku32.nusamusic.data.ArtistBiography?,
    loading: Boolean
) {
    val displayArtist = ArtistNameUtils.firstArtist(artistName).ifBlank { "Unknown artist" }

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
                    nusaText("Memuat biografi artis…", "Loading artist biography…"),
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
                        nusaText("Biografi tidak tersedia", "Biography not available")
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
                nusaText("TENTANG ARTIS", "ABOUT THE ARTIST"),
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
            val biographyScrollState =
                androidx.compose.foundation.rememberScrollState()

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(178.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF111111))
                    .padding(horizontal = 18.dp, vertical = 16.dp)
                    .verticalScroll(biographyScrollState)
            ) {
                Text(
                    text = biography.text,
                    color = Color(0xFFE7E7E7),
                    fontSize = 15.sp,
                    lineHeight = 23.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Text(
                text = nusaText("Sumber: Last.fm (", "Source: Last.fm (" + biography.sourceLanguage.uppercase() + ")",
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
    var showMetadataDialog by remember { mutableStateOf(false) }

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
    val label = if (isHiRes) "Hi-Res" else "Lossless"

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
                    .clickable(enabled = song != null && info != null) {
                        showMetadataDialog = true
                    }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    androidx.compose.foundation.Image(
                        painter = painterResource(id = R.drawable.apple_lossless_logo),
                        contentDescription = if (hiRes) "Hi-Res" else "Lossless",
                        contentScale = ContentScale.Fit,
                        colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(
                            if (hiRes) Color(0xFF3D3728) else MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.size(
                            width = 20.dp,
                            height = 11.dp
                        )
                    )

                    Spacer(Modifier.width(5.dp))

                    Text(
                        text = if (hiRes) "Hi-Res" else "Lossless",
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

    if (showMetadataDialog && song != null && info != null) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showMetadataDialog = false },
            properties = androidx.compose.ui.window.DialogProperties(
                dismissOnClickOutside = true,
                dismissOnBackPress = true,
                usePlatformDefaultWidth = false
            )
        ) {
            androidx.compose.animation.AnimatedVisibility(
                visible = true,
                enter = androidx.compose.animation.fadeIn(
                    animationSpec = tween(180)
                ) + androidx.compose.animation.scaleIn(
                    initialScale = 0.94f,
                    animationSpec = tween(
                        durationMillis = 220,
                        easing = androidx.compose.animation.core.FastOutSlowInEasing
                    )
                )
            ) {
                Surface(
                    modifier = Modifier
                        .widthIn(min = 300.dp, max = 352.dp)
                        .padding(horizontal = 18.dp),
                    shape = RoundedCornerShape(28.dp),
                    color = if (isHiRes) {
                        Color(0xFFDDD0A6)
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
                    tonalElevation = 8.dp,
                    shadowElevation = 18.dp
                ) {
                    Column(
                        modifier = Modifier.padding(
                            start = 22.dp,
                            end = 22.dp,
                            top = 20.dp,
                            bottom = 12.dp
                        )
                    ) {
                        Text(
                            text = nusaText("Info Audio", "Audio Info"),
                            fontSize = 21.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isHiRes) {
                                Color(0xFF3D3728)
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )

                        Text(
                            text = song.title,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            color = if (isHiRes) Color(0xFF5A4E2F) else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 3.dp)
                        )

                        Text(
                            text = ArtistNameUtils.firstArtist(song.artist),
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            color = if (isHiRes) Color(0xFF5A4E2F) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f),
                            modifier = Modifier.padding(top = 1.dp)
                        )

                        Spacer(Modifier.height(16.dp))

                        MetadataRow(
                            label = nusaText("Codec", "Codec"),
                            value = info.codecName,
                            hiRes = isHiRes
                        )
                        MetadataDivider(hiRes = isHiRes)
                        MetadataRow(
                            label = nusaText("Sample rate", "Sample rate"),
                            value = formatSampleRate(info.sampleRateHz),
                            hiRes = isHiRes
                        )
                        MetadataDivider(hiRes = isHiRes)
                        MetadataRow(
                            label = nusaText("Kedalaman bit", "Bit depth"),
                            value = formatBitDepth(info.bitDepth),
                            hiRes = isHiRes
                        )
                        MetadataDivider(hiRes = isHiRes)
                        MetadataRow(
                            label = nusaText("Durasi", "Duration"),
                            value = formatTime(song.durationMs),
                            hiRes = isHiRes
                        )

                        Spacer(Modifier.height(12.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.055f)
                                )
                                .clickable {
                                    showMetadataDialog = false
                                }
                                .padding(vertical = 11.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = nusaText("Tutup", "Close"),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatSampleRate(sampleRateHz: Int?): String {
    return sampleRateHz?.let { rate ->
        if (rate % 1000 == 0) {
            (rate / 1000).toString() + " kHz"
        } else {
            String.format(Locale.US, "%.1f kHz", rate / 1000f)
        }
    } ?: nusaText("Tidak tersedia", "Not available")
}

private fun formatBitDepth(bitDepth: Int?): String {
    return bitDepth?.let { it.toString() + "-bit" } ?: nusaText("Tidak tersedia", "Not available")
}

@Composable
private fun MetadataRow(
    label: String,
    value: String,
    hiRes: Boolean = false
) {
    val labelColor = if (hiRes) {
        Color(0xFF6B5B38)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val valueColor = if (hiRes) {
        Color(0xFF3D3728)
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = labelColor,
            modifier = Modifier.weight(1f)
        )

        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = valueColor,
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 165.dp)
        )
    }
}

@Composable
private fun MetadataDivider(hiRes: Boolean = false) {
    androidx.compose.material3.HorizontalDivider(
        color = if (hiRes) {
            Color(0xFF3D3728).copy(alpha = 0.16f)
        } else {
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f)
        },
        thickness = 1.dp
    )
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
            artistName = ArtistNameUtils.firstArtist(song?.artist),
            maxSize = 256
        )
    }

    if (artistBitmap != null) {
        androidx.compose.foundation.Image(
            bitmap = artistBitmap!!.asImageBitmap(),
            contentDescription = ArtistNameUtils.firstArtist(song?.artist),
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
        val stroke = 3.0.dp.toPx()

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
        modifier = Modifier.size(width = 96.dp, height = 58.dp),
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 0.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                icon,
                contentDescription = contentDescription,
                modifier = Modifier
                    .size(32.dp)
                    .graphicsLayer {
                        scaleX = 1.05f
                        scaleY = 1.05f
                    }
            )
        }
    }
}

@Composable
private fun VinylRecord(
    song: Song?,
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    modifier: Modifier = Modifier
) {
    // Keep the physical angle continuous while changing rotation speed.
    // Playback starts and stops with a gentle acceleration/deceleration instead
    // of an abrupt jump.
    val rotation = remember { Animatable(0f) }
    val rotationSpeed = remember { Animatable(0f) }

    LaunchedEffect(isPlaying) {
        val targetSpeed = if (isPlaying) {
            360f / 6.5f
        } else {
            0f
        }

        rotationSpeed.animateTo(
            targetValue = targetSpeed,
            animationSpec = tween(
                durationMillis = 560,
                easing = androidx.compose.animation.core.FastOutSlowInEasing
            )
        )
    }

    LaunchedEffect(Unit) {
        var lastFrameNanos = 0L

        while (isActive) {
            val frameNanos = androidx.compose.runtime.withFrameNanos { it }

            if (lastFrameNanos != 0L) {
                val deltaSeconds =
                    ((frameNanos - lastFrameNanos).coerceAtMost(100_000_000L)) /
                        1_000_000_000f

                val nextRotation =
                    (rotation.value + rotationSpeed.value * deltaSeconds) % 360f

                rotation.snapTo(nextRotation)
            }

            lastFrameNanos = frameNanos
        }
    }

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

    val progress = if (durationMs > 0L) {
        (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    rotationZ = rotation.value
                    shadowElevation = 18.dp.toPx()
                    shape = CircleShape
                    clip = false
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
            // POWER LED REFLECTION
            // A narrow, soft red bias on the rotating PVC. Because this code
            // lives inside the rotating vinyl layer, the highlight itself
            // rotates with the record and catches the groove texture.
            if (isPlaying && song != null) {
                val reflectionCenter = androidx.compose.ui.geometry.Offset(
                    size.width * 0.145f,
                    size.height * 0.805f
                )

                drawOval(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFF4A4A).copy(alpha = 0.050f),
                            Color(0xFFFF3333).copy(alpha = 0.022f),
                            Color.Transparent
                        ),
                        center = reflectionCenter,
                        radius = radius * 0.19f
                    ),
                    topLeft = androidx.compose.ui.geometry.Offset(
                        reflectionCenter.x - radius * 0.30f,
                        reflectionCenter.y - radius * 0.075f
                    ),
                    size = androidx.compose.ui.geometry.Size(
                        radius * 0.60f,
                        radius * 0.15f
                    )
                )

                // A second, tighter streak follows the vinyl's groove direction.
                drawArc(
                    color = Color(0xFFFF5959).copy(alpha = 0.028f),
                    startAngle = 145f,
                    sweepAngle = 26f,
                    useCenter = false,
                    topLeft = androidx.compose.ui.geometry.Offset(
                        reflectionCenter.x - radius * 0.23f,
                        reflectionCenter.y - radius * 0.23f
                    ),
                    size = androidx.compose.ui.geometry.Size(
                        radius * 0.46f,
                        radius * 0.46f
                    ),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = 1.6f
                    )
                )
            }

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

        // Center label/artwork is treated as a real paper label adhered
        // to the vinyl: slightly warm paper base, soft edge shading, grain,
        // fibers and subtle crease marks.
        val paperTexture = remember {
            val random = Random(2047)
            List(150) {
                floatArrayOf(
                    random.nextFloat(),
                    random.nextFloat(),
                    random.nextFloat(),
                    random.nextFloat()
                )
            }
        }

        Box(
            modifier = Modifier
                .size(142.dp)
                .clip(CircleShape)
                .graphicsLayer {
                    shadowElevation = 3.dp.toPx()
                    shape = CircleShape
                    clip = true
                }
                .background(Color(0xFFE7E3D7))
        ) {
            ArtworkView(
                song = song,
                maxSizePx = 512,
                modifier = Modifier.fillMaxSize()
            )

            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                val labelRadius = size.minDimension / 2f
                val center = androidx.compose.ui.geometry.Offset(
                    size.width / 2f,
                    size.height / 2f
                )

                // Soft paper edge shading: the printed label is not a
                // perfectly flat digital circle.
                drawCircle(
                    color = Color.White.copy(alpha = 0.045f),
                    radius = labelRadius * 0.992f,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = 2.2f
                    )
                )
                drawCircle(
                    color = Color.Black.copy(alpha = 0.085f),
                    radius = labelRadius * 0.976f,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = 2.4f
                    )
                )

                // Fine paper grain.
                for (sample in paperTexture) {
                    val x = sample[0] * size.width
                    val y = sample[1] * size.height
                    val dx = x - center.x
                    val dy = y - center.y

                    if (dx * dx + dy * dy <= labelRadius * labelRadius) {
                        val bright = sample[3] > 0.53f
                        drawCircle(
                            color = if (bright) {
                                Color.White.copy(alpha = 0.018f + sample[2] * 0.020f)
                            } else {
                                Color.Black.copy(alpha = 0.010f + sample[2] * 0.014f)
                            },
                            radius = 0.22f + sample[2] * 0.58f,
                            center = androidx.compose.ui.geometry.Offset(x, y)
                        )
                    }
                }

                // Very subtle fibers/wrinkles following a natural paper surface.
                for (i in 0 until 14) {
                    val startX = size.width * (0.10f + (i % 5) * 0.17f)
                    val startY = size.height * (0.18f + (i % 7) * 0.095f)
                    val endX = startX + size.width * (0.12f + (i % 4) * 0.055f)
                    val endY = startY + size.height * (0.025f + (i % 3) * 0.018f)

                    drawLine(
                        color = Color.Black.copy(alpha = 0.022f),
                        start = androidx.compose.ui.geometry.Offset(startX, startY),
                        end = androidx.compose.ui.geometry.Offset(endX, endY),
                        strokeWidth = 0.55f
                    )
                }

                // Printed paper catch-light, like a slightly glossy label
                // pressed onto the record surface.
                drawArc(
                    color = Color.White.copy(alpha = 0.10f),
                    startAngle = -78f,
                    sweepAngle = 27f,
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
                        width = 2.6f
                    )
                )

                // Gentle lower edge shade makes the paper look physically
                // seated against the recessed label area.
                drawArc(
                    color = Color.Black.copy(alpha = 0.055f),
                    startAngle = 58f,
                    sweepAngle = 116f,
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
                        width = 2.8f
                    )
                )
            }
        }

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

        VinylPowerIndicator(
            isPlaying = isPlaying && song != null,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(
                    start = 9.dp,
                    bottom = 22.dp
                )
        )

        VinylTonearm(
            isPlaying = isPlaying,
            hasSong = song != null,
            progress = progress,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun VinylPowerIndicator(
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val lightAlpha by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0.10f,
        animationSpec = tween(durationMillis = 260),
        label = "powerLightAlpha"
    )

    // Compact physical switch. The actual light is intentionally treated as
    // an optical source: soft bloom + warm hotspot, not a UI dot.
    Box(
        modifier = modifier
            .size(34.dp)
            .graphicsLayer {
                shadowElevation = 4.dp.toPx()
                shape = CircleShape
                clip = false
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(52.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                val center = androidx.compose.ui.geometry.Offset(
                    size.width / 2f,
                    size.height / 2f
                )

                // Physical bezel center / radius.
                val bezelRadius = 15.2f

                // ---------------------------------------------------------
                // OPTICAL BLOOM
                // ---------------------------------------------------------
                // Side-emitting lamp: the light leaves the switch laterally,
                // producing a soft streak instead of a visible red dot.
                val lightCenter = androidx.compose.ui.geometry.Offset(
                    center.x + 16.5f,
                    center.y + 0.5f
                )

                if (isPlaying) {
                    // Broad spill.
                    drawOval(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFFF3838).copy(alpha = 0.14f * lightAlpha),
                                Color(0xFFFF3838).copy(alpha = 0.052f * lightAlpha),
                                Color(0xFFFF3838).copy(alpha = 0.012f * lightAlpha),
                                Color.Transparent
                            ),
                            center = lightCenter,
                            radius = 17f
                        ),
                        topLeft = androidx.compose.ui.geometry.Offset(
                            lightCenter.x - 18f,
                            lightCenter.y - 7f
                        ),
                        size = androidx.compose.ui.geometry.Size(
                            36f,
                            14f
                        )
                    )

                    // Focused beam edge.
                    drawLine(
                        color = Color(0xFFFF4B4B).copy(alpha = 0.11f * lightAlpha),
                        start = androidx.compose.ui.geometry.Offset(
                            lightCenter.x - 1f,
                            lightCenter.y
                        ),
                        end = androidx.compose.ui.geometry.Offset(
                            lightCenter.x + 11f,
                            lightCenter.y + 0.4f
                        ),
                        strokeWidth = 1.8f,
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                }

                // ---------------------------------------------------------
                // SWITCH BODY
                // ---------------------------------------------------------
                drawCircle(
                    color = Color.Black.copy(alpha = 0.15f),
                    radius = bezelRadius * 0.98f,
                    center = androidx.compose.ui.geometry.Offset(
                        center.x + 1.0f,
                        center.y + 1.7f
                    )
                )

                drawCircle(
                    color = Color(0xFF171717),
                    radius = bezelRadius,
                    center = center
                )

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF474747),
                            Color(0xFF252525),
                            Color(0xFF0C0C0C)
                        ),
                        center = androidx.compose.ui.geometry.Offset(
                            center.x - 4.0f,
                            center.y - 4.0f
                        ),
                        radius = 15.5f
                    ),
                    radius = 12.7f,
                    center = center
                )

                drawArc(
                    color = Color.White.copy(alpha = 0.15f),
                    startAngle = 212f,
                    sweepAngle = 95f,
                    useCenter = false,
                    topLeft = androidx.compose.ui.geometry.Offset(
                        center.x - 12.7f,
                        center.y - 12.7f
                    ),
                    size = androidx.compose.ui.geometry.Size(
                        25.4f,
                        25.4f
                    ),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = 1.0f
                    )
                )

                // ---------------------------------------------------------
                // SIDE-FACING LIGHT SOURCE
                // ---------------------------------------------------------
                // Recessed side slot. It is bright but not circular.
                drawRoundRect(
                    color = Color.Black.copy(alpha = 0.82f),
                    topLeft = androidx.compose.ui.geometry.Offset(
                        lightCenter.x - 4.5f,
                        lightCenter.y - 2.5f
                    ),
                    size = androidx.compose.ui.geometry.Size(
                        9f,
                        5f
                    ),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(
                        2.2f,
                        2.2f
                    )
                )

                // Hot elongated LED lens.
                drawRoundRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFFFF7777).copy(alpha = 0.55f * lightAlpha),
                            Color(0xFFFF3030).copy(alpha = 0.95f * lightAlpha),
                            Color(0xFFD90909).copy(alpha = 0.62f * lightAlpha),
                            Color.Transparent
                        )
                    ),
                    topLeft = androidx.compose.ui.geometry.Offset(
                        lightCenter.x - 3.8f,
                        lightCenter.y - 1.9f
                    ),
                    size = androidx.compose.ui.geometry.Size(
                        8.5f,
                        3.8f
                    ),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(
                        1.8f,
                        1.8f
                    )
                )

                // Tiny glass catch-light.
                drawLine(
                    color = Color.White.copy(alpha = 0.32f * lightAlpha),
                    start = androidx.compose.ui.geometry.Offset(
                        lightCenter.x - 1.9f,
                        lightCenter.y - 1.1f
                    ),
                    end = androidx.compose.ui.geometry.Offset(
                        lightCenter.x - 0.2f,
                        lightCenter.y - 1.1f
                    ),
                    strokeWidth = 0.75f,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                )
            }
        }
    }
}

@Composable
private fun VinylTonearm(
    isPlaying: Boolean,
    hasSong: Boolean,
    progress: Float,
    modifier: Modifier = Modifier
) {
    // The whole tonearm assembly rotates from its pivot. The arm, headshell
    // and stylus stay physically connected instead of moving independently.
    val targetAngle = if (isPlaying && hasSong) {
        progress * 5.0f
    } else {
        0f
    }

    val armAngle by animateFloatAsState(
        targetValue = targetAngle,
        animationSpec = tween(
            durationMillis = if (isPlaying) 900 else 620,
            easing = androidx.compose.animation.core.FastOutSlowInEasing
        ),
        label = "tonearmAngle"
    )

    val targetContact = if (isPlaying && hasSong) 1f else 0f
    val contact by animateFloatAsState(
        targetValue = targetContact,
        animationSpec = tween(
            durationMillis = if (isPlaying) 720 else 400,
            easing = androidx.compose.animation.core.FastOutSlowInEasing
        ),
        label = "tonearmContact"
    )

    Canvas(modifier = modifier) {
        // Pivot remains fixed in the white area outside the vinyl.
        val pivot = androidx.compose.ui.geometry.Offset(
            x = size.width * 0.935f,
            y = size.height * 0.125f
        )

        // Reference-style slim curved arm.
        val armPath = androidx.compose.ui.graphics.Path().apply {
            moveTo(pivot.x, pivot.y)

            cubicTo(
                size.width * 0.938f,
                size.height * 0.235f,
                size.width * 0.940f,
                size.height * 0.345f,
                size.width * 0.915f,
                size.height * 0.440f
            )

            cubicTo(
                size.width * 0.895f,
                size.height * 0.525f,
                size.width * 0.865f,
                size.height * 0.600f,
                size.width * 0.825f,
                size.height * 0.665f
            )

            cubicTo(
                size.width * 0.810f,
                size.height * 0.690f,
                size.width * 0.800f,
                size.height * 0.710f,
                size.width * 0.788f,
                size.height * 0.725f
            )
        }

        val headshellCenter = androidx.compose.ui.geometry.Offset(
            x = size.width * 0.775f,
            y = size.height * 0.745f
        )

        val stylusTip = androidx.compose.ui.geometry.Offset(
            x = size.width * 0.748f,
            y = size.height * 0.805f +
                size.height * 0.010f * (1f - contact)
        )

        // Keep all rotating parts connected to this same pivot.
        rotate(
            degrees = armAngle,
            pivot = pivot
        ) {
            // Very subtle shadow under the complete metal arm.
            drawPath(
                path = armPath,
                color = Color.Black.copy(alpha = 0.14f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = 14.0f,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round,
                    join = androidx.compose.ui.graphics.StrokeJoin.Round
                )
            )

            // Dark under-shell gives the arm some physical thickness.
            drawPath(
                path = armPath,
                color = Color(0xFF3C3C3C),
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = 11.5f,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round,
                    join = androidx.compose.ui.graphics.StrokeJoin.Round
                )
            )

            // Brushed-metal tube.
            drawPath(
                path = armPath,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF8B8B8B),
                        Color(0xFFE5E5E5),
                        Color(0xFFB7B7B7),
                        Color(0xFFF4F4F4),
                        Color(0xFF898989)
                    ),
                    start = pivot,
                    end = androidx.compose.ui.geometry.Offset(
                        size.width * 0.75f,
                        size.height * 0.75f
                    )
                ),
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = 8.8f,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round,
                    join = androidx.compose.ui.graphics.StrokeJoin.Round
                )
            )

            // Thin specular highlight.
            drawPath(
                path = armPath,
                color = Color.White.copy(alpha = 0.44f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = 1.35f,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round,
                    join = androidx.compose.ui.graphics.StrokeJoin.Round
                )
            )

            // Small collar under the pivot.
            val collarTop = androidx.compose.ui.geometry.Offset(
                x = pivot.x,
                y = pivot.y + size.height * 0.042f
            )
            val collarBottom = androidx.compose.ui.geometry.Offset(
                x = pivot.x,
                y = pivot.y + size.height * 0.093f
            )

            drawLine(
                color = Color(0xFF202020),
                start = collarTop,
                end = collarBottom,
                strokeWidth = size.width * 0.024f,
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )

            drawLine(
                color = Color(0xFFD0D0D0),
                start = androidx.compose.ui.geometry.Offset(
                    collarTop.x - 1f,
                    collarTop.y
                ),
                end = androidx.compose.ui.geometry.Offset(
                    collarBottom.x - 1f,
                    collarBottom.y
                ),
                strokeWidth = size.width * 0.010f,
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )

            // The headshell follows the exact same rotation.
            val shellWidth = size.width * 0.078f
            val shellHeight = size.height * 0.066f

            val shellNeckStart = androidx.compose.ui.geometry.Offset(
                x = size.width * 0.785f,
                y = size.height * 0.700f
            )

            drawLine(
                color = Color(0xFF252525),
                start = shellNeckStart,
                end = headshellCenter,
                strokeWidth = 10.0f,
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )

            drawLine(
                color = Color(0xFFD0D0D0),
                start = shellNeckStart,
                end = headshellCenter,
                strokeWidth = 6.2f,
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )

            // Rectangular headshell inspired by the supplied reference.
            drawRoundRect(
                color = Color(0xFF111111),
                topLeft = androidx.compose.ui.geometry.Offset(
                    headshellCenter.x - shellWidth * 0.50f,
                    headshellCenter.y - shellHeight * 0.50f
                ),
                size = androidx.compose.ui.geometry.Size(
                    shellWidth,
                    shellHeight
                ),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(
                    2.5f,
                    2.5f
                )
            )

            drawRoundRect(
                color = Color(0xFFE0E0E0),
                topLeft = androidx.compose.ui.geometry.Offset(
                    headshellCenter.x - shellWidth * 0.42f,
                    headshellCenter.y - shellHeight * 0.42f
                ),
                size = androidx.compose.ui.geometry.Size(
                    shellWidth * 0.84f,
                    shellHeight * 0.84f
                ),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(
                    2f,
                    2f
                )
            )

            // Small cartridge perforations.
            for (row in 0 until 3) {
                for (col in 0 until 3) {
                    drawCircle(
                        color = Color(0xFF6A6A6A),
                        radius = 1.10f,
                        center = androidx.compose.ui.geometry.Offset(
                            x = headshellCenter.x -
                                shellWidth * 0.20f +
                                col * shellWidth * 0.16f,
                            y = headshellCenter.y -
                                shellHeight * 0.16f +
                                row * shellHeight * 0.16f
                        )
                    )
                }
            }

            // Stylus is physically attached to the headshell and therefore
            // rotates with the complete arm assembly.
            val cantileverStart = androidx.compose.ui.geometry.Offset(
                x = headshellCenter.x - shellWidth * 0.18f,
                y = headshellCenter.y + shellHeight * 0.42f
            )

            drawLine(
                color = Color(0xFF161616),
                start = cantileverStart,
                end = stylusTip,
                strokeWidth = 2.25f,
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )

            drawCircle(
                color = if (contact > 0.50f) {
                    Color(0xFFE2E2E2)
                } else {
                    Color(0xFF777777)
                },
                radius = 2.45f,
                center = stylusTip
            )
        }

        // Pivot bearing itself is fixed. The arm rotates around this exact
        // center, making the movement physically understandable.
        val pivotShadow = androidx.compose.ui.geometry.Offset(
            pivot.x + size.minDimension * 0.007f,
            pivot.y + size.minDimension * 0.010f
        )

        drawCircle(
            color = Color.Black.copy(alpha = 0.12f),
            radius = size.minDimension * 0.048f,
            center = pivotShadow
        )

        drawCircle(
            color = Color(0xFF1A1A1A),
            radius = size.minDimension * 0.045f,
            center = pivot
        )

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF4A4A4A),
                    Color(0xFF202020),
                    Color(0xFF0D0D0D)
                ),
                center = androidx.compose.ui.geometry.Offset(
                    pivot.x - size.minDimension * 0.011f,
                    pivot.y - size.minDimension * 0.011f
                ),
                radius = size.minDimension * 0.043f
            ),
            radius = size.minDimension * 0.036f,
            center = pivot
        )

        drawCircle(
            color = Color(0xFFB8B8B8),
            radius = size.minDimension * 0.014f,
            center = pivot
        )

        drawCircle(
            color = Color(0xFF161616),
            radius = size.minDimension * 0.008f,
            center = pivot
        )
    }
}

private data class GrainDot(
    val x: Float,
    val y: Float,
    val radius: Float,
    val alpha: Float,
    val dark: Boolean
)

private data class GrainFiber(
    val x: Float,
    val y: Float,
    val length: Float,
    val alpha: Float,
    val dark: Boolean
)

@Composable
private fun ArtworkView(
    song: Song?,
    maxSizePx: Int = 512,
    modifier: Modifier = Modifier,
    monochrome: Boolean = false
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val density = androidx.compose.ui.platform.LocalDensity.current
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(song?.uri, maxSizePx) {
        bitmap = song?.let {
            ArtworkLoader.load(
                context = context,
                uriString = it.uri,
                maxSize = maxSizePx,
                cacheKey = it.albumId.takeIf { albumId -> albumId > 0L }?.toString()
            )
        }
    }

    Box(
        modifier = modifier
            .background(
                Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.surfaceVariant,
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
            .drawWithCache {
                val seed = (song?.id ?: song?.uri ?: "nusa").hashCode()
                val random = Random(seed)
                val speckles = List(24) {
                    GrainDot(
                        x = random.nextFloat(),
                        y = random.nextFloat(),
                        radius = 0.35f + random.nextFloat() * 1.15f,
                        alpha = 0.018f + random.nextFloat() * 0.045f,
                        dark = random.nextBoolean()
                    )
                }
                val fibers = List(3) {
                    GrainFiber(
                        x = random.nextFloat(),
                        y = random.nextFloat(),
                        length = 6f + random.nextFloat() * 16f,
                        alpha = 0.012f + random.nextFloat() * 0.022f,
                        dark = random.nextBoolean()
                    )
                }

                onDrawWithContent {
                    drawContent()

                    val minSide = size.minDimension
                    speckles.forEach { dot ->
                        drawCircle(
                            color = if (dot.dark) {
                                Color.Black.copy(alpha = dot.alpha)
                            } else {
                                Color.White.copy(alpha = dot.alpha)
                            },
                            radius = dot.radius * (minSide / 180f),
                            center = androidx.compose.ui.geometry.Offset(
                                x = size.width * dot.x,
                                y = size.height * dot.y
                            )
                        )
                    }

                    fibers.forEach { fiber ->
                        val start = androidx.compose.ui.geometry.Offset(
                            x = size.width * fiber.x,
                            y = size.height * fiber.y
                        )
                        drawLine(
                            color = if (fiber.dark) {
                                Color.Black.copy(alpha = fiber.alpha)
                            } else {
                                Color.White.copy(alpha = fiber.alpha)
                            },
                            start = start,
                            end = androidx.compose.ui.geometry.Offset(
                                x = start.x + fiber.length,
                                y = start.y + fiber.length * 0.12f
                            ),
                            strokeWidth = 0.65f
                        )
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        val targetBitmap = bitmap
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

private data class WornScratch(
    val x1: Float,
    val y1: Float,
    val x2: Float,
    val y2: Float,
    val alpha: Float,
    val width: Float
)

private data class WornScuff(
    val x: Float,
    val y: Float,
    val radius: Float,
    val alpha: Float
)

@Composable
private fun WornCoverArtwork(
    song: Song,
    maxSizePx: Int,
    modifier: Modifier = Modifier
) {
    val scratches = remember(song.id) {
        val random = Random(song.id.hashCode())
        List(18) {
            val edge = random.nextInt(4)
            when (edge) {
                0 -> {
                    val x = random.nextFloat()
                    val y = random.nextFloat() * 0.18f
                    WornScratch(
                        x1 = x,
                        y1 = y,
                        x2 = (x + 0.02f + random.nextFloat() * 0.08f).coerceAtMost(1f),
                        y2 = (y + 0.01f + random.nextFloat() * 0.025f).coerceAtMost(0.22f),
                        alpha = 0.16f + random.nextFloat() * 0.18f,
                        width = 0.45f + random.nextFloat() * 0.75f
                    )
                }
                1 -> {
                    val x = 0.82f + random.nextFloat() * 0.18f
                    val y = random.nextFloat()
                    WornScratch(
                        x1 = x,
                        y1 = y,
                        x2 = (x - 0.01f - random.nextFloat() * 0.04f).coerceAtLeast(0f),
                        y2 = (y + 0.02f + random.nextFloat() * 0.10f).coerceAtMost(1f),
                        alpha = 0.14f + random.nextFloat() * 0.18f,
                        width = 0.45f + random.nextFloat() * 0.75f
                    )
                }
                2 -> {
                    val x = random.nextFloat()
                    val y = 0.82f + random.nextFloat() * 0.18f
                    WornScratch(
                        x1 = x,
                        y1 = y,
                        x2 = (x + 0.02f + random.nextFloat() * 0.08f).coerceAtMost(1f),
                        y2 = (y - 0.01f - random.nextFloat() * 0.03f).coerceAtLeast(0f),
                        alpha = 0.14f + random.nextFloat() * 0.17f,
                        width = 0.45f + random.nextFloat() * 0.80f
                    )
                }
                else -> {
                    val x = random.nextFloat() * 0.18f
                    val y = random.nextFloat()
                    WornScratch(
                        x1 = x,
                        y1 = y,
                        x2 = (x + 0.01f + random.nextFloat() * 0.035f).coerceAtMost(1f),
                        y2 = (y + 0.02f + random.nextFloat() * 0.10f).coerceAtMost(1f),
                        alpha = 0.14f + random.nextFloat() * 0.18f,
                        width = 0.45f + random.nextFloat() * 0.80f
                    )
                }
            }
        }
    }

    val scuffs = remember(song.id) {
        val random = Random(song.id.hashCode() xor 0x5A17)
        List(22) {
            WornScuff(
                x = random.nextFloat(),
                y = random.nextFloat(),
                radius = 0.7f + random.nextFloat() * 2.2f,
                alpha = 0.018f + random.nextFloat() * 0.045f
            )
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .graphicsLayer {
                shadowElevation = with(density) {
                    2.5.dp.toPx()
                }
                shape = RoundedCornerShape(8.dp)
                clip = false
            }
            // Clear plastic shell: transparent body with a very subtle edge.
            .background(Color.White.copy(alpha = 0.025f))
            .border(
                width = 1.15.dp,
                color = Color.White.copy(alpha = 0.22f),
                shape = RoundedCornerShape(8.dp)
            )
    ) {
        // The artwork sits inside a slightly raised black paper sleeve.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(2.5.dp)
                .clip(RoundedCornerShape(6.dp))
        ) {
            ArtworkView(
                song = song,
                maxSizePx = maxSizePx,
                modifier = Modifier.fillMaxSize()
            )

            Canvas(modifier = Modifier.fillMaxSize()) {
                val edge = 2.5f

                // Soft dirty-paper vignette.
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.08f),
                            Color.Black.copy(alpha = 0.34f)
                        ),
                        center = androidx.compose.ui.geometry.Offset(
                            size.width * 0.50f,
                            size.height * 0.48f
                        ),
                        radius = size.minDimension * 0.76f
                    )
                )

                // Fine edge wear, similar to the scuffed corners of the reference.
                scratches.forEach { mark ->
                    drawLine(
                        color = Color.White.copy(alpha = mark.alpha),
                        start = androidx.compose.ui.geometry.Offset(
                            size.width * mark.x1,
                            size.height * mark.y1
                        ),
                        end = androidx.compose.ui.geometry.Offset(
                            size.width * mark.x2,
                            size.height * mark.y2
                        ),
                        strokeWidth = mark.width
                    )
                }

                // Small paper dust/scuff points.
                scuffs.forEach { dot ->
                    drawCircle(
                        color = Color.White.copy(alpha = dot.alpha),
                        radius = dot.radius,
                        center = androidx.compose.ui.geometry.Offset(
                            size.width * dot.x,
                            size.height * dot.y
                        )
                    )
                }

                // Worn highlights concentrated around the four edges.
                drawLine(
                    color = Color.White.copy(alpha = 0.18f),
                    start = androidx.compose.ui.geometry.Offset(
                        1f,
                        edge + size.height * 0.12f
                    ),
                    end = androidx.compose.ui.geometry.Offset(
                        1f,
                        edge + size.height * 0.35f
                    ),
                    strokeWidth = 1.3f
                )
                drawLine(
                    color = Color.White.copy(alpha = 0.15f),
                    start = androidx.compose.ui.geometry.Offset(
                        size.width - 1f,
                        size.height * 0.54f
                    ),
                    end = androidx.compose.ui.geometry.Offset(
                        size.width - 1f,
                        size.height * 0.76f
                    ),
                    strokeWidth = 1.2f
                )
                drawLine(
                    color = Color.White.copy(alpha = 0.16f),
                    start = androidx.compose.ui.geometry.Offset(
                        size.width * 0.44f,
                        1f
                    ),
                    end = androidx.compose.ui.geometry.Offset(
                        size.width * 0.70f,
                        1f
                    ),
                    strokeWidth = 1.25f
                )
                drawLine(
                    color = Color.White.copy(alpha = 0.14f),
                    start = androidx.compose.ui.geometry.Offset(
                        size.width * 0.15f,
                        size.height - 1f
                    ),
                    end = androidx.compose.ui.geometry.Offset(
                        size.width * 0.34f,
                        size.height - 1f
                    ),
                    strokeWidth = 1.1f
                )
            }
        }

        // Clear plastic/glass reflection over the whole sleeve.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(8.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.11f),
                            Color.Transparent,
                            Color.White.copy(alpha = 0.025f),
                            Color.Transparent
                        ),
                        start = androidx.compose.ui.geometry.Offset(
                            0f,
                            0f
                        ),
                        end = androidx.compose.ui.geometry.Offset(
                            3000f,
                            3000f
                        )
                    )
                )
        )

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(8.dp))
        ) {
            // Broad diagonal glass catch-light.
            drawLine(
                color = Color.White.copy(alpha = 0.10f),
                start = androidx.compose.ui.geometry.Offset(
                    x = size.width * 0.08f,
                    y = size.height * 0.14f
                ),
                end = androidx.compose.ui.geometry.Offset(
                    x = size.width * 0.46f,
                    y = size.height * 0.02f
                ),
                strokeWidth = 2.2f
            )

            // Thin top-edge reflection.
            drawLine(
                color = Color.White.copy(alpha = 0.17f),
                start = androidx.compose.ui.geometry.Offset(
                    x = size.width * 0.14f,
                    y = 1.2f
                ),
                end = androidx.compose.ui.geometry.Offset(
                    x = size.width * 0.78f,
                    y = 1.2f
                ),
                strokeWidth = 1.3f
            )

            // Small lower reflection makes the plastic feel dimensional.
            drawLine(
                color = Color.White.copy(alpha = 0.055f),
                start = androidx.compose.ui.geometry.Offset(
                    x = size.width * 0.32f,
                    y = size.height - 1.5f
                ),
                end = androidx.compose.ui.geometry.Offset(
                    x = size.width * 0.88f,
                    y = size.height - 1.5f
                ),
                strokeWidth = 1.0f
            )
        }
    }
}

@Composable
private fun LibrarySongRow(
    song: Song,
    selected: Boolean,
    onPlay: (Song) -> Unit
) {
    // Fixed height keeps grid rows stable when the active track changes.
    val cardHeight = 194.dp
    val artworkSize = if (selected) 160.dp else 148.dp
    val artworkAreaHeight = 164.dp

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(cardHeight)
            .clip(RoundedCornerShape(16.dp))
            .then(
                if (selected) {
                    Modifier
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF292B2E),
                                    Color(0xFF151618)
                                )
                            ),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .graphicsLayer {
                            scaleX = 1.012f
                            scaleY = 1.012f
                        }
                } else {
                    Modifier
                }
            )
            .clickable { onPlay(song) }
            .padding(horizontal = 4.dp, vertical = 6.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .height(artworkAreaHeight)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {


                Box(
                    modifier = Modifier
                        .size(artworkSize)
                        .clip(RoundedCornerShape(7.dp))
                        .background(Color(0xFF0A0A0A))
                        .padding(2.dp)
                ) {
                    WornCoverArtwork(
                        song = song,
                        maxSizePx = 320,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Text(
                text = song.title,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                fontSize = if (selected) 13.sp else 12.sp,
                lineHeight = 13.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
            )

            Text(
                text = song.artist,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                fontSize = 9.5.sp,
                lineHeight = 10.sp,
                color = if (selected) Color(0xFFD2D2D2) else Color(0xFFAAAAAA),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
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