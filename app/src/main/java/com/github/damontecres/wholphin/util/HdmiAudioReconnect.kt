package com.github.damontecres.wholphin.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull

/** Wait for HDMI audio to reconnect before the player chooses passthrough or decoding. */
internal suspend fun awaitHdmiAudioReconnect(
    context: Context,
    timeoutMs: Long = 5000,
): Boolean {
    val connected = CompletableDeferred<Unit>()
    val receiver =
        object : BroadcastReceiver() {
            override fun onReceive(
                context: Context?,
                intent: Intent?,
            ) {
                if (intent?.action == AudioManager.ACTION_HDMI_AUDIO_PLUG &&
                    intent.getIntExtra(AudioManager.EXTRA_AUDIO_PLUG_STATE, 0) == 1
                ) {
                    connected.complete(Unit)
                }
            }
        }
    val sticky =
        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(AudioManager.ACTION_HDMI_AUDIO_PLUG),
            ContextCompat.RECEIVER_EXPORTED,
        )
    try {
        // Register first so reconnect cannot be lost between reading state and subscribing.
        if (sticky == null) return true
        receiver.onReceive(context, sticky)
        return withTimeoutOrNull(timeoutMs) {
            connected.await()
            true
        } ?: false
    } finally {
        context.unregisterReceiver(receiver)
    }
}
