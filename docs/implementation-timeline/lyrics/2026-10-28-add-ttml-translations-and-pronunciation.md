# TTML translations and pronunciation

> Date: 2026-10-28
> Status: verified
> Change type: feature
> Scope: Immersive lyric annotations in PR #178
> Based on: ba2e2dfdf59216d750d717c7621ebcb391513ed1

## Accepted outcome

Show supplied translation or pronunciation beneath the original lyric, using the user's Apple Music screenshot as a hierarchy reference. The sample TTML contains timed pinyin, rather than a translated language. Preserve original word highlighting, character motion, duet alignment, backing windows, scrolling and Save behavior.

The original lyric remains the largest type. Pronunciation uses 55% of the lyric size with bold weight and a 14sp minimum; translation uses 65% with the same minimum. Both inherit system font scaling. Timed pronunciation brightens from a 58% white base to full white; translation uses the existing lyric-presence envelope to brighten from 68% to full white when its line is active. Spacing groups all three texts inside one measured and tappable row. Existing artwork, fonts and focus effects retain their owners. No Apple assets or implementation were copied.

## Implementation

- [TtmlCompanionReader](../../../app/src/main/kotlin/com/yohandeku32/nusamusic/data/TtmlCompanionReader.kt) reads inline companion roles and iTunes metadata indexed by source keys. Stable first-variant selection and inline precedence avoid combining different translations. Readable pronunciation text retains exact character ranges for each valid timed word, including repeated words and partial timing.
- [LyricsDecoder](../../../app/src/main/kotlin/com/yohandeku32/nusamusic/data/LyricsDecoder.kt) binds companions to source rows before filtering, start normalization and sorting. Translations enter existing line models; pronunciation is a sidecar indexed by final rows, including backing rows. Companion descendants cannot establish sung timing or singer identity.
- [LyricsRepository](../../../app/src/main/kotlin/com/yohandeku32/nusamusic/data/LyricsRepository.kt) and [ImmersiveLyricsPanel](../../../app/src/main/kotlin/com/yohandeku32/nusamusic/ui/ImmersiveLyricsPanel.kt) carry that sidecar into the existing lyric view. Stored source TTML is unchanged, so cache and original-file Save retain the annotations without a data migration.
- [CenteredLyricsLine](../../../app/src/main/kotlin/com/yohandeku32/nusamusic/ui/CenteredLyricsLine.kt) displays translations for karaoke and line-synced rows. Both companion types share the parent's seek target, height, sentence transform and voice alignment. Backing companions share the backing row's reveal and fade.
- [LyricsPronunciationRow](../../../app/src/main/kotlin/com/yohandeku32/nusamusic/ui/LyricsPronunciationRow.kt) caches wrapped text and reuses the main painter for complete timed annotations. Its shaped-text fallback caches exact word paths for partial annotations. Both sample supplied timing without playback-frame remeasurement.

## Verification

Android compilation succeeded. The initial feature's focused Gradle run passed **419 tests, zero failures, zero skips**, covering all lyric tests plus the affected metadata readers and writers. Its new coverage consists of **13 TTML companion tests** and **six native Robolectric/Compose tests**. Lint completed with zero errors; the project retains existing warnings.

Native checks exercise initial visibility, tapping pronunciation and translation to seek, keyboard Tab/Enter, source-row height, right-aligned duet text, 320dp at 1.8x font scale, unchanged paused pixels and the complete scrolling lyric viewport. Six actual renders are written to `app/build/reports/lyrics-companion-preview/` and uploaded by CI as `Lyrics-companion-previews`. Their sentences are synthetic test fixtures.

A separate local check decoded the user-supplied TTML: **36 lyric rows and 32 timed pronunciation rows**, with the first word beginning at 9,234ms and the first pronunciation ending at 11,751ms. The four English rows receive no invented pronunciation, and no translated language is fabricated. The supplied song file is not included in repository fixtures.

Commands: `:app:testDebugUnitTest --tests '*Lyrics*' --tests '*TtmlCompanionsTest' --tests '*EmbeddedLyricsReaderTest' --tests '*AudioContainerLyricsWriterTest' --tests '*Mp3EmbeddedLyricsWriterTest'` and `:app:lintDebug`. No APK was assembled.

### Same-day follow-up: stronger companion highlighting

The user found the companion animation too faint. The original pronunciation base was already 82% white, leaving only 18 percentage points of headroom. Its base is now 58%, with bold weight and a size increase from 50% to 55% of the main lyric. The timed full-white endpoint leaves 42 percentage points of headroom. Translations now brighten from 68% to full white using the original lyric's onset/release envelope. These changes add no timing estimates, movement or size changes during playback.

The follow-up passed **31 focused tests with zero failures**: eight native companion UI tests, five existing viewport-motion tests and 18 animation-timing tests. Two new native pixel regressions isolate the companion bands rather than comparing the whole screen: completed pinyin highlighting must increase peak red by at least 90/255, and active translation must increase it by at least 65/255. The translation's bounds stay identical and its inactive brightness returns after release. Existing paused-pixel, keyboard, duet, tap and narrow large-text checks still pass. Android compilation and lintDebug pass; native captures include `strong-pronunciation-highlight.png` and `strong-translation-highlight.png`.

