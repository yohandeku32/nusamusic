# Studio Black as a second Vinyl machine theme

> Date: 2026-11-01
> Status: verified

The owner selected Studio Black from the concept board and requested it as the second settings option while retaining the existing machine. Add a separate VinylMachineTheme with Classic first and Studio Black second under Settings > Player appearance > Vinyl machine theme. Show it only in Vinyl mode; do not add another player presentation mode.

Read/write vinyl_machine_theme through the existing UI SharedPreferences. Missing, unknown and incompatible stored values fall back to Classic without rewriting existing mode preferences. Selection updates the live renderer and persists across reads; changing to CD/Immersive keeps the Vinyl choice. Existing users keep the accepted silver machine by default.

Studio Black uses a charcoal matte chassis, sharper corners, an inset dark-metal edge and lower lip, dark platter metal and bearing/counterweight, chrome tube, a red indicator, short mechanical power toggle and repeat dial. The existing play and repeat targets/callbacks remain. Keyboard focus uses a light outline on the dark machine. The reference is visual inspiration only; no concept-board bitmap or source branding is bundled.

Pass the theme from app state through TurntablePlayer to the deck and tonearm. The record renderer, whole-surface rotation, shadows, cover retention, cover-derived lower gradient, metadata geometry, library, audio and lyrics remain shared. Reuse cached canvas geometry/shadows; no new animation loop or asset download. ENERGY 2 / RHYTHM 2 / MOTION 2 remain.

Native verification covers both machines with real loaded fixture artwork, classic render equality against revision 24, stable deck/title geometry across live theme changes, mechanical controls, settings selection/persistence, fallback values, switching CD/Vinyl, and 320 dp at 200% text. Physical-device appearance, TalkBack and frame pacing remain unverified. Revisit this record when theme storage, selector placement or machine rendering changes.
Verified: 37 selected tests pass, debug assembly succeeds and full Android Lint has zero errors. Settings and native machine/full-screen previews inspected after contrast correction. Classic fixture PNG remains byte-identical to revision 24. Whitespace and documentation links checked.
