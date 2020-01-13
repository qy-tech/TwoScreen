package com.qytech.twoscreen.widget

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.qytech.twoscreen.PlayerThread
import com.qytech.twoscreen.util.CpuInfoReader
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.*
import kotlin.math.ceil

/**
 * Created by Jax on 2020-01-11.
 * Description :
 * Version : V1.0.0
 */
class VideoPlayerView(context: Context, attributeSet: AttributeSet? = null) :
    SurfaceView(context, attributeSet), SurfaceHolder.Callback {

    private var playerThread: PlayerThread? = null
    private var videoPath: String = ""
    private var videoFd: AssetFileDescriptor? = null
    var fps = 0
        private set
    private var cpuInfo = arrayOfNulls<String>(2)
    private var isSurfaceValid = false


    var showCpuInfo = true


    private var updateCpuUsageJob: Job? = null
    private var paint: Paint = Paint().apply {
        color = Color.RED
        textSize = 36f
    }

    init {
        setBackgroundColor(Color.TRANSPARENT)
        holder.addCallback(this)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        cancelUpdateUsageJob()
        updateCpuUsageJob = updateCpuUsage()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        cancelUpdateUsageJob()
        releasePlayer()
    }

    private fun cancelUpdateUsageJob() {
        if (updateCpuUsageJob?.isActive == true) {
            updateCpuUsageJob?.cancel()
            updateCpuUsageJob = null
        }
    }

    fun setDataSource(path: String) {
        this.videoPath = path
        this.videoFd = null
        if (isSurfaceValid) {
            startPlay()
        }
    }

    fun setDataSource(fd: AssetFileDescriptor) {
        this.videoFd = fd
        this.videoPath = ""
        if (isSurfaceValid) {
            startPlay()
        }
    }


    override fun surfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
    }

    override fun surfaceDestroyed(holder: SurfaceHolder?) {
        isSurfaceValid = false
        releasePlayer()
    }

    override fun surfaceCreated(holder: SurfaceHolder?) {
        isSurfaceValid = true
        startPlay()
    }

    private fun releasePlayer() {
        if (playerThread?.isInterrupted == false) {
            playerThread?.interrupt()
            playerThread = null
        }
    }

    private fun startPlay() {
        releasePlayer()
        if (videoFd != null) {
            playerThread = PlayerThread(holder?.surface, videoFd!!)
        } else if (videoPath.isNotBlank()) {
            playerThread = PlayerThread(holder?.surface, videoPath)
        }
        playerThread?.setOnCompletionListener(object : PlayerThread.OnCompletionListener {
            override fun onCompletion() {
                playerThread?.run()
            }
        })
        playerThread?.setOnFpsChangeListener(object : PlayerThread.OnFpsChangeListener {
            override fun onFpsChange(fps: Int) {
                this@VideoPlayerView.fps = fps
                postInvalidate()
            }

        })
        playerThread?.start()
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
}