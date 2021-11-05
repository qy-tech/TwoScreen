package com.qytech.twoscreen

import android.app.Presentation
import android.content.Context
import android.content.pm.ActivityInfo
import android.view.Display
import android.view.WindowManager
import android.widget.Toast
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

    fun setDataSource(path: String) {
        videoView.post {
            videoView.setDataSource(path)
        }
    }

    init {
        Timber.d(" date: 2020-01-09 ")
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.view_display_custom)

        Toast.makeText(
            context,
            "width is ${display.width} height is ${display.height} ",
            Toast.LENGTH_SHORT
        ).show()
        playVideo()
    }

    private fun playVideo() {
        val afd = resources.openRawResourceFd(R.raw.test_320x240)
        videoView.setDataSource(afd)
        videoView.showCpuInfo = true
    }
}