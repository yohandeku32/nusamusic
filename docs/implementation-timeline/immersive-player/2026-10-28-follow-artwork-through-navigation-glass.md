# Follow artwork through navigation glass

> Date: 2026-10-28
> Status: verification in progress
> Change type: fix
> Scope: Now Playing navigation material and artwork-colour handoff

## Cause and outcome

The navigation's optical fallback used a mostly opaque grey gradient. The live Haze layer was drawn on top of that plate, with another fixed grey fallback tint. Even though the player already published the artwork palette, the stacked material obscured its hue. A cold track change also reset the navigation colour before the next prepared artwork arrived, while the ambient retained the previous real image.

[ImmersiveGlassPill](../../../app/src/main/kotlin/com/yohandeku32/nusamusic/ui/ImmersiveGlassPill.kt) now derives a dark translucent tint from the artwork itself, uses that tint for both live and platform-fallback rendering, and fades the optical fallback out as Haze fades in. White reflections and noise are lighter. The existing 20dp blur remains confined to the navigation; this adds no bitmap sampler, video decoder or dependency.

[MainActivity](../../../app/src/main/kotlin/com/yohandeku32/nusamusic/MainActivity.kt) retains the last prepared palette until a cached or newly prepared palette for the current track is ready. Existing URI guards prevent delayed artwork from an earlier track replacing the current palette. Static artwork and the first usable animated frame keep their existing palette owner. The existing colour tween respects platform motion scaling.

The user requested a less grey, less solid navigation surface that follows artwork. The existing geometry, destinations and hit areas remain; saved disabled-glass preferences and solid portal navigation retain their existing behavior. No Settings switch is reintroduced.

## Verification

Native render, contrast, navigation action, reduced-motion and compatibility checks are in progress. Results are recorded after completion. Physical-device Haze rendering and GPU/frame pacing remain acceptance checks; native test previews are labelled test content.
