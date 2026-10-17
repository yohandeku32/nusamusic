package com.yohandeku32.nusamusic.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mocharealm.accompanist.lyrics.ui.composable.lyrics.KaraokeLyricsView
import com.yohandeku32.nusamusic.data.ImmersiveArtworkCache
import com.yohandeku32.nusamusic.model.Song
import kotlinx.coroutines.CoroutineScope
import java.util.Locale

private fun lyricsText(id: String, en: String) = if (Locale.getDefault().language == "id") id else en

@Composable
fun ImmersiveLyricsPage(
    song: Song?,
    positionMs: State<Long>,
    durationMs: Long,
    isVisible: Boolean,
    state: ImmersiveLyricsState,
    scope: CoroutineScope,
    onBack: () -> Unit,
    onLibrary: () -> Unit,
    onSeek: (Long) -> Unit,
    controls: @Composable () -> Unit
) {
    val entry = song?.let { state.forSong(it) }
    var importTarget by remember { mutableStateOf<Song?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val target = importTarget
        importTarget = null
        if (uri != null && target != null) state.import(target, uri, scope)
    }
    fun chooseFile() {
        if (song == null) return
        importTarget = song
        // Many document providers label LRC/TTML as generic binary files.
        picker.launch(arrayOf("*/*"))
    }
    LaunchedEffect(song, isVisible) {
        if (isVisible && song != null) state.load(song, scope)
    }
    BackHandler(enabled = isVisible, onBack = onBack)
    val artwork = song?.let { ImmersiveArtworkCache.cached(it) }
    MaterialTheme(colorScheme = darkColorScheme(primary = Color.White, background = Color.Black, surface = Color(0xFF181818))) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            artwork?.let {
                Image(it.blur.asImageBitmap(), null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            }
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = .64f), Color.Black.copy(alpha = .5f), Color.Black.copy(alpha = .94f)))))
            Column(Modifier.fillMaxSize()) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, lyricsText("Kembali ke pemutar", "Back to player")) }
                    Column(Modifier.weight(1f).padding(horizontal = 8.dp)) {
                        Text(lyricsText("LIRIK", "LYRICS"), color = Color.White.copy(alpha = .6f), fontSize = 10.sp, letterSpacing = 2.sp)
                        Text(song?.title ?: lyricsText("Pilih lagu", "Choose a song"), fontWeight = FontWeight.SemiBold, fontSize = 18.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        song?.let { Text(it.artist, color = Color.White.copy(alpha = .65f), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                    }
                    IconButton(onClick = ::chooseFile, enabled = song != null) {
                        Icon(Icons.Default.FileOpen, lyricsText("Impor lirik TTML atau LRC", "Import TTML or LRC lyrics"))
                    }
                    IconButton(onClick = onLibrary) { Icon(Icons.AutoMirrored.Filled.QueueMusic, lyricsText("Daftar lagu", "Songs")) }
                }
                if (entry?.importFailed == true) Text(
                    lyricsText("File tidak dapat dibaca. Pilih lirik TTML/LRC dengan waktu yang valid.", "Cannot read this file. Choose TTML/LRC lyrics with valid timestamps."),
                    color = Color(0xFFFFC8BC), fontSize = 12.sp, modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp))
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    val data = entry?.data
                    when {
                        data?.synced != null -> key(song?.uri, data.synced) {
                            // Keep offscreen lyrics still; the activity polls faster only on this page.
                            val frozenTime = remember(isVisible) { positionMs.value.toInt() }
                            val clock = remember(positionMs, isVisible, frozenTime) {
                                { if (isVisible) positionMs.value.coerceIn(0, Int.MAX_VALUE.toLong()).toInt() else frozenTime }
                            }
                            KaraokeLyricsView(
                                listState = requireNotNull(entry).list,
                                lyrics = data.synced,
                                currentPosition = clock,
                                onLineClicked = { onSeek(it.start.toLong().coerceIn(0, durationMs.coerceAtLeast(song?.durationMs ?: 0))) },
                                onLinePressed = {},
                                normalLineTextStyle = TextStyle(fontSize = 32.sp, lineHeight = 40.sp, fontWeight = FontWeight.Bold),
                                accompanimentLineTextStyle = TextStyle(fontSize = 20.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold),
                                useBlurEffect = false,
                                offset = 64.dp,
                                modifier = Modifier.fillMaxSize().padding(horizontal = 26.dp)
                            )
                        }
                        data?.stored?.plain == true -> Text(data.stored.content,
                            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 28.dp, vertical = 24.dp),
                            color = Color.White, fontSize = 28.sp, lineHeight = 40.sp, fontWeight = FontWeight.Bold)
                        entry?.loading == true -> Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                            Spacer(Modifier.height(16.dp))
                            Text(lyricsText("Mencari lirik…", "Finding lyrics…"), color = Color.White.copy(alpha = .65f))
                        }
                        else -> Column(Modifier.align(Alignment.Center).padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Lyrics, null, tint = Color.White.copy(alpha = .35f), modifier = Modifier.size(54.dp))
                            Spacer(Modifier.height(18.dp))
                            Text(when {
                                song == null -> lyricsText("Putar lagu untuk melihat lirik", "Play a song to see its lyrics")
                                entry?.failed == true -> lyricsText("Lirik belum bisa dimuat", "Could not load lyrics")
                                data?.stored?.instrumental == true -> lyricsText("Lagu instrumental", "Instrumental track")
                                else -> lyricsText("Lirik belum tersedia", "Lyrics aren't available yet")
                            }, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                            if (song != null) {
                                Spacer(Modifier.height(10.dp))
                                Text(lyricsText("Tambahkan file TTML atau LRC untuk lagu ini.", "Add a TTML or LRC file for this song."),
                                    color = Color.White.copy(alpha = .65f), fontSize = 14.sp)
                                Spacer(Modifier.height(18.dp))
                                OutlinedButton(onClick = ::chooseFile, enabled = entry?.loading != true) { Text(lyricsText("Impor lirik", "Import lyrics")) }
                                TextButton(onClick = { state.load(song, scope, retry = true) }, enabled = entry?.loading != true) {
                                    Text(lyricsText("Cari lagi", "Search again"))
                                }
                            }
                        }
                    }
                }
                if (entry?.data != null && (entry.data!!.stored.content.isNotBlank() || entry.data!!.stored.instrumental)) {
                    val source = entry.data!!.stored.source
                    val uriHandler = LocalUriHandler.current
                    val label = when (source) {
                        "imported" -> lyricsText("Lirik tersimpan di perangkat", "Lyrics saved on device")
                        "local" -> lyricsText("File lirik lokal", "Local lyrics file")
                        else -> "LRCLIB"
                    } + if (entry.data!!.stored.plain) lyricsText(" · tanpa sinkronisasi", " · unsynced") else ""
                    Row(Modifier.fillMaxWidth().padding(horizontal = 22.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(label, Modifier.weight(1f), color = Color.White.copy(alpha = .5f), fontSize = 11.sp)
                        if (source == "LRCLIB") TextButton(onClick = { uriHandler.openUri("https://lrclib.net") }) { Text("LRCLIB ↗", fontSize = 11.sp) }
                    }
                }
                Column(Modifier.fillMaxWidth().padding(horizontal = 22.dp)) { controls() }
            }
        }
    }
}
