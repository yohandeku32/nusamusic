# Keep the search dialog status bar hidden

> Date: 2026-10-27
> Status: verified
> Change type: fix
> Scope: Immersive library search window

## Outcome and cause

Opening search must retain the player's hidden status bar. Search uses a separate Compose Dialog window, but only the Activity window previously requested status-bar hiding. Edge-to-edge dialog layout did not carry that visibility request into the new window.

[ImmersiveSearchDialog](../../../app/src/main/kotlin/com/yohandeku32/nusamusic/ui/ImmersiveSearchDialog.kt) now configures its own window through DialogWindowProvider and WindowInsetsControllerCompat. It hides only statusBars and retains the transient-by-swipe system behavior. A window-focus listener reasserts the request when focus returns and is removed when the dialog leaves composition.

The repair uses the existing dialog owner, not a timer or a change to the Activity. It avoids FLAG_FULLSCREEN so the existing IME layout contract remains available on older Android. Navigation-bar visibility, query persistence on dismissal, keyboard actions, results, and source selection keep their existing behavior.

## Verification

- A regression test first failed against the original dialog: the dialog requested status-bar visibility even after its Activity requested that status bars be hidden (requested status bit 1, expected 0).
- After the repair, all 3 new [search-window regressions](../../../app/src/test/kotlin/com/yohandeku32/nusamusic/ui/ImmersiveSearchWindowTest.kt) passed on Robolectric Android 14. They check status-only hiding with text input and IME action, focus loss/return after an explicit reveal request, reopening with a new dialog window, and isolation from the Activity's visibility request.
- All 7 existing ImmersiveMusicLibraryTest checks passed in the same run. Android compilation passed as part of the run; no whitespace errors were found.
- Android `:app:lintDebug` passed.
- [Full CI source verification](https://github.com/yohandeku32/nusamusic/actions/runs/40482286647) for `62a6202f904bdae24b7606cefe2d048d16e37a75` compiled successfully and ran 590 tests: 586 passed and 4 failed. All three new search-window tests passed.
- The remaining failures were the three pre-existing ImmersiveArtistHeroTest checks and IllegalArgumentException at ImmersiveMusicLibraryTest.kt:171 during its Indonesian search-dismissal check. The identical additional failure occurred [before this repair](https://github.com/yohandeku32/nusamusic/actions/runs/40478472437/attempts/1), then passed on that source's CI retry. It also passed in the current focused local run. The cause of that existing intermittent runner failure remains unresolved; the full suite is not reported as green.

## Limits and maintenance

These are window-policy and Compose-interaction tests. They do not render the physical Android status bar or a real keyboard. On-device verification remains necessary for device-specific IME transitions, status-bar rendering, Android versions outside the tested runtime, and navigation modes.

Keep the policy scoped to the dialog window and target statusBars only. Preserve the existing IME-aware layout and remove the focus listener on disposal. A system edge swipe may temporarily reveal bars, consistently with the player's existing transient-bar behavior.
