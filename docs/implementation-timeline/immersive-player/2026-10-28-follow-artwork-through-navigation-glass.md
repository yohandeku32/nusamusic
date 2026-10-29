# Follow artwork through navigation glass

> Date: 2026-10-28
> Status: verified with device acceptance remaining
> Change type: fix
> Scope: Now Playing navigation material and artwork-colour handoff

## Cause and outcome

The navigation's optical fallback used a mostly opaque grey gradient. The live Haze layer was drawn on top of that plate, with another fixed grey fallback tint. Even though the player already published the artwork palette, the stacked material obscured its hue. A cold track change also reset the navigation colour before the next prepared artwork arrived, while the ambient retained the previous real image.

[ImmersiveGlassPill](../../../app/src/main/kotlin/com/yohandeku32/nusamusic/ui/ImmersiveGlassPill.kt) now derives a dark translucent tint from the artwork itself, uses that tint for both live and platform-fallback rendering, and fades the optical fallback out as Haze fades in. White reflections and noise are lighter. The existing 20dp blur remains confined to the navigation; this adds no bitmap sampler, video decoder or dependency.

[MainActivity](../../../app/src/main/kotlin/com/yohandeku32/nusamusic/MainActivity.kt) retains the last prepared palette until a cached or newly prepared palette for the current track is ready. Existing URI guards prevent delayed artwork from an earlier track replacing the current palette. Static artwork and the first usable animated frame keep their existing palette owner. The existing colour tween respects platform motion scaling.

The user requested a less grey, less solid navigation surface that follows artwork. The existing geometry, destinations and hit areas remain; saved disabled-glass preferences and solid portal navigation retain their existing behavior. No Settings switch is reintroduced.

## Verification

- Fifteen focused checks passed: seven new native material tests plus the existing navigation-centering and glow UI checks. They cover artwork hue changes, visible backdrop detail, a single material at full Haze reveal, waiting for a missing source, white-glyph contrast on white artwork, unchanged disabled appearance, reduced motion, and navigation callbacks.
- The reduced-motion regression changes Android's actual animator flag after mounting the surface and advances/waits for each frame. Its exact pixel comparison retains the requirement that the colour is already final within three frames. Advancing a single 48ms block did not synchronize the view/recomposition work and produced a false intermediate snapshot.
- The Haze tests explicitly use the platform scrim path. Native software-canvas capture cannot exercise the hardware blur backend; the full player callback check uses its saved disabled-glass path. These tests do not claim physical-device Haze verification.
- [PR CI run 40617411150](https://github.com/yohandeku32/nusamusic/actions/runs/40617411150) verified source `399ed208cf7229f6005e8f29c909d215926e48c2`: compilation passed, 626 tests ran, and 623 passed. The only failures were the same three pre-existing ImmersiveArtistHeroTest layout cases. All seven navigation-glass tests and the existing library, glow and lyric checks passed. The workflow remains red for those existing failures.
- Native test previews are labelled test content and uploaded as Navigation-glass-previews (artifact 12785757734). Final `:app:lintDebug` passed locally: 0 errors, 120 warnings and 3 hints. Hardware blur and device frame pacing were not measured.

Physical-device Haze rendering, GPU/frame pacing and storage/loading delays on real collections remain acceptance checks.
