@file:OptIn(markerClass = [UnstableApi::class])

package com.github.damontecres.wholphin.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import androidx.annotation.OptIn
import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.audio.AudioSink
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowSystemClock
import java.nio.ByteBuffer
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class HdmiReconnectAudioSinkTest {
    private val context = mockk<Context>(relaxed = true)
    private val delegate = mockk<AudioSink>(relaxed = true)
    private val receiver = slot<BroadcastReceiver>()
    private val buffer = ByteBuffer.wrap(byteArrayOf(1, 2, 3, 4))

    private fun plug(connected: Boolean) =
        Intent(AudioManager.ACTION_HDMI_AUDIO_PLUG)
            .putExtra(AudioManager.EXTRA_AUDIO_PLUG_STATE, if (connected) 1 else 0)

    private fun create(connected: Boolean): HdmiReconnectAudioSink {
        every {
            context.registerReceiver(capture(receiver), any<IntentFilter>(), isNull(), isNull(), any<Int>())
        } returns plug(connected)
        every { delegate.handleBuffer(buffer, 1234L, 1) } returns true
        return HdmiReconnectAudioSink(delegate, context)
    }

    @Test
    fun retainsBufferAndTimestampUntilHdmiReconnects() {
        val sink = create(false)
        assertFalse(sink.handleBuffer(buffer, 1234L, 1))
        assertFalse(sink.handleBuffer(buffer, 1234L, 1))
        assertEquals(0, buffer.position())
        verify(exactly = 0) { delegate.handleBuffer(any(), any(), any()) }
        receiver.captured.onReceive(context, plug(true))
        assertTrue(sink.handleBuffer(buffer, 1234L, 1))
        verify(exactly = 1) { delegate.handleBuffer(buffer, 1234L, 1) }
        sink.release()
        verify(exactly = 1) { context.unregisterReceiver(receiver.captured) }
    }

    @Test
    fun connectedOutputIsNotDelayed() {
        val sink = create(true)
        assertTrue(sink.handleBuffer(buffer, 1234L, 1))
        sink.release()
    }

    @Test
    fun prolongedDisconnectReturnsControlToNormalAudioHandling() {
        val sink = create(false)
        assertFalse(sink.handleBuffer(buffer, 1234L, 1))
        ShadowSystemClock.advanceBy(Duration.ofSeconds(6))
        assertTrue(sink.handleBuffer(buffer, 1234L, 1))
        sink.release()
    }

    @Test
    fun retainsLosslessBufferWhileOldOutputCloses() {
        val sink = create(true)
        for (mime in listOf(MimeTypes.AUDIO_TRUEHD, MimeTypes.AUDIO_DTS_HD)) {
            sink.flush()
            val failure =
                AudioSink.InitializationException(
                    "Output still reserved",
                    0,
                    Format.Builder().setSampleMimeType(mime).build(),
                    false,
                    null,
                )
            every { delegate.handleBuffer(buffer, 1234L, 1) } throws failure
            assertFalse(sink.handleBuffer(buffer, 1234L, 1))
            ShadowSystemClock.advanceBy(Duration.ofSeconds(2))
            assertFalse(sink.handleBuffer(buffer, 1234L, 1))
            assertEquals(0, buffer.position())
            every { delegate.handleBuffer(buffer, 1234L, 1) } returns true
            assertTrue(sink.handleBuffer(buffer, 1234L, 1))
        }
        sink.release()
    }

    @Test
    fun persistentLosslessInitializationFailureIsReported() {
        val sink = create(true)
        val failure =
            AudioSink.InitializationException(
                "Output unavailable",
                0,
                Format.Builder().setSampleMimeType(MimeTypes.AUDIO_TRUEHD).build(),
                false,
                null,
            )
        every { delegate.handleBuffer(buffer, 1234L, 1) } throws failure
        assertFalse(sink.handleBuffer(buffer, 1234L, 1))
        ShadowSystemClock.advanceBy(Duration.ofSeconds(5))
        org.junit.Assert.assertThrows(AudioSink.InitializationException::class.java) { sink.handleBuffer(buffer, 1234L, 1) }
        sink.release()
    }

    @Test
    fun otherCodecFailuresAreNotRetried() {
        val sink = create(true)
        val failure =
            AudioSink.InitializationException(
                "Output unavailable",
                0,
                Format.Builder().setSampleMimeType(MimeTypes.AUDIO_E_AC3).build(),
                false,
                null,
            )
        every { delegate.handleBuffer(buffer, 1234L, 1) } throws failure
        org.junit.Assert.assertThrows(AudioSink.InitializationException::class.java) { sink.handleBuffer(buffer, 1234L, 1) }
        sink.release()
    }
}
