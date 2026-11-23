@file:OptIn(markerClass = [UnstableApi::class])

package com.github.damontecres.wholphin.util

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.audio.DefaultAudioSink

/** Keep DTS-HD tracks within Fire OS's audio-client allocation budget. */
internal class CubeAudioTrackBufferSizeProvider(
    private val delegate: DefaultAudioSink.AudioTrackBufferSizeProvider = DefaultAudioSink.AudioTrackBufferSizeProvider.DEFAULT,
) : DefaultAudioSink.AudioTrackBufferSizeProvider {
    override fun getBufferSizeInBytes(
        minBufferSizeInBytes: Int,
        encoding: Int,
        outputMode: Int,
        pcmFrameSize: Int,
        sampleRate: Int,
        bitrate: Int,
        maxAudioTrackPlaybackSpeed: Double,
    ): Int {
        val requested =
            delegate.getBufferSizeInBytes(
                minBufferSizeInBytes,
                encoding,
                outputMode,
                pcmFrameSize,
                sampleRate,
                bitrate,
                maxAudioTrackPlaybackSpeed,
            )
        if (encoding != C.ENCODING_DTS_HD || outputMode != DefaultAudioSink.OUTPUT_MODE_PASSTHROUGH) return requested
        // Unknown-bitrate DTS-HD requests can exceed 2 MiB even after Media3's
        // allocation retry. Fire OS rounds them to 4 MiB plus its control block.
        // Leave room for track replacement during seeks; keep the platform minimum.
        val limited = requested.coerceAtMost(1024 * 1024).coerceAtLeast(minBufferSizeInBytes)
        return ((limited + pcmFrameSize - 1) / pcmFrameSize) * pcmFrameSize
    }
}
