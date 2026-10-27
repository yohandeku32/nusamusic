# Keep library rows readable and overlays consistent

> Date: 2026-10-27
> Status: partial
> Change type: fix
> Scope: Immersive library, search, and song deletion confirmation in PR #178
> Based on: b1ecf9ecbc4ad826efe5ff4f278b2f2f1f6f4a49

## Accepted outcome

Keep the existing artwork and compact track presentation. Correct the four approved audit findings: final-row navigation clearance, destructive text contrast, confirmation theme ownership, and Indonesian/English labels with the Aruna display name.

## Implementation

- `ImmersiveArtworkPlayer` passes bottom content padding through `libraryContent`, using the measured host and navigation bounds. The calculation subtracts the system bottom inset already consumed by the library and adds a 12dp reading gap. A 96dp reserve applies before bounds are ready. `ImmersiveMusicLibrary` retains a 24dp default for standalone presentation.
- `ImmersiveColors.destructiveText` defines light `#B3261E` and dark `#FFB4AB` labels. Menu and confirmation share the token; overlay surfaces use the neutral library palette.
- The confirmation is inside the library's existing `MaterialTheme`, independent of system appearance. Cancel, dismissal, and the Android deletion request keep their original callbacks.
- `ImmersiveLibraryText` follows the player's existing device-language convention. Labels, counts, search, sort names, accessibility descriptions, and the warning use Indonesian or English. The warning names Aruna; track metadata, stored sort keys, and application ID are preserved.

Source owners are in `app/src/main/kotlin/com/yohandeku32/nusamusic/ui/`, with the overlay callback connected from `MainActivity.kt`. CD, Vinyl, lyrics, artwork resolution, and playback queue behavior are outside this change.

## Verification

Verification is in progress. The focused regression suite is `ImmersiveMusicLibraryTest`. Tests cover opposite parent/library themes, confirmation and cancellation, localized search and sorting, retained actions, and final-row clearance inside the actual overlay at normal and narrow/large-text sizes.

Calculated contrast on the final neutral surfaces: light **6.54:1**, dark **9.93:1**, both above 4.5:1 for normal text.

Physical-device rendering, touch behavior, and frame pacing remain unverified. Simulated screenshots and Compose assertions do not establish those results.

## Maintenance

Recheck clearance if the navigation anchors, safe-area ownership, or portal layout changes. Recheck destructive text whenever overlay backgrounds change. Library labels intentionally follow the existing locale mechanism; update the shared selector if the app adopts an independent language preference.
