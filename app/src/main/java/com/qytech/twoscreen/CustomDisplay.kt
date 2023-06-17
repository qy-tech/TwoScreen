package com.qytech.twoscreen

import android.app.Presentation
import android.content.Context
import android.content.pm.ActivityInfo
import android.view.Display
import android.view.WindowManager
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import com.qytech.twoscreen.databinding.ViewDisplayCustomBinding
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

    private var binding: ViewDisplayCustomBinding
    fun setDataSource(path: String) {
        binding.videoView.post {
            binding.videoView.setDataSource(path)
        }
    }

    init {
        Timber.d(" date: 2020-01-09 ")
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        binding = DataBindingUtil.inflate(layoutInflater, R.layout.view_display_custom, null, false)
        setContentView(binding.root)
        playVideo()
    }

    private fun playVideo() {
        val afd = resources.openRawResourceFd(R.raw.test_720p_apple)
        binding.videoView.setDataSource(afd)
        binding.videoView.showCpuInfo = true
    }
}