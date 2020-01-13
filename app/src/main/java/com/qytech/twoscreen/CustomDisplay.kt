package com.qytech.twoscreen

import android.app.Presentation
import android.content.Context
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

    fun setDataSource(path: String) {
        videoView.post {
            videoView.setDataSource(path)
        }
    }

    init {
        Timber.d(" date: 2020-01-09 ")
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.view_display_custom)
        playVideo()
    }

    private fun playVideo() {
        val afd = resources.openRawResourceFd(R.raw.test_30fps)
        videoView.setDataSource(afd)
        videoView.showCpuInfo = false
    }
}