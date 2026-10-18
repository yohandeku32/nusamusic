# Embedded Lyrics (Immersive only)

Tap Lyrics in the Immersive Now Playing controls. CD and Vinyl do not expose this feature.
The modal has one open anchor at half the window height, full width, 28dp top corners,
and a dark artwork-derived gradient. Dismiss with Back, the close button, an outside tap,
or a downward swipe. The existing player remains visible behind it.

## Sources and timing

LyricsRepository opens only the current local audio URI on Dispatchers.IO. It never
queries a lyric service, opens a sidecar, or reads previous imported/remote lyric caches.

| Container | Embedded metadata |
| --- | --- |
| MP3 and ID3-prefixed audio | ID3v2.2/2.3/2.4 USLT/ULT, millisecond SYLT/SLT, lyric TXXX/TXX |
| FLAC | Vorbis comment LYRICS, UNSYNCEDLYRICS, SYNCEDLYRICS and compatible lyric keys |
| OGG Vorbis / Opus | Vorbis comment / OpusTags packets, including continued packets |
| M4A / MP4 | ilst ©lyr, freeform lyric fields, ID32; moov may follow mdat |
| WAV / AIFF | Embedded ID3 chunks |

Fields may contain plain text, LRC, Enhanced LRC or timed TTML. The selector prefers
unit-timed data, then line-timed data, then plain text. Authored adjacent units such as
ber / sa / ma remain separate. No text is split to invent word or syllable timestamps.
Line-only TTML and LRC use line focus. Plain text scrolls manually with no timed animation.

For start-only formats, a unit ends at the next authored start; the final unit uses the
next line boundary or track duration. This is a boundary convention, not a vocal-duration
estimate. Explicit Enhanced LRC terminal stamps and TTML end times take precedence.
Unsupported MPEG-frame-count SYLT, compressed/encrypted ID3 frames, non-seekable content
providers and metadata beyond the bounded limits are ignored. The fallback is exactly:
No embedded lyrics available.

## Playback and performance

The visible sheet samples MediaController.currentPosition on the Compose display clock.
The controller accounts for playback speed; the UI adds no delayed tween or invented
wall-clock timing. Pausing holds position, seek discontinuities update immediately, and
changing songs replaces the keyed lyric view and cancels the previous metadata read.
The frame loop runs only while playing and the lifecycle is STARTED.

Active/fading text layers and active gap indicators observe frame positions. A derived
focus state changes list composition only at lyric boundaries. Text measurements are
retained; the list is lazy. Manual drag/fling suspends following until 2.5 seconds after
scrolling stops. Tapping a timed row seeks to its actual start.

Long-note glow breathes and sweeps slowly within the embedded unit interval.
Onset movement has one damped pulse; it settles independently of the held-note glow.
Row fades, mini-voice visibility, countdown phases and following all use audio time.
The high-damping follow spring integrates audio deltas, so pause holds its position.
Explicit player discontinuities reset following and suppress historical onset/fade replay.

Countdowns are temporary overlays rather than list spacer rows. During the final 440 ms
of an authored break, the already measured next row begins moving into place while the
dots fade. Mini backing vocals use authored accompaniment flags and unioned unit windows;
translations with unit timing follow those same windows, including explicit internal
silence. Line-only translations stay hidden because their separate vocal activity cannot
be established. Measured mini text slots remain stable while their layers fade/slide.
Start-only unit intervals remain visual boundary conventions, never vocal detection.

Parsed lyrics are retained for up to 8 songs. A separate embedded-only disk cache retains
up to 120 records, keyed by audio identity, duration, size and modification time where
provided. Empty results expire after 6 hours. Settings > Lyrics > Reload embedded lyrics
bypasses caches, useful after retagging a file through a provider without modification time.
Reader limits: 16 MiB per metadata block, 32 MiB aggregate reads, 512 KiB per lyric field,
32 candidate fields, bounded container nesting/packet/frame counts. Audio payloads and
large mdat atoms are skipped with seeking rather than copied.

## Verification

Run with Java 21+ and the project's configured Android SDK:

    ./gradlew :app:testDebugUnitTest :app:assembleDebug

Binary fixtures cover plain USLT, line/word/syllable Enhanced LRC, SYLT, UTF-16,
ID3v2.4 TXXX, FLAC, OGG/Opus packet continuation, M4A freeform/©lyr metadata,
moov-after-large-mdat seeking, missing/corrupt tags and non-monotonic timestamps.
Clock/timeline tests cover pause-position stability, speed-adjusted audio samples,
backward seeks, source replacement and boundary conditions.

Device acceptance checks (require an Android device): play tagged fixtures in Immersive;
verify the half-height panel, close/swipe/outside dismissal, line taps, manual fling,
pause/resume, seek, track changes and playback speed. Profile 60/90/120Hz hardware with
animated artwork enabled. JVM tests and an APK build do not measure device frame pacing.

Format references: https://id3.org/id3v2.3.0 and
https://www.xiph.org/vorbis/doc/v-comment.html
