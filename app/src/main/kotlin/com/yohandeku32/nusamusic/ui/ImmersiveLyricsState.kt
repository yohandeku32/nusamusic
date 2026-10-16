package com.yohandeku32.nusamusic.ui

import android.net.Uri
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
    var failed by mutableStateOf(false)
    var importFailed by mutableStateOf(false)
    var attempted = false
    var job: Job? = null
    var generation = 0
    var importing = false
    val list = LazyListState()
}

/** Owned above the pager so swiping away does not cancel a load or lose scroll state. */
class ImmersiveLyricsState(private val repository: LyricsRepository) {
    private val entries = LruCache<String, SongLyricsState>(8)
    private var activeLoad: SongLyricsState? = null
    fun forSong(song: Song): SongLyricsState = entries.get(LyricsRepository.cacheKey(song))
        ?: SongLyricsState().also { entries.put(LyricsRepository.cacheKey(song), it) }

    fun load(song: Song, scope: CoroutineScope, retry: Boolean = false) {
        val state = forSong(song)
        activeLoad?.takeIf { it !== state && it.loading && !it.importing }?.let { previous ->
            previous.generation++
            previous.job?.cancel()
            previous.loading = false
            previous.attempted = previous.data != null
        }
        activeLoad = state
        if (state.loading || (state.attempted && !retry)) return
        val generation = ++state.generation
        state.loading = true
        state.failed = false
        state.attempted = true
        state.job = scope.launch {
            try { state.data = repository.load(song, retry) }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { if (state.generation == generation) state.failed = true }
            finally { if (state.generation == generation) state.loading = false }
        }
    }

    fun import(song: Song, uri: Uri, scope: CoroutineScope) {
        val state = forSong(song)
        val previousImport = state.job.takeIf { state.importing }
        state.job?.cancel()
        val generation = ++state.generation
        state.importFailed = false
        state.importing = true
        state.loading = true
        state.job = scope.launch {
            try {
                // Serialize rapid import replacements, without waiting on an old network lookup.
                previousImport?.join()
                state.data = repository.import(song, uri)
                state.attempted = true
                state.failed = false
                state.list.scrollToItem(0)
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { if (state.generation == generation) state.importFailed = true }
            finally {
                if (state.generation == generation) {
                    state.loading = false
                    state.importing = false
                }
            }
        }
    }
}
