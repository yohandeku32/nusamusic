# Match Now Playing navigation to the Hi-Res material

> Date: 2026-10-28
> Status: verified
> Change type: refinement
> Scope: Now Playing navigation surface and its unused blur capture

## Accepted direction

The requested reference is the existing shape behind the Hi-Res text in AudioQualityPill. That badge uses the basic [immersiveGlassPill](../../../app/src/main/kotlin/com/yohandeku32/nusamusic/ui/ImmersiveGlassPill.kt) material over the already blurred artwork. Navigation now reuses that exact primitive: white at 9% with an 8% white edge in dark appearance, and black at 5.5% with a 9% black edge in light appearance. Only corner radius differs: the badge retains its compact rounded rectangle and navigation retains its measured capsules/circle.

This supersedes the heavier navigation material in [the preceding record](2026-10-28-follow-artwork-through-navigation-glass.md). Artwork colour is visible through the shared thin material directly. The extra navigation tint tween, optical highlights, Haze surface and source recording were removed. Haze had no remaining application consumers, so its dependency was removed as well. The existing artwork blur, dynamic glow and transport palette keep their owners.

[ImmersiveArtworkPlayer](../../../app/src/main/kotlin/com/yohandeku32/nusamusic/ui/ImmersiveArtworkPlayer.kt) uses dark navigation ink on a light Now Playing surface. Existing black portal navigation retains white ink; touch areas, centering, page transitions and callback ownership remain. The saved former glass flag continues to govern existing press motion and cannot restore the old opaque material. [ImmersiveArtistPortal](../../../app/src/main/kotlin/com/yohandeku32/nusamusic/ui/ImmersiveArtistPortal.kt) no longer records an unused backdrop.

## Verification

- Android compilation, 15 focused checks and lintDebug passed locally. The checks comprise seven native navigation cases, three centering checks and five existing glow UI cases.
- Native pixel comparisons match the actual shared Hi-Res material in both themes, excluding their different corners. A background colour change shows through immediately and remains identical after waiting. Other cases cover dark ink contrast, enabled navigation callbacks, Library/Home return, the former glass flag, an empty player and 320dp/1.5x text with real scrolling.
- All seven native navigation checks passed again after matching the preview fixture's font size and line height to AudioQualityPill. The preview is explicitly labelled test content.
- [PR CI run 40620336115](https://github.com/yohandeku32/nusamusic/actions/runs/40620336115) compiled source cf5dbbec2814e0cacdc2d44cd0db7101f857e4d3 and ran 626 tests: 623 passed, with only the same three pre-existing artist-layout failures. All Hi-Res-material checks passed. Lint reported 0 errors, 119 warnings and 3 hints.

These are native-rendered test fixtures, not physical-device captures or personal music. Device frame pacing and real collections remain acceptance checks. The Hi-Res badge's production metadata classification and text are unchanged.
