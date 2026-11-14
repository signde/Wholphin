@file:OptIn(markerClass = [UnstableApi::class])

package com.github.damontecres.wholphin.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.SystemClock
import androidx.annotation.OptIn
import androidx.core.content.ContextCompat
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.ForwardingAudioSink
import timber.log.Timber
import java.nio.ByteBuffer

/** Retain queued audio while Fire OS reconnects HDMI during decoder startup. */
internal class HdmiReconnectAudioSink(
    sink: AudioSink,
    private val context: Context,
) : ForwardingAudioSink(sink) {
    @Volatile private var connected = true
    private var waitingSince = -1L
    private var registered = true
    private val receiver =
        object : BroadcastReceiver() {
            override fun onReceive(
                context: Context?,
                intent: Intent?,
            ) {
                if (intent?.action == AudioManager.ACTION_HDMI_AUDIO_PLUG) {
                    connected = intent.getIntExtra(AudioManager.EXTRA_AUDIO_PLUG_STATE, 0) == 1
                }
            }
        }

    init {
        val sticky =
            ContextCompat.registerReceiver(
                context,
                receiver,
                IntentFilter(AudioManager.ACTION_HDMI_AUDIO_PLUG),
                ContextCompat.RECEIVER_EXPORTED,
            )
        if (sticky != null) receiver.onReceive(context, sticky)
    }

    override fun handleBuffer(
        buffer: ByteBuffer,
        presentationTimeUs: Long,
        encodedAccessUnitCount: Int,
    ): Boolean {
        if (!connected) {
            val now = SystemClock.elapsedRealtime()
            if (waitingSince == -1L) {
                waitingSince = now
                Timber.i("Holding audio while HDMI reconnects")
            }
            if (now - waitingSince < 5000) return false
        } else if (waitingSince != -1L) {
            Timber.i("HDMI reconnected; submitting queued audio")
            waitingSince = -1L
        }
        return super.handleBuffer(buffer, presentationTimeUs, encodedAccessUnitCount)
    }

    override fun reset() {
        waitingSince = -1L
        super.reset()
    }

    override fun release() {
        try {
            super.release()
        } finally {
            if (registered) {
                context.unregisterReceiver(receiver)
                registered = false
            }
        }
    }
}