## Shared animation follow-up

The user requested the main lyric's animation for companion text after the feasibility assessment. Fully timed pronunciation now uses [SyllableLyricsPainter](../../../app/src/main/kotlin/com/yohandeku32/nusamusic/ui/SyllableLyricsPainter.kt) with its original word intervals, progressive fill, connected character wave and reliable held-note geometry. Drawing the renderer at the companion text scale also scales its lift and halo without duplicating the animation engine. Main lyrics retain the painter's original opacity defaults; pronunciation explicitly retains the stronger 58% base/full-white highlight.

[LyricsCompanionMotion](../../../app/src/main/kotlin/com/yohandeku32/nusamusic/ui/LyricsCompanionMotion.kt) admits only completely covered, valid timed text into that renderer. Untimed words or malformed ranges keep the shaped fallback, including existing valid highlight ranges. No source timings are replaced or expanded. Rapid pronunciation words retain the main engine's threshold and do not acquire a fake held note.

The initial shared-animation version made translations follow the measured source painter's lift, swell and halo at 65% strength. That third-row coupling was subsequently removed in the stable-translation correction below. Translation words remain un-timed. Line-synced originals retain line-level brightness. Pausing freezes the source and pronunciation poses; seeking invalidates the monotonic frame cache. A zero animation duration scale removes additional pronunciation geometry, including a change made while paused. Layout height and tap targets do not animate.

Initial integration passed 33 focused parser/UI checks. The final Android regression run passed **428 lyric/metadata tests, zero failures, zero skips**, and lintDebug completed with zero errors (existing project warnings remain). New coverage includes four annotation-adapter contracts and three native regressions: held pinyin visibly rises, translation follows the source without reflow, zero-duration-scale returns both to rest, pause retains identical pixels, seeking backward restores identical destination pixels, and rapid words remain stationary. Native captures include `held-companions-moving.png` and `held-companions-reduced-motion.png`. No APK was assembled.

## Limits and maintenance

There is no automatic translation, pronunciation generation or language picker. The first nonempty metadata variant is used. Chinese/Japanese pronunciation with matching timing uses the paired layout described below; other cases retain the companion-sentence fallback. Missing or malformed optional word timestamps leave the corresponding text readable without synthetic timing.

Physical-device rendering, TalkBack speech, artwork-dependent contrast and frame pacing remain unverified. Existing distant-row fading and blur remain part of the lyric renderer. At full row focus, pronunciation's base measures 6.63:1 against the native test's dark background, translation's inactive base measures 8.67:1, and full-white active text measures 17.78:1. This focused run does not claim a full application audit or resolution of previously reported unrelated artist-layout failures.

Keep companion association on source identity rather than matching starts alone: simultaneous duet rows can have identical times, and container padding can reorder sung starts. Keep exact text ranges when changing pronunciation parsing, because invalid or untimed repeated words must not shift a later highlight. See the [lyrics guide](../../EMBEDDED_LYRICS.md).

## Chinese/Japanese paired reading follow-up

The user clarified that pinyin/romaji should sit directly underneath the matching character or kanji group, following the supplied screenshot. [LyricsInterlinearLayout](../../../app/src/main/kotlin/com/yohandeku32/nusamusic/ui/LyricsInterlinearLayout.kt) joins source and reading spans at common authored interval boundaries. One-to-many and many-to-one intervals form a shared group; repeated text is matched by its intervals. Unannotated tails retain their source cells without fabricated readings.

The two levels are measured as one group. Each cell uses the greater visible width and centers both text runs; source whitespace becomes a real gap between cells. A cell wraps intact, preserving its source and reading on the same visual row. Wrapped duet rows share the same right edge. Both existing syllable painters draw into the paired layout, retaining their authored timing, contrast, motion, visibility and seek target. Source wave grouping explicitly ignores added reading-cell padding while preserving authored whitespace as a word boundary.

Non-CJK content, incomplete/ambiguous timing, unsupported bidirectional mixtures and indivisible groups wider than the available width use the existing separate reading-row fallback. No reading-to-character mapping is invented. Sentence translations remain below the entire paired block.

Native renders cover a nine-character Chinese line, multi-kanji Japanese reading, 320dp with 1.8x text, right-aligned duet, pause and backward seek. Geometry checks use the actual placed text layouts, not stored center labels. A separate local check verified that all 32 annotated rows of the user-supplied TTML have unambiguous reading groups; its four English-only rows retain their existing layout.

