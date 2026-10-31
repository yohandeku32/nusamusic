# Aruna

Aruna is an Android music player for the music stored on your device, built with Kotlin, Jetpack Compose and AndroidX Media3. Start with the Immersive player or choose Vinyl or CD in Settings, browse your collection, and follow synchronized lyrics while you listen.

Requires **Android 10 (API 29) or newer**. The app is under active development.

Developed by [@yohandeku32](https://github.com/yohandeku32). The repository and Android application ID still use `nusamusic`; the app's display name is **Aruna**. Version information lives in [app/build.gradle.kts](app/build.gradle.kts).

## First launch

New listeners see a short Aruna welcome with three choices: choose a music folder, find audio on the device, or set up later. Access is requested after the listener chooses an import action. Cancelling a folder selection or declining audio access leaves import choices available in the empty song list. The welcome is shown once; existing settings and library history preserve returning-listener continuity.

## Player and library

| Player | Presentation |
| --- | --- |
| Immersive (default) | Large album artwork, a blurred backdrop, light/dark appearance and optional animated covers. |
| CD | Rotating disc inside a jewel case, with a row of discs for choosing tracks. |

Choose a presentation in Settings. A missing or unavailable saved mode uses Immersive; an explicitly saved CD choice is preserved. Playback includes background audio, notification controls, shuffle, repeat, saved playback position and optional crossfade from 1 to 12 seconds.

In Immersive Settings, **Now Playing scale** enlarges the main player's artwork, text and controls from 100% to 125% in 5% steps. The choice is saved; **Reset** returns to 100%. The layout reflows on narrow screens and retains the system font-size preference.

Aruna reads your device library through MediaStore and folders you select, including subfolders. It skips tracks shorter than 10 seconds. The library supports search, album and artist browsing, and sorting by title, artist, album, date added or duration.

Pull to refresh checks both the Android media index and manually selected folders, just like the explicit scan in Settings. Unchanged folder tags are reused; newly copied songs do not need to be indexed by Android before a selected-folder refresh can discover them.

The folder scanner recognizes AAC, ALAC, FLAC, M4A, MP3, OGG, OPUS, WAV and WMA extensions. Actual playback depends on the device's decoders and Media3's container support. Lossless and Hi-Res badges come from inspected audio metadata. A reported depth of at least 24 bits qualifies for the Hi-Res flag; the badge does not certify bit-perfect output to a DAC.

## Discovery and artists

In Immersive mode, **Discover** opens a feed of albums, artists and recently played songs from your local collection. Artist rankings use listening history recorded on the device. Before there is listening history, the feed shows artists from your library.

Open an artist or album from Discovery to browse that artist's local tracks and albums. Artist pages can show a Deezer portrait and a Last.fm biography. Playback and local browsing work without either service. About Aruna in Settings shows the app version and developer credit.

## Lyrics

Open **Lyrics** from the Immersive player's controls. Lyrics replace the artwork foreground within the player while playback controls remain available. Tap Lyrics again to return to the artwork. In Settings, adjust lyric size, enable or disable online lyrics, or reload the current song's lyrics.

In Vinyl, scroll below the player or tap its footer arrow to reveal a half-screen lyrics section. Active lyrics follow near the top of its rounded panel. The record adapts to screen height so the normal playback area fits through its quality badge. A fixed metadata area keeps the record, artist portrait, lower controls, and quality slot stable as titles change. One-line titles remain close to the artist name. The small artist portrait resolves Apple Music artwork through the existing unavatar provider, with local cover art as a fallback.

- Embedded lyrics load first. The reader accepts plain text, LRC, Enhanced LRC and TTML from supported audio metadata.
- Fully timed embedded karaoke takes priority. When online lyrics are enabled, lrc.red can provide a more precisely synchronized version; LRCLIB provides a line-synced fallback.
- Authored TTML timings drive word and syllable highlighting, duet alignment and backing vocals. Translations and composer/lyricist credits appear when supplied.
- TTML translations and pronunciation (such as pinyin) appear beneath their original lyric, including line-synced lyrics and duet voices. Pronunciation words brighten using supplied timestamps. These texts come from the lyric file; Aruna does not generate translations.
- Timed Chinese/Japanese readings align directly under the corresponding character or kanji group. The shared layout centers each reading and wraps it together with its original text when matching time boundaries are available.
- Short vocal pulls use a wave across individual characters. Nearby short words share one continuously moving crest across word boundaries; long held notes retain their separate character-by-character 3D rise and release. Rapid words and uncertain timing avoid the extra character animation.
- Animations use playback time, hold their pose on pause and resample after a seek. Tap a timed lyric row to seek; manual scrolling temporarily suspends automatic following.

When online lyrics improve the file's existing lyrics, **Save** queues a metadata update to the original MP3, FLAC or M4A file. Grant access to the music folder when prompted. A clock means the original is still waiting to be written; switch songs so the player can release the file. Success is reported only after the original matches the verified output. Saving updates the standard readable lyric and authored timing without converting the audio.

See [Lyrics: sources, timing and saving](docs/EMBEDDED_LYRICS.md) for supported metadata, folder access and verification details.

## Animated artwork

Enable animated artwork in the Immersive player settings. Choose **Apple first**, **Spotify first**, **Apple only** or **Spotify only**. Apple is the default priority and accepts portrait Tall artwork only. Spotify Canvas is an optional alternative; static local artwork remains the fallback.

Artwork lookup uses song metadata and external resolvers. The current Spotify resolver uses the independent Spotycovs service without a Spotify login. Provider availability and matching can vary. Aruna caches video data and can prefetch artwork for up to five upcoming tracks in the actual playback queue, including shuffle order. Prefetching uses network data.

Canvas matching supports non-Latin titles and artist names. Equivalent trusted proxy links share cached video bytes, and prefetch yields when the active player needs bandwidth. See the [Canvas cache and matching verification](docs/implementation-timeline/immersive-player/2026-10-30-improve-canvas-matching-and-cache-reuse.md) for measured results and limits.

## Network use and storage

Local playback does not require an account. Optional features make network requests:

| Feature | Information used |
| --- | --- |
| Online lyrics | Song title, artist, album and duration for matching. |
| Animated artwork | Song/album metadata for Apple Tall artwork or Spotify Canvas lookup and video downloads. |
| Artist information | Artist names for portraits and biographies. |

These lookup features do not upload the audio file. Disable online lyrics and animated artwork in Settings to stop those lookups. Listening history stays on the device. Cached provider content may be unavailable after Android clears app caches.

## Build and checks

Use **JDK 21** for development and tests. The project compiles Android sources for Java 17, targets API 36 and requires Android SDK Platform 36. Use a compatible Android Studio version and the included **Gradle 9.6.0 wrapper**; no separate Gradle installation is needed. Plugin versions are defined in [build.gradle.kts](build.gradle.kts).

Clone the repository and open its root in Android Studio:

~~~sh
git clone https://github.com/yohandeku32/nusamusic.git
cd nusamusic
~~~

Set `ANDROID_HOME` to your SDK directory, or create an ignored `local.properties` file:

~~~properties
sdk.dir=/path/to/android-sdk
~~~

Run checks without packaging an APK:

~~~sh
./gradlew :app:testDebugUnitTest
./gradlew :app:lintDebug
~~~

On Windows PowerShell, use `.\gradlew.bat` in place of `./gradlew`.

To build a debug APK when needed:

~~~sh
./gradlew :app:assembleDebug
~~~

The output is `app/build/outputs/apk/debug/app-debug.apk`.

The [Android workflow](.github/workflows/android-build.yml) is currently named **Nusa Android build**. It compiles and runs unit tests for matching source changes. PR #178 runs checks without packaging an APK. Manual runs, pushes to `main`, and the existing PR #176 exception can assemble and upload a debug APK. Its path filters cover Android sources, Gradle configuration and the workflow itself; a pull request containing only documentation does not match them.

Tests include JUnit and Robolectric/Compose checks. Physical-device testing is still needed for playback, folder access, animations, touch behavior and frame pacing; passing unit tests does not establish those results.

### Last.fm biographies

Local playback works without a Last.fm key. To enable biographies in your own build, add this to `local.properties`:

~~~properties
LASTFM_API_KEY=YOUR_LASTFM_API_KEY
~~~

The build also accepts a `LASTFM_API_KEY` Gradle property or environment variable, in that priority order after `local.properties`. Keep keys out of Git. The key is included in the resulting APK. Biography lookup tries Indonesian first, then English.

### Optional FFmpeg decoder

The standard build uses Android/Media3 decoders. FFmpeg is available only when a matching native Media3 1.9.3 AAR is supplied at `app/libs/media3-decoder-ffmpeg.aar`. Both the main and crossfade players can use it as a fallback. It does not add missing container extractors.

See [FFmpeg Android integration](docs/FFMPEG_ANDROID.md) for the native build script, manual workflow and distribution notices.

### Optional CD sticker font

For a local build, place a legally obtained `Aerosoldier_PERSONAL_USE_ONLY.otf` at:

~~~text
app/src/main/assets/fonts/Aerosoldier_PERSONAL_USE_ONLY.otf
~~~

The file is gitignored and is not included in the public repository. The CD sticker uses a fallback font when it is absent. Follow the font's license before distributing a build that includes it. Custom TTF/OTF player-title fonts can also be selected in the app.

## Source layout

| Path | Contents |
| --- | --- |
| [app/src/main/kotlin/com/yohandeku32/nusamusic/MainActivity.kt](app/src/main/kotlin/com/yohandeku32/nusamusic/MainActivity.kt) | App entry point, screen integration and navigation. |
| [app/src/main/kotlin/com/yohandeku32/nusamusic/ui](app/src/main/kotlin/com/yohandeku32/nusamusic/ui) | Player presentations, Discovery, Settings and lyric rendering. |
| [app/src/main/kotlin/com/yohandeku32/nusamusic/data](app/src/main/kotlin/com/yohandeku32/nusamusic/data) | Library scanning, metadata, artwork, lyric providers and file writers. |
| [app/src/main/kotlin/com/yohandeku32/nusamusic/playback](app/src/main/kotlin/com/yohandeku32/nusamusic/playback) | Media3 playback service, queue and crossfade. |
| [app/src/test](app/src/test) | Unit and Robolectric tests. |
| [docs](docs) | Lyrics and native decoder guides. |

The application ID remains `com.yohandeku32.nusamusic`. See [third-party notices](THIRD_PARTY_NOTICES.md) for bundled assets and dependency notices. Large libraries, long playback sessions, storage permissions after restart and external DAC behavior still need device testing.
