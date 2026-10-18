package com.yohandeku32.nusamusic.ui

/** Retained per measured glyph; sampling changes scalars, never animation jobs or timestamps. */
internal class LyricsExpressionController(
    private val startMs: Int,
    private val endMs: Int,
    private val profile: LyricsAdaptiveExpression
) {
    private val connection = profile.glowConnection(endMs)
    var glow = 0f
        private set
    var wave = 0f
        private set

    fun update(playbackMs: Int) {
        val phrase = profile.phraseAt(startMs, playbackMs)
        wave = profile.wave * phrase
        glow = profile.glow * profile.glowEnvelope(startMs, endMs, playbackMs, connection) *
            phrase * lyricBreathing(startMs, endMs, playbackMs)
    }
}
