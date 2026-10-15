#!/usr/bin/env bash
# Build AndroidX Media3 1.9.3's FFmpeg audio decoder against FFmpeg 6.0.
# Run from Linux with: bash scripts/build-ffmpeg-android.sh
set -euo pipefail

project_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
build_root="${FFMPEG_BUILD_ROOT:-$project_root/build/ffmpeg-native}"
media_root="$build_root/media3"
ffmpeg_root="$build_root/ffmpeg"
ndk_root="${ANDROID_NDK_HOME:-${ANDROID_NDK_ROOT:-}}"
host_platform="${FFMPEG_HOST_PLATFORM:-linux-x86_64}"

if [[ -z "$ndk_root" || ! -d "$ndk_root/toolchains/llvm/prebuilt/$host_platform" ]]; then
  echo "Set ANDROID_NDK_HOME to an installed Android NDK (recommended: r26b)." >&2
  exit 1
fi
for command in git cmake make unzip; do
  command -v "$command" >/dev/null || { echo "Missing tool: $command" >&2; exit 1; }
done
mkdir -p "$build_root"
if [[ ! -d "$media_root/.git" ]]; then
  git clone --depth=1 --branch 1.9.3 https://github.com/androidx/media.git "$media_root"
fi
if [[ ! -d "$ffmpeg_root/.git" ]]; then
  git clone --depth=1 --branch n6.0 https://github.com/FFmpeg/FFmpeg.git "$ffmpeg_root"
fi

module_main="$media_root/libraries/decoder_ffmpeg/src/main"
jni_root="$module_main/jni"
if [[ ! -L "$jni_root/ffmpeg" && ! -e "$jni_root/ffmpeg" ]]; then
  ln -s "$ffmpeg_root" "$jni_root/ffmpeg"
fi

# FFmpeg 6.0 autoconfigure can accidentally detect host iconv while cross-
# compiling with NDK r26b; Android's linker has no iconv_open/iconv_close.
# Disable iconv for these audio decoders in the upstream build helper.
if ! grep -q -- '--disable-iconv' "$jni_root/build_ffmpeg.sh"; then
  sed -i 's/--disable-vulkan/--disable-vulkan --disable-iconv/' "$jni_root/build_ffmpeg.sh"
fi
grep -q -- '--disable-iconv' "$jni_root/build_ffmpeg.sh" || {
  echo "Could not turn off FFmpeg iconv auto-detection" >&2
  exit 1
}

# Only enable audio decoders explicitly mapped by FfmpegLibrary. Additional
# native codecs alone don't make unsupported container formats demuxable.
decoders=(aac mp3 flac alac vorbis opus ac3 eac3 truehd dca)
(
  cd "$jni_root"
  bash ./build_ffmpeg.sh "$module_main" "$ndk_root" "$host_platform" 29 "${decoders[@]}"
)
(
  cd "$media_root"
  chmod +x ./gradlew
  ./gradlew :lib-decoder-ffmpeg:assembleRelease --no-daemon
)

aar="$(find "$media_root/libraries/decoder_ffmpeg/build/outputs/aar" -maxdepth 1 -type f -name '*release.aar' -print -quit)"
if [[ -z "$aar" ]]; then
  echo "FFmpeg extension AAR not produced by the upstream Gradle build." >&2
  exit 1
fi
if ! unzip -Z -1 "$aar" | grep -Eq '^jni/[^/]+/libffmpegJNI\.so$'; then
  echo "The AAR contains no libffmpegJNI.so: refusing to install a Java-only stub." >&2
  exit 1
fi
mkdir -p "$project_root/app/libs"
cp "$aar" "$project_root/app/libs/media3-decoder-ffmpeg.aar"
echo "FFmpeg AAR ready: $project_root/app/libs/media3-decoder-ffmpeg.aar"
echo "Now build Nusa: ./gradlew :app:assembleDebug -PrequireFfmpegDecoder=true"
