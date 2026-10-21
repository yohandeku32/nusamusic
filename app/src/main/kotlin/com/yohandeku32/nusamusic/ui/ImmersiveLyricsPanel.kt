package com.yohandeku32.nusamusic.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
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
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Save
import com.yohandeku32.nusamusic.data.AudioLyricsEmbedder
import com.yohandeku32.nusamusic.data.SongLyrics
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.yohandeku32.nusamusic.model.Song
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive

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
    modifier: Modifier = Modifier,
    onBeforeEmbed: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var showEmbedConfirmation by remember { mutableStateOf(false) }
    var pendingEmbed by remember { mutableStateOf<PendingLyricsEmbed?>(null) }
    var embedBusy by remember { mutableStateOf(false) }
    val activeSong by rememberUpdatedState(song)
    val beforeEmbed by rememberUpdatedState(onBeforeEmbed)
    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { selected ->
        val candidate = pendingEmbed
        pendingEmbed = null
        if (selected != null && candidate != null && !embedBusy) {
            embedBusy = true
            scope.launch {
                try {
                    if (activeSong?.uri != candidate.song.uri) {
                        throw IllegalArgumentException(
                            "The playing song changed. Select Embed again."
                        )
                    }
                    AudioLyricsEmbedder(context.applicationContext).embed(
                        candidate.song, selected, candidate.lyrics,
                        beforeWrite = {
                            check(activeSong?.uri == candidate.song.uri) {
                                "The playing song changed. Try again."
                            }
                            beforeEmbed()
                        }
                    )
                    state.load(candidate.song, scope, retry = true)
                    Toast.makeText(context, "Lyrics embedded in MP3", Toast.LENGTH_LONG).show()
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    Toast.makeText(
                        context, error.message ?: "Unable to embed lyrics",
                        Toast.LENGTH_LONG
                    ).show()
                } finally {
                    embedBusy = false
                }
            }
        }
    }
    val entry = song?.let { state.forSong(it) }
    LaunchedEffect(song) { if (song != null) state.load(song, scope) }

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
            entry != null && (entry.loading || !entry.attempted) &&
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
                    data?.onlineNotice ?: "No synchronized lyrics found",
                    color = Color.White.copy(alpha = 0.76f),
                    fontSize = 16.sp
                )
                // A transient provider error should not require leaving the
                // lyrics page and finding Reload inside Settings.
                if (song != null) TextButton(
                    onClick = { state.load(song, scope, retry = true) },
                    enabled = entry?.loading != true
                ) {
                    Text("Try again", color = Color.White)
                }
            }
        }

        // Only remotely downloaded lyrics need embedding. Show one small
        // outlined 44dp touch target at the top-right of the Lyrics viewport.
        // Existing local-file lyrics have already been embedded and do not
        // need a second write action.
        if (song != null && data != null &&
            data.stored.content.isNotBlank() &&
            data.stored.source != "embedded"
        ) {
            IconButton(
                onClick = { showEmbedConfirmation = true },
                enabled = !embedBusy,
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
                        imageVector = Lucide.Save,
                        contentDescription = "Embed lyrics into the original audio file",
                        tint = Color.White,
                        modifier = Modifier.size(21.dp)
                    )
                }
            }
        }
    }

    if (showEmbedConfirmation && song != null) AlertDialog(
        onDismissRequest = { showEmbedConfirmation = false },
        title = { Text("Embed lyrics in audio") },
        text = {
            Text(
                "Choose the original MP3 file for this song. " +
                "NusaMusic verifies the file and makes a temporary backup " +
                "before writing the timed lyrics into its ID3 tags. " +
                "Other formats are not supported yet."
            )
        },
        confirmButton = {
            TextButton(onClick = {
                showEmbedConfirmation = false
                val existing = entry?.data
                if (existing != null && existing.stored.content.isNotBlank()) {
                    pendingEmbed = PendingLyricsEmbed(song, existing)
                    filePicker.launch(arrayOf("audio/*", "application/octet-stream"))
                }
            }) {
                Text("Choose MP3")
            }
        },
        dismissButton = {
            TextButton(onClick = { showEmbedConfirmation = false }) {
                Text("Cancel")
            }
        }
    )
}
