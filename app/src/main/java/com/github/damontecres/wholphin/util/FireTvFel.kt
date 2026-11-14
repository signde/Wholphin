@file:OptIn(markerClass = [UnstableApi::class])

package com.github.damontecres.wholphin.util

import android.media.MediaCodec
import android.os.Build
import androidx.annotation.OptIn
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DecoderCounters
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.mediacodec.ForwardingMediaCodecAdapter
import androidx.media3.exoplayer.mediacodec.MediaCodecAdapter
import timber.log.Timber
import java.nio.ByteBuffer

/** Hardware Profile 7 processing is opt-in through the module's session interface. */
object FireTvFel {
    private val loaded =
        Build.MODEL == "AFTGAZL" &&
            runCatching {
                System.loadLibrary("wholphinfel")
                true
            }.getOrDefault(false)

    fun available(): Boolean = loaded && nativeAvailable()

    external fun nativeAvailable(): Boolean

    external fun nativeCreate(
        width: Int,
        height: Int,
    ): Long

    external fun nativePrepare(
        handle: Long,
        data: ByteBuffer,
        offset: Int,
        size: Int,
    ): Int

    external fun nativeCommit(
        handle: Long,
        pts: Long,
    )

    external fun nativeFlush(handle: Long)

    external fun nativeClose(handle: Long)
}

class FireTvFelCodecAdapterFactory(
    private val delegate: MediaCodecAdapter.Factory,
) : MediaCodecAdapter.Factory {
    override fun createAdapter(configuration: MediaCodecAdapter.Configuration): MediaCodecAdapter {
        val p7 = DolbyVisionProfile7Conversion.isProfile7(configuration.format)
        if (!p7 || configuration.format.drmInitData != null || !FireTvFel.available()) {
            return delegate.createAdapter(configuration)
        }
        check(configuration.codecInfo.mimeType == MimeTypes.VIDEO_DOLBY_VISION) {
            "Native Profile 7 requires the Dolby Vision decoder"
        }
        Timber.i("Fire TV native P7 decoder: %s", configuration.codecInfo.name)
        return FireTvFelCodecAdapter(
            delegate.createAdapter(configuration),
            NativeFelSession(configuration.format.width, configuration.format.height),
        )
    }
}

internal interface FelSession {
    fun prepare(
        data: ByteBuffer,
        offset: Int,
        size: Int,
    ): Int

    fun commit(pts: Long)

    fun flush()

    fun close()
}

private class NativeFelSession(
    width: Int,
    height: Int,
) : FelSession {
    private var handle = FireTvFel.nativeCreate(width, height)

    override fun prepare(
        data: ByteBuffer,
        offset: Int,
        size: Int,
    ): Int {
        check(handle != 0L)
        return FireTvFel.nativePrepare(handle, data, offset, size)
    }

    override fun commit(pts: Long) = FireTvFel.nativeCommit(handle, pts)

    override fun flush() = FireTvFel.nativeFlush(handle)

    override fun close() {
        if (handle != 0L) FireTvFel.nativeClose(handle)
        handle = 0
    }
}

internal class FireTvFelCodecAdapter(
    delegate: MediaCodecAdapter,
    private val fel: FelSession,
) : ForwardingMediaCodecAdapter(delegate) {
    @Synchronized
    override fun queueInputBuffer(
        index: Int,
        offset: Int,
        size: Int,
        presentationTimeUs: Long,
        flags: Int,
    ) {
        val sample = size > 0 && flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG == 0
        val outputSize =
            if (sample) fel.prepare(checkNotNull(getInputBuffer(index)), offset, size) else size
        try {
            super.queueInputBuffer(index, offset, outputSize, presentationTimeUs, flags)
            // BL/EL pairing must use the exact timestamp submitted to MediaCodec.
            if (sample) fel.commit(presentationTimeUs)
        } catch (failure: Exception) {
            fel.flush()
            throw failure
        }
    }

    @Synchronized
    override fun flush() {
        super.flush()
        fel.flush()
    }

    @Synchronized
    override fun release() {
        try {
            super.release()
        } finally {
            fel.close()
        }
    }
}

/** Decoder summary for unattended development testing; no media URLs or account details. */
class FireTvFelAnalyticsListener : AnalyticsListener {
    override fun onVideoDisabled(
        eventTime: AnalyticsListener.EventTime,
        decoderCounters: DecoderCounters,
    ) {
        decoderCounters.ensureUpdated()
        Timber.tag("WholphinFEL").i(
            "Video totals: rendered=%d dropped=%d skipped=%d inputDropped=%d",
            decoderCounters.renderedOutputBufferCount,
            decoderCounters.droppedBufferCount,
            decoderCounters.skippedOutputBufferCount,
            decoderCounters.droppedInputBufferCount,
        )
    }

    override fun onAudioUnderrun(
        eventTime: AnalyticsListener.EventTime,
        bufferSize: Int,
        bufferSizeMs: Long,
        elapsedSinceLastFeedMs: Long,
    ) {
        Timber.tag("WholphinFEL").w(
            "Audio underrun: bufferMs=%d lastFeedMs=%d",
            bufferSizeMs,
            elapsedSinceLastFeedMs,
        )
    }
}
