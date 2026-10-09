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
import com.yohandeku32.nusamusic.model.PlayerPresentationMode
import com.yohandeku32.nusamusic.model.Song
import com.yohandeku32.nusamusic.playback.PlaybackService
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
    private val playbackPositionState = mutableLongStateOf(0L)
    private var positionMs by playbackPositionState
    private var durationMs by mutableLongStateOf(0L)
    private var playerPageVisible = true
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
    private var libraryCacheLoaded = false
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
                    if (playerPageVisible) {
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
                delay(if (isPlaying && playerPageVisible) 250L else 1_000L)
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
                    onPlaybackOrderChanged = ::updatePlaybackOrder,
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
                    selectedMusicFolders = selectedMusicFolders,
                    onPlayerPageVisibilityChanged = ::setPlayerPageVisible,
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

    private fun setPlayerPageVisible(visible: Boolean) {
        playerPageVisible = visible
        if (!visible) return

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
        controller?.let { c ->
            // Use the list's selected sort order for the Media3 queue as well as
            // the visible library. This keeps Next/Previous aligned with sorting.
            val queueSongs = orderedSongs.ifEmpty { songs }
            val targetIndex = queueSongs.indexOfFirst { it.id == song.id }
            if (targetIndex < 0) return@let

            // Avoid resetting the decoder when the queue already uses this exact order.
            val queueMatches = c.mediaItemCount == queueSongs.size &&
                queueSongs.indices.all { index ->
                    c.getMediaItemAt(index).mediaId == queueSongs[index].id.toString()
                }

            if (queueMatches) {
                c.seekTo(targetIndex, 0L)
                c.play()
            } else {
                c.setMediaItems(
                    queueSongs.map(::mediaItemFor),
                    targetIndex,
                    0L
                )
                c.prepare()
                c.play()
            }

            currentSong = song
            isPlaying = true
        }
    }

    private fun updatePlaybackOrder(orderedSongs: List<Song>) {
        if (orderedSongs.isEmpty()) return

        val c = controller ?: return
        val currentMediaId = c.currentMediaItem?.mediaId ?: return
        val orderedIds = orderedSongs.map { it.id.toString() }
        val currentIds = (0 until c.mediaItemCount)
            .map { index -> c.getMediaItemAt(index).mediaId }

        if (
            currentIds.size == orderedIds.size &&
            currentIds.toSet() == orderedIds.toSet()
        ) {
            // Reorder existing queue entries in place to preserve the active
            // decoder, current position and ongoing playback.
            orderedIds.forEachIndexed { targetIndex, targetId ->
                val sourceIndex = (targetIndex until c.mediaItemCount)
                    .firstOrNull { index ->
                        c.getMediaItemAt(index).mediaId == targetId
                    } ?: return@forEachIndexed

                if (sourceIndex != targetIndex) {
                    c.moveMediaItem(sourceIndex, targetIndex)
                }
            }
            return
        }

        // If the library membership changed, keep the current track and position
        // while synchronizing the queue to the newly sorted library.
        val currentIndex = orderedIds.indexOf(currentMediaId)
        if (currentIndex < 0) return

        val wasPlayWhenReady = c.playWhenReady
        val positionMs = c.currentPosition.coerceAtLeast(0L)
        val savedRepeatMode = c.repeatMode
        val savedShuffleEnabled = c.shuffleModeEnabled

        c.setMediaItems(
            orderedSongs.map(::mediaItemFor),
            currentIndex,
            positionMs
        )
        c.repeatMode = savedRepeatMode
        c.shuffleModeEnabled = savedShuffleEnabled
        c.prepare()
        if (wasPlayWhenReady) c.play() else c.pause()
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
    onPlaybackOrderChanged: (List<Song>) -> Unit,
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
    selectedMusicFolders: List<String>,
    onPlayerPageVisibilityChanged: (Boolean) -> Unit,
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

    var customTitleTypeface by remember {
        mutableStateOf<Typeface?>(null)
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
        } ?: when (titleFontName) {
            "Sans Serif" -> FontFamily.SansSerif
            "Monospace" -> FontFamily.Monospace
            "Cursive" -> FontFamily.Cursive
            "Default" -> FontFamily.Default
            else -> FontFamily.Serif
        }
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

            LibrarySortOption.RECENTLY_ADDED ->
                songs.sortedWith(
                    compareByDescending<Song> { it.dateAddedMs }
                        .thenBy { it.title.lowercase(Locale.ROOT) }
                )

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

    LaunchedEffect(filtered) {
        onPlaybackOrderChanged(filtered)
    }

    val alphabet = remember {
        listOf('#') + ('A'..'Z')
    }

    val alphabetTargets = remember(filtered) {
        buildMap {
            filtered.forEachIndexed { index, song ->
                val key = alphabetIndexKey(song.title)
                if (key !in this) {
                    put(key, index)
                }
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
        androidx.compose.foundation.lazy.rememberLazyListState()
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

    // Bring the currently playing song into view when opening the song list.
    LaunchedEffect(pagerState) {
        androidx.compose.runtime.snapshotFlow { pagerState.settledPage }
            .collect { settledPage ->
                onPlayerPageVisibilityChanged(settledPage == 0)
                if (settledPage == 1 && currentSong != null) {
                    val index = filtered.indexOfFirst { it.id == currentSong.id }

                    if (index >= 0) {
                        kotlinx.coroutines.yield()
                        libraryListState.animateScrollToItem(index)
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
                    .verticalScroll(androidx.compose.foundation.rememberScrollState())
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
                    nusaText("FONT JUDUL LAGU", "TITLE FONT"),
                    fontSize = 11.sp,
                    letterSpacing = 1.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))

                var titleFontMenuExpanded by remember {
                    mutableStateOf(false)
                }

                Box {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        onClick = {
                            titleFontMenuExpanded = true
                        }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    horizontal = 16.dp,
                                    vertical = 14.dp
                                ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = customTitleFontName ?: titleFontName,
                                    fontFamily = titleFontFamily,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    nusaText(
                                        "Hanya memengaruhi judul lagu di halaman utama.",
                                        "Only affects the song title on the main player."
                                    ),
                                    fontSize = 11.sp,
                                    lineHeight = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Icon(
                                Icons.Default.KeyboardArrowDown,
                                contentDescription = nusaText(
                                    "Pilih font",
                                    "Choose font"
                                ),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = titleFontMenuExpanded,
                        onDismissRequest = {
                            titleFontMenuExpanded = false
                        }
                    ) {
                        listOf(
                            "Default",
                            "Sans Serif",
                            "Serif",
                            "Monospace",
                            "Cursive"
                        ).forEach { fontName ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = fontName,
                                        fontFamily = when (fontName) {
                                            "Sans Serif" -> FontFamily.SansSerif
                                            "Monospace" -> FontFamily.Monospace
                                            "Cursive" -> FontFamily.Cursive
                                            "Default" -> FontFamily.Default
                                            else -> FontFamily.Serif
                                        },
                                        fontWeight = if (
                                            fontName == titleFontName
                                        ) {
                                            FontWeight.Bold
                                        } else {
                                            FontWeight.Normal
                                        }
                                    )
                                },
                                onClick = {
                                    titleFontName = fontName
                                    onClearCustomTitleFont()

                                    uiPrefs.edit()
                                        .remove("custom_title_font_path")
                                        .remove("custom_title_font_name")
                                        .putString(
                                            "title_font_family",
                                            fontName
                                        )
                                        .apply()

                                    titleFontMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    onClick = onChooseCustomTitleFont
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = 16.dp,
                                vertical = 14.dp
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                nusaText(
                                    "Choose custom font",
                                    "Choose custom font"
                                ),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                ".TTF / .OTF",
                                fontSize = 11.sp,
                                color =
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Icon(
                            Icons.Default.FolderOpen,
                            contentDescription = nusaText(
                                "Pilih font",
                                "Choose font"
                            ),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (customTitleFontPath != null) {
                    Spacer(Modifier.height(8.dp))

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        onClick = onResetCustomTitleFont
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    horizontal = 16.dp,
                                    vertical = 12.dp
                                ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    nusaText(
                                        "Reset to system font",
                                        "Reset to system font"
                                    ),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    customTitleFontName ?: "Custom font",
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    overflow =
                                        androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                    color =
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Icon(
                                Icons.Default.Close,
                                contentDescription = nusaText(
                                    "Reset font",
                                    "Reset font"
                                ),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(Modifier.height(22.dp))

                Text(
                    nusaText("TAMPILAN PEMUTAR", "PLAYER PRESENTATION"),
                    fontSize = 11.sp,
                    letterSpacing = 1.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PlayerButtonStyleOption(
                        title = "Vinyl",
                        subtitle = nusaText(
                            "Piringan hitam berputar.",
                            "Rotating vinyl record."
                        ),
                        selected = playerPresentationMode == PlayerPresentationMode.VINYL,
                        onClick = {
                            playerPresentationMode = PlayerPresentationMode.VINYL
                            uiPrefs.edit()
                                .putString(
                                    "player_presentation_mode",
                                    PlayerPresentationMode.VINYL.preferenceValue
                                )
                                .apply()
                        },
                        modifier = Modifier.weight(1f)
                    )
                    PlayerButtonStyleOption(
                        title = nusaText("Sampul imersif", "Immersive art"),
                        subtitle = nusaText(
                            "Sampul album layar penuh.",
                            "Full-cover album artwork."
                        ),
                        selected = playerPresentationMode == PlayerPresentationMode.IMMERSIVE_ARTWORK,
                        onClick = {
                            playerPresentationMode = PlayerPresentationMode.IMMERSIVE_ARTWORK
                            uiPrefs.edit()
                                .putString(
                                    "player_presentation_mode",
                                    PlayerPresentationMode.IMMERSIVE_ARTWORK.preferenceValue
                                )
                                .apply()
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(22.dp))

                Text(
                    nusaText("GAYA TOMBOL PEMUTARAN", "PLAYER BUTTON STYLE"),
                    fontSize = 11.sp,
                    letterSpacing = 1.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PlayerButtonStyleOption(
                        title = nusaText("3D Realistis", "3D Realistic"),
                        subtitle = nusaText(
                            "Tekstur dan kedalaman seperti tombol fisik.",
                            "Textured, physical button depth."
                        ),
                        selected = realisticControls,
                        onClick = {
                            realisticControls = true
                            uiPrefs.edit()
                                .putBoolean("realistic_controls", true)
                                .apply()
                        },
                        modifier = Modifier.weight(1f)
                    )
                    PlayerButtonStyleOption(
                        title = "Flat",
                        subtitle = nusaText(
                            "Tampilan datar seperti versi sebelumnya.",
                            "Simple flat buttons like before."
                        ),
                        selected = !realisticControls,
                        onClick = {
                            realisticControls = false
                            uiPrefs.edit()
                                .putBoolean("realistic_controls", false)
                                .apply()
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(22.dp))

                Text(
                    nusaText("CROSSFADE", "CROSSFADE"),
                    fontSize = 11.sp,
                    letterSpacing = 1.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    tonalElevation = 0.dp
                ) {
                    Column(
                        modifier = Modifier.padding(
                            horizontal = 16.dp,
                            vertical = 8.dp
                        )
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    nusaText("Crossfade", "Crossfade"),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    nusaText(
                                        "Transisi halus antara dua lagu.",
                                        "Smooth transition between two songs."
                                    ),
                                    fontSize = 12.sp,
                                    lineHeight = 17.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            androidx.compose.material3.Switch(
                                checked = crossfadeEnabled,
                                onCheckedChange = { enabled ->
                                    crossfadeEnabled = enabled
                                    playbackPrefs.edit()
                                        .putBoolean("crossfade_enabled", enabled)
                                        .apply()
                                }
                            )
                        }

                        Spacer(Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                nusaText("Durasi", "Duration"),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                "${crossfadeDurationSeconds.toInt()} s",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        androidx.compose.material3.Slider(
                            value = crossfadeDurationSeconds,
                            onValueChange = { crossfadeDurationSeconds = it },
                            onValueChangeFinished = {
                                playbackPrefs.edit()
                                    .putLong(
                                        "crossfade_duration_ms",
                                        (crossfadeDurationSeconds * 1_000L).toLong()
                                    )
                                    .apply()
                            },
                            valueRange = 1f..12f,
                            steps = 10,
                            enabled = crossfadeEnabled
                        )

                        Text(
                            nusaText(
                                "1–12 detik. Default 5 detik.",
                                "1–12 seconds. Default is 5 seconds."
                            ),
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

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
                                nusaText("Scan folder musik", "Scan music folder"),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                nusaText("Pilih folder lokal yang berisi musik untuk dipindai.", "Choose a local folder containing music to scan."),
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
                                .background(
                                    if (playerPresentationMode == PlayerPresentationMode.IMMERSIVE_ARTWORK ||
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
                                        .fillParentMaxHeight()
                                        .clip(
                                            RoundedCornerShape(
                                                bottomStart = 34.dp,
                                                bottomEnd = 34.dp
                                            )
                                        )
                                        .then(
                                            if (playerPresentationMode == PlayerPresentationMode.VINYL) {
                                                Modifier.background(MaterialTheme.colorScheme.background)
                                            } else {
                                                Modifier
                                            }
                                        )
                                ) {
                                    if (playerPresentationMode == PlayerPresentationMode.IMMERSIVE_ARTWORK) {
                                        ImmersiveArtwork(
                                            song = currentSong,
                                            modifier = Modifier.fillMaxSize()
                                        )

                                        // Keep the cover artwork visible while gradually darkening
                                        // the area behind the title and playback controls.
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(
                                                    Brush.verticalGradient(
                                                        colorStops = arrayOf(
                                                            0.00f to Color.Transparent,
                                                            0.32f to Color.Transparent,
                                                            0.40f to Color.Black.copy(alpha = 0.12f),
                                                            0.48f to Color.Black.copy(alpha = 0.32f),
                                                            0.60f to Color.Black.copy(alpha = 0.62f),
                                                            0.74f to Color.Black.copy(alpha = 0.84f),
                                                            0.88f to Color.Black.copy(alpha = 0.95f),
                                                            1.00f to Color.Black.copy(alpha = 0.99f)
                                                        )
                                                    )
                                                )
                                        )
                                    }

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
                                                    contentDescription = nusaText("Lainnya", "More"),
                                                    tint = if (
                                                        playerPresentationMode == PlayerPresentationMode.IMMERSIVE_ARTWORK
                                                    ) Color.White else MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                            IconButton(onClick = { showSettings = true }) {
                                                Icon(
                                                    Icons.Default.Settings,
                                                    contentDescription = nusaText("Pengaturan", "Settings"),
                                                    tint = if (
                                                        playerPresentationMode == PlayerPresentationMode.IMMERSIVE_ARTWORK
                                                    ) Color.White else MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                    )

                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .then(
                                                if (playerPresentationMode == PlayerPresentationMode.IMMERSIVE_ARTWORK) {
                                                    Modifier.weight(1f)
                                                } else {
                                                    Modifier
                                                }
                                            )
                                            .padding(horizontal = 22.dp),
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
                                        val screenWidthDp =
                                            androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp
                                        val estimatedTitleLines = remember(titleText, titleSize, screenWidthDp) {
                                            val availableWidthDp = (screenWidthDp - 52f).coerceAtLeast(120f)
                                            val estimatedCharacterWidthDp = (titleSize.value * 0.60f).coerceAtLeast(1f)
                                            val estimatedCharactersPerLine =
                                                (availableWidthDp / estimatedCharacterWidthDp).coerceAtLeast(1f)
                                            kotlin.math.ceil(
                                                titleText.length / estimatedCharactersPerLine
                                            ).toInt().coerceIn(1, 6)
                                        }
                                        val titleLineHeightDp = with(
                                            androidx.compose.ui.platform.LocalDensity.current
                                        ) {
                                            titleLineHeight.toDp()
                                        }
                                        val titleBoxHeight = if (immersiveArtworkMode) {
                                            maxOf(
                                                82.dp,
                                                titleLineHeightDp * estimatedTitleLines.toFloat() + 8.dp
                                            )
                                        } else {
                                            82.dp
                                        }
                                        val titleExtraHeight = titleBoxHeight - 82.dp

                                        if (playerPresentationMode == PlayerPresentationMode.VINYL) {
                                            Spacer(Modifier.height(2.dp))
                                        } else {
                                            val screenHeight =
                                                androidx.compose.ui.platform.LocalConfiguration.current
                                                    .screenHeightDp.dp
                                            Spacer(
                                                Modifier.height(
                                                    (
                                                        (screenHeight * 0.30f).coerceIn(160.dp, 250.dp) +
                                                            16.dp - titleExtraHeight
                                                    ).coerceAtLeast(0.dp)
                                                )
                                            )
                                        }

                                        if (playerPresentationMode == PlayerPresentationMode.VINYL) {
                                            VinylRecord(
                                                song = currentSong,
                                                isPlaying = isPlaying,
                                                positionMsState = positionMsState,
                                                durationMs = durationMs,
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
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                titleText,
                                                fontFamily = titleFontFamily,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = titleSize,
                                                lineHeight = titleLineHeight,
                                                maxLines = if (immersiveArtworkMode) estimatedTitleLines else 3,
                                                overflow = if (immersiveArtworkMode) {
                                                    androidx.compose.ui.text.style.TextOverflow.Clip
                                                } else {
                                                    androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                                },
                                                modifier = Modifier.padding(horizontal = 4.dp),
                                                color = if (
                                                    playerPresentationMode == PlayerPresentationMode.IMMERSIVE_ARTWORK
                                                ) Color.White else MaterialTheme.colorScheme.onBackground,
                                                textAlign = TextAlign.Center
                                            )
                                        }

                                        Text(
                                            currentSong?.artist ?: nusaText("Perpustakaan musik lokal Anda", "Your local music library"),
                                            modifier = Modifier.fillMaxWidth(),
                                            fontSize = 14.sp,
                                            maxLines = 1,
                                            textAlign = TextAlign.Center,
                                            color = if (
                                                playerPresentationMode == PlayerPresentationMode.IMMERSIVE_ARTWORK
                                            ) {
                                                Color.White.copy(alpha = 0.82f)
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            }
                                        )

                                        Spacer(Modifier.height(16.dp))

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
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            if (immersiveArtworkMode) {
                                                IconButton(
                                                    onClick = onPrevious,
                                                    enabled = currentSong != null,
                                                    modifier = Modifier.size(68.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Rounded.FastRewind,
                                                        contentDescription = nusaText("Sebelumnya", "Previous"),
                                                        tint = Color.White,
                                                        modifier = Modifier.size(40.dp)
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

                                            Spacer(Modifier.width(12.dp))

                                            if (immersiveArtworkMode) {
                                                IconButton(
                                                    onClick = if (currentSong == null) onRequestPermission else onTogglePlay,
                                                    enabled = true,
                                                    modifier = Modifier.size(96.dp)
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
                                                        modifier = Modifier.size(60.dp)
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

                                            Spacer(Modifier.width(12.dp))

                                            if (immersiveArtworkMode) {
                                                IconButton(
                                                    onClick = onNext,
                                                    enabled = currentSong != null,
                                                    modifier = Modifier.size(68.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Rounded.FastForward,
                                                        contentDescription = nusaText("Berikutnya", "Next"),
                                                        tint = Color.White,
                                                        modifier = Modifier.size(40.dp)
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
                                                        },
                                                    color = if (
                                                        playerPresentationMode == PlayerPresentationMode.IMMERSIVE_ARTWORK
                                                    ) Color.White else MaterialTheme.colorScheme.onSurface
                                                )
                                            }

                                            Spacer(Modifier.weight(1f))

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
                                            Spacer(Modifier.height(12.dp))
                                            AudioQualityPill(
                                                song = currentSong,
                                                immersive = true
                                            )
                                        }

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
                                .background(MaterialTheme.colorScheme.background)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(76.dp)
                                    .background(MaterialTheme.colorScheme.background)
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
                                        tint = MaterialTheme.colorScheme.onBackground
                                    )
                                }

                                Text(
                                    text = nusaText("Daftar Lagu", "Songs"),
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.Center,
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

                                                    if (
                                                        option == LibrarySortOption.RECENTLY_ADDED &&
                                                        songs.any { it.dateAddedMs <= 0L }
                                                    ) {
                                                        onScanMusic()
                                                    }
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
                                                nusaText(
                                                    "Tidak ada musik lokal",
                                                    "No local music found"
                                                ),
                                                color = Color(0xFF9D9D9D),
                                                fontSize = 14.sp
                                            )
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
                                                Color.Black.copy(alpha = 0.78f)
                                            } else {
                                                Color.Black.copy(alpha = 0.22f)
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

                        }
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

@Composable
private fun AudioQualityPill(
    song: Song?,
    immersive: Boolean = false
) {
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
    val isHiRes = isLossless && info?.isHiRes == true

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
                    .padding(top = if (immersive) 0.dp else 4.dp)
                    .clip(RoundedCornerShape(50))
                    .background(
                        color = if (immersive) {
                            Color.White.copy(alpha = 0.94f)
                        } else if (hiRes) {
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

    if (isLossless && !immersive) {
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
            color = if (immersive) {
                Color.White.copy(alpha = 0.48f)
            } else {
                Color(0xFFD0CDC6)
            },
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
private fun PlayerButtonStyleOption(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(82.dp),
        shape = RoundedCornerShape(16.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        },
        border = if (selected) {
            androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
            )
        } else {
            null
        },
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(3.dp))
            Text(
                subtitle,
                fontSize = 10.sp,
                lineHeight = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
    positionMsState: State<Long>,
    durationMs: Long,
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

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    rotationZ = rotation.floatValue
                    shadowElevation = 25.dp.toPx()
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

            // Stronger but still soft physical cast shadow around the
            // lower perimeter. Because the complete record layer rotates,
            // this highlight/shadow interaction rotates naturally with it.
            drawCircle(
                color = Color.Black.copy(alpha = 0.18f),
                center = androidx.compose.ui.geometry.Offset(
                    centerX + radius * 0.008f,
                    centerY + radius * 0.045f
                ),
                radius = radius * 1.005f
            )

            drawCircle(
                color = Color(0xFF090909),
                center = androidx.compose.ui.geometry.Offset(
                    centerX,
                    centerY + edgeOffset
                ),
                radius = radius * 0.992f
            )

            drawArc(
                color = Color.Black.copy(alpha = 0.78f),

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
                color = Color.White.copy(alpha = 0.10f),
                startAngle = 188f,
                sweepAngle = 147f,
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

            // Distributed lacquer reflections. A real vinyl catches light
            // across several parts of the rotating surface rather than in one
            // fixed bright spot.
            val reflections = arrayOf(
                floatArrayOf(-92f, 27f, 0.20f, 7.5f),
                floatArrayOf(-28f, 24f, 0.105f, 5.0f),
                floatArrayOf(34f, 31f, 0.075f, 4.0f),
                floatArrayOf(103f, 22f, 0.115f, 5.5f),
                floatArrayOf(162f, 30f, 0.065f, 4.2f),
                floatArrayOf(224f, 26f, 0.095f, 4.8f),
                floatArrayOf(286f, 32f, 0.055f, 3.6f)
            )

            reflections.forEach { mark ->
                drawArc(
                    color = Color.White.copy(alpha = mark[2]),
                    startAngle = mark[0],
                    sweepAngle = mark[1],
                    useCenter = false,
                    topLeft = androidx.compose.ui.geometry.Offset(
                        size.width * 0.045f,
                        size.height * 0.045f
                    ),
                    size = androidx.compose.ui.geometry.Size(
                        size.width * 0.91f,
                        size.height * 0.91f
                    ),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = mark[3]
                    )
                )
            }

            // Secondary reflection bands at different radii create a wider,
            // broken reflection pattern across the record.
            val secondaryBands = arrayOf(
                floatArrayOf(-72f, 34f, 0.075f, 0.94f, 8f),
                floatArrayOf(18f, 48f, 0.052f, 0.82f, 5.5f),
                floatArrayOf(118f, 30f, 0.048f, 0.88f, 7f),
                floatArrayOf(198f, 42f, 0.038f, 0.76f, 4.5f),
                floatArrayOf(292f, 26f, 0.045f, 0.70f, 6f)
            )

            secondaryBands.forEach { mark ->
                val diameter = size.minDimension * mark[3]
                drawArc(
                    color = Color.White.copy(alpha = mark[2]),
                    startAngle = mark[0],
                    sweepAngle = mark[1],
                    useCenter = false,
                    topLeft = androidx.compose.ui.geometry.Offset(
                        centerX - diameter / 2f,
                        centerY - diameter / 2f
                    ),
                    size = androidx.compose.ui.geometry.Size(
                        diameter,
                        diameter
                    ),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = mark[4]
                    )
                )
            }

            // Very soft diffuse catches spread over the face.
            val diffuseSpots = arrayOf(
                androidx.compose.ui.geometry.Offset(size.width * 0.24f, size.height * 0.19f),
                androidx.compose.ui.geometry.Offset(size.width * 0.76f, size.height * 0.25f),
                androidx.compose.ui.geometry.Offset(size.width * 0.84f, size.height * 0.63f),
                androidx.compose.ui.geometry.Offset(size.width * 0.59f, size.height * 0.84f),
                androidx.compose.ui.geometry.Offset(size.width * 0.22f, size.height * 0.68f)
            )

            diffuseSpots.forEachIndexed { index, spot ->
                val spotRadius = radius * (0.20f + index * 0.015f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.020f + index * 0.002f),
                            Color.White.copy(alpha = 0.007f),
                            Color.Transparent
                        ),
                        center = spot,
                        radius = spotRadius
                    ),
                    radius = spotRadius,
                    center = spot
                )
            }

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
            List(260) {
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

                // Fine paper grain. The album cover keeps a tactile printed-paper
                // feel without adding scratches or distressed surface marks.
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

                // Very subtle fibers following a natural paper surface.
                for (i in 0 until 22) {
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
            ) {
                // Subtle pressed-metal texture inside the small center cap.
                Canvas(Modifier.fillMaxSize()) {
                    val spindleCenter = androidx.compose.ui.geometry.Offset(
                        size.width / 2f,
                        size.height / 2f
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = 0.045f),
                        radius = size.minDimension * 0.33f,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = 0.7f
                        )
                    )
                    drawCircle(
                        color = Color.Black.copy(alpha = 0.34f),
                        radius = size.minDimension * 0.43f,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = 0.7f
                        )
                    )
                    for (i in 0 until 12) {
                        val angle = Math.toRadians((i * 31f + 7f).toDouble())
                        val distance = size.minDimension * (0.20f + (i % 3) * 0.08f)
                        val x = spindleCenter.x + kotlin.math.cos(angle).toFloat() * distance
                        val y = spindleCenter.y + kotlin.math.sin(angle).toFloat() * distance
                        drawCircle(
                            color = if (i % 2 == 0) {
                                Color.White.copy(alpha = 0.018f)
                            } else {
                                Color.Black.copy(alpha = 0.022f)
                            },
                            radius = 0.55f,
                            center = androidx.compose.ui.geometry.Offset(x, y)
                        )
                    }
                }
            }
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFA8A8A8))
            )
        }

        VinylTonearm(
            isPlaying = isPlaying,
            hasSong = song != null,
            positionMsState = positionMsState,
            durationMs = durationMs,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun VinylTonearm(
    isPlaying: Boolean,
    hasSong: Boolean,
    positionMsState: State<Long>,
    durationMs: Long,
    modifier: Modifier = Modifier
) {
    // The tonearm is drawn entirely with Compose Canvas.
    // This keeps the proportions locked to one coordinate system, so the
    // artwork cannot be stretched/squashed by Image content scaling.
    val positionMs by positionMsState
    val trackProgress = if (durationMs > 0L) {
        (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    // The stylus is physically mounted down-right from the pivot in the
    // Canvas artwork. A small angle puts the stylus at the outer groove;
    // increasing the angle moves it inward toward the label without crossing
    // onto the album artwork.
    val targetAngle = if (hasSong) {
        // Start just a little farther outward than the previous position,
        // while keeping the same smooth inward tracking range.
        2.5f + trackProgress * 13f
    } else {
        0f
    }

    val armAngle by animateFloatAsState(
        targetValue = targetAngle,
        animationSpec = if (isPlaying && hasSong) {
            androidx.compose.animation.core.spring(
                dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy,
                stiffness = 180f
            )
        } else {
            androidx.compose.animation.core.spring(
                dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy,
                stiffness = 220f
            )
        },
        label = "tonearmAngle"
    )

    Canvas(modifier = modifier) {
        // Reference artwork is 297 × 732. Scale uniformly from that exact
        // coordinate system, then anchor the transparent design area to the
        // top-right just like the original artwork.
        val designScale = minOf(size.width, size.height) * 0.92f / 732f
        val designWidth = 297f * designScale

        // Keep the pivot clearly in the white area to the right of the vinyl.
        // The slight right shift is intentional; a small part of the artwork
        // may sit beyond the canvas edge, matching a real turntable layout.
        val pivotShift = size.width * 0.115f
        val left = size.width - designWidth + pivotShift
        val top = -size.height * 0.085f

        fun x(value: Float): Float = left + value * designScale
        fun y(value: Float): Float = top + value * designScale
        fun point(px: Float, py: Float) =
            androidx.compose.ui.geometry.Offset(x(px), y(py))

        val pivot = point(151f, 171f)

        rotate(
            degrees = armAngle,
            pivot = pivot
        ) {
            val softShadow = Color.Black.copy(alpha = 0.12f)

            // A soft radial shadow grounds the pivot against the turntable
            // surface before the metal bearing is drawn over it.
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.16f),
                        Color.Black.copy(alpha = 0.08f),
                        Color.Transparent
                    ),
                    center = androidx.compose.ui.geometry.Offset(
                        x(153.5f),
                        y(176f)
                    ),
                    radius = 58f * designScale
                ),
                radius = 58f * designScale,
                center = androidx.compose.ui.geometry.Offset(
                    x(153.5f),
                    y(176f)
                )
            )

            // Pivot contact shadow: several soft offset layers make the
            // bearing look seated above the white turntable surface.
            drawCircle(
                color = Color.Black.copy(alpha = 0.08f),
                radius = 53f * designScale,
                center = androidx.compose.ui.geometry.Offset(
                    x(151f) + 3.5f * designScale,
                    y(171f) + 4.5f * designScale
                )
            )
            drawCircle(
                color = Color.Black.copy(alpha = 0.11f),
                radius = 48f * designScale,
                center = androidx.compose.ui.geometry.Offset(
                    x(151f) + 2.2f * designScale,
                    y(171f) + 3.0f * designScale
                )
            )
            drawCircle(
                color = Color.Black.copy(alpha = 0.13f),
                radius = 43f * designScale,
                center = androidx.compose.ui.geometry.Offset(
                    x(151f) + 1.3f * designScale,
                    y(171f) + 2.0f * designScale
                )
            )

            // Soft contact shadow under the metal arm and cartridge.
            val shadowPath = androidx.compose.ui.graphics.Path().apply {
                moveTo(x(145f), y(249f))
                cubicTo(
                    x(145f), y(346f),
                    x(191f), y(475f),
                    x(129f), y(585f)
                )
            }
            drawPath(
                path = shadowPath,
                color = softShadow.copy(alpha = 0.08f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = 22f * designScale,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                )
            )
            drawPath(
                path = shadowPath,
                color = softShadow.copy(alpha = 0.14f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = 12f * designScale,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                )
            )

            // Upper spindle / counterweight stem.
            drawRoundRect(
                color = Color(0xFF161616),
                topLeft = point(139f, 18f),
                size = androidx.compose.ui.geometry.Size(
                    23f * designScale,
                    63f * designScale
                ),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(
                    5f * designScale,
                    5f * designScale
                )
            )
            drawRoundRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFE7EBED),
                        Color(0xFF8B9093),
                        Color(0xFFF8F9F9),
                        Color(0xFF6E7477)
                    ),
                    start = point(139f, 0f),
                    end = point(162f, 0f)
                ),
                topLeft = point(143f, 17f),
                size = androidx.compose.ui.geometry.Size(
                    15f * designScale,
                    58f * designScale
                ),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(
                    3f * designScale,
                    3f * designScale
                )
            )
            drawRoundRect(
                color = Color(0xFF111111),
                topLeft = point(147f, 20f),
                size = androidx.compose.ui.geometry.Size(
                    5f * designScale,
                    50f * designScale
                ),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(
                    2f * designScale,
                    2f * designScale
                )
            )

            // Brushed-metal counterweight housing.
            val housingPath = androidx.compose.ui.graphics.Path().apply {
                moveTo(x(111f), y(51f))
                cubicTo(x(119f), y(46f), x(167f), y(46f), x(174f), y(52f))
                lineTo(x(170f), y(103f))
                cubicTo(x(166f), y(113f), x(119f), y(113f), x(115f), y(103f))
                close()
            }
            drawPath(
                path = housingPath,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFF2F4F5),
                        Color(0xFFB7BEC2),
                        Color(0xFFF9FAFA),
                        Color(0xFF8F979A)
                    ),
                    start = point(110f, 55f),
                    end = point(176f, 55f)
                )
            )
            drawPath(
                path = housingPath,
                color = Color.Black.copy(alpha = 0.18f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = 1.7f * designScale
                )
            )
            drawRect(
                color = Color(0xFF34383A),
                topLeft = point(129f, 52f),
                size = androidx.compose.ui.geometry.Size(
                    8f * designScale,
                    53f * designScale
                )
            )
            drawRect(
                color = Color.White.copy(alpha = 0.55f),
                topLeft = point(151f, 52f),
                size = androidx.compose.ui.geometry.Size(
                    6f * designScale,
                    48f * designScale
                )
            )

            // Bearing collar.
            drawRoundRect(
                color = Color(0xFF9FA5A7),
                topLeft = point(128f, 100f),
                size = androidx.compose.ui.geometry.Size(
                    46f * designScale,
                    28f * designScale
                ),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(
                    6f * designScale,
                    6f * designScale
                )
            )
            drawRect(
                color = Color(0xFF4C5052),
                topLeft = point(133f, 100f),
                size = androidx.compose.ui.geometry.Size(
                    31f * designScale,
                    4f * designScale
                )
            )
            drawLine(
                color = Color.White.copy(alpha = 0.7f),
                start = point(134f, 107f),
                end = point(165f, 107f),
                strokeWidth = 1.4f * designScale
            )

            // Main black bearing wheel.
            drawCircle(
                color = Color(0xFF080808),
                radius = 48f * designScale,
                center = point(151f, 171f)
            )
            drawCircle(
                color = Color(0xFF1D1D1D),
                radius = 43f * designScale,
                center = point(151f, 171f)
            )
            drawCircle(
                color = Color(0xFFBFC4C6),
                radius = 31f * designScale,
                center = point(151f, 171f)
            )
            drawCircle(
                color = Color(0xFF111111),
                radius = 25f * designScale,
                center = point(151f, 171f)
            )
            drawCircle(
                color = Color(0xFFE4E6E7),
                radius = 18f * designScale,
                center = point(151f, 171f)
            )
            drawCircle(
                color = Color(0xFF2A2A2A),
                radius = 12f * designScale,
                center = point(151f, 171f)
            )
            drawCircle(
                color = Color(0xFFD7DADB),
                radius = 8f * designScale,
                center = point(151f, 171f)
            )

            // Small hardware details around the bearing.
            drawCircle(
                color = Color(0xFFCCB26B),
                radius = 7f * designScale,
                center = point(99f, 168f)
            )
            drawCircle(
                color = Color(0xFFE2CF91),
                radius = 4f * designScale,
                center = point(99f, 168f)
            )
            drawCircle(
                color = Color(0xFFCCB26B),
                radius = 7f * designScale,
                center = point(202f, 192f)
            )
            drawCircle(
                color = Color(0xFFE2CF91),
                radius = 4f * designScale,
                center = point(202f, 192f)
            )
            drawCircle(
                color = Color(0xFF7F8587),
                radius = 3.2f * designScale,
                center = point(123f, 113f)
            )
            drawCircle(
                color = Color(0xFF7F8587),
                radius = 3.2f * designScale,
                center = point(162f, 114f)
            )

            // Black mounting block below the bearing.
            drawRoundRect(
                color = Color(0xFF101010),
                topLeft = point(137f, 214f),
                size = androidx.compose.ui.geometry.Size(
                    29f * designScale,
                    52f * designScale
                ),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(
                    2f * designScale,
                    2f * designScale
                )
            )
            drawRect(
                color = Color(0xFF191919),
                topLeft = point(140f, 214f),
                size = androidx.compose.ui.geometry.Size(
                    23f * designScale,
                    50f * designScale
                )
            )

            // Long polished S-shaped tonearm.
            val armPath = androidx.compose.ui.graphics.Path().apply {
                moveTo(x(150f), y(255f))
                cubicTo(
                    x(148f), y(328f),
                    x(175f), y(445f),
                    x(176f), y(495f)
                )
                cubicTo(
                    x(177f), y(540f),
                    x(151f), y(565f),
                    x(130f), y(596f)
                )
            }

            // Dark outer edge.
            drawPath(
                path = armPath,
                color = Color(0xFF5A5E60),
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = 14f * designScale,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round,
                    join = androidx.compose.ui.graphics.StrokeJoin.Round
                )
            )

            // Main chrome tube.
            drawPath(
                path = armPath,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF63696B),
                        Color(0xFFF7F8F8),
                        Color(0xFFB8BEC0),
                        Color(0xFFFDFDFD),
                        Color(0xFF666B6D)
                    ),
                    start = point(128f, 255f),
                    end = point(191f, 540f)
                ),
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = 10.5f * designScale,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round,
                    join = androidx.compose.ui.graphics.StrokeJoin.Round
                )
            )
            drawPath(
                path = armPath,
                color = Color.White.copy(alpha = 0.72f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = 1.9f * designScale,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                )
            )

            // Cartridge neck.
            drawRoundRect(
                color = Color(0xFF2B2F30),
                topLeft = point(119f, 573f),
                size = androidx.compose.ui.geometry.Size(
                    31f * designScale,
                    26f * designScale
                ),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(
                    4f * designScale,
                    4f * designScale
                )
            )

            // White cartridge body, angled down-left.
            val cartridgePath = androidx.compose.ui.graphics.Path().apply {
                moveTo(x(128f), y(588f))
                lineTo(x(164f), y(603f))
                lineTo(x(111f), y(703f))
                lineTo(x(75f), y(686f))
                close()
            }
            drawPath(
                path = cartridgePath,
                color = Color(0xFFE9ECEC)
            )
            drawPath(
                path = cartridgePath,
                color = Color(0xFF2B2E2F),
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = 3f * designScale
                )
            )

            // Cartridge face / beveled edge.
            val cartridgeEdge = androidx.compose.ui.graphics.Path().apply {
                moveTo(x(75f), y(686f))
                lineTo(x(111f), y(703f))
                lineTo(x(106f), y(714f))
                lineTo(x(69f), y(697f))
                close()
            }
            drawPath(
                path = cartridgeEdge,
                color = Color(0xFF222526)
            )

            // Vent / screw dots on the cartridge face.
            val holeRows = listOf(
                listOf(100f, 621f, 93f, 635f, 86f, 649f, 79f, 663f),
                listOf(110f, 625f, 103f, 639f, 96f, 653f, 89f, 667f),
                listOf(120f, 629f, 113f, 643f, 106f, 657f, 99f, 671f)
            )
            holeRows.forEach { row ->
                var i = 0
                while (i < row.size) {
                    drawCircle(
                        color = Color(0xFF252829),
                        radius = 3.1f * designScale,
                        center = point(row[i], row[i + 1])
                    )
                    i += 2
                }
            }

            // Stylus tip.
            drawLine(
                color = Color(0xFF7B8082),
                start = point(130f, 704f),
                end = point(173f, 721f),
                strokeWidth = 5f * designScale,
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )
            drawLine(
                color = Color(0xFFD5D8D9),
                start = point(130f, 704f),
                end = point(173f, 721f),
                strokeWidth = 2.2f * designScale,
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )
            drawCircle(
                color = Color(0xFF505456),
                radius = 3.2f * designScale,
                center = point(173f, 721f)
            )
        }
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
private fun ImmersiveArtwork(
    song: Song?,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(Color(0xFF121212))
    ) {
        ArtworkView(
            song = song,
            maxSizePx = 1_600,
            modifier = Modifier.fillMaxSize()
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0f to Color.Black.copy(alpha = 0.48f),
                            0.16f to Color.Black.copy(alpha = 0.30f),
                            0.34f to Color.Transparent,
                            0.55f to Color.Transparent,
                            0.72f to Color.Black.copy(alpha = 0.25f),
                            0.88f to Color.Black.copy(alpha = 0.60f),
                            1f to Color.Black.copy(alpha = 0.78f)
                        )
                    )
                )
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
private fun LibrarySongListRow(
    song: Song,
    selected: Boolean,
    onPlay: (Song) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var codecInfo by remember(song.uri) { mutableStateOf<AudioCodecInfo?>(null) }

    LaunchedEffect(context, song.uri) {
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