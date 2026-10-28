# Use TTML credits when file tags are empty

Verified 28 October 2026. Scope: lyric authorship metadata and its existing credit displays.

## Outcome and ownership

The Immersive lyric header previously read only the file's composer tag and displayed **Unknown composer** when that tag was absent. The supplied TTML contains five authors in iTunes `songwriters/songwriter` metadata. The decoder now reads explicit authorship lists from the document head and carries them through selection, online enrichment and disk persistence.

- File composer/lyricist tags take priority over corresponding TTML roles. Each missing role can fall back independently.
- General songwriters remain a third role in `SongCredits`. They supply the header only when no composer exists, with **SONGWRITERS** as its label. The shared credit block uses **Written by**. No songwriter is guessed to be specifically a composer or lyricist.
- Explicit `composers/composer`, `lyricists/lyricist` and `songwriters/songwriter` lists are accepted within top-level head metadata, regardless of namespace prefix or wrapper container. Names are normalized/deduplicated in authored order; each role is bounded to 32 names of 2,048 characters. Blank and explicit unknown placeholders are ignored.
- Agents, lyric-file editors, copyright owners, arbitrary body metadata and sung credit-like text are excluded. Existing XML size/element/entity restrictions remain authoritative.
- Only the selected TTML contributes local fallback credits. An edited standard plain lyric does not inherit authors from a discarded, stale timed candidate.
- A valid matching online lyric can fill missing credits even when local timing remains selected. Existing text matching and duet preservation remain; different lyrics cannot provide credits. Reading credits does not request an additional provider or write audio metadata.

The implementation lives in [TtmlCreditsReader](../../../app/src/main/kotlin/com/yohandeku32/nusamusic/data/TtmlCreditsReader.kt), [LyricsDecoder](../../../app/src/main/kotlin/com/yohandeku32/nusamusic/data/LyricsDecoder.kt) and [LyricsRepository](../../../app/src/main/kotlin/com/yohandeku32/nusamusic/data/LyricsRepository.kt). Existing `SongCredits` consumers retain default-empty compatibility. `LyricsDiskStore` adds an optional songwriter property: older entries remain readable, and old raw TTML is decoded for credits without requiring cache invalidation.

## Verification

Thirteen new parser/selection contracts cover the supplied five-author metadata shape, prefixes/wrappers, explicit roles, file-tag priority, excluded people/text, cleanup/limits, old raw caches, matching online enrichment with unchanged local timing, timing upgrades, different-song rejection, selected-candidate ownership, edited standard text and missing/invalid input. One new disk round trip retains Unicode songwriters independently of composer tags.

Four native Compose tests verify the fallback header and label, file-composer precedence, the original unknown state, and a 320dp/1.8x text viewport. Native synthetic previews are emitted to the existing `lyrics-companion-preview` directory as `ttml-songwriter-*.png`. The preview names are labelled test content; the song's lyrics are not copied into it.

Final focused verification: **481 tests, zero failures/errors/skips**, including all lyric/metadata contracts and six existing player-scale UI checks. Android compilation and lintDebug passed with zero lint errors (120 existing warnings and three hints). No APK assembled. Physical-device rendering remains unverified; no animation behavior or source timestamps changed.
