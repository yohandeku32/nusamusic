package com.yohandeku32.nusamusic.ui

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Artist navigation is local-first. Retain scroll position and selected album
 * across page swipes without initiating external portrait lookups or keeping
 * stale artist bitmaps in memory.
 */
class ImmersiveArtistPageState(val artist: String) {
    var selectedAlbumKey by mutableStateOf<String?>(null)
    val listState = LazyListState()
}