The final focused run passed **443 lyric/metadata tests with zero failures/skips**. New coverage adds eight interval-mapping contracts, five native paired-layout tests, and two source-animation grouping regressions. The native center check permits at most 0.6px difference between the actual placed source and reading centers. Android compilation and lintDebug passed with zero lint errors; existing project warnings remain. A prior concurrent analysis hit an internal lint exception; the final unchanged-source run completed successfully. No APK was assembled. Physical-device rendering, TalkBack, artwork-dependent contrast and frame pacing remain unverified.

## Stable third-row translation correction

The user reported that the sentence translation (third text level) moved when words in the original lyric performed short-wave or held-note animation. Native tests reproduced both cases before the repair. The cause was the source-pose layer explicitly attached to `LyricsTranslation`: its baseline and vertical scale inherited the source word's pose, and a source-driven halo added a further pulse.

`LyricsTranslation` now receives only its own line-presence envelope. The source-pose transform and halo drawing are removed from that component, keeping its baseline, bounds and text size independent of short/long word effects. Its 68%-to-white line activation, layout height, duet alignment and parent seek target remain. Main/pinyin animation, paired CJK reading layout, list scrolling and sentence-group following retain their existing behavior.

Regression coverage updates the held-note native check to require identical translation bounds with motion enabled/disabled while pinyin still visibly rises. A short-word check samples five playback positions; an additional CJK test samples both a held note and a short pull beneath paired readings and verifies the third row's exact bounds, row height and seek action.

Before repair, both short/held regression checks failed against the source-pose coupling. After repair, **23 focused native UI checks passed with zero failures/skips**: 13 companion checks, five paired-layout checks and five sentence-following checks. Android compilation and lintDebug passed with zero lint errors (existing project warnings remain). The earlier 443-test lyric/metadata run is historical coverage for the preceding paired-layout change, not a newly repeated full run for this small correction. No APK assembled. Physical-device verification remains outstanding.

## CJK held-note ownership correction

The user clarified that the main Chinese/Japanese text pulled preceding characters into a later character's sustained animation, while the second reading row was already correct. The whitespace/gap grouping in `LyricsHeldWordWave` merged contiguous source units, selected the group's longest hold and assigned it to every member. `LyricsLetterWave` likewise treated the merged phrase as one interval. Reading-cell gap relaxation amplified this for unspaced CJK text. The error was temporal ownership, not pronunciation placement.

Both wave models now stop joining at a Han, hiragana or katakana timing unit. Each such unit uses its own authored interval and measured extent, independent of cell padding. A unit explicitly containing multiple kanji still animates internally as one authored word; no character timing is invented. Latin fragments retain the existing joining/whitespace policy, preserving pinyin/romaji behavior.

Before repair, three interval-ownership checks and a native pixel regression failed. The checks cover Chinese, hiragana, katakana, two adjacent held units and a multi-kanji authored unit. The native test holds the middle main character and compares the preceding/following character regions against a main-only neutral-motion control: neighbors must be pixel-identical, the held character must still differ, and the second reading band must remain identical. Existing gap tests now explicitly exercise Latin fragments rather than assuming a CJK word boundary from absent spaces. Initial targeted verification passed 75 checks. Final verification passed **449 lyric/metadata tests with zero failures/skips**, Android compilation and lintDebug with zero lint errors (existing project warnings remain). No APK assembled; physical-device verification remains outstanding.

## Soft highlight handoff between words

The listener requested gradual emphasis entering the next word rather than a visible restart at every boundary. Short-word geometry already shared a crest where eligible, but each syllable's narrow fill independently started at zero and completed at one. The painter now samples `LyricsHighlightHandoff`: adjacent reliable units share a measured fill cursor at the boundary, with matching position/velocity at its entrance and exit. Its white shoulder is at most 12dp per side. Anticipation and completion are bounded to 90ms from each unit's own timestamps, including short rests; faster words use smaller windows. Unit timestamps, syllable progress, seek targets and metadata remain unchanged.

This is fill continuity, not sustained-note propagation. Han/kana authored units retain their independent held-note lift/projection and reading placement. The same painter supplies timed pronunciation. A separate `motionEnabled` flag now disables pronunciation geometry for reduced motion without treating reliable timestamps as inferred; its fill stays identical. Inferred timing, overlapping units, rests beyond 45ms and excessive measured spacing retain the previous fill, as do RTL runs. The model uses fixed per-unit boundary references and cached shaders; no frame job, new dependency or layout animation is introduced.

Seven new model checks cover shared boundary position, monotone sweep/velocity, 60/90/120Hz sampling, pause/seek, Latin mixed widths, wrapped paths, long holds, short rests and timing fallbacks. A new native CJK fixture verifies visible emphasis before the next onset while the later character's pixels and row height remain unchanged; pause and backward seek restore identical pixels. Native previews are written as `soft-cjk-handoff-*.png` in the existing companion-preview directory. Final verification passed **457 lyric/metadata tests with zero failures/errors/skips**, Android compilation and lintDebug with zero errors (120 existing warnings and three hints). Existing rapid-pronunciation/reduced-motion and CJK hold-isolation regressions pass. No APK assembled; physical-device smoothness and GPU frame pacing remain unverified.
