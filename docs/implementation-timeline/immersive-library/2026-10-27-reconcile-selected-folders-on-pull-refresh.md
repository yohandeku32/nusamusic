# Reconcile selected folders on pull refresh

> Date: 2026-10-27
> Status: verified
> Change type: fix
> Scope: Immersive library pull-to-refresh and explicit scan ownership

## Cause and outcome

The Immersive overlay's pull gesture called a separate quick-refresh callback with scanSelectedFolders=false. That path refreshed Android's MediaStore index but retained cached SAF folder rows. New audio inside a manually selected folder could therefore remain invisible until Settings performed its full folder scan.

The overlay now receives the same onScanMusic callback as Settings and the other explicit library refresh surfaces. The duplicate quick-refresh method and parameter were removed. One scanMusic owner requests selected-folder reconciliation and preserves the existing pending-manual-refresh behavior when another scan is already running.

The repository still publishes Android-indexed changes first, performs folder enumeration off the UI thread, and reuses cached metadata for documents whose modification time has not changed. Automatic MediaStore notifications retain their lighter indexed-update path; an explicit pull is a request to check all selected sources.

## Verification

- A native Compose regression performs the real pull gesture on the Immersive overlay and observes one scan request.
- Two provider-backed repository regressions cover a new file in a nested selected folder with MediaStore access disabled, reuse of unchanged tags, duplicate-free repeat scanning and reconciliation of removed documents. The fast indexed-only path demonstrates the original miss; full reconciliation finds the new song.
- All 12 focused folder/library checks passed with Android compilation. The final full local run completed 619 tests: 616 passed, with only three pre-existing artist-layout failures. `:app:lintDebug` passed. PR CI results are recorded after completion.

Tests simulate DocumentsProvider listings and extracted metadata. Real storage grants, provider latency, copying partially written audio and physical gesture behavior remain device acceptance checks. Selected-folder traversal is deliberately part of manual refresh because SAF has no portable change-generation checkpoint.
