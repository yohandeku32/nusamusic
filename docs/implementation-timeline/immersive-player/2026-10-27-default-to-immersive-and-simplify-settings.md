# Default to Immersive and simplify Settings

> Date: 2026-10-27
> Status: verified
> Change type: feature
> Scope: Presentation defaults, Settings, and first-run library recovery

[PlayerPresentationMode](../../../app/src/main/kotlin/com/yohandeku32/nusamusic/model/PlayerPresentationMode.kt) now resolves absent, unknown, empty or unavailable preferences to Immersive. Explicit saved CD and Immersive choices retain their meaning. The same resolver governs startup artwork preparation, lookahead and the visible player; it does not rewrite saved preferences.

The Liquid Glass row, callback parameters and GitHub action/address were removed from [PlayerSettings](../../../app/src/main/kotlin/com/yohandeku32/nusamusic/ui/PlayerSettings.kt). App version and developer credit remain. Existing glass rendering and its saved preference are retained at their existing owner; this change removes the Settings control.

The first-launch import choices now remain available in the empty Immersive library, using the same MusicImportActions as the welcome. Both production library call sites supply the real folder picker. Empty-state scrolling reserves the measured navigation clearance outside the scroll viewport, so folder/device/scan actions remain reachable with enlarged text. Normal song rows and explicitly selected CD mode retain their existing behavior. The welcome keeps its existing visual palette; its destination follows the new presentation default.

## Verification

- The combined presentation/intro/library/search run passed 32 checks. New cases cover a missing preference choosing Immersive, invalid/legacy fallback, saved-mode preservation, actual empty-library import callbacks and rescan access at narrow width with large text.
- Android compilation passed. Source inspection confirms no Liquid Glass setting, GitHub label/address, URI handler or obsolete Settings callback remains.
- The final full local run completed 619 tests: 616 passed, with only the three pre-existing artist-layout failures. `:app:lintDebug` passed. [PR CI run 40499592898](https://github.com/yohandeku32/nusamusic/actions/runs/40499592898) verified application source `68d303992212042f663cafe3578d5857f1107551`: compilation passed and 619 tests ran, with 616 passing and only the same three pre-existing `ImmersiveArtistHeroTest` failures. All changed default/import, folder-refresh and original-file lyric tests passed. The workflow is red because of those existing failures; no APK was assembled.

Physical first-run handoff and OEM viewport behavior remain device acceptance checks. See [onboarding](../onboarding/README.md) for the welcome contract.
