# Restore the original centered Vinyl player

> Date: 2026-10-31
> Status: verified locally; physical-device verification pending
> Change type: fix
> Scope: Vinyl presentation and player appearance selection

Restore the earlier Vinyl presentation using the owner's two application screenshots as the visual reference. Work is based on development revision `f04e37c1`, preserving the current Immersive and CD implementations.

## Outcome

- Vinyl is selectable again and a saved `vinyl` preference restores Vinyl. The default for a missing or unknown preference remains Immersive.
- A complete, centered record replaces the oversized record that was cropped against the left edge. Its artwork label occupies 48% of the disc diameter, matching the supplied screenshots.
- The curved metal tonearm is restored from `b522a849^`, before the separate turntable deck redesign. Position still follows the current track's playback progress.
- The cached record painter reuses the earlier fine-groove proportions from `969958cd`, with a dark PVC face and the original black center aperture and small gray spindle.
- Centered serif metadata, a straight progress bar with timestamps below it, a black circular play button, and pale previous/next pills restore the original hierarchy.
- Artist artwork, playback options, and Settings are at the top. Share, Favorite, artist biography, and the metadata-driven audio-quality badge remain available below the transport controls.
- The artist section loads existing Last.fm data through `ArtistBiographyLoader`; loading and unavailable states do not fabricate biography text.

## Decisions

- The warm paper palette is local to Vinyl so it remains readable under either system theme and does not alter CD or Immersive.
- The disc is the primary visual focus. Its size is proportional on phones and capped at 380 dp on larger screens.
- The existing title font and size preferences remain in use. Larger text and shorter displays can scroll through the parent list rather than clipping controls inside a fixed-height deck.
- Shuffle and Repeat use the playback-options menu, leaving the three transport buttons in the screenshot's arrangement.
- Record rotation uses the existing playback animation and honors Android's disabled-animation setting. The arm uses Compose's standard animation behavior.
- Mode tiles may grow and labels may wrap onto two lines to accommodate three choices on narrow displays.

## Implementation

- [Presentation preference](../../../app/src/main/kotlin/com/yohandeku32/nusamusic/model/PlayerPresentationMode.kt)
- [Player composition](../../../app/src/main/kotlin/com/yohandeku32/nusamusic/ui/TurntablePlayer.kt)
- [Current lower section, now lyrics](../../../app/src/main/kotlin/com/yohandeku32/nusamusic/ui/VinylLyricsSection.kt)
- [Integration and playback callbacks](../../../app/src/main/kotlin/com/yohandeku32/nusamusic/MainActivity.kt)

## Verification

- `:app:assembleDebug`: passed for the final source.
- `:app:testDebugUnitTest --tests '*PlayerPresentationModeTest' --tests '*VinylPlayerUiTest'`: 8 tests passed, no failures or skips, including a final rerun after texture and spacing adjustments.
- Native Robolectric/Compose tests exercised play/pause, previous/next, shuffle, repeat, settings and footer callbacks, semantic seeking, keyboard seeking, and the empty-player state.
- Rendered previews were inspected at 411 x 914 dp, 320 x 640 dp with 200% font scaling, and 800 x 914 dp. The record remains centered and capped on the wide layout; compact controls remain reachable through scrolling.
- `:app:lintDebug`: passed with existing project findings. This run preceded the final fine-groove painter and top-spacing adjustment; those final changes were subsequently compiled and exercised by the native UI tests.
- Changed text color pairs have contrast ratios of at least 7.08:1. Buttons retain native Material keyboard/focus behavior; the progress slider also has a verified keyboard seek path.
- Previews use explicit fixture content and an empty artwork fallback. They do not verify the owner's album art, music files, Android share chooser, live Last.fm responses, or physical playback.

## Limitations

The biography outcome above records the initial restoration. [The subsequent refinement](2026-10-31-stabilize-controls-and-add-lyrics.md) replaces that section with lyrics and stabilizes the title/badge layout.

Last.fm biography text depends on an existing cache or the build's configured API key. Final playback and visual fidelity with the owner's library require a physical Android device. Changes are local and are not a published release.
