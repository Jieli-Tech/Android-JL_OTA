package com.jieli.otasdk.util

import android.os.Environment
import java.io.File
import java.util.Locale

/**
 *
 * @ClassName:      FileTransferUtil
 * @Description:     文件传输工具
 * @Author:         ZhangHuanMing
 * @CreateDate:     2022/10/14 19:01
 */
class FileTransferUtil {
    companion object {

        /**
         * 获取下载文件夹
         */
        fun getDownLoadFolder(): File? {
            return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        }

        /**
         * 获取log存放的文件夹
         */
        fun getLogFolder(): Unit {

        }
        fun checkFileName(): Unit {

        }
        /**
         * 获取新升级文件名
         */
        fun getNewUpgradeFileName(oldFileName: String, parent: File): String {
            var result = "upgrade.ufw"
            if (oldFileName.uppercase(Locale.ROOT).endsWith(".UFW")) {//文件名后缀以ufw结尾
                result = oldFileName
            }// 。说明可能是别的App加密混淆文件名
            var tempResult = result
            var resultFile = File(parent, tempResult)
            var i = 0
            while (resultFile.exists()) {//文件已存在
                i++
                tempResult = result.dropLast(4) + "($i).ufw"
                resultFile = File(parent, tempResult)
            }
            return tempResult
        }
    }
}