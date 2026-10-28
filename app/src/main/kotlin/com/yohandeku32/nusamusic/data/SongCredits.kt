package com.yohandeku32.nusamusic.data

/** Authored roles remain separate; absence is represented by an empty list. */
data class SongCredits(
    val composers: List<String> = emptyList(),
    val lyricists: List<String> = emptyList(),
    val songwriters: List<String> = emptyList()
) {
    val isEmpty: Boolean get() = composers.isEmpty() && lyricists.isEmpty() && songwriters.isEmpty()

    fun withFallback(other: SongCredits) = SongCredits(
        composers.ifEmpty { other.composers }, lyricists.ifEmpty { other.lyricists },
        songwriters.ifEmpty { other.songwriters }
    )
}
/**
 * The human-facing iTunes/MP4 ©lyr tag is commonly edited by other music
 * apps. Keep it distinct from NusaMusic-owned timed ---- metadata: if the two
 * have different words, the edited ©lyr must not be hidden by stale timing.
 */
internal data class EmbeddedLyricsMetadata(
    val lyrics: List<String>,
    val credits: SongCredits = SongCredits(),
    val preferredPlainLyrics: String? = null
)
