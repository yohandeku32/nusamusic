# Nusa

Nusa is an Android music player for files stored on your device. It's built with Kotlin and Jetpack Compose, with a vinyl record at the centre of the player screen.

The current version is **3.13.4** (`versionCode 46`). It runs on Android 10 and newer and is still in active development.

## Features

- A vinyl-style player with rotating artwork and reflections, a moving tonearm, and playback controls.
- A dark CD player with rotating album artwork inside a clear jewel case, a tick-mark seek bar, and a swipeable row of discs. The case, reflections, and centre clip stay still while the disc rotates. Choose **CD** under **Player presentation** in settings, then swipe the bottom row and tap a disc to play that song. It uses the library's current sort order and the existing playback queue; the selected mode is saved between launches.
- Immersive artwork with a dominant edge-to-edge cover, a softly blurred colour backdrop, left-aligned song details, and compact playback controls. Artist has its own round profile icon beside the Lyrics, Library, and Settings pill (and is also reachable with a right swipe). The layout adapts to short screens, landscape, and larger text. Animated covers are optional, with local artwork as the fallback.
- Background playback, notification controls, shuffle, and repeat through AndroidX Media3.
- Crossfade from 1 to 12 seconds, with a default of 5 seconds when enabled.
- A local library with album artwork, A–Z navigation, and sorting by title, artist, album, date added, or duration.
- Startup distinguishes a library still loading from a confirmed empty collection. Cached songs appear as soon as they are ready; an initial scan shows a loading message until it completes, and a failed scan shows a retry message.
- The Immersive art player keeps its footer free of library-empty/loading messages; library status remains on the library page. Its last static cover, blurred backdrop, and colours are saved as one small launch preview and warmed in the background on reopening, without waiting for the scanned library or reopening the audio file. The first usable cover appears immediately, with crossfades retained for subsequent cover changes. The full-resolution cover and optional animation continue loading in the background.
- Music scanning through MediaStore and folders you select, including subfolders. Tracks shorter than 10 seconds are skipped.
- Saved playback state, including the current track, position, shuffle, and repeat settings.
- Artist portraits from Deezer and biographies from Last.fm.
- In Immersive art, swipe right from the player to open the artist page (swipe left to return). It has a monochrome Deezer portrait fading into black, Last.fm biography, your five most-played local tracks, and a two-column grid of local albums. Tap an album to browse and play its tracks; the back button returns to the artist overview, then to the player. The library remains one leftward swipe from the player.
- Main-page swipes use MIUIX-inspired motion: a full-width foreground slide, quarter-speed parallax on the covered page, a dimmed background, and rounded leading corners during the transition. Swipes and navigation buttons share critically damped spring settling. The effect follows drag progress directly, supports right-to-left layouts, and leaves the settled page square and fully opaque.
- System fonts and custom TTF or OTF files for the player title.

The folder scanner recognises AAC, ALAC, FLAC, M4A, MP3, OGG, OPUS, WAV, and WMA files. Playback support depends on the codecs available on the device.

**Optional FFmpeg decoder:** Both the main player and crossfade player can use Media3's native FFmpeg audio renderer as a fallback when bundled. The standard APK **does not** include the native FFmpeg extension; build a matching 1.9.3 AAR or use the manual **Build FFmpeg-enabled Nusa APK** GitHub Actions workflow. FFmpeg decoding does not automatically add unsupported container extractors such as APE or WMA. See [FFmpeg Android integration](docs/FFMPEG_ANDROID.md).

Lossless and Hi-Res labels use audio metadata. The Hi-Res label appears when the reported bit depth is at least 24 bits; it doesn't confirm bit-perfect output to your headphones or DAC.

## Optional CD sticker font (local only)

The CD jewel-case sticker prints the **current artist name in UPPERCASE**, shortening long multi-word artist names to initials. To use the *Aerosoldier PERSONAL USE* font in a locally built APK, copy your legally obtained `Aerosoldier_PERSONAL_USE_ONLY.otf` into:

`app/src/main/assets/fonts/Aerosoldier_PERSONAL_USE_ONLY.otf`

Create the `assets/fonts` folders if they do not exist, then rebuild the APK with Android Studio. The font file is intentionally **gitignored and not bundled with the public repository** due to its personal-use licensing. Without that local asset, the sticker uses the existing handwritten fallback font. Check your font licence before distributing an APK that contains it.

## Build

You'll need:

