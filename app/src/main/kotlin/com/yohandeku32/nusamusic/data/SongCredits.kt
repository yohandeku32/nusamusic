package com.yohandeku32.nusamusic.data

/** Authored roles remain separate; absence is represented by an empty list. */
data class SongCredits(val composers: List<String> = emptyList(), val lyricists: List<String> = emptyList()) {
    val isEmpty: Boolean get() = composers.isEmpty() && lyricists.isEmpty()
}
internal data class EmbeddedLyricsMetadata(val lyrics: List<String>, val credits: SongCredits = SongCredits())
