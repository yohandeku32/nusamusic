# Add the silver turntable chassis and mechanical controls

> Date: 2026-10-31
> Status: verified locally; physical-device verification pending
> Change type: UI redesign
> Scope: Vinyl machine, recessed platter, tonearm, and mechanical playback controls

## Direction and boundaries

The owner's supplied turntable image governs this redesign: a square silver-gray shell with rounded corners, an inset black record, a large artwork center, a slender curved arm on the right, and small physical controls at the lower corners. The image is visual reference only; no source bitmap, logo, or album art is copied into the app. The existing cream page, serif metadata, normal transport, quality badge, first-artist portrait lookup, and half-height lyrics remain in place.

Visual thesis: a compact silver music instrument with a recessed black platter and functional mechanical controls, while the surrounding player stays quiet and readable. ENERGY 2 / RHYTHM 2 / MOTION 2. Material gradients explain metal, recesses, and moving parts; shadows belong to the housing, record contact, and tonearm. The album artwork remains the principal color accent.

## Implementation

- [VinylTurntableDeck](../../../app/src/main/kotlin/com/yohandeku32/nusamusic/ui/VinylTurntableDeck.kt) draws the shell, concentric platter well, subtle seeded material grain, status light, power lever, and repeat rocker. Brushes and material geometry are cached by measured size.
- [VinylTonearm](../../../app/src/main/kotlin/com/yohandeku32/nusamusic/ui/VinylTonearm.kt) uses one 360-unit coordinate system for a slimmer curved tube, counterweight, bearing, cartridge, and stylus. Only the moving assembly follows progress; the base stays mounted to the chassis. The empty player parks the arm outward, and disabled system animation uses a direct state change.
- The real record renderer and artwork loader remain in use. The center label grows to 56% of disc diameter to match the new reference proportions. A tighter contact shadow keeps the record inside the recessed platter.
- The left lever calls the existing play/pause action, including its empty-library recovery. The right rocker cycles the existing repeat mode and is disabled without a song. It does not claim to change RPM or playback speed. Material interaction surfaces provide independent 48 x 56 dp targets, ripple feedback, keyboard focus borders, localized names, and current-state semantics.
- The existing bounded player reserves at least 112 dp for the machine stage and an 8 dp allowance for its upper hardware. The chassis is capped at 416 dp and the record occupies 79% of it. Metadata height stays independent of the current title, preserving machine/profile/control positions as songs change. Extreme text scaling keeps the existing readable-height scroll fallback.

## Verification

All 43 selected checks pass: 13 native player UI, 1 native artwork render, 3 native lyrics, 22 portrait resolver/cache/queue, and 4 presentation preference tests. Machine regressions exercise both physical switches, repeat state transitions, short-screen touch bounds, keyboard operation, unchanged title/profile geometry, one-screen quality visibility, and large-text reachability. Standard, short, large-title/navigation-space, wide, and artwork fixture previews were inspected.

Debug assembly and APK v2 signature verification pass. Android Lint passes without errors, retaining existing project warnings. Its fixture-only state-creation finding was fixed by hoisting the test position state outside composition; the artwork test was rerun afterward and passes. Whitespace and documentation-link checks also pass.

The artwork fixture passes synthetic embedded picture bytes through the production ArtworkLoader and VinylRecord renderer. Its pink label explicitly reads TEST COVER and is not shipped product content. A close-up native preview is generated separately from the page previews, whose empty artwork placeholder remains intentional.

An initial keyboard test attempted to request focus while the harness remained in touch input mode. It now switches the test environment to keyboard mode before asserting focus and activation, as the existing intro/lyrics tests do. Production keyboard behavior is not replaced with test-only handlers.

## Limits

No physical Android phone, TalkBack journey, or hardware frame pacing was verified. The existing rotating record still honors playback visibility and system animation settings. No new network provider, binary artwork asset, font, or dependency is introduced by this redesign. Reference-specific branding and speed labels are deliberately absent because the app retains its own identity and actual playback controls.
