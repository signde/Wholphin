@file:OptIn(markerClass = [UnstableApi::class])

package com.github.damontecres.wholphin.util

import android.media.MediaCodec
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.mediacodec.MediaCodecAdapter
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.Assert.assertThrows
import org.junit.Test
import java.nio.ByteBuffer

class FireTvFelCodecAdapterTest {
    private val codec = mockk<MediaCodecAdapter>(relaxed = true)
    private val session = mockk<FelSession>(relaxed = true)
    private val data = ByteBuffer.allocateDirect(64)
    private val adapter = FireTvFelCodecAdapter(codec, session)

    @Test
    fun commitsExactTimestampOnlyAfterBaseLayerWasQueued() {
        every { codec.getInputBuffer(3) } returns data
        every { session.prepare(data, 4, 48) } returns 31
        adapter.queueInputBuffer(3, 4, 48, 1000000123456, 0)
        verifyOrder {
            session.prepare(data, 4, 48)
            codec.queueInputBuffer(3, 4, 31, 1000000123456, 0)
            session.commit(1000000123456)
        }
    }

    @Test
    fun failedBaseLayerQueueDoesNotCommitEnhancementLayer() {
        every { codec.getInputBuffer(3) } returns data
        every { session.prepare(data, 0, 48) } returns 31
        every { codec.queueInputBuffer(any(), any(), any(), any(), any()) } throws IllegalStateException()
        assertThrows(IllegalStateException::class.java) {
            adapter.queueInputBuffer(3, 0, 48, 9000, 0)
        }
        verify(exactly = 0) { session.commit(any()) }
        verify(exactly = 1) { session.flush() }
    }

    @Test
    fun codecConfigurationAndEmptyEosDoNotEnterFelPipeline() {
        adapter.queueInputBuffer(3, 0, 48, 0, MediaCodec.BUFFER_FLAG_CODEC_CONFIG)
        adapter.queueInputBuffer(3, 0, 0, 9000, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
        verify(exactly = 0) { session.prepare(any(), any(), any()) }
        verify(exactly = 0) { session.commit(any()) }
        verify { codec.queueInputBuffer(3, 0, 0, 9000, MediaCodec.BUFFER_FLAG_END_OF_STREAM) }
    }

    @Test
    fun seekFlushesBothDecodersInOrder() {
        adapter.flush()
        verifyOrder {
            codec.flush()
            session.flush()
        }
    }

    @Test
    fun closesFelSessionEvenIfBaseDecoderReleaseFails() {
        every { codec.release() } throws IllegalStateException()
        assertThrows(IllegalStateException::class.java) { adapter.release() }
        verify(exactly = 1) { session.close() }
    }
}
