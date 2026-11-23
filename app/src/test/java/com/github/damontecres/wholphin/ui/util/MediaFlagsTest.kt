package com.github.damontecres.wholphin.ui.util

import com.github.damontecres.wholphin.preferences.AppPreference
import com.github.damontecres.wholphin.preferences.AppPreferences
import com.github.damontecres.wholphin.preferences.InterfacePreferences
import com.github.damontecres.wholphin.test.TestTracks
import org.jellyfin.sdk.model.api.AudioSpatialFormat
import org.jellyfin.sdk.model.api.MediaStream
import org.jellyfin.sdk.model.api.MediaStreamType
import org.jellyfin.sdk.model.api.VideoRangeType
import org.jellyfin.sdk.model.api.VideoType
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

    @Test fun sourceQualityUsesAf3TagsAndArtwork() {
        val cases =
            mapOf(
                "WEB-DL" to "web-dl",
                "webdl" to "web-dl",
                "WEBRip" to "web-dl",
                "web-rip" to "web-dl",
                "WebTV" to "webtv",
                "web-tv" to "webtv",
                "BluRay" to "bluray",
                "blu-ray" to "bluray",
                "BDRip" to "bluray",
                "BRRip" to "bluray",
                "REMUX" to "bluray",
                "BD25" to "bluray",
                "BD50" to "bluray",
                "BDMV" to "bluray",
                "HDDVD" to "hddvd",
                "HD-DVD" to "hddvd",
                "HD DVD" to "hddvd",
                "DVD" to "dvd",
                "DVDRip" to "dvd",
                "SDTV" to "sdtv",
                "SD-TV" to "sdtv",
                "PDTV" to "sdtv",
                "HDTV" to "hdtv",
                "DSR" to "hdtv",
            )
        for ((tag, artwork) in cases) {
            assertEquals(tag, "source/$artwork", MediaFlags.source(null, itemPath = "/Movies/Film [1080p][$tag].mkv")?.artwork)
        }
        assertEquals("source/dvd", MediaFlags.source(null, itemPath = "/Movies/Film/VIDEO_TS.IFO")?.artwork)
        assertEquals("source/dvd", MediaFlags.source(null, itemPath = "/Movies/Film.ISO")?.artwork)
    }

    @Test fun uhdSourceRequiresBothResolutionAndDiscEvidence() {
        for (resolution in listOf("4K", "2160P", "UHD", "UltraHD", "Ultra HD")) {
            for (disc in listOf("remux", "BluRay", "blu-ray", "BDRip", "BRRip", "BDMV", "BD66", "BD100")) {
                assertEquals(
                    "$resolution $disc",
                    "source/ultrahd",
                    MediaFlags.source(null, itemPath = "/Movies/Film [$resolution][$disc].mkv")?.artwork,
                )
            }
        }
        val croppedUhd = stream.copy(width = 3840, height = 1600)
        assertEquals("source/ultrahd", MediaFlags.source(null, croppedUhd, "/Movies/Film [Remux].mkv")?.artwork)
        assertEquals("source/web-dl", MediaFlags.source(null, croppedUhd, "/Movies/Film [WEB-DL].mkv")?.artwork)
        assertNull(MediaFlags.source(null, croppedUhd, "/Movies/Film [2160p][HEVC][Atmos].mkv"))
        assertNull(MediaFlags.source(null))
    }

    @Test fun sourcePriorityMatchesAf3() {
        assertEquals("source/ultrahd", MediaFlags.source(null, itemPath = "/4K Remux/Film [WEB-DL].mkv")?.artwork)
        assertEquals("source/webtv", MediaFlags.source(null, itemPath = "/Movies/Film [WebTV][WEB-DL].mkv")?.artwork)
        assertEquals("source/web-dl", MediaFlags.source(null, itemPath = "/Movies/Film [WEB-DL][REMUX].mkv")?.artwork)
        assertEquals("source/hddvd", MediaFlags.source(null, itemPath = "/Movies/Film [HD DVD][REMUX].mkv")?.artwork)
        assertEquals("source/sdtv", MediaFlags.source(null, itemPath = "/Movies/Film [PDTV][DSR].mkv")?.artwork)
    }

    @Test fun selectedSourcePathOverridesDefaultAndExplicitDiscTypesStillWork() {
        val source = TestTracks.Builder().buildMediaSourceInfo()
        assertEquals(
            "source/web-dl",
            MediaFlags.source(source.copy(path = "/Movies/Film [WEB-DL].mkv"), itemPath = "/Movies/Film [BluRay].mkv")?.artwork,
        )
        assertEquals("source/bluray", MediaFlags.source(source.copy(path = ""), itemPath = "/BluRay/Film.mkv")?.artwork)
        assertEquals("source/bluray", MediaFlags.source(source.copy(videoType = VideoType.BLU_RAY))?.artwork)
        assertEquals(
            "source/ultrahd",
            MediaFlags.source(source.copy(videoType = VideoType.BLU_RAY), stream.copy(width = 3840, height = 2160))?.artwork,
        )
        assertEquals("source/dvd", MediaFlags.source(source.copy(videoType = VideoType.DVD))?.artwork)
    }

}
