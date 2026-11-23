@file:OptIn(markerClass = [UnstableApi::class])

package com.github.damontecres.wholphin.util

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.audio.AudioOutput
import androidx.media3.exoplayer.audio.AudioOutputProvider

internal class CubeAudioOutputProvider(
    private val delegate: AudioOutputProvider,
) : AudioOutputProvider by delegate {
    override fun getAudioOutput(config: AudioOutputProvider.OutputConfig): AudioOutput {
        // A new lossless passthrough session must not inherit the old direct output's counter
        // while Fire OS is still processing its asynchronous flush.
        val freshConfig =
            if ((config.encoding == C.ENCODING_DOLBY_TRUEHD || config.encoding == C.ENCODING_DTS_HD) && !config.isOffload &&
                !config.isTunneling
            ) {
                config.buildUpon().setAudioSessionId(C.AUDIO_SESSION_ID_UNSET).build()
            } else {
                config
            }
        return delegate.getAudioOutput(freshConfig)
    }
}
