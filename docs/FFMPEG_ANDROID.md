# Optional FFmpeg decoder for NusaMusic (Android)

NusaMusic uses **AndroidX Media3 1.9.3**. The default APK uses Android / Media3's
built-in audio decoders. Media3's official `decoder_ffmpeg` extension **is not
published to Google's Maven repository** and must be built with Android NDK
and packaged separately. NusaMusic has the renderer wiring in place, but
**FFmpeg is NOT active in an APK unless its native AAR is provided**.

## What it can and cannot do

- The standard Media3 decoder keeps priority; FFmpeg is a fallback for an
  extracted audio stream that the platform decoder cannot handle.
- The native build enables `aac`, `mp3`, `flac`, `alac`, `vorbis`, `opus`,
  `ac3`, `eac3`, `truehd` and `dca` decoders. Actual playback still depends on
  the container/extractor and the exact media format being recognized by Media3.
- **It does not automatically add APE, WMA, AIFF, or other unsupported container
  demuxers.** Simply compiling an FFmpeg decoder does not add an FFmpeg
  container extractor to ExoPlayer or make Android MediaStore index those files.
- Nothing changes for the CD view, track queue, Shuffle, or crossfade: both
  ExoPlayer instances use the same extension-aware renderer factory.

## Build the official native extension (Linux)

Prerequisites: JDK, Android SDK, **NDK r26b**, CMake 3.21+, Git and Make.
The helper script pins AndroidX Media3 to **1.9.3** and FFmpeg to **n6.0**
rather than using an unverified third-party AAR.

1. Install the NDK using Android SDK Manager; set
   `ANDROID_NDK_HOME=/path/to/Android/Sdk/ndk/26.1.10909125`.
2. From the **NusaMusic repository root**, run:

   ```bash
   bash scripts/build-ffmpeg-android.sh
   ```

   It downloads the tagged official source into the ignored
   `build/ffmpeg-native/` directory, builds the audio codecs for
   `armeabi-v7a`, `arm64-v8a`, `x86`, and `x86_64`, assembles the AndroidX
   `media3-lib-decoder-ffmpeg` release AAR, and copies it to
   `app/libs/media3-decoder-ffmpeg.aar`.
3. Build a **real FFmpeg-enabled** NusaMusic APK:

   ```bash
   ./gradlew :app:assembleDebug -PrequireFfmpegDecoder=true
   ```

   This flag fails the build if the AAR is missing. The normal build
   `./gradlew :app:assembleDebug` deliberately works without FFmpeg.

4. Install on your Android device, play a supported sample that requires
   FFmpeg's decoder, and check native loading:

   ```bash
   adb logcat -s NusaDecoder
   ```

   Expect `FFmpeg decoder available: ...`. If the message says the
   extension is missing or JNI cannot load, **FFmpeg is not active**.

If you already built a matching AAR, copy it to
`app/libs/media3-decoder-ffmpeg.aar` instead of using the script. The
Gradle build checks that it includes `libffmpegJNI.so`. An AAR without the
native library is rejected.

## Production and licensing

The AndroidX extension source is Apache-2.0. Its native FFmpeg components
have separate LGPL/GPL licensing obligations, depending on their compile
flags and enabled components. Before distributing an FFmpeg-enabled APK,
review the exact FFmpeg configuration and library licenses; provide the
required notices, corresponding source code, and other license materials.
Do not redistribute proprietary codec libraries without the relevant
rights. Native FFmpeg libraries increase APK size and should be tested on
each ABI and Android version you ship.

References:

- [AndroidX Media3 FFmpeg decoder build instructions (tag 1.9.3)](https://github.com/androidx/media/blob/1.9.3/libraries/decoder_ffmpeg/README.md)
- [AndroidX Media3 supported formats and extractor limitations](https://developer.android.com/media/media3/exoplayer/supported-formats)
- [FFmpeg licensing](https://ffmpeg.org/legal.html)
