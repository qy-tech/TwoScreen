package com.qytech.twoscreen.ui.main

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
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
import com.qytech.twoscreen.databinding.MainFragmentBinding
import com.qytech.twoscreen.util.FileUtils
import com.qytech.twoscreen.util.PathUtil
import timber.log.Timber
import java.io.File

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
    private val gpioFile = File("/sys/class/gpio/gpio21/value")
    private val hdmiStatusFile = File("/sys/class/drm/card0-HDMI-A-1/status")

    private lateinit var binding: MainFragmentBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = MainFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(MainViewModel::class.java)
        initDisplayManager()
        if (customDisplay == null) {
            initCustomDisplay()
        }
        binding.message.visibility = if (displays.isNullOrEmpty()) View.GONE else View.VISIBLE

        val afd = requireContext().resources.openRawResourceFd(R.raw.test_1080p_ldh)
        binding.videoView.setDataSource(afd)
        binding.btnSelectVideo.setOnClickListener {
            selectVideo(CODE_MAIN_VIDEO)
        }
        binding.btnPresentationVideo.setOnClickListener {
            selectVideo(CODE_PRESENTATION_VIDEO)
        }
        binding.btnPresentationVideo.visibility = View.GONE
        binding.btnDecodeTest.visibility = View.GONE
        binding.btnSelectVideo.visibility = View.GONE
        binding.message.setOnClickListener {
            Timber.d("on message click date: 2020-01-09 ")
            if (customDisplay?.isShowing == true) {
                customDisplay?.dismiss()
            } else {
                customDisplay?.show()
            }
        }
        binding.switchGpioTest.isChecked = FileUtils.readFromFile(hdmiStatusFile) == "connected"
        binding.switchGpioTest.setOnCheckedChangeListener { _, isChecked ->
            if (hdmiStatusFile.exists() && hdmiStatusFile.canRead() && hdmiStatusFile.canWrite()) {
                if (isChecked) {
                    FileUtils.write2File(hdmiStatusFile, "on")
                } else {
                    FileUtils.write2File(hdmiStatusFile, "off")
                }
                updateDisplays()
            }
        }
        customDisplay?.show()
    }

    override fun onDestroy() {
        super.onDestroy()
        releaseCustomDisplay()
    }

    private fun updateDisplays() {
        Timber.d("updateDisplays size ${displays.size}")
        binding.message.postDelayed({
            displays = displayManager.getDisplays(DisplayManager.DISPLAY_CATEGORY_PRESENTATION)
            binding.message.visibility = if (displays.isNullOrEmpty()) View.GONE else View.VISIBLE
            Timber.d("updateDisplays size ${displays.size}")
        }, 500L)
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
                data?.data?.let {
                    binding.videoView.setDataSource(PathUtil.getPath(requireContext(), it))
                }
            }

            CODE_PRESENTATION_VIDEO -> {
//                customDisplay?.setVideoURI(data?.data)
                data?.data?.let {
                    customDisplay?.setDataSource(PathUtil.getPath(requireContext(), it))
                }
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
