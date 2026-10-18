package com.yohandeku32.nusamusic.ui

import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeSyllable

/** Only authored units reach the karaoke painter. */
internal fun syllableProgress(syllable: KaraokeSyllable, now: Int): Float = when {
    now < syllable.start -> 0f
    now >= syllable.end -> 1f
    else -> ((now.toLong() - syllable.start).toFloat() / (syllable.end.toLong() - syllable.start).coerceAtLeast(1)).coerceIn(0f, 1f)
}
