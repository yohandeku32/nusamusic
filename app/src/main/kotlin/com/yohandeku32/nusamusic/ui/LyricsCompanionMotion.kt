package com.yohandeku32.nusamusic.ui

import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeAlignment
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeSyllable
import com.yohandeku32.nusamusic.data.LyricsPronunciation
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer

internal fun Modifier.lyricsCompanionMotion(pose: () -> LyricsCompanionPose, amount: Float): Modifier = graphicsLayer {
    val value = pose()
    translationY = value.liftDp * density * amount
    scaleY = 1f + (value.scaleY - 1f) * amount
    transformOrigin = TransformOrigin(0f, 0.5f)
    clip = false
}

internal data class LyricsCompanionPose(
    val liftDp: Float = 0f,
    val scaleY: Float = 1f,
    val glow: Float = 0f
) {
    companion object { val Rest = LyricsCompanionPose() }
}

/** A partial annotation keeps the shaped fallback; gaps never acquire vocal timing. */
internal fun pronunciationKaraokeLine(value: LyricsPronunciation): KaraokeLine? {
    if (value.words.isEmpty()) return null
    var cursor = 0
    val syllables = ArrayList<KaraokeSyllable>()
    for ((index, word) in value.words.withIndex()) {
        val range = word.range
        if (range.isEmpty() || range.first < cursor || range.last >= value.text.length ||
            word.start < 0 || word.end <= word.start ||
            value.text.substring(cursor, range.first).any { !it.isWhitespace() }) return null
        val next = value.words.getOrNull(index + 1)?.range?.first ?: value.text.length
        if (next <= range.last || next > value.text.length ||
            value.text.substring(range.last + 1, next).any { !it.isWhitespace() }) return null
        syllables += KaraokeSyllable(value.text.substring(cursor, next), word.start, word.end)
        cursor = next
    }
    if (cursor != value.text.length) return null
    return KaraokeLine(syllables, null, false, KaraokeAlignment.Unspecified,
        syllables.minOf { it.start }, syllables.maxOf { it.end })
}
