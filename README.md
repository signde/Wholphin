# Wholphin FEL

Experimental fork of [Wholphin](https://github.com/damontecres/Wholphin), an
open-source Jellyfin client for Android TV. This fork adds native Dolby Vision
Profile 7 FEL and MEL playback on Fire TV Cube 3 using the Fire TV AV Magisk module.

## What’s added

- Hardware Profile 7 FEL processing through ExoPlayer, with separate base-layer
  and enhancement-layer decoding. FEL and MEL keep their original Profile 7 metadata.
- HDMI audio reconnect handling for TrueHD passthrough during refresh-rate and
  Dolby Vision transitions, plus local FFmpeg audio decoding when needed.
- Dolby Vision profile and FEL/MEL labels from Jellyfin metadata.
- Signde’s colored media icons in title details, Recommended rows, and library
  views with **Show details** enabled. The **Colored media icons** option is on by
  default under Settings → Advanced → Interface. Dolby Vision icons are green for
  P7 FEL, tangerine for P7 MEL, and white for other profiles.

FEL/MEL labels and their colored icons require the optional
[HDR Metadata server plugin](https://github.com/signde/jellyfin-plugin-hdr-metadata).
The plugin is not required for FEL playback.

## Requirements

- Fire TV Cube 3 (Gazelle), rooted with Magisk and a compatible
  [Fire TV AV module](https://github.com/signde/firetv-passthrough/releases).
  The released FEL module supports Fire OS **7.7.0.2 (PS7702.4965N)** and
  **7.7.1.7 (PS7717.5741N)**. Check the module’s compatibility list before installing.
- A Dolby Vision-capable display. Lossless audio passthrough also requires a
  compatible HDMI audio system.
- Jellyfin server **12.4.0** for this initial release. Other server versions have
  not been validated with this fork.

Native FEL support is for Cube 3 only. The Fire TV Stick 4K Max 2 is not supported
for this feature.

## Installation

1. Back up your app settings and server configuration.
2. Install the Fire TV AV module on supported firmware and reboot into Fire OS.
3. Sideload the **armeabi-v7a APK** from this fork’s
   [Releases](https://github.com/signde/Wholphin/releases). Store versions of
   Wholphin do not include these changes.
4. Open **Wholphin FEL**, connect to Jellyfin, and select **ExoPlayer** for playback.
   Enable audio passthrough and frame-rate matching as appropriate for your setup.
5. For FEL/MEL library labels, install the optional HDR Metadata plugin and refresh
   metadata for the titles you want labeled.

The app uses `com.github.damontecres.wholphin.fel`, so it can coexist with stock
Wholphin. Future signed releases can install over this release. An earlier debug
build with the same package uses a different key: back up its settings and
uninstall it before installing the signed APK. Updates to this Fire TV build
must be installed manually from this fork’s releases.

## Limitations

- Experimental. Back up first and use at your own risk. If it breaks, you keep
  both pieces.
- Native Profile 7 playback requires unencrypted content and an active FEL module.
  MPV does not use this fork’s FEL integration.
- 4K60 FEL performance has not been validated. Start with normal movie frame rates.
- Jellyfin may still transcode unsupported media. Check its playback information
  if a title does not direct play.
- TrueHD sample-rate support depends on the connected audio system. Local PCM
  decoding does not preserve Atmos objects.
- Files with missing or conflicting FEL/MEL metadata use the generic Dolby Vision
  icon. The plugin cannot label remote streams or files with multiple video tracks.

## Credits and development

Thanks to [damontecres and the Wholphin contributors](https://github.com/damontecres/Wholphin)
for the client, and [djnice](https://github.com/djnice/vlc-fel-android) for the
Android FEL work adapted for Cube 3. This fork also uses quietvoid’s libdovi and
Media3’s FFmpeg audio extension. The media-flag artwork is from signde’s Kodi addon.

See [native/README.md](native/README.md) for the playback implementation, dependency
licenses, build instructions, and tests.
