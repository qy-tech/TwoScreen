package com.qytech.twoscreen

import android.content.res.AssetFileDescriptor
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.view.Surface
import timber.log.Timber
import java.nio.ByteBuffer

/**
 * Created by Jax on 2020-01-11.
 * Description :
 * Version : V1.0.0
 */
class PlayerThread(private val surface: Surface?) : Thread() {

    constructor(surface: Surface?, assetFileDescriptor: AssetFileDescriptor) : this(surface) {
        this.videoFd = assetFileDescriptor
    }

    constructor(surface: Surface?, path: String) : this(surface) {
        this.videoPath = path
    }

    interface OnCompletionListener {
        fun onCompletion()
    }

    interface OnFpsChangeListener {
        fun onFpsChange(fps: Int)
    }

    private var completionListener: OnCompletionListener? = null
    private var fpsChangeListener: OnFpsChangeListener? = null

    private var extractor: MediaExtractor? = null
    private var decoder: MediaCodec? = null
    private var videoFd: AssetFileDescriptor? = null
    private var videoPath: String? = null
    fun setOnCompletionListener(listener: OnCompletionListener) {
        this.completionListener = listener
    }

    fun setOnFpsChangeListener(listener: OnFpsChangeListener) {
        this.fpsChangeListener = listener
    }

    override fun run() {
        if (videoPath?.isBlank() == true && videoFd == null) {
            return
        }
        try {
            extractor = MediaExtractor()
            if (videoFd != null) {
                videoFd?.let {
                    extractor?.setDataSource(it.fileDescriptor, it.startOffset, it.length)
                }
            } else if (videoPath?.isNotBlank() == true) {
                videoPath?.let {
                    extractor?.setDataSource(it)
                }
            }
            decoder = createDecoder(surface, extractor)
            decoder?.let { decoder ->
                decoder.start()
                val inputBuffers: Array<ByteBuffer> = decoder.inputBuffers
                var outputBuffers: Array<ByteBuffer> = decoder.outputBuffers
                val info = MediaCodec.BufferInfo()
                var isEOS = false
                val startMs = System.currentTimeMillis()
                var lastTime = 0L
                var fps = 0
                while (!interrupted()) {
                    if (!isEOS) {
                        val inIndex = decoder.dequeueInputBuffer(10000)
                        if (inIndex >= 0) {
                            val buffer: ByteBuffer = inputBuffers[inIndex]
                            val sampleSize = extractor?.readSampleData(buffer, 0) ?: 0
                            if (sampleSize < 0) {
                                // We shouldn't stop the playback at this point, just pass the EOS
                                // flag to decoder, we will get it again from the
                                // dequeueOutputBuffer
                                Timber
                                    .d("InputBuffer BUFFER_FLAG_END_OF_STREAM")
                                decoder.queueInputBuffer(
                                    inIndex,
                                    0,
                                    0,
                                    0,
                                    MediaCodec.BUFFER_FLAG_END_OF_STREAM
                                )
                                isEOS = true
                            } else {
                                decoder.queueInputBuffer(
                                    inIndex,
                                    0,
                                    sampleSize,
                                    extractor?.sampleTime ?: 0,
                                    0
                                )
                                extractor?.advance()
                                if (System.currentTimeMillis() - lastTime > 1000L) {
                                    if (!isInterrupted) {
                                        fpsChangeListener?.onFpsChange(fps)
                                    }
                                    lastTime = System.currentTimeMillis()
                                    fps = 0
                                } else {
                                    fps++
                                }
                            }
                        }
                    }
                    when (val outIndex = decoder.dequeueOutputBuffer(info, 10000)) {
                        MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED -> {
                            Timber.d("INFO_OUTPUT_BUFFERS_CHANGED")
                            outputBuffers = decoder.outputBuffers
                        }

                        MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                            Timber.d("New format %s", decoder.outputFormat)
                        }

                        MediaCodec.INFO_TRY_AGAIN_LATER -> {
                            Timber.d("dequeueOutputBuffer timed out!")
                        }

                        else -> {
                            val buffer: ByteBuffer = outputBuffers[outIndex]

                            Timber.d("We can't use this buffer but render it due to the API limit, $buffer")
                            // We use a very simple clock to keep the video FPS, or the video
                            // playback will be too fast
                            while (info.presentationTimeUs / 1000 > System.currentTimeMillis() - startMs) {
                                try {
                                    sleep(10)
                                } catch (e: InterruptedException) {
                                    e.printStackTrace()
                                    break
                                }
                            }
                            decoder.releaseOutputBuffer(outIndex, true)
                        }
                    }
                    // All decoded frames have been rendered, we can stop playing now
                    if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                        Timber.d("OutputBuffer BUFFER_FLAG_END_OF_STREAM")
                        break
                    }
                }
            }
            release()
            if (!isInterrupted) {
                completionListener?.onCompletion()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun createDecoder(surface: Surface?, mediaExtractor: MediaExtractor?): MediaCodec? {
        mediaExtractor?.let { extractor ->
            (0 until extractor.trackCount).map { index ->
                val format = extractor.getTrackFormat(index)
                val mime = format.getString(MediaFormat.KEY_MIME)
                if (mime?.startsWith("video/") == true) {
                    extractor.selectTrack(index)
                    return MediaCodec.createDecoderByType(mime).apply {
                        configure(format, surface, null, 0)
                    }
                }
            }
        }
        return null
    }

    override fun interrupt() {
        super.interrupt()
        release()
    }

    private fun release() {
        try {
            decoder?.stop()
            decoder?.release()
            extractor?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


}