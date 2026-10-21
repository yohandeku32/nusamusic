package com.yohandeku32.nusamusic.ui

/**
 * Audio-time sampling for syllable highlight and animation.
 * Ignore only tiny backward jitter within the same playback epoch. An explicit
 * Media3 position discontinuity changes the revision and reanchors at once,
 * even for a seek of one millisecond.
 */
internal class LyricsFrameTimeSmoother {
    private var revision = Int.MIN_VALUE
    private var lastSample = Int.MIN_VALUE

    fun sample(positionMs: Int, playbackRevision: Int): Int {
        if (playbackRevision != revision) {
            revision = playbackRevision
            lastSample = positionMs
            return positionMs
        }
        val stable = if (lastSample != Int.MIN_VALUE &&
            positionMs < lastSample && lastSample.toLong() - positionMs <= 24L
        ) lastSample else positionMs
        lastSample = stable
        return stable
    }
}
