package com.qytech.twoscreen.ui.main

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.net.Uri
import android.os.Bundle
import android.view.Display
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import com.qytech.twoscreen.CustomDisplay
import com.qytech.twoscreen.R
import kotlinx.android.synthetic.main.main_fragment.*
import timber.log.Timber

class MainFragment : Fragment() {

    companion object {
        const val CODE_MAIN_VIDEO = 0x101
        const val CODE_PRESENTATION_VIDEO = 0x102
        fun newInstance() = MainFragment()
    }

    private lateinit var viewModel: MainViewModel
    private lateinit var displayManager: DisplayManager
    private var displays = arrayOf<Display>()
    private var customDisplay: CustomDisplay? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.main_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(MainViewModel::class.java)
        // TODO: Use the ViewModel
        initDisplayManager()
        //        videoView.setVideoPath("${Environment.getExternalStorageDirectory()}/big-buck-bunny-1080p-60fps-30s.mp4")
        //        videoView.setVideoURI(Uri.parse("android.resource://${requireActivity().packageName}/raw/cat1"))
        videoView.setVideoURI(Uri.parse("android.resource://${requireActivity().packageName}/raw/test_60fps"))
        videoView.setOnPreparedListener {
            it.isLooping = true
            videoView.start()
        }
        videoView.setOnCompletionListener {
        }
        btn_select_video.setOnClickListener {
            selectVideo(CODE_MAIN_VIDEO)
        }
        btn_presentation_video.setOnClickListener {
            selectVideo(CODE_PRESENTATION_VIDEO)
        }
        message.setOnClickListener {
            Timber.d("on message click date: 2020-01-09 ")
            if (customDisplay == null) {
                initCustomDisplay()
            }
            if (customDisplay?.isShowing == true) {
                customDisplay?.dismiss()
                btn_presentation_video.visibility = View.GONE
            } else {
                customDisplay?.show()
                btn_presentation_video.visibility = View.VISIBLE
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        releaseCustomDisplay()
    }

    private fun initDisplayManager() {
        displayManager =
            requireContext().getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        displays = displayManager.getDisplays(DisplayManager.DISPLAY_CATEGORY_PRESENTATION)
    }

    private fun initCustomDisplay() {
        Timber.d("initCustomDisplay date: 2020-01-09 ${displays.size}")
        if (displays.isNotEmpty()) {
            releaseCustomDisplay()
            customDisplay = CustomDisplay(requireContext(), displays[0])
        } else {
            Toast.makeText(requireContext(), "不支持多屏!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun selectVideo(requestCode: Int) {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "video/*"
        startActivityForResult(Intent.createChooser(intent, "Select Video"), requestCode)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != Activity.RESULT_OK) {
            return
        }
        when (requestCode) {
            CODE_MAIN_VIDEO -> {
                videoView.setVideoURI(data?.data)
            }
            CODE_PRESENTATION_VIDEO -> {
                customDisplay?.setVideoURI(data?.data)
            }
        }
    }

    private fun releaseCustomDisplay() {
        if (customDisplay?.isShowing == true) {
            customDisplay?.dismiss()
        }
        customDisplay = null
    }

}
