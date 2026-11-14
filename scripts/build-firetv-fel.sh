#!/usr/bin/env bash
set -euo pipefail

# Build the optional, ARMv7-only Fire TV Cube 3 JNI libraries from source.
# LIBDOVI_DIR contains include/libdovi/rpu_parser.h and lib/libdovi.a built
# for armv7-linux-androideabi (dolby_vision 3.3.2 with cargo-c).
: "${ANDROID_NDK_HOME:?Set ANDROID_NDK_HOME to Android NDK r28c}"
: "${LIBDOVI_DIR:?Set LIBDOVI_DIR to the ARMv7 libdovi install prefix}"
repo_dir="$(cd "$(dirname "$0")/.." && pwd)"
build_dir="$repo_dir/native/build/armeabi-v7a"
output_dir="$repo_dir/app/src/main/jniLibs/armeabi-v7a"
cmake -S "$repo_dir/native" -B "$build_dir" \
    -DCMAKE_TOOLCHAIN_FILE="$ANDROID_NDK_HOME/build/cmake/android.toolchain.cmake" \
    -DANDROID_ABI=armeabi-v7a -DANDROID_PLATFORM=android-26 \
    -DANDROID_STL=c++_static -DCMAKE_BUILD_TYPE=RelWithDebInfo \
    -DLIBDOVI_DIR="$LIBDOVI_DIR"
cmake --build "$build_dir" --parallel "${FEL_BUILD_JOBS:-4}"
mkdir -p "$output_dir"
cp "$build_dir/libwholphinfel.so" "$build_dir/dvfelplay/libdvfelplay.so" "$output_dir/"
# The normal Gradle build packages these generated libraries. They are gitignored.
printf '%s\n' 'Native FEL libraries ready. Build with ./gradlew :app:assembleFiretvDebug'
