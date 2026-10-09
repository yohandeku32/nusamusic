# Nusa

Nusa is a local music player for Android, built with Kotlin and Jetpack Compose.

The project started as a simple local music player and has grown into a more complete music app with a custom vinyl-style player, library management, crossfade, artist information, and custom fonts.

> **Status:** Active development  
> **Version:** 3.9.2  
> **versionCode:** 36  
> **Branch:** `main`  
> **Last updated:** 6 October 2026

## Current Progress

Most of the core player features are already working. The project is now mainly focused on UI polish, playback stability, performance, and testing before a proper release.

| Feature | Status |
|---|---|
| Local music playback | ✅ Working |
| Background playback | ✅ Working |
| MediaSession | ✅ Working |
| Notification controls | ✅ Working |
| Shuffle / Repeat | ✅ Working |
| Crossfade | ✅ Working |
| Crossfade queue / Previous / Next | ✅ Working |
| Playback state restore | ✅ Working |
| MediaStore music scan | ✅ Working |
| Manual folder access | ✅ Working |
| Recursive folder scan | ✅ Working |
| Ignore tracks under 10 seconds | ✅ Working |
| Automatic library refresh | ✅ Working |
| Album artwork | ✅ Working |
| Main player UI | ✅ Working |
| Rotating vinyl player | ✅ Working |
| Vinyl / immersive artwork presentation setting | ✅ Working |
| Tonearm / playback position | ✅ Working |
| Song library | ✅ Working |
| A–Z index | ✅ Working |
| Sorting | ✅ Working |
| Artist portrait | ✅ Working |
| Artist biography | ✅ Working |
| Multi-artist handling | ✅ Working |
| Share | ✅ Working |
| Favorite | ✅ Working |
| Lossless / Hi-Res badge | ✅ Working |
| Hi-Res information dialog | ✅ Working |
| System title fonts | ✅ Working |
| Custom TTF / OTF fonts | ✅ Working |
| Device language support | ✅ Working |
| USB DAC playback | ⏸️ Paused |
| Bit-perfect hardware testing | ⏳ Not done |
| Production release | ⏳ Not ready |

## Main Player

The main screen can use either the existing rotating vinyl record or immersive artwork filling the player background, with the title, progress, playback controls, and actions layered over subtle top and bottom scrims. The Hi-Res badge remains on a light pill for contrast. Choose the presentation in Settings; vinyl remains the default.

Current player features include:

- rotating vinyl
- immersive album artwork with subtle scrims behind controls and player actions
- physical-looking grooves and surface detail
- vinyl shadow and edge thickness
- light reflections
- album artwork inside the record label
- tonearm movement based on playback progress
- song title and artist
- progress bar
- playback controls
- Lossless / Hi-Res badge
- artist portrait
- artist biography
- settings

The vinyl rendering is still being refined. The goal is to make the material, grooves, and light reflections feel closer to a real record without making the UI too heavy.

## Song Library

The library is currently a **list view**.

Each item shows:

```text
[Artwork]  Song Title                    [Hi-Res]
           Artist
```

The Hi-Res marker appears only when the audio metadata reports a bit depth of 24-bit or higher.

The current song is shown with a subtle active-tab style so it is easy to see which track is playing.

The library also has:

- A–Z index on the right
- tap and drag alphabet navigation
- sorting
- Recently Added
- album / artist / title / duration sorting
- plain album artwork without the extra worn-cover effect

The library background uses the same light background as the main player.

## Sorting

Available sorting options:

```text
Title A–Z
Title Z–A
Artist A–Z
Recently Added
Album A–Z
Shortest duration
Longest duration
```

## Music Scanning

Nusa can load music from Android MediaStore and from folders selected by the user.

Folder scanning is recursive, so subfolders are included.

Supported formats include:

```text
AAC
ALAC
FLAC
M4A
MP3
OGG
OPUS
WAV
WMA
```

Tracks shorter than 10 seconds are ignored.

When new music is found, the library can refresh without rebuilding the active playback queue.

## Crossfade

Crossfade uses two ExoPlayer instances during the transition.

```text
Current track
      ↓
   Fade out
      ↘
       overlap
      ↗
   Fade in
      ↓
Next track
```

Crossfade can be set from 1 to 12 seconds, with 5 seconds as the default.

The queue is restored after the handoff so that:

- Next keeps working
- Previous keeps working
- Repeat keeps working
- Shuffle order is preserved

A bug where **Previous stopped working after a crossfade handoff** has already been fixed.

## Playback State

Nusa saves:

- current song
- playback position
- play/pause state
- shuffle mode
- repeat mode

When the app is opened again, it can restore the previous playback state.

## Artist Information

### Biography

Artist biographies are loaded from Last.fm.

The current behavior is:

- use the first artist from multi-artist metadata
- request the Indonesian biography
- do not fall back to English
- show long biographies in a scrollable area

Configure the Last.fm API key using the first non-empty value found here:

```properties
LASTFM_API_KEY=YOUR_LASTFM_API_KEY
```

