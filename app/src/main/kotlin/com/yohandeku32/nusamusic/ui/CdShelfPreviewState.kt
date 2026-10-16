package com.yohandeku32.nusamusic.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.yohandeku32.nusamusic.model.Song

internal data class CdShelfSelection(val index: Int, val song: Song, val originCenterX: Float)
internal enum class CdShelfPreviewPhase { CLOSED, OPENING, READY, RETURNING }

/** A preview never changes playback until a confirmed return has completed. */
internal class CdShelfPreviewState {
    var selection by mutableStateOf<CdShelfSelection?>(null)
        private set
    var phase by mutableStateOf(CdShelfPreviewPhase.CLOSED)
        private set
    private var playOnReturn = false

    fun show(item: CdShelfSelection): Boolean {
        if (selection != null) return false
        selection = item
        phase = CdShelfPreviewPhase.OPENING
        return true
    }

    fun ready(item: CdShelfSelection) {
        if (selection == item && phase == CdShelfPreviewPhase.OPENING) phase = CdShelfPreviewPhase.READY
    }

    fun beginReturn(item: CdShelfSelection, play: Boolean): Boolean {
        if (selection != item || phase == CdShelfPreviewPhase.RETURNING) return false
        if (play && phase != CdShelfPreviewPhase.READY) return false
        playOnReturn = play
        phase = CdShelfPreviewPhase.RETURNING
        return true
    }

    fun completeReturn(item: CdShelfSelection): Song? {
        if (selection != item || phase != CdShelfPreviewPhase.RETURNING) return null
        val song = item.song.takeIf { playOnReturn }
        discard()
        return song
    }

    fun discard() {
        selection = null
        phase = CdShelfPreviewPhase.CLOSED
        playOnReturn = false
    }
}
