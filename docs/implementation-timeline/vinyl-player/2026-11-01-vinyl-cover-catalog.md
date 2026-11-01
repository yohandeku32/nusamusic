# Vinyl cover catalog, option C

> Date: 2026-11-01
> Base: main 6b987fd6 (PR #184)
> Status: verified; APK and PR prepared

The owner chose the two-column catalog concept and asked to use merged main. PR #179 is merged into the former development branch and its 9174aca8 commit is an ancestor of main. Start this change from main 6b987fd6, including subsequent Classic size/startup and dual-palette changes. Main also contains the intentional removal of the old Studio Black implementation (11c500db); do not resurrect that implementation as part of the catalog.

Replace overlapping record sleeves with a light, ivory cover catalog in Vinyl mode. Square original artwork sits in front of a static grooved record peeking to the right; title and artist sit below. Selected track has an outline and play/pause marker. Use two columns on ordinary phones, one for narrow/enlarged-text layouts, and three/four on wider layouts. Cap content at 920 dp. Reuse the current VinylPainter and existing artwork loader/cache rather than adding concept images or generated covers to the app.

Hoist query and LazyGridState in MainActivity. Filter actual title/artist case-insensitively while retaining sorted order and stable song-ID keys; playing a result uses the visible result queue. Search changes reset the grid to the first result without playing. Empty results retain search and clear actions. Native IME padding is scoped to the Vinyl library because the activity uses adjustNothing. Search IME action and track/menu selection dismiss the keyboard.

Keep the existing sort menu, back-to-player route, folder/permission/scanning/empty-library states and delete confirmation. The card overflow menu and long press both request the same existing delete confirmation. Current-track taps toggle playback; other cards play the correct song. The bottom panel exposes current artwork/title/artist and play/pause alongside the existing shuffle/repeat controls. Its reserved space scales with text. Opening track info returns to the player. Alphabet navigation uses song indices in the grid and rebuilds targets from filtered results; it no longer addresses stale list rows. Pause uncached image reads during grid scroll while cached covers remain visible.

A pre-existing main regression was exposed by the hue sweep: ColorUtils.blendARGB at 0.09f can truncate alpha to 254, after which calculateContrast rejects the translucent background (#febbc3ec). Preserve RGB and explicitly restore alpha 255 after each blend. Existing all-hue contrast coverage proves the repair; do not change the accepted dual-palette visual treatment.

Direction: option C supplies cover arrangement, neutral palette and metadata hierarchy. Real app navigation and import remain authoritative. Preview titles/art are labelled local fixtures; production uses actual song metadata/art. ENERGY 2 / RHYTHM 2 / MOTION 1 for a static browsing catalog. Physical-device keyboard/IME behavior, TalkBack and frame pacing remain unverified.
A second existing main check failed Android Lint because VinylDeckArtworkTest created mutableStateOf(0L) inside composition despite already hoisting position. Pass the existing position state into the fixture. Catalog keyboard activation also preserves the focused card after Enter while dismissing the software keyboard.

Verification: all 48 selected tests passed (catalog 7, player 27, artwork 3, tint 4, library states 7). assembleDebug and lintDebug succeeded on 2026-11-01. Native renderer previews checked at phone, 320 dp with 200% text, and 800 dp width. The catalog tests exercise selection, toggle, overflow deletion, long press, search/clear/empty results, scrolling, keyboard activation/focus and mini-player navigation. Physical-device IME/TalkBack remain unverified.
