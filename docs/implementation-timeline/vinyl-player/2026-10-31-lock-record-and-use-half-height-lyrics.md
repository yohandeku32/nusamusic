# Keep Vinyl geometry stable and reveal half-height lyrics

> Date: 2026-10-31
> Status: verified locally; device verification pending
> Change type: fix
> Scope: metadata geometry, lyric section height, and Vinyl-only lyric follow position

## Outcome and cause

The record no longer consumes the height released by a one-line title. A metadata area reserves the measured two-line title height plus the artist label. The actual title and artist remain a compact, centered group inside that area. Record size and position, header portrait, transport, and quality slot therefore remain independent of title length at a given viewport and font preference.

The preceding implementation stabilized only the lower controls. A native regression reproduced a 37 dp vertical record shift when a one-line title changed to two lines at 411 x 914 dp. The header portrait was already fixed, and now has an explicit geometry assertion too.

The black lyrics section occupies half the playback viewport, including its header, padding, and system navigation inset. As the last outer-list item, this bounds the reveal to approximately half a screen, retaining the lower player above it like the original biography composition. A rounded dark panel contains the lyrics. Timed lyrics use 24 dp top padding and a top follow anchor rather than the full-panel center. Ordinary Immersive lyrics retain the existing centered mode through the default argument.

## Implementation

- [Turntable layout](../../../app/src/main/kotlin/com/yohandeku32/nusamusic/ui/TurntablePlayer.kt)
- [Half-height lyric section](../../../app/src/main/kotlin/com/yohandeku32/nusamusic/ui/VinylLyricsSection.kt)
- [Shared panel with optional alignment](../../../app/src/main/kotlin/com/yohandeku32/nusamusic/ui/ImmersiveLyricsPanel.kt)
- [Timed lyric padding and follow position](../../../app/src/main/kotlin/com/yohandeku32/nusamusic/ui/SyncedLyricsView.kt)
- [Player regressions](../../../app/src/test/kotlin/com/yohandeku32/nusamusic/ui/VinylPlayerUiTest.kt)
- [Lyrics integration regressions](../../../app/src/test/kotlin/com/yohandeku32/nusamusic/ui/VinylLyricsSectionTest.kt)

## Verification

- 28 selected tests pass: 11 native player tests, 3 native lyrics tests, 7 existing lyric anchor/motion tests, 4 presentation preference tests, and 3 portrait-source tests.
- Record and portrait bounds remain identical across repeated short/long title changes at 411 x 914 dp and 360 x 616 dp with a 44 sp title. Wide-screen record bounds also remain stable and stay within the existing 380 dp cap while fitting the remaining height.
- The complete quality touch target and playback controls stay inside the short viewport without scrolling. A one-line title retains a close artist gap.
- The integration fixture uses an outer scrolling player-height item followed by the actual lyric section. Its 457 dp section is revealed from the middle of a 914 dp viewport; active first and later rows appear within 60 dp of the lyric panel's top and tapping seeks to their real fixture times.
- Production embedded lyric loading, empty-state retry, keyboard seek, and large-text reachability pass. The default centered lyric anchor and follow-motion tests also pass.
- Debug assembly, Android Lint, and APK v2 signature verification pass. Short-phone, short-title, larger-title/navigation-space, wide, and half-section native previews were inspected. Whitespace and documentation-link checks pass.

## Limits

Physical device verification remains pending. Extreme text scaling retains the existing readable-height fallback and can scroll. The source of artwork, quality metadata, and online lyrics is unchanged. The half-height region is the outer section; scrolling within its bounded lyric viewport remains available for browsing the rest of a song.

This supersedes the record-stage title compensation described in [the preceding one-screen fix](2026-10-31-fit-player-through-quality-in-one-screen.md).
