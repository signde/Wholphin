package com.github.damontecres.wholphin.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class HdmiAudioReconnectTest {
    private val context = mockk<Context>(relaxed = true)
    private val receiver = slot<BroadcastReceiver>()

    private fun plug(connected: Boolean) =
        Intent(AudioManager.ACTION_HDMI_AUDIO_PLUG)
            .putExtra(AudioManager.EXTRA_AUDIO_PLUG_STATE, if (connected) 1 else 0)

    private fun initialState(intent: Intent?) {
        every {
            context.registerReceiver(capture(receiver), any<IntentFilter>(), isNull(), isNull(), any<Int>())
        } returns intent
    }

    @Test
    fun alreadyConnectedDoesNotDelayPlayback() =
        runBlocking {
            initialState(plug(true))
            assertTrue(awaitHdmiAudioReconnect(context))
            verify(exactly = 1) { context.unregisterReceiver(receiver.captured) }
        }

    @Test
    fun waitsForReconnectAndIgnoresUnrelatedOrDisconnectedEvents() =
        runBlocking {
            initialState(plug(false))
            val result = async(start = CoroutineStart.UNDISPATCHED) { awaitHdmiAudioReconnect(context) }
            receiver.captured.onReceive(context, Intent("unrelated"))
            receiver.captured.onReceive(context, plug(false))
            assertFalse(result.isCompleted)
            receiver.captured.onReceive(context, plug(true))
            assertTrue(result.await())
            verify(exactly = 1) { context.unregisterReceiver(receiver.captured) }
        }

    @Test
    fun missingBroadcastDoesNotBlockPlayback() =
        runBlocking {
            initialState(null)
            assertTrue(awaitHdmiAudioReconnect(context))
            verify(exactly = 1) { context.unregisterReceiver(receiver.captured) }
        }

    @Test
    fun timeoutUnregistersReceiver() =
        runBlocking {
            initialState(plug(false))
            assertFalse(awaitHdmiAudioReconnect(context, timeoutMs = 1))
            verify(exactly = 1) { context.unregisterReceiver(receiver.captured) }
        }

    @Test
    fun cancellationUnregistersReceiver() =
        runBlocking {
            initialState(plug(false))
            val result = async(start = CoroutineStart.UNDISPATCHED) { awaitHdmiAudioReconnect(context) }
            result.cancel()
            result.join()
            verify(exactly = 1) { context.unregisterReceiver(receiver.captured) }
        }
}
