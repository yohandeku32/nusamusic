package com.yohandeku32.nusamusic.data

/**
 * Returns the first artist name from metadata that contains multiple artists.
 *
 * Common collaboration separators are handled while preserving normal artist
 * names that contain an ampersand, e.g. "Simon & Garfunkel".
 */
object ArtistNameUtils {

    fun firstArtist(raw: String?): String {
        val value = raw?.trim().orEmpty()
        if (value.isBlank()) return ""

        return value
            .split(
                Regex(
                    "(?i)\\s+(?:feat\\.?|ft\\.?|featuring)\\s+|\\s*;\\s*|\\s*,\\s*"
                )
            )
            .firstOrNull { it.isNotBlank() }
            ?.trim()
            ?: value
    }
}
