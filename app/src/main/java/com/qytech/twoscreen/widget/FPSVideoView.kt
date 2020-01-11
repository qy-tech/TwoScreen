package com.qytech.twoscreen.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.util.AttributeSet
import android.widget.VideoView
import com.qytech.twoscreen.util.CpuInfoReader
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.*
import kotlin.math.ceil

/**
 * Created by Jax on 2020-01-08.
 * Description :
 * Version : V1.0.0
 */
class FpsVideoView(context: Context, attributeSet: AttributeSet? = null) :
    VideoView(context, attributeSet) {

    var fps = 0
        private set
    private var cpuInfo = arrayOfNulls<String>(2)

    var showCpuInfo = true

    private var updateCpuUsageJob: Job? = null
    private var paint: Paint = Paint().apply {
        color = Color.RED
        textSize = 36f
    }

    init {
        Timber.d(" date: 2020-01-08 ")
        setBackgroundColor(Color.TRANSPARENT)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        cancelUpdateUsageJob()
        updateCpuUsageJob = updateCpuUsage()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        cancelUpdateUsageJob()
    }

    private fun cancelUpdateUsageJob() {
        if (updateCpuUsageJob?.isActive == true) {
            updateCpuUsageJob?.cancel()
            updateCpuUsageJob = null
        }
    }

    override fun setVideoURI(uri: Uri?, headers: MutableMap<String, String>?) {
        super.setVideoURI(uri, headers)
        fps = getFrameRate(uri, headers)
        postInvalidate()
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        drawFps(canvas)
        drawCupInfo(canvas)
    }

    private fun drawFps(canvas: Canvas?) {
        val text = "FPS: $fps fps"
        canvas?.drawText(
            text, measuredWidth * 0.95f - getTextWidth(paint, text), measuredHeight * 0.1f, paint
        )
    }

    private fun drawCupInfo(canvas: Canvas?) {
        cpuInfo.mapIndexed { index, text ->
            if (text.isNullOrBlank()) {
                return@mapIndexed
            }
            canvas?.drawText(
                text,
                measuredWidth * 0.95f - getTextWidth(paint, text),
                measuredHeight * 0.2f + index * paint.textSize * 2,
                paint
            )
        }
    }

    private fun getTextWidth(paint: Paint, text: String?): Float {
        if (text.isNullOrEmpty()) {
            return 0f
        }
        val widths = FloatArray(text.length)
        paint.getTextWidths(text, widths)
        return widths.sumBy {
            ceil(it).toInt()
        }.toFloat()
    }

    private fun updateCpuUsage() = GlobalScope.launch {
        while (showCpuInfo) {
            cpuInfo[0] = "CpuTemp: ${CpuInfoReader.getCpuTemp() / 1000}℃"
            cpuInfo[1] = String.format(
                Locale.getDefault(),
                "CpuUsage: %.2f %%",
                CpuInfoReader.getCpuUsage()
            )
            postInvalidate()
        }
    }

    private fun getFrameRate(
        uri: Uri?,
        headers: MutableMap<String, String>?
    ): Int {
        if (uri == null) {
            return 0
        }
        var extractor: MediaExtractor? = null
        var format: MediaFormat?
        try {
            extractor = MediaExtractor()
            extractor.setDataSource(context, uri, headers)
            val numTracks = extractor.trackCount
            (0..numTracks).map { index ->
                format = extractor.getTrackFormat(index)
                if (format?.getString(MediaFormat.KEY_MIME)?.startsWith("video/") == true &&
                    format?.containsKey(MediaFormat.KEY_FRAME_RATE) == true
                ) {
                    return format?.getInteger(MediaFormat.KEY_FRAME_RATE) ?: 0
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            extractor?.release()
        }
        return 0
    }

    companion object {
        const val TIME_NANOS_SECONDS = 1000_000_000L
    }
}