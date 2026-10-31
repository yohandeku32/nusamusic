# Fit Vinyl through the quality badge in one screen

> Date: 2026-10-31
> Status: verified locally; device verification pending
> Change type: fix
> Scope: Vinyl player height, record sizing, metadata spacing, and viewport regression tests

## Outcome

The player now budgets its available viewport before sizing the record. Header, metadata, progress, transport, footer, and the 44 dp quality slot are measured within a bounded column. The record takes the remaining height, retaining its original maximum width and 380 dp cap. Spacing and transport dimensions tighten on shorter displays while preserving usable touch targets.

The title uses its natural one- or two-line height. It no longer reserves a blank second line below a one-line title. The record stage absorbs the title-height difference, keeping lower controls stable and maintaining a close title-to-artist gap. The quality face remains 24 dp; no quality metadata is invented for files without a supported badge.

The lower lyric section remains a separate item below the player. It is not part of the one-screen playback area.

## Diagnosis

The previous implementation specified a minimum viewport height, then stacked a fixed-width record, fixed spacers, and fixed controls. Their combined height could exceed a short viewport. Earlier tests scrolled to the badge before checking it, so they proved reachability rather than initial full visibility.

Two new tests failed before the repair: a 360 x 640 dp viewport put transport controls outside the frame, and a one-line title retained an empty line before the artist. They now test full unclipped bounds without requesting any scroll, plus the rendered first-line bottom rather than the padded title container.

## Implementation

- [Bounded player and adaptive record stage](../../../app/src/main/kotlin/com/yohandeku32/nusamusic/ui/TurntablePlayer.kt)
- [Viewport, navigation-space, title-gap, and stability regressions](../../../app/src/test/kotlin/com/yohandeku32/nusamusic/ui/VinylPlayerUiTest.kt)

The test footer now uses 48 dp icon buttons like the application rather than text-button fixture captions. Another case reserves 24 dp for system navigation while using the largest normal title preference (44 sp).

## Verification

- Final debug assembly and APK v2 signature verification passed.
- 19 selected tests pass: 10 native Vinyl UI tests, 2 native lyric tests, 4 preference tests, and 3 portrait-source tests.
- Full bounds are checked without scrolling for Settings, seek, previous/play/next, Lyrics, Share, Favorite, and the entire Hi-Res touch target at 360 x 640 dp.
- A second full-visibility case uses a long title at 44 sp with 24 dp reserved for navigation, leaving a 360 x 616 dp playback viewport. All controls and the badge remain inside that viewport.
- A one-line title at 360 x 740 dp has a verified visible-text-to-artist gap of no more than 12 dp. Other tests confirm lower-control positions remain stable across title and badge changes.
- Native short-phone, one-line-title, navigation-reserved/large-title, standard, and wide previews were inspected. Large-font accessibility tests remain operable through the parent scroll when the minimum readable content exceeds the viewport.
- Whitespace and documentation-link checks pass. Final Android Lint passed with existing project findings.

## Limits

Ordinary portrait phone sizes are the one-screen target. Extreme font scaling retains a minimum readable content height and can use the existing parent scroll instead of clipping controls or reducing touch targets. Actual device insets and the owner's library still require device verification. Photo and lyric providers are unchanged.

This supersedes the reserved two-line title approach in [the previous refinement](2026-10-31-stabilize-controls-and-add-lyrics.md).
