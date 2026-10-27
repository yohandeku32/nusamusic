# Flowing sentence motion

> Date: 2026-10-27
> Status: verified
> Change type: fix
> Scope: Immersive synchronized lyrics in PR #178
> Based on: 5445274f6110e62e458bf846c09ddeb1d25c64f2

## Accepted outcome

Replace the rigid shared-panel impression with sentence-level following: the active sentence leads and upcoming sentences follow with restrained spatial lag. Keep authored word timing, existing character expression, duet sides, translations, backing windows, seek and manual browsing intact. Apple Music is a motion reference supplied by the user; no Apple code, artwork or measured algorithm is used.

## Ownership

- [SyncedLyricsView](../../../app/src/main/kotlin/com/yohandeku32/nusamusic/ui/SyncedLyricsView.kt) retains layout, focus, the scroll mutation and interaction arbitration. Consumed auto-scroll feeds the new visual followers. Audio-time deltas stop follow movement while paused.
- [LyricsLineMotion](../../../app/src/main/kotlin/com/yohandeku32/nusamusic/ui/LyricsLineMotion.kt) keeps per-row position/velocity. A critically damped analytic response is faster at the active cue and slower for upcoming cues. Onset ranks keep simultaneous duet voices in phase. Only the visible range advances; retained observable row state lets the first draw subscribe before movement begins.
- One draw-layer transform surrounds main lyric, translation and attached backing. Countdown is outside that transform. The old opacity-derived main-only lift is removed so two movement systems do not stack.

Offsets are nonnegative and ordered, capped at 35% of the shortest visible row height within 8–32dp. This bounds spacing changes without remeasuring text each frame. Seek/visibility/viewport boundaries, reverse rejoin and direct browsing clear the additional offset. The coroutine's `MotionDurationScale` suppresses sentence followers at zero, including a preference change during a paused transition.

## Verification

Local Android compilation succeeded. Initial focused verification completed **392 tests with zero failures** (`:app:testDebugUnitTest --tests '*Lyrics*'`). This includes:

- 12 follower-model regressions covering phase differences, continuity on cue changes, ordered/bounded positions, pause, reset, equal-onset voices, recycling, reduced motion, comparable 60/90/120 Hz positions, and snapshot observation after first draw/reset.
- Four native Robolectric/Compose tests exercising distinct rendered sentence travel, pause during transition, zero-duration-scale while paused, tap-to-seek, manual browsing, and 320dp/1.5x wrapped text.
- Existing lyric timing, focus, playback-clock, duet/grouping and character-motion regressions.

An additional native regression covers refreshed duet metadata with unchanged lyric content. The follow effect is keyed to the display grouping so a replacement follower stays connected to the scroll loop.

The native test produces 43 playback-driven PNG frames in `app/build/reports/lyrics-motion-preview/`; CI uploads them as `Lyrics-motion-previews`. All words in the preview are test fixtures. The frames are actual component renders rather than a separately recreated animation.

Physical-device frame pacing, platform GPU blur and visual equivalence to another player remain unverified. The PR's existing unrelated artist-layout failures must be assessed separately from this focused lyric run.

## Maintenance

Keep the draw-layer observable state stable across resets. An unobserved zero offset at first draw can leave correct numeric motion invisible. Keep follow and visual deltas on the playback clock, and exercise paused transitions if changing the mutation loop. Recheck equal-onset voices and spacing bounds if display-row grouping changes. [Lyrics guide](../../EMBEDDED_LYRICS.md) documents current behavior.
