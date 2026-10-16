package com.yohandeku32.nusamusic.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.yohandeku32.nusamusic.model.Song

internal data class CdDiscTransfer(val token: Long, val index: Int, val song: Song)
internal enum class CdDiscTransferPhase { OPENING, FLYING }

/** A replaced or cancelled insertion cannot complete a newer selection. */
internal class CdDiscTransferState {
    var request by mutableStateOf<CdDiscTransfer?>(null)
        private set
    var phase by mutableStateOf(CdDiscTransferPhase.OPENING)
        private set
    private var token = 0L

    fun begin(index: Int, song: Song): CdDiscTransfer {
        return CdDiscTransfer(++token, index, song).also {
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
}
