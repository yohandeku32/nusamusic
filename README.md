# Nusa Music

Modern Android music player focused on local/offline playback with a clean vinyl-style interface.

## Current Features

- Scan and play local audio files from the Android device
- Background playback with AndroidX Media3 / ExoPlayer and MediaSession
- Full-screen scrolling music player
- Vinyl-style rotating record with embedded album artwork
- Album artwork loading with memory/disk caching for smoother performance
- Automatic artist portrait lookup from Deezer with local caching
- Artist portraits displayed in monochrome
- Audio quality badge for ALAC/FLAC:
  - **Lossless** for bit depth below 24-bit
  - **Hi-Res Lossless** for 24-bit and above
- Displays the supplied Apple-style Lossless logo asset
- Remembers the last played song and playback position across app restarts
- Remembers shuffle and repeat settings
- Floating Shuffle and Repeat controls when scrolling through the library
- Floating button to return to the main player
- Share current song
- Favorite toggle on the player
- Swipe up to open the artist information area
- Artist biography loaded from Last.fm using the artist.getInfo API
- Biography lookup tries Indonesian first and falls back to English
- Biography results cached in memory to avoid repeated requests for the same artist
- Light and dark theme support
- Immersive player presentation with the Android status bar hidden while the navigation bar remains available

## Performance

The project is optimized for a local library with hundreds of songs:

- `LazyColumn` for virtualized song rendering
- Stable item keys and content types
- Downsampled album-art decoding
- Bounded bitmap caching
- Background loading for artwork and artist portraits
- Reduced scroll-triggered recomposition for smoother scrolling
- Lightweight vinyl rendering for smoother rotation

## Audio Quality Detection

For supported lossless tracks, Nusa Music reads local media metadata to determine codec and bit depth.

Examples:

```text
Apple Lossless  → Lossless / Hi-Res Lossless
FLAC            → Lossless / Hi-Res Lossless
```

The quality tier is based on the detected bit depth:

```text
< 24-bit   → Lossless
>= 24-bit  → Hi-Res Lossless
```

## Artist Biography

The former lyrics area is now used for **artist biography**.

Nusa Music uses the Last.fm `artist.getInfo` API. The app requests the Indonesian biography first and falls back to English when needed. Last.fm's artist metadata includes a biography field, and the response also provides the resolved artist name and source URL.

### Last.fm API Key

Last.fm requires an API key for `artist.getInfo`. The key is read locally from the project's `local.properties` file:

```properties
LASTFM_API_KEY=YOUR_LASTFM_API_KEY
```

Do not commit `local.properties` or publish the API key in the repository. Internet access is required when fetching artist biographies.

## Artist Images

When a song starts playing, Nusa Music can look up the artist name through the public Deezer artist search endpoint.

The lookup is cached locally so the same artist does not need to be requested repeatedly. When a portrait cannot be resolved, the app falls back to local album artwork.

Internet access is needed for fetching artist portraits and artist biographies.

## Tech Stack

- Kotlin
- Jetpack Compose
- Material 3
- AndroidX Media3 / ExoPlayer
- MediaSession
- Android MediaStore
- Coroutines

## Project

Package: `com.yohandeku32.nusamusic`

Repository: [github.com/yohandeku32/nusamusic](https://github.com/yohandeku32/nusamusic)

Open the project in Android Studio and sync Gradle.

## Notes

The current UI is intentionally kept stable. New changes should preserve the existing player design unless a specific UI change is requested.

