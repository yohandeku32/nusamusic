package com.yohandeku32.nusamusic.ui

import androidx.collection.LruCache
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.yohandeku32.nusamusic.data.LyricsRepository
import com.yohandeku32.nusamusic.data.SongLyrics
import com.yohandeku32.nusamusic.model.Song
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class SongLyricsState {
    var data by mutableStateOf<SongLyrics?>(null)
    var loading by mutableStateOf(false)
    var attempted = false
    var job: Job? = null
    var generation = 0
    val list = LazyListState()
}

/** Bounded parsed cache; a new song cancels the previous reader. */
class ImmersiveLyricsState(private val repository: LyricsRepository) {
    private val entries = LruCache<String, SongLyricsState>(8)
    private var activeLoad: SongLyricsState? = null
    fun forSong(song: Song): SongLyricsState = entries.get(LyricsRepository.cacheKey(song))
        ?: SongLyricsState().also { entries.put(LyricsRepository.cacheKey(song), it) }

    fun load(song: Song, scope: CoroutineScope, retry: Boolean = false) {
        val state = forSong(song)
        activeLoad?.takeIf { it !== state }?.let { previous ->
            previous.generation++
            previous.job?.cancel()
            previous.loading = false
            previous.attempted = previous.data != null
        }
        activeLoad = state
        if (state.loading || (state.attempted && !retry)) return
        val generation = ++state.generation
        state.loading = true
        state.attempted = true
        state.job = scope.launch {
            try {
                val result = repository.load(song, retry)
                if (state.generation == generation) state.data = result
            } catch (e: CancellationException) {
                if (state.generation == generation) state.attempted = state.data != null
                throw e
            }
            catch (_: Exception) { if (state.generation == generation) state.data = null }
            finally { if (state.generation == generation) state.loading = false }
        }
    }
}
