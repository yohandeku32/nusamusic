package com.yohandeku32.nusamusic.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.yohandeku32.nusamusic.model.Song

internal data class CdDiscTransfer(val token: Long, val index: Int, val song: Song, val previousSongId: Long?)
internal enum class CdDiscTransferPhase { OPENING, FLYING, SEATING, ARRIVED }

/** A replaced or cancelled insertion cannot complete a newer selection. */
internal class CdDiscTransferState {
    var request by mutableStateOf<CdDiscTransfer?>(null)
        private set
    var phase by mutableStateOf(CdDiscTransferPhase.OPENING)
        private set
    private var token = 0L

    fun begin(index: Int, song: Song, previousSongId: Long? = null): CdDiscTransfer {
        return CdDiscTransfer(++token, index, song, previousSongId).also {
            request = it
            phase = CdDiscTransferPhase.OPENING
        }
    }

    fun fly(item: CdDiscTransfer): Boolean {
        if (request != item || phase != CdDiscTransferPhase.OPENING) return false
        phase = CdDiscTransferPhase.FLYING
        return true
    }

    fun finish(item: CdDiscTransfer) {
        if (request == item) request = null
    }

    fun seat(item: CdDiscTransfer): Boolean {
        if (request != item || phase != CdDiscTransferPhase.FLYING) return false
        phase = CdDiscTransferPhase.SEATING
        return true
    }

    /** Release playback once, after the flight and stationary handoff finish. */
    fun arrive(item: CdDiscTransfer): Song? {
        if (request != item || phase != CdDiscTransferPhase.SEATING) return null
        phase = CdDiscTransferPhase.ARRIVED
        return item.song
    }
}
