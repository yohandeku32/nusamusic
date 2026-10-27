# Add a first-launch welcome with music import

> Date: 2026-10-27
> Status: verified
> Change type: feature
> Scope: First launch, music-source handoff, and empty CD library recovery

## Accepted outcome

A new installation opens one short welcome screen, then lets the listener choose a music folder, find device audio, or defer setup. The welcome uses the existing Aruna disc icon and the default CD player's charcoal, warm text and lime action. No new logo, account, feature carousel or external image asset is introduced.

The record has one finite 420 ms entrance. Actions remain usable immediately, and system animation scaling controls the animation. Indonesian and English copy follow the current device language.

## Ownership and flow

- [ArunaIntro](../../../app/src/main/kotlin/com/yohandeku32/nusamusic/ui/ArunaIntro.kt) owns the completion preference, welcome host, visual screen, and shared MusicImportActions.
- [MainActivity](../../../app/src/main/kotlin/com/yohandeku32/nusamusic/MainActivity.kt) evaluates existing-use evidence before startup caches initialize and wires the host to its existing folder picker and runtime-permission launcher. The main Compose player tree is entered after the welcome decision; Activity/session initialization retains its existing ownership.
- **Choose a music folder** completes the welcome, opens Songs, and invokes the existing Android folder picker. **Find music on this device** completes the welcome, opens Songs, then explicitly requests audio access if needed. **Not now** completes the welcome and enters the player without requesting access.
- Cancelled folder selection or denied audio permission leaves the library available. The empty CD library now provides the same two import actions, so choosing a folder remains available as a recovery path. A genuinely empty library with access also offers folder selection; filtered-to-zero results do not acquire that import suggestion.
- Automatic startup permission requests are removed. Existing manual access actions remain available; opening or reopening the application alone no longer prompts for audio access.
- The welcome host uses saved instance state for its visibility and the library destination, without repeating a picker/permission request during restoration. Completion uses ui_preferences/welcome_completed_v1.

## Existing installations and interruptions

If the marker is absent, existing UI preferences, library preferences/folders or playback history migrate directly to completed. Existing configured listeners are not sent through the new welcome. A fresh installation writes an unfinished marker before other startup preferences can be created, so closing the app during the welcome does not make the next launch look like an existing installation.

The marker is completed when a listener chooses an exit action, not when audio access succeeds. This records that the welcome was seen; it does not claim a folder was imported or permission was granted. Android Back can leave the unfinished welcome without falsely completing it. Already granted audio permission alone is not treated as evidence that a fresh installation was previously used.

## Verification

- All 16 new checks passed: five preference-policy checks and eleven Compose/native UI checks. They cover fresh/unfinished/completed installs, existing settings and library migration, explicit folder/device/defer choices, callback-based recovery after cancellation/denial, no automatic access request, saved-state restoration, Indonesian/English, keyboard activation and visible focus, zero animation scale, a finite entrance, portrait, narrow 2x text, landscape and tablet layouts.
- Native previews render the production welcome with the existing app icon. Tests also use a small test-only destination to observe the welcome host's route and recovery callbacks; those assertions are not a recording of the Android picker or permission dialog.
- The combined focused local run passed 29 tests: all 16 intro checks, 7 library checks, 3 search-window checks and 3 presentation-mode checks. All 16 intro checks passed again after using Compose LocalActivity for navigation-bar ownership. Android `:app:lintDebug` passed.
- [Full source verification](https://github.com/yohandeku32/nusamusic/actions/runs/40492327802) for `7a10f7187e71026481166ab5d7ee6c45e8830c74` compiled successfully and ran 606 tests: 603 passed, with only the three pre-existing artist-layout failures. All new intro tests and all library/search checks passed. CI uploaded First-launch-previews.

## Limits and maintenance

Physical clean-install/update flows, real document providers, runtime-permission UI and permanent-denial behavior, OEM navigation/IME transitions, and device frame pacing remain unverified. Folder import and permission callbacks reuse the existing Activity contracts; their actual system handoff still needs device validation.

Keep the unfinished marker authoritative even if other caches appear during a pending first launch. Do not infer first use solely from a granted permission or overwrite an existing presentation preference. Keep access actions explicit and the empty library's folder recovery reachable.
