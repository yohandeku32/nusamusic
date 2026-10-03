package com.yohandeku32.nusamusic.data

/**
 * Returns the first artist name from metadata that contains multiple artists.
 *
 * Common collaboration separators are handled. Ampersand is also treated as
 * a collaboration separator because this project prioritizes resolving the
 * first artist for biography and portrait lookup.
 */
object ArtistNameUtils {

    fun firstArtist(raw: String?): String {
        val value = raw?.trim().orEmpty()
        if (value.isBlank()) return ""

        return value
            .split(
                Regex(
                    "(?i)\\s+(?:feat\\.?|ft\\.?|featuring)\\s+|\\s*&\\s*|\\s*;\\s*|\\s*,\\s*|\\s+/\\s+|\\s+\\bx\\b\\s+"
                )
            )
            .firstOrNull { it.isNotBlank() }
            ?.trim()
            ?: value
    }
}
