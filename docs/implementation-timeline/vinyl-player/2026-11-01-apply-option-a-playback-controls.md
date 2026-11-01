# Apply option A playback controls

> Date: 2026-11-01
> Status: verified locally; physical-device and TalkBack inspection pending

The owner selected option A from the generated comparison: shuffle, previous, central play/pause, next, and repeat in one row below the waveform/timestamps. Previous/next become compact gray rounded squares with 14 dp corners; play/pause remains a black circle. Shuffle/repeat have no filled background and show a small red active dot. Repeat-one uses its own icon. Existing localized labels, state descriptions, and shared callbacks connect the row, menu, and machine controls to the same player state.

Keep the existing 76–92 dp transport layout slot so the machine, profile, metadata, waveform, and footer do not shift. Normal widths use 48 dp side targets and a 72 dp primary button; the 320 dp layout uses 44 dp sides and a 64 dp primary button to preserve the established gutters. Disable shuffle/repeat without a selected song while retaining the existing empty-player Play action. No audio engine, setting storage, dependency, or new animation is introduced.

Direction: ENERGY 2 / RHYTHM 2 / MOTION 2. The generated concept is visual guidance only, not an asset bundled into the app. All 27 selected tests pass (21 player UI, 4 library, 2 artwork). The new native regression verifies five-control order/non-overlap at 320 dp, minimum 44 dp targets, direct shuffle and all repeat states, keyboard activation, and full visibility. Existing empty-player checks now require the two new controls to disable. Standard and active compact native previews inspected. Final debug assembly, full lint (zero errors), signature, whitespace, and documentation links pass. Physical-device and TalkBack inspection remain outstanding.
