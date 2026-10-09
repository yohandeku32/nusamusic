# Nusa

Nusa is an Android local music player built with Kotlin, Jetpack Compose, and AndroidX Media3. It plays audio stored on the device and provides library management, playback controls, album artwork, artist information, and configurable player presentations.

## Project Status

| Item | Value |
|---|---|
| Development status | Active development / pre-release |
| App version | 3.13.4 |
| Version code | 46 |
| Default branch | `main` |
| Last updated | 9 October 2026 |

The application is functional, but it has not been designated as a production release. Hardware validation for bit-perfect playback is outstanding.

## Features

| Feature | Status |
|---|---|
| Local audio playback | Available |
| Background playback and MediaSession | Available |
| Notification playback controls | Available |
| Shuffle, repeat, previous, and next | Available |
| Playback state restoration | Available |
| Crossfade | Available |
| Android MediaStore scanning | Available |
| User-selected folders and recursive scanning | Available |
| Automatic library refresh | Available |
| Ignore tracks shorter than 10 seconds | Available |
| Album artwork | Available |
| Vinyl and Immersive Artwork player modes | Available |
| Animated album artwork | Optional; experimental |
| Song list, sorting, and A–Z navigation | Available |
| Artist portrait and biography | Available |
| Lossless / Hi-Res metadata badge | Available |
| Built-in and custom title fonts | Available |
| USB DAC / bit-perfect playback | Paused |

## Player

Nusa provides two player presentations, configurable in Settings.

### Vinyl

The Vinyl presentation includes a rotating record, album artwork in the center label, a tonearm linked to playback progress, surface details, and lighting effects.

### Immersive Artwork

The Immersive Artwork presentation places the album cover behind the playback interface. The artwork extends from the top edge to the area above the playback controls. The title, artist, progress bar, and controls retain their own layout. A dark frosted-glass treatment blends the lower artwork with the controls.

In this mode, the Lossless / Hi-Res badge uses a solid white background for contrast. The Animated Artwork setting can be turned on or off independently of the player presentation.

## Animated Artwork

Animated Artwork is an experimental feature for the Immersive Artwork mode. It retrieves a matching animated album cover through the community-maintained [Apple Music Animated Artworks API](https://github.com/m8tec/apple-music-animated-artworks), hosted at `https://artwork.m8tec.top`.

The lookup uses the local track's artist, album, and title. Nusa prefers the tall artwork variant for immersive playback. Local album artwork remains the fallback when a match is unavailable or the animated stream fails.

The feature has the following behavior and limits:

- Animated Artwork can be enabled or disabled in Settings.
- The animated stream is silent; its audio track is disabled.
- Video track selection is limited to 720 × 1280, 30 fps, and 4 Mbps.
- The static cover remains visible while the first video frame loads.
- The animated video is removed when the Immersive Artwork page is not visible.
- Artwork lookup results are cached in memory.

The API is community-operated and is not an official Apple service. Availability and matching results depend on the external service and network connection. Playback smoothness may also vary by device.

## Music Library

Nusa reads local audio using Android MediaStore and supports folders selected through the Storage Access Framework. Folder scanning includes subfolders. Tracks shorter than 10 seconds are excluded.

The library provides:

- List view with album artwork, title, artist, and Hi-Res marker
- A–Z index with tap and drag navigation
- Sorting by title, artist, album, date added, and duration
- Recently Added
- Library refresh when new audio is detected
- Current-track highlighting

Available sorting options:

- Title A–Z
- Title Z–A
- Artist A–Z
- Recently Added
- Album A–Z
- Shortest duration
- Longest duration

The cached library metadata still needs further validation to ensure Recently Added remains consistent after an application restart.

## Playback and Crossfade

Playback uses AndroidX Media3 ExoPlayer and MediaSession. Nusa stores the current track, playback position, play/pause state, shuffle mode, and repeat mode for restoration when the application is reopened.

Crossfade uses a second ExoPlayer during track transitions. Its duration can be set from 1 to 12 seconds; the default is 5 seconds. The implementation preserves queue behavior for Next, Previous, Repeat, and Shuffle during the handoff.

## Audio Quality

Nusa reads audio metadata and displays Lossless or Hi-Res indicators when the available metadata meets the application's criteria.

Supported metadata examples include:

- Apple Lossless (ALAC)
- FLAC

The song-list Hi-Res marker is shown when the reported bit depth is 24-bit or higher. Unknown bit depth does not qualify for the marker. The badge reflects file metadata; it does not establish that the playback output is bit-perfect.

USB DAC integration, including earlier Decent USB Audio work, is currently paused. Bit-perfect playback is not marked as production-ready and has not completed hardware validation.

## Artist Information

Artist portraits are looked up and cached locally. Artist biographies use the Last.fm API. For tracks with multiple artists, the first artist is used for lookup. Biography requests use Indonesian text when available, without falling back to English.

To configure Last.fm, provide an API key through one of these sources:

```properties
LASTFM_API_KEY=YOUR_LASTFM_API_KEY
```

Supported locations are the project `local.properties`, a Gradle project property (`-PLASTFM_API_KEY=...` or `~/.gradle/gradle.properties`), or the `LASTFM_API_KEY` environment variable. The value is bundled into the app at build time. Do not commit API keys or `local.properties`.

## Appearance and Settings

Settings include:

- Player presentation: Vinyl or Immersive Artwork
- Animated Artwork on/off
- Song title size
- System title fonts: Default, Sans Serif, Serif, Monospace, and Cursive
- Custom title fonts in TTF and OTF formats
- Playback button style
- Crossfade enablement and duration

Custom fonts are stored in the application's private storage and restored when Nusa starts again. The interface language follows the device language where translations are provided.

## Performance

Performance-related measures include lazy list rendering, stable list item keys, cached and downsampled artwork, background artwork loading, and reduced work for the blurred artwork layer. Animated video track selection is limited to reduce decoding load, and the video is removed when its page is not visible.

Testing is still required across Android devices, large music libraries, long playback sessions, and varying network conditions.

## Technology Stack

- Kotlin
- Jetpack Compose and Material 3
- AndroidX Media3 ExoPlayer and MediaSession
- Kotlin Coroutines
- Android MediaStore
- Storage Access Framework
- Last.fm API
- Apple Music Animated Artworks community API

## Android Configuration

| Setting | Value |
|---|---|
| compileSdk | 36 |
| targetSdk | 36 |
| minSdk | 29 |
| Java | 17 |
| Media3 | 1.9.3 |
| Compose BOM | 2025.10.01 |

## Build

Open the project in Android Studio and allow Gradle sync to finish. To build a debug APK from the project root:

```bash
./gradlew assembleDebug
```

On Windows:

```bat
gradlew.bat assembleDebug
```

The APK is normally written to `app/build/outputs/apk/debug/`.

## Project Structure

```text
app/
└── src/main/
    ├── kotlin/com/yohandeku32/nusamusic/
    │   ├── MainActivity.kt
    │   ├── data/
    │   ├── model/
    │   └── playback/
    └── res/
```

## Remaining Work

- Validate playback stability with Animated Artwork enabled on different devices.
- Test the library with large collections and long playback sessions.
- Resolve the remaining Recently Added cache consistency issue.
- Continue USB DAC work after hardware testing is available.
- Complete device testing before preparing a production release.

## Repository

https://github.com/yohandeku32/nusamusic

Package name: `com.yohandeku32.nusamusic`
