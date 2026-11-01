# Make the black record surface visibly rotate

> Date: 2026-11-01
> Status: verified

The owner observed that only the artwork appeared to turn. The face and grooves already used the cover's transform, but concentric geometry is rotationally symmetric and the dominant sheen remained stationary. Move the sheen into the same rotating layer as the grooves and cover so the black playing area has a clear moving cue. Keep the spindle, cast/contact shadows, platter, tonearm and chassis stationary.

This supersedes the prior stationary-lighting choice for the record's sheen. Reuse the existing angle, acceleration/deceleration and transform; no second clock, layout motion, per-frame bitmap generation, or new visual asset is introduced. Preserve the accepted record proportions, center edge, shadow boundaries, and player controls.

Extend the controlled-frame native rotation check to inspect only an outer annulus, excluding the cover: substantial luminance change must occur there during play. Keep pause settling and resume checks. Physical-device appearance and frame pacing remain unverified. ENERGY 2 / RHYTHM 2 / MOTION 2 remain unchanged.
Verified: all 31 selected tests pass, including the black-surface annulus comparison and pause/resume checks. Native frames inspected show the sheen moving with the cover. Debug assembly and full Android Lint pass without errors; physical-device appearance remains unverified.
