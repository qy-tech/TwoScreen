package com.qytech.twoscreen.util

import android.os.Process
import kotlinx.coroutines.delay
import java.io.*
import java.util.regex.Pattern


/**
 * Created by Jax on 2020-01-10.
 * Description :
 * Version : V1.0.0
 */
object CpuInfoReader {
    //Cpu 小核频率
    private const val SCALING_CUR_FREQ_0 =
        "/sys/devices/system/cpu/cpufreq/policy0/scaling_cur_freq"

    //cpu大核频率
    private const val SCALING_CUR_FREQ_4 =
        "/sys/devices/system/cpu/cpufreq/policy4/scaling_cur_freq"

    //cpu温度
    private const val CPU_TEMP = "/sys/class/thermal/thermal_zone0/temp"

    //GPU频率
    private const val GPU_FREQ = "/sys/class/devfreq/ff9a0000.gpu/cur_freq"

    //GPU温度
    private const val GPU_TEMP = "/sys/class/thermal/thermal_zone1/temp"
    private const val SOC = "/sys/devices/system/cpu/soc"
    private const val SCALING_CUR_FREQ = "/sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq"
    private const val CPUINFO_MAX_FREQ = "/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq"
    private const val CPUINFO_MIN_FREQ = "/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_min_freq"
    private const val SCALING_AVAILABLE_FREQUENCIES =
        "/sys/devices/system/cpu/cpu0/cpufreq/scaling_available_frequencies"

    suspend fun getProcessCpuRate(): Float {
        val totalCpuTime1 = getCpuTime()[0]
        val processCpuTime1 = getAppCpuTime()
        delay(360L)
        val totalCpuTime2 = getCpuTime()[0]
        val processCpuTime2 = getAppCpuTime()
        return (processCpuTime2 - processCpuTime1) / (totalCpuTime2 - totalCpuTime1).toFloat() * 100
    }

    suspend fun getCpuUsage(): Float {
        val cpuInfoLast = getCpuTime()
        delay(1000L)
        val cpuInfoCurrent = getCpuTime()

        val totalTime = cpuInfoCurrent[0] - cpuInfoLast[0]
        val iddleTime = cpuInfoCurrent[1] - cpuInfoLast[1]
        val percent = (totalTime - iddleTime) / totalTime.toFloat() * 100
        return if (percent == 0f) {
            1.0f
        } else {
            percent
        }
    }

    // 获取系统总CPU使用时间和空闲时间
    private fun getCpuTime(): LongArray {
        val cpuInfo = LongArray(2)
        var cpuInfos: Array<String> = try {
            val reader = BufferedReader(
                InputStreamReader(
                    FileInputStream("/proc/stat")
                ), 1000
            )
            val load: String = reader.readLine()
            reader.close()
            load.split(" ").toTypedArray()
        } catch (ex: IOException) {
            ex.printStackTrace()
            return cpuInfo
        }
        val totalCpu = cpuInfos?.copyOfRange(2, 8)?.sumOf { it.toLong() } ?: 0
        cpuInfo[0] = totalCpu
        cpuInfo[1] = cpuInfos[5].toLong()

        return cpuInfo
    }

    private fun getAppCpuTime(): Long {
        var cpuInfos: Array<String>? = null
        try {
            val pid = Process.myPid()
            val reader = BufferedReader(
                InputStreamReader(
                    FileInputStream("/proc/$pid/stat")
                ), 1000
            )
            val load: String = reader.readLine()
            reader.close()
            cpuInfos = load.split(" ").toTypedArray()
        } catch (ex: IOException) {
            ex.printStackTrace()
        }
        return if (cpuInfos.isNullOrEmpty()) {
            0L
        } else {
            cpuInfos[13].toLong() + cpuInfos[14].toLong() + cpuInfos[15].toLong() + cpuInfos[16].toLong()
        }
    }


    /**
     * 获取CPU核数
     * @return
     */
    fun getCpuCores(): Int { // Private Class to display only CPU devices in the directory listing
        class CpuFilter : FileFilter {
            override fun accept(pathname: File): Boolean { // Check if filename is "cpu", followed by a single digit number
                return Pattern.matches("cpu[0-9]", pathname.name)
            }
        }
        return try { // Get directory containing CPU info
            val dir = File("/sys/devices/system/cpu/")
            // Filter to only list the devices we care about
            val files: Array<File> = dir.listFiles(CpuFilter())
            // Return the number of cores (virtual CPU devices)
            files.size
        } catch (e: Exception) {
            e.printStackTrace()
            // Default to return 1 core
            -1
        }
    }

    /**
     * 获取CPU型号
     * @return
     */
    fun getCpuModel(): String? {
        return FileUtils.readFromFile(File(SOC))
    }

    /**
     * 获取CPU最低频率
     * @return
     */
    fun getCpuMinFreq(): Int {
        return FileUtils.readFromFile(File(CPUINFO_MIN_FREQ))?.toInt() ?: -1
    }

    /**
     * 获取CPU最高频率
     * @return
     */
    fun getCpuMaxFreq(): Int {
        return FileUtils.readFromFile(File(CPUINFO_MAX_FREQ))?.toInt() ?: -1
    }

    /**
     * 获取CPU当前频率
     * @return
     */
    fun getCpuCurrentFreq(): Int {
        return FileUtils.readFromFile(File(SCALING_CUR_FREQ))?.toInt() ?: -1 //cpuinfo_cur_freq
    }
    /*
	public static List<Integer> getAvailableFrequencies() {
		String freqs = FileUtils.readFromFile(new File(SCALING_AVAILABLE_FREQUENCIES));
	}
*/

    /*
	public static List<Integer> getAvailableFrequencies() {
		String freqs = FileUtils.readFromFile(new File(SCALING_AVAILABLE_FREQUENCIES));
	}
*/
    /**
     * 获取CPU大核频率
     *
     * @return
     */
    fun getCpuPolicy4Freq(): Int {
        return FileUtils.readFromFile(File(SCALING_CUR_FREQ_4))?.toInt() ?: -1 // cpuinfo_cur_freq
    }

    /**
     * 获取CPU小核频率
     *
     * @return
     */
    fun getCpuPolicy0Freq(): Int {
        return FileUtils.readFromFile(File(SCALING_CUR_FREQ_0))?.toInt() ?: -1 // cpuinfo_cur_freq
    }

    /**
     * 获取CPU温度
     *
     * @return
     */
    fun getCpuTemp(): Int {
        return FileUtils.readFromFile(File(CPU_TEMP))?.toInt() ?: -1 // cpuinfo_cur_freq
    }

    /**
     * 获取GPU温度
     *
     * @return
     */
    fun getGpuTemp(): Int {
        return FileUtils.readFromFile(File(GPU_TEMP))?.toInt() ?: -1 // cpuinfo_cur_freq
    }

    /**
     * 获取GPU频率
     *
     * @return
     */
    fun getGpuFreq(): Int {
        return FileUtils.readFromFile(File(GPU_FREQ))?.toInt() ?: -1 // cpuinfo_cur_freq
    }
}