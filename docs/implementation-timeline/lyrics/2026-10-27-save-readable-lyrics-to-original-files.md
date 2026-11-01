# Save readable lyrics to the original audio metadata

> Date: 2026-10-27
> Status: verified
> Change type: fix
> Scope: Explicit online-lyric Save, standard metadata fields, verification and Aruna naming

## Accepted contract

Online lyrics are written after the listener presses Save, not automatically after matching. A successful Save must update the original audio's metadata rather than only showing a cached or queued copy. An active audio source still waits for playback to release it; the clock and initial message now describe that pending state instead of claiming a completed save. Success is emitted only after original-file verification.

## Correction

The previous writers could preserve old standard readable fields while adding newer authored timing to an app-specific field. That left other players/tag editors showing the old lyric, and M4A's authoritative standard text could supersede the new timing on readback.

- MP3 now replaces canonical/default USLT and old owned descriptions, places the new standard lyric before retained alternatives, and updates TXXX:LYRICS. Named alternate translations and unrelated tags remain.
- FLAC replaces standard readable LYRICS and old owned fields, writes ARUNA_LYRICS, and migrates an old app-owned vendor string. Other vendors and non-lyric comments remain.
- M4A updates ©lyr and writes com.aruna:LYRICS, replacing old owned freeforms. Audio offsets, artwork and unrelated atoms remain unchanged.
- EmbeddedLyricsReader accepts both new and old owned FLAC fields. Existing files are changed only by explicit Save.
- Unsupported file extensions are rejected before queueing. A failed queued Save can prompt review of its folder permission on retry. Requeueing removes the stale failure marker only after the new request is durable.
- AudioLyricsEmbedder compares original identity, verifies staged lyrics and committed bytes, and verifies rollback after failure. The last intact backup is kept in app data if recovery fails, independent of disposable caches. Cancellation during rollback does not prevent restoration.
- The explicit queue operation schedules an inactive-file drain as well as retaining the track-transition trigger. No change makes an active decoder source writable.

Pending/success descriptions and folder-review messages use Aruna. Generated metadata identifiers and network client names now use Aruna; legacy reader names, storage keys and Android application identity remain where needed for compatibility.

## Verification

- Four new provider-backed integration tests exercise the production embedder against real temporary original files. They read saved MP3/FLAC/M4A bytes directly without a lyric repository/cache, preserve the audio payload, reject a different document, enforce the active-source guard, and restore the original after corrupt provider output.
- Three additional writer regressions cover replacement of standard lyrics and migration of old MP3/FLAC/M4A app-owned fields while retaining alternate/unrelated data. The existing M4A preservation test now requires the standard lyric to update.
- The final full local run completed 619 tests: 616 passed, with only the three pre-existing artist-layout failures. `:app:lintDebug` passed. [PR CI run 40499592898](https://github.com/yohandeku32/nusamusic/actions/runs/40499592898) verified application source `68d303992212042f663cafe3578d5857f1107551`: compilation passed and 619 tests ran, with 616 passing and only the same three pre-existing `ImmersiveArtistHeroTest` failures. All changed default/import, folder-refresh and original-file lyric tests passed. The workflow is red because of those existing failures; no APK was assembled.

These use synthetic files and a test ContentProvider, not user music or a physical DocumentsProvider. Storage grants, device interruption, large-file timing, concurrent third-party edits and real-player readback remain on-device acceptance checks. The pending queue remains explicitly distinct from a completed original-file write.

See [the current save guide](../../EMBEDDED_LYRICS.md) and [AudioLyricsEmbedder](../../../app/src/main/kotlin/com/yohandeku32/nusamusic/data/AudioLyricsEmbedder.kt).
