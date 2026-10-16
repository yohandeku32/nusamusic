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
import android.graphics.Typeface
import android.content.SharedPreferences
import org.json.JSONArray
import java.text.Normalizer
import kotlin.math.abs
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.WindowCompat
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
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
import com.yohandeku32.nusamusic.data.ArtworkBlur
import com.yohandeku32.nusamusic.data.ImmersiveArtworkCache
import com.yohandeku32.nusamusic.data.ImmersiveArtworkSnapshot
import com.yohandeku32.nusamusic.data.AppleMusicAnimatedArtworkLoader
import com.yohandeku32.nusamusic.data.AudioCodecInfo
import com.yohandeku32.nusamusic.data.AudioCodecLoader
import com.yohandeku32.nusamusic.data.ArtistImageLoader
import com.yohandeku32.nusamusic.data.ArtistBiographyLoader
import com.yohandeku32.nusamusic.data.ArtistNameUtils
import com.yohandeku32.nusamusic.data.MusicRepository
import com.yohandeku32.nusamusic.data.LyricsRepository
import com.yohandeku32.nusamusic.model.PlayerPresentationMode
import com.yohandeku32.nusamusic.model.LibraryContentState
import com.yohandeku32.nusamusic.model.Song
import com.yohandeku32.nusamusic.playback.PlaybackService
import com.yohandeku32.nusamusic.ui.vinylLabelDiameter
import com.yohandeku32.nusamusic.ui.VinylLabelPainter
import com.yohandeku32.nusamusic.ui.VinylPainter
import com.yohandeku32.nusamusic.ui.VinylShadowPainter
import com.yohandeku32.nusamusic.ui.PlayerSettingsContent
import com.yohandeku32.nusamusic.ui.CdSongGrid
import com.yohandeku32.nusamusic.ui.CdPlayer
import com.yohandeku32.nusamusic.ui.TurntablePlayer
import com.yohandeku32.nusamusic.ui.TurntableLabels
import com.yohandeku32.nusamusic.ui.VinylSongList
import com.yohandeku32.nusamusic.ui.resolveArtworkBottom
import com.yohandeku32.nusamusic.ui.ImmersiveArtworkPainter
import com.yohandeku32.nusamusic.ui.ImmersiveArtistPage
import com.yohandeku32.nusamusic.ui.ImmersiveArtistPageState
import com.yohandeku32.nusamusic.ui.ImmersiveLyricsPage
import com.yohandeku32.nusamusic.ui.ImmersiveLyricsState
import com.yohandeku32.nusamusic.ui.MiuixHorizontalPager
import com.yohandeku32.nusamusic.ui.MiuixPlayerLyricsPager
import com.yohandeku32.nusamusic.ui.animateMiuixScrollToPage
import com.yohandeku32.nusamusic.ui.artworkBackdropColor
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
    // Avoid checking every Media3 queue item whenever a carousel disc is tapped.
    private var queuedPlaybackOrder: List<Song>? = null
    private var pendingPlaySongJob: Job? = null
    private var songs by mutableStateOf<List<Song>>(emptyList())
    private var currentSong by mutableStateOf<Song?>(null)
    private var isPlaying by mutableStateOf(false)
    private val playbackPositionState = mutableLongStateOf(0L)
    private var positionMs by playbackPositionState
    private var durationMs by mutableLongStateOf(0L)
    private var playerPageVisible = true
    private var lyricsPageVisible = false
    private var permissionGranted by mutableStateOf(false)
    private var shuffleEnabled by mutableStateOf(false)
    private var repeatMode by mutableIntStateOf(Player.REPEAT_MODE_OFF)
    private var customTitleFontPath by mutableStateOf<String?>(null)
    private var customTitleFontName by mutableStateOf<String?>(null)

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
    private var libraryCacheLoaded by mutableStateOf(false)
    private var libraryScanFailed by mutableStateOf(false)
    private var scanAfterCacheLoad = false

    private val musicContentObserver = object : ContentObserver(
        Handler(Looper.getMainLooper())
    ) {
        override fun onChange(selfChange: Boolean, uri: Uri?) {
            super.onChange(selfChange, uri)

            if (!permissionGranted) return
            if (isScanningMusic) return

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

    private val titleFontPicker =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri == null) return@registerForActivityResult

            val displayName = runCatching {
                contentResolver.query(
                    uri,
                    arrayOf(
                        android.provider.OpenableColumns.DISPLAY_NAME
                    ),
                    null,
                    null,
                    null
                )?.use { cursor ->
                    val index = cursor.getColumnIndex(
                        android.provider.OpenableColumns.DISPLAY_NAME
                    )
                    if (index >= 0 && cursor.moveToFirst()) {
                        cursor.getString(index)
                    } else {
                        null
                    }
                }
            }.getOrNull()
                ?.takeIf { it.isNotBlank() }
                ?: (uri.lastPathSegment ?: "custom-font")

            val extension = displayName
                .substringAfterLast('.', "")
                .lowercase(Locale.ROOT)

            if (extension != "ttf" && extension != "otf") {
                android.widget.Toast.makeText(
                    this,
                    nusaText(
                        "Pilih file font .TTF atau .OTF",
                        "Please choose a .TTF or .OTF font file"
                    ),
                    android.widget.Toast.LENGTH_SHORT
                ).show()
                return@registerForActivityResult
            }

            lifecycleScope.launch(Dispatchers.IO) {
                val result = runCatching {
                    val fontsDir = java.io.File(
                        filesDir,
                        "custom_fonts"
                    ).apply { mkdirs() }

                    val safeBaseName = displayName
                        .replace(Regex("[^A-Za-z0-9._-]"), "_")

                    val targetFile = java.io.File(
                        fontsDir,
                        System.currentTimeMillis().toString() +
                            "_" + safeBaseName
                    )

                    contentResolver.openInputStream(uri)?.use { input ->
                        targetFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    } ?: error("Unable to open font file")

                    // Verify immediately so a corrupt/unsupported file never
                    // becomes the active font.
                    Typeface.createFromFile(targetFile)

                    targetFile
                }

                withContext(Dispatchers.Main) {
                    result.onSuccess { targetFile ->
                        val oldPath = customTitleFontPath

                        customTitleFontPath = targetFile.absolutePath
                        customTitleFontName = displayName

                        uiPrefsForFonts().edit()
                            .putString(
                                "custom_title_font_path",
                                targetFile.absolutePath
                            )
                            .putString(
                                "custom_title_font_name",
                                displayName
                            )
                            .putString(
                                "title_font_family",
                                "Custom Font"
                            )
                            .apply()

                        if (
                            oldPath != null &&
                            oldPath != targetFile.absolutePath
                        ) {
                            runCatching {
                                java.io.File(oldPath).delete()
                            }
                        }

                        android.widget.Toast.makeText(
                            this@MainActivity,
                            nusaText(
                                "Font berhasil diterapkan",
                                "Font applied successfully"
                            ),
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }.onFailure {
                        android.widget.Toast.makeText(
                            this@MainActivity,
                            nusaText(
                                "Font tidak dapat digunakan",
                                "This font cannot be used"
                            ),
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
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

        customTitleFontPath = uiPrefsForFonts()
            .getString("custom_title_font_path", null)
            ?.takeIf { java.io.File(it).exists() }

        customTitleFontName = uiPrefsForFonts()
            .getString("custom_title_font_name", null)
            ?.takeIf { customTitleFontPath != null }

        permissionGranted = checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED

        contentResolver.registerContentObserver(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            true,
            musicContentObserver
        )

        // Restore the last track before Compose draws the first frame. The full
        // library and Media3 controller can finish initializing in the background.
        currentSong = readLastSongForLaunch()
        if (PlayerPresentationMode.fromPreference(
                uiPrefsForFonts().getString("player_presentation_mode", null)
            ) == PlayerPresentationMode.IMMERSIVE_ARTWORK
        ) {
            currentSong?.let { song ->
                // Warm the small launch preview in parallel with the library read,
                // so Compose can use it on its first frame whenever already ready.
                lifecycleScope.launch { ImmersiveArtworkCache.restore(this@MainActivity, song) }
            }
        }
        loadCachedSongs()
        if (!permissionGranted) {
            permissionLauncher.launch(permission)
        }

        val token = SessionToken(this, ComponentName(this, PlaybackService::class.java))
        val future = MediaController.Builder(this, token).buildAsync()
        future.addListener({
            val c = future.get()
            controller = c
            queuedPlaybackOrder = null
            c.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(playing: Boolean) {
                    isPlaying = playing
                    persistPlaybackState(c)
                }

                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    val id = mediaItem?.mediaId?.toLongOrNull()
                    currentSong = songs.firstOrNull { it.id == id } ?: currentSong
                    positionMs = c.currentPosition.coerceAtLeast(0L)
                    durationMs = c.duration.coerceAtLeast(0L)
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
            lifecycleScope.launch {
                restorePlaybackStateIfNeeded(c)
            }
        }, mainExecutor)

        lifecycleScope.launch {
            while (isActive) {
                val playbackUiActive = lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.STARTED)
                controller?.let { c ->
                    if (playbackUiActive && (playerPageVisible || lyricsPageVisible)) {
                        positionMs = c.currentPosition.coerceAtLeast(0L)
                        durationMs = c.duration.coerceAtLeast(0L)
                    }
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
                delay(when {
                    isPlaying && playbackUiActive && lyricsPageVisible -> 50L
                    isPlaying && playbackUiActive && playerPageVisible -> 250L
                    else -> 1_000L
                })
            }
        }

        setContent {
            NusaMusicTheme {
                NusaMusicApp(
                    songs = songs,
                    currentSong = currentSong,
                    isPlaying = isPlaying,
                    positionMsState = playbackPositionState,
                    durationMs = durationMs,
                    permissionGranted = permissionGranted,
                    onPlay = ::playSong,
                    onTogglePlay = ::togglePlay,
                    customTitleFontPath = customTitleFontPath,
                    customTitleFontName = customTitleFontName,
                    onChooseCustomTitleFont = {
                        titleFontPicker.launch(
                            arrayOf(
                                "font/ttf",
                                "font/otf",
                                "application/x-font-ttf",
                                "application/octet-stream",
                                "*/*"
                            )
                        )
                    },
                    onClearCustomTitleFont = {
                        val oldPath = customTitleFontPath
                        customTitleFontPath = null
                        customTitleFontName = null

                        if (oldPath != null) {
                            runCatching {
                                java.io.File(oldPath).delete()
                            }
                        }
                    },
                    onResetCustomTitleFont = {
                        val oldPath = customTitleFontPath
                        customTitleFontPath = null
                        customTitleFontName = null

                        uiPrefsForFonts().edit()
                            .remove("custom_title_font_path")
                            .remove("custom_title_font_name")
                            .putString(
                                "title_font_family",
                                "Serif"
                            )
                            .apply()

                        if (oldPath != null) {
                            runCatching {
                                java.io.File(oldPath).delete()
                            }
                        }
                    },
                    onNext = ::nextSong,
                    onPrevious = ::previousSong,
                    onSeek = ::seekTo,
                    onShare = ::shareCurrentSong,
                    onToggleShuffle = ::toggleShuffle,
                    onToggleRepeat = ::toggleRepeat,
                    shuffleEnabled = shuffleEnabled,
                    repeatMode = repeatMode,
                    isScanningMusic = isScanningMusic,
                    libraryCacheLoaded = libraryCacheLoaded,
                    libraryScanFailed = libraryScanFailed,
                    selectedMusicFolders = selectedMusicFolders,
                    onPlaybackPageVisibilityChanged = ::setPlaybackPageVisible,
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
    }

    private fun syncCurrentSong(c: MediaController) {
        val id = c.currentMediaItem?.mediaId?.toLongOrNull() ?: return
        songs.firstOrNull { it.id == id }?.let { currentSong = it }
    }

    /**
     * Resolves the previous track from a small persisted snapshot first. On
     * upgrades where that snapshot does not yet exist, fall back to the cached
     * library and extract only the matching song rather than parsing every row.
     */
    private fun readLastSongForLaunch(): Song? {
        val mediaId = playbackPrefs.getString("media_id", null) ?: return null
        val snapshotId = playbackPrefs.getString("last_song_id", null)

        if (
            snapshotId == mediaId &&
            !playbackPrefs.getString("last_song_uri", null).isNullOrBlank()
        ) {
            return runCatching {
                Song(
                    id = snapshotId.toLong(),
                    title = playbackPrefs.getString("last_song_title", null)
                        ?: nusaText("Judul tidak diketahui", "Unknown title"),
                    artist = playbackPrefs.getString("last_song_artist", null)
                        ?: nusaText("Artis tidak diketahui", "Unknown artist"),
                    album = playbackPrefs.getString("last_song_album", null)
                        ?: nusaText("Album tidak diketahui", "Unknown album"),
                    uri = requireNotNull(playbackPrefs.getString("last_song_uri", null)),
                    durationMs = playbackPrefs.getLong("last_song_duration_ms", 0L),
                    albumId = playbackPrefs.getLong("last_song_album_id", 0L),
                    dateAddedMs = playbackPrefs.getLong("last_song_date_added_ms", 0L)
                )
            }.getOrNull()
        }

        // One-time migration path for existing installs that only have the
        // older songs_cache_json + media_id values.
        val raw = libraryPrefs.getString("songs_cache_json", null) ?: return null
        return runCatching {
            val array = JSONArray(raw)
            var match: Song? = null
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                if (item.optString("id") != mediaId) continue

                match = Song(
                    id = item.getLong("id"),
                    title = item.getString("title"),
                    artist = item.getString("artist"),
                    album = item.getString("album"),
                    uri = item.getString("uri"),
                    durationMs = item.getLong("durationMs"),
                    albumId = item.getLong("albumId"),
                    dateAddedMs = item.optLong("dateAddedMs", 0L)
                )
                break
            }
            match
        }.getOrNull()
    }

    private fun setPlaybackPageVisible(playerVisible: Boolean, lyricsVisible: Boolean) {
        playerPageVisible = playerVisible
        lyricsPageVisible = lyricsVisible
        if (!playerVisible && !lyricsVisible) return

        controller?.let { c ->
            positionMs = c.currentPosition.coerceAtLeast(0L)
            durationMs = c.duration.coerceAtLeast(0L)
            isPlaying = c.isPlaying
        }
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

        val snapshotSong = currentSong?.takeIf { it.id.toString() == mediaId }
            ?: songs.firstOrNull { it.id.toString() == mediaId }

        val editor = playbackPrefs.edit()
            .putString("media_id", mediaId)
            .putLong("position_ms", currentPosition)
            .putBoolean("is_playing", c.isPlaying)
            .putBoolean("shuffle_enabled", c.shuffleModeEnabled)
            .putInt("repeat_mode", c.repeatMode)

        // Keep a compact snapshot of the active song so the player can render
        // it immediately on the next cold start, without waiting for library JSON.
        snapshotSong?.let { song ->
            editor
                .putString("last_song_id", song.id.toString())
                .putString("last_song_title", song.title)
                .putString("last_song_artist", song.artist)
                .putString("last_song_album", song.album)
                .putString("last_song_uri", song.uri)
                .putLong("last_song_duration_ms", song.durationMs)
                .putLong("last_song_album_id", song.albumId)
                .putLong("last_song_date_added_ms", song.dateAddedMs)
        }

        editor.apply()
        lastPersistedPosition = currentPosition
    }

    private suspend fun restorePlaybackStateIfNeeded(c: MediaController) {
        val songSnapshot = songs
        if (songSnapshot.isEmpty() || c.currentMediaItem != null) return

        val mediaId = playbackPrefs.getString("media_id", null) ?: return
        val index = songSnapshot.indexOfFirst { it.id.toString() == mediaId }
        if (index < 0) return

        val savedPosition = playbackPrefs.getLong("position_ms", 0L).coerceAtLeast(0L)
        val savedPlaying = playbackPrefs.getBoolean("is_playing", false)
        val savedShuffle = playbackPrefs.getBoolean("shuffle_enabled", false)
        val savedRepeat = playbackPrefs.getInt("repeat_mode", Player.REPEAT_MODE_OFF)

        // MediaItem construction can be expensive for large libraries. Prepare
        // the lightweight queue objects off the main thread first.
        val mediaItems = withContext(Dispatchers.Default) {
            songSnapshot.map(::mediaItemFor)
        }

        // A scan may have completed while the queue was being prepared.
        if (songs !== songSnapshot || c.currentMediaItem != null) return

        val batchSize = 48

        // Start with the saved track, then build the surrounding queue in small
        // batches. This avoids one large setMediaItems() call freezing the UI.
        c.shuffleModeEnabled = false
        c.setMediaItems(listOf(mediaItems[index]), 0, savedPosition)

        var nextIndex = index + 1
        while (nextIndex < mediaItems.size) {
            val endIndex = minOf(nextIndex + batchSize, mediaItems.size)
            c.addMediaItems(mediaItems.subList(nextIndex, endIndex))
            nextIndex = endIndex
            kotlinx.coroutines.yield()
        }

        // Prepend earlier tracks from nearest to farthest so the final queue
        // retains the same library order with the saved track at its old index.
        var prefixEnd = index
        while (prefixEnd > 0) {
            val prefixStart = (prefixEnd - batchSize).coerceAtLeast(0)
            c.addMediaItems(0, mediaItems.subList(prefixStart, prefixEnd))
            prefixEnd = prefixStart
            kotlinx.coroutines.yield()
        }

        c.shuffleModeEnabled = savedShuffle
        c.repeatMode = savedRepeat
        c.prepare()
        queuedPlaybackOrder = songSnapshot

        currentSong = songSnapshot[index]
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

            val shouldScan = (cachedSongs == null || scanAfterCacheLoad) &&
                (permissionGranted || selectedMusicFolders.isNotEmpty())
            scanAfterCacheLoad = false
            libraryCacheLoaded = true
            // Start the initial scan before playback restoration can suspend.
            // Compose must not draw a ready-but-empty library in that gap.
            if (shouldScan) scanMusic()

            controller?.let { c ->
                syncCurrentSong(c)
                restorePlaybackStateIfNeeded(c)
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
                            albumId = item.getLong("albumId"),
                            dateAddedMs = item.optLong("dateAddedMs", 0L)
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
                    put("dateAddedMs", song.dateAddedMs)
                }
            )
        }

        libraryPrefs.edit()
            .putString("songs_cache_json", array.toString())
            .apply()
    }

    private suspend fun refreshSongs(): Int {
        // Snapshot Compose state on the main thread, then do indexing, cache
        // serialization, ID comparison and MediaItem preparation off-thread.
        val cachedSongs = songs
        val previousIds = withContext(Dispatchers.Default) {
            cachedSongs.mapTo(HashSet()) { it.id }
        }
        val folderUris = selectedMusicFolders.toSet()

        val refreshedSongs = withContext(Dispatchers.IO) {
            MusicRepository(this@MainActivity)
                .loadSongs(
                    extraFolderUris = folderUris,
                    cachedSongs = cachedSongs
                )
                .also(::persistCachedSongs)
        }

        val newSongs = withContext(Dispatchers.Default) {
            if (previousIds.isEmpty()) {
                emptyList()
            } else {
                refreshedSongs.filter { it.id !in previousIds }
            }
        }

        // Publish the new library only after its cache has been persisted.
        songs = refreshedSongs

        controller?.let { c ->
            syncCurrentSong(c)
            restorePlaybackStateIfNeeded(c)

            // Keep the current queue intact, but append genuinely new tracks in
            // small batches so one large refresh cannot monopolize the UI thread.
            if (newSongs.isNotEmpty() && c.mediaItemCount > 0) {
                val queuedIds = HashSet<Long>(c.mediaItemCount)
                for (index in 0 until c.mediaItemCount) {
                    c.getMediaItemAt(index).mediaId.toLongOrNull()?.let {
                        queuedIds += it
                    }
                }

                val queueAdditions = withContext(Dispatchers.Default) {
                    newSongs.asSequence()
                        .filter { it.id !in queuedIds }
                        .map(::mediaItemFor)
                        .toList()
                }

                queueAdditions.chunked(32).forEach { batch ->
                    c.addMediaItems(batch)
                    kotlinx.coroutines.yield()
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
        if (!libraryCacheLoaded) {
            scanAfterCacheLoad = true
            return
        }
        if (isScanningMusic) return
        if (!permissionGranted && selectedMusicFolders.isEmpty()) {
            return
        }

        automaticMusicScanJob?.cancel()
        libraryScanFailed = false
        isScanningMusic = true

        lifecycleScope.launch {
            val result = runCatching { refreshSongs() }
            libraryScanFailed = result.isFailure
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

    private fun uiPrefsForFonts(): SharedPreferences {
        return getSharedPreferences(
            "ui_preferences",
            android.content.Context.MODE_PRIVATE
        )
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

    private fun playSong(song: Song, orderedSongs: List<Song>) {
        val c = controller ?: return
        val queueSongs = orderedSongs.ifEmpty { songs }
        val targetIndex = queueSongs.indexOfFirst { it.id == song.id }
        if (targetIndex < 0) return

        // Avoid an O(library size) series of MediaController calls for every
        // carousel tap. This queue only changes when we explicitly load an order.
        val queueMatches = queuedPlaybackOrder === queueSongs &&
            c.mediaItemCount == queueSongs.size &&
            c.getMediaItemAt(targetIndex).mediaId == song.id.toString()

        pendingPlaySongJob?.cancel()
        pendingPlaySongJob = null

        if (queueMatches) {
            c.seekTo(targetIndex, 0L)
            c.play()
            currentSong = song
            isPlaying = true
            return
        }

        // Build large playlists off the UI thread. Rapid taps cancel older
        // requests instead of stacking expensive full-queue rebuilds.
        queuedPlaybackOrder = null
        pendingPlaySongJob = lifecycleScope.launch {
            val mediaItems = withContext(Dispatchers.Default) {
                queueSongs.map(::mediaItemFor)
            }
            if (controller !== c) return@launch

            c.setMediaItems(mediaItems, targetIndex, 0L)
            c.prepare()
            c.play()
            queuedPlaybackOrder = queueSongs
            currentSong = song
            isPlaying = true
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
        positionMs = value.coerceAtLeast(0L)
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
        pendingPlaySongJob?.cancel()
        pendingPlaySongJob = null
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
    modifier: Modifier = Modifier,
    color: Color = androidx.compose.material3.LocalContentColor.current
) {
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
    color: Color = Color.Unspecified,
    modifier: Modifier = Modifier
) {
    val resolvedColor = if (selected) {
        Color(0xFFFF4F6D)
    } else if (color != Color.Unspecified) {
        color
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
                color = resolvedColor
            )
        } else {
            drawPath(
                path = path,
                color = resolvedColor,
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
    TITLE_ASC("Title A–Z", "Title A–Z"),
    TITLE_DESC("Title Z–A", "Title Z–A"),
    ARTIST_ASC("Artist A–Z", "Artist A–Z"),
    RECENTLY_ADDED("Recently Added", "Recently Added"),
    ALBUM_ASC("Album A–Z", "Album A–Z"),
    DURATION_ASC("Shortest duration", "Shortest duration"),
    DURATION_DESC("Longest duration", "Longest duration");

    val label: String
        get() = englishLabel
}

private data class SortedLibrary(
    val songs: List<Song>,
    val alphabetTargets: Map<Char, Int>
)

private fun alphabetIndexKey(title: String): Char {
    val normalizedTitle = Normalizer.normalize(
        title.trim(),
        Normalizer.Form.NFD
    )
    val firstBaseCharacter = normalizedTitle.firstOrNull { character ->
        when (Character.getType(character)) {
            Character.NON_SPACING_MARK.toInt(),
            Character.COMBINING_SPACING_MARK.toInt(),
            Character.ENCLOSING_MARK.toInt() -> false
            else -> true
        }
    } ?: return '#'

    return firstBaseCharacter
        .uppercaseChar()
        .takeIf { it in 'A'..'Z' }
        ?: '#'
}

private data class StackAlbum(
    val key: String,
    val title: String,
    val artist: String,
    val coverSong: Song,
    val tracks: List<Song>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NusaMusicApp(
    songs: List<Song>,
    currentSong: Song?,
    isPlaying: Boolean,
    positionMsState: State<Long>,
    durationMs: Long,
    permissionGranted: Boolean,
    onPlay: (Song, List<Song>) -> Unit,
    onTogglePlay: () -> Unit,
    customTitleFontPath: String?,
    customTitleFontName: String?,
    onChooseCustomTitleFont: () -> Unit,
    onClearCustomTitleFont: () -> Unit,
    onResetCustomTitleFont: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onShare: (Song?) -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    shuffleEnabled: Boolean,
    repeatMode: Int,
    isScanningMusic: Boolean,
    libraryCacheLoaded: Boolean,
    libraryScanFailed: Boolean,
    selectedMusicFolders: List<String>,
    onPlaybackPageVisibilityChanged: (Boolean, Boolean) -> Unit,
    onScanMusic: () -> Unit,
    onSelectMusicFolder: () -> Unit,
    onRemoveMusicFolder: (String) -> Unit,
    onRequestPermission: () -> Unit
) {
    var isFavorite by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var biographyExpanded by remember { mutableStateOf(false) }
    val artistKey = ArtistNameUtils.cacheKey(currentSong?.artist)
    var artistBiography by remember(artistKey) {
        mutableStateOf(ArtistBiographyLoader.cached(currentSong?.artist))
    }
    var biographyLoading by remember(artistKey) {
        mutableStateOf(false)
    }

    val context = androidx.compose.ui.platform.LocalContext.current
    val lyricsState = remember(context) { ImmersiveLyricsState(LyricsRepository(context)) }
    val artistPageStates = remember { androidx.collection.LruCache<String, ImmersiveArtistPageState>(8) }
    val artistPageState = remember(artistKey) {
        artistPageStates.get(artistKey) ?: ImmersiveArtistPageState(
            ArtistNameUtils.firstArtist(currentSong?.artist)
        ).also { artistPageStates.put(artistKey, it) }
    }
    val uiPrefs = remember(context) {
        context.getSharedPreferences("ui_preferences", android.content.Context.MODE_PRIVATE)
    }
    var titleFontSize by remember {
        mutableStateOf(uiPrefs.getFloat("title_font_size", 34f))
    }
    var titleFontName by remember {
        mutableStateOf(
            uiPrefs.getString("title_font_family", "Serif") ?: "Serif"
        )
    }
    var playerPresentationMode by remember {
        mutableStateOf(
            PlayerPresentationMode.fromPreference(
                uiPrefs.getString("player_presentation_mode", null)
            )
        )
    }
    val showArtistBiography = playerPresentationMode != PlayerPresentationMode.CD
    var animatedArtworkEnabled by remember {
        mutableStateOf(uiPrefs.getBoolean("animated_artwork_enabled", true))
    }
    var roundedCarouselCorners by remember {
        mutableStateOf(uiPrefs.getBoolean("rounded_carousel_corners", true))
    }
    var immersiveArtworkBottomPx by remember { mutableFloatStateOf(Float.NaN) }
    var immersiveHasQualityBadge by remember { mutableStateOf(true) }
    val immersiveFrameCoordinates = remember { mutableStateOf<LayoutCoordinates?>(null) }
    val immersiveArtistCoordinates = remember { mutableStateOf<LayoutCoordinates?>(null) }

    fun updateImmersiveArtworkBottom() {
        val frame = immersiveFrameCoordinates.value ?: return
        val artist = immersiveArtistCoordinates.value ?: return
        if (!frame.isAttached || !artist.isAttached) return
        val bottom = frame.localPositionOf(
            artist,
            androidx.compose.ui.geometry.Offset(0f, artist.size.height.toFloat())
        ).y
        if (bottom.isFinite() && bottom > 0f &&
            (!immersiveArtworkBottomPx.isFinite() || abs(bottom - immersiveArtworkBottomPx) > 0.5f)
        ) immersiveArtworkBottomPx = bottom
    }
    var customTitleTypeface by remember {
        mutableStateOf<Typeface?>(null)
    }

    LaunchedEffect(showSettings) {
        if (showSettings) {
            // Modal sheets can temporarily reveal the status bar on some Android versions.
            // Hide it again after the sheet has finished opening.
            delay(180)
            val activity = context as? MainActivity
            if (activity != null) {
                androidx.core.view.WindowInsetsControllerCompat(
                    activity.window,
                    activity.window.decorView
                ).hide(androidx.core.view.WindowInsetsCompat.Type.statusBars())
            }
        }
    }

    LaunchedEffect(customTitleFontPath) {
        customTitleTypeface = customTitleFontPath?.let { path ->
            withContext(Dispatchers.IO) {
                runCatching {
                    Typeface.createFromFile(path)
                }.getOrNull()
            }
        }
    }

    val titleFontFamily = remember(
        titleFontName,
        customTitleTypeface
    ) {
        customTitleTypeface?.let {
            FontFamily(it)
        } ?: titleFontFamilyFor(titleFontName)
    }
    var realisticControls by remember {
        mutableStateOf(uiPrefs.getBoolean("realistic_controls", true))
    }
    val playbackPrefs = remember(context) {
        context.getSharedPreferences(
            "playback_preferences",
            android.content.Context.MODE_PRIVATE
        )
    }
    var crossfadeEnabled by remember {
        mutableStateOf(playbackPrefs.getBoolean("crossfade_enabled", false))
    }
    var crossfadeDurationSeconds by remember {
        mutableStateOf(
            (playbackPrefs.getLong("crossfade_duration_ms", 5_000L) / 1_000L)
                .coerceIn(1L, 12L)
                .toFloat()
        )
    }
    var sortMenuExpanded by remember { mutableStateOf(false) }
    var librarySortOption by remember {
        mutableStateOf(
            runCatching {
                LibrarySortOption.valueOf(
                    uiPrefs.getString(
                        "library_sort_option",
                        LibrarySortOption.TITLE_ASC.name
                    ) ?: LibrarySortOption.TITLE_ASC.name
                )
            }.getOrDefault(LibrarySortOption.TITLE_ASC)
        )
    }

    // Sort and calculate the A–Z index outside Compose's main thread.
    // Retain the last ready list during rapid sort changes, so the player and
    // CD carousel never receive an intermediate, partially sorted list.
    var sortedLibrary by remember(songs) {
        mutableStateOf(SortedLibrary(songs, emptyMap()))
    }
    LaunchedEffect(songs, librarySortOption) {
        val prepared = withContext(Dispatchers.Default) {
            val ordered = when (librarySortOption) {
                LibrarySortOption.TITLE_ASC ->
                    songs.sortedBy { it.title.lowercase(Locale.ROOT) }
                LibrarySortOption.TITLE_DESC ->
                    songs.sortedByDescending { it.title.lowercase(Locale.ROOT) }
                LibrarySortOption.ARTIST_ASC ->
                    songs.sortedBy { it.artist.lowercase(Locale.ROOT) }
                LibrarySortOption.RECENTLY_ADDED ->
                    songs.sortedWith(
                        compareByDescending<Song> { it.dateAddedMs }
                            .thenBy { it.title.lowercase(Locale.ROOT) }
                    )
                LibrarySortOption.ALBUM_ASC ->
                    songs.sortedBy { it.album.lowercase(Locale.ROOT) }
                LibrarySortOption.DURATION_ASC ->
                    songs.sortedBy { it.durationMs }
                LibrarySortOption.DURATION_DESC ->
                    songs.sortedByDescending { it.durationMs }
            }
            val targets = buildMap<Char, Int> {
                ordered.forEachIndexed { index, track ->
                    val letter = alphabetIndexKey(track.title)
                    if (letter !in this) put(letter, index)
                }
            }
            SortedLibrary(ordered, targets)
        }
        sortedLibrary = prepared
    }
    val filtered = sortedLibrary.songs
    val hasMusicAccess = permissionGranted || selectedMusicFolders.isNotEmpty()
    val libraryContentState = LibraryContentState.resolve(
        hasSongs = songs.isNotEmpty(),
        hasAccess = hasMusicAccess,
        cacheLoaded = libraryCacheLoaded,
        scanning = isScanningMusic,
        scanFailed = libraryScanFailed
    )
    val emptyLibraryMessage = when (libraryContentState) {
        LibraryContentState.LOADING -> nusaText("Memuat koleksi musik…", "Loading your music…")
        LibraryContentState.PERMISSION_REQUIRED -> nusaText("Izinkan akses musik untuk mulai memutar.", "Allow music access to start listening.")
        LibraryContentState.SCAN_FAILED -> nusaText("Musik belum dapat dimuat. Coba pindai ulang.", "Unable to load music. Try scanning again.")
        LibraryContentState.EMPTY -> nusaText("Tidak ada musik lokal", "No local music found")
        LibraryContentState.CONTENT -> ""
    }
    val alphabetTargets = sortedLibrary.alphabetTargets

    // Sorting is a visual library action; it must never reconstruct or move
    // thousands of entries in an actively playing Media3 queue. When a song
    // is selected, playSong() already applies the currently displayed order.

    val alphabet = remember {
        listOf('#') + ('A'..'Z')
    }

    // The artist sits to the left, so a rightward swipe from the player opens it.
    val artistPageEnabled = playerPresentationMode == PlayerPresentationMode.IMMERSIVE_ARTWORK
    val playerPage = if (artistPageEnabled) 1 else 0
    val libraryPage = playerPage + 1
    val lyricsPagerState = androidx.compose.runtime.key(artistPageEnabled) {
        androidx.compose.foundation.pager.rememberPagerState(pageCount = { 2 })
    }
    var libraryWasVisible by remember { mutableStateOf(false) }
    val pagerState = androidx.compose.runtime.key(artistPageEnabled) {
        androidx.compose.foundation.pager.rememberPagerState(
            initialPage = if (libraryWasVisible) libraryPage else playerPage,
            pageCount = { if (artistPageEnabled) 3 else 2 }
        )
    }
    val playerScrollState =
        androidx.compose.foundation.lazy.rememberLazyListState()
    val libraryListState =
        androidx.compose.foundation.lazy.rememberLazyListState()
    // Cached covers remain visible while scrolling; pause expensive
    // native metadata/image reads until the list has settled.
    val libraryRowLoadsEnabled by remember(libraryListState) {
        derivedStateOf { !libraryListState.isScrollInProgress }
    }
    val scope = rememberCoroutineScope()
    var alphabetVisible by remember { mutableStateOf(false) }
    var alphabetDragging by remember { mutableStateOf(false) }
    var alphabetHideJob by remember { mutableStateOf<Job?>(null) }
    val density = androidx.compose.ui.platform.LocalDensity.current
    val revealPlayerCurve by remember {
        derivedStateOf {
            playerScrollState.firstVisibleItemScrollOffset > 18
        }
    }

    fun revealAlphabet() {
        alphabetVisible = true
        alphabetHideJob?.cancel()
        alphabetHideJob = null
    }

    fun scheduleAlphabetHide() {
        alphabetHideJob?.cancel()
        if (alphabetDragging || libraryListState.isScrollInProgress) return

        alphabetHideJob = scope.launch {
            delay(3_000)
            if (!alphabetDragging && !libraryListState.isScrollInProgress) {
                alphabetVisible = false
            }
            alphabetHideJob = null
        }
    }

    LaunchedEffect(libraryListState) {
        androidx.compose.runtime.snapshotFlow {
            libraryListState.isScrollInProgress
        }.collect { isScrolling ->
            if (isScrolling) {
                revealAlphabet()
            } else if (alphabetVisible) {
                scheduleAlphabetHide()
            }
        }
    }

    // Opening the library must not change the user's manually chosen scroll position.
    LaunchedEffect(pagerState.settledPage, lyricsPagerState.settledPage, playerPage) {
        libraryWasVisible = pagerState.settledPage == libraryPage
        onPlaybackPageVisibilityChanged(
            pagerState.settledPage == playerPage && (!artistPageEnabled || lyricsPagerState.settledPage == 0),
            artistPageEnabled && pagerState.settledPage == playerPage && lyricsPagerState.settledPage == 1)
    }

    // CD is always dark, including when the device uses the light theme.
    androidx.compose.runtime.DisposableEffect(playerPresentationMode, pagerState.settledPage) {
        val activity = context as? ComponentActivity
        val insets = activity?.let { WindowCompat.getInsetsController(it.window, it.window.decorView) }
        val previousLightNavigation = insets?.isAppearanceLightNavigationBars
        if (playerPresentationMode == PlayerPresentationMode.CD && pagerState.settledPage == playerPage) {
            insets?.isAppearanceLightNavigationBars = false
        }
        onDispose {
            if (previousLightNavigation != null) insets?.isAppearanceLightNavigationBars = previousLightNavigation
        }
    }

    LaunchedEffect(artistKey, showArtistBiography) {
        biographyLoading = false
        if (!showArtistBiography) return@LaunchedEffect
        if (artistBiography != null) return@LaunchedEffect
        biographyLoading =
            ArtistNameUtils.firstArtist(currentSong?.artist).isNotBlank()
        artistBiography = currentSong?.let {
            ArtistBiographyLoader.load(context, it)
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
            // ModalBottomSheet is hosted in its own dialog window. Hiding the
            // Activity status bar alone is not enough because the dialog can
            // reveal its own system bars while opening.
            val sheetView = androidx.compose.ui.platform.LocalView.current
            val sheetWindow = (sheetView.parent as? androidx.compose.ui.window.DialogWindowProvider)?.window

            // Keep the Settings dialog in fullscreen mode. ModalBottomSheet can
            // reset system-bar visibility during its entrance/settling animation,
            // so re-apply the hidden state when its window or sheet state changes.
            val hideSettingsStatusBar: () -> Unit = {
                sheetWindow?.let { dialogWindow ->
                    dialogWindow.addFlags(android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN)
                    WindowCompat.setDecorFitsSystemWindows(dialogWindow, false)

                    @Suppress("DEPRECATION")
                    run {
                        dialogWindow.decorView.systemUiVisibility =
                            dialogWindow.decorView.systemUiVisibility or
                                android.view.View.SYSTEM_UI_FLAG_FULLSCREEN or
                                android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    }

                    androidx.core.view.WindowInsetsControllerCompat(
                        dialogWindow,
                        dialogWindow.decorView
                    ).apply {
                        systemBarsBehavior =
                            androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                        hide(androidx.core.view.WindowInsetsCompat.Type.statusBars())
                    }
                }
            }

            androidx.compose.runtime.SideEffect {
                hideSettingsStatusBar()
            }

            LaunchedEffect(sheetWindow) {
                // Keep enforcing fullscreen for the entire lifetime of Settings.
                // The sheet/dialog can restore status-bar visibility after its
                // entrance animation, so a one-time hide is not reliable.
                while (kotlinx.coroutines.currentCoroutineContext().isActive) {
                    hideSettingsStatusBar()
                    delay(100)
                }
            }

            PlayerSettingsContent(
                titleFontSize = titleFontSize,
                onTitleFontSizeChange = { titleFontSize = it },
                onTitleFontSizeChangeFinished = {
                    uiPrefs.edit().putFloat("title_font_size", titleFontSize).apply()
                },
                titleFontName = titleFontName,
                titleFontFamily = titleFontFamily,
                customTitleFontName = customTitleFontName,
                hasCustomTitleFont = customTitleFontPath != null,
                onTitleFontSelected = { fontName ->
                    titleFontName = fontName
                    onClearCustomTitleFont()
                    uiPrefs.edit()
                        .remove("custom_title_font_path")
                        .remove("custom_title_font_name")
                        .putString("title_font_family", fontName)
                        .apply()
                },
                onChooseCustomTitleFont = onChooseCustomTitleFont,
                onResetCustomTitleFont = onResetCustomTitleFont,
                playerPresentationMode = playerPresentationMode,
                onPresentationSelected = { mode ->
                    if (mode.isAvailable) {
                        playerPresentationMode = mode
                        uiPrefs.edit().putString("player_presentation_mode", mode.preferenceValue).apply()
                    }
                },
                animatedArtworkEnabled = animatedArtworkEnabled,
                roundedCarouselCorners = roundedCarouselCorners,
                onRoundedCarouselCornersChange = { rounded ->
                    roundedCarouselCorners = rounded
                    uiPrefs.edit().putBoolean("rounded_carousel_corners", rounded).apply()
                },
                onAnimatedArtworkChange = { enabled ->
                    animatedArtworkEnabled = enabled
                    uiPrefs.edit().putBoolean("animated_artwork_enabled", enabled).apply()
                },
                realisticControls = realisticControls,
                onRealisticControlsChange = { realistic ->
                    realisticControls = realistic
                    uiPrefs.edit().putBoolean("realistic_controls", realistic).apply()
                },
                crossfadeEnabled = crossfadeEnabled,
                onCrossfadeChange = { enabled ->
                    crossfadeEnabled = enabled
                    playbackPrefs.edit().putBoolean("crossfade_enabled", enabled).apply()
                },
                crossfadeDurationSeconds = crossfadeDurationSeconds,
                onCrossfadeDurationChange = { crossfadeDurationSeconds = it },
                onCrossfadeDurationChangeFinished = {
                    playbackPrefs.edit()
                        .putLong("crossfade_duration_ms", (crossfadeDurationSeconds * 1_000L).toLong())
                        .apply()
                },
                musicFolders = selectedMusicFolders.map { it to displayMusicFolderName(it) },
                isScanningMusic = isScanningMusic,
                onSelectMusicFolder = onSelectMusicFolder,
                onRemoveMusicFolder = onRemoveMusicFolder,
                onScanMusic = onScanMusic,
                onDismiss = { showSettings = false }
            )
        }
    }

    // The player and lyrics render the same progress, transport and action buttons.
    val playerControls: @Composable () -> Unit = {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            val immersiveArtworkMode = artistPageEnabled
            PlaybackProgress(
                positionMsState = positionMsState,
                durationMs = durationMs,
                enabled = currentSong != null && durationMs > 0L,
                onSeek = onSeek,
                immersive = playerPresentationMode == PlayerPresentationMode.IMMERSIVE_ARTWORK,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
            )

            Spacer(Modifier.height(5.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (immersiveArtworkMode) {
                    IconButton(
                        onClick = onPrevious,
                        enabled = currentSong != null,
                        modifier = Modifier.size(76.dp)
                    ) {
                        Icon(
                            Icons.Rounded.FastRewind,
                            contentDescription = nusaText("Sebelumnya", "Previous"),
                            tint = Color.White,
                            modifier = Modifier.size(54.dp)
                        )
                    }
                } else {
                    TransportPillButton(
                        icon = Icons.Rounded.FastRewind,
                        contentDescription = nusaText("Sebelumnya", "Previous"),
                        onClick = onPrevious,
                        enabled = currentSong != null,
                        realistic = realisticControls
                    )
                }

                Spacer(Modifier.width(if (immersiveArtworkMode) 8.dp else 12.dp))

                if (immersiveArtworkMode) {
                    IconButton(
                        onClick = if (currentSong == null) onRequestPermission else onTogglePlay,
                        enabled = true,
                        modifier = Modifier.size(104.dp)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) {
                                Icons.Rounded.Pause
                            } else {
                                Icons.Rounded.PlayArrow
                            },
                            contentDescription = if (isPlaying) {
                                nusaText("Jeda", "Pause")
                            } else {
                                nusaText("Putar", "Play")
                            },
                            tint = Color.White,
                            modifier = Modifier.size(80.dp)
                        )
                    }
                } else {
                    PlayerControlButton(
                        realistic = realisticControls,
                        icon = if (isPlaying) {
                            Icons.Rounded.Pause
                        } else {
                            Icons.Rounded.PlayArrow
                        },
                        contentDescription = if (isPlaying) {
                            nusaText("Jeda", "Pause")
                        } else {
                            nusaText("Putar", "Play")
                        },
                        onClick = if (currentSong == null) {
                            onRequestPermission
                        } else {
                            onTogglePlay
                        },
                        enabled = true,
                        modifier = Modifier.size(84.dp),
                        iconSize = 40.dp
                    )
                }

                Spacer(Modifier.width(if (immersiveArtworkMode) 8.dp else 12.dp))

                if (immersiveArtworkMode) {
                    IconButton(
                        onClick = onNext,
                        enabled = currentSong != null,
                        modifier = Modifier.size(76.dp)
                    ) {
                        Icon(
                            Icons.Rounded.FastForward,
                            contentDescription = nusaText("Berikutnya", "Next"),
                            tint = Color.White,
                            modifier = Modifier.size(54.dp)
                        )
                    }
                } else {
                    TransportPillButton(
                        icon = Icons.Rounded.FastForward,
                        contentDescription = nusaText("Berikutnya", "Next"),
                        onClick = onNext,
                        enabled = currentSong != null,
                        realistic = realisticControls
                    )
                }
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
                        if (artistPageEnabled) {
                            scope.launch { pagerState.animateMiuixScrollToPage(0); lyricsPagerState.scrollToPage(0) }
                            return@IconButton
                        }
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
                    if (artistPageEnabled) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = nusaText("Buka halaman artis • geser ke kanan", "Open artist page • swipe right"),
                            tint = Color.White,
                            modifier = Modifier.size(25.dp)
                        )
                    } else BiographyArrowIcon(
                        modifier = Modifier
                            .size(25.dp)
                            .graphicsLayer {
                                rotationZ = biographyArrowRotation
                            },
                        color = if (
                            playerPresentationMode == PlayerPresentationMode.IMMERSIVE_ARTWORK
                        ) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(Modifier.weight(1f))

                if (artistPageEnabled) {
                    IconButton(
                        onClick = { scope.launch { lyricsPagerState.animateMiuixScrollToPage(if (lyricsPagerState.currentPage == 0) 1 else 0) } },
                        enabled = currentSong != null,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Icon(if (lyricsPagerState.targetPage == 1) Icons.Default.KeyboardArrowDown else Icons.Default.Lyrics,
                            if (lyricsPagerState.targetPage == 1) nusaText("Kembali ke pemutar • geser ke bawah", "Back to player • swipe down") else nusaText("Buka lirik • geser ke atas", "Open lyrics • swipe up"),
                            tint = Color.White, modifier = Modifier.size(22.dp))
                    }
                }

                IconButton(
                    onClick = { onShare(currentSong) },
                    enabled = currentSong != null,
                    modifier = Modifier.size(42.dp)
                ) {
                    ChatGptStyleShareIcon(
                        modifier = Modifier.size(22.dp),
                        color = if (
                            playerPresentationMode == PlayerPresentationMode.IMMERSIVE_ARTWORK
                        ) Color.White else Color.Unspecified
                    )
                }

                IconButton(
                    onClick = { isFavorite = !isFavorite },
                    enabled = currentSong != null,
                    modifier = Modifier.size(42.dp)
                ) {
                    NusaFavoriteIcon(
                        selected = isFavorite,
                        color = if (
                            playerPresentationMode == PlayerPresentationMode.IMMERSIVE_ARTWORK
                        ) Color.White else Color.Unspecified,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            if (playerPresentationMode == PlayerPresentationMode.VINYL) {
                Spacer(Modifier.height(8.dp))
                AudioQualityPill(song = currentSong)
            } else {
                Spacer(Modifier.height(36.dp))
            }
        }
    }

    val pageBackdrop = when (playerPresentationMode) {
        PlayerPresentationMode.CD -> Color(0xFF111111)
        PlayerPresentationMode.IMMERSIVE_ARTWORK -> Color.Black
        else -> MaterialTheme.colorScheme.background
    }
    Scaffold(
        // The CD player uses a permanently dark surface even on phones in
        // light mode. Match it behind the swipeable library to avoid flashes.
        containerColor = pageBackdrop,
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0)
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            MiuixHorizontalPager(
                state = pagerState,
                backgroundColor = pageBackdrop,
                userScrollEnabled = !artistPageEnabled || (lyricsPagerState.currentPage == 0 && !lyricsPagerState.isScrollInProgress),
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when {
                    artistPageEnabled && page == 0 -> ImmersiveArtistPage(
                        artistName = currentSong?.artist,
                        songs = songs,
                        currentSong = currentSong,
                        biography = artistBiography,
                        biographyLoading = biographyLoading,
                        isVisible = pagerState.settledPage == 0,
                        state = artistPageState,
                        onVisible = { artistPageState.loadPortrait(context, scope) },
                        onBack = { scope.launch { pagerState.animateMiuixScrollToPage(playerPage) } },
                        onPlay = onPlay,
                        artwork = { song, modifier ->
                            ArtworkView(song = song, maxSizePx = 512, modifier = modifier, grainOverlay = false)
                        }
                    )
                    page == playerPage -> {
                        MiuixPlayerLyricsPager(
                            enabled = artistPageEnabled,
                            state = lyricsPagerState,
                            backgroundColor = pageBackdrop
                        ) { verticalPage ->
                            if (verticalPage == 1) {
                            ImmersiveLyricsPage(
                                song = currentSong,
                                positionMs = positionMsState,
                                durationMs = durationMs,
                                isVisible = pagerState.settledPage == playerPage && lyricsPagerState.settledPage == 1,
                                state = lyricsState,
                                scope = scope,
                                onBack = { scope.launch { lyricsPagerState.animateMiuixScrollToPage(0) } },
                                onLibrary = { scope.launch {
                                    pagerState.animateMiuixScrollToPage(libraryPage)
                                    lyricsPagerState.scrollToPage(0)
                                } },
                                onSeek = onSeek,
                                controls = playerControls
                            )
                            } else {
                        BoxWithConstraints(Modifier.fillMaxSize()) {
                        val playerViewportHeight = maxHeight
                        if (playerPresentationMode == PlayerPresentationMode.CD) {
                            // The CD page scrolls only its own content, without
                            // the shared biography container or its black backdrop.
                            CdPlayer(
                                song = currentSong,
                                songs = filtered,
                                isPlaying = isPlaying,
                                isVisible = pagerState.settledPage == playerPage,
                                positionMsState = positionMsState,
                                durationMs = durationMs,
                                viewportHeight = playerViewportHeight,
                                titleFontSize = titleFontSize,
                                titleFontFamily = titleFontFamily,
                                roundedCarouselCorners = roundedCarouselCorners,
                                shuffleEnabled = shuffleEnabled,
                                repeatMode = repeatMode,
                                onPlayPause = {
                                    when {
                                        currentSong != null -> onTogglePlay()
                                        filtered.isNotEmpty() -> onPlay(filtered.first(), filtered)
                                        !permissionGranted && selectedMusicFolders.isEmpty() -> onRequestPermission()
                                        else -> scope.launch { pagerState.animateMiuixScrollToPage(libraryPage) }
                                    }
                                },
                                onPrevious = onPrevious,
                                onNext = onNext,
                                onSeek = onSeek,
                                onShuffle = onToggleShuffle,
                                onRepeat = onToggleRepeat,
                                onSelectSong = { onPlay(it, filtered) },
                                onSettings = { showSettings = true },
                                onLibrary = { scope.launch { pagerState.animateMiuixScrollToPage(libraryPage) } },
                                artwork = { cdSong, artworkModifier ->
                                    androidx.compose.runtime.key(cdSong?.uri) {
                                        ArtworkView(
                                            song = cdSong,
                                            maxSizePx = 512,
                                            fallbackMaxSizePx = 256,
                                            loadDelayMs = 40L,
                                            modifier = artworkModifier
                                        )
                                    }
                                },
                                thumbnailArtwork = { cdSong, artworkModifier ->
                                    androidx.compose.runtime.key(cdSong?.uri) {
                                        ArtworkView(song = cdSong, maxSizePx = 256, fallbackMaxSizePx = 128, loadDelayMs = 65L, modifier = artworkModifier)
                                    }
                                },
                                emptyState = {
                                    Text(
                                        text = emptyLibraryMessage,
                                        color = Color(0xFFA5A5A0),
                                        fontSize = 12.sp,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(24.dp)
                                    )
                                },
                                modifier = Modifier.fillMaxSize()
                                    .verticalScroll(
                                        state = androidx.compose.foundation.rememberScrollState(),
                                        overscrollEffect = null
                                    )
                            )
                            return@BoxWithConstraints
                        }
                        LazyColumn(
                            state = playerScrollState,
                            userScrollEnabled = !artistPageEnabled,
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    if (playerPresentationMode != PlayerPresentationMode.VINYL ||
                                        revealPlayerCurve
                                    ) {
                                        Color.Black
                                    } else {
                                        MaterialTheme.colorScheme.background
                                    }
                                ),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .then(
                                            if (playerPresentationMode != PlayerPresentationMode.IMMERSIVE_ARTWORK) {
                                                Modifier.heightIn(min = playerViewportHeight)
                                            } else Modifier.fillParentMaxHeight()
                                        )
                                        .then(
                                            if (playerPresentationMode == PlayerPresentationMode.IMMERSIVE_ARTWORK) {
                                                Modifier.onGloballyPositioned { coordinates ->
                                                    immersiveFrameCoordinates.value = coordinates
                                                    updateImmersiveArtworkBottom()
                                                }
                                            } else Modifier
                                        )
                                        .then(
                                            if (playerPresentationMode == PlayerPresentationMode.IMMERSIVE_ARTWORK) {
                                                // Keep the immersive player edge square and fully black so
                                                // rounded-corner antialiasing cannot reveal a light-colored seam
                                                // between the player and the biography section.
                                                Modifier.background(Color.Black)
                                            } else {
                                                Modifier
                                                    .clip(
                                                        RoundedCornerShape(
                                                            bottomStart = 34.dp,
                                                            bottomEnd = 34.dp
                                                        )
                                                    )
                                                    .background(MaterialTheme.colorScheme.background)
                                            }
                                        )
                                ) {
                                    if (playerPresentationMode == PlayerPresentationMode.IMMERSIVE_ARTWORK) {
                                        ImmersiveArtwork(
                                            song = currentSong,
                                            animatedArtworkEnabled = animatedArtworkEnabled && pagerState.settledPage == playerPage && lyricsPagerState.settledPage == 0,
                                            artistBottomPx = immersiveArtworkBottomPx,
                                            modifier = Modifier.matchParentSize()
                                        )
                                    }

                                    if (playerPresentationMode == PlayerPresentationMode.VINYL) {
                                        TurntablePlayer(
                                            title = currentSong?.title ?: nusaText("Pilih lagu", "Choose a song"),
                                            artist = currentSong?.artist ?: nusaText("Perpustakaan musik lokal Anda", "Your local music library"),
                                            titleFontFamily = titleFontFamily,
                                            titleFontSize = titleFontSize,
                                            viewportHeight = playerViewportHeight,
                                            isPlaying = isPlaying,
                                            isVisible = pagerState.settledPage == playerPage,
                                            hasSong = currentSong != null,
                                            positionMsState = positionMsState,
                                            durationMs = durationMs,
                                            shuffleEnabled = shuffleEnabled,
                                            repeatMode = repeatMode,
                                            realisticControls = realisticControls,
                                            labels = TurntableLabels(
                                                play = nusaText("Putar", "Play"),
                                                pause = nusaText("Jeda", "Pause"),
                                                previous = nusaText("Sebelumnya", "Previous"),
                                                next = nusaText("Berikutnya", "Next"),
                                                shuffle = nusaText("Acak", "Shuffle"),
                                                shuffleOn = nusaText("Acak aktif", "Shuffle on"),
                                                shuffleOff = nusaText("Acak nonaktif", "Shuffle off"),
                                                repeat = nusaText("Ulangi", "Repeat"),
                                                repeatOff = nusaText("Ulangi nonaktif", "Repeat off"),
                                                repeatAll = nusaText("Ulangi semua", "Repeat all"),
                                                repeatOne = nusaText("Ulangi satu lagu", "Repeat one"),
                                                seek = nusaText("Posisi pemutaran", "Playback position")
                                            ),
                                            onPlayPause = {
                                                when {
                                                    currentSong != null -> onTogglePlay()
                                                    filtered.isNotEmpty() -> onPlay(filtered.first(), filtered)
                                                    !permissionGranted && selectedMusicFolders.isEmpty() -> onRequestPermission()
                                                    else -> scope.launch { pagerState.animateMiuixScrollToPage(libraryPage) }
                                                }
                                            },
                                            onPrevious = onPrevious,
                                            onNext = onNext,
                                            onSeek = onSeek,
                                            onShuffle = onToggleShuffle,
                                            onRepeat = onToggleRepeat,
                                            record = { recordModifier ->
                                                VinylRecord(
                                                    song = currentSong,
                                                    isPlaying = isPlaying && pagerState.settledPage == playerPage,
                                                    modifier = recordModifier
                                                )
                                            },
                                            quality = { AudioQualityPill(song = currentSong) },
                                            footer = {
                                                Row(
                                                    Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    // Settings takes the former arrow position at the left.
                                                    IconButton(onClick = { showSettings = true }) {
                                                        Icon(
                                                            Icons.Default.MoreHoriz,
                                                            contentDescription = nusaText("Pengaturan", "Settings"),
                                                            tint = MaterialTheme.colorScheme.onSurface
                                                        )
                                                    }
                                                    Spacer(Modifier.weight(1f))
                                                    IconButton(onClick = { onShare(currentSong) }, enabled = currentSong != null) {
                                                        ChatGptStyleShareIcon(Modifier.size(21.dp), color = MaterialTheme.colorScheme.onSurface)
                                                    }
                                                    IconButton(onClick = { isFavorite = !isFavorite }, enabled = currentSong != null) {
                                                        NusaFavoriteIcon(selected = isFavorite, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(21.dp))
                                                    }
                                                }
                                            },
                                            emptyState = {
                                                if (filtered.isEmpty()) {
                                                    Text(
                                                        text = emptyLibraryMessage,
                                                        fontSize = 12.sp,
                                                        textAlign = TextAlign.Center,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                                                    )
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    } else {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .then(
                                                if (playerPresentationMode == PlayerPresentationMode.VINYL) {
                                                    Modifier.background(MaterialTheme.colorScheme.background)
                                                } else {
                                                    Modifier
                                                }
                                            ),
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
                                            containerColor = if (
                                                playerPresentationMode == PlayerPresentationMode.IMMERSIVE_ARTWORK
                                            ) Color.Transparent else MaterialTheme.colorScheme.background,
                                            scrolledContainerColor = if (
                                                playerPresentationMode == PlayerPresentationMode.IMMERSIVE_ARTWORK
                                            ) Color.Transparent else MaterialTheme.colorScheme.background,
                                            actionIconContentColor = if (
                                                playerPresentationMode == PlayerPresentationMode.IMMERSIVE_ARTWORK
                                            ) Color.White else MaterialTheme.colorScheme.onSurface,
                                            navigationIconContentColor = if (
                                                playerPresentationMode == PlayerPresentationMode.IMMERSIVE_ARTWORK
                                            ) Color.White else MaterialTheme.colorScheme.onSurface
                                        ),
                                        windowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
                                        navigationIcon = {
                                            // Keep the top-left corner clear in Immersive mode.
                                            // Other presentation modes retain the artist avatar.
                                            if (playerPresentationMode != PlayerPresentationMode.IMMERSIVE_ARTWORK) {
                                                ArtistAvatar(
                                                    song = currentSong,
                                                    modifier = Modifier
                                                        .padding(start = 18.dp)
                                                        .size(40.dp)
                                                        .clip(CircleShape)
                                                )
                                            }
                                        },
                                        actions = {
                                            IconButton(onClick = { showSettings = true }) {
                                                Icon(
                                                    Icons.Default.MoreHoriz,
                                                    contentDescription = nusaText("Pengaturan", "Settings"),
                                                    tint = if (
                                                        playerPresentationMode == PlayerPresentationMode.IMMERSIVE_ARTWORK
                                                    ) Color.White else MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                    )

                                    BoxWithConstraints(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .then(
                                                if (playerPresentationMode == PlayerPresentationMode.IMMERSIVE_ARTWORK) {
                                                    Modifier.weight(1f)
                                                } else {
                                                    Modifier
                                                }
                                            )
                                            .padding(horizontal = 22.dp)
                                    ) {
                                        val playerContentHeight = maxHeight
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .then(
                                                    if (playerPresentationMode == PlayerPresentationMode.IMMERSIVE_ARTWORK) {
                                                        Modifier.fillMaxHeight()
                                                    } else Modifier
                                                ),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                        val immersiveArtworkMode =
                                            playerPresentationMode == PlayerPresentationMode.IMMERSIVE_ARTWORK
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
                                        val threeLineTitleHeight = with(
                                            androidx.compose.ui.platform.LocalDensity.current
                                        ) {
                                            ((titleFontSize * 37f / 34f).sp).toDp() * 3f + 6.dp
                                        }
                                        val titleBoxHeight = if (immersiveArtworkMode) {
                                            maxOf(104.dp, threeLineTitleHeight)
                                        } else {
                                            82.dp
                                        }

                                        if (playerPresentationMode == PlayerPresentationMode.VINYL) {
                                            Spacer(Modifier.height(2.dp))
                                        } else {
                                            // Reserve the lower controls first; this space only fills
                                            // what is left and cannot push them down for a long title.
                                            val heroSpace = (playerContentHeight - titleBoxHeight - 322.dp)
                                                .coerceIn(0.dp, playerContentHeight * 0.33f)
                                            val metadataOffset = if (immersiveArtworkMode) {
                                                // Move metadata into the unused pill area for lossy tracks.
                                                24.dp + if (immersiveHasQualityBadge) 0.dp else 32.dp
                                            } else 0.dp
                                            Spacer(
                                                Modifier.height(heroSpace + metadataOffset)
                                            )
                                        }

                                        if (playerPresentationMode == PlayerPresentationMode.VINYL) {
                                            VinylRecord(
                                                song = currentSong,
                                                isPlaying = isPlaying,
                                                modifier = Modifier
                                                    .fillMaxWidth(0.84f)
                                                    .aspectRatio(1f)
                                            )
                                        }

                                        if (playerPresentationMode == PlayerPresentationMode.VINYL) {
                                            Spacer(Modifier.height(24.dp))
                                        }

                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(titleBoxHeight),
                                            contentAlignment = Alignment.BottomCenter
                                        ) {
                                            Text(
                                                titleText,
                                                fontFamily = titleFontFamily,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = titleSize,
                                                lineHeight = titleLineHeight,
                                                maxLines = 3,
                                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                                modifier = Modifier.padding(horizontal = 4.dp),
                                                color = if (
                                                    playerPresentationMode == PlayerPresentationMode.IMMERSIVE_ARTWORK
                                                ) Color.White else MaterialTheme.colorScheme.onBackground,
                                                textAlign = TextAlign.Center
                                            )
                                        }

                                        Text(
                                            currentSong?.artist ?: nusaText("Perpustakaan musik lokal Anda", "Your local music library"),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .then(
                                                    if (immersiveArtworkMode) {
                                                        Modifier.onGloballyPositioned { coordinates ->
                                                            immersiveArtistCoordinates.value = coordinates
                                                            updateImmersiveArtworkBottom()
                                                        }
                                                    } else Modifier
                                                ),
                                            fontSize = 14.sp,
                                            maxLines = 1,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                            textAlign = TextAlign.Center,
                                            color = if (
                                                playerPresentationMode == PlayerPresentationMode.IMMERSIVE_ARTWORK
                                            ) {
                                                Color.White.copy(alpha = 0.82f)
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            }
                                        )

                                        if (immersiveArtworkMode) {
                                            if (immersiveHasQualityBadge) Spacer(Modifier.height(10.dp))
                                            AudioQualityPill(
                                                song = currentSong,
                                                immersive = true,
                                                onBadgeVisibilityChanged = { immersiveHasQualityBadge = it }
                                            )
                                            Spacer(Modifier.weight(1f))
                                        } else {
                                            Spacer(Modifier.height(16.dp))
                                        }

                                        playerControls()

                                        if (!hasMusicAccess) {
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
                                        } else if (playerPresentationMode != PlayerPresentationMode.IMMERSIVE_ARTWORK && filtered.isEmpty()) {
                                            Text(
                                                emptyLibraryMessage,
                                                color = Color(0xFF9D9D9D),
                                                fontSize = 14.sp
                                            )
                                        }
                                        }
                                    }
                                }
                                }
                            }
                            }

                            if (showArtistBiography && !artistPageEnabled) {
                                item {
                                    ArtistBiographySection(
                                        artistName = currentSong?.artist,
                                        biography = artistBiography,
                                        loading = biographyLoading
                                    )
                                }
                            }
                        }
                        }
                    }

                        }
                    }

                    page == libraryPage -> {
                        val vinylLibrary = playerPresentationMode == PlayerPresentationMode.VINYL
                        val cdLibrary = playerPresentationMode == PlayerPresentationMode.CD
                        // Match the CD player's charcoal background and lime
                        // accents. This theme is scoped ONLY to the song list,
                        // so Vinyl and other presentation modes stay untouched.
                        val libraryScheme = if (cdLibrary) {
                            darkColorScheme(
                                background = Color(0xFF111111),
                                surface = Color(0xFF191919),
                                surfaceVariant = Color(0xFF252525),
                                primary = Color(0xFFB5DF51),
                                onPrimary = Color(0xFF111111),
                                onBackground = Color(0xFFF2F0E9),
                                onSurface = Color(0xFFF2F0E9),
                                onSurfaceVariant = Color(0xFFA5A5A0)
                            )
                        } else {
                            MaterialTheme.colorScheme
                        }
                        MaterialTheme(colorScheme = libraryScheme) {
                        val libraryBackground = MaterialTheme.colorScheme.background
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(libraryBackground)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(76.dp)
                                    .background(libraryBackground)
                                    .padding(horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = {
                                        scope.launch {
                                            pagerState.animateMiuixScrollToPage(playerPage)
                                        }
                                    }
                                ) {
                                    Icon(
                                        Icons.Default.ArrowBack,
                                        contentDescription = nusaText("Kembali ke pemutar", "Back to player"),
                                        tint = MaterialTheme.colorScheme.onBackground
                                    )
                                }

                                Text(
                                    text = nusaText("Daftar Lagu", "Songs"),
                                    modifier = Modifier.weight(1f),
                                    textAlign = if (vinylLibrary) TextAlign.Start else TextAlign.Center,
                                    fontSize = 21.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )

                                Box {
                                    IconButton(
                                        onClick = { sortMenuExpanded = true }
                                    ) {
                                            Icon(
                                                Icons.Default.Sort,
                                                contentDescription = nusaText("Urutkan lagu", "Sort songs"),
                                                tint = MaterialTheme.colorScheme.onBackground
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
                                                    uiPrefs.edit()
                                                        .putString(
                                                            "library_sort_option",
                                                            option.name
                                                        )
                                                        .apply()
                                                    sortMenuExpanded = false
                                                    // Sorting must never trigger a full media scan.
                                                    // Missing dates sort last; users can refresh explicitly.
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            PullToRefreshBox(
                                isRefreshing = isScanningMusic,
                                onRefresh = onScanMusic,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                if (cdLibrary &&
                                    (permissionGranted || selectedMusicFolders.isNotEmpty()) &&
                                    filtered.isNotEmpty()
                                ) {
                                    CdSongGrid(
                                        songs = filtered,
                                        currentSongId = currentSong?.id,
                                        onPlay = { onPlay(it, filtered) },
                                        artwork = { song, artworkModifier ->
                                            ArtworkView(song = song, maxSizePx = 512, modifier = artworkModifier)
                                        },
                                        modifier = Modifier.fillMaxSize().padding(top = 76.dp)
                                    )
                                } else if (vinylLibrary &&
                                    (permissionGranted || selectedMusicFolders.isNotEmpty()) &&
                                    filtered.isNotEmpty()
                                ) {
                                    VinylSongList(
                                        songs = filtered,
                                        state = libraryListState,
                                        currentSongId = currentSong?.id,
                                        isPlaying = isPlaying,
                                        backgroundColor = libraryBackground,
                                        playLabel = nusaText("Putar", "Play"),
                                        pauseLabel = nusaText("Jeda", "Pause"),
                                        unknownArtistLabel = nusaText("Artis tidak dikenal", "Unknown artist"),
                                        onPlay = { onPlay(it, filtered) },
                                        onTogglePlay = onTogglePlay,
                                        artwork = { song, artworkModifier ->
                                            ArtworkView(song = song, maxSizePx = 256, modifier = artworkModifier)
                                        },
                                        modifier = Modifier.fillMaxSize().padding(top = 76.dp, bottom = 84.dp)
                                    )
                                } else {
                                LazyColumn(
                                    state = libraryListState,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(top = 76.dp),
                                    contentPadding = PaddingValues(
                                        top = 8.dp,
                                        start = 12.dp,
                                        end = 12.dp,
                                        bottom = 140.dp
                                    ),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                if (
                                    (permissionGranted || selectedMusicFolders.isNotEmpty()) &&
                                    filtered.isNotEmpty()
                                ) {
                                    items(
                                        items = filtered,
                                        key = { song -> song.id },
                                        contentType = { "library-song" }
                                    ) { song ->
                                        LibrarySongListRow(
                                            song = song,
                                            selected = currentSong?.id == song.id,
                                            loadDetails = libraryRowLoadsEnabled,
                                            onPlay = { selectedSong ->
                                                onPlay(selectedSong, filtered)
                                            }
                                        )
                                    }
                                } else if (!permissionGranted && selectedMusicFolders.isEmpty()) {
                                    item {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 80.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                nusaText(
                                                    "Izinkan Nusa mengakses file audio Anda.",
                                                    "Give Nusa access to your audio files."
                                                ),
                                                color = Color(0xFF9D9D9D),
                                                fontSize = 14.sp
                                            )
                                            Spacer(Modifier.height(12.dp))
                                            FilledIconButton(onClick = onRequestPermission) {
                                                Icon(
                                                    Icons.Default.FolderOpen,
                                                    contentDescription = nusaText(
                                                        "Izinkan akses musik",
                                                        "Allow music access"
                                                    )
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
                                                emptyLibraryMessage,
                                                color = Color(0xFF9D9D9D),
                                                fontSize = 14.sp
                                            )
                                        }
                                    }
                                }
                                }
                                }

                            }

                            fun requestAlphabetScroll(targetIndex: Int) {
                                if (targetIndex < 0) return

                                scope.launch {
                                    libraryListState.animateScrollToItem(
                                        index = targetIndex,
                                        scrollOffset = 0
                                    )
                                }
                            }

                            var alphabetDragJob: Job? = null

                            fun followAlphabetDrag(targetIndex: Int) {
                                if (targetIndex < 0) return

                                // Keep only the latest drag request so rapid
                                // alphabet movement does not build a coroutine backlog.
                                alphabetDragJob?.cancel()
                                alphabetDragJob = scope.launch {
                                    libraryListState.scrollToItem(
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
                                            alphabetDragging = true
                                            revealAlphabet()

                                            val slotHeight =
                                                size.height / alphabet.size.toFloat()
                                            val slot = (offset.y / slotHeight)
                                                .toInt()
                                                .coerceIn(0, alphabet.lastIndex)
                                            val targetIndex =
                                                alphabetTargets[alphabet[slot]] ?: -1

                                            if (targetIndex >= 0) {
                                                lastDragTarget = targetIndex
                                                followAlphabetDrag(targetIndex)
                                            }
                                        },
                                        onDrag = { change, _ ->
                                            change.consume()
                                            revealAlphabet()

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
                                                followAlphabetDrag(targetIndex)
                                            }
                                        },
                                        onDragEnd = {
                                            alphabetDragging = false
                                            lastDragTarget = -1
                                            scheduleAlphabetHide()
                                        },
                                        onDragCancel = {
                                            alphabetDragging = false
                                            lastDragTarget = -1
                                            scheduleAlphabetHide()
                                        }
                                    )
                                }

                            val alphabetAlpha by animateFloatAsState(
                                targetValue = if (alphabetVisible) 1f else 0f,
                                animationSpec = tween(220),
                                label = "alphabetIndexAlpha"
                            )

                            Box(
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Column(
                                    modifier = alphabetIndexModifier
                                        .align(Alignment.CenterEnd)
                                        .graphicsLayer {
                                            alpha = alphabetAlpha
                                        },
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
                                                revealAlphabet()
                                                requestAlphabetScroll(targetIndex)
                                                scheduleAlphabetHide()
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
                                                MaterialTheme.colorScheme.onBackground.copy(alpha = 0.78f)
                                            } else {
                                                MaterialTheme.colorScheme.onBackground.copy(alpha = 0.22f)
                                            }
                                        )
                                    }
                                    }
                                }
                            }

                            val showFloatingControls by remember(vinylLibrary, filtered.isNotEmpty()) {
                                derivedStateOf {
                                    if (vinylLibrary) filtered.isNotEmpty()
                                    else libraryListState.firstVisibleItemIndex >= 4
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
                                    color = if (vinylLibrary) MaterialTheme.colorScheme.surface
                                        else MaterialTheme.colorScheme.surfaceVariant,
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

                        }
                        } // Library-only theme
                    }
                }
            }
        }
    }
}

@Composable
private fun PrototypeAlbumRecordCard(
    song: Song,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
    ) {
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val radius = size.minDimension * 0.40f
            val center = androidx.compose.ui.geometry.Offset(
                x = size.width * 0.68f,
                y = size.height * 0.50f
            )

            // Vinyl shadow.
            drawCircle(
                color = Color.Black.copy(alpha = 0.22f),
                radius = radius * 1.035f,
                center = center.copy(
                    y = center.y + size.minDimension * 0.012f
                )
            )

            // Realistic dark disc base.
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF303030),
                        Color(0xFF111111),
                        Color(0xFF050505)
                    ),
                    center = androidx.compose.ui.geometry.Offset(
                        center.x - radius * 0.20f,
                        center.y - radius * 0.28f
                    ),
                    radius = radius * 1.15f
                ),
                radius = radius,
                center = center
            )

            // Fine grooves.
            for (ring in 1..26) {
                val ringRadius = radius * (
                    0.28f + ring * 0.025f
                )
                if (ringRadius < radius * 0.97f) {
                    drawCircle(
                        color = if (ring % 2 == 0) {
                            Color.White.copy(alpha = 0.028f)
                        } else {
                            Color.Black.copy(alpha = 0.17f)
                        },
                        radius = ringRadius,
                        center = center,
                        style =
                            androidx.compose.ui.graphics.drawscope.Stroke(
                                width = 0.65f
                            )
                    )
                }
            }

            // Lacquer highlight.
            drawArc(
                color = Color.White.copy(alpha = 0.13f),
                startAngle = 205f,
                sweepAngle = 72f,
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(
                    center.x - radius,
                    center.y - radius
                ),
                size = androidx.compose.ui.geometry.Size(
                    radius * 2f,
                    radius * 2f
                ),
                style =
                    androidx.compose.ui.graphics.drawscope.Stroke(
                        width = radius * 0.045f
                    )
            )

            // Center label.
            drawCircle(
                color = Color(0xFF363636),
                radius = radius * 0.18f,
                center = center
            )
            drawCircle(
                color = Color(0xFFB42E2A),
                radius = radius * 0.115f,
                center = center
            )
            drawCircle(
                color = Color.Black,
                radius = radius * 0.035f,
                center = center
            )
        }

        // Square album artwork in front of the record.
        Box(
            modifier = Modifier
                .fillMaxHeight(0.82f)
                .aspectRatio(1f)
                .align(Alignment.CenterStart)
                .clip(RoundedCornerShape(8.dp))
                .graphicsLayer {
                    shadowElevation = 14.dp.toPx()
                    shape = RoundedCornerShape(8.dp)
                    clip = false
                }
        ) {
            ArtworkView(
                song = song,
                maxSizePx = 700,
                modifier = Modifier.fillMaxSize()
            )
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
                        nusaText(
                            "Atur LASTFM_API_KEY di local.properties atau environment",
                            "Set LASTFM_API_KEY in local.properties or the environment"
                        )
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
                text = nusaText(
                    "Sumber: Last.fm (" + biography.sourceLanguage.uppercase() + ")",
                    "Source: Last.fm (" + biography.sourceLanguage.uppercase() + ")"
                ),
                color = Color(0xFF777777),
                fontSize = 11.sp,
                modifier = Modifier.padding(start = 4.dp, top = 8.dp)
            )
        }
    }
}

private enum class AudioQualityBadge {
    LOSSLESS,
    HI_RES,
    DOLBY
}

@Composable
private fun AudioQualityPill(
    song: Song?,
    immersive: Boolean = false,
    onBadgeVisibilityChanged: ((Boolean) -> Unit)? = null
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var codecInfo by remember { mutableStateOf<AudioCodecInfo?>(null) }
    var resolvedCodecUri by remember { mutableStateOf<String?>(null) }
    var showMetadataDialog by remember(song?.uri) { mutableStateOf(false) }

    LaunchedEffect(song?.uri) {
        val targetUri = song?.uri
        if (targetUri == null) {
            codecInfo = null
            resolvedCodecUri = null
            return@LaunchedEffect
        }

        // Keep the previous badge visible while the next file is inspected. Replacing
        // it only after loading prevents a blank frame when tracks change.
        val loaded = AudioCodecLoader.load(
            context = context,
            uriString = targetUri
        )
        codecInfo = loaded
        resolvedCodecUri = targetUri
    }

    val info = codecInfo
    val isLossless = info?.codecName == "Apple Lossless" || info?.codecName == "FLAC"
    val isHiRes = isLossless && info?.isHiRes == true
    val isDolby = info?.isDolby == true
    val badge = when {
        isDolby -> AudioQualityBadge.DOLBY
        isHiRes -> AudioQualityBadge.HI_RES
        isLossless -> AudioQualityBadge.LOSSLESS
        else -> null
    }
    val metadataMatchesSong = song != null && resolvedCodecUri == song.uri
    val badgeVisibilityCallback by rememberUpdatedState(onBadgeVisibilityChanged)
    LaunchedEffect(song?.uri, resolvedCodecUri, badge) {
        // Retain the previous layout until metadata for the new track is resolved.
        if (song == null || resolvedCodecUri == song.uri) {
            badgeVisibilityCallback?.invoke(badge != null)
        }
    }

    Crossfade(
        targetState = badge,
        animationSpec = tween(durationMillis = 240),
        label = "audioQualityBadge"
    ) { visibleBadge ->
        when (visibleBadge) {
            AudioQualityBadge.LOSSLESS,
            AudioQualityBadge.HI_RES -> {
                val hiRes = visibleBadge == AudioQualityBadge.HI_RES
                Box(
                    modifier = Modifier
                        .padding(top = if (immersive) 0.dp else 4.dp)
                        .clip(RoundedCornerShape(50))
                        .background(
                            color = if (immersive) {
                                Color.White
                            } else if (hiRes) {
                                Color(0xFFB5A77C).copy(alpha = 0.42f)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            }
                        )
                        .clickable(enabled = metadataMatchesSong && info != null) {
                            showMetadataDialog = true
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
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
                                width = 16.dp,
                                height = 9.dp
                            )
                        )

                        Spacer(Modifier.width(4.dp))

                        Text(
                            text = if (hiRes) "Hi-Res" else "Lossless",
                            fontSize = 10.sp,
                            lineHeight = 11.sp,
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
            AudioQualityBadge.DOLBY -> {
                Box(
                    modifier = Modifier
                        .padding(top = if (immersive) 0.dp else 4.dp)
                        .clip(RoundedCornerShape(50))
                        .background(
                            if (immersive) Color.White else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .clickable(enabled = metadataMatchesSong && info != null) {
                            showMetadataDialog = true
                        }
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = androidx.compose.ui.text.buildAnnotatedString {
                            withStyle(
                                androidx.compose.ui.text.SpanStyle(
                                    fontWeight = FontWeight.Bold
                                )
                            ) {
                                append("Dolby")
                            }
                            append(" ")
                            withStyle(
                                androidx.compose.ui.text.SpanStyle(
                                    fontWeight = FontWeight.Light
                                )
                            ) {
                                append("Atmos")
                            }
                        },
                        fontSize = 10.sp,
                        lineHeight = 11.sp,
                        letterSpacing = (-0.35).sp,
                        maxLines = 1,
                        color = if (immersive) {
                            Color(0xFF1B1B1B)
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                }
            }
            null -> Spacer(Modifier.size(0.dp))
        }
    }

    if (badge != null && !immersive) {
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
    immersive: Boolean = false,
    modifier: Modifier = Modifier
) {
    val fraction = if (durationMs > 0L) {
        (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    val primary = if (immersive) Color.White else MaterialTheme.colorScheme.primary

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
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            val fraction =
                                (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                            onSeek((fraction * durationMs).toLong())
                        },
                        onDragEnd = {},
                        onDragCancel = {},
                        onHorizontalDrag = { change, _ ->
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
        val stroke = 5.dp.toPx()
        val startX = stroke / 2f
        val endX = (size.width - startX).coerceAtLeast(startX)

        drawLine(
            color = if (immersive) {
                Color.White.copy(alpha = 0.48f)
            } else {
                Color(0xFFD0CDC6)
            },
            start = androidx.compose.ui.geometry.Offset(startX, y),
            end = androidx.compose.ui.geometry.Offset(endX, y),
            strokeWidth = stroke,
            cap = androidx.compose.ui.graphics.StrokeCap.Round
        )

        if (fraction > 0f) {
            drawLine(
                color = primary,
                start = androidx.compose.ui.geometry.Offset(startX, y),
                end = androidx.compose.ui.geometry.Offset(startX + (endX - startX) * fraction, y),
                strokeWidth = stroke,
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )
        }
    }
}

@Composable
private fun PlaybackProgress(
    positionMsState: State<Long>,
    durationMs: Long,
    enabled: Boolean,
    onSeek: (Long) -> Unit,
    immersive: Boolean = false,
    modifier: Modifier = Modifier
) {
    val positionMs by positionMsState

    Column(modifier = modifier) {
        SimpleProgressBar(
            positionMs = positionMs,
            durationMs = durationMs,
            enabled = enabled,
            onSeek = onSeek,
            immersive = immersive,
            modifier = Modifier.fillMaxWidth()
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val textColor = if (immersive) Color.White else MaterialTheme.colorScheme.onBackground
            Text(formatTime(positionMs), fontSize = 12.sp, color = textColor)
            Text(formatTime(durationMs), fontSize = 12.sp, color = textColor)
        }
    }
}

@Composable
private fun TransportPillButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    enabled: Boolean,
    realistic: Boolean = true
) {
    if (realistic) {
        RealisticControlButton(
            icon = icon,
            contentDescription = contentDescription,
            onClick = onClick,
            enabled = enabled,
            circular = false,
            modifier = Modifier.size(width = 96.dp, height = 58.dp),
            iconSize = 32.dp
        )
    } else {
        Surface(
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
}

@Composable
private fun PlayerControlButton(
    realistic: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier,
    iconSize: androidx.compose.ui.unit.Dp
) {
    if (realistic) {
        RealisticControlButton(
            icon = icon,
            contentDescription = contentDescription,
            onClick = onClick,
            enabled = enabled,
            circular = true,
            modifier = modifier,
            iconSize = iconSize
        )
    } else {
        FilledIconButton(
            onClick = onClick,
            enabled = enabled,
            modifier = modifier,
            shape = CircleShape
        ) {
            Icon(
                icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(iconSize)
            )
        }
    }
}

@Composable
private fun RealisticControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    enabled: Boolean,
    circular: Boolean,
    modifier: Modifier = Modifier,
    iconSize: androidx.compose.ui.unit.Dp = 32.dp
) {
    var isPressed by remember { mutableStateOf(false) }
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = if (isPressed) {
            androidx.compose.animation.core.tween(
                durationMillis = 85,
                easing = androidx.compose.animation.core.FastOutSlowInEasing
            )
        } else {
            androidx.compose.animation.core.tween(
                durationMillis = 150,
                easing = androidx.compose.animation.core.FastOutSlowInEasing
            )
        },
        label = "controlPressScale"
    )

    // Keep the original NusaMusic button palette (surfaceVariant), but give it
    // physical depth, soft reflections and subtle texture instead of changing
    // the UI into a collection of black controls.
    val dark = isSystemInDarkTheme()
    val palette = if (dark) {
        listOf(
            Color(0xFF2A2A2A),
            Color(0xFF1C1C1C),
            Color(0xFF242424),
            Color(0xFF303030)
        )
    } else {
        listOf(
            Color(0xFFF0EEE8),
            Color(0xFFE2E0DA),
            Color(0xFFE9E7E1),
            Color(0xFFD8D6D0)
        )
    }

    val shape = if (circular) CircleShape else RoundedCornerShape(50)
    val iconColor = if (enabled) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.42f)
    }

    Surface(
        modifier = modifier
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
                shadowElevation = if (isPressed) 2.5.dp.toPx() else 5.dp.toPx()
                this.shape = shape
                clip = false
            },
        shape = shape,
        color = Color.Transparent,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(enabled) {
                    if (enabled) {
                        detectTapGestures(
                            onPress = {
                                isPressed = true
                                val released = tryAwaitRelease()
                                isPressed = false
                                if (released) {
                                    onClick()
                                }
                            }
                        )
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val minDim = size.minDimension

                if (circular) {
                    val r = minDim / 2f
                    val center = androidx.compose.ui.geometry.Offset(w / 2f, h / 2f)

                    // Real physical drop shadow and lower lip.
                    drawCircle(
                        color = Color.Black.copy(alpha = if (enabled) 0.24f else 0.10f),
                        radius = r * 0.95f,
                        center = androidx.compose.ui.geometry.Offset(center.x, center.y + 3.0f)
                    )
                    drawCircle(
                        color = palette[1],
                        radius = r * 0.975f,
                        center = center
                    )

                    // Lively satin surface using the same neutral palette as
                    // the original Material surfaceVariant button.
                    drawCircle(
                        brush = Brush.linearGradient(
                            colors = palette,
                            start = androidx.compose.ui.geometry.Offset(
                                w * 0.18f,
                                h * 0.08f
                            ),
                            end = androidx.compose.ui.geometry.Offset(
                                w * 0.84f,
                                h * 0.94f
                            )
                        ),
                        radius = r * 0.91f,
                        center = center
                    )

                    // Recessed inner edge.
                    drawCircle(
                        color = Color.Black.copy(alpha = if (dark) 0.26f else 0.10f),
                        radius = r * 0.78f,
                        center = center,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5f)
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = if (dark) 0.13f else 0.40f),
                        radius = r * 0.80f,
                        center = center,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.2f)
                    )

                    // Clean concentric machining texture. Kept sparse so the
                    // surface reads as brushed material instead of visual noise.
                    for (i in 0 until 4) {
                        drawCircle(
                            color = Color.White.copy(alpha = if (dark) 0.020f else 0.055f),
                            radius = r * (0.52f + i * 0.075f),
                            center = center,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 0.45f)
                        )
                    }

                    // A small, deterministic set of fine micro specks.
                    for (i in 0 until 22) {
                        val dx = (((i * 37) % 101) / 100f - 0.5f) * r * 1.45f
                        val dy = (((i * 61) % 97) / 96f - 0.5f) * r * 1.45f
                        val px = center.x + dx
                        val py = center.y + dy
                        if (dx * dx + dy * dy < r * r * 0.58f) {
                            drawCircle(
                                color = if (i % 2 == 0) {
                                    Color.White.copy(alpha = if (dark) 0.025f else 0.055f)
                                } else {
                                    Color.Black.copy(alpha = if (dark) 0.025f else 0.018f)
                                },
                                radius = 0.55f,
                                center = androidx.compose.ui.geometry.Offset(px, py)
                            )
                        }
                    }

                    // Natural top-left reflected light.
                    drawArc(
                        color = Color.White.copy(alpha = if (dark) 0.20f else 0.42f),
                        startAngle = 205f,
                        sweepAngle = 62f,
                        useCenter = false,
                        topLeft = androidx.compose.ui.geometry.Offset(
                            center.x - r * 0.82f,
                            center.y - r * 0.82f
                        ),
                        size = androidx.compose.ui.geometry.Size(
                            r * 1.64f,
                            r * 1.64f
                        ),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.5f)
                    )
                } else {
                    val radius = h / 2f

                    // Lower shadow gives the pill physical separation from
                    // the background without changing its neutral color.
                    drawRoundRect(
                        color = Color.Black.copy(alpha = if (enabled) 0.22f else 0.08f),
                        topLeft = androidx.compose.ui.geometry.Offset(1.5f, 3.5f),
                        size = androidx.compose.ui.geometry.Size(w - 3f, h - 3f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius, radius)
                    )
                    drawRoundRect(
                        color = palette[1],
                        topLeft = androidx.compose.ui.geometry.Offset(1.5f, 1.5f),
                        size = androidx.compose.ui.geometry.Size(w - 3f, h - 4f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius, radius)
                    )
                    drawRoundRect(
                        brush = Brush.linearGradient(
                            colors = palette,
                            start = androidx.compose.ui.geometry.Offset(w * 0.08f, h * 0.06f),
                            end = androidx.compose.ui.geometry.Offset(w * 0.90f, h * 0.96f)
                        ),
                        topLeft = androidx.compose.ui.geometry.Offset(2.5f, 2.5f),
                        size = androidx.compose.ui.geometry.Size(w - 5f, h - 7f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius - 2f, radius - 2f)
                    )

                    // Highlight and inset border: realistic, but still clearly
                    // the original gray/cream NusaMusic button.
                    drawRoundRect(
                        color = Color.White.copy(alpha = if (dark) 0.13f else 0.42f),
                        topLeft = androidx.compose.ui.geometry.Offset(4.5f, 4f),
                        size = androidx.compose.ui.geometry.Size(w - 9f, h * 0.34f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius * 0.72f, radius * 0.72f),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.2f)
                    )
                    drawRoundRect(
                        color = Color.Black.copy(alpha = if (dark) 0.24f else 0.10f),
                        topLeft = androidx.compose.ui.geometry.Offset(5f, 5.5f),
                        size = androidx.compose.ui.geometry.Size(w - 10f, h - 11f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius * 0.78f, radius * 0.78f),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 0.9f)
                    )

                    // Evenly spaced, very fine brushed-metal grain.
                    for (i in 0 until 16) {
                        val yy = h * (0.22f + i / 30f)
                        drawLine(
                            color = if (i % 2 == 0) {
                                Color.White.copy(alpha = if (dark) 0.010f else 0.026f)
                            } else {
                                Color.Black.copy(alpha = if (dark) 0.012f else 0.010f)
                            },
                            start = androidx.compose.ui.geometry.Offset(10f, yy),
                            end = androidx.compose.ui.geometry.Offset(w - 10f, yy),
                            strokeWidth = 0.5f
                        )
                    }
                }

                if (!enabled) {
                    drawRect(color = Color.Black.copy(alpha = 0.10f))
                }
            }

            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(iconSize),
                tint = iconColor
            )
        }
    }
}


