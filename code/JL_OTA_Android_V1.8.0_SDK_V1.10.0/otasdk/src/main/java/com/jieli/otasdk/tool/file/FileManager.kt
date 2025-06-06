package com.jieli.otasdk.tool.file

import com.jieli.otasdk.MainApplication
import java.io.File
import java.io.IOException


/**
 * FileManager
 * @author zqjasonZhong
 * @since 2025/4/9
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 文件管理器
 */
object FileManager {

    private val context = MainApplication.instance

    fun isUpgradeFile(fileName: String): Boolean =
        fileName.endsWith(".ufw", true) || fileName.endsWith(".bfu", true)

    fun readUpgradeFile(): MutableList<File> {
        try {
            val files = mutableListOf<File>()
            val otaPath = context.oTAFileDir
            //读取私有空间下Upgrade文件夹的升级文件列表
            val folder = File(otaPath)
            if (folder.exists() && folder.isDirectory) {
                folder.listFiles()?.forEach { file ->
                    if (file.isFile && isUpgradeFile(file.name)) {
                        if (!files.contains(file)) {
                            files.add(file)
                        }
                    }
                }
            }
            return files
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return mutableListOf()
    }
}