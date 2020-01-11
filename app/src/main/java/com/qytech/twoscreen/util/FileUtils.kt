package com.qytech.twoscreen.util

import android.content.Context
import java.io.*


/**
 * Created by Jax on 2020-01-10.
 * Description :
 * Version : V1.0.0
 */
object FileUtils {
    /**
     * 读取文件
     * @param file
     * @return 字符串
     */
    fun readFromFile(file: File?): String? {
        return if (file != null && file.exists()) {
            try {
                val fin = FileInputStream(file)
                val reader = BufferedReader(InputStreamReader(fin))
                val value: String = reader.readLine()
                fin.close()
                value
            } catch (e: IOException) {
                null
            }
        } else null
    }

    /**
     * 文件中写入字符串
     * @param file
     * @param enabled
     */
    fun write2File(
        file: File?,
        value: String?
    ): Boolean { //if((file == null) || (!file.exists())) return false;
        return try {
            val fout = FileOutputStream(file)
            val pWriter = PrintWriter(fout)
            pWriter.println(value)
            pWriter.flush()
            pWriter.close()
            fout.close()
            true
        } catch (re: IOException) {
            re.printStackTrace()
            false
        }
    }


    /**
     * 将Asset下的文件复制到/data/data/.../files/目录下
     * @param context
     * @param fileName
     */
    fun copyFromAsset(
        context: Context,
        fileName: String,
        recreate: Boolean
    ): Boolean {
        var buf = ByteArray(20480)
        return try {
            val fileDir: File = context.filesDir
            if (!fileDir.exists()) {
                fileDir.mkdirs()
            }
            val destFilePath: String =
                fileDir.absolutePath + File.separator.toString() + fileName
            val destFile = File(destFilePath)
            if (!destFile.exists() || recreate) {
                destFile.createNewFile()
            } else {
                return true
            }
            val os = FileOutputStream(destFilePath) // 得到数据库文件的写入流
            val `is`: InputStream = context.assets.open(fileName) // 得到数据库文件的数据流
            var cnt: Int
            while (`is`.read(buf).also { cnt = it } != -1) {
                os.write(buf, 0, cnt)
            }
            os.flush()
            `is`.close()
            os.close()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun getDataFileFullPath(context: Context, fileName: String): String? {
        val fileDir: File = context.filesDir
        return fileDir.absolutePath + File.separator.toString() + fileName
    }

    /**
     * 读取文件内容
     * @param file
     * @return
     */
    fun readFileContent(file: File?): ByteArray? {
        var fin: InputStream? = null
        try {
            fin = FileInputStream(file)
            val readBuffer = ByteArray(20480)
            var readLen = 0
            val contentBuf = ByteArrayOutputStream()
            while (fin.read(readBuffer).also({ readLen = it }) > 0) {
                contentBuf.write(readBuffer, 0, readLen)
            }
            return contentBuf.toByteArray()
        } catch (e: Exception) {
        } finally {
            if (fin != null) {
                try {
                    fin.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
        return null
    }
}