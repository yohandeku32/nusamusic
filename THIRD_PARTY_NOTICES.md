# Third-party notices

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
