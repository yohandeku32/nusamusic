# Add optional dynamic artwork glow

> Date: 2026-10-27
> Status: verified
> Change type: feature
> Scope: Immersive Now Playing artwork and appearance settings

## Intent and outcome

Dynamic light from animated artwork or Spotify Canvas can spread into the main player's existing blurred background. The setting **Dynamic artwork glow** is off by default and persists independently of the animated-artwork preference. Turning it off restores the existing frozen artwork ambience. When animated artwork is disabled, the setting explains that dependency while preserving the user's selection.

The accepted direction is one subtle artwork-derived ambient layer. Controls, library appearance, lyrics motion, provider priority, and audio playback keep their existing ownership.

## Implementation

- [MainActivity](../../../../app/src/main/kotlin/com/yohandeku32/nusamusic/MainActivity.kt) connects the preference, the current video instance, and the scene. Sampling requires the option, animated artwork, a visible player outside Lyrics, a resumed lifecycle, and a nonzero animation duration scale. Song and URL changes reset the dynamic image, and cancellation prevents old work from publishing into the next session.
- [DynamicArtworkGlow](../../../../app/src/main/kotlin/com/yohandeku32/nusamusic/ui/DynamicArtworkGlow.kt) stores the preference, diffuses spatial video colours, and renders the light. Dark samples contribute transparency rather than a black overlay. Opacity is bounded at 140/255 and decreases toward the transport controls.
- [ArtworkBlur](../../../../app/src/main/kotlin/com/yohandeku32/nusamusic/data/ArtworkBlur.kt) reuses the visible TextureView viewport. Dynamic captures have a longest edge of 96 pixels; existing callers retain their 320-pixel default. No additional decoder, media request, or dependency is introduced.
- Sampling is sequential, at least 200 ms apart plus processing time. Paused/buffering video or an unchanged playback position performs no readback. Pixel diffusion runs on Dispatchers.Default, while TextureView access stays on the UI thread. Slower system animation scales lengthen the interval, keeping the 160 ms image transition shorter than the interval.
- [ImmersiveArtworkScene](../../../../app/src/main/kotlin/com/yohandeku32/nusamusic/ui/ImmersiveArtworkScene.kt) composites the new light separately from the existing 700 ms track-change blur transition, below the light-appearance tint. It follows the same swipe fade as the visible video. Toggle off or hiding the player removes the light and cancels sampling; the original static capture remains available.
- [PlayerSettings](../../../../app/src/main/kotlin/com/yohandeku32/nusamusic/ui/PlayerSettings.kt) uses the existing localized toggle component and exposes the option only for Immersive.

## Verification

- Android Kotlin compilation passed.
- The focused run passed 20 tests: 12 new glow tests (7 pixel/preference/sampler tests and 5 native Compose tests) plus 8 existing ambient/blur tests. Coverage includes default off, persistence, source hue/position, dark and neutral input, unchanged playback, cancellation before publish, capture retry, zero duration scale, native toggle interaction, narrow light appearance, actual rendered light, and pixel-identical restoration after switching off.
- Native screenshot fixtures exercise the production scene and player. The final focused run also passed after coupling moving artwork and light, capturing 41 frames and asserting changes below the sharp artwork region.
- Android `:app:lintDebug` passed. CI verification is recorded in PR #178; it must be read separately from these local checks.

## Limitations and maintenance

Native tests use generated colour fields identified as test previews, not licensed album art or a recording of live provider playback. Physical TextureView readback, provider streams, GPU/battery cost, real-device frame pacing, and accessibility tooling still require device verification. Passing pixel and Compose tests does not establish those properties.

Retain the default-off preference and original frozen blur path. Do not feed frequent video samples into the track-change Crossfade or publish dynamic colours into the transport palette. Keep captures serial, recycle captured inputs after background processing, and let Compose release images after their fade instead of recycling an image still being drawn.

The PR's pre-existing artist-title tests have three known failures; a whole-suite result must distinguish those from any new regression.
