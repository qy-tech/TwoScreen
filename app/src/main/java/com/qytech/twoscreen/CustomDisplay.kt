package com.qytech.twoscreen

import android.app.Presentation
import android.content.Context
import android.net.Uri
import android.view.Display
import android.view.WindowManager
import kotlinx.android.synthetic.main.view_display_custom.*
import timber.log.Timber

/**
 * Created by Jax on 2020-01-08.
 * Description :
 * Version : V1.0.0
 */
class CustomDisplay(
    context: Context,
    display: Display,
    theme: Int = R.style.AppTheme
) : Presentation(context, display, theme) {

    fun setVideoURI(uri: Uri?) {
        videoView.post {
            videoView.setVideoURI(uri)
        }
    }

    init {
        Timber.d(" date: 2020-01-09 ")
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.view_display_custom)
        playVideo()
    }

    private fun playVideo() {
//        val path = "${Environment.getExternalStorageDirectory()}/test_30.mp4"
//        videoView.setVideoPath(path)
        videoView.setVideoURI(Uri.parse("android.resource://${context.packageName}/raw/test_30fps"))
        videoView.showCpuInfo = false
        videoView.setOnPreparedListener { mp ->
            mp.isLooping = true
            mp.start()
        }
    }


    /*@TargetApi(Build.VERSION_CODES.O)
    private fun initMediaPlayer(holder: SurfaceHolder?) {
        val path = "${Environment.getExternalStorageDirectory()}/1080P30_bilibili.flv"
//        val path2 =
//            "${Environment.getExternalStorageDirectory()}/big-buck-bunny-1080p-60fps-30s.mp4"
        Timber.d("initMediaPlayer date: 2020-01-09 getFrameRate ${getFrameRate(path)}")
        mediaPlayer = MediaPlayer.create(context, Uri.fromFile(File(path)), holder)
        mediaPlayer?.setOnPreparedListener { mp ->
            Timber.d("initMediaPlayer date: 2020-01-09 videoWidth ${mp.videoWidth} videoHeight ${mp.videoHeight}")
            resetSurfaceHolder(mp, holder)
            mp.isLooping = true
            mp.start()
        }


    }

    private fun resetSurfaceHolder(mp: MediaPlayer, holder: SurfaceHolder?) {
        val videoWidth = mp.videoWidth
        val videoHeight = mp.videoHeight
        val viewWidth = surfaceView.measuredWidth
        val viewHeight = surfaceView.measuredHeight
        val wr = viewWidth / videoWidth.toFloat()
        val hr = viewWidth / videoHeight.toFloat()
        val ar = videoWidth / videoHeight.toFloat()
        var width = viewWidth
        var height = viewHeight
        if (wr > hr) {
            width = (viewHeight * ar).toInt()
        } else {
            height = (viewHeight / ar).toInt()
        }
        holder?.setFixedSize(width, height)
    }*/
}