package com.github.damontecres.wholphin.ui.util

import org.jellyfin.sdk.model.api.AudioSpatialFormat
import org.jellyfin.sdk.model.api.MediaSourceInfo
import org.jellyfin.sdk.model.api.MediaStream
import kotlin.math.abs

/** A source-format badge, not a claim about the connected display or speaker layout. */
data class MediaFlag(
    val artwork: String?,
    val label: String,
)

object MediaFlags {
    private fun flag(
        key: String,
        label: String,
    ) = MediaFlag(key.takeIf { it in mediaFlagResources }, label)

    fun video(stream: MediaStream?): List<MediaFlag> =
        buildList {
            if (stream == null) return@buildList
            val range = stream.videoRangeType.name
            val dv = DolbyVisionLabel.format("Dolby Vision", stream.dvProfile, stream.comment)
            if (range == "DOVI_INVALID") {
                add(MediaFlag(null, "Dolby Vision (invalid)"))
            } else if (range.startsWith("DOVI") || !stream.videoDoViTitle.isNullOrBlank() || stream.dvProfile != null ||
                dv != "Dolby Vision"
            ) {
                val layer =
                    when {
                        dv.endsWith("P7 FEL") -> "_fel"
                        dv.endsWith("P7 MEL") -> "_mel"
                        else -> ""
                    }
                add(flag("hdr/dolbyvision$layer", dv))
            } else {
                when (range) {
                    "HDR10" -> add(flag("hdr/hdr10", "HDR10"))
                    "HDR10_PLUS" -> add(flag("hdr/hdr10plus", "HDR10+"))
                    "HLG" -> add(flag("hdr/hlg", "HLG"))
                    else -> if (stream.videoRange.name == "HDR") add(MediaFlag(null, "HDR"))
                }
            }
            val width = stream.width
            val height = stream.height
            if (width != null && height != null && width > 0 && height > 0) {
                val label = StreamFormatting.resolutionString(width, height, stream.isInterlaced)
                val key = if (label == "4K") "2160" else label.removeSuffix("p").removeSuffix("i")
                add(flag("resolution/$key", label))
            }
            aspectRatio(stream.aspectRatio)?.let { ratio ->
                val supported =
                    listOf("1.00", "1.19", "1.33", "1.37", "1.66", "1.78", "1.85", "2.00", "2.20", "2.35", "2.40", "2.55", "2.76")
                val nearest = supported.minBy { abs(it.toDouble() - ratio) }
                if (abs(nearest.toDouble() - ratio) < .015) {
                    add(flag("aspectratio/$nearest", "${stream.aspectRatio} aspect ratio"))
                } else {
                    add(MediaFlag(null, stream.aspectRatio!!))
                }
            }
            stream.codec?.takeIf { it.isNotBlank() }?.let { codec ->
                val key =
                    when (codec.lowercase()) {
                        "h265", "hev1", "hvc1" -> "hevc"
                        "avc", "avc1" -> "h264"
                        "vc-1" -> "vc1"
                        "mpeg1" -> "mpeg1video"
                        "mpeg2" -> "mpeg2video"
                        else -> codec.lowercase()
                    }
                add(flag("source/$key", codec.uppercase()))
            }
        }

    fun source(source: MediaSourceInfo?): MediaFlag? =
        when (source?.videoType?.name) {
            "BLURAY" -> flag("source/bluray", "Blu-ray")
            "DVD" -> flag("source/dvd", "DVD")
            else -> null // MKV/MP4 does not identify disc, remux, or streaming provenance.
        }

    fun audio(stream: MediaStream?): List<MediaFlag> =
        buildList {
            if (stream == null) return@buildList
            val codec = stream.codec.orEmpty().lowercase()
            val profile = stream.profile.orEmpty()
            val atmos = stream.audioSpatialFormat == AudioSpatialFormat.DOLBY_ATMOS || profile.contains("Atmos", true)
            val dtsx =
                stream.audioSpatialFormat == AudioSpatialFormat.DTSX || profile.contains("DTS:X", true) || profile.contains("DTS-X", true)
            val key =
                when {
                    atmos -> "atmos"
                    dtsx -> "dtsx"
                    profile.contains("DTS", true) && profile.contains("MA", true) -> "dtshd_ma"
                    profile.contains("DTS", true) && (profile.contains("HRA", true) || profile.contains("HD H", true)) -> "dtshd_hra"
                    codec.startsWith("pcm_") -> "pcm"
                    codec == "dts" || codec == "dca" -> "dts"
                    codec == "aif" || codec == "aifc" -> "aiff"
                    codec == "mp3float" -> "mp3"
                    codec == "ogg" -> "vorbis"
                    codec == "wavpack" -> "wv"
                    else -> codec
                }
            val label =
                when {
                    atmos -> "Dolby Atmos"
                    dtsx -> "DTS:X"
                    profile.isNotBlank() -> profile
                    else -> codec.uppercase()
                }
            if (label.isNotBlank()) add(flag("audio/$key", label))
            // Atmos object audio does not specify a 5.1.4/7.1.4 playback layout.
            // Only use a channel icon when the reported layout matches its meaning.
            val layout =
                stream.channelLayout
                    ?.lowercase()
                    ?.replace("(side)", "")
                    ?.trim()
            val channelKey =
                when (layout) {
                    "mono", "1.0" -> "1"
                    "stereo", "2.0" -> "2"
                    "3.0" -> "3"
                    "4.0", "quad" -> "4"
                    "5.0" -> "5"
                    "5.1" -> "6"
                    "6.1" -> "7"
                    "7.1" -> "8"
                    else -> null
                }
            if (channelKey != null && stream.channels == channelKey.toInt()) {
                add(flag("channels/$channelKey", stream.channelLayout!!))
            } else if (!stream.channelLayout.isNullOrBlank()) {
                add(MediaFlag(null, stream.channelLayout!!))
            } else {
                stream.channels?.takeIf { it > 0 }?.let { add(MediaFlag(null, "$it ch")) }
            }
        }

    private fun aspectRatio(value: String?): Double? {
        val parts = value?.split(':', '/') ?: return null
        val ratio =
            if (parts.size == 2) {
                val a = parts[0].toDoubleOrNull() ?: return null
                val b = parts[1].toDoubleOrNull()?.takeIf { it > 0 } ?: return null
                a / b
            } else {
                value.toDoubleOrNull()
            }
        return ratio?.takeIf { it.isFinite() && it > 0 }
    }
}