@Composable
private fun VinylRecord(
    song: Song?,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    // Keep the physical angle continuous while changing rotation speed.
    // Playback starts and stops with a gentle acceleration/deceleration instead
    // of an abrupt jump.
    val rotation = remember { mutableFloatStateOf(0f) }
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
                durationMillis = 950,
                easing = androidx.compose.animation.core.FastOutSlowInEasing
            )
        )
    }

    LaunchedEffect(isPlaying) {
        var lastFrameNanos = 0L

        while (isActive) {
            if (isPlaying || rotationSpeed.value > 0.01f) {
                val frameNanos = androidx.compose.runtime.withFrameNanos { it }

                if (lastFrameNanos != 0L) {
                    val deltaSeconds =
                        ((frameNanos - lastFrameNanos).coerceAtMost(100_000_000L)) /
                            1_000_000_000f

                    val nextRotation =
                        (rotation.floatValue + rotationSpeed.value * deltaSeconds) % 360f

                    rotation.floatValue = nextRotation
                }

                lastFrameNanos = frameNanos
            } else {
                // No playback and no residual rotation: avoid a continuous
                // 60 FPS loop while the record is idle.
                lastFrameNanos = 0L
                delay(120L)
            }
        }
    }

    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        val labelPixels = vinylLabelDiameter(minOf(constraints.maxWidth, constraints.maxHeight))
        val labelSize = with(LocalDensity.current) { labelPixels.toDp() }
        // The cast shadow belongs to the turntable lighting, not the spinning surface.
        Box(
            Modifier
                .fillMaxSize()
                .drawWithCache {
                    val painter = VinylShadowPainter(size, 16.dp.toPx(), 6.dp.toPx())
                    onDrawBehind { with(painter) { drawShadow() } }
                }
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { rotationZ = rotation.floatValue }
                .drawWithCache {
                    val painter = VinylPainter(size)
                    onDrawBehind { with(painter) { drawFace() } }
                },
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier
                    .matchParentSize()
                    .drawWithCache {
                        val painter = VinylPainter(size)
                        onDrawBehind { with(painter) { drawReflections() } }
                    }
            )

            // A 12-inch record's paper label is approximately a third of its diameter.
            Box(
                modifier = Modifier
                    .size(labelSize)
                    .clip(CircleShape)
                    .background(Color(0xFF202321))
            ) {
                ArtworkView(
                    song = song,
                    maxSizePx = 512,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    Modifier
                        .fillMaxSize()
                        .drawWithCache {
                            val painter = VinylLabelPainter(size)
                            onDrawBehind { with(painter) { drawFinish() } }
                        }
                )
            }
        }

        // The spindle stays centered while the record and its reflections rotate.
        Box(
            Modifier
                .fillMaxSize()
                .drawWithCache {
                    val painter = VinylPainter(size)
                    onDrawBehind {
                        with(painter) { drawSpindle() }
                    }
                }
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

private suspend fun prepareImmersiveArtwork(bitmap: Bitmap): Pair<Bitmap, Color> {
    return withContext(Dispatchers.Default) {
        val pixels = IntArray(64) { index ->
            val x = ((index % 8 + 0.5f) / 8f * bitmap.width).toInt()
                .coerceIn(0, bitmap.width - 1)
            val y = ((index / 8 + 0.5f) / 8f * bitmap.height).toInt()
                .coerceIn(0, bitmap.height - 1)
            bitmap.getPixel(x, y)
        }
        ArtworkBlur.create(bitmap) to artworkBackdropColor(pixels)
    }
}

private fun immersiveScrimOpacity(bitmap: Bitmap?): Float {
    if (bitmap == null) return 0.08f
    var brightnessSquared = 0f
    for (row in 0 until 8) {
        for (column in 0 until 8) {
            val x = ((column + 0.5f) / 8f * bitmap.width).toInt()
                .coerceIn(0, bitmap.width - 1)
            val y = ((row + 0.5f) / 8f * bitmap.height).toInt()
                .coerceIn(0, bitmap.height - 1)
            val pixel = bitmap.getPixel(x, y)
            val luminance = (
                ((pixel shr 16) and 0xFF) * 0.2126f +
                    ((pixel shr 8) and 0xFF) * 0.7152f +
                    (pixel and 0xFF) * 0.0722f
                ) / 255f
            brightnessSquared += luminance * luminance
        }
    }
    val brightness = kotlin.math.sqrt(brightnessSquared / 64f)
    // Light tint protection for legibility, not a dark veil over artwork colours.
    return (0.02f + brightness * 0.08f).coerceIn(0.03f, 0.11f)
}

@Composable
private fun ImmersiveArtwork(
    song: Song?,
    animatedArtworkEnabled: Boolean = true,
    artistBottomPx: Float = Float.NaN,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    // Retain the current frame while the next song's artwork is decoded. Resetting
    // these states on every URI change briefly exposed the empty placeholder.
    val initialSnapshot = remember { song?.let { ImmersiveArtworkCache.cached(it) } }
    val initialCover = remember {
        initialSnapshot?.cover ?: song?.let {
            ArtworkLoader.cached(it.uri, 1_600, it.albumId.takeIf { id -> id > 0L }?.toString())
        }
    }
    var artwork by remember { mutableStateOf(initialCover) }
    var staticBlur by remember { mutableStateOf(initialSnapshot?.blur) }
    var backdrop by remember { mutableStateOf(initialSnapshot?.let { Color(it.backdrop) } ?: Color(0xFF141414)) }
    var hasPresentedArtwork by remember { mutableStateOf(initialCover != null) }
    LaunchedEffect(artwork) {
        if (artwork != null && !hasPresentedArtwork) {
            androidx.compose.runtime.withFrameNanos { }
            hasPresentedArtwork = true
        }
    }
    var animatedArtworkUrl by remember(song?.id, song?.uri) { mutableStateOf<String?>(null) }
    var artworkReloadAttempt by remember(song?.uri) { mutableIntStateOf(0) }
    // Freeze the first usable animated frame's colours for the entire song,
    // including when an unavailable stream switches to an alternative URL.
    var frozenAnimatedBlur by remember(song?.uri) { mutableStateOf<Bitmap?>(null) }
    var frozenAnimatedBackdrop by remember(song?.uri) { mutableStateOf<Color?>(null) }

    LaunchedEffect(song?.uri, song?.albumId) {
        val targetSong = song
        if (targetSong == null) {
            artwork = null
            staticBlur = null
            backdrop = Color(0xFF141414)
            return@LaunchedEffect
        }

        val albumCacheKey = targetSong.albumId
            .takeIf { albumId -> albumId > 0L }
            ?.toString()

        suspend fun publish(bitmap: Bitmap?) {
            val prepared = bitmap?.let { prepareImmersiveArtwork(it) }
            artwork = bitmap
            staticBlur = prepared?.first
            backdrop = prepared?.second ?: Color(0xFF141414)
            if (bitmap != null && prepared != null) {
                ImmersiveArtworkCache.save(context, targetSong,
                    ImmersiveArtworkSnapshot(bitmap, prepared.first, prepared.second.toArgb()))
            }
        }

        // Restore the small prepared preview before reopening the audio file.
        // It includes the blur and palette, so the first cover needs no extra preparation.
        val restored = ImmersiveArtworkCache.restore(context, targetSong)
        if (restored != null) {
            artwork = restored.cover
            staticBlur = restored.blur
            backdrop = Color(restored.backdrop)
        }

        val cachedFullArtwork = ArtworkLoader.cached(
            uriString = targetSong.uri,
            maxSize = 1_600,
            cacheKey = albumCacheKey,
            allowSmallerPreview = false
        )
        if (cachedFullArtwork != null) {
            if (cachedFullArtwork !== artwork) publish(cachedFullArtwork)
            else if (staticBlur == null) publish(cachedFullArtwork)
            else if (restored != null) ImmersiveArtworkCache.save(context, targetSong, restored)
            return@LaunchedEffect
        }

        // The vinyl view normally has a 512 px cover in memory already. Present it
        // immediately, then crossfade to the full-resolution immersive cover.
        val cachedPreview = ArtworkLoader.cached(
            uriString = targetSong.uri,
            maxSize = 512,
            cacheKey = albumCacheKey
        )
        if (restored == null && cachedPreview != null && cachedPreview !== artwork) {
            publish(cachedPreview)
        }

        val loaded = ArtworkLoader.load(
            context = context,
            uriString = targetSong.uri,
            maxSize = 1_600,
            cacheKey = albumCacheKey
        )
        if (loaded != null && loaded !== artwork) publish(loaded)
        else if (loaded == null && restored == null && cachedPreview == null) publish(null)
    }

    LaunchedEffect(
        song?.id, song?.title, song?.artist, song?.album, animatedArtworkEnabled, artworkReloadAttempt
    ) {
        if (!animatedArtworkEnabled || song == null) {
            animatedArtworkUrl = null
            return@LaunchedEffect
        }
        repeat(2) { attempt ->
            val url = AppleMusicAnimatedArtworkLoader.loadAnimatedArtworkUrl(
                context, song, forceRefresh = artworkReloadAttempt > 0 || attempt > 0
            )
            if (url != null) {
                animatedArtworkUrl = url
                return@LaunchedEffect
            }
            if (attempt == 0) delay(1_500L)
        }
    }

    val activeBlur = if (animatedArtworkEnabled && animatedArtworkUrl != null) {
        frozenAnimatedBlur ?: staticBlur
    } else staticBlur
    val targetScrimOpacity = remember(activeBlur) { immersiveScrimOpacity(activeBlur) }
    val scrimOpacity by animateFloatAsState(
        targetValue = targetScrimOpacity,
        animationSpec = tween(durationMillis = if (hasPresentedArtwork) 360 else 0),
        label = "immersiveScrimOpacity"
    )
    // The animated frame supplies both the frozen blur and its actual colours.
    // The static album colour is only a fallback until a video frame is captured.
    val animatedBackdrop by animateColorAsState(
        targetValue = if (animatedArtworkEnabled && animatedArtworkUrl != null) {
            frozenAnimatedBackdrop ?: backdrop
        } else backdrop,
        animationSpec = tween(durationMillis = if (hasPresentedArtwork) 500 else 0),
        label = "immersiveBackdrop"
    )

    BoxWithConstraints(modifier.background(animatedBackdrop)) {
        val sharpBottom = resolveArtworkBottom(artistBottomPx, constraints.maxHeight.toFloat())
        val sharpHeight = with(LocalDensity.current) { sharpBottom.toDp() }
        val fadeSpan = sharpBottom * 0.72f

        // Match the cover crop and extend its blurred edge colours without reflections.
        Crossfade(
            targetState = activeBlur,
            animationSpec = tween(durationMillis = if (hasPresentedArtwork) 360 else 0),
            label = "immersiveBlur",
            modifier = Modifier.matchParentSize()
        ) { blurBitmap ->
            Box(
                Modifier
                    .matchParentSize()
                    .drawWithCache {
                        val paint = if (
                            blurBitmap != null && size.width > 0f && sharpBottom > 0f
                        ) {
                            val scale = maxOf(
                                size.width / blurBitmap.width,
                                sharpBottom / blurBitmap.height
                            )
                            val matrix = android.graphics.Matrix().apply {
                                setScale(scale, scale)
                                postTranslate((size.width - blurBitmap.width * scale) / 2f, 0f)
                            }
                            val shader = android.graphics.BitmapShader(
                                blurBitmap,
                                android.graphics.Shader.TileMode.CLAMP,
                                android.graphics.Shader.TileMode.CLAMP
                            ).apply { setLocalMatrix(matrix) }
                            android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG).apply {
                                this.shader = shader
                            }
                        } else null
                        onDrawBehind {
                            if (paint != null) drawIntoCanvas { canvas ->
                                canvas.nativeCanvas.drawRect(0f, 0f, size.width, size.height, paint)
                            }
                        }
                    }
            )
        }

        // Blend toward the dominant cover tone without lifting dark artwork with white.
        Box(
            Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        0.00f to Color.Transparent,
                        0.28f to Color.Transparent,
                        0.48f to animatedBackdrop.copy(alpha = 0.12f),
                        0.68f to animatedBackdrop.copy(alpha = 0.35f),
                        0.84f to animatedBackdrop.copy(alpha = 0.65f),
                        1.00f to animatedBackdrop.copy(alpha = 0.90f)
                    )
                )
        )

        // Static and animated artwork share the same boundary and feathered mask.
        Box(
            Modifier
                .fillMaxWidth()
                .height(sharpHeight)
                .align(Alignment.TopCenter)
                .graphicsLayer {
                    clip = true
                    compositingStrategy = CompositingStrategy.Offscreen
                }
                .drawWithCache {
                    val painter = ImmersiveArtworkPainter(size, size.height, fadeSpan)
                    onDrawWithContent {
                        drawContent()
                        with(painter) { fadeArtwork() }
                    }
                }
        ) {
            Crossfade(
                targetState = artwork,
                animationSpec = tween(durationMillis = if (hasPresentedArtwork) 360 else 0),
                label = "immersiveCover",
                modifier = Modifier.fillMaxSize()
            ) { cover ->
                if (cover != null) {
                    Image(
                        bitmap = cover.asImageBitmap(),
                        contentDescription = song?.title,
                        contentScale = ContentScale.Crop,
                        alignment = Alignment.TopCenter,
                        modifier = Modifier.fillMaxSize()
                    )
                } else if (song == null) {
                    Icon(
                        Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.18f),
                        modifier = Modifier.size(42.dp).align(Alignment.Center)
                    )
                }
            }

            val activeUrl = animatedArtworkUrl
            if (animatedArtworkEnabled && activeUrl != null) {
                AnimatedArtworkVideoBackground(
                    url = activeUrl,
                    modifier = Modifier.fillMaxSize(),
                    captureBackdrop = frozenAnimatedBlur == null,
                    onBackdropReady = { blurredFrame, frameColor ->
                        if (animatedArtworkUrl == activeUrl && frozenAnimatedBlur == null) {
                            frozenAnimatedBlur = blurredFrame
                            frozenAnimatedBackdrop = frameColor
                        }
                    },
                    onUnavailable = {
                        if (animatedArtworkUrl == activeUrl) {
                            val alternative = AppleMusicAnimatedArtworkLoader.nextPlaybackUrl(activeUrl)
                            animatedArtworkUrl = alternative
                            if (alternative == null && artworkReloadAttempt < 1) artworkReloadAttempt++
                        }
                    }
                )
            }
        }

        Box(
            Modifier
                .matchParentSize()
                .graphicsLayer { alpha = scrimOpacity }
                .drawWithCache {
                    val painter = ImmersiveArtworkPainter(size, sharpBottom, fadeSpan)
                    onDrawBehind { with(painter) { drawScrim() } }
                }
        )
    }
}

