# Add adjustable main-player scale

> Date: 2026-10-28
> Status: verified
> Change type: feature
> Scope: Immersive Now Playing scale, preferences, responsive controls and overlay boundaries

## Contract and ownership

The listener selected scaling the whole main player, including artwork, text and buttons. Settings now provides Now Playing scale from 100% to 125% in 5% steps, with Reset to 100%. Selection is written after each distinct step in ui_preferences/immersive_player_scale. Missing, malformed or non-finite preferences use 100%; finite values are clamped and rounded. Reading a default does not create a preference.

[MainActivity](../../../app/src/main/kotlin/com/yohandeku32/nusamusic/MainActivity.kt) owns the current value and persistence callback. [PlayerScaleHost](../../../app/src/main/kotlin/com/yohandeku32/nusamusic/ui/PlayerScale.kt) changes the player's layout density while retaining the system font scale. The provider stays mounted as the value changes. Controls reflow rather than being clipped by a whole-screen graphics transform. Sharp static/animated artwork receives the same magnification inside its feathered responsive frame; the existing ambient remains full bleed.

When the transport row no longer has its minimum useful width, shuffle/repeat use a separate row and the foreground can scroll. The navigation group keeps enough horizontal space for its touch targets. Existing slot centering and callback owners are retained.

Library and Discovery content return to the original screen density. Shared navigation interpolates back to that density as a full-screen page opens, preserving its measured centre; this keeps an expanded Library capsule within a narrow screen. Library bottom clearance is calculated in screen units. Settings stays outside the player scale provider. Actual system insets are retained, and the system's font-size preference remains effective.

The control is in the Immersive appearance group. It does not change Android display settings, saved title/lyric font sizes, music data or playback.

## Verification

- Four preference tests and six native scale tests passed. Fifteen related navigation, centering and glow checks passed in the combined run; the scale case was then rechecked using physical screen bounds rather than node-local dp units.
- Native tests cover 100% pixel equality with the unwrapped player, enlargement of artwork and physical touch targets, actual taps, 320dp/568dp with 1.5x system text, 800dp/400dp landscape, real Library scrolling and final-row clearance, normal Library density, keyboard adjustment, persisted selection and Reset.
- Default-player pixels were also compared with the preceding Hi-Res native preview and were unchanged. Lint completed with 0 errors, 120 warnings and 3 hints.
- [PR CI run 40625507465](https://github.com/yohandeku32/nusamusic/actions/runs/40625507465) compiled source 289a7cba683bbf8853e78e01a241cca1f427e315 and ran 636 tests: 632 passed. All ten new scale checks passed. The remaining failures are the three pre-existing ImmersiveArtistHeroTest layout cases and the previously intermittent indonesianLibraryLocalizesSortingSearchMenusAndWarning failure. That search test also failed before this feature (run 40616469091) and passed in the preceding Hi-Res run 40620336115. No claim is made that this feature repairs that intermittent failure.
- Player-scale-previews was uploaded as artifact 12789052737. The final source change after verification records results only.

Previews use labelled test artwork and synthetic song rows. Physical-device animated artwork, screen-reader operation, frame pacing and real collections remain acceptance checks.
