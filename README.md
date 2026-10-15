# Nusa

Nusa is an Android music player for files stored on your device. It's built with Kotlin and Jetpack Compose, with a vinyl record at the centre of the player screen.

The current version is **3.13.4** (`versionCode 46`). It runs on Android 10 and newer and is still in active development.

## Features

- A vinyl-style player with rotating artwork and reflections, a moving tonearm, and playback controls.
- A dark CD player with rotating album artwork inside a clear jewel case, a tick-mark seek bar, and a swipeable row of discs. The case, reflections, and centre clip stay still while the disc rotates. Choose **CD** under **Player presentation** in settings, then swipe the bottom row and tap a disc to play that song. It uses the library's current sort order and the existing playback queue; the selected mode is saved between launches.
- Immersive artwork with a full-width cover and a gradual fade into the controls. Animated covers are optional, with local artwork as the fallback.
- Background playback, notification controls, shuffle, and repeat through AndroidX Media3.
- Crossfade from 1 to 12 seconds, with a default of 5 seconds when enabled.
- A local library with album artwork, A–Z navigation, and sorting by title, artist, album, date added, or duration.
- Music scanning through MediaStore and folders you select, including subfolders. Tracks shorter than 10 seconds are skipped.
- Saved playback state, including the current track, position, shuffle, and repeat settings.
- Artist portraits from Deezer and biographies from Last.fm.
- System fonts and custom TTF or OTF files for the player title.

The folder scanner recognises AAC, ALAC, FLAC, M4A, MP3, OGG, OPUS, WAV, and WMA files. Playback support depends on the codecs available on the device.

**Optional FFmpeg decoder:** Both the main player and crossfade player can use Media3's native FFmpeg audio renderer as a fallback when bundled. The standard APK **does not** include the native FFmpeg extension; build a matching 1.9.3 AAR or use the manual **Build FFmpeg-enabled Nusa APK** GitHub Actions workflow. FFmpeg decoding does not automatically add unsupported container extractors such as APE or WMA. See [FFmpeg Android integration](docs/FFMPEG_ANDROID.md).

Lossless and Hi-Res labels use audio metadata. The Hi-Res label appears when the reported bit depth is at least 24 bits; it doesn't confirm bit-perfect output to your headphones or DAC.

## Build

You'll need:

- JDK 17 or newer.
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

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`. To run Android lint:

```sh
./gradlew :app:lintDebug
```

Run the unit tests with `./gradlew :app:testDebugUnitTest`. There are no instrumentation tests yet. Playback, folder permissions, and crossfade still need testing on Android devices.

Choose **Immersive art** in the player settings to switch from the vinyl view. Nusa reads the playing file's tags and searches Apple's catalog by title and artist. Animated streams come from the m8tec lookup service; the local cover stays visible when a stream is unavailable.

## Last.fm biographies

Biographies need a Last.fm API key. Local playback works without one.

Add your key to `local.properties`:

```properties
LASTFM_API_KEY=YOUR_LASTFM_API_KEY
```

You can also use the `LASTFM_API_KEY` Gradle property or environment variable. The build uses the first non-empty value from `local.properties`, the Gradle property, then the environment variable. Keep keys and `local.properties` out of Git. The key is included in the APK at build time.

The lookup uses the first artist in a multi-artist tag. It requests an Indonesian biography first and falls back to English if needed.

## Development notes

The app still needs more testing with large libraries, long playback sessions, and folder permissions after a restart. The USB DAC experiment is on hold, and bit-perfect hardware playback hasn't been verified.

Most of the UI lives in `MainActivity.kt`. Music scanning and metadata loading are in `data/`; background playback and crossfade are in `playback/PlaybackService.kt`.

The application ID is `com.yohandeku32.nusamusic`. See [third-party notices](THIRD_PARTY_NOTICES.md) for bundled assets and dependency notices.
