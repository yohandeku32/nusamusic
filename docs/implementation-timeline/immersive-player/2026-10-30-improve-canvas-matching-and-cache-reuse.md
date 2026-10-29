# Improve Canvas matching and cache reuse

Date: 2026-10-30

## Request and evidence

The listener requested checking GitHub icon availability and improving Spotify Canvas consistency/download speed. Icon research is a separate read-only comparison; no icon library or visual design is changed in this repair.

Three controlled regressions failed before the repair: exact Chinese/Japanese/Korean names normalized to empty strings, a valid direct Spotify CDN video lost to a proxy URL, and trusted proxy aliases downloaded the same completed clip again. The current prefetch loop also waited for foreground buffering while retaining an open cache writer.

One read-only public resolver probe returned HTTP 503 in 1,804ms. That is one observation from this environment, not a provider availability statistic or a measurement of the listener's network. It prevents claiming a universal live download latency improvement.

## Changes and contracts

- Metadata matching retains Unicode letters, digits and meaningful non-Latin combining marks. Latin accent folding remains; Japanese voicing and Hindi vowel marks cannot collapse into a different title.
- Within the same selected JSON object, a valid canvaz.scdn.co MP4 is preferred over its proxy representation. Existing candidate/object order remains, so a CDN URL from another candidate cannot replace the selected recording. Proxy-only replies remain supported. The strict HTTPS/host/path validation is retained.
- Both playback and preload use the same Media3 cache-key factory. Trusted www/bare-host and query-order aliases for one proxy track ID use the common legacy canonical URL key. Track IDs, signed direct CDN URLs, custom DataSpec keys and unrelated hosts retain distinct identities. The 64MiB cache and existing preload byte budgets remain.
- Initial foreground protection still waits before opening a speculative reader. If foreground demand appears after opening, preload stops after the current read and closes the writer instead of waiting with its cache span held. Failed open and cancellation also close the reader. A blocking upstream read can still last until the existing network timeout; reads are not closed concurrently from another thread.
- No extra network concurrency, provider, credentials, polling or whole-app framework change is introduced. Provider preference, static fallback, URL metadata TTLs and shared resolution ownership retain their existing paths.

## Measurement protocol

Robolectric API 34 with a real Media3 SimpleCache and a counted in-memory upstream. Each of three repetitions starts with a fresh cache, reads a 512KiB deterministic payload through the canonical proxy URL, then through an equivalent host/query-order alias. Every output byte is verified. The upstream substitutes for HTTP to isolate cache identity; elapsed time is not used as a network-speed claim.

Before: each repetition opened upstream twice and transferred 1,048,576 bytes. After: one open and 524,288 bytes. The second access transfers zero upstream bytes. A partial-prefix case verifies a 128KiB preload resumes at byte 131,072 through an alias and downloads only the missing bytes, then replays without another upstream read.

Additional checks cover foreground yielding, exact byte limits, cancellation, failed open, Unicode identity and candidate selection. Android compilation, all 41 focused Canvas/artwork/cache tests and lintDebug passed, with zero failures/errors/skips and zero lint errors (120 existing warnings and three hints). Nine tests are new. Physical-device first-frame time, real CDN throughput, GPU/frame pacing and provider availability remain unverified. No APK is assembled.

Primary API evidence: [Media3 cache key factory](https://developer.android.com/reference/androidx/media3/datasource/cache/CacheDataSource.Factory#setCacheKeyFactory(androidx.media3.datasource.cache.CacheKeyFactory)) and [cache-lock behavior](https://developer.android.com/reference/androidx/media3/datasource/cache/CacheDataSource#FLAG_BLOCK_ON_CACHE).
