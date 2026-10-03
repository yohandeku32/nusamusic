# Nusa Music

Nusa Music is a modern Android music player focused on local/offline playback, a realistic vinyl-style player, and a future-ready Hi-Res / USB DAC audio path.

> **Project status:** Active development  
> **Status snapshot:** 3 October 2026  
> **Current branch:** `main`

## Project Progress

The project has moved beyond the basic music-player prototype. The UI, local library, MediaSession playback, artist information, persistence, performance optimizations, and the first integration of a dedicated Hi-Res USB audio engine are now implemented.

The remaining work is mainly **hardware validation and refinement of the Hi-Res path**, especially testing with real USB DACs and confirming sample-rate / bit-depth transitions on target devices.

### Overall status

| Area | Status | Notes |
|---|---|---|
| Local music scanning | ✅ Implemented | Reads local audio through Android MediaStore |
| Normal playback | ✅ Implemented | Media3 / ExoPlayer |
| Background playback | ✅ Implemented | MediaSession + PlaybackService |
| Lock-screen / notification control | ✅ Implemented | Media session controls are tied to Nusa Music |
| Player UI | ✅ Implemented | Full-screen player with swipe navigation |
| Realistic vinyl | ✅ Implemented | Physical thickness, grooves, grain and subtle wear |
| Smooth vinyl rotation | ✅ Implemented | Gradual acceleration/deceleration on Play/Pause |
| Album artwork | ✅ Implemented | Memory/disk caching and downsampled decoding |
| Library grid | ✅ Implemented | 2-column grid with stable card sizes |
| Library search | ❌ Removed | Replaced by A–Z index |
| A–Z library index | ✅ Implemented | Tap and vertical swipe navigation |
| Sorting | ✅ Implemented | Title, artist, album and duration |
| Artist portrait | ✅ Implemented | Deezer lookup with local caching |
| Artist biography | ✅ Implemented | Last.fm `artist.getInfo` |
| Biography internal scrolling | ✅ Implemented | Long text scrolls inside the biography box |
| Share | ✅ Implemented | Custom Nusa Music share icon |
| Favorite | ✅ Implemented | Custom favorite icon |
| Shuffle / Repeat | ✅ Implemented | Media3 playback modes |
| Playback state restore | ✅ Implemented | Song, position, shuffle and repeat |
| Title font-size setting | ✅ Implemented | Stored in SharedPreferences |
| Hi-Res metadata badge | ✅ Implemented | Lossless / Hi-Res Lossless indicator |
| Dedicated USB Hi-Res engine | 🟡 Integrated | Decent USB Audio engine is wired into PlaybackService |
| USB DAC permission flow | 🟡 Integrated | USB Audio device detection and permission handling added |
| Bit-perfect hardware validation | ⏳ Pending | Requires real USB DAC testing |
| DSD playback | ⏳ Not implemented | Current dedicated native path targets FLAC/PCM |
| Production release build | ⏳ Pending | Final validation and device testing still required |

## Current UI

### Main Player

The player currently includes:

- Realistic rotating vinyl record
- Album artwork embedded in the vinyl
- Configurable song-title font size
- Playback controls
- Shuffle and repeat
- Share and favorite controls
- Lossless / Hi-Res Lossless badge
- Artist portrait
- Artist biography area
- Swipe navigation between player and library

### Vinyl Rendering

The vinyl is intentionally rendered as a physical object instead of a flat black circle.

Current rendering includes:

- visible edge thickness
- pressed groove rings
- subtle PVC grain
- fine hairlines
- sleeve-rub / micro-scuff details
- glossy reflected light
- center well and spindle
- continuous rotation angle while paused/resuming

Playback rotation uses gradual acceleration and deceleration rather than an abrupt start/stop.

## Music Library

The library uses a **2-column grid** designed for large local collections.

Current behavior:

- fixed card height to prevent layout jumping
- large album artwork
- worn paper / plastic-sleeve texture
- subtle physical shadow
- active-track emphasis without a large selection border
- title and artist shown below artwork
- sorting menu
- A–Z index on the right side
- vertical swipe across A–Z index
- smooth cancellation of previous index scroll animations

The former search button has been intentionally removed.

## Library Performance

Performance work currently includes:

- `LazyVerticalGrid` for virtualized library rendering
- stable song keys and content types
- fixed grid item height
- downsampled artwork decoding
- bounded bitmap caching
- background artwork / portrait loading
- reduced unnecessary recomposition
- delayed centering of the active song until the pager settles
- adjacent pager page preparation
- lightweight custom vinyl rendering

The player-to-library swipe was also optimized so the library does not repeatedly recalculate the active-song position while the user is still dragging.

## Artist Biography

The old lyrics area was replaced with artist biography.

Nusa Music uses the Last.fm `artist.getInfo` API.

The lookup strategy is:

1. Request Indonesian biography first
2. Fall back to English when needed
3. Cache the resolved biography in memory
4. Display the biography in a fixed dark panel
5. Allow long biography text to scroll **inside the panel**

### Last.fm API Key

Add the key to `local.properties`:

```properties
LASTFM_API_KEY=YOUR_LASTFM_API_KEY
```

Do not commit `local.properties` or expose the API key publicly.

## Artist Images

Artist portraits are resolved through the public Deezer artist-search endpoint.

The app:

