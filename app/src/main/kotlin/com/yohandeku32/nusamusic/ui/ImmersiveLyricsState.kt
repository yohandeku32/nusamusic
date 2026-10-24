package com.yohandeku32.nusamusic.ui

import androidx.collection.LruCache
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.yohandeku32.nusamusic.data.LyricsRepository
import com.yohandeku32.nusamusic.data.SongLyrics
import com.yohandeku32.nusamusic.data.lyricQuality
import com.yohandeku32.nusamusic.data.hasBackingVocals
import com.yohandeku32.nusamusic.model.Song
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Save should also upgrade an existing embedded lyric when a higher-quality
 * online syllable version is available. Extra authored backing vocals are
 * also an upgrade even when the main karaoke quality is already equal.
 */
internal fun lyricsNeedEmbedding(
    displayed: SongLyrics?,
    embeddedQuality: Int,
    embeddedHasBackingVocals: Boolean = false
): Boolean =
    displayed != null &&
        displayed.stored.source != "embedded" &&
        displayed.stored.content.isNotBlank() &&
        (lyricQuality(displayed) > embeddedQuality ||
            (lyricQuality(displayed) >= embeddedQuality &&
                hasBackingVocals(displayed) && !embeddedHasBackingVocals))

class SongLyricsState {
    var data by mutableStateOf<SongLyrics?>(null)
    var embeddedLyricsQuality by mutableIntStateOf(0)
    var embeddedHasBackingVocals by mutableStateOf(false)
    // A completed, verified write must not briefly re-show Save while Android's
    // media index continues to report the earlier version of this audio file.
    var confirmedEmbeddedQuality = 0
    var loading by mutableStateOf(false)
    var attempted = false
    var lastOnlineEnabled: Boolean? = null
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

    /** Called only after AudioLyricsEmbedder verified the original file write. */
    fun markEmbedded(song: Song, saved: SongLyrics) {
        forSong(song).apply {
            confirmedEmbeddedQuality = maxOf(confirmedEmbeddedQuality, lyricQuality(saved))
            embeddedLyricsQuality = maxOf(embeddedLyricsQuality, confirmedEmbeddedQuality)
            embeddedHasBackingVocals = embeddedHasBackingVocals || hasBackingVocals(saved)
        }
    }

    fun load(
        song: Song,
        scope: CoroutineScope,
        retry: Boolean = false,
        onlineEnabled: Boolean = true
    ) {
        val state = forSong(song)
        val settingChanged = state.lastOnlineEnabled != null &&
            state.lastOnlineEnabled != onlineEnabled
        val reload = retry || settingChanged
        state.lastOnlineEnabled = onlineEnabled
        // Offline means that a previously displayed online-only result must
        // not linger until the next song or until a local metadata read finishes.
        if (settingChanged && !onlineEnabled &&
            state.data?.stored?.source !in listOf("embedded", "saved-in-app")) {
            state.data = null
        }
        activeLoad?.takeIf { it !== state }?.let { previous ->
            previous.generation++
            previous.job?.cancel()
            previous.loading = false
            previous.attempted = previous.data != null
        }
        activeLoad = state
        // Metadata updates from another tag editor supersede an older read.
        // In particular, do not drop forced refreshes just because the
        // previous request is still reading the original audio file.
        if (state.loading && reload) {
            state.generation++
            state.job?.cancel()
            state.loading = false
        }
        if (state.loading || (state.attempted && !reload)) return
        val generation = ++state.generation
        state.loading = true
        state.attempted = true
        state.job = scope.launch {
            try {
                val result = repository.load(song, reload, onlineEnabled) { local ->
                    // Keep the embedded version's actual timing quality
                    // separate from the online version chosen for display.
                    // A line-synced file (2) can upgrade to syllable TTML (3).
                    if (state.generation == generation) {
                        // A durable in-app Save is not yet an embedded
                        // metadata write. Keep the pending clock until the
                        // original audio has been verified after playback.
                        if (local.stored.source != "saved-in-app") {
                            state.embeddedLyricsQuality = maxOf(
                                lyricQuality(local), state.confirmedEmbeddedQuality
                            )
                            state.embeddedHasBackingVocals =
                                state.embeddedHasBackingVocals || hasBackingVocals(local)
                        }
                        state.data = local
                    }
                }
                if (state.generation == generation) state.data = result
            } catch (e: CancellationException) {
                if (state.generation == generation) state.attempted = state.data != null
                throw e
            } catch (_: Exception) {
                if (state.generation == generation) state.data = null
            } finally {
                if (state.generation == generation) state.loading = false
            }
        }
    }
}
