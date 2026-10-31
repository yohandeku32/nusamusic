# Stabilize Vinyl controls and replace biography with lyrics

> Date: 2026-10-31
> Status: verified locally; device/provider verification pending
> Change type: fix
> Scope: Vinyl player layout, quality badge, lyrics section, artist avatar

## Outcome

- Track titles occupy a fixed two-line slot. Longer titles use an ellipsis and retain the complete value in text semantics. The artist row occupies one line. Changing track metadata no longer changes the transport controls' vertical position.
- The quality area always reserves 44 dp, including while metadata is loading or the current file has no quality badge. The visible Hi-Res/Lossless face is 24 dp at normal font scale, while its clickable area remains at least 44 dp.
- The former biography section is replaced by the existing synchronized lyrics panel. The footer arrow opens the lyrics area; timed rows seek through the current playback callback. The existing local/online loading, retry, and queued-save behavior is reused.
- Vinyl exposes lyric font size, online lookup, and reload in Settings. The mini-lyrics toggle remains specific to Immersive.
- The round artist avatar loads and caches Apple Music portraits with `AppleArtistPortraitLoader`, using the existing unavatar resolver. The local album cover is shown while loading or when no portrait is available. This does not introduce MusicKit authentication or a new portrait service.

## Diagnosis

A native Compose regression test reproduced the title problem before the repair: switching a one-line title to a three-line title moved the Play control from 619 dp to 693 dp, a 74 dp shift. The title previously allowed a variable one-to-three-line height. The old badge also painted its entire 44 dp minimum touch height and could alter content height as metadata appeared.

The repair gives the owning layout stable metadata and quality slots and separates the small badge face from its touch target. The record, tonearm, and playback callbacks remain in use.

## Implementation

- [Fixed title and quality slots](../../../app/src/main/kotlin/com/yohandeku32/nusamusic/ui/TurntablePlayer.kt)
- [Compact quality face and touch target](../../../app/src/main/kotlin/com/yohandeku32/nusamusic/ui/VinylQualityBadge.kt)
- [Bounded lyrics section](../../../app/src/main/kotlin/com/yohandeku32/nusamusic/ui/VinylLyricsSection.kt)
- [Lyrics and avatar integration](../../../app/src/main/kotlin/com/yohandeku32/nusamusic/MainActivity.kt)
- [Regression tests](../../../app/src/test/kotlin/com/yohandeku32/nusamusic/ui/VinylPlayerUiTest.kt)
- [Embedded lyric integration tests](../../../app/src/test/kotlin/com/yohandeku32/nusamusic/ui/VinylLyricsSectionTest.kt)

## Verification

- Final debug APK assembly passed.
- Final `:app:lintDebug` passed with existing project findings; APK signature verification passed (v2).
- All 16 selected tests passed: 7 native Vinyl UI tests, 2 native lyric integration tests, 4 presentation-preference tests, and 3 Apple portrait URL/fallback tests.
- The title regression now keeps both the Play control and quality badge at their previous Y coordinates when changing from a short title to a long title. Badge disappearance and a Hi-Res-to-Lossless label change also preserve the following section's Y coordinate.
- Badge geometry tests measure a 24 dp visible face and a touch target of at least 44 dp; the metadata callback remains operable.
- The native lyric tests read a generated local MP3 fixture, display its timed lines in the Vinyl section, and verify that tapping the first line seeks to 1,000 ms. The no-lyrics state and retry action also pass.
- Native previews at 411 dp, 320 dp with 200% fonts, and 800 dp were inspected. The compact badge and dark lyrics section were inspected separately.
- The lyric integration harness manually advances the Compose frame clock and idles the Android main looper. This permits IO-to-Main lyric publication while avoiding an unbounded automatic-idling wait on the existing lyric animation driver; the production driver was not changed.
- New badge text has 8.04:1 contrast against its face. The existing shared lyric renderer is reused without changing its animation or focus treatment.

## Limits

The fixed two-line title slot described above was subsequently replaced by [natural-height metadata and a viewport-sized player](2026-10-31-fit-player-through-quality-in-one-screen.md). This entry records the earlier refinement.

Live portrait availability depends on the existing Apple Music/unavatar provider and its cache policy. Native tests exercise the lyric engine with local fixture metadata, not the owner's music or live lyric providers. Physical playback, the Android share chooser, and production provider availability require device checks.

This follows [the original Vinyl restoration](2026-10-31-restore-original-vinyl-player.md). Changes remain local until explicitly published.
