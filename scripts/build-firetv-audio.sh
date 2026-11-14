#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
BUILD_ROOT="${AUDIO_BUILD_ROOT:-$ROOT/.build/firetv-audio}"
: "${ANDROID_NDK_HOME:?Set ANDROID_NDK_HOME to Android NDK r28c}"
: "${ANDROID_HOME:?Set ANDROID_HOME to the Android SDK}"
: "${JAVA_HOME:?Set JAVA_HOME to JDK 17}"
mkdir -p "$BUILD_ROOT"
export MEDIA3_SOURCE_DIR="$BUILD_ROOT/media"
FFMPEG_SOURCE_DIR="$BUILD_ROOT/ffmpeg"
export FFMPEG_JNI_DIR="$BUILD_ROOT/jniLibs"
if [ ! -d "$MEDIA3_SOURCE_DIR/.git" ]; then
    git clone --depth 1 --filter=blob:none --sparse --branch 1.11.1 https://github.com/androidx/media.git "$MEDIA3_SOURCE_DIR"
    git -C "$MEDIA3_SOURCE_DIR" sparse-checkout set libraries/decoder_ffmpeg
fi
if [ ! -d "$FFMPEG_SOURCE_DIR/.git" ]; then
    git clone --depth 1 --branch n9.0 https://github.com/FFmpeg/FFmpeg.git "$FFMPEG_SOURCE_DIR"
fi
[ "$(git -C "$MEDIA3_SOURCE_DIR" rev-parse HEAD)" = 8c6678b657ede1e7883fc164ef73ed483c7796c3 ]
[ "$(git -C "$FFMPEG_SOURCE_DIR" rev-parse HEAD)" = d32b387f2b0a484599d4587d651891f0c63c4238 ]
MEDIA_JNI="$MEDIA3_SOURCE_DIR/libraries/decoder_ffmpeg/src/main/jni"
[ -e "$MEDIA_JNI/ffmpeg" ] || ln -s "$FFMPEG_SOURCE_DIR" "$MEDIA_JNI/ffmpeg"
# Use Media3's build recipe, limited to the Cube's ARMv7 ABI. FFmpeg 9 removed postproc.
python3 - "$MEDIA_JNI/build_ffmpeg.sh" "$BUILD_ROOT/build-ffmpeg-arm.sh" <<'PY'
import sys
from pathlib import Path
source = Path(sys.argv[1]).read_text().replace('    --disable-postproc\n', '')
Path(sys.argv[2]).write_text(source.split('make clean')[0])
PY
HOST_PLATFORM="$(uname -s | tr '[:upper:]' '[:lower:]')-x86_64"
bash "$BUILD_ROOT/build-ffmpeg-arm.sh" "$(dirname "$MEDIA_JNI")" "$ANDROID_NDK_HOME" "$HOST_PLATFORM" 21 dca ac3 eac3 mlp truehd flac alac pcm_mulaw pcm_alaw mp3
cmake -S "$MEDIA_JNI" -B "$BUILD_ROOT/native" -G 'Unix Makefiles' \
    -DCMAKE_TOOLCHAIN_FILE="$ANDROID_NDK_HOME/build/cmake/android.toolchain.cmake" \
    -DANDROID_ABI=armeabi-v7a -DANDROID_PLATFORM=android-21 -DCMAKE_BUILD_TYPE=Release
cmake --build "$BUILD_ROOT/native" -j 6
mkdir -p "$FFMPEG_JNI_DIR/armeabi-v7a" "$ROOT/app/libs"
cp "$BUILD_ROOT/native/libffmpegJNI.so" "$FFMPEG_JNI_DIR/armeabi-v7a/"
"$ROOT/gradlew" -p "$ROOT/native/audio-extension" assembleRelease
cp "$ROOT/native/audio-extension/build/outputs/aar/firetv-ffmpeg-decoder-release.aar" "$ROOT/app/libs/lib-decoder-ffmpeg-release.aar"
