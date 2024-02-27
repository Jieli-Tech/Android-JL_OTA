package com.jieli.otasdk.viewmodel

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.jieli.otasdk.MainApplication
import com.jieli.otasdk.util.DownloadFileUtil
import java.io.File
import java.text.SimpleDateFormat
import java.util.*


/**
 * @ClassName: DownloadFileViewModel
 * @Description: java类作用描述
 * @Author: ZhangHuanMing
 * @CreateDate: 2023/4/25 14:41
 */
class DownloadFileViewModel : ViewModel() {
    private val TAG = "DownloadFileViewModel"
    private var httpUrl: String? = null
    val downloadStatusMLD = MutableLiveData<DownloadFileUtil.DownloadFileEvent>()
    fun getHttpUrl(): String? {
        return this.httpUrl
    }

    fun downloadFile(httpUrl: String) {
        this.httpUrl = httpUrl
        val parentFilePath = MainApplication.getOTAFileDir()
        var fileName = httpUrl?.split("/")?.last()

        val fileTypeIndex = fileName.lastIndexOf(".")
        val realName = fileName.substring(0, fileTypeIndex)
        fileName = realName + ".ufw"
//        if (fileName == null) {
//            fileName = "upgrade.ufw"
//        }
        var resultFile = File(File(parentFilePath), fileName)
        if (resultFile.exists()) {
            val fileTypeIndex = fileName.lastIndexOf(".")
            val fileType = fileName.substring(fileTypeIndex)
            val realName = fileName.substring(0, fileTypeIndex)
            val format: SimpleDateFormat = SimpleDateFormat("yyyyMMddHHmmssSSS", Locale.ENGLISH)
            val str1: String = format.format(Date())
            fileName = realName + "_" + str1 + fileType
        }
        Log.d(TAG, "onActivityResult: fileName  :" + fileName)

        Log.d(TAG, "onActivityResult: 扫描结果 http :" + httpUrl)
        val downloadCallback = object : DownloadFileUtil.DownloadFileCallback {
            override fun onEvent(event: DownloadFileUtil.DownloadFileEvent?) {
                this@DownloadFileViewModel.downloadStatusMLD.postValue((event))
            }
        }
        val resultPath = parentFilePath + File.separator + fileName
        DownloadFileUtil.downloadFile(httpUrl, resultPath, downloadCallback)
    }
}