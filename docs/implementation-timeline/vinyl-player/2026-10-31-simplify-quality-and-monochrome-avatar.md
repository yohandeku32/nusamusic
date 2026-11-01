# Simplify quality, lower metadata, and use a monochrome avatar

> Date: 2026-10-31
> Status: verified locally; physical-device verification pending
> Change type: UI refinement
> Scope: Vinyl quality treatment, metadata placement, and circular artist portrait

## Outcome

Remove the quality face background, clipping shape, and decorative padding. Keep only the existing lossless icon and real quality text, slightly smaller at 16 dp and 11 sp/14 sp line height, with a 4 dp gap. Its transparent interaction area retains a minimum 44 dp target and the existing metadata action; the 44 dp quality layout slot remains reserved across availability/label changes.

Move the title/artist/quality group down 8 dp as one unit using a layout offset. The machine, header portrait, progress bar, normal controls, and shared 36 dp content margin retain their positions. The minimum progress gap accommodates this offset on short screens, keeping touch regions separate. Track changes still use the fixed two-line metadata budget.

Apply a remembered zero-saturation ColorMatrix to the resolved portrait in the circular Vinyl avatar. Its local artwork fallback already uses monochrome. The source bitmap, disk/memory caches, first-artist lookup, and colored portraits elsewhere in the app are unchanged; this is a presentation filter, not an image rewrite.

No new asset, dependency, API behavior, typography family, or playback action is introduced. The accepted silver turntable direction and ENERGY 2 / RHYTHM 2 / MOTION 2 remain.

## Verification

All 23 selected tests pass: 15 native player UI, 1 artwork render, 3 lyrics integration, and 4 presentation preference tests. The quality regression checks a 16 dp visual row with a full touch target and samples the page color in the icon/text gap to prove the background is absent. Existing layout tests retain original margins, first-pixel progress alignment, metadata stability, short/wide layouts, large text, and physical/keyboard actions.

Final debug assembly, Android Lint, APK v2 signature verification, whitespace, and documentation links pass. Standard and compact/large-title native previews were inspected. Quality ink against the cream page has 10.54:1 contrast.

The Apple portrait monochrome branch is checked in source and compilation. The layout fixture uses a black avatar placeholder; it does not prove a live Apple photo's displayed pixels. Physical-device rendering and TalkBack remain unverified.

## Sources

- [Quality label](../../../app/src/main/kotlin/com/yohandeku32/nusamusic/ui/VinylQualityBadge.kt)
- [Metadata layout](../../../app/src/main/kotlin/com/yohandeku32/nusamusic/ui/TurntablePlayer.kt)
- [ArtistAvatar](../../../app/src/main/kotlin/com/yohandeku32/nusamusic/MainActivity.kt)
- [Native layout regressions](../../../app/src/test/kotlin/com/yohandeku32/nusamusic/ui/VinylPlayerUiTest.kt)
