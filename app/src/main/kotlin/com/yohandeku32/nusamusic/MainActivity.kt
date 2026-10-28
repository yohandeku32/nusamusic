package com.yohandeku32.nusamusic

import android.Manifest
import androidx.compose.material3.TextButton
import androidx.lifecycle.compose.currentStateAsState
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
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.zIndex
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import com.yohandeku32.nusamusic.data.AnimatedArtworkQueuePreloader
import com.yohandeku32.nusamusic.data.ArtworkSourceOrder
import com.yohandeku32.nusamusic.data.AppleTallVideoCache
import com.yohandeku32.nusamusic.data.upcomingPlaybackIndices
import com.yohandeku32.nusamusic.data.SpotifyCanvasArtworkLoader
import com.yohandeku32.nusamusic.data.SpotifyCanvasVideoCache
import com.yohandeku32.nusamusic.data.AudioCodecInfo
import com.yohandeku32.nusamusic.data.AudioCodecLoader
import com.yohandeku32.nusamusic.data.ArtistNameUtils
import com.yohandeku32.nusamusic.data.AppleArtistPortraitLoader
import com.yohandeku32.nusamusic.data.portraitPrefetchArtists
import com.yohandeku32.nusamusic.data.MusicRepository
import com.yohandeku32.nusamusic.data.MediaStoreCheckpoint
import com.yohandeku32.nusamusic.data.LyricsRepository
import com.yohandeku32.nusamusic.data.LyricsPendingSaveQueue
import com.yohandeku32.nusamusic.data.SongLyrics
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
import com.yohandeku32.nusamusic.ui.ImmersiveMusicLibrary
import com.yohandeku32.nusamusic.ui.immersiveColors
import com.yohandeku32.nusamusic.ui.ImmersiveArtworkPlayer
import com.yohandeku32.nusamusic.ui.immersiveGlassPill
import com.yohandeku32.nusamusic.ui.ImmersiveArtworkScene
import com.yohandeku32.nusamusic.ui.ImmersiveArtistPage
import com.yohandeku32.nusamusic.ui.ImmersiveArtistPageState
import com.yohandeku32.nusamusic.ui.ImmersiveDiscoveryPage
import com.yohandeku32.nusamusic.ui.ImmersiveLyricsPanel
import com.yohandeku32.nusamusic.ui.ImmersiveLyricsState
import com.yohandeku32.nusamusic.ui.MiuixHorizontalPager
import com.yohandeku32.nusamusic.ui.animateMiuixScrollToPage
import com.yohandeku32.nusamusic.ui.artworkBackdropColor
import com.yohandeku32.nusamusic.ui.PlayerScaleHost
import com.yohandeku32.nusamusic.ui.PlayerScalePreference

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withPermit
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
    private var nextArtworkLookaheadJob: Job? = null
    private var nextArtworkLookaheadIds: List<Long> = emptyList()
    private var nextArtistPortraitJob: Job? = null
    private var nextArtistPortraitIds: List<Long> = emptyList()
    // At most two remote artist photos at once, regardless of queue changes.
    private val artistPortraitSlots = kotlinx.coroutines.sync.Semaphore(2)
    private var restoringPlaybackQueue = false
    private var songs by mutableStateOf<List<Song>>(emptyList())
    private var currentSong by mutableStateOf<Song?>(null)
    private var pendingDeletedSong: Song? = null
    private var deletionRequiresRetry = false
    // Re-read the currently playing embedded metadata when an external app
    // edits the audio file, without waiting for a manual Library rescan.
    private var lyricsMetadataGeneration by mutableIntStateOf(0)
    private var isPlaying by mutableStateOf(false)
    private var lyricsPlaybackEpoch by mutableIntStateOf(0)
    private val pendingLyricSaves by lazy { LyricsPendingSaveQueue(applicationContext) }
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
    private var mediaStoreRefreshPending = false
    private var manualRefreshPending = false
    private var libraryCacheLoaded by mutableStateOf(false)
    private var libraryScanFailed by mutableStateOf(false)
    private var scanAfterCacheLoad = false

    private val musicContentObserver = object : ContentObserver(
        Handler(Looper.getMainLooper())
    ) {
        override fun onChange(selfChange: Boolean, uri: Uri?) {
            super.onChange(selfChange, uri)

            if (!permissionGranted) return
            // Android can notify the audio collection URI (not the song's
            // item URI) when an external editor changes M4A/MP3 tags. An exact
            // URI comparison misses those notifications and leaves Lyrics stale.
            // A generation change refreshes only the current song's small
            // metadata reader, NOT the whole audio payload or entire library.
            if (currentSong != null) lyricsMetadataGeneration++
            if (isScanningMusic) {
                // Never lose a change event received during a running scan.
                mediaStoreRefreshPending = true
                return
            }

            scheduleMediaStoreRefresh()
        }
    }

    private val musicFolderPicker =
        registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            if (uri == null) return@registerForActivityResult

            // Persist write access too when the DocumentsProvider grants it.
            // This is required for explicit per-song SAF deletion. Providers
            // with read-only folders can still be imported normally.
            val read = Intent.FLAG_GRANT_READ_URI_PERMISSION
            val write = Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            runCatching {
                contentResolver.takePersistableUriPermission(uri, read or write)
            }.onFailure {
                runCatching { contentResolver.takePersistableUriPermission(uri, read) }
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

    private val permanentDeleteLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        val target = pendingDeletedSong
        pendingDeletedSong = null
        if (result.resultCode == android.app.Activity.RESULT_OK && target != null) {
            if (deletionRequiresRetry) {
                deletionRequiresRetry = false
                lifecycleScope.launch { deleteWithGrantedPermission(target) }
            } else {
                completePermanentDeletion(target)
            }
        } else {
            deletionRequiresRetry = false
        }
    }

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            permissionGranted = granted
            if (granted) {
                if (libraryCacheLoaded) {
                    if (!libraryPrefs.contains("songs_cache_json")) {
                        scanMusic()
                    } else {
                        startMusicScan(scanSelectedFolders = false, showToast = false)
                    }
                } else {
                    scanAfterCacheLoad = true
                }
            }
        }

    // Android can restore a Compose text field's focus when the activity
    // returns from the background, even while the library page is offscreen.
    // Clear native focus AND request IME hide on both lifecycle boundaries.
    private fun dismissRestoredKeyboard() {
        currentFocus?.clearFocus()
        window.decorView.clearFocus()
        androidx.core.view.WindowInsetsControllerCompat(
            window, window.decorView
        ).hide(androidx.core.view.WindowInsetsCompat.Type.ime())
    }

    override fun onPause() {
        dismissRestoredKeyboard()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        // External tag editors may modify ID3/FLAC without notifying
        // MediaStore. A foreground return rechecks the current song.
        lyricsMetadataGeneration++
        // Run after Android reapplies window focus; do not let a retained
        // BasicTextField bring the keyboard up over the music player.
        window.decorView.post { dismissRestoredKeyboard() }
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
        val showWelcome = com.yohandeku32.nusamusic.ui.ArunaIntroPreferences.prepare(
            uiPrefsForFonts(), existingUse = selectedMusicFolders.isNotEmpty() ||
                libraryPrefs.all.isNotEmpty() || playbackPrefs.all.isNotEmpty()
        )

        contentResolver.registerContentObserver(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            true,
            musicContentObserver
        )

        // Restore the last track before Compose draws the first frame. The full
        // library and Media3 controller can finish initializing in the background.
        currentSong = readLastSongForLaunch()
        // Start verifying the one visible badge before Compose inflates the
        // expensive artwork/player tree. It shares AudioCodecLoader's cache
        // with the Library, so no duplicate MediaExtractor runs.
        currentSong?.let { launchSong ->
            lifecycleScope.launch {
                AudioCodecLoader.load(this@MainActivity, launchSong.uri)
            }
        }
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

        val token = SessionToken(this, ComponentName(this, PlaybackService::class.java))
        val future = MediaController.Builder(this, token).buildAsync()
        future.addListener({
            val c = future.get()
            controller = c
            queuedPlaybackOrder = null
            c.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(playing: Boolean) {
                    isPlaying = playing
                    positionMs = c.currentPosition.coerceAtLeast(0L)
                    persistPlaybackState(c)
                }

                override fun onPositionDiscontinuity(oldPosition: Player.PositionInfo, newPosition: Player.PositionInfo, reason: Int) {
                    lyricsPlaybackEpoch++
                    positionMs = c.currentPosition.coerceAtLeast(0L)
                }

                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    val id = mediaItem?.mediaId?.toLongOrNull()
                    currentSong = songs.firstOrNull { it.id == id } ?: currentSong
                    positionMs = c.currentPosition.coerceAtLeast(0L)
                    durationMs = c.duration.coerceAtLeast(0L)
                    persistPlaybackState(c, force = true)
                    scheduleArtworkLookahead(c)
                    schedulePendingLyricCommit()
                }

                override fun onTimelineChanged(
                    timeline: androidx.media3.common.Timeline,
                    reason: Int
                ) {
                    // Queue rebuilds, restores, and shuffled order can change
                    // the next song without changing the currently playing song.
                    scheduleArtworkLookahead(c)
                }

                override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                    shuffleEnabled = shuffleModeEnabled
                    persistPlaybackState(c, force = true)
                    scheduleArtworkLookahead(c)
                }

                override fun onRepeatModeChanged(repeatModeValue: Int) {
                    repeatMode = repeatModeValue
                    persistPlaybackState(c, force = true)
                    scheduleArtworkLookahead(c)
                }
            })

            shuffleEnabled = c.shuffleModeEnabled
            repeatMode = c.repeatMode
            syncCurrentSong(c)
            lifecycleScope.launch {
                restorePlaybackStateIfNeeded(c)
                schedulePendingLyricCommit()
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
                    if (currentSong?.id != c.currentMediaItem?.mediaId?.toLongOrNull()) syncCurrentSong(c)

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
                    isPlaying && playbackUiActive && lyricsPageVisible -> 250L
                    isPlaying && playbackUiActive && playerPageVisible -> 250L
                    else -> 1_000L
                })
            }
        }

        setContent {
            NusaMusicTheme {
                com.yohandeku32.nusamusic.ui.ArunaIntroHost(
                    preferences = uiPrefsForFonts(), showInitially = showWelcome,
                    onChooseFolder = ::openMusicFolderPicker,
                    onDeviceMusic = {
                        if (permissionGranted) scanMusic() else permissionLauncher.launch(permission)
                    }
                ) { openLibrary ->
                NusaMusicApp(
                    startInLibrary = openLibrary,
                    songs = songs,
                    currentSong = currentSong,
                    isPlaying = isPlaying,
                    positionMsState = playbackPositionState,
                    lyricsPlaybackEpoch = lyricsPlaybackEpoch,
                    lyricsMetadataGeneration = lyricsMetadataGeneration,
                    readPlaybackPosition = { controller?.currentPosition?.coerceAtLeast(0L) ?: positionMs },
                    readPlaybackEpoch = { lyricsPlaybackEpoch },
                    durationMs = durationMs,
                    permissionGranted = permissionGranted,
                    onPlay = ::playSong,
                    onPlayNext = ::queueSongNext,
                    onDeleteSong = ::requestPermanentSongDeletion,
                    onTogglePlay = ::togglePlay,
                    onQueueLyrics = { song, lyrics ->
                        pendingLyricSaves.enqueue(song, lyrics)
                        schedulePendingLyricCommit()
                    },
                    isLyricsQueued = { song -> pendingLyricSaves.isQueued(song.id) },
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
        }

        window.decorView.post { hideStatusBar() }
    }

    /**
     * Media3 must release the previous track before SAF rewrites it. Never
     * pause the current song and never truncate its currently opened source.
     * A durable pending request also resumes after reopening NusaMusic.
     */
    private fun schedulePendingLyricCommit() {
        lifecycleScope.launch {
            // Allow decoder/media source handoff to finish after a transition.
            delay(1_400L)
            if (controller == null) return@launch
            pendingLyricSaves.finishInactive(
                currentSongId = { controller?.currentMediaItem?.mediaId?.toLongOrNull() },
                onResult = { _, success, message ->
                    if (success) lyricsMetadataGeneration++
                    android.widget.Toast.makeText(
                        this@MainActivity, message, android.widget.Toast.LENGTH_LONG
                    ).show()
                }
            )
        }
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
        restoringPlaybackQueue = true
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
        restoringPlaybackQueue = false
        scheduleArtworkLookahead(c)
    }

    private fun toggleShuffle() {
        controller?.let { c ->
            c.shuffleModeEnabled = !c.shuffleModeEnabled
            shuffleEnabled = c.shuffleModeEnabled
            persistPlaybackState(c, force = true)
            scheduleArtworkLookahead(c)
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
            if (shouldScan) {
                scanMusic()
            } else if (permissionGranted) {
                // Check for new tracks while retaining the cached list on
                // screen. Opening the player does not traverse SAF folders.
                startMusicScan(scanSelectedFolders = false, showToast = false)
            }

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
            libraryPrefs.edit()
                .remove("songs_cache_json")
                .remove("mediastore_checkpoint_version")
                .remove("mediastore_checkpoint_generation")
                .apply()
            null
        }
    }

    private fun readMediaStoreCheckpoint(): MediaStoreCheckpoint? {
        if (!libraryPrefs.contains("songs_cache_json") ||
            !libraryPrefs.contains("mediastore_checkpoint_generation")
        ) return null
        val version = libraryPrefs.getString("mediastore_checkpoint_version", null)
            ?: return null
        return MediaStoreCheckpoint(
            version = version,
            generation = libraryPrefs.getLong("mediastore_checkpoint_generation", 0L)
        )
    }

    private fun persistCachedSongs(
        songsToCache: List<Song>,
        checkpoint: MediaStoreCheckpoint?
    ) {
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
            .apply {
                if (checkpoint == null) {
                    remove("mediastore_checkpoint_version")
                    remove("mediastore_checkpoint_generation")
                } else {
                    putString("mediastore_checkpoint_version", checkpoint.version)
                    putLong("mediastore_checkpoint_generation", checkpoint.generation)
                }
            }
            .apply()
    }

    private suspend fun refreshSongs(scanSelectedFolders: Boolean): Int {
        // Snapshot on main; index and serialize the cache off the UI thread.
        val cachedSongs = songs
        val previousIds = withContext(Dispatchers.Default) {
            cachedSongs.mapTo(HashSet()) { it.id }
        }
        val folderUris = selectedMusicFolders.toSet()
        val checkpoint = readMediaStoreCheckpoint()
        val repository = MusicRepository(this@MainActivity)

        val fastScan = withContext(Dispatchers.IO) {
            repository.loadSongs(
                extraFolderUris = folderUris,
                cachedSongs = cachedSongs,
                previousCheckpoint = checkpoint,
                scanSelectedFolders = false,
                includeMediaStore = permissionGranted
            )
        }

        // With manually selected folders, publish the OS-indexed changes now,
        // not after a slow DocumentsProvider traversal. Keep existing SAF songs
        // visible until that traversal finishes.
        if (scanSelectedFolders && folderUris.isNotEmpty()) {
            songs = fastScan.songs
        }

        val completedScan = withContext(Dispatchers.IO) {
            val result = if (scanSelectedFolders && folderUris.isNotEmpty()) {
                repository.loadSongs(
                    extraFolderUris = folderUris,
                    cachedSongs = fastScan.songs,
                    previousCheckpoint = fastScan.checkpoint,
                    scanSelectedFolders = true,
                    includeMediaStore = permissionGranted
                )
            } else {
                fastScan
            }
            persistCachedSongs(result.songs, result.checkpoint)
            result
        }

        val refreshedSongs = completedScan.songs
        val newSongs = withContext(Dispatchers.Default) {
            if (previousIds.isEmpty()) emptyList()
            else refreshedSongs.filter { it.id !in previousIds }
        }

        songs = refreshedSongs
        controller?.let { c ->
            syncCurrentSong(c)
            restorePlaybackStateIfNeeded(c)

            // Preserve playback order and append just the newly indexed songs.
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

    private fun scheduleMediaStoreRefresh() {
        automaticMusicScanJob?.cancel()
        automaticMusicScanJob = lifecycleScope.launch {
            // Indexers often emit multiple notifications for one new file.
            delay(900L)
            startMusicScan(scanSelectedFolders = false, showToast = false)
        }
    }

    private fun scanMusic() {
        // Explicit settings/manual scan: reconcile selected SAF folders too.
        automaticMusicScanJob?.cancel()
        startMusicScan(scanSelectedFolders = true, showToast = true)
    }

    private fun startMusicScan(scanSelectedFolders: Boolean, showToast: Boolean) {
        if (!libraryCacheLoaded) {
            scanAfterCacheLoad = true
            return
        }
        if (isScanningMusic) {
            if (scanSelectedFolders) manualRefreshPending = true
            else mediaStoreRefreshPending = true
            return
        }
        if (!permissionGranted && selectedMusicFolders.isEmpty()) return

        libraryScanFailed = false
        isScanningMusic = true

        lifecycleScope.launch {
            val result = runCatching { refreshSongs(scanSelectedFolders) }
            libraryScanFailed = result.isFailure
            isScanningMusic = false

            if (showToast) {
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

            // A device update or another swipe arriving mid-scan is not lost.
            val manual = manualRefreshPending
            val media = mediaStoreRefreshPending
            manualRefreshPending = false
            mediaStoreRefreshPending = false
            when {
                manual -> scanMusic()
                media -> scheduleMediaStoreRefresh()
            }
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

    private fun queueSongNext(song: Song) {
        val c = controller ?: return
        if (c.currentMediaItemIndex < 0 || c.mediaItemCount == 0) {
            playSong(song, listOf(song))
            return
        }
        // Inserting a track requires one MediaController call; do not
        // rebuild the entire Media3 queue when tapping "Play next".
        pendingPlaySongJob?.cancel()
        pendingPlaySongJob = null
        c.addMediaItem(
            (c.currentMediaItemIndex + 1).coerceAtMost(c.mediaItemCount),
            mediaItemFor(song)
        )
        queuedPlaybackOrder = null
        android.widget.Toast.makeText(
            this, "Added to Up Next", android.widget.Toast.LENGTH_SHORT
        ).show()
    }

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

    /**
     * Prefetch the current Artist portrait and those of the next six REAL
     * Media3 queue items. The timeline handles shuffle, repeat, and Play Next;
     * the library's display/sort order is never used as a proxy for playback.
     * All disk/network IO is off-main and is bounded by two fetch slots.
     */
    private fun scheduleArtistPortraitLookahead(player: MediaController) {
        if (restoringPlaybackQueue) return
        val immersive = PlayerPresentationMode.fromPreference(
            uiPrefsForFonts().getString("player_presentation_mode", null)
        ) == PlayerPresentationMode.IMMERSIVE_ARTWORK
        if (!immersive || songs.isEmpty() || player.mediaItemCount == 0 ||
            player.currentTimeline.isEmpty
        ) {
            nextArtistPortraitJob?.cancel()
            nextArtistPortraitJob = null
            nextArtistPortraitIds = emptyList()
            return
        }
        val timeline = player.currentTimeline
        val repeat = if (player.repeatMode == Player.REPEAT_MODE_ALL)
            Player.REPEAT_MODE_ALL else Player.REPEAT_MODE_OFF
        val next = upcomingPlaybackIndices(
            currentIndex = player.currentMediaItemIndex,
            itemCount = player.mediaItemCount,
            limit = 6,
            nextIndex = { index ->
                timeline.getNextWindowIndex(index, repeat, player.shuffleModeEnabled)
            }
        )
        val ids = (listOfNotNull(player.currentMediaItem?.mediaId?.toLongOrNull()) +
            next.mapNotNull { player.getMediaItemAt(it).mediaId.toLongOrNull() })
            .distinct()
        if (ids == nextArtistPortraitIds) return
        nextArtistPortraitJob?.cancel()
        nextArtistPortraitIds = ids
        if (ids.isEmpty()) return

        val librarySnapshot = songs
        nextArtistPortraitJob = lifecycleScope.launch {
            val artists = withContext(Dispatchers.Default) {
                val byId = librarySnapshot.associateBy { it.id }
                portraitPrefetchArtists(
                    ids.mapNotNull(byId::get).map { it.artist }
                )
            }
            kotlinx.coroutines.supervisorScope {
                artists.forEachIndexed { index, name ->
                    launch(Dispatchers.IO) {
                        // The foreground player gets its first frame first.
                        // Current Artist starts earliest; upcoming Artists
                        // follow in playback order without saturating data.
                        delay(if (index == 0) 120L else 500L + (index - 1) * 140L)
                        artistPortraitSlots.withPermit {
                            AppleArtistPortraitLoader.warm(this@MainActivity, name)
                        }
                    }
                }
            }
        }
    }

    /**
     * Keep exactly five real upcoming Media3 entries warm, including its
     * shuffled order. Do not guess the next tracks from library sorting.
     * No ExoPlayer instances are created here (five decoders would stutter);
     * metadata resolution is capped at two and video-byte IO at one.
     */
    private fun scheduleArtworkLookahead(player: MediaController) {
        // Portrait lookahead runs even when animated video artwork is disabled.
        scheduleArtistPortraitLookahead(player)
        if (restoringPlaybackQueue) return
        val preferences = uiPrefsForFonts()
        val sourceOrder = ArtworkSourceOrder.fromPreference(
            preferences.getString("animated_artwork_source_order", null)
        )
        val enabled = preferences.getBoolean("animated_artwork_enabled", true) &&
            PlayerPresentationMode.fromPreference(
                preferences.getString("player_presentation_mode", null)
            ) == PlayerPresentationMode.IMMERSIVE_ARTWORK
        if (!enabled || songs.isEmpty() || player.mediaItemCount <= 1) {
            nextArtworkLookaheadJob?.cancel()
            nextArtworkLookaheadJob = null
            nextArtworkLookaheadIds = emptyList()
            return
        }

        val timeline = player.currentTimeline
        if (timeline.isEmpty) return
        val mode = if (player.repeatMode == Player.REPEAT_MODE_ALL)
            Player.REPEAT_MODE_ALL else Player.REPEAT_MODE_OFF
        val indices = upcomingPlaybackIndices(
            currentIndex = player.currentMediaItemIndex,
            itemCount = player.mediaItemCount,
            limit = 5,
            nextIndex = { index ->
                timeline.getNextWindowIndex(index, mode, player.shuffleModeEnabled)
            }
        )
        val ids = indices.mapNotNull { index ->
            player.getMediaItemAt(index).mediaId.toLongOrNull()
        }
        if (ids == nextArtworkLookaheadIds) return
        nextArtworkLookaheadJob?.cancel()
        nextArtworkLookaheadIds = ids
        if (ids.isEmpty()) return
        val library = songs
        nextArtworkLookaheadJob = lifecycleScope.launch {
            // Give the foreground Canvas/Apple decoder time to acquire its
            // first frame before speculative network reads begin.
            delay(1_100L)
            val ordered = withContext(Dispatchers.Default) {
                val byId = library.associateBy { it.id }
                ids.mapNotNull(byId::get)
            }
            kotlinx.coroutines.supervisorScope {
                // Resolve two metadata requests concurrently, but fetch actual
                // video bytes strictly in playback order (next track first).
                // A faster response from song five must not take the
                // bandwidth allocated to the immediate Next track.
                val lookups = ordered.mapIndexed { offset, song ->
                    async {
                        delay(offset * 90L)
                        AnimatedArtworkQueuePreloader.resolve(
                            this@MainActivity, song, sourceOrder
                        )
                    }
                }
                for ((offset, song) in ordered.withIndex()) {
                    lookups[offset].await()
                    AnimatedArtworkQueuePreloader.preload(
                        this@MainActivity, song, priority = offset,
                        order = sourceOrder
                    )
                }
            }
        }
    }

    // Re-evaluate after a library scan, mode switch or animated-artwork
    // setting change; controller timeline may have existed before songs loaded.
    internal fun refreshArtworkLookahead(reset: Boolean = false) {
        if (reset) {
            nextArtworkLookaheadIds = emptyList()
            nextArtistPortraitIds = emptyList()
        }
        controller?.let(::scheduleArtworkLookahead)
    }

    /**
     * Two distinct confirmations: NusaMusic asks first, then Android grants
     * access to delete the actual MediaStore item (Android 11+). Files from a
     * user-picked SAF folder require the provider's write grant.
     */
    private fun requestPermanentSongDeletion(song: Song) {
        val uri = runCatching { Uri.parse(song.uri) }.getOrNull() ?: return
        if (uri.scheme != "content") {
            deletionError("Only songs from device storage can be deleted.")
            return
        }
        if (pendingDeletedSong != null) return
        pendingDeletedSong = song
        deletionRequiresRetry = false
        if (uri.authority == MediaStore.AUTHORITY && Build.VERSION.SDK_INT >= 30) {
            val request = runCatching {
                MediaStore.createDeleteRequest(contentResolver, listOf(uri))
            }
            request.onSuccess { intentSender ->
                val intent = androidx.activity.result.IntentSenderRequest.Builder(
                    intentSender.intentSender
                ).build()
                permanentDeleteLauncher.launch(intent)
            }.onFailure {
                pendingDeletedSong = null
                deletionError("Unable to request permission to delete this song.")
            }
        } else {
            lifecycleScope.launch { deleteWithGrantedPermission(song) }
        }
    }

    private suspend fun deleteWithGrantedPermission(song: Song) {
        val uri = Uri.parse(song.uri)
        try {
            val removed = withContext(Dispatchers.IO) {
                when {
                    android.provider.DocumentsContract.isDocumentUri(this@MainActivity, uri) ->
                        android.provider.DocumentsContract.deleteDocument(contentResolver, uri)
                    uri.authority == MediaStore.AUTHORITY ->
                        contentResolver.delete(uri, null, null) > 0
                    else -> false
                }
            }
            pendingDeletedSong = null
            deletionRequiresRetry = false
            if (removed) completePermanentDeletion(song)
            else deletionError("File could not be deleted. Re-select its folder with write access.")
        } catch (e: android.app.RecoverableSecurityException) {
            if (Build.VERSION.SDK_INT >= 29) {
                pendingDeletedSong = song
                deletionRequiresRetry = true
                val request = androidx.activity.result.IntentSenderRequest.Builder(
                    e.userAction.actionIntent.intentSender
                ).build()
                permanentDeleteLauncher.launch(request)
            } else {
                pendingDeletedSong = null
                deletionError("Storage provider denied deletion.")
            }
        } catch (_: Exception) {
            pendingDeletedSong = null
            deletionRequiresRetry = false
            deletionError("Storage provider denied deletion. Grant folder write access.")
        }
    }

    private fun completePermanentDeletion(song: Song) {
        // Remove the deleted item from the active Media3 queue as well as
        // the Library, so a Next/Previous action never opens a dead URI.
        controller?.let { player ->
            for (index in player.mediaItemCount - 1 downTo 0) {
                if (player.getMediaItemAt(index).mediaId == song.id.toString()) {
                    player.removeMediaItem(index)
                }
            }
            if (player.mediaItemCount == 0) {
                player.stop()
                currentSong = null
                isPlaying = false
                positionMs = 0L
                durationMs = 0L
            } else syncCurrentSong(player)
        }
        songs = songs.filterNot { it.id == song.id && it.uri == song.uri }
        val updatedSongs = songs
        lifecycleScope.launch(Dispatchers.IO) {
            persistCachedSongs(updatedSongs, null)
        }
        lyricsMetadataGeneration++
        startMusicScan(scanSelectedFolders = selectedMusicFolders.isNotEmpty(), showToast = false)
        android.widget.Toast.makeText(
            this, "Song permanently deleted", android.widget.Toast.LENGTH_SHORT
        ).show()
    }

    private fun deletionError(message: String) {
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_LONG).show()
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
        nextArtistPortraitJob?.cancel()
        nextArtistPortraitJob = null
        nextArtistPortraitIds = emptyList()
        nextArtworkLookaheadJob?.cancel()
        nextArtworkLookaheadJob = null
        nextArtworkLookaheadIds = emptyList()
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
    readPlaybackPosition: () -> Long,
    readPlaybackEpoch: () -> Int,
    lyricsPlaybackEpoch: Int,
    lyricsMetadataGeneration: Int,
    durationMs: Long,
    permissionGranted: Boolean,
    onPlay: (Song, List<Song>) -> Unit,
    onPlayNext: (Song) -> Unit,
    onDeleteSong: (Song) -> Unit,
    onTogglePlay: () -> Unit,
    onQueueLyrics: suspend (Song, SongLyrics) -> Unit,
    isLyricsQueued: (Song) -> Boolean,
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
    onRequestPermission: () -> Unit,
    startInLibrary: Boolean = false
) {
    var isFavorite by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var artistOverlayOpen by remember { mutableStateOf(false) }
    var discoveryArtistName by remember { mutableStateOf<String?>(null) }
    val discoveryListState = androidx.compose.foundation.lazy.rememberLazyListState()
    var libraryOverlayOpen by remember { mutableStateOf(startInLibrary) }
    // Decode animated artwork only after either immersive overlay has fully
    // closed; restarting Media3 while the pill is moving causes a brief hitch.
    var immersiveVideoSuspended by remember { mutableStateOf(false) }
    LaunchedEffect(artistOverlayOpen, libraryOverlayOpen) {
        if (artistOverlayOpen || libraryOverlayOpen) {
            immersiveVideoSuspended = true
        } else if (immersiveVideoSuspended) {
            delay(630L)
            immersiveVideoSuspended = false
        }
    }
    var deleteFromLibrary by remember { mutableStateOf<Song?>(null) }
    val artistKey = ArtistNameUtils.cacheKey(currentSong?.artist)

    val context = androidx.compose.ui.platform.LocalContext.current
    // Restore verified Hi-Res badges as soon as the library becomes available,
    // before the user opens or flings through the song list. All disk IO stays
    // off the main thread; badge cache lookups inside rows are synchronous.
    LaunchedEffect(songs) {
        AudioCodecLoader.warmVerifiedBadges(context, songs)
    }
    var onlineLyricsEnabled by remember {
        mutableStateOf(context.getSharedPreferences(
            "ui_preferences", android.content.Context.MODE_PRIVATE
        ).getBoolean("online_lyrics_enabled", true))
    }
    val lyricsState = remember(context) { ImmersiveLyricsState(LyricsRepository(context)) }
    LaunchedEffect(currentSong?.uri, lyricsMetadataGeneration, onlineLyricsEnabled) {
        val activeSong = currentSong ?: return@LaunchedEffect
        if (!LYRICS_ENABLED || lyricsMetadataGeneration == 0) return@LaunchedEffect
        // File changes from other tag editors (especially M4A) may not
        // increment MediaStore generation or file size. Re-read the audio's
        // actual metadata on every foreground return / notification without
        // forcing a slow network request or discarding online cache.
        delay(420L)
        lyricsState.load(
            activeSong, this, onlineEnabled = onlineLyricsEnabled,
            refreshEmbeddedOnly = true
        )
    }
    val artistPageStates = remember { androidx.collection.LruCache<String, ImmersiveArtistPageState>(8) }
    val discoveryArtistKey = ArtistNameUtils.cacheKey(discoveryArtistName)
    val artistPageState = remember(discoveryArtistKey) {
        artistPageStates.get(discoveryArtistKey) ?: ImmersiveArtistPageState(
            ArtistNameUtils.firstArtist(discoveryArtistName)
        ).also { artistPageStates.put(discoveryArtistKey, it) }
    }
    // Artist opens on a true-black surface. Reuse only the existing cover,
    // avoiding backdrop bitmap decoding and blur work during this animation.
    var artistTransitionCover by remember(currentSong?.uri) {
        mutableStateOf(currentSong?.let { song ->
            ImmersiveArtworkCache.cached(song)?.cover ?: ArtworkLoader.cached(
                song.uri, 1_600, song.albumId.takeIf { it > 0L }?.toString()
            )
        })
    }
    // Already computed and cached with each track; never resample the
    // bitmap on the UI thread just to recolor the navigation glass.
    var playerGlassArtworkColor by remember {
        mutableStateOf(
            currentSong?.let { ImmersiveArtworkCache.cached(it)?.backdrop }?.let { Color(it) }
                ?: Color(0xFF141414)
        )
    }
    LaunchedEffect(currentSong?.uri) {
        currentSong?.let { ImmersiveArtworkCache.cached(it)?.backdrop }?.let {
            playerGlassArtworkColor = Color(it)
        }
    }
    // Color belongs to the current track, not the selected app theme.
    // Reuse cached art where possible; extract only when Vinyl is active.
    var vinylAccentColor by remember(currentSong?.uri) {
        mutableStateOf(Color(0xFFAAB0B8))
    }
    val uiPrefs = remember(context) {
        context.getSharedPreferences("ui_preferences", android.content.Context.MODE_PRIVATE)
    }
    var playerScale by remember { mutableFloatStateOf(PlayerScalePreference.read(uiPrefs)) }
    var titleFontSize by remember {
        mutableStateOf(uiPrefs.getFloat("title_font_size", 34f))
    }
    var lyricsFontSize by remember {
        mutableFloatStateOf(uiPrefs.getFloat("lyrics_font_size", 32f).let {
            if (it.isFinite()) it.coerceIn(18f, 48f) else 32f
        })
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
    LaunchedEffect(currentSong?.uri, playerPresentationMode) {
        val active = currentSong ?: return@LaunchedEffect
        if (playerPresentationMode != PlayerPresentationMode.VINYL) return@LaunchedEffect
        val cacheKey = active.albumId.takeIf { it > 0L }?.toString()
        val image = ArtworkLoader.cached(
            uriString = active.uri, maxSize = 256, cacheKey = cacheKey
        ) ?: ArtworkLoader.load(
            context = context, uriString = active.uri, maxSize = 256, cacheKey = cacheKey
        )
        if (image != null) {
            vinylAccentColor = withContext(kotlinx.coroutines.Dispatchers.Default) {
                vinylAccentFromArtwork(image)
            }
        }
    }
    // Cover the brief startup window before Media3 restores its queue.
    // The queue lookahead also includes this Artist; the loader shares cache
    // and de-duplicates requests when both paths are active.
    LaunchedEffect(artistKey, playerPresentationMode) {
        if (playerPresentationMode != PlayerPresentationMode.IMMERSIVE_ARTWORK) {
            return@LaunchedEffect
        }
        val name = ArtistNameUtils.firstArtist(currentSong?.artist)
        if (name.isBlank()) return@LaunchedEffect
        delay(380L)
        AppleArtistPortraitLoader.warm(context, name)
    }
    // This option themes ONLY the Immersive song library and its search.
    // Reuse the existing preference on upgrade so the chosen color persists.
    var libraryLightMode by remember {
        mutableStateOf(
            uiPrefs.getBoolean("immersive_library_light_mode",
                uiPrefs.getBoolean("immersive_light_mode", false))
        )
    }
    // Retain the saved rendering choice after removing its settings row.
    val liquidGlassEnabled = remember {
        uiPrefs.getBoolean("immersive_liquid_glass_enabled", true)
    }
    var animatedArtworkEnabled by remember {
        mutableStateOf(uiPrefs.getBoolean("animated_artwork_enabled", true))
    }
    var dynamicGlowEnabled by remember {
        mutableStateOf(com.yohandeku32.nusamusic.ui.DynamicGlowPreference.read(uiPrefs))
    }
    var artworkSourceOrder by remember {
        mutableStateOf(ArtworkSourceOrder.fromPreference(
            uiPrefs.getString("animated_artwork_source_order", null)
        ))
    }
    var roundedCarouselCorners by remember {
        mutableStateOf(uiPrefs.getBoolean("rounded_carousel_corners", true))
    }
    var lyricsRequested by remember(playerPresentationMode) { mutableStateOf(false) }
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

    // Artist is a rising in-player overlay; only Now Playing and Library
    // participate in horizontal pager gestures.
    val artistPageEnabled = playerPresentationMode == PlayerPresentationMode.IMMERSIVE_ARTWORK
    val playerPage = 0
    val libraryPage = 1
    var libraryWasVisible by remember { mutableStateOf(startInLibrary) }
    val pagerState = androidx.compose.runtime.key(artistPageEnabled) {
        androidx.compose.foundation.pager.rememberPagerState(
            initialPage = if (!artistPageEnabled && libraryWasVisible)
                libraryPage else playerPage,
            pageCount = { if (artistPageEnabled) 1 else 2 }
        )
    }
    LaunchedEffect(playerPresentationMode) {
        if (!artistPageEnabled) {
            artistOverlayOpen = false
            libraryOverlayOpen = false
        }
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
    fun openDiscoveryArtist(name: String, albumKey: String? = null) {
        val key = ArtistNameUtils.cacheKey(name)
        val state = artistPageStates.get(key) ?: ImmersiveArtistPageState(name).also {
            artistPageStates.put(key, it)
        }
        state.selectedAlbumKey = albumKey
        discoveryArtistName = name
        scope.launch { state.listState.scrollToItem(0) }
    }
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
    LaunchedEffect(songs, playerPresentationMode, animatedArtworkEnabled, artworkSourceOrder) {
        (context as? MainActivity)?.refreshArtworkLookahead()
    }

    LaunchedEffect(pagerState.settledPage, lyricsRequested, playerPage) {
        libraryWasVisible = pagerState.settledPage == libraryPage
        onPlaybackPageVisibilityChanged(
            pagerState.settledPage == playerPage,
            artistPageEnabled && pagerState.settledPage == playerPage && lyricsRequested)
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

    deleteFromLibrary?.let { candidate ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { deleteFromLibrary = null },
            title = {
                Text(nusaText("Hapus lagu permanen?", "Delete song permanently?"))
            },
            text = {
                Text(nusaText(
                    "“${candidate.title}” akan dihapus dari penyimpanan perangkat, bukan hanya dari daftar lagu. Tindakan ini tidak dapat dibatalkan.",
                    "“${candidate.title}” will be deleted from device storage, not just the song list. This cannot be undone."
                ))
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    deleteFromLibrary = null
                    onDeleteSong(candidate)
                }) {
                    Text(nusaText("Hapus permanen", "Delete permanently"), color = Color(0xFFD54A4A))
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { deleteFromLibrary = null }) {
                    Text(nusaText("Batal", "Cancel"))
                }
            }
        )
    }

    if (showSettings) {
        val settingsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        var settingsClosing by remember { mutableStateOf(false) }
        val closeSettings: () -> Unit = {
            if (!settingsClosing) {
                settingsClosing = true
                scope.launch {
                    try {
                        // Dismiss with the sheet's own downward animation,
                        // including when pulled from the top of the list.
                        settingsSheetState.hide()
                    } finally {
                        showSettings = false
                    }
                }
            }
        }

        ModalBottomSheet(
            onDismissRequest = closeSettings,
            sheetState = settingsSheetState,
            // Only the dedicated header and top-of-list overscroll trigger
            // dismissal. Outer sheet dragging stays disabled so scrolling
            // inside Settings cannot make the entire panel jitter.
            sheetGesturesEnabled = false,
            dragHandle = null,
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.onBackground
        ) {
            // ModalBottomSheet owns a separate dialog window. Configure its
            // fullscreen flags ONCE when that window is attached, instead of
            // writing WindowInsets/systemUiVisibility every 100 ms during
            // scroll. Repeated writes can invalidate dialog measurements and
            // make the entire settings sheet appear to shake.
            val sheetView = androidx.compose.ui.platform.LocalView.current
            val sheetWindow = (sheetView.parent as? androidx.compose.ui.window.DialogWindowProvider)?.window
            androidx.compose.runtime.DisposableEffect(sheetWindow) {
                sheetWindow?.let { dialogWindow ->
                    dialogWindow.addFlags(android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN)
                    WindowCompat.setDecorFitsSystemWindows(dialogWindow, false)
                    androidx.core.view.WindowInsetsControllerCompat(
                        dialogWindow,
                        dialogWindow.decorView
                    ).hide(androidx.core.view.WindowInsetsCompat.Type.statusBars())
                }
                onDispose { /* Dialog window is destroyed on dismiss. */ }
            }

            PlayerSettingsContent(
                lyricsFontSize = lyricsFontSize,
                onLyricsFontSizeChange = { lyricsFontSize = it },
                onLyricsFontSizeChangeFinished = {
                    uiPrefs.edit().putFloat("lyrics_font_size", lyricsFontSize).apply()
                },
                hasLyricsSong = currentSong != null,
                lyricsLoading = LYRICS_ENABLED && currentSong?.let { lyricsState.forSong(it).loading } == true,
                onRetryLyrics = {
                    currentSong?.let {
                        lyricsState.load(it, scope, retry = true, onlineEnabled = onlineLyricsEnabled)
                    }
                },
                onlineLyricsEnabled = onlineLyricsEnabled,
                onOnlineLyricsChange = { enabled ->
                    if (onlineLyricsEnabled != enabled) {
                        onlineLyricsEnabled = enabled
                        uiPrefs.edit().putBoolean("online_lyrics_enabled", enabled).apply()
                        currentSong?.let {
                            lyricsState.load(it, scope, retry = true, onlineEnabled = enabled)
                        }
                    }
                },
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
                playerScale = playerScale,
                onPlayerScaleChange = { value ->
                    val normalized = PlayerScalePreference.normalize(value)
                    if (playerScale != normalized) {
                        playerScale = normalized
                        PlayerScalePreference.write(uiPrefs, normalized)
                    }
                },
                libraryLightMode = libraryLightMode,
                onLibraryLightModeChange = { light ->
                    libraryLightMode = light
                    uiPrefs.edit()
                        .putBoolean("immersive_library_light_mode", light)
                        .apply()
                },
                onPresentationSelected = { mode ->
                    if (mode.isAvailable) {
                        playerPresentationMode = mode
                        uiPrefs.edit().putString("player_presentation_mode", mode.preferenceValue).apply()
                    }
                },
                animatedArtworkEnabled = animatedArtworkEnabled,
                dynamicGlowEnabled = dynamicGlowEnabled,
                onDynamicGlowChange = { enabled ->
                    dynamicGlowEnabled = enabled
                    com.yohandeku32.nusamusic.ui.DynamicGlowPreference.write(uiPrefs, enabled)
                },
                roundedCarouselCorners = roundedCarouselCorners,
                onRoundedCarouselCornersChange = { rounded ->
                    roundedCarouselCorners = rounded
                    uiPrefs.edit().putBoolean("rounded_carousel_corners", rounded).apply()
                },
                onAnimatedArtworkChange = { enabled ->
                    animatedArtworkEnabled = enabled
                    uiPrefs.edit().putBoolean("animated_artwork_enabled", enabled).apply()
                },
                artworkSourceOrder = artworkSourceOrder.name,
                onArtworkSourceOrderChange = { preference ->
                    val chosen = ArtworkSourceOrder.fromPreference(preference)
                    if (chosen != artworkSourceOrder) {
                        artworkSourceOrder = chosen
                        uiPrefs.edit()
                            .putString("animated_artwork_source_order", chosen.name)
                            .apply()
                        (context as? MainActivity)?.refreshArtworkLookahead(reset = true)
                    }
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
                onDismiss = closeSettings
            )
        }
    }

    val pageBackdrop = when (playerPresentationMode) {
        PlayerPresentationMode.CD -> Color(0xFF111111)
        PlayerPresentationMode.IMMERSIVE_ARTWORK -> Color.Black
        else -> MaterialTheme.colorScheme.background
    }
    // Lyrics is an in-page focus mode, no modal dialog or slide-up sheet.
    // Back closes the focus mode first and keeps the Now Playing page intact.
    androidx.activity.compose.BackHandler(
        enabled = lyricsRequested && pagerState.settledPage == playerPage && !showSettings
    ) { lyricsRequested = false }
    // The Library is now an in-player overlay rather than a pager destination.
    // Physical Android Back must dismiss it even when its search isn't active.
    androidx.activity.compose.BackHandler(
        enabled = libraryOverlayOpen && !showSettings
    ) { libraryOverlayOpen = false }
    LaunchedEffect(pagerState.settledPage) {
        if (pagerState.settledPage != playerPage) lyricsRequested = false
    }
    LaunchedEffect(pagerState.settledPage) {
        if (pagerState.settledPage != playerPage) {
            artistOverlayOpen = false
            libraryOverlayOpen = false
        }
    }

    val keyboardForPager = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    val focusForPager = androidx.compose.ui.platform.LocalFocusManager.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    // The Activity window can hide its keyboard while Compose still remembers
    // a focused BasicTextField. Clear Compose focus on both backgrounding and
    // return; otherwise Android reopens Gboard on top of the main player.
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_PAUSE,
                androidx.lifecycle.Lifecycle.Event.ON_STOP,
                androidx.lifecycle.Lifecycle.Event.ON_RESUME -> {
                    keyboardForPager?.hide()
                    focusForPager.clearFocus(force = true)
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    // Off-screen pages remain composed for swiping. Explicitly clear their
    // text focus so the IME cannot follow a reverse swipe to Now Playing.
    LaunchedEffect(pagerState.settledPage) {
        if (pagerState.settledPage != libraryPage) {
            keyboardForPager?.hide()
            focusForPager.clearFocus(force = true)
        }
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
                modifier = Modifier.fillMaxSize(),
                userScrollEnabled = !artistPageEnabled &&
                    !artistOverlayOpen && !libraryOverlayOpen,
                // The library has its own surface, but NO artificial backing
                // patch. The previous overlapping page now supplies the real
                // corner pixels while swiping in either direction.
                pageSurface = { page ->
                    if (artistPageEnabled && page == libraryPage) {
                        immersiveColors(libraryLightMode).background
                    } else pageBackdrop
                },
                onHorizontalSwipeStarted = {
                    // Avoid running expensive global focus/IME callbacks on
                    // player <-> artist gestures where no keyboard is open.
                    if (pagerState.settledPage == libraryPage) {
                        keyboardForPager?.hide()
                        focusForPager.clearFocus(force = true)
                    }
                }
            ) { page ->
                when {
                    page == playerPage -> {
                        BoxWithConstraints(Modifier.fillMaxSize()) {
                        val playerViewportHeight = maxHeight
                        if (playerPresentationMode == PlayerPresentationMode.IMMERSIVE_ARTWORK) {
                            PlayerScaleHost(playerScale) {
                                val lyricCredits = currentSong?.let { lyricsState.forSong(it).data?.ttmlCredits }
                                ImmersiveArtworkPlayer(
                                    title = currentSong?.title ?: nusaText("Pilih lagu", "Choose a song"),
                                    composerNames = lyricCredits?.let { it.songwriters.ifEmpty { it.composers }.joinToString(", ") }.orEmpty(),
                                    artist = currentSong?.artist ?: nusaText("Perpustakaan musik lokal Anda", "Your local music library"),
                                    titleFontFamily = if (uiPrefs.contains("title_font_family")) titleFontFamily else FontFamily.SansSerif,
                                    titleFontSize = titleFontSize,
                                    hasSong = currentSong != null,
                                    isPlaying = isPlaying,
                                    isFavorite = isFavorite,
                                    shuffleEnabled = shuffleEnabled,
                                    repeatMode = repeatMode,
                                    lyricsOpen = lyricsRequested,
                                    lyricsEnabled = LYRICS_ENABLED,
                                    onPlayPause = if (currentSong == null) onRequestPermission else onTogglePlay,
                                    onPrevious = onPrevious, onNext = onNext,
                                    onShuffle = onToggleShuffle, onRepeat = onToggleRepeat,
                                    onFavorite = { isFavorite = !isFavorite },
                                    onLyrics = {
                                        artistOverlayOpen = false
                                        libraryOverlayOpen = false
                                        lyricsRequested = !lyricsRequested
                                    },
                                    onDiscovery = {
                                        if (!artistOverlayOpen || libraryOverlayOpen) {
                                            lyricsRequested = false
                                            discoveryArtistName = null
                                            libraryOverlayOpen = false
                                            artistOverlayOpen = true
                                        }
                                    },
                                    discoveryOpen = artistOverlayOpen,
                                    canCloseDiscoveryBySwipe = {
                                        val state = if (discoveryArtistName == null) discoveryListState
                                            else artistPageState.listState
                                        state.firstVisibleItemIndex == 0 && state.firstVisibleItemScrollOffset < 12
                                    },
                                    onDiscoveryClose = { artistOverlayOpen = false },
                                    discoveryContent = { settled ->
                                        val selectedArtist = discoveryArtistName
                                        if (selectedArtist == null) {
                                            ImmersiveDiscoveryPage(
                                                songs = songs, currentSong = currentSong, isPlaying = isPlaying,
                                                listState = discoveryListState, isVisible = artistOverlayOpen,
                                                isSettled = settled,
                                                onBack = { artistOverlayOpen = false },
                                                onArtist = { openDiscoveryArtist(it) },
                                                onAlbum = { openDiscoveryArtist(it.artist, it.albumKey) },
                                                onPlay = { selected, queue ->
                                                    artistOverlayOpen = false
                                                    onPlay(selected, queue)
                                                },
                                                onTogglePlay = onTogglePlay,
                                                onLibrary = {
                                                    artistOverlayOpen = false
                                                    libraryOverlayOpen = true
                                                },
                                                artwork = { track, tile ->
                                                    ArtworkView(track, maxSizePx = 320, modifier = tile,
                                                        loadAllowed = settled, grainOverlay = false)
                                                }
                                            )
                                        } else {
                                        ImmersiveArtistPage(
                                            artistName = selectedArtist,
                                            songs = songs,
                                            currentSong = currentSong,
                                            isVisible = artistOverlayOpen,
                                            state = artistPageState,
                                            onBack = { discoveryArtistName = null },
                                            onPlay = { selected, queue ->
                                                artistOverlayOpen = false
                                                onPlay(selected, queue)
                                            },
                                            artwork = { song, modifier ->
                                                ArtworkView(song, maxSizePx = 512, modifier = modifier,
                                                    grainOverlay = false)
                                            },
                                            heroArtwork = { song, modifier ->
                                                val cover = artistTransitionCover
                                                if (song.uri == currentSong?.uri && cover != null) {
                                                    Image(bitmap = cover.asImageBitmap(),
                                                        contentDescription = song.title,
                                                        contentScale = ContentScale.Crop,
                                                        modifier = modifier)
                                                } else {
                                                    ArtworkView(song, maxSizePx = 512,
                                                        modifier = modifier, grainOverlay = false)
                                                }
                                            },
                                            overlayMode = true
                                        )
                                        }
                                    },
                                    libraryOpen = libraryOverlayOpen,
                                    libraryLightMode = libraryLightMode,
                                    liquidGlassEnabled = liquidGlassEnabled,
                                    glassArtworkColor = playerGlassArtworkColor,
                                    onLibraryClose = { libraryOverlayOpen = false },
                                    onHome = {
                                        artistOverlayOpen = false
                                        libraryOverlayOpen = false
                                    },
                                    libraryContent = { settled, bottomContentPadding ->
                                        ImmersiveMusicLibrary(
                                            songs = songs,
                                            currentSong = currentSong,
                                            isPlaying = isPlaying,
                                            isVisible = libraryOverlayOpen && settled,
                                            bottomContentPadding = bottomContentPadding,
                                            lightMode = libraryLightMode,
                                            hasMusicAccess = hasMusicAccess,
                                            isScanning = isScanningMusic,
                                            emptyMessage = emptyLibraryMessage,
                                            onReturnToPlayer = { libraryOverlayOpen = false },
                                            onPlay = { selected, queue ->
                                                libraryOverlayOpen = false
                                                onPlay(selected, queue)
                                            },
                                            onPlayNext = onPlayNext,
                                            onDeleteSong = onDeleteSong,
                                            onShare = { onShare(it) },
                                            onTogglePlay = onTogglePlay,
                                            onScanMusic = onScanMusic,
                                            onRequestPermission = onRequestPermission,
                                            onChooseMusicFolder = onSelectMusicFolder,
                                            artwork = { track, tileModifier, settled, index ->
                                                ArtworkView(
                                                    song = track,
                                                    maxSizePx = 144,
                                                    // Cached album thumbnails are available
                                                    // instantly; defer misses until the
                                                    // Library entrance finishes. Stagger
                                                    // each visible row to avoid frame spikes.
                                                    loadAllowed = settled,
                                                    loadDelayMs = 40L +
                                                        (index.coerceIn(0, 12) * 28L),
                                                    grainOverlay = false,
                                                    modifier = tileModifier
                                                )
                                            }
                                        )
                                    },
                                    onLibrary = {
                                        artistOverlayOpen = false
                                        lyricsRequested = false
                                        libraryOverlayOpen = true
                                    },
                                    onShare = { onShare(currentSong) },
                                    onSettings = {
                                        artistOverlayOpen = false
                                        libraryOverlayOpen = false
                                        showSettings = true
                                    },
                                    artwork = { artworkModifier, heroHeight, heroWidth ->
                                        ImmersiveArtwork(
                                            currentSong, animatedArtworkEnabled, heroHeight, heroWidth,
                                            artworkModifier, lyricsMode = lyricsRequested,
                                            sourceOrder = artworkSourceOrder,
                                            dynamicGlowEnabled = dynamicGlowEnabled,
                                            playerPageVisible = pagerState.settledPage == playerPage &&
                                                !artistOverlayOpen && !libraryOverlayOpen &&
                                                !immersiveVideoSuspended,
                                            pageReveal = {
                                                // Renderer reads the changing pager
                                                // offset only inside graphicsLayer.
                                                // Fade live TextureView early on BOTH
                                                // swipe directions; the existing cover
                                                // stays warm beneath it.
                                                kotlin.math.abs(
                                                    playerPage - pagerState.currentPage -
                                                        pagerState.currentPageOffsetFraction
                                                ).coerceIn(0f, 1f)
                                            },
                                            onCoverReady = { uri, cover ->
                                                if (uri == currentSong?.uri) artistTransitionCover = cover
                                            },
                                            onPaletteReady = { uri, palette ->
                                                if (uri == currentSong?.uri) playerGlassArtworkColor = palette
                                            }
                                        )
                                    },
                                    progress = { progressModifier ->
                                        PlaybackProgress(
                                            positionMsState,
                                            durationMs.takeIf { it > 0L } ?: currentSong?.durationMs ?: 0L,
                                            currentSong != null,
                                            onSeek, immersive = true, modifier = progressModifier
                                        )
                                    },
                                    quality = { AudioQualityPill(song = currentSong, immersive = true) },
                                    lyrics = { lyricsModifier ->
                                        ImmersiveLyricsPanel(
                                            song = currentSong,
                                            positionMs = positionMsState,
                                            readPlaybackPosition = readPlaybackPosition,
                                            readPlaybackEpoch = readPlaybackEpoch,
                                            isPlaying = isPlaying,
                                            playbackEpoch = lyricsPlaybackEpoch,
                                            durationMs = durationMs,
                                            state = lyricsState,
                                            onSeek = onSeek,
                                            fontSize = lyricsFontSize,
                                            onlineLyricsEnabled = onlineLyricsEnabled,
                                            modifier = lyricsModifier,
                                            onQueueLyrics = onQueueLyrics,
                                            isLyricsQueued = isLyricsQueued
                                        )
                                    },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            return@BoxWithConstraints
                        }
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
                                        .heightIn(min = playerViewportHeight)
                                        .clip(RoundedCornerShape(bottomStart = 34.dp, bottomEnd = 34.dp))
                                        .background(MaterialTheme.colorScheme.background)
                                ) {
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
                                            accentColor = vinylAccentColor,
                                            sleeve = { sleeveModifier ->
                                                ArtworkView(
                                                    song = currentSong,
                                                    maxSizePx = 512,
                                                    modifier = sleeveModifier,
                                                    grainOverlay = false
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
                                    }
                                }
                            }

                        }
                        }
                    }

                    page == libraryPage -> {
                        if (playerPresentationMode == PlayerPresentationMode.IMMERSIVE_ARTWORK) {
                            ImmersiveMusicLibrary(
                                songs = songs,
                                currentSong = currentSong,
                                isPlaying = isPlaying,
                                isVisible = pagerState.settledPage == libraryPage &&
                                    !pagerState.isScrollInProgress,
                                lightMode = libraryLightMode,
                                hasMusicAccess = hasMusicAccess,
                                isScanning = isScanningMusic,
                                emptyMessage = emptyLibraryMessage,
                                onReturnToPlayer = {
                                    scope.launch { pagerState.animateMiuixScrollToPage(playerPage) }
                                },
                                onPlay = onPlay,
                                onPlayNext = onPlayNext,
                                onDeleteSong = onDeleteSong,
                                onShare = { onShare(it) },
                                onTogglePlay = onTogglePlay,
                                onScanMusic = onScanMusic,
                                onRequestPermission = onRequestPermission,
                                onChooseMusicFolder = onSelectMusicFolder,
                                artwork = { track, tileModifier, settled, index ->
                                    ArtworkView(
                                        song = track,
                                        maxSizePx = 144,
                                        loadAllowed = settled,
                                        loadDelayMs = 40L +
                                            (index.coerceIn(0, 12) * 28L),
                                        grainOverlay = false,
                                        modifier = tileModifier
                                    )
                                }
                            )
                        } else {
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
                                        onRequestDelete = { deleteFromLibrary = it },
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
                                        onRequestDelete = { deleteFromLibrary = it },
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
                                            },
                                            onRequestDelete = { deleteFromLibrary = it }
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
                                                    "Tambahkan musik untuk mulai mendengarkan.",
                                                    "Add music to start listening."
                                                ),
                                                color = Color(0xFF9D9D9D),
                                                fontSize = 14.sp
                                            )
                                            Spacer(Modifier.height(12.dp))
                                            com.yohandeku32.nusamusic.ui.MusicImportActions(
                                                onChooseFolder = onSelectMusicFolder,
                                                onDeviceMusic = onRequestPermission,
                                                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
                                            )
                                        }
                                    }
                                } else {
                                    item {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 80.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                emptyLibraryMessage,
                                                color = Color(0xFF9D9D9D),
                                                fontSize = 14.sp
                                            )
                                            if (!isScanningMusic && songs.isEmpty()) TextButton(onClick = onSelectMusicFolder) {
                                                Text(nusaText("Pilih folder musik", "Choose a music folder"))
                                            }
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
                        } // Legacy CD/Vinyl library
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

private enum class AudioQualityBadge {
    LOSSLESS,
    HI_RES,
    DOLBY
}

@Composable
private fun AudioQualityPill(
    song: Song?,
    immersive: Boolean = false,
    immersiveLightMode: Boolean = false,
    onBadgeVisibilityChanged: ((Boolean) -> Unit)? = null
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    // Use already verified codec data immediately when a row/player exists
    // instead of waiting one extra LaunchedEffect frame to reveal Hi-Res.
    val initialCodec = song?.uri?.let(AudioCodecLoader::cached)
    var codecInfo by remember(song?.uri) { mutableStateOf(initialCodec) }
    var resolvedCodecUri by remember(song?.uri) {
        mutableStateOf(if (initialCodec != null) song?.uri else null)
    }
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
                        .then(
                            // Preserve the original compact rounded rectangle;
                            // only its translucent glass material matches the
                            // bottom controls, not their capsule silhouette.
                            if (immersive) Modifier.immersiveGlassPill(7.dp, immersiveLightMode)
                            else Modifier
                                .clip(RoundedCornerShape(7.dp))
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                                )
                        )
                        .clickable(enabled = metadataMatchesSong && info != null) {
                            showMetadataDialog = true
                        }
                        .padding(horizontal = 9.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (hiRes) "Hi-Res" else "Lossless",
                        fontSize = if (immersive) 11.sp else 10.sp,
                        lineHeight = if (immersive) 13.sp else 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (immersive) {
                            if (immersiveLightMode) Color(0xFF171A20) else Color.White
                        } else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            AudioQualityBadge.DOLBY -> {
                Box(
                    modifier = Modifier
                        .padding(top = if (immersive) 0.dp else 4.dp)
                        .then(
                            if (immersive) Modifier.immersiveGlassPill(100.dp, immersiveLightMode)
                            else Modifier
                                .clip(RoundedCornerShape(50))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        )
                        .clickable(enabled = metadataMatchesSong && info != null) {
                            showMetadataDialog = true
                        }
                        .padding(horizontal = if (immersive) 9.dp else 8.dp, vertical = if (immersive) 4.dp else 3.dp),
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
                        fontSize = if (immersive) 11.sp else 10.sp,
                        lineHeight = if (immersive) 12.sp else 11.sp,
                        letterSpacing = (-0.35).sp,
                        maxLines = 1,
                        color = if (immersive) {
                            if (immersiveLightMode) Color(0xFF171A20) else Color.White
                        } else MaterialTheme.colorScheme.onSurface
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
    // Local art is available immediately; never start a Deezer request for a
    // small control that needs to respond at 90/120Hz.
    ArtworkView(
        song = song,
        maxSizePx = 128,
        modifier = modifier,
        monochrome = true,
        grainOverlay = false
    )
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
    immersiveLightMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    val fraction = if (durationMs > 0L) {
        (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    val primary = if (immersive) {
        if (immersiveLightMode) Color(0xFF20242D) else Color.White
    } else MaterialTheme.colorScheme.primary

    androidx.compose.foundation.Canvas(
        modifier = modifier
            .height(if (immersive) 36.dp else 28.dp)
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
        val stroke = (if (immersive) 4.25.dp else 5.dp).toPx()
        val startX = stroke / 2f
        val endX = (size.width - startX).coerceAtLeast(startX)

        drawLine(
            color = if (immersive) {
                if (immersiveLightMode) Color.Black.copy(alpha = 0.19f)
                else Color.White.copy(alpha = 0.22f)
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
    immersiveLightMode: Boolean = false,
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
            immersiveLightMode = immersiveLightMode,
            modifier = Modifier.fillMaxWidth()
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val textColor = if (immersive) {
                if (immersiveLightMode) Color(0xFF30343A).copy(alpha = 0.77f)
                else Color.White.copy(alpha = 0.75f)
            } else MaterialTheme.colorScheme.onBackground
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


/**
 * Select a readable, saturated-but-not-neon accent from the current album art.
 * The small sample and HSV conversion run off the UI thread once per track.
 * Neutral covers fall back to brushed-silver rather than a fixed red theme.
 */
private fun vinylAccentFromArtwork(bitmap: Bitmap): Color {
    val hsv = FloatArray(3)
    val weights = FloatArray(12)
    for (row in 0 until 8) {
        for (column in 0 until 8) {
            val x = ((column + 0.5f) / 8f * bitmap.width).toInt()
                .coerceIn(0, bitmap.width - 1)
            val y = ((row + 0.5f) / 8f * bitmap.height).toInt()
                .coerceIn(0, bitmap.height - 1)
            android.graphics.Color.colorToHSV(bitmap.getPixel(x, y), hsv)
            val saturation = hsv[1]
            val value = hsv[2]
            if (saturation < 0.22f || value < 0.16f || value > 0.97f) continue
            val bucket = ((hsv[0] / 30f).toInt()).coerceIn(0, weights.lastIndex)
            weights[bucket] += saturation * value
        }
    }
    val winningIndex = weights.indices.maxByOrNull { weights[it] } ?: 0
    if (weights[winningIndex] < 0.50f) return Color(0xFFAAB0B8)
    val hue = winningIndex * 30f + 15f
    return Color(
        android.graphics.Color.HSVToColor(
            floatArrayOf(hue, 0.62f, 0.88f)
        )
    )
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
    // Bright covers need neutral shading behind white controls now that the
    // tonal glaze is translucent. Dark artwork keeps its colour and contrast.
    return (0.06f + brightness * 0.50f).coerceIn(0.08f, 0.56f)
}

private enum class AnimatedArtworkProvider { APPLE_MUSIC, SPOTIFY_CANVAS }

@Composable
private fun ImmersiveArtwork(
    song: Song?,
    animatedArtworkEnabled: Boolean = true,
    heroHeight: androidx.compose.ui.unit.Dp,
    heroWidthFraction: Float = 1f,
    modifier: Modifier = Modifier,
    lyricsMode: Boolean = false,
    lightMode: Boolean = false,
    sourceOrder: ArtworkSourceOrder = ArtworkSourceOrder.APPLE_FIRST,
    playerPageVisible: Boolean = true,
    pageReveal: () -> Float = { 0f },
    onCoverReady: (String, Bitmap) -> Unit = { _, _ -> },
    onPaletteReady: (String, Color) -> Unit = { _, _ -> },
    dynamicGlowEnabled: Boolean = false
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    // Open the persistent MP4 cache off the UI thread while Apple is searched.
    // Reuses one cache for all Canvas players; no disk work on a draw frame.
    LaunchedEffect(animatedArtworkEnabled) {
        if (animatedArtworkEnabled) {
            // Both caches share Android's database provider. Initialize them
            // sequentially on IO (inside prepare) to avoid startup DB lock
            // contention while the artwork screen is first composed.
            AppleTallVideoCache.prepare(context)
            SpotifyCanvasVideoCache.prepare(context)
        }
    }
    // Retain the current frame while the next song's artwork is decoded. Resetting
    // these states on every URI change briefly exposed the empty placeholder.
    val initialSnapshot = remember { song?.let { ImmersiveArtworkCache.cached(it) } }
    val initialCover = remember {
        initialSnapshot?.cover ?: song?.let {
            ArtworkLoader.cached(it.uri, 1_600, it.albumId.takeIf { id -> id > 0L }?.toString())
        }
    }
    var artwork by remember { mutableStateOf(initialCover) }
    var artworkSongUri by remember {
        mutableStateOf(song?.uri.takeIf { initialCover != null })
    }
    val latestCoverReady by rememberUpdatedState(onCoverReady)
    LaunchedEffect(artwork, artworkSongUri) {
        val cover = artwork
        val uri = artworkSongUri
        if (cover != null && uri != null) latestCoverReady(uri, cover)
    }
    var staticBlur by remember { mutableStateOf(initialSnapshot?.blur) }
    // Do not swap to the plain cover or a black placeholder between Next
    // presses. Hold whichever real ambient was last visible until the next
    // cover / animated frame has finished preparing its own blur.
    var retainedAmbientBlur by remember { mutableStateOf(initialSnapshot?.blur) }
    var backdrop by remember { mutableStateOf(initialSnapshot?.let { Color(it.backdrop) } ?: Color(0xFF141414)) }
    var hasPresentedArtwork by remember { mutableStateOf(initialCover != null) }
    LaunchedEffect(artwork) {
        if (artwork != null && !hasPresentedArtwork) {
            androidx.compose.runtime.withFrameNanos { }
            hasPresentedArtwork = true
        }
    }
    var animatedArtworkUrl by remember(song?.id, song?.uri) { mutableStateOf<String?>(null) }
    var artworkProvider by remember(song?.uri) { mutableStateOf(AnimatedArtworkProvider.APPLE_MUSIC) }
    var spotifyFallbackRequested by remember(song?.uri) { mutableStateOf(false) }
    var spotifyPlaybackFailed by remember(song?.uri) { mutableStateOf(false) }
    var animatedFirstFrameUrl by remember(song?.uri) { mutableStateOf<String?>(null) }
    // Freeze the first usable animated frame's colours for the entire song,
    // including when an unavailable stream switches to an alternative URL.
    var frozenAnimatedBlur by remember(song?.uri) { mutableStateOf<Bitmap?>(null) }
    var frozenAnimatedBackdrop by remember(song?.uri) { mutableStateOf<Color?>(null) }
    var dynamicGlow by remember(song?.uri, animatedArtworkUrl) { mutableStateOf<Bitmap?>(null) }
    val durationScale = rememberCoroutineScope().coroutineContext[androidx.compose.ui.MotionDurationScale]
    val lifecycleState by androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    val glowActive = dynamicGlowEnabled && animatedArtworkEnabled && !lyricsMode && playerPageVisible &&
        lifecycleState.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED) &&
        (durationScale?.scaleFactor ?: 1f) > 0f
    LaunchedEffect(glowActive) { if (!glowActive) dynamicGlow = null }

    LaunchedEffect(song?.uri, song?.albumId) {
        val targetSong = song
        if (targetSong == null) {
            // Keep the previous artwork through transient Media3 null items.
            // Clearing it exposes the page background for one frame.
            return@LaunchedEffect
        }

        val albumCacheKey = targetSong.albumId
            .takeIf { albumId -> albumId > 0L }
            ?.toString()

        suspend fun publish(bitmap: Bitmap?) {
            // Never replace a visible image with null during a slow lookup.
            if (bitmap == null) return
            val prepared = prepareImmersiveArtwork(bitmap)
            // Publish the actual cover + its blur in the same snapshot.
            artwork = bitmap
            artworkSongUri = targetSong.uri
            staticBlur = prepared.first
            retainedAmbientBlur = prepared.first
            backdrop = prepared.second
            ImmersiveArtworkCache.save(context, targetSong,
                ImmersiveArtworkSnapshot(bitmap, prepared.first, prepared.second.toArgb()))
        }

        // Restore the small prepared preview before reopening the audio file.
        // It includes the blur and palette, so the first cover needs no extra preparation.
        val restored = ImmersiveArtworkCache.restore(context, targetSong)
        if (restored != null) {
            artwork = restored.cover
            artworkSongUri = targetSong.uri
            staticBlur = restored.blur
            retainedAmbientBlur = restored.blur
            backdrop = Color(restored.backdrop)
        }

        val cachedFullArtwork = ArtworkLoader.cached(
            uriString = targetSong.uri,
            maxSize = 1_600,
            cacheKey = albumCacheKey,
            allowSmallerPreview = false
        )
        if (cachedFullArtwork != null) {
            if (restored != null) {
                // The restored blur is already the correct album. Avoid
                // regenerating it and restarting a full-screen crossfade on
                // every Next -> Previous revisit.
                artwork = cachedFullArtwork
                artworkSongUri = targetSong.uri
            } else if (cachedFullArtwork !== artwork || staticBlur == null) {
                publish(cachedFullArtwork)
            }
            return@LaunchedEffect
        }

        // Show the ready thumbnail immediately, but do NOT start another
        // full-screen blur transition for it. The blur retains its previous
        // frame until the new full cover is prepared. Avoid two successive
        // fullscreen fades (preview -> full -> video), which caused flashes
        // during consecutive Next presses on 90/120Hz devices.
        val cachedPreview = ArtworkLoader.cached(
            uriString = targetSong.uri,
            maxSize = 512,
            cacheKey = albumCacheKey
        )
        if (restored == null && cachedPreview != null && cachedPreview !== artwork) {
            artwork = cachedPreview
            artworkSongUri = targetSong.uri
        }

        val loaded = ArtworkLoader.load(
            context = context,
            uriString = targetSong.uri,
            maxSize = 1_600,
            cacheKey = albumCacheKey
        )
        if (loaded != null && restored == null) {
            publish(loaded)
        } else if (loaded != null && loaded !== artwork) {
            // Existing disk-restored ambient is valid for this track; the
            // higher-resolution hero can change without a second blur fade.
            artwork = loaded
            artworkSongUri = targetSong.uri
        } else if (loaded == null && restored == null && cachedPreview != null) {
            // Full-size extraction can fail for some formats; settle on the
            // thumbnail's blur rather than keeping another song's ambience.
            publish(cachedPreview)
        }
    }

    LaunchedEffect(
        song?.id, song?.title, song?.artist, song?.album,
        animatedArtworkEnabled, spotifyFallbackRequested, spotifyPlaybackFailed, sourceOrder
    ) {
        if (!animatedArtworkEnabled || song == null) {
            animatedArtworkUrl = null
            return@LaunchedEffect
        }
        // Use the same provider decision prepared for the next five songs.
        // Respect the selected provider order without late lookup results
        // replacing a stream that is already playing.
        if (spotifyPlaybackFailed && sourceOrder == ArtworkSourceOrder.SPOTIFY_FIRST) {
            // The preferred Spotify stream failed after bounded Media3
            // recovery: only now try Apple artwork, with no provider race.
            artworkProvider = AnimatedArtworkProvider.APPLE_MUSIC
            animatedArtworkUrl =
                AppleMusicAnimatedArtworkLoader.loadAnimatedArtworkUrl(context, song)
        } else if (!spotifyFallbackRequested) {
            val candidate = AnimatedArtworkQueuePreloader.ready(song, sourceOrder)
                ?: AnimatedArtworkQueuePreloader.resolve(context, song, sourceOrder)
            if (candidate != null) {
                artworkProvider = when (candidate.provider) {
                    AnimatedArtworkQueuePreloader.Provider.APPLE_TALL ->
                        AnimatedArtworkProvider.APPLE_MUSIC
                    AnimatedArtworkQueuePreloader.Provider.SPOTIFY_CANVAS ->
                        AnimatedArtworkProvider.SPOTIFY_CANVAS
                }
                animatedArtworkUrl = candidate.url
            } else {
                animatedArtworkUrl = null
            }
        } else {
            // All Apple playback variants failed. Spotify remains an alternate
            // only when selected in settings.
            if (sourceOrder == ArtworkSourceOrder.APPLE_ONLY) {
                animatedArtworkUrl = null
            } else {
                artworkProvider = AnimatedArtworkProvider.SPOTIFY_CANVAS
                animatedArtworkUrl = SpotifyCanvasArtworkLoader.findCanvasUrl(context, song)
            }
        }
    }

    // Static cover and first animated frame both have a prepared palette.
    // Publish once per actual colour change; ignore retained previous tracks.
    val latestPaletteReady by rememberUpdatedState(onPaletteReady)
    LaunchedEffect(
        song?.uri, artworkSongUri, backdrop, frozenAnimatedBackdrop,
        animatedArtworkEnabled, animatedArtworkUrl
    ) {
        val uri = song?.uri
        if (uri != null && artworkSongUri == uri) {
            latestPaletteReady(uri,
                if (animatedArtworkEnabled && animatedArtworkUrl != null) {
                    frozenAnimatedBackdrop ?: backdrop
                } else backdrop
            )
        }
    }
    val activeBlur = retainedAmbientBlur ?: staticBlur
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

    val heroOpacity by animateFloatAsState(
        targetValue = if (lyricsMode) 0f else 1f,
        animationSpec = tween(190),
        label = "immersiveLyricsHeroFade"
    )

    ImmersiveArtworkScene(
        artwork = artwork,
        blur = activeBlur,
        backdrop = animatedBackdrop,
        lightMode = lightMode,
        heroHeight = heroHeight,
        heroWidthFraction = heroWidthFraction,
        heroOpacity = heroOpacity,
        scrimOpacity = scrimOpacity,
        animateChanges = hasPresentedArtwork,
        description = song?.title,
        dynamicGlow = if (glowActive) dynamicGlow else null,
        glowReveal = pageReveal,
        animatedArtwork = {
            val activeUrl = animatedArtworkUrl
            if (animatedArtworkEnabled && activeUrl != null) {
                // A new video URL must get its OWN animation state. Without
                // this key, animateFloatAsState carries alpha=1 from the old
                // TextureView, exposing the new player's empty/black surface
                // for 1–3 frames each time Next is pressed.
                androidx.compose.runtime.key(song?.uri, activeUrl) {
                    AnimatedArtworkVideoBackground(
                        url = activeUrl,
                        playbackVisible = !lyricsMode && playerPageVisible,
                        // During any horizontal swipe, leave the already-decoded
                        // still cover on screen and hide the live TextureView
                        // over the first 14% of travel. Keep the decoder/player
                        // alive: reversing the gesture needs no new stream.
                        modifier = Modifier.fillMaxSize().graphicsLayer {
                            val fraction = (pageReveal() / 0.14f).coerceIn(0f, 1f)
                            val eased = fraction * fraction * (3f - 2f * fraction)
                            alpha = 1f - eased
                        },
                        captureBackdrop = frozenAnimatedBlur == null,
                        dynamicGlowEnabled = glowActive,
                        onGlowReady = { frame ->
                            if (glowActive && animatedArtworkUrl == activeUrl) dynamicGlow = frame
                        },
                        onBackdropReady = { blurredFrame, frameColor ->
                            if (animatedArtworkUrl == activeUrl && frozenAnimatedBlur == null) {
                                frozenAnimatedBlur = blurredFrame
                                retainedAmbientBlur = blurredFrame
                                frozenAnimatedBackdrop = frameColor
                            }
                        },
                        onFirstFrame = {
                            if (animatedArtworkUrl == activeUrl) animatedFirstFrameUrl = activeUrl
                        },
                        onUnavailable = {
                            if (animatedArtworkUrl == activeUrl) {
                                when (artworkProvider) {
                                    AnimatedArtworkProvider.APPLE_MUSIC -> {
                                        AnimatedArtworkQueuePreloader.invalidate(song, activeUrl)
                                        val alternative = AppleMusicAnimatedArtworkLoader.nextPlaybackUrl(activeUrl)
                                        if (alternative != null) {
                                            animatedArtworkUrl = alternative
                                        } else {
                                            // A failed Apple fallback after
                                            // Spotify-first must not return to
                                            // the same failed Canvas repeatedly.
                                            animatedArtworkUrl = null
                                            if (AnimatedArtworkQueuePreloader.spotifyAfterAppleFailure(
                                                    sourceOrder, spotifyPlaybackFailed
                                                )) {
                                                spotifyFallbackRequested = true
                                            }
                                        }
                                    }
                                    AnimatedArtworkProvider.SPOTIFY_CANVAS -> {
                                        // A CDN stall is not evidence the Canvas
                                        // URL disappeared. Retain the verified
                                        // lookup across Next -> Previous, so the
                                        // song can play again with a fresh player.
                                        // Only hide this exhausted player instance.
                                        // In Spotify-first mode, try Apple artwork
                                        // once. For Spotify-only stay on the static
                                        // cover until the user revisits this song.
                                        animatedArtworkUrl = null
                                        if (sourceOrder == ArtworkSourceOrder.SPOTIFY_FIRST) {
                                            spotifyPlaybackFailed = true
                                        }
                                    }
                                }
                            }
                        }
                    )
                }
            }
        },
        modifier = modifier
    )
}
@Composable
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
private fun AnimatedArtworkVideoBackground(
    url: String,
    playbackVisible: Boolean,
    modifier: Modifier = Modifier,
    captureBackdrop: Boolean,
    onBackdropReady: (Bitmap, Color) -> Unit,
    onFirstFrame: () -> Unit,
    onUnavailable: () -> Unit,
    dynamicGlowEnabled: Boolean = false,
    onGlowReady: (Bitmap) -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val isCanvasStream = remember(url) { SpotifyCanvasArtworkLoader.isSpotifyCanvasVideo(url) }
    var videoReady by remember(url) { mutableStateOf(false) }
    var videoVisible by remember(url) { mutableStateOf(false) }
    var playbackRetryCount by remember(url) { mutableIntStateOf(0) }
    // A stalled stream is different from a provider switch: keep the last
    // rendered frame visible while Media3 performs a bounded recovery.
    var bufferingSinceMs by remember(url) { mutableLongStateOf(0L) }
    var artworkPlayerView by remember { mutableStateOf<androidx.media3.ui.PlayerView?>(null) }
    val onBackdropReadyCallback by rememberUpdatedState(onBackdropReady)
    val onUnavailableCallback by rememberUpdatedState(onUnavailable)
    val onFirstFrameCallback by rememberUpdatedState(onFirstFrame)
    val shouldCaptureBackdrop by rememberUpdatedState(captureBackdrop)
    val shouldPlayVideo by rememberUpdatedState(playbackVisible)
    val onGlowReadyCallback by rememberUpdatedState(onGlowReady)

    val player = remember(url) {
        val trackSelector = androidx.media3.exoplayer.trackselection.DefaultTrackSelector(context).apply {
            parameters = buildUponParameters()
                .setMaxVideoSize(720, 1280)
                .setMaxVideoFrameRate(30)
                .setMaxVideoBitrate(4_000_000)
                .setTrackTypeDisabled(androidx.media3.common.C.TRACK_TYPE_AUDIO, true)
                .build()
        }
        val builder = androidx.media3.exoplayer.ExoPlayer.Builder(context)
        val isSpotifyCanvas = SpotifyCanvasArtworkLoader.isSpotifyCanvasVideo(url)
        if (isSpotifyCanvas) {
            SpotifyCanvasVideoCache.mediaSourceFactory(context)?.let {
                builder.setMediaSourceFactory(it)
            }
        } else {
            // Reuse the very same Apple HLS playlist/segment bytes fetched for
            // songs 2-6 in the speculative playback queue.
            AppleTallVideoCache.mediaSourceFactory(context)?.let {
                builder.setMediaSourceFactory(it)
            }
        }
        builder
            .setTrackSelector(trackSelector)
            // The external Spotify proxy can briefly throttle between
            // repeat loops. Give Canvas a larger background buffer than
            // Apple's HLS stream, but still reveal the first frame quickly.
            .setLoadControl(
                androidx.media3.exoplayer.DefaultLoadControl.Builder()
                    .setBufferDurationsMs(
                        if (isSpotifyCanvas) 5_000 else 2_500,
                        if (isSpotifyCanvas) 22_000 else 15_000,
                        if (isSpotifyCanvas) 450 else 350,
                        if (isSpotifyCanvas) 1_500 else 1_250
                    )
                    .build()
            )
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
                    .setMimeType(
                        // Spotycovs delivers Canvas as an MP4 stream from
                        // /api/canvas?id=...&stream=1 (no .mp4 suffix).
                        // Classifying it as HLS caused Media3 to parse
                        // progressive video bytes as an m3u8 playlist.
                        if (SpotifyCanvasArtworkLoader.isSpotifyCanvasVideo(url) ||
                            url.substringBefore('?').endsWith(".mp4", ignoreCase = true)
                        ) androidx.media3.common.MimeTypes.VIDEO_MP4
                        else androidx.media3.common.MimeTypes.APPLICATION_M3U8
                    )
                    .build()
            )
        }
    }

    // Prevent speculative Apple/Spotify lookups and video downloads from
    // taking bandwidth while the active Canvas is still filling its loop.
    // READY may mean only ~450ms buffered; a first frame alone does not
    // mean there is enough media to play smoothly.
    fun protectForegroundCanvas() {
        if (!isCanvasStream) return
        val buffering = player.playbackState == Player.STATE_IDLE ||
            player.playbackState == Player.STATE_BUFFERING
        val bufferedAhead = (player.bufferedPosition - player.currentPosition).coerceAtLeast(0L)
        val filling = player.isLoading && bufferedAhead < 3_500L
        SpotifyCanvasVideoCache.setForegroundBuffering(
            url, shouldPlayVideo && (buffering || filling)
        )
    }

    // Do not require a successful TextureView screenshot to reveal animated artwork.
    // Some devices render video correctly but never return a usable capture.
    LaunchedEffect(player, videoReady) {
        if (videoReady) {
            // Media3 first-frame notification can precede the View hierarchy's
            // next composition. Wait just one vsync to guarantee its TextureView
            // is attached before showing alpha>0; otherwise a black rectangle
            // can appear during rapid skips.
            androidx.compose.runtime.withFrameNanos { }
            videoVisible = true
        } else {
            videoVisible = false
        }
    }

    LaunchedEffect(player, artworkPlayerView, videoVisible, playbackVisible) {
        val view = artworkPlayerView ?: return@LaunchedEffect
        // Start after the video has actually faded in: hidden TextureViews on
        // some devices return a black bitmap even when playback has started.
        if (!videoVisible || !shouldCaptureBackdrop || !playbackVisible) return@LaunchedEffect
        delay(650L)
        repeat(8) {
            if (!shouldPlayVideo) return@LaunchedEffect
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
            delay(220L)
        }
    }

    LaunchedEffect(player, artworkPlayerView, videoVisible, playbackVisible, dynamicGlowEnabled) {
        val view = artworkPlayerView ?: return@LaunchedEffect
        if (!dynamicGlowEnabled || !videoVisible || !playbackVisible) return@LaunchedEffect
        com.yohandeku32.nusamusic.ui.sampleArtworkGlow(
            position = { if (player.isPlaying) player.currentPosition else null },
            capture = { ArtworkBlur.captureViewport(view, maxSize = 96) },
            publish = { onGlowReadyCallback(it) }
        )
    }

    LaunchedEffect(player, playbackRetryCount, playbackVisible) {
        if (playbackRetryCount > 0 && playbackVisible) {
            delay(650L * playbackRetryCount)
            if (!shouldPlayVideo) return@LaunchedEffect
            // Resume an interrupted loop from the beginning on the SAME
            // player/TextureView; don't wait on a stalled partial MP4 read.
            player.seekTo(0L)
            player.prepare()
            player.playWhenReady = shouldPlayVideo
        }
    }

    LaunchedEffect(player, bufferingSinceMs, playbackVisible, playbackRetryCount) {
        if (!playbackVisible || bufferingSinceMs == 0L || !videoReady) return@LaunchedEffect
        delay(6_500L)
        if (!shouldPlayVideo || player.playbackState != Player.STATE_BUFFERING ||
            bufferingSinceMs == 0L
        ) return@LaunchedEffect
        if (playbackRetryCount < 2) {
            // An IO stream can stall indefinitely without emitting
            // onPlayerError. Recover once, without hiding the last frame.
            playbackRetryCount++
            bufferingSinceMs = 0L
        } else {
            videoReady = false
            videoVisible = false
            onUnavailableCallback()
        }
    }

    androidx.compose.runtime.DisposableEffect(player, url) {
        if (isCanvasStream && shouldPlayVideo) {
            SpotifyCanvasVideoCache.setForegroundBuffering(url, true)
        }
        val listener = object : Player.Listener {
            override fun onRenderedFirstFrame() {
                protectForegroundCanvas()
                videoReady = true
                bufferingSinceMs = 0L
                onFirstFrameCallback()
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                protectForegroundCanvas()
                bufferingSinceMs =
                    if (playbackState == Player.STATE_BUFFERING && videoReady)
                        android.os.SystemClock.elapsedRealtime()
                    else 0L
            }

            override fun onIsLoadingChanged(isLoading: Boolean) {
                protectForegroundCanvas()
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                if (isCanvasStream) SpotifyCanvasVideoCache.setForegroundBuffering(url, false)
                bufferingSinceMs = 0L
                if (error.errorCode in 2000..2999 && playbackRetryCount < 2) {
                    // Transient CDN/proxy IO errors should not make Canvas
                    // disappear between retries after the first frame.
                    playbackRetryCount++
                } else {
                    // Only give up after recovery is exhausted, not on every
                    // buffering interruption or on a late Apple response.
                    videoReady = false
                    videoVisible = false
                    onUnavailableCallback()
                }
            }
        }

        player.addListener(listener)
        player.prepare()
        player.playWhenReady = shouldPlayVideo
        onDispose {
            if (isCanvasStream) SpotifyCanvasVideoCache.setForegroundBuffering(url, false)
            player.removeListener(listener)
            player.release()
        }
    }

    // Hidden animated covers must stop decoding while Lyrics paints the
    // TTML glyphs. Retain ExoPlayer/TextureView so closing Lyrics resumes
    // without the expensive HLS player setup or re-buffering from scratch.
    LaunchedEffect(player, playbackVisible) {
        player.playWhenReady = playbackVisible
        protectForegroundCanvas()
        if (isCanvasStream && playbackVisible) {
            // ExoPlayer may remain in READY while downloading. Check buffered
            // headroom until at least 3.5s is available, so prefetch cannot
            // starve the currently moving Canvas loop during track changes.
            repeat(24) {
                delay(250L)
                protectForegroundCanvas()
                if (!shouldPlayVideo || !player.isLoading ||
                    (player.bufferedPosition - player.currentPosition) >= 3_500L
                ) return@LaunchedEffect
            }
        }
    }

    val videoAlpha by animateFloatAsState(
        targetValue = if (videoVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 180),
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
        // Recycled Library rows may already have a full-resolution-enough
        // thumbnail in shared memory. Skip the delay, coroutine IO handoff,
        // and metadata retriever entirely for those tracks.
        val cachedExactOrLarger = song?.let {
            ArtworkLoader.cached(
                uriString = it.uri,
                maxSize = maxSizePx,
                cacheKey = it.albumId.takeIf { albumId -> albumId > 0L }?.toString(),
                allowSmallerPreview = false
            )
        }
        if (cachedExactOrLarger != null) {
            bitmap = cachedExactOrLarger
            return@LaunchedEffect
        }
        // Track rows that previously had no embedded image must not reopen
        // the file and resample it every time LazyColumn recycles them.
        if (song != null && ArtworkLoader.isMissingCached(
                song.uri, maxSizePx,
                song.albumId.takeIf { it > 0L }?.toString()
            )
        ) {
            bitmap = null
            return@LaunchedEffect
        }
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

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun LibrarySongListRow(
    song: Song,
    selected: Boolean,
    loadDetails: Boolean,
    onPlay: (Song) -> Unit,
    onRequestDelete: (Song) -> Unit
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
            .combinedClickable(
                onClick = { onPlay(song) },
                onLongClick = { onRequestDelete(song) },
                onLongClickLabel = "Delete ${song.title} permanently"
            )
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
