package com.yohandeku32.nusamusic.ui

import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.*
import com.yohandeku32.nusamusic.data.ArtistImageLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Owned by the app, so paging to the library cannot dispose the profile state. */
class ImmersiveArtistPageState(val artist: String) {
    var portrait by mutableStateOf<Bitmap?>(ArtistImageLoader.cached(artist, 768))
        private set
    var portraitLoading by mutableStateOf(false)
        private set
    var selectedAlbumKey by mutableStateOf<String?>(null)
    var bioExpanded by mutableStateOf(false)
    val listState = LazyListState()
    private var lastPortraitAttempt: Long? = null

    fun loadPortrait(context: Context, scope: CoroutineScope) {
        if (artist.isBlank() || portrait != null || portraitLoading) return
        val now = SystemClock.elapsedRealtime()
        if (lastPortraitAttempt?.let { now - it < 5 * 60_000L } == true) return
        lastPortraitAttempt = now
        portraitLoading = true
        // Use the parent's scope: a swipe away must not cancel an in-flight download.
        scope.launch {
            try {
                portrait = ArtistImageLoader.load(context, artist, maxSize = 768)
            } finally {
                portraitLoading = false
            }
        }
    }
}
