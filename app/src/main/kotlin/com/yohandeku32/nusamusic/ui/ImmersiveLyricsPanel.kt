package com.yohandeku32.nusamusic.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yohandeku32.nusamusic.model.Song
import kotlinx.coroutines.CoroutineScope
import java.util.Locale

private fun lyricsText(id: String, en: String) = if (Locale.getDefault().language == "id") id else en

/** Transparent back face of the artwork: the player owns its background and controls. */
@Composable
fun ImmersiveLyricsPanel(
    song: Song?,
    positionMs: State<Long>,
    durationMs: Long,
    isVisible: Boolean,
    state: ImmersiveLyricsState,
    scope: CoroutineScope,
    onBack: () -> Unit,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val entry = song?.let { state.forSong(it) }
    var menuExpanded by remember { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current
    var importTarget by remember { mutableStateOf<Song?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val target = importTarget
        importTarget = null
        if (uri != null && target != null) state.import(target, uri, scope)
    }
    fun chooseFile() {
        if (song == null) return
        importTarget = song
        picker.launch(arrayOf("*/*"))
    }
    LaunchedEffect(song, isVisible) {
        if (isVisible && song != null) state.load(song, scope)
    }
    BackHandler(enabled = isVisible, onBack = onBack)
    MaterialTheme(colorScheme = darkColorScheme(primary = Color.White, onPrimary = Color.Black,
        onBackground = Color.White, surface = Color(0xFF202020), onSurface = Color.White,
        onSurfaceVariant = Color(0xFFCCCCCC))) {
        CompositionLocalProvider(LocalContentColor provides Color.White) {
            Column(modifier) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) { Icon(Icons.Default.Close, lyricsText("Tutup lirik", "Close lyrics"), tint = Color.White) }
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = ::chooseFile, enabled = song != null) {
                        Icon(Icons.Default.FileOpen, lyricsText("Impor lirik TTML atau LRC", "Import TTML or LRC lyrics"))
                    }
                    Box {
                        IconButton(onClick = { menuExpanded = true }) { Icon(Icons.Default.MoreVert, lyricsText("Opsi lirik", "Lyrics options")) }
                        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                            if (song != null) DropdownMenuItem(text = { Text(lyricsText("Cari ulang lirik", "Find lyrics again"), color = Color.White) },
                                enabled = entry?.loading != true,
                                onClick = { menuExpanded = false; state.load(song, scope, retry = true) })
                            if (entry?.data?.stored?.source == "LRCLIB") DropdownMenuItem(text = { Text("LRCLIB ↗", color = Color.White) },
                                onClick = { menuExpanded = false; uriHandler.openUri("https://lrclib.net") })
                        }
                    }
                }
                if (entry?.importFailed == true) Text(lyricsText("File lirik tidak valid", "Invalid lyrics file"),
                    color = Color(0xFFFFC8BC), fontSize = 12.sp, modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp))
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    val data = entry?.data
                    when {
                        data?.synced != null -> key(song?.uri, data.synced) {
                            SyncedLyricsView(
                                listState = requireNotNull(entry).list,
                                lyrics = data.synced,
                                position = positionMs,
                                visible = isVisible,
                                onSeek = { onSeek(it.start.toLong().coerceIn(0, durationMs.coerceAtLeast(song?.durationMs ?: 0))) },
                                modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp)
                            )
                        }
                        data?.stored?.plain == true -> Text(data.stored.content,
                            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 28.dp, vertical = 16.dp),
                            color = Color.White, fontSize = 28.sp, lineHeight = 40.sp, fontWeight = FontWeight.Bold)
                        entry?.loading == true -> CircularProgressIndicator(Modifier.align(Alignment.Center).size(24.dp), color = Color.White, strokeWidth = 2.dp)
                        else -> Column(Modifier.align(Alignment.Center).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(when {
                                song == null -> lyricsText("Pilih lagu", "Choose a song")
                                entry?.failed == true -> lyricsText("Lirik belum bisa dimuat", "Could not load lyrics")
                                data?.stored?.instrumental == true -> lyricsText("Instrumental", "Instrumental")
                                else -> lyricsText("Lirik belum tersedia", "Lyrics aren't available yet")
                            }, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                            if (song != null) {
                                Spacer(Modifier.height(12.dp))
                                OutlinedButton(onClick = ::chooseFile) { Text(lyricsText("Impor lirik", "Import lyrics"), color = Color.White) }
                            }
                        }
                    }
                }
            }
        }
    }
}