1. normalizes the artist name
2. checks memory cache
3. checks local persistent cache
4. searches Deezer
5. downloads and stores the portrait
6. falls back to album artwork when no portrait is found

Portraits are displayed in monochrome in the player.

## Audio Quality Detection

Nusa Music reads local media metadata to identify codec and bit depth.

Examples:

```text
Apple Lossless → Lossless / Hi-Res Lossless
FLAC           → Lossless / Hi-Res Lossless
```

Current UI tier:

```text
< 24-bit  → Lossless
>= 24-bit → Hi-Res Lossless
```

This badge is a **metadata classification**, not proof that the Android output path is bit-perfect.

## Hi-Res / USB DAC Audio Engine

A dedicated USB audio path has now been integrated using the open-source **Decent USB Audio** components from:

https://github.com/Ma145/decent-player

The pinned upstream release is:

```text
v0.1.0-libs
```

### Integrated components

- `decent-usb-audio-driver`
- `decent-usb-audio-wrapper-media3`

The upstream project describes these components as MIT-licensed original work. Its native FLAC source also contains the applicable upstream FLAC notices. See `THIRD_PARTY_NOTICES.md`.

### Current architecture

```text
Nusa Music UI
      ↓
MediaSession / Media3
      ↓
ExoPlayer
      ↓
UsbAudioSink
      ↓
Decent USB Audio Engine
      ↓
USB Audio Class 2.0
      ↓
USB DAC
```

The project keeps the normal Media3 path available and introduces the USB engine as the dedicated output route when a compatible USB DAC is available.

### Current Hi-Res integration

Implemented in the codebase:

- USB Audio Class device detection
- USB permission handling
- USB device attachment intent handling
- dedicated Media3 `AudioSink`
- native USB isochronous output path
- sample-rate-aware USB configuration
- bit-depth-aware USB output
- native FLAC engine path supplied by the upstream component
- Media3 load-control integration for the native path
- MediaSession integration with the dedicated PlaybackService

### Important current limitation

The Hi-Res engine integration is **code-complete enough for the next validation stage, but it has not yet been declared hardware-verified for Nusa Music**.

The following still need to be tested on a real device + DAC combination:

- FLAC 24-bit / 96 kHz
- FLAC 24-bit / 192 kHz
- sample-rate switching between tracks
- DAC bit-depth negotiation
- seeking while using the native engine
- Pause / Resume
- track-to-track transitions
- USB reconnect / permission recovery
- long-duration playback stability

The project's normal Android audio path should remain available as a fallback when a compatible USB DAC is not present.

## Media Notification

Nusa Music uses AndroidX Media3 `MediaSession` through `PlaybackService`.

The session explicitly defines a `sessionActivity` pointing to:

```text
MainActivity
```

This means tapping the Nusa Music media notification can return to Nusa Music rather than leaving the user in another media application.

Android may still display multiple media sessions from different applications; a control belonging to another application's session will continue to control that application's player.

## Playback Persistence

The player stores:

- current media ID
- playback position
- play/pause state
- shuffle mode
- repeat mode

When the application starts again, the previous track and approximate position can be restored.

## Technology Stack

- Kotlin
- Jetpack Compose
- Material 3
- AndroidX Media3 / ExoPlayer
- MediaSession
- Android MediaStore
- Kotlin Coroutines
- Native C++ / JNI for the dedicated USB audio path
- Last.fm API
- Deezer public artist search endpoint

## Project Requirements

Current Hi-Res USB integration requires:

- Android 10 / API 29 or newer
- a compatible USB Audio Class 2.0 DAC
- USB host support
- Media3 / ExoPlayer
- internet access for Gradle to obtain the pinned Decent Audio Engine AARs during the first build

The first build after pulling the Hi-Res integration may download the upstream engine artifacts from the pinned GitHub release.

## Project Structure

Main application package:

```text
com.yohandeku32.nusamusic
```

Important areas:

```text
app/
 └─ src/main/kotlin/com/yohandeku32/nusamusic/
      ├─ MainActivity.kt
      ├─ data/
      │   ├─ ArtworkLoader.kt
      │   ├─ ArtistImageLoader.kt
      │   ├─ ArtistBiographyLoader.kt
      │   ├─ AudioCodecLoader.kt
      │   └─ MusicRepository.kt
      ├─ model/
      │   └─ Song.kt
      └─ playback/
          └─ PlaybackService.kt
```

The Hi-Res engine AARs are downloaded by Gradle and cached under the application's build directory.

## Third-Party Notices

See:

```text
THIRD_PARTY_NOTICES.md
```

for the current attribution and licensing notes for the integrated Decent USB Audio components.

## Current Development Stage

Nusa Music is currently at the stage of:

```text
UI/UX            → Advanced / stable iteration
Local playback   → Implemented
MediaSession     → Implemented
Library          → Implemented
Artist metadata  → Implemented
Performance      → Ongoing optimization
Hi-Res engine    → Integrated
USB DAC          → Ready for hardware validation
Release          → Not final yet
```

### Next technical milestone

The next milestone is to validate the dedicated audio engine on a real Android phone and USB DAC, then inspect Logcat and actual DAC-reported sample rate / bit depth during playback.

The project should only be considered **Hi-Res verified** after those hardware tests succeed.

## Repository

GitHub:

https://github.com/yohandeku32/nusamusic

Package:

`com.yohandeku32.nusamusic`

Current branch:

`main`