- JDK 21 or newer for the full test suite (Accompanist Core's JVM dependency uses Java 21 bytecode). Android source/target compatibility remains Java 17.
- Android SDK Platform 36 and Build Tools 36.0.0.
- Gradle 9.6.0 or newer, as required by Android Gradle Plugin 9.4.0.

The project targets Android 16 (API 36), with a minimum of API 29. It uses AGP's built-in Kotlin support and the Compose compiler plugin 2.2.10.

Open the project in a compatible Android Studio version, or build from the repository root with the included Gradle wrapper.

Set `ANDROID_HOME` to your SDK directory, or add its path to an ignored `local.properties` file:

```properties
sdk.dir=/path/to/android-sdk
```

Build the debug APK:

```sh
./gradlew :app:assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

Push and pull-request checks compile Android sources and run unit tests without
packaging an APK. Run **Nusa Android build** manually in GitHub Actions when an
APK download is needed; only manual runs assemble and upload it.

To run Android lint:

```sh
./gradlew :app:lintDebug
```

Run the unit tests with `./gradlew :app:testDebugUnitTest`. There are no instrumentation tests yet. Playback, folder permissions, and crossfade still need testing on Android devices.

Choose **Immersive artwork** in Player Settings to use animated album covers. Nusa first searches Apple Music via the m8tec artwork resolver. Only if Apple's animation is missing or all its video variants fail to play does Nusa search **Spotify Canvas** by local song artist and title via the independent [Spotycovs API](https://www.spotycovs.lol/). The Spotify resolver is unofficial, uses no Spotify login or cookie in the APK, and may change or become unavailable. Canvas playback accepts MP4 links from Spotify's `canvaz.scdn.co` CDN or Spotycovs' validated `/api/canvas?id=…&stream=1` video proxy, with strict HTTPS host/path checks. Apple and Spotify URL lookups run concurrently, with a short 400 ms Apple-first priority window; if Apple is slow and Canvas is ready, Canvas can appear without waiting for all Apple catalog attempts. An Apple match may replace the pending Canvas only before its first frame is displayed. Verified Spotify Canvas URL matches are stored privately for up to three days to avoid repeated resolver calls. Canvas videos use an app-private 64 MiB least-recently-used Media3 disk cache (Android may purge cache files). After a brief queue-stability delay, Nusa can preload at most 512 KiB of the actual next track's Canvas, including shuffle order. Prefetching uses network data and is disabled when animated artwork or immersive mode is off. Cached content is never uploaded. Local static artwork always remains as the final fallback. The artwork animation switch enables or disables both sources together. These lookups send artist and title metadata to external providers; the audio file itself is never uploaded.

## Last.fm biographies

Biographies need a Last.fm API key. Local playback works without one.

Add your key to `local.properties`:

```properties
LASTFM_API_KEY=YOUR_LASTFM_API_KEY
```

You can also use the `LASTFM_API_KEY` Gradle property or environment variable. The build uses the first non-empty value from `local.properties`, the Gradle property, then the environment variable. Keep keys and `local.properties` out of Git. The key is included in the APK at build time.

The lookup uses the first artist in a multi-artist tag. It requests an Indonesian biography first and falls back to English if needed.

The artist page matches the same primary artist in local tags, ignoring case and extra whitespace. Album grouping uses album tags, including tracks scanned from selected folders. Deezer and Last.fm supply the portrait and biography only; all playable songs and albums come from your scanned local library. Without a connection or Last.fm key, local albums and playback remain available, and the page shows a portrait placeholder or a biography unavailable message.

Visited artist profiles keep their portrait, selected album, expanded biography, and scroll position while you swipe between the artist, player, and library pages. Up to eight profiles are kept in memory. Deezer portraits and successful Last.fm biographies are also saved on the device and reused after reopening the app, including offline. A swipe away does not cancel a portrait download, and failed lookups wait five minutes before retrying rather than requesting the same missing data on every visit. Local albums and listening-history rankings still update when your collection or playback history changes.

Most-played tracks use listening history stored on the device from this version onward; earlier plays cannot be reconstructed. A play counts once per track session after 30 seconds of listening, or half the track duration for shorter tracks. Pausing and seeking do not add listening time. Background playback is counted by the playback service, and repeat starts a new session. No listening history is sent to Deezer or Last.fm.

## Immersive lyrics

Lyrics are available only in Immersive mode. The Lyrics button opens a rounded
bottom sheet at 50% of the window height, with an artwork-derived background.
Close, swipe down, or tap outside to dismiss. Settings → Lyrics controls font size.

Lyrics first load from the current local audio file. The bounded reader
supports ID3 USLT/SYLT/TXXX, FLAC and Ogg Vorbis/Opus comments, MP4/M4A lyrics
atoms, and ID3 chunks in WAV/AIFF. Fully-timed embedded karaoke wins. If the
file has only line-synced, plain or missing lyrics, NusaMusic tries the public
lrc.red community API, matching the song by title and artist before fetching
the ISRC-based song JSON. Original TTML supplies authored word-level timing;
structured per-word or line timing is used when TTML is unavailable. Local
text displays immediately while these network requests run.
Online hits are cached separately on-device; misses expire after 6 hours.
No login, API key, WebView, song audio upload or automatic timestamps.
An offline/network failure keeps the original local lyrics unchanged.
A manual Lyrics reload also refreshes the online lookup.

Syllable duration continuously controls the existing glow and motion. Notes
under 250 ms use only progressive highlighting; 250–600 ms notes have minimal
glow and subtle motion. Soft glow and breathing develop on longer notes, with
elastic contours and micro-vibration reserved for sustained vocals. Runs of
short syllables reduce decoration across a phrase, recovering gradually into
a held note. Inferred LRC tails and inherited TTML container ends cannot trigger
full sustained-vocal expression; their original highlight timestamps stay intact.
Attack, sustain and release use playback time, while the traveling wave and
slow breathing keep their global phase across syllable boundaries. Glow and
sharp text share one intact glyph transform. Measured glyphs retain their
gradients, matrices, expression profiles and shadow palette across frames.
Audio-time effects freeze on pause and evaluate the new phase directly on seeks.
When a word has a reliably timed held vocal, only that word receives the
smooth, word-local mountain wave, rising through ~42% of its authored hold and
releasing gently through ~58%. Earlier words remain still; neighboring lines
keep their original highlighting. Short notes and uncertain metadata tails
never trigger an invented held-vowel animation.

The V13 dynamic engine keeps timing, phrase analysis, expression and visual
focus separate. `LyricsTimeline` retains the authored active windows;
`LyricsPhraseAnalysis` prepares continuous duration/gap-weighted context from
nearby earlier and later units. Look-ahead gently prepares the current note
for an upcoming hold without activating future text. Each measured glyph
retains a `LyricsExpressionController`; soft outgoing halos bridge adjacent
notes while strong bloom releases naturally. Existing continuous wave,
bubble, elastic contour and micro-vibration carriers keep their playback phase.
Inferred timing remains limited to simple expression.

`LyricsVisualFocusController` prepares a continuous scroll target just before
the next main-line onset. One retained spring uses calmer response on rapid
rows and never returns to an older overlapping main line. Blur/opacity share
smooth line-cue transitions, with all active voices staying sharp and the
existing backing-vocal depth retained. Authored instrumental countdowns,
manual-scroll priority, first-row alignment and tap-to-seek retain their layout
and interaction paths. Tests include synthetic embedded timing fixtures for
rap, pop, ballad, long holds, mixed cadence, backing vocals and instrumental gaps;
they do not substitute for listening/rendering/FPS tests on a physical device.

One lifecycle-bound publisher reads actual player position and discontinuity
epoch together on the UI thread. Only a real player event resets motion;
a slow frame is never treated as a seek. Tap-to-seek issues one player request,
without an optimistic second lyrics clock. Each song gets its own motion state.

Touch-down immediately stops auto-follow through the scroll mutator's UserInput
priority, before drag slop. One effect owns initial placement, distant rejoin
and spring corrections. Seeks cancel its previous work; visible targets and
manual resumption use smooth corrections instead of repeating an initial snap.
The first vocal row sits 12.5% of the lyric viewport above the usual centre,
including after seeking back; subsequent rows retain the existing follow anchor.
Following yields to the full gesture/fling and then the browsing grace period.
Duplicate timestamps keep the first authored anchor, and duplicate backing
content has distinct source-index keys.

Timed backing voices appear beneath the related main line. Their complete
measured height, including padding, expands and collapses from audio time.
Once the exit finishes, the backing is removed from composition and measurement.
Authored silence between backing units also collapses the space. Following
uses the main text height as its anchor so backing expansion does not shift
the target. Three-dot countdowns exist only during authored gaps of at least
three seconds. A temporary row above the associated lyric creates actual space,
pushing that lyric and following rows down even in mid-song or intra-line gaps.
Its height eases back to zero before singing resumes; completed countdowns
leave no permanent space. The dots scroll with the lyrics during manual browsing.

Composed by and Lyrics by sections appear after the final lyric when the file
contains those roles: ID3 TCOM/TEXT (including v2.2 aliases), Vorbis
COMPOSER/LYRICIST, MP4 ©wrt composer, or compatible freeform fields. Roles stay
separate, absent names remain hidden, and credits survive the embedded cache.
Credits scroll with the list and never become an automatic follow target.
Following stops after the final vocal exit so users can browse the ending.

Visible rows retain their text measurements; frame updates affect drawing,
layers, and the active backing container's measurement. Manual scrolling
suspends following until scrolling stops and 2.5 seconds pass. Up to eight
parsed songs are retained in memory; the embedded disk cache has 120 entries.

Accompanist Core 0.4.2 and UI 1.0.15 remain pinned for Compose 1.9.
Unit tests cover metadata formats/credits, cache persistence, authored vocal
windows, pause/seek behavior, backing height reclamation and simulated
60/90/120 Hz timing. Rendering appearance, actual FPS, touch feel, and provider
compatibility still require physical-device testing.

## Development notes

The app still needs more testing with large libraries, long playback sessions, and folder permissions after a restart. The USB DAC experiment is on hold, and bit-perfect hardware playback hasn't been verified.

Most of the UI lives in `MainActivity.kt`. Music scanning and metadata loading are in `data/`; background playback and crossfade are in `playback/PlaybackService.kt`.

The application ID is `com.yohandeku32.nusamusic`. See [third-party notices](THIRD_PARTY_NOTICES.md) for bundled assets and dependency notices.
