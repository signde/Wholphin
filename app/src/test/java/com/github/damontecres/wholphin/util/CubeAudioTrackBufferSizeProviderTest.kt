@file:OptIn(markerClass = [UnstableApi::class])

package com.github.damontecres.wholphin.util

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.audio.DefaultAudioSink
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CubeAudioTrackBufferSizeProviderTest {
    private val provider = CubeAudioTrackBufferSizeProvider()

    private fun size(
        encoding: Int,
        minimum: Int = 4096,
        mode: Int = DefaultAudioSink.OUTPUT_MODE_PASSTHROUGH,
        bitrate: Int = Format.NO_VALUE,
    ) = provider.getBufferSizeInBytes(minimum, encoding, mode, 1, 48000, bitrate, 1.0)

    @Test
    fun unknownBitrateDtsHdFitsTrackReplacementBudget() {
        val upstream =
            DefaultAudioSink.AudioTrackBufferSizeProvider.DEFAULT.getBufferSizeInBytes(
                4096,
                C.ENCODING_DTS_HD,
                DefaultAudioSink.OUTPUT_MODE_PASSTHROUGH,
                1,
                48000,
                Format.NO_VALUE,
                1.0,
            )
        assertTrue(upstream > 2 * 1024 * 1024)
        assertEquals(1024 * 1024, size(C.ENCODING_DTS_HD))
    }

    @Test
    fun platformMinimumIsRespected() {
        assertEquals(1500000, size(C.ENCODING_DTS_HD, minimum = 1500000))
    }

    @Test
    fun smallerKnownBitrateBufferIsUnchanged() {
        assertEquals(32000, size(C.ENCODING_DTS_HD, bitrate = 256000))
    }

    @Test
    fun unrelatedEncodingsAndOffloadKeepUpstreamSizing() {
        for (encoding in listOf(C.ENCODING_DOLBY_TRUEHD, C.ENCODING_E_AC3, C.ENCODING_AC3, C.ENCODING_DTS, C.ENCODING_PCM_16BIT)) {
            val mode = if (encoding == C.ENCODING_PCM_16BIT) DefaultAudioSink.OUTPUT_MODE_PCM else DefaultAudioSink.OUTPUT_MODE_PASSTHROUGH
            val expected =
                DefaultAudioSink.AudioTrackBufferSizeProvider.DEFAULT.getBufferSizeInBytes(
                    4096,
                    encoding,
                    mode,
                    1,
                    48000,
                    Format.NO_VALUE,
                    1.0,
                )
            assertEquals(expected, size(encoding, mode = mode))
        }
        val offload = DefaultAudioSink.OUTPUT_MODE_OFFLOAD
        assertEquals(
            DefaultAudioSink.AudioTrackBufferSizeProvider.DEFAULT.getBufferSizeInBytes(
                4096,
                C.ENCODING_DTS_HD,
                offload,
                1,
                48000,
                Format.NO_VALUE,
                1.0,
            ),
            size(C.ENCODING_DTS_HD, mode = offload),
        )
    }
}
