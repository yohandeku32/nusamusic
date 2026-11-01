package com.yohandeku32.nusamusic.ui

/**
 * A word/syllable shader never samples an earlier moment in the same
 * playback revision. Only an explicit seek/track discontinuity resets it.
 *
 * Media3's reported clock can be corrected by >24ms on real hardware.
 * A 24ms-only filter allowed a brief reverse replay ("tiny loop") on
 * sustained karaoke words and on the moving mountain glow.
 */
internal class LyricsFrameTimeSmoother {
    private var revision = Int.MIN_VALUE
    private var lastSample = Int.MIN_VALUE

    fun sample(positionMs: Int, playbackRevision: Int): Int {
        if (playbackRevision != revision) {
            revision = playbackRevision
            lastSample = positionMs
        } else if (positionMs > lastSample) {
            lastSample = positionMs
        }
        return lastSample
    }
}
