package com.github.damontecres.wholphin.ui.util

import com.github.damontecres.wholphin.preferences.AppPreference
import com.github.damontecres.wholphin.preferences.AppPreferences
import com.github.damontecres.wholphin.preferences.InterfacePreferences
import org.jellyfin.sdk.model.api.AudioSpatialFormat
import org.jellyfin.sdk.model.api.MediaStream
import org.jellyfin.sdk.model.api.MediaStreamType
import org.jellyfin.sdk.model.api.VideoRangeType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaFlagsTest {
    private val stream =
        MediaStream(
            index = 0,
            type = MediaStreamType.VIDEO,
            isInterlaced = false,
            isDefault = false,
            isForced = false,
            isHearingImpaired = false,
            isOriginal = false,
            isExternal = false,
            isTextSubtitleStream = false,
            supportsExternalStream = false,
        )

    private fun video(
        profile: Int? = 7,
        comment: String? = null,
    ) = stream.copy(index = 0, type = MediaStreamType.VIDEO, dvProfile = profile, videoRangeType = VideoRangeType.DOVI, comment = comment)

    @Test fun felRequiresMatchingProbeEvidence() {
        assertEquals("hdr/dolbyvision_fel", MediaFlags.video(video(comment = "[hdrprobe-dv:v1;p=7;el=FEL]")).first().artwork)
        assertEquals("hdr/dolbyvision_mel", MediaFlags.video(video(comment = "[hdrprobe-dv:v1;p=7;el=MEL]")).first().artwork)
        assertEquals("hdr/dolbyvision", MediaFlags.video(video()).first().artwork)
        assertEquals("hdr/dolbyvision", MediaFlags.video(video(profile = 8, comment = "[hdrprobe-dv:v1;p=7;el=FEL]")).first().artwork)
    }

    @Test fun spatialAudioDoesNotInventHeightChannels() {
        val flags =
            MediaFlags.audio(
                stream.copy(
                    index = 1,
                    type = MediaStreamType.AUDIO,
                    codec = "truehd",
                    channels = 8,
                    channelLayout = "7.1",
                    audioSpatialFormat = AudioSpatialFormat.DOLBY_ATMOS,
                ),
            )
        assertEquals(listOf("audio/atmos", "channels/8"), flags.map { it.artwork })
        val unknownLayout = MediaFlags.audio(stream.copy(index = 1, type = MediaStreamType.AUDIO, codec = "truehd", channels = 8))
        assertNull(unknownLayout.last().artwork)
        assertEquals("8 ch", unknownLayout.last().label)
    }

    @Test fun croppedUhdAndRationalAspectAreRecognized() {
        val flags =
            MediaFlags.video(
                stream.copy(index = 0, type = MediaStreamType.VIDEO, width = 3840, height = 1600, aspectRatio = "12:5", codec = "hevc"),
            )
        assertEquals(listOf("resolution/2160", "aspectratio/2.40", "source/hevc"), flags.map { it.artwork })
    }

    @Test fun unknownFormatsKeepText() {
        val flags = MediaFlags.video(stream.copy(index = 0, type = MediaStreamType.VIDEO, codec = "futurecodec", aspectRatio = "1.92"))
        assertEquals(listOf("1.92", "FUTURECODEC"), flags.map { it.label })
        assertTrue(flags.all { it.artwork == null })
    }

    @Test fun dtsProfilesSelectMatchingArtwork() {
        assertEquals(
            "audio/dtshd_ma",
            MediaFlags.audio(stream.copy(index = 1, type = MediaStreamType.AUDIO, codec = "dts", profile = "DTS-HD MA")).first().artwork,
        )
        assertEquals(
            "audio/dtsx",
            MediaFlags
                .audio(
                    stream.copy(index = 1, type = MediaStreamType.AUDIO, codec = "dts", audioSpatialFormat = AudioSpatialFormat.DTSX),
                ).first()
                .artwork,
        )
    }

    @Test fun existingInstallsDefaultOnAndExplicitOffPersists() {
        val original = AppPreferences.getDefaultInstance()
        assertTrue(AppPreference.ColoredMediaIcons.getter(original))
        val disabled = original.toBuilder().setInterfacePreferences(InterfacePreferences.newBuilder().setColoredMediaIcons(false)).build()
        assertFalse(AppPreference.ColoredMediaIcons.getter(disabled))
        assertFalse(InterfaceCustomization(AppPreferences.parseFrom(disabled.toByteArray())).coloredMediaIcons)
    }
}
