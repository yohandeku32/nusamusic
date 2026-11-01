# Move a wave packet with playback progress

> Date: 2026-10-31
> Status: verified locally; physical-device frame pacing unverified

The owner's clarification requires one group of waves traveling from left to right with song position, leaving a straight line behind it. Center three thin animated curves on the actual playback fraction and apply a compact taper within 16% of track width on either side. Outside that packet every point returns to the baseline, including the already-passed region. Played ink advances with the same fraction; remaining track stays gray.

Preserve immediate appearance, source amplitude when ready, pause/visibility/reduced-motion behavior, margins, seek, timestamps, machine, and metadata. The carrier is a stylized progress animation, not an oscilloscope. Decoder and cache behavior are unchanged. ENERGY 2 / RHYTHM 2 / MOTION 2 remain appropriate.

All 22 selected checks pass: 20 native player UI and 2 waveform lifecycle. The native regression checks the packet near the left edge at 10%, near the right edge at 90%, a straight future region, a straight passed region after traversal, and a fully straight track at completion. Motion, amplitude, silence, keyboard/touch seek, and compact/wide layout checks remain green. Debug assembly, lint (zero errors), APK signature, whitespace, documentation links, and the native packet preview pass. Physical-device frame pacing is unverified.

Sources: [renderer](../../../app/src/main/kotlin/com/yohandeku32/nusamusic/ui/VinylWaveform.kt), [native checks](../../../app/src/test/kotlin/com/yohandeku32/nusamusic/ui/VinylPlayerUiTest.kt).
