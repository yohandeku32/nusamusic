# Aruna

Aruna is an Android music player for music stored on your device, built with Kotlin, Jetpack Compose and AndroidX Media3. It supports Vinyl, CD and Immersive player presentations.

Requires **Android 10 (API 29) or newer**. Developed by [@yohandeku32](https://github.com/yohandeku32).

## Development status

The project is now named **Aruna**. The default `main` branch still contains the earlier Nusa app label. The app rename, Discovery, online lyrics, lyric saving and the latest character-wave improvements are being developed in [PR #178](https://github.com/yohandeku32/nusamusic/pull/178).

For those changes, see the [development README](https://github.com/yohandeku32/nusamusic/blob/feat/immersive-library-on-soft-ambient-20261019/README.md). The features and setup below describe the current `main` branch.

## Player and library

| Presentation | Features |
| --- | --- |
| Vinyl | Rotating record artwork, reflections and a moving tonearm. |
| CD | Rotating artwork inside a jewel case, a seek bar and a row of discs for choosing tracks. |
| Immersive | Large album artwork, a blurred backdrop, artist information and compact playback controls. |

Choose a presentation in Settings. Playback includes background audio, notification controls, shuffle, repeat, saved playback position and optional crossfade from 1 to 12 seconds.

The local library supports album and artist browsing, A-Z navigation and sorting by title, artist, album, date added or duration. Scan music through MediaStore or select folders, including subfolders. Tracks shorter than 10 seconds are skipped.

The folder scanner recognizes AAC, ALAC, FLAC, M4A, MP3, OGG, OPUS, WAV and WMA extensions. Actual playback depends on the device's decoders and Media3's container support. Lossless and Hi-Res labels use inspected metadata. A reported depth of at least 24 bits qualifies for the Hi-Res flag; the badge does not certify bit-perfect output to a DAC.

## Artist information and artwork

Artist pages show local albums and listening-history rankings, with optional Deezer portraits and Last.fm biographies. Local browsing and playback work without these services. Listening history stays on the device.

Immersive animated artwork uses song tags to search Apple's catalog and the m8tec artwork resolver. Local cover art remains visible when no usable animation is available. These optional features require network access.

### Last.fm setup

Biographies require a Last.fm API key in your own build. Add it to an ignored `local.properties` file:

~~~properties
LASTFM_API_KEY=YOUR_LASTFM_API_KEY
~~~

The build also accepts the `LASTFM_API_KEY` Gradle property or environment variable, in that priority order after `local.properties`. Keep keys out of Git. The key is included in the resulting APK. Biography lookup tries Indonesian first, then English.

## Embedded lyrics

In Immersive mode, open Lyrics to view lyrics from the current audio file. The `main` implementation supports plain text, LRC, Enhanced LRC and TTML from supported embedded metadata. Authored line, word or syllable timings drive highlighting; plain text remains available for manual scrolling.

Tap a timed row to seek. Manual browsing temporarily suspends automatic following. Motion follows playback time, pauses with the song and resamples after a seek. Translations, backing vocals and credits depend on the metadata supplied by the file.

See the [embedded lyric guide](docs/EMBEDDED_LYRICS.md) for supported metadata and verification. The expanded online-lyrics and saving features are documented in [PR #178's lyric guide](https://github.com/yohandeku32/nusamusic/blob/feat/immersive-library-on-soft-ambient-20261019/docs/EMBEDDED_LYRICS.md).

## Build and checks

Use **JDK 21**, Android SDK Platform 36 and a compatible Android Studio version. Android sources compile for Java 17. The project targets API 36 and uses the included **Gradle 9.6.0 wrapper**.

~~~sh
git clone https://github.com/yohandeku32/nusamusic.git
cd nusamusic
~~~

Open the repository root in Android Studio. Set `ANDROID_HOME` to your SDK directory, or add its path to an ignored `local.properties` file:

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

The [Android workflow](.github/workflows/android-build.yml) is currently named **Nusa Android build**. Matching source changes compile and run tests. Pull-request runs do not package an APK on `main`'s workflow; manual runs and matching pushes to `main` can assemble and upload one. A documentation-only push does not match the workflow's path filters.

### Optional FFmpeg decoder

The standard build uses Android/Media3 decoders. FFmpeg becomes available when a matching native Media3 1.9.3 AAR is supplied at `app/libs/media3-decoder-ffmpeg.aar`. Both the main and crossfade players can use it as a fallback. It does not add missing container extractors.

See [FFmpeg Android integration](docs/FFMPEG_ANDROID.md) for the native build script, manual workflow and distribution notices.

### Optional fonts

Custom TTF/OTF player-title fonts can be selected in the app. For the optional CD sticker font in a local build, place a legally obtained `Aerosoldier_PERSONAL_USE_ONLY.otf` at:

~~~text
app/src/main/assets/fonts/Aerosoldier_PERSONAL_USE_ONLY.otf
~~~

This file is gitignored and is not included in the public repository. The CD sticker uses its fallback font when the asset is absent. Follow the font's license before distributing a build containing it.

## Development and notices

Version metadata is in [app/build.gradle.kts](app/build.gradle.kts). The Android application ID remains `com.yohandeku32.nusamusic`, and the repository, workflow and some internal identifiers retain their legacy names.

Most screen integration is in [MainActivity.kt](app/src/main/kotlin/com/yohandeku32/nusamusic/MainActivity.kt). Player components live in [ui/](app/src/main/kotlin/com/yohandeku32/nusamusic/ui), metadata and scanning in [data/](app/src/main/kotlin/com/yohandeku32/nusamusic/data), and background playback in [playback/](app/src/main/kotlin/com/yohandeku32/nusamusic/playback).

Playback reliability, large libraries, folder permissions after restart, lyric motion and frame pacing still need physical-device testing. See [third-party notices](THIRD_PARTY_NOTICES.md) for bundled assets and dependency notices.
