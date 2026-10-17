# Third-party notices

## Accompanist Lyrics

Nusa uses Accompanist Lyrics Core **0.4.2** and Lyrics UI **1.0.15** from
[6xingyv/accompanist-lyrics-core](https://github.com/6xingyv/accompanist-lyrics-core)
and [6xingyv/accompanist-lyrics-ui](https://github.com/6xingyv/accompanist-lyrics-ui).
Both are distributed under Apache License 2.0. These releases keep the lyrics
renderer on the app's Compose 1.9 generation. The original license is included
in `app/src/main/assets/licenses/accompanist-apache-2.0.txt`.

Optional online lyrics are retrieved from [LRCLIB](https://lrclib.net), with
attribution in Settings → Lyrics. Imported lyrics are copied into app storage;
online results are cached separately.

NusaMusic integrates the **Decent USB Audio** engine from:

https://github.com/Ma145/decent-player

The integrated release is pinned to **v0.1.0-libs**.

The following components are used by NusaMusic:

- decent-usb-audio-driver
- decent-usb-audio-wrapper-media3

These components are distributed by their upstream project under the MIT License.
The USB driver release also contains the upstream libFLAC implementation and
its applicable third-party notices. See the upstream repository for the
complete license and notice files.

The engine is downloaded by Gradle at build time from the pinned GitHub
release asset URLs and cached under the Gradle build directory.

## Hi-Res Audio logo

`app/src/main/res/drawable-nodpi/hi_res_audio_logo.png` is a bundled thumbnail
of the [Hi-Res Audio logo](https://en.wikipedia.org/wiki/File:Hi-Res_Audio_(logo).svg).
Wikimedia identifies the logo as public domain because it is ineligible for
copyright; attribution is not required. The logo is trademarked and is used
only to identify Hi-Res audio.

## Optional FFmpeg audio decoder

FFmpeg is **not bundled in normal NusaMusic builds**. When an FFmpeg-enabled
APK is built using the optional native AAR, it includes the AndroidX Media3
FFmpeg decoder module (Apache-2.0) and statically linked FFmpeg libraries
with their **separate** LGPL/GPL licensing requirements. The build helper
pins AndroidX Media3 1.9.3 and upstream FFmpeg n6.0. Shipping such an APK
requires reviewing the exact FFmpeg build configuration, source/notice
obligations, and supported ABI requirements. See
[docs/FFMPEG_ANDROID.md](docs/FFMPEG_ANDROID.md) and
https://ffmpeg.org/legal.html.
