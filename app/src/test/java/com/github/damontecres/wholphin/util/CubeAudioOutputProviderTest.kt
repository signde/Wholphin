@file:OptIn(markerClass = [UnstableApi::class])

package com.github.damontecres.wholphin.util

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.audio.AudioOutput
import androidx.media3.exoplayer.audio.AudioOutputProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class CubeAudioOutputProviderTest {
    @Test
    fun losslessReplacementRequestsFreshSessionWithoutChangingAudioFormat() {
        val delegate = mockk<AudioOutputProvider>()
        val output = mockk<AudioOutput>()
        val config =
            AudioOutputProvider.OutputConfig
                .Builder()
                .setEncoding(C.ENCODING_DOLBY_TRUEHD)
                .setSampleRate(96000)
                .setChannelMask(252)
                .setBufferSize(768000)
                .setAudioSessionId(105)
                .build()
        val captured = slot<AudioOutputProvider.OutputConfig>()
        every { delegate.getAudioOutput(capture(captured)) } returns output
        val provider = CubeAudioOutputProvider(delegate)
        for (encoding in listOf(C.ENCODING_DOLBY_TRUEHD, C.ENCODING_DTS_HD)) {
            assertSame(output, provider.getAudioOutput(config.buildUpon().setEncoding(encoding).build()))
            assertEquals(C.AUDIO_SESSION_ID_UNSET, captured.captured.audioSessionId)
            assertEquals(96000, captured.captured.sampleRate)
            assertEquals(768000, captured.captured.bufferSize)
            assertEquals(252, captured.captured.channelMask)
        }
    }

    @Test
    fun otherCodecsAndOffloadAndTunnelingKeepSession() {
        val delegate = mockk<AudioOutputProvider>()
        val output = mockk<AudioOutput>()
        val captured = slot<AudioOutputProvider.OutputConfig>()
        every { delegate.getAudioOutput(capture(captured)) } returns output
        val provider = CubeAudioOutputProvider(delegate)
        val base =
            AudioOutputProvider.OutputConfig
                .Builder()
                .setEncoding(C.ENCODING_DOLBY_TRUEHD)
                .setAudioSessionId(105)
        for (config in listOf(
            base.setIsOffload(true).build(),
            base.setIsOffload(false).setIsTunneling(true).build(),
            base.setIsTunneling(false).setEncoding(C.ENCODING_DTS).build(),
            base.setEncoding(C.ENCODING_E_AC3).build(),
        )) {
            provider.getAudioOutput(config)
            assertSame(config, captured.captured)
        }
    }
}
