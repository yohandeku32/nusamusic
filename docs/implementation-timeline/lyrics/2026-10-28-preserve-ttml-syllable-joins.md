# Preserve TTML syllable joins

Verified 28 October 2026. Scope: TTML text normalization and pronunciation spacing.

The reported `mencintaimu` word appeared as `men cin tai mu` even though its spans represented one word. Three regression cases reproduced this before repair. The decoder forwarded pretty-print indentation to Core 0.4.2, whose XML reader collapses all whitespace to spaces and whose TTML parser appends inter-span text to the preceding syllable. The result introduced word boundaries before measurement and wrapping. A separate companion-reader rule inserted spaces between all pronunciation spans, including Latin syllables.

## Repair

- [TtmlWhitespace](../../../app/src/main/kotlin/com/yohandeku32/nusamusic/data/TtmlWhitespace.kt) distinguishes whitespace-only indentation containing line breaks/tabs from explicit word separators. It is shared by main text normalization and companion text extraction. Ordinary spaces, leaf-span text, CDATA and inherited `xml:space="preserve"` are retained. A nearer `xml:space="default"` restores indentation handling. No dictionary, language guess or removal of real word spaces is used.
- [TtmlCompanionReader](../../../app/src/main/kotlin/com/yohandeku32/nusamusic/data/TtmlCompanionReader.kt) defers metadata pronunciation construction until the source line is known. CJK source readings retain automatic pinyin/romaji separation. Latin source readings preserve authored joins/spaces and valid ranges. Translation text and nested backing vocals cannot falsely classify a Latin main line as CJK.
- The raw TTML, intervals, singer associations, highlight and movement models retain their owners. Existing raw caches are decoded through the corrected path without forced invalidation. This addresses text boundaries before the renderer rather than visually hiding whitespace.

The [AMLL syllable-format instructions](https://github.com/amll-dev/amll-ttml-db/blob/main/instructions/ttml-specification-en.md#6-whitespace-and-formatting-rules) distinguish formatted source from explicitly authored word spaces. This is support for that lyric dialect, not a replacement for general XML whitespace semantics.

## Verification

Seven text contracts cover compact/indented spans, real word spaces, original intervals, preserve/default inheritance, Latin reading joins, metadata/inline readings and unchanged CJK pinyin spacing. Three of the original five checks failed against the preceding source and pass after repair. Two native Compose tests compare formatted and compact main/reading output pixel-for-pixel at 411dp and 320dp/1.8x text; row height and wrapping agree. Native previews use the synthetic phrase `mencintaimu selalu` and are emitted as `joined-ttml-word-*.png` in the existing companion-preview directory.

The same update carries the requested TTML-only **SONGWRITERS** header; its provenance and label checks are recorded in the [authorship record](2026-10-28-use-ttml-credits-when-tags-are-empty.md). Focused verification passed **492 tests with zero failures/errors/skips**, including existing motion/metadata and six player-scale checks. Android compilation and lintDebug passed with zero errors (existing project warnings remain). No APK assembled. The user's exact additional failing song files and physical-device rendering remain unverified; the reported failure shape is reproduced and repaired locally.
