package com.yohandeku32.nusamusic.data

/** Counts listening time, never playback position (seeking cannot create a play). */
class ListeningCounter {
    private var uri: String? = null
    private var lastTimeMs = 0L
    private var wasPlaying = false
    private var listenedMs = 0L
    private var counted = false

    fun reset() {
        uri = null
        wasPlaying = false
        listenedMs = 0L
        counted = false
    }

    fun sample(currentUri: String?, isPlaying: Boolean, nowMs: Long, durationMs: Long): String? {
        if (currentUri != uri) {
            reset()
            uri = currentUri
        } else if (wasPlaying) {
            listenedMs += (nowMs - lastTimeMs).coerceAtLeast(0L)
        }
        lastTimeMs = nowMs
        wasPlaying = isPlaying
        val threshold = if (durationMs > 0L) minOf(30_000L, durationMs / 2L).coerceAtLeast(1L) else 30_000L
        if (currentUri != null && !counted && listenedMs >= threshold) {
            counted = true
            return currentUri
        }
        return null
    }
}
