package com.yohandeku32.nusamusic.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Touch-down blocks following before drag slop; an old idle timer cannot resume a newer gesture. */
internal class LyricsInteractionState {
    var touching by mutableStateOf(false)
        private set
    var dragging by mutableStateOf(false)
        private set
    var browsing by mutableStateOf(false)
        private set
    var generation by mutableIntStateOf(0)
        private set
    val canFollow: Boolean get() = !touching && !dragging && !browsing

    fun touchDown() { touching = true; browse() }
    fun touchUp() { touching = false }
    fun drag(value: Boolean) { dragging = value; if (value) browse() }
    fun browse() { browsing = true; generation++ }
    fun resume(token: Int) { if (token == generation && !touching && !dragging) browsing = false }
    fun cancelledAuto(token: Int) { if (token == generation) browse() }
    fun seek() { if (!touching && !dragging) { generation++; browsing = false } }
}
