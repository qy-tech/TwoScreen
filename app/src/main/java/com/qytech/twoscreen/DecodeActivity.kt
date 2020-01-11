package com.qytech.twoscreen

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.MediaCodec
import android.media.MediaCodec.BufferInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.os.Bundle
import android.os.Environment
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import timber.log.Timber
import java.nio.ByteBuffer


/**
 * Created by Jax on 2020-01-10.
 * Description :
 * Version : V1.0.0
 */
class DecodeActivity : Activity(), SurfaceHolder.Callback {
    private var mPlayer: PlayerThread? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sv = SurfaceView(this)
        sv.holder.addCallback(this)
        setContentView(sv)
    }


    override fun onDestroy() {
        super.onDestroy()
    }

    override fun surfaceCreated(holder: SurfaceHolder?) {}
    override fun surfaceChanged(
        holder: SurfaceHolder,
        format: Int,
        width: Int,
        height: Int
    ) {
        if (mPlayer == null) {
            mPlayer = PlayerThread(holder.surface)
            mPlayer?.start()
        }
    }

    override fun surfaceDestroyed(holder: SurfaceHolder?) {
        if (mPlayer != null) {
            mPlayer?.interrupt()
        }
    }

    private inner class PlayerThread(private val surface: Surface) : Thread() {
        private var extractor: MediaExtractor? = null
        private var decoder: MediaCodec? = null
        override fun run() {
            extractor = MediaExtractor()
            extractor?.setDataSource(SAMPLE)
            for (i in 0 until extractor!!.trackCount) {
                val format = extractor!!.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME)
                if (mime.startsWith("video/")) {
                    extractor!!.selectTrack(i)
                    decoder = MediaCodec.createDecoderByType(mime)
//                    decoder?.setCallback(object : MediaCodec.Callback() {
//                        override fun onOutputBufferAvailable(
//                            codec: MediaCodec,
//                            index: Int,
//                            info: BufferInfo
//                        ) {
//                            Timber.d("onOutputBufferAvailable date: 2020-01-10 ")
//                        }
//
//                        override fun onInputBufferAvailable(codec: MediaCodec, index: Int) {
//                            Timber.d("onInputBufferAvailable date: 2020-01-10 ")
//                        }
//
//                        override fun onOutputFormatChanged(codec: MediaCodec, format: MediaFormat) {
//                            Timber.d("onOutputFormatChanged date: 2020-01-10 ")
//                        }
//
//                        override fun onError(codec: MediaCodec, e: MediaCodec.CodecException) {
//                            Timber.d("onError date: 2020-01-10 ")
//                        }
//                    })
                    decoder?.configure(format, surface, null, 0)
                    break
                }
            }
            if (decoder == null) {
                Timber.tag("DecodeActivity").e("Can't find video info!")
                return
            }
            decoder!!.start()

            val inputBuffers: Array<ByteBuffer> = decoder!!.inputBuffers
            var outputBuffers: Array<ByteBuffer> = decoder!!.outputBuffers
            val info = BufferInfo()
            var isEOS = false
            val startMs = System.currentTimeMillis()
            while (!interrupted()) {
                if (!isEOS) {
                    val inIndex = decoder!!.dequeueInputBuffer(10000)
                    if (inIndex >= 0) {
                        val buffer: ByteBuffer = inputBuffers[inIndex]
                        val sampleSize = extractor!!.readSampleData(buffer, 0)
                        if (sampleSize < 0) {
                            // We shouldn't stop the playback at this point, just pass the EOS
                            // flag to decoder, we will get it again from the
                            // dequeueOutputBuffer
                            Timber.tag("DecodeActivity").d("InputBuffer BUFFER_FLAG_END_OF_STREAM")
                            decoder!!.queueInputBuffer(
                                inIndex,
                                0,
                                0,
                                0,
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM
                            )
                            isEOS = true
                        } else {
                            decoder!!.queueInputBuffer(
                                inIndex,
                                0,
                                sampleSize,
                                extractor!!.sampleTime,
                                0
                            )
                            extractor!!.advance()
                            Timber.d("run queueInputBuffer date: 2020-01-10 ")
                        }
                    }
                }
                when (val outIndex = decoder!!.dequeueOutputBuffer(info, 10000)) {
                    MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED -> {
                        Timber.tag("DecodeActivity").d("INFO_OUTPUT_BUFFERS_CHANGED")
                        outputBuffers = decoder!!.outputBuffers
                    }
                    MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> Timber.tag("DecodeActivity").d(
                        "New format %s",
                        decoder!!.outputFormat
                    )
                    MediaCodec.INFO_TRY_AGAIN_LATER -> Timber.tag("DecodeActivity").d("dequeueOutputBuffer timed out!")
                    else -> {
                        val buffer: ByteBuffer = outputBuffers[outIndex]
                        Timber.tag("DecodeActivity")
                            .d("We can't use this buffer but render it due to the API limit, $buffer")
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
                        decoder?.releaseOutputBuffer(outIndex, true)
                    }
                }
                // All decoded frames have been rendered, we can stop playing now
                if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                    Timber.tag("DecodeActivity").d("OutputBuffer BUFFER_FLAG_END_OF_STREAM")
                    break
                }
            }
            decoder!!.stop()
            decoder!!.release()
            extractor!!.release()
        }

    }

    companion object {
        private val SAMPLE: String =
            Environment.getExternalStorageDirectory().toString() + "/big-buck-bunny-1080p-60fps-30s.mp4"

        fun start(context: Context) {
            val intent = Intent(context, DecodeActivity::class.java)
            context.startActivity(intent)
        }
    }
}