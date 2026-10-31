# Resolve first-artist portraits directly and warm Vinyl playback

> Date: 2026-10-31
> Status: verified locally; physical-device verification pending
> Change type: fix
> Scope: shared artist portrait resolver/cache, Vinyl lookahead, and avatar retry on track change

## Contract and diagnosis

The first listed artist owns the portrait, including collaboration tags. The existing `ArtistNameUtils.firstArtist` and its cache key remain authoritative for separators. Do not search guest singers or choose the first vaguely matching catalog result.

The old source requested a full image through unavatar. Every failed request, including timeout/rate limit/cancellation, could become a 12-hour negative disk entry. The foreground avatar requested portraits on demand, while queue lookahead and startup warmup were explicitly restricted to Immersive. Consequently Vinyl could repeatedly encounter cold artists even though a shared prefetch system existed.

## Repair

- Query Apple's public iTunes catalog for `musicArtist`, match the complete normalized first artist name, and retain the canonical artist URL and ID. Reject tracks, tribute/similar names, and different artists. Matching retains Unicode letters and digits.
- Read the matching `MusicGroup`/`Person` JSON-LD on that public Apple Music artist page, rather than selecting a generic social image or album cover. Download a 600 px JPEG directly from Apple's CDN. This is a public catalog/page integration, not authenticated MusicKit.
- A bounded 12 MiB memory cache, 48-photo disk cache, and per-artist single flight share work across the player, Discovery, artist page, and warmup. Photo and resolved URL entries have a 30-day TTL; metadata/failure files are bounded separately.
- Cache valid misses for six hours and transient errors for only 30 seconds. Preserve cancellation; keep a previously valid expired photo if refresh fails. A new v2 cache namespace ignores old proxy failure entries. A persisted CDN URL skips repeated catalog/page work if only the image request needs retrying.
- Enable the existing current-plus-next-six artist warmup in Vinyl as well as Immersive. Retain two prefetch slots and the 36-new-artists/day automatic budget. A null avatar can retry when the track changes without clearing a valid same-artist portrait.

## Evidence and performance limits

Live desktop probes on 2026-10-31 used Linkin Park, Daft Punk, and AC/DC, twice each, alternating proxy and direct chains on the same connection. All six direct chains found matching Apple artist metadata and returned valid JPEG images. Image bytes changed from 465,925 to 112,798, 194,186 to 61,863, and 265,119 to 72,770 respectively (approximately 68–76% less image data). Catalog/page bytes are additional on the initial lookup.

The direct uncached-client chain was slower than the edge-cached proxy in this sample: 738–2,949 ms versus 381–1,956 ms. These results do not establish faster first loads. The intended latency improvement is eliminating repeated lookups through cache hits and preparing upcoming Vinyl artists ahead of playback; reducing image bytes and memory also retains more portraits. Server/CDN caches were not cleared, and this is not a phone percentile benchmark.

## Verification

All 36 selected tests pass: 9 resolver contracts, 10 cache integration tests, 3 queue-selection tests, 11 native Vinyl player tests, and 3 native lyrics integration tests. Regression cases cover exact first-singer matching, Unicode and URL validation, memory/disk reuse, concurrent consumers, transient-error recovery, image-only retry, valid misses, stale offline photos, cancellation, legacy cache isolation, and bounded failure files. Repeated and process-restored cache hits make zero extra network requests; concurrent aliases share the same bitmap and three cold requests.

Debug assembly, Android Lint, APK v2 signature verification, whitespace checks, and documentation links pass. A live public artist-page check using the production Aruna User-Agent also returned matching JSON-LD successfully. Physical-device behavior remains unverified.

## Sources and maintenance

- [Apple Search API contract](https://performance-partners.apple.com/search-api)
- [Observed public Linkin Park artist metadata](https://music.apple.com/us/artist/linkin-park/148662)
- [Resolver](../../../app/src/main/kotlin/com/yohandeku32/nusamusic/data/AppleArtistPortraitResolver.kt)
- [Loader and bounded caches](../../../app/src/main/kotlin/com/yohandeku32/nusamusic/data/AppleArtistPortraitLoader.kt)
- [Cache integration tests](../../../app/src/test/kotlin/com/yohandeku32/nusamusic/data/AppleArtistPortraitStoreTest.kt)

The public artist-page JSON-LD shape is an external dependency and may change. Live endpoint availability does not prove physical-device rendering, offline-to-online recovery timing on every screen, or performance on the owner's mobile connection. The older implementation record's unavatar description is historical and superseded here.