@Composable
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
private fun AnimatedArtworkVideoBackground(
    url: String,
    modifier: Modifier = Modifier,
    captureBackdrop: Boolean,
    onBackdropReady: (Bitmap, Color) -> Unit,
    onUnavailable: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var videoReady by remember(url) { mutableStateOf(false) }
    var videoVisible by remember(url) { mutableStateOf(false) }
    var playbackRetryCount by remember(url) { mutableIntStateOf(0) }
    var artworkPlayerView by remember { mutableStateOf<androidx.media3.ui.PlayerView?>(null) }
    val onBackdropReadyCallback by rememberUpdatedState(onBackdropReady)
    val onUnavailableCallback by rememberUpdatedState(onUnavailable)
    val shouldCaptureBackdrop by rememberUpdatedState(captureBackdrop)

    val player = remember(url) {
        val trackSelector = androidx.media3.exoplayer.trackselection.DefaultTrackSelector(context).apply {
            parameters = buildUponParameters()
                .setMaxVideoSize(720, 1280)
                .setMaxVideoFrameRate(30)
                .setMaxVideoBitrate(4_000_000)
                .setTrackTypeDisabled(androidx.media3.common.C.TRACK_TYPE_AUDIO, true)
                .build()
        }
        androidx.media3.exoplayer.ExoPlayer.Builder(context)
            .setTrackSelector(trackSelector)
            .setAudioAttributes(
                androidx.media3.common.AudioAttributes.Builder()
                    .setUsage(androidx.media3.common.C.USAGE_MEDIA)
                    .setContentType(androidx.media3.common.C.AUDIO_CONTENT_TYPE_MOVIE)
                    .build(),
                false
            )
            .build()
            .apply {
            volume = 0f
            repeatMode = Player.REPEAT_MODE_ONE
            setMediaItem(
                MediaItem.Builder()
                    .setUri(url)
                    .setMimeType(androidx.media3.common.MimeTypes.APPLICATION_M3U8)
                    .build()
            )
        }
    }

    // Do not require a successful TextureView screenshot to reveal animated artwork.
    // Some devices render video correctly but never return a usable capture.
    LaunchedEffect(player, videoReady) {
        if (videoReady) {
            delay(400L)
            videoVisible = true
        }
    }

    LaunchedEffect(player, artworkPlayerView, videoVisible) {
        val view = artworkPlayerView ?: return@LaunchedEffect
        // Start after the video has actually faded in: hidden TextureViews on
        // some devices return a black bitmap even when playback has started.
        if (!videoVisible || !shouldCaptureBackdrop) return@LaunchedEffect
        delay(450L)
        repeat(40) {
            androidx.compose.runtime.withFrameNanos { }
            val frame = ArtworkBlur.captureViewport(view)
            if (frame != null) {
                if (ArtworkBlur.hasVisibleFrame(frame)) {
                    val prepared = try {
                        prepareImmersiveArtwork(frame)
                    } finally {
                        frame.recycle()
                    }
                    onBackdropReadyCallback(prepared.first, prepared.second)
                    return@LaunchedEffect
                }
                frame.recycle()
            }
            delay(150L)
        }
    }

    LaunchedEffect(player, playbackRetryCount) {
        if (playbackRetryCount > 0) {
            delay(750L * playbackRetryCount)
            player.prepare()
            player.playWhenReady = true
        }
    }

    androidx.compose.runtime.DisposableEffect(player, url) {
        val listener = object : Player.Listener {
            override fun onRenderedFirstFrame() {
                videoReady = true
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                videoReady = false
                videoVisible = false
                if (error.errorCode in 2000..2999 && playbackRetryCount < 2) {
                    playbackRetryCount++
                } else onUnavailableCallback()
            }
        }

        player.addListener(listener)
        player.prepare()
        player.playWhenReady = true
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    val videoAlpha by animateFloatAsState(
        targetValue = if (videoVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 360),
        label = "animatedArtworkFade"
    )

    androidx.compose.ui.viewinterop.AndroidView(
        factory = { viewContext ->
            val canvasPlayerView = android.view.LayoutInflater.from(viewContext)
                .inflate(
                    R.layout.immersive_canvas_player_view,
                    null,
                    false
                ) as androidx.media3.ui.PlayerView
            canvasPlayerView.player = player
            artworkPlayerView = canvasPlayerView
            canvasPlayerView
        },
        update = { playerView ->
            if (playerView.player !== player) playerView.player = player
            if (artworkPlayerView !== playerView) artworkPlayerView = playerView
        },
        modifier = modifier.graphicsLayer {
            // Keep the still artwork visible until the first video frame is ready.
            alpha = videoAlpha
        }
    )
}

@Composable
private fun ArtworkView(
    song: Song?,
    maxSizePx: Int = 512,
    modifier: Modifier = Modifier,
    monochrome: Boolean = false,
    fallbackMaxSizePx: Int? = null,
    loadDelayMs: Long = 0L,
    loadAllowed: Boolean = true,
    grainOverlay: Boolean = true
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val density = androidx.compose.ui.platform.LocalDensity.current
    // A cached carousel thumbnail is shown instantly until the full CD cover
    // is ready. Preserve default behaviour in Vinyl and other player modes.
    var bitmap by remember(song?.uri, maxSizePx, fallbackMaxSizePx) {
        mutableStateOf(
            song?.let {
                val cacheKey = it.albumId.takeIf { albumId -> albumId > 0L }?.toString()
                ArtworkLoader.cached(
                    uriString = it.uri,
                    maxSize = maxSizePx,
                    cacheKey = cacheKey
                ) ?: fallbackMaxSizePx?.let { previewSize ->
                    ArtworkLoader.cached(
                        uriString = it.uri,
                        maxSize = previewSize,
                        cacheKey = cacheKey
                    )
                }
            }
        )
    }

    LaunchedEffect(song?.uri, maxSizePx, loadDelayMs, loadAllowed) {
        if (!loadAllowed) return@LaunchedEffect
        // Skip IO for tracks that are replaced by rapid Shuffle transitions.
        if (song != null && loadDelayMs > 0L) delay(loadDelayMs)
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
            .then(if (grainOverlay) Modifier.drawWithCache {
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
            } else Modifier),
        contentAlignment = Alignment.Center
    ) {
        val cachedImage = remember(bitmap) { bitmap?.asImageBitmap() }
        if (cachedImage != null) {
            androidx.compose.foundation.Image(
                bitmap = cachedImage,
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
private fun LibrarySongListRow(
    song: Song,
    selected: Boolean,
    loadDetails: Boolean,
    onPlay: (Song) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    // Cached Hi-Res information appears on the first composed frame, even
    // when the user is actively scrolling the playlist.
    var codecInfo by remember(song.uri) {
        mutableStateOf(AudioCodecLoader.cached(song.uri))
    }
    val currentlyScrolling by rememberUpdatedState(!loadDetails)

    LaunchedEffect(context, song.uri) {
        // Unlike cover decoding, the tiny Hi-Res badge must not wait for
        // scrolling to stop. A short dwell prevents flung-away rows from
        // opening extractors; cached entries already render immediately.
        if (codecInfo == null && currentlyScrolling) delay(35L)
        codecInfo = AudioCodecLoader.load(
            context = context,
            uriString = song.uri
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .then(
                if (selected) {
                    Modifier.background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.07f),
                        RoundedCornerShape(12.dp)
                    )
                } else {
                    Modifier
                }
            )
            .clickable { onPlay(song) }
            .padding(
                horizontal = 8.dp,
                vertical = 7.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(58.dp)
                .clip(RoundedCornerShape(7.dp))
        ) {
            ArtworkView(
                song = song,
                maxSizePx = 160,
                // Load visible thumbnails even while scrolling. The previous
                // scroll gate left covers blank until the fling stopped.
                // A brief debounce plus the shared 2-decoder limit keeps
                // fast flings from flooding storage with abandoned requests.
                loadDelayMs = 24L,
                grainOverlay = false,
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = song.title,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                fontSize = 15.sp,
                lineHeight = 19.sp,
                fontWeight = if (selected) {
                    FontWeight.Bold
                } else {
                    FontWeight.Medium
                },
                color = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )

            Spacer(Modifier.height(2.dp))

            Text(
                text = song.artist.ifBlank {
                    nusaText("Artis tidak dikenal", "Unknown artist")
                },
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                fontSize = 12.5.sp,
                lineHeight = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (codecInfo?.isHiRes == true) {
            Spacer(Modifier.width(8.dp))
            Image(
                painter = painterResource(id = R.drawable.hi_res_audio_logo),
                contentDescription = "Hi-Res Audio",
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(22.dp)
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
