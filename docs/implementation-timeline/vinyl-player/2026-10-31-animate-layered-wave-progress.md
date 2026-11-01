# Animate layered line-wave progress immediately

> Date: 2026-10-31
> Status: verified locally; physical-device startup and frame pacing unverified
> Change type: Vinyl progress presentation and perceived readiness
> Scope: line-wave rendering, motion lifecycle, and pending-analysis presentation

## Direction and behavior

The owner's new image replaces the filled full-song envelope with three fine, overlapping curves, flattening at both edges. Recreate that visual principle directly with Compose paths in Vinyl's existing cream/neutral palette; no source bitmap, branding, or asset enters the app. ENERGY 2 / RHYTHM 2 / MOTION 2 remain appropriate: a slow wave indicates active playback while the turntable remains the screen's focus.

Use three 1.4 dp strokes within a 40 dp drawing area inside the existing 48 dp Slider. Keep the timestamp row, seek semantics/actions, 36 dp margins, machine, metadata, quality, and footer geometry. Primary played ink is dark, secondary curves muted, and the unplayed portion gray. There is no filled silhouette or vertical cursor. All curves taper to the same baseline at their ends.

## Remove the readiness dependency

The previous renderer waits for the full-song decoder/cache loader to return, then draws a filled envelope. Before that completion, its `waveform == null` branch draws a straight line. This explains the user's report that the wave itself takes too long to appear; a cache miss can require full-file decoding before the curve is visible.

The new line-wave presentation is available on the first render whenever a song is selected. While amplitudes are pending or unavailable, a neutral 0.7 display level drives the explicitly stylized curves. Once source data arrives, interpolate the current playback position between adjacent cached PCM peaks and normalize with a square-root display curve. Silent source data stays flat. The curve carrier is a visual animation, not a literal oscilloscope, FFT, or whole-song amplitude graph. Its motion does not claim to measure beats or frequencies. This deliberately changes readiness behavior without claiming that full decoding became faster.

The existing decoder, cancellation, cache limits, format handling, and source-file privacy remain unchanged. No FFmpeg, microphone permission, dependency, network call, or source-audio write is added. Failed analysis retains usable animated progress with the existing unavailable amplitude state; an empty player stays flat and disabled for seek.

## Motion ownership and cost

Playback phase is a drawing-only state. Three reusable paths and a 97-point taper window are allocated per drawing cache; frame updates reset those paths rather than allocating a new path graph. A 3.6-second carrier cycle publishes phase updates no more often than every 33 ms. This is an update cap, not a measured device frame-rate guarantee.

Animation runs only while playback is active, the player page is selected, an actual song exists, the activity is RESUMED, and system motion duration scale is positive. Pausing, hiding the page, backgrounding, or disabling system animations freezes phase. Resuming restarts frame timing from the current phase rather than catching up for time spent inactive. Phase survives amplitude delivery so background analysis does not restart the animation. Seeking and time/color progress continue with reduced motion.

## Verification

All 46 selected tests pass: 19 native player UI, 10 PCM, 7 cache, 2 waveform composition lifecycle, 1 artwork, 3 lyrics, and 4 presentation preference checks. The native rendering regression creates real PCM peaks with quiet/loud/silent sections, checks that the stroke drawing remains open rather than filled, compares amplitude excursions at different playback positions, and retains semantic/touch seeking. Existing short-screen, metadata stability, margins, keyboard, and large-text checks remain green.

Controlled-frame tests show geometry changes during playback and exact pixel equality after pausing, hiding the page, backgrounding the activity, and setting motion duration scale to zero. The background capture uses previously measured bounds and the native view directly, because Compose's test semantics registry intentionally unregisters a non-resumed root. Another test renders curves before any PCM is supplied and proves they animate while analysis is pending. This protects the first-render readiness contract without a guessed sleep or a full-decoder completion dependency.

The 411 x 914 dp native reference-style fixture, compact 360 x 616 dp large-title case, and 800 x 914 dp wide render were inspected. PCM and album/profile placeholders are explicitly test content. Final debug assembly, Android Lint (zero errors), APK v2 signature, whitespace, and documentation-link checks pass. No Android device is connected. The readiness comparison is a dependency/first-render regression using absent PCM data, not a wall-clock phone benchmark. Physical device startup latency, compressed decoding, and frame pacing remain unverified.

## Sources

- [Line curves and motion](../../../app/src/main/kotlin/com/yohandeku32/nusamusic/ui/VinylWaveform.kt)
- [Progress integration](../../../app/src/main/kotlin/com/yohandeku32/nusamusic/ui/TurntablePlayer.kt)
- [Native player regressions](../../../app/src/test/kotlin/com/yohandeku32/nusamusic/ui/VinylPlayerUiTest.kt)
- [Original analyzer and cache contract](2026-10-31-add-source-audio-waveform.md)
