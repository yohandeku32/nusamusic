# Enlarge the deck and align metadata with the progress bar

> Date: 2026-10-31
> Status: verified locally; physical-device verification pending
> Change type: UI refinement
> Scope: Vinyl sizing, shared horizontal grid, metadata alignment, and quality badge

## Outcome and decisions

The accepted silver machine remains the visual focus. Its chassis now fills the same width as the progress bar, with a shared 16 dp gutter inside a centered content frame capped at 512 dp. Header, left-aligned song title and artist, the quality badge, transport, and footer use that grid. Vertical spacing is tightened to allow the machine to grow while keeping controls in one screen at normal text settings.

The quality face is a small rectangle with square corners rather than a capsule. It retains its existing metadata-driven label, icon, contrast, click action, and minimum 44 dp target, and sits immediately below the artist name. The full two-line metadata/quality area is bottom-aligned, keeping artist/badge and lower-control positions stable while the title varies between one and two natural lines. The missing-badge state retains the same reserved space.

On a short viewport, the chassis retains its full shared width and adapts its height. The concentric well, circular record, and square-coordinate tonearm are centered together using the smaller dimension; they are not stretched. The physical controls stay at the chassis corners with their original 48 x 56 dp targets. The ordinary tall-phone chassis remains square when space permits.

ENERGY 2 / RHYTHM 2 / MOTION 2 and the prior silver/cream/black palette remain. No new asset, logo, text style, playback action, or dependency is added. Portrait loading and half-height lyrics retain their owners.

## Implementation

- [Shared grid, metadata and quality placement](../../../app/src/main/kotlin/com/yohandeku32/nusamusic/ui/TurntablePlayer.kt)
- [Width/height-aware chassis and proportional mechanism](../../../app/src/main/kotlin/com/yohandeku32/nusamusic/ui/VinylTurntableDeck.kt)
- [Square-corner quality face](../../../app/src/main/kotlin/com/yohandeku32/nusamusic/ui/VinylQualityBadge.kt)
- [Layout, label pixels, touch and keyboard regressions](../../../app/src/test/kotlin/com/yohandeku32/nusamusic/ui/VinylPlayerUiTest.kt)

## Verification

All 23 selected tests pass: 15 native player UI, 1 native artwork render, 3 native lyrics integration, and 4 presentation preference tests. Added regressions compare the deck/progress edges, visible title/artist glyph alignment, badge position below the artist, and filled square-corner pixels. Existing one-screen, title stability, physical-control, keyboard, large-text, and wide-layout cases also pass. Compact cases assert that the record stays square inside the wider chassis.

Material's Slider reserves half its track height at each end even with the empty thumb. The custom straight track compensates for that inset. A pixel regression verifies the actual dark progress stroke begins at the chassis edge, separately from the slider's expanded touch bounds.

Debug assembly, Android Lint, APK v2 signature verification, whitespace, and documentation-link checks pass. Standard, one-line, short/large-title, wide, and artwork previews were inspected. The ordinary 411 dp phone chassis is now 379 dp wide, up from 349 dp in the preceding native preview.

## Limits

The quality rectangle is not a fixed 1:1 tile: its width follows the real quality label and icon. This preserves Hi-Res, Lossless, and other actual labels without tiny or truncated text. Extreme system font scaling retains the readable-height scrolling fallback. Physical Android devices, TalkBack, and hardware frame pacing remain unverified.
