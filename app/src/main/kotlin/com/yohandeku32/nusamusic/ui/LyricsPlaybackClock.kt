package com.yohandeku32.nusamusic.ui

import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** A retained time callback must read live state after an offscreen load or a return swipe. */
internal class LyricsPlaybackClock(position: State<Long>, visible: Boolean) {
    private var position by mutableStateOf(position)
    private var visible by mutableStateOf(visible)
    private var heldPosition by mutableIntStateOf(clamp(position.value))
    val read: () -> Int = { if (this.visible) clamp(this.position.value) else heldPosition }

    fun update(position: State<Long>, visible: Boolean) {
        if (!visible && (this.visible || this.position !== position)) heldPosition = clamp(position.value)
        this.position = position
        this.visible = visible
    }

    private fun clamp(value: Long) = value.coerceIn(0L, Int.MAX_VALUE.toLong()).toInt()
}
