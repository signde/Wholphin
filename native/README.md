# Native Profile 7 playback on Fire TV Cube 3

This fork adds hardware Dolby Vision Profile 7 playback to
Wholphin's ExoPlayer backend. It requires the Fire TV AV Magisk module with
its shared `/dev/dvfel` interface. No module or system file is installed by
the player.

FEL access units are split before MediaCodec input. The base layer and original
Profile 7 RPU go to the Dolby decoder; the enhancement layer goes to a separate
hardware HEVC decoder and the module's Dolby composer path. Frames are paired
using the base-layer codec timestamp. MEL keeps its original Profile 7 RPU and
base layer, removing only the unused nested enhancement-layer NALs. Neither
native path converts the stream to Profile 8.1.

Native processing requires unencrypted Profile 7 content and the module’s helper
on Gazelle. Other streams keep Wholphin's normal playback path. The
native path disables video tunneling and uses synchronous codec input so EL
pairing cannot precede BL submission. Jellyfin can still transcode a file for
other reasons, such as unsupported audio or a codec level outside its device
profile; check the server's playback decision.

## Credits

`dvfelplay/` is copied from the Fire TV adaptation of djnice's VLC FEL work:
https://github.com/signde/vlc-fel-android, commit
`58b7b69259c9ed1839ec6247a562cfcbda751175`.
It is GPL-2.0-or-later; the original license and source notices are retained.
The original project is https://github.com/djnice/vlc-fel-android.

This fork adds the JNI bridge and MediaCodec integration.
The native library links quietvoid's MIT-licensed `dolby_vision` / libdovi 3.3.2.
Its license is included in `LIBDOVI-LICENSE`.

## Development build

Build libdovi 3.3.2 for `armv7-linux-androideabi` with cargo-c, then set
`ANDROID_NDK_HOME` to NDK r28c and `LIBDOVI_DIR` to its install prefix. Run:

```sh
./scripts/build-firetv-fel.sh
./gradlew :app:assembleFiretvDebug
```

The native build produces gitignored `.so` files for the Cube's 32-bit Android
userspace. Install the `armeabi-v7a` APK. The app is named **Wholphin FEL** and uses
`com.github.damontecres.wholphin.fel` for both debug and release builds. It can
coexist with the original Wholphin app. Its launcher icon and TV banner include
a FEL label. Builds without the native libraries use upstream playback behavior.

The adapter tests cover codec queue ordering, failed queue submission, seek
flush, EOS/config buffers, and release cleanup. The standalone native test
checks that EL removal preserves the original RPU and respects buffer bounds:

```sh
./gradlew :app:testFiretvDebugUnitTest --tests '*FireTvFelCodecAdapterTest'
clang++ -std=c++17 -fsanitize=address,undefined native/tests/annexb_test.cpp -o /tmp/wholphin-annexb-test
/tmp/wholphin-annexb-test
```

This build is experimental. Decoder activity alone does not confirm correct
FEL output or audio sync; those need visual and listening checks. 4K60 FEL
performance has not been validated.

## Local audio decoding and library labels

Run `scripts/build-firetv-audio.sh` before building the app to include Media3’s
FFmpeg audio extension. It uses Media3 1.11.1 and FFmpeg 9.0, pinned to the source
revisions in the script. It requires JDK 17, the Android SDK, NDK r28c, CMake,
Make, Python, and Git. Set `JAVA_HOME`, `ANDROID_HOME`, and `ANDROID_NDK_HOME`
first. The extension decodes TrueHD, DTS, AC3/EAC3, and the other codecs supported
by upstream Wholphin’s audio extension. Upstream copyrights and licenses are
retained.

TrueHD passthrough depends on the connected audio system’s capabilities. After a
refresh-rate change, the player waits for HDMI audio to reconnect before choosing
its audio path. It also retains queued audio during a brief reconnect when the
Dolby Vision decoder starts. Both waits have time limits so playback cannot wait
indefinitely for HDMI.

A connection that advertises only 48 kHz TrueHD may reject a 96 kHz track; the app
does not impose a 48 kHz limit. The FFmpeg extension remains available for local
decoding, but PCM output does not preserve Atmos objects.

The Profile 7 device profile uses the Dolby Vision decoder’s UHD capability
instead of the generic HEVC decoder’s lower reported level. Other HEVC formats
keep their existing limits.

Library labels use Jellyfin’s DV profile metadata. FEL/MEL labels require the
[HDR Metadata server plugin](https://github.com/signde/jellyfin-plugin-hdr-metadata),
which annotates the video stream’s `Comment` field. Missing or conflicting
annotations are not treated as FEL or MEL. Labels describe the source file, not
the current output mode.
