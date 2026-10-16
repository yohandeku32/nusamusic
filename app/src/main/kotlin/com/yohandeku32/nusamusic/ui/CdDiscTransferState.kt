package com.yohandeku32.nusamusic.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.yohandeku32.nusamusic.model.Song

internal enum class CdDiscTransferDirection { INSERT, RETURN }
internal data class CdDiscTransfer(
    val token: Long, val index: Int, val song: Song, val previousSongId: Long?,
    val direction: CdDiscTransferDirection = CdDiscTransferDirection.INSERT,
    val resumePlayback: Boolean = false
)
internal enum class CdDiscTransferPhase { OPENING, FLYING, SEATING, ARRIVED, PREPARING_RETURN, RETURNING, CLOSING }

/** A replaced or cancelled insertion cannot complete a newer selection. */
internal class CdDiscTransferState {
    var request by mutableStateOf<CdDiscTransfer?>(null)
        private set
    var phase by mutableStateOf(CdDiscTransferPhase.OPENING)
        private set
    private var token = 0L
    var storedSongId by mutableStateOf<Long?>(null)
        private set

    fun begin(index: Int, song: Song, previousSongId: Long? = null): CdDiscTransfer {
        return CdDiscTransfer(++token, index, song, previousSongId,
            resumePlayback = previousSongId == song.id && storedSongId == song.id).also {
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
        storedSongId = null
        return item.song
    }

    fun beginReturn(index: Int, song: Song): CdDiscTransfer? {
        if (request != null || storedSongId == song.id) return null
        return CdDiscTransfer(++token, index, song, song.id, CdDiscTransferDirection.RETURN).also {
            request = it
            phase = CdDiscTransferPhase.PREPARING_RETURN
        }
    }

    fun returnFlight(item: CdDiscTransfer): Boolean {
        if (request != item || phase != CdDiscTransferPhase.PREPARING_RETURN) return false
        phase = CdDiscTransferPhase.RETURNING
        return true
    }

    fun close(item: CdDiscTransfer): Boolean {
        if (request != item || phase != CdDiscTransferPhase.RETURNING) return false
        storedSongId = item.song.id
        phase = CdDiscTransferPhase.CLOSING
        return true
    }

    fun syncPlayer(songId: Long?, playing: Boolean) {
        if (request == null && (songId != storedSongId || playing)) storedSongId = null
    }
}
