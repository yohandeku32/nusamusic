package com.yohandeku32.nusamusic.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.yohandeku32.nusamusic.model.Song
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * Separate IME window, outside Immersive's animated pager/Media3 Compose tree.
 * Only cheap title+artist rows: no audio metadata or album cover decoding.
 */
@Composable
internal fun ImmersiveSearchDialog(
    songs: List<Song>,
    initialQuery: String,
    lightMode: Boolean = false,
    onDismiss: (String) -> Unit,
    onPlay: (Song, List<Song>) -> Unit
) {
    val colors = immersiveColors(lightMode)
    val black = colors.background
    val white = colors.primaryText
    val secondary = colors.secondaryText
    val surface = colors.surface
    val keyboard = LocalSoftwareKeyboardController.current
    val focus = LocalFocusManager.current
    val requester = remember { FocusRequester() }
    var query by remember { mutableStateOf(initialQuery) }
    var matches by remember(songs) { mutableStateOf(songs) }
    val dismissNow by rememberUpdatedState(newValue = {
        keyboard?.hide()
        focus.clearFocus(force = true)
        onDismiss(query)
    })

    // Prevent a dialog text field from restoring Gboard above Now Playing
    // when the application is minimized and later reopened.
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                dismissNow()
            }
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(songs, query) {
        if (query.isNotBlank()) delay(120L)
        val trimmed = query.trim()
        matches = withContext(Dispatchers.Default) {
            val filtered = if (trimmed.isBlank()) songs else songs.filter {
                it.title.contains(trimmed, ignoreCase = true) ||
                    it.artist.contains(trimmed, ignoreCase = true) ||
                    it.album.contains(trimmed, ignoreCase = true)
            }
            filtered.sortedBy { it.title.lowercase(Locale.ROOT) }
        }
    }

    Dialog(
        onDismissRequest = { dismissNow() },
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        LaunchedEffect(Unit) {
            withFrameNanos { }
            requester.requestFocus()
            keyboard?.show()
        }
        Column(
            Modifier.fillMaxSize().background(black)
                .windowInsetsPadding(WindowInsets.safeDrawing).imePadding()
                // Close search on horizontal swipes without consuming them.
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown(
                            requireUnconsumed = false, pass = PointerEventPass.Initial
                        )
                        val slop = viewConfiguration.touchSlop
                        var dx = 0f
                        var dy = 0f
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) break
                            val delta = change.position - change.previousPosition
                            dx += delta.x
                            dy += delta.y
                            if (kotlin.math.abs(dx) > slop &&
                                kotlin.math.abs(dx) > kotlin.math.abs(dy) * 1.15f
                            ) {
                                dismissNow()
                                break
                            }
                        }
                    }
                }
        ) {
            Row(
                Modifier.fillMaxWidth().height(64.dp)
                    .padding(start = 12.dp, end = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { dismissNow() }) {
                    Icon(Icons.Rounded.ArrowBack, libraryText("Tutup pencarian", "Close search"), tint = white)
                }
                Box(
                    Modifier.weight(1f).height(48.dp)
                        .clip(RoundedCornerShape(15.dp)).background(surface),
                    contentAlignment = Alignment.CenterStart
                ) {
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        textStyle = TextStyle(color = white, fontSize = 14.sp),
                        cursorBrush = SolidColor(white),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Search
                        ),
                        keyboardActions = KeyboardActions(onSearch = {
                            keyboard?.hide()
                            focus.clearFocus(force = true)
                        }),
                        decorationBox = { inner ->
                            Row(
                                Modifier.fillMaxSize().padding(horizontal = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Rounded.Search, null, tint = secondary,
                                    modifier = Modifier.size(19.dp))
                                Spacer(Modifier.width(9.dp))
                                Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                                    if (query.isEmpty()) {
                                        Text(libraryText("Cari lagu, artis, album", "Search songs, artists, albums"),
                                            color = secondary, fontSize = 13.sp,
                                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                    inner()
                                }
                                if (query.isNotEmpty()) {
                                    IconButton(onClick = { query = "" }) {
                                        Icon(Icons.Rounded.Close, libraryText("Hapus pencarian", "Clear search"), tint = secondary,
                                            modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().focusRequester(requester)
                    )
                }
            }
            Text(
                librarySongCount(matches.size),
                color = secondary, fontSize = 12.sp,
                modifier = Modifier.padding(start = 24.dp, top = 12.dp, bottom = 8.dp)
            )
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 20.dp)
            ) {
                items(matches, key = { it.id }, contentType = { "search-result" }) { song ->
                    Column(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                            .clickable {
                                val selected = matches
                                dismissNow()
                                onPlay(song, selected)
                            }
                            .padding(horizontal = 10.dp, vertical = 11.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(song.title, color = white, fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(song.artist.ifBlank { libraryText("Artis tidak diketahui", "Unknown artist") },
                            color = secondary, fontSize = 12.sp,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                if (matches.isEmpty()) {
                    item {
                        Text(libraryText("Tidak ada lagu yang cocok", "No matching songs"), color = secondary,
                            modifier = Modifier.fillMaxWidth().padding(top = 50.dp))
                    }
                }
            }
        }
    }
}