You can set it in the project `local.properties`, as a Gradle project property
(`-PLASTFM_API_KEY=...` or `~/.gradle/gradle.properties`), or in the
`LASTFM_API_KEY` environment variable. The value is bundled into the app at
build time. Do not commit API keys or `local.properties`.

### Artist Portrait

Artist portraits are looked up and cached locally.

For multiple artists, the first artist is used for the lookup.

## Apple Music Animated Artwork (Experimental)

Immersive Artwork can optionally show Apple Music's animated album cover as a
silent looping video. It uses the community-maintained
[Apple Music Animated Artworks API](https://github.com/m8tec/apple-music-animated-artworks)
at `https://artwork.m8tec.top`.

Nusa searches by the local song's artist, album, and title. It prefers the tall
artwork variant for immersive playback and falls back to the square variant if
needed. Local album artwork stays visible while animation loads and remains the
fallback when no animated artwork exists, the API is unavailable, or playback fails.

No Spotify Developer account or Canvas backend setup is required for this
feature. The public API is community-run, not an official Apple API, and may be
rate-limited or change. For that reason, the feature is optional and static cover
art remains the default fallback.

Apple Music artwork lookup is performed only while Immersive Artwork is shown.
Results are cached in memory to avoid repeatedly querying the service for the same album.

## Audio Quality

Nusa reads the audio metadata and shows:

```text
Lossless
Hi-Res
```

The player badge uses codec and bit depth metadata. In the song list, the Hi-Res marker is shown only when the reported bit depth is 24-bit or higher; unknown bit depth does not show the marker. The app reads metadata rather than inferring quality from the filename extension.

For example:

```text
Apple Lossless → Lossless / Hi-Res
FLAC           → Lossless / Hi-Res
```

A Hi-Res badge only describes the file metadata. It does not prove that the output path is bit-perfect.

## Custom Title Font

The title on the main player can use a different font.

System options:

```text
Default
Sans Serif
Serif
Monospace
Cursive
```

Custom font files are also supported:

```text
.TTF
.OTF
```

The selected custom font is stored in the app's private storage and is restored when the app starts again.

## Performance

Performance work has included:

- lazy list rendering
- stable item keys
- artwork caching
- background artwork loading
- downsampled image decoding
- smoother A–Z scrolling
- reduced unnecessary recomposition
- Media3 buffering configuration

There is still more testing to do with large music libraries and long playback sessions.

## USB DAC / Hi-Res

A USB Hi-Res playback path was experimented with earlier in the project, including Decent USB Audio integration.

For now, the USB playback path is **paused** and is not part of the active Nusa 3.9.2 playback flow.

That means USB DAC and bit-perfect playback are not marked as production-ready.

Hardware testing is still needed before this part of the project is brought back.

## Known Remaining Work

### Recently Added

The app already reads `dateAdded` information while scanning. The cached library data still needs a small update so **Recently Added** stays fully consistent after an app restart.

### UI

The vinyl material and light reflections are still being refined.

### Testing

More testing is still needed for:

- different Android devices
- large music libraries
- long playback sessions
- folder permission recovery
- crossfade edge cases
- release builds

## Tech Stack

- Kotlin
- Jetpack Compose
- Material 3
- AndroidX Media3
- ExoPlayer
- MediaSession
- Kotlin Coroutines
- Android MediaStore
- Storage Access Framework
- Last.fm API
- Deezer artist lookup

## Android Configuration

```text
compileSdk  = 36
targetSdk   = 36
minSdk      = 29
Java        = 17
Media3      = 1.9.3
Compose BOM = 2025.10.01
```

## Project Structure

```text
app/
└─ src/main/kotlin/com/yohandeku32/nusamusic/
   ├─ MainActivity.kt
   ├─ data/
   │  ├─ ArtworkLoader.kt
   │  ├─ ArtistImageLoader.kt
   │  ├─ ArtistBiographyLoader.kt
   │  ├─ AudioCodecLoader.kt
   │  └─ MusicRepository.kt
   ├─ model/
   │  └─ Song.kt
   └─ playback/
      └─ PlaybackService.kt
```

## Development Stage

Nusa is currently in **active development / pre-release**.

The core player is working. Most of the remaining work is refinement and testing rather than building the basic player from scratch.

```text
Core playback         → ✅
Background playback   → ✅
MediaSession          → ✅
Library               → ✅
Folder scanning       → ✅
Sorting & A–Z         → ✅
Playback persistence  → ✅
Crossfade             → ✅
Artist information    → ✅
Custom fonts          → ✅
Performance           → 🟡 Ongoing
Vinyl realism         → 🟡 Ongoing
USB DAC               → ⏸️ Paused
Hardware validation   → ⏳ Pending
Production release    → ⏳ Pending
```

## Next Steps

The next phase is mostly about polishing what is already there:

1. Finish the library and player UI details.
2. Keep crossfade and queue behavior stable.
3. Test performance with larger libraries.
4. Fix the remaining cache issue for Recently Added.
5. Continue USB DAC work when hardware testing is available.
6. Prepare a proper release build.

## Repository

https://github.com/yohandeku32/nusamusic

Package:

`com.yohandeku32.nusamusic`

Current version:

`3.9.2`
