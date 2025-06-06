package com.jieli.otasdk

import android.app.Application
import com.jieli.component.ActivityManager
import com.jieli.component.utils.ToastUtil
import com.jieli.jlFileTransfer.FileUtils
import com.jieli.jl_bt_ota.util.CommonUtil
import com.jieli.jl_bt_ota.util.JL_Log
import com.jieli.jl_bt_ota.util.PreferencesHelper
import com.jieli.otasdk.data.constant.OtaConstant
import com.jieli.otasdk.util.AppUtil
import com.jieli.otasdk.util.CrashHandler
import com.jieli.otasdk.util.FileUtil
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream

/**
 * MainApplication
 * @author zqjasonZhong
 * @since 2025/2/11
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 应用主入口
 */
class MainApplication : Application() {
    /**
     * 是否调试模式
     */
    private val isDebug = BuildConfig.DEBUG

    /**
     * OTA文件夹路径
     */
    lateinit var oTAFileDir: String
        private set

    /**
     * 日志文件夹路径
     */
    lateinit var logFileDir: String
        private set

    @Throws(Throwable::class)
    protected fun finalize() {
        handleLog(false)
    }


    override fun onCreate() {
        super.onCreate()
        instance = this
        oTAFileDir =
            FileUtil.createFilePath(this, FileUtil.DIR_UPGRADE)/*FileUtil.getDownloadFolderPath()*/
        logFileDir = JL_Log.getSaveLogPath(instance)

        ActivityManager.init(this)
        ToastUtil.init(this)
        CommonUtil.setMainContext(this)
        handleLog(isDebug)
        //第一次使用新的OTA文件夹地址
//        checkOldOTAFileDir();
    }

    private fun handleLog(isDebug: Boolean) {
        if (isDebug) {
            CrashHandler.getInstance().init(this)
        }
        JL_Log.setLog(isDebug)
        JL_Log.setIsSaveLogFile(this, isDebug)
    }

    private fun checkOldOTAFileDir() { //删除新升级文件夹的所有文件，拷贝旧文件夹的文件进去
        val sharedPreferences = PreferencesHelper.getSharedPreferences(
            this
        )
        //是不是第一次使用新的OTA文件路径
        val isFirstUseNewOTAFileDir = sharedPreferences.getBoolean("isFirstUseNewOTAFileDir", true)
        if (!isFirstUseNewOTAFileDir) return
        val newPath = oTAFileDir
        val newFolder = File(newPath)
        val newFiles = newFolder.listFiles()
        JL_Log.d(TAG, "checkOldOTAFileDir", "New file size : ${newFiles?.size ?: 0}")
        if (newFiles?.isNotEmpty() == true) {
            for (tempFile in newFiles) {
                tempFile.delete()
            }
        }
        val oldPath = AppUtil.createFilePath(instance, OtaConstant.DIR_UPGRADE)
        val oldFolder = File(oldPath)
        val oldFiles = oldFolder.listFiles()
        if (oldFiles?.isNotEmpty() == true) { //原升级文件夹存在文件
            for (tempFile in oldFiles) {
                val resultPath = newPath + File.separator + tempFile.name
                val resultFile = File(resultPath)
                if (!resultFile.exists()) {
                    var inputStream: InputStream?
                    try {
                        inputStream = FileInputStream(tempFile)
                        FileUtils.copyFile(inputStream, resultPath)
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
            }
        }
        PreferencesHelper.putBooleanValue(this, "isFirstUseNewOTAFileDir", false)
    }

    companion object {
        private const val TAG = "MainApplication"

        @JvmStatic
        lateinit var instance: MainApplication
            private set
    }

}

