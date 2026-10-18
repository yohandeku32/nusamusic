package com.yohandeku32.nusamusic.ui

/**
 * A sustained closing vowel carries the COMPLETE rendered phrase, including
 * earlier syllables of its word and wrapped rows. Existing audio-time carriers
 * supply one shared transform; the progressive fill remains per authored unit.
 * Prepared with the painter, never recreated at syllable boundaries.
 */
internal class LyricsClosingVocalMotion(
    timings: List<LyricsExpressionTiming>,
    expressions: List<LyricsAdaptiveExpression>,
    horizontalDp: Float
) {
    private val terminalIndex = timings.indices.maxWithOrNull(
        compareBy<Int> { timings[it].start }.thenBy { timings[it].end }
    )
    private val terminal = terminalIndex?.let { timings[it] }
    private val expression = terminalIndex?.let { expressions.getOrNull(it) }
    // Wait for all earlier units to finish, including overlapping timings.
    private val startMs = maxOf(terminal?.start ?: 0,
        timings.indices.filter { it != terminalIndex }.maxOfOrNull { timings[it].end } ?: 0)
    private val endMs = terminal?.end ?: startMs
    private val eligibility = if (terminal?.reliableEnd == false) 0f else
        minOf(expression?.breathing ?: 0f,
            LyricsAdaptiveExpression(endMs.toLong() - startMs).breathing)
    private val wave = LyricsFloatingMotion(horizontalDp)
    private val bubble = LyricsLongVocalMotion(
        terminal?.start ?: startMs, endMs, horizontalDp,
        expression ?: LyricsAdaptiveExpression(0)
    )

    var weight = 0f
        private set
    val localWeight get() = 1f - weight
    var scaleX = 1f
        private set
    var scaleY = 1f
        private set
    var offsetXDp = 0f
        private set
    var offsetYDp = 0f
        private set

    fun update(playbackMs: Int) {
        val profile = expression
        if (profile == null || eligibility <= 0f) {
            reset()
            return
        }
        weight = eligibility * profile.envelope(startMs, endMs, playbackMs) *
            profile.phraseAt(terminal?.start ?: startMs, playbackMs)
        if (weight <= 0f) {
            reset()
            return
        }
        wave.update(playbackMs, profile.wave)
        bubble.update(playbackMs, 1f)
        // Crossfade local motion into the group instead of doubling the lift
        // or scaling each syllable about a different pivot during a full hold.
        scaleX = 1f + (wave.scale * bubble.scaleX - 1f) * weight
        scaleY = 1f + (wave.scale * bubble.scaleY - 1f) * weight
        offsetXDp = bubble.offsetXDp * weight
        offsetYDp = (bubble.offsetYDp - wave.liftDp) * weight
    }

    private fun reset() {
        weight = 0f
        scaleX = 1f
        scaleY = 1f
        offsetXDp = 0f
        offsetYDp = 0f
    }
}
