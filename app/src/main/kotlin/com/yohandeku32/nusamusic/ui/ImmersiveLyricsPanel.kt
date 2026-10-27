package com.yohandeku32.nusamusic.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import com.yohandeku32.nusamusic.data.LyricsFolderAccess
import com.yohandeku32.nusamusic.data.LyricsPendingSaveQueue
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Save
import com.composables.icons.lucide.Clock3
import com.yohandeku32.nusamusic.data.SongLyrics
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.yohandeku32.nusamusic.model.Song
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

private fun lyricsText(id: String, en: String) =
    if (Locale.getDefault().language.equals("id", true)) id else en

/**
 * In-place Immersive lyric viewport. This is the existing Lyrics engine
 * (same state, Media3 audio clock, follow-scroll, timing, taps, backing
 * vocals, credits and cached text) without a separate modal window.
 *
 * Its parent owns the size and entrance/exit motion; no anchored bottom
 * sheet or modal scrim can cover the Now Playing playback controls. The
 * parent supplies the left margin; synced rows retain their 16dp padding
 * for the glow and plain lines use the same horizontal start.
 */
private data class PendingLyricsEmbed(val song: Song, val lyrics: SongLyrics)

@Composable
fun ImmersiveLyricsPanel(
    song: Song?,
    positionMs: State<Long>,
    readPlaybackPosition: () -> Long,
    readPlaybackEpoch: () -> Int,
    isPlaying: Boolean,
    playbackEpoch: Int,
    durationMs: Long,
    state: ImmersiveLyricsState,
    onSeek: (Long) -> Unit,
    fontSize: Float,
    onlineLyricsEnabled: Boolean,
    modifier: Modifier = Modifier,
    onQueueLyrics: suspend (Song, SongLyrics) -> Unit,
    isLyricsQueued: (Song) -> Boolean
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val folderAccess = remember(context) { LyricsFolderAccess(context.applicationContext) }
    var pendingFolder by remember { mutableStateOf<PendingLyricsEmbed?>(null) }
    var folderNeedsReview by remember { mutableStateOf<PendingLyricsEmbed?>(null) }
    var embedBusy by remember { mutableStateOf(false) }
    val activeSong by rememberUpdatedState(song)

    suspend fun writeLyrics(snapshot: PendingLyricsEmbed) {
        check(activeSong?.uri == snapshot.song.uri) {
            "The playing song changed. Open Lyrics and save it again."
        }
        // Rewriting Media3's active source can freeze playback or truncate
        // frames under its open descriptor. Persist this Save request only.
        // The MainActivity queue embeds it when a different song starts.
        onQueueLyrics(snapshot.song, snapshot.lyrics)
        // The original file is untouched while Media3 reads it, but the
        // authored lyrics are immediately durable and visible offline.
        state.load(snapshot.song, scope, retry = true, onlineEnabled = onlineLyricsEnabled)
        Toast.makeText(context,
            lyricsText(
                "Menunggu penulisan metadata. Putar lagu lain agar berkas ini dapat ditulis dengan aman.",
                "Waiting to write metadata. Switch to another song so this file can be written safely."
            ),
            Toast.LENGTH_LONG).show()
    }

    val folderPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        val snapshot = pendingFolder
        pendingFolder = null
        if (uri == null || snapshot == null) {
            embedBusy = false
        } else {
            scope.launch {
                try {
                    folderAccess.rememberFolder(uri)
                    check(activeSong?.uri == snapshot.song.uri) {
                        "The playing song changed. Save its lyrics again."
                    }
                    // No expensive SAF traversal or whole-song hashing on
                    // the currently-playing audio source.
                    writeLyrics(snapshot)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    Toast.makeText(context, error.message ?: "Unable to embed lyrics",
                        Toast.LENGTH_LONG).show()
                } finally {
                    embedBusy = false
                }
            }
        }
    }

    fun saveWithFolderAccess(snapshot: PendingLyricsEmbed) {
        if (embedBusy) return
        embedBusy = true
        scope.launch {
            try {
                check(activeSong?.uri == snapshot.song.uri) {
                    "The playing song changed. Save its lyrics again."
                }
                folderAccess.requireSupportedSong(snapshot.song)
                if (withContext(Dispatchers.IO) {
                        LyricsPendingSaveQueue.needsFolderReview(context, snapshot.song.id)
                    }) {
                    folderNeedsReview = snapshot
                } else if (folderAccess.authorizedTrees().isEmpty()) {
                    // One-time parent folder permission. Reading the original
                    // audio and writing metadata happen after track transition.
                    pendingFolder = snapshot
                    folderPicker.launch(null)
                } else {
                    writeLyrics(snapshot)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                // An unavailable folder picker should not leave Save disabled.
                pendingFolder = null
                Toast.makeText(context, error.message ?: "Unable to embed lyrics",
                    Toast.LENGTH_LONG).show()
            } finally {
                if (pendingFolder == null) embedBusy = false
            }
        }
    }
    val entry = song?.let { state.forSong(it) }
    val queuedForSong = song?.let { isLyricsQueued(it) } ?: false
    LaunchedEffect(song?.uri, queuedForSong, onlineLyricsEnabled) {
        if (song != null) {
            // The panel mounts anew whenever Lyrics is opened; refresh the
            // actual M4A metadata instead of restoring stale in-memory lines.
            // Keep the online cache for fast background-vocal upgrades.
            val stalePreview = entry?.data?.stored?.source == "saved-in-app"
            state.load(
                song, scope,
                retry = queuedForSong || stalePreview ||
                    (entry?.lastOnlineEnabled != null &&
                        entry.lastOnlineEnabled != onlineLyricsEnabled),
                onlineEnabled = onlineLyricsEnabled,
                refreshEmbeddedOnly = true
            )
        }
    }

    // Retain the original frame-clock driver. Pause freezes all renderers,
    // while Media3 seeks update the epoch and re-anchor lyric follow motion.
    val playback = remember(song?.uri) {
        LyricsPlaybackState(readPlaybackPosition(), readPlaybackEpoch())
    }
    val framePosition = playback.position
    val motion = playback.motion
    val readPosition by rememberUpdatedState(readPlaybackPosition)
    val readEpoch by rememberUpdatedState(readPlaybackEpoch)
    val playing by rememberUpdatedState(isPlaying)
    val epoch by rememberUpdatedState(playbackEpoch)
    val positionSource by rememberUpdatedState(positionMs)
    val owner = LocalLifecycleOwner.current
    LaunchedEffect(song?.uri, owner) {
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            snapshotFlow { playing to epoch }.collectLatest { (running, _) ->
                playback.publish(readPosition(), readEpoch())
                if (running) while (isActive) withFrameNanos {
                    playback.publish(readPosition(), readEpoch())
                } else snapshotFlow { positionSource.value }.collect {
                    playback.publish(readPosition(), readEpoch())
                }
            }
        }
    }

    Box(modifier) {
        val data = entry?.data
        when {
            entry != null && !entry.localReadComplete && (entry.loading || !entry.attempted) &&
                data?.stored?.content.isNullOrBlank() ->
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center).size(24.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )

            data?.synced != null -> key(song?.uri, data.synced) {
                SyncedLyricsView(
                    data.synced,
                    framePosition,
                    true,
                    requireNotNull(entry).list,
                    motion,
                    onSeek = {
                        val target = it.start.toLong()
                            .coerceIn(0, maxOf(durationMs, song?.durationMs ?: 0))
                        onSeek(target)
                    },
                    fontSize = fontSize,
                    credits = com.yohandeku32.nusamusic.data.SongCredits(),
                    modifier = Modifier.fillMaxSize(),
                    inferredEnds = data.inferredEnds,
                    duetRightLines = data.duetRightLines
                )
            }

            data?.stored?.plain == true -> {
                val lines = remember(data) { data.stored.content.lines() }
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp)
                ) {
                    items(lines) { line ->
                        Text(
                            line,
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = fontSize.sp,
                            lineHeight = (fontSize * 1.25f).sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            else -> Column(
                Modifier.align(Alignment.Center).padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    data?.onlineNotice ?: if (entry?.loading == true && entry.localReadComplete) "Checking online lyrics…" else "No synchronized lyrics found",
                    color = Color.White.copy(alpha = 0.76f),
                    fontSize = 16.sp
                )
                // A transient provider error should not require leaving the
                // lyrics page and finding Reload inside Settings.
                if (song != null) TextButton(
                    onClick = {
                        state.load(song, scope, retry = true, onlineEnabled = onlineLyricsEnabled)
                    },
                    enabled = entry?.loading != true
                ) {
                    Text("Try again", color = Color.White)
                }
            }
        }

        val saveVisible = song != null && (queuedForSong || lyricsNeedEmbedding(
            data, entry?.embeddedLyricsQuality ?: 0,
            entry?.embeddedHasBackingVocals ?: false
        ))
        // Offer one-tap Save for a missing lyric OR a richer online version.
        if (saveVisible && song != null) {
            val queued = queuedForSong
            IconButton(
                onClick = {
                    data?.let { available ->
                        saveWithFolderAccess(PendingLyricsEmbed(song, available))
                    }
                },
                enabled = !embedBusy && !queued,
                modifier = Modifier.align(Alignment.TopEnd)
                    .padding(top = 2.dp, end = 16.dp)
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.42f))
            ) {
                if (embedBusy) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = if (queued) Lucide.Clock3 else Lucide.Save,
                        contentDescription = if (queued)
                            lyricsText("Menunggu penulisan metadata; putar lagu lain untuk menyelesaikan",
                                "Waiting to write metadata; switch songs to finish")
                        else lyricsText("Simpan lirik ke metadata lagu", "Save lyrics to song metadata"),
                        tint = Color.White,
                        modifier = Modifier.size(21.dp)
                    )
                }
            }
        }
    }


    if (folderNeedsReview != null) {
        AlertDialog(
            onDismissRequest = { folderNeedsReview = null },
            title = { Text(lyricsText("Tinjau akses folder musik", "Review music folder access")) },
            text = {
                Text(
                    lyricsText(
                        "Metadata belum berhasil ditulis. Pilih folder yang berisi lagu ini dengan akses tulis, lalu coba lagi. Aruna akan mengingat izin foldernya.",
                        "The metadata write did not finish. Choose the folder containing this song with write access and try again. Aruna will remember the folder permission."
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val snapshot = folderNeedsReview ?: return@TextButton
                    folderNeedsReview = null
                    pendingFolder = snapshot
                    embedBusy = true
                    try {
                        folderPicker.launch(null)
                    } catch (error: Exception) {
                        pendingFolder = null
                        embedBusy = false
                        Toast.makeText(context, error.message ?: "Cannot open folder picker",
                            Toast.LENGTH_LONG).show()
                    }
                }) { Text(lyricsText("Pilih folder musik", "Choose music folder")) }
            },
            dismissButton = {
                TextButton(onClick = { folderNeedsReview = null }) { Text(lyricsText("Batal", "Cancel")) }
            }
        )
    }
}
