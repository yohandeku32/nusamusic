# Restore the previous player margins

> Date: 2026-10-31
> Status: verified locally; physical-device verification pending
> Change type: UI correction
> Scope: Vinyl content gutters, header/footer spacing, and the previous vertical rhythm

## Outcome

The preceding 16 dp gutter made the machine too wide for the owner's intended composition. Restore the 36 dp progress/transport margin from the earlier accepted player. The chassis, left-aligned title and artist, and rectangular quality face below the artist follow that content edge. On a 411 dp phone, the chassis is 339 dp wide instead of the rejected 379 dp version.

Header/footer return to their previous 24 dp margins, transport returns to its 480 dp outer width cap, and the earlier adaptive vertical spacing and primary transport sizing are restored. The current 512 dp content frame cap remains for the machine/metadata/progress group. The square-corner quality face, metadata placement, stable track-change geometry, and half-height lyrics stay in place.

The 36 dp margin comes from the progress and transport layout in the accepted machine source at `bf2951ab`, rather than a new margin choice. The regression now asserts that original inset explicitly while retaining the deck/progress alignment and visible stroke pixel check.

## Verification

All 23 selected tests pass: 15 native player UI, 1 artwork render, 3 lyrics integration, and 4 presentation preference tests. The phone regression asserts a 36 dp left margin and a 375 dp right edge in a 411 dp viewport. Shared progress/text/badge alignment, short/long title stability, compact/wide layouts, physical controls, keyboard, and large-text reachability pass. Standard and one-line native previews were inspected.

Debug assembly, Android Lint, APK v2 signature verification, whitespace, and documentation-link checks pass. Physical-device layout remains unverified. No new asset, dependency, network behavior, or playback action is introduced.

## Sources

- [Turntable layout](../../../app/src/main/kotlin/com/yohandeku32/nusamusic/ui/TurntablePlayer.kt)
- [Native player regressions](../../../app/src/test/kotlin/com/yohandeku32/nusamusic/ui/VinylPlayerUiTest.kt)
- [Previous layout record, superseded for margins](2026-10-31-enlarge-deck-align-metadata-and-quality.md)
