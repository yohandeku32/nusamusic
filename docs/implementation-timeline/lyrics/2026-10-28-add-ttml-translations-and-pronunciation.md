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

Translations follow the actual measured source painter's lift, swell and halo at 65% strength. The source pose is cached per playback time/revision and shared by the draw layer and halo. Translation words remain un-timed. Line-synced originals retain line-level brightness. Pausing freezes the source and companion poses; seeking invalidates the monotonic frame cache. A zero animation duration scale removes the additional companion geometry, including a change made while paused. Layout height and tap targets do not animate.

Initial integration passed 33 focused parser/UI checks. The final Android regression run passed **428 lyric/metadata tests, zero failures, zero skips**, and lintDebug completed with zero errors (existing project warnings remain). New coverage includes four annotation-adapter contracts and three native regressions: held pinyin visibly rises, translation follows the source without reflow, zero-duration-scale returns both to rest, pause retains identical pixels, seeking backward restores identical destination pixels, and rapid words remain stationary. Native captures include `held-companions-moving.png` and `held-companions-reduced-motion.png`. No APK was assembled.

## Limits and maintenance

There is no automatic translation, pronunciation generation or language picker. The first nonempty metadata variant is used. Pronunciation wraps as a companion sentence; character-by-character interlinear positioning is not added. Missing or malformed optional word timestamps leave the corresponding text readable without synthetic timing.

Physical-device rendering, TalkBack speech, artwork-dependent contrast and frame pacing remain unverified. Existing distant-row fading and blur remain part of the lyric renderer. At full row focus, pronunciation's base measures 6.63:1 against the native test's dark background, translation's inactive base measures 8.67:1, and full-white active text measures 17.78:1. This focused run does not claim a full application audit or resolution of previously reported unrelated artist-layout failures.

Keep companion association on source identity rather than matching starts alone: simultaneous duet rows can have identical times, and container padding can reorder sung starts. Keep exact text ranges when changing pronunciation parsing, because invalid or untimed repeated words must not shift a later highlight. See the [lyrics guide](../../EMBEDDED_LYRICS.md).
