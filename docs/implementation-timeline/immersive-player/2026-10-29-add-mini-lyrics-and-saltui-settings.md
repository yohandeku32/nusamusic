# Add mini lyrics and pilot SaltUI lyric settings

Date: 2026-10-29

## Request and result

The listener requested one animated lyric line above the normal player's title, with a saved Settings toggle, and then requested trying SaltUI in Aruna. Immersive Settings now provides **Mini lyrics**, off by default. The Lyrics section uses SaltUI for Mini lyrics, Online lyrics and Reload lyrics. Existing Settings navigation, preview and size slider retain their implementation.

## Mini lyric contract

- The optional row uses the existing parsed lyric state, source preference and online-lookup policy. With online lookup disabled, stale online entries cannot render; embedded and saved-in-app entries remain eligible.
- Only the active main line appears. Backing-only passages, intros, long rests, missing lyrics and ended lines are blank. Overlapping voices select one stable main anchor. Line-synced lyrics remain supported.
- The main syllable painter renders the row at 18sp from its 34sp coordinate system, retaining authored syllable joins, timing, highlight handoffs and isolated held-note expression. Pronunciation and sentence translation do not enter this row.
- Text stays on one unwrapped line. An overflowing line pans with its vocal cursor, including RTL direction, inside a local clip. Large system fonts remain effective. Light player mode uses dark ink.
- The full and mini views share a lifecycle-aware Media3 frame driver. Pause freezes the pose, forward/backward seek evaluates the destination, and background lifecycle stops frame sampling. Reduced motion removes extra geometry while retaining timed highlighting.
- With the setting enabled, a reserved row keeps title/transport layout stable while full lyrics or overlays hide the mini view. Disabled restores the prior player pixels exactly. Hidden settings/pages/full lyrics do not mount the mini host.

## SaltUI decision

[SaltUI](https://github.com/Moriafly/SaltUI) is a Compose component library. This pilot pins [2.6.0-beta03](https://github.com/Moriafly/SaltUI/tree/2.6.0-beta03), whose Kotlin 2.2.10 / Compose 1.9 generation matches Aruna. Aruna's existing BOM resolves Compose UI to 1.9.4. Current SaltUI 3.x uses a newer toolchain; upgrading the whole app is outside this pilot.

ArunaSaltTheme maps the current Material palette to Salt's local colors and dimensions. Real ItemSwitcher and ItemButton components emit callbacks into Aruna's existing preferences and lyric reload flow. This changes control rendering, not Media3 playback or TTML parsing. The existing Settings section owns outer spacing.

The dependency is a beta and remains narrowly scoped. Apache-2.0 license text from the pinned tag is bundled in assets and listed in THIRD_PARTY_NOTICES. Its transitive hiddenapibypass module is excluded because the public controls used here do not require reflection/window helpers; the resolved runtime graph confirms its absence. No dependency-information hiding is configured.

## Verification

- 506 focused lyric, metadata/writer and player-scale tests passed, with no failures/errors/skips. This includes 14 new mini lyric, frame-clock and actual Settings checks.
- Native Compose/Robolectric checks cover timed/line-synced content, one-row selection, pause, held-note movement, backward/forward seeks, track changes, lifecycle cancellation, reduced motion, light ink, 320dp/font 1.8, real Salt callbacks, reload loading guard, preference reopen and exact disabled restoration.
- Android compilation and lintDebug passed with zero errors. Native previews were inspected. Repository-wide CI and physical-device GPU/frame pacing remain separate checks; no APK was assembled.

## Maintenance

Start with MiniLyrics.kt, LyricsFramePlayback.kt, ArunaSaltTheme.kt and PlayerSettings.kt. Retain the shared painter and source policy when extending the mini view. Expand SaltUI beyond these controls only with explicit product scope and compatibility validation.
