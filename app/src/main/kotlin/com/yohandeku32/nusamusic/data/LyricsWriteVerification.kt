package com.yohandeku32.nusamusic.data

/**
 * The metadata reader intentionally strips only envelope whitespace/BOM from
 * lyrics. Comparing the unnormalised provider response against its result
 * incorrectly rejects a valid staged FLAC/M4A/MP3 write.
 *
 * Preserve authored timestamps and the lyric body: only normalise equivalent
 * line endings and the same surrounding whitespace removed by the reader.
 */
internal fun canonicalSavedLyrics(value: String): String = value
    .replace("\r\n", "\n")
    .replace('\r', '\n')
    .trim('\uFEFF', '\u0000', ' ', '\n')

internal fun stagedLyricsMatch(expected: String, candidates: List<String>): Boolean {
    val canonical = canonicalSavedLyrics(expected)
    return canonical.isNotBlank() && candidates.any { canonicalSavedLyrics(it) == canonical }
}
