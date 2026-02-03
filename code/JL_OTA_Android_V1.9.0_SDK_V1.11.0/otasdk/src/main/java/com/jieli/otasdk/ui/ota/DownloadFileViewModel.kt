package com.jieli.otasdk.ui.ota

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.jieli.otasdk.MainApplication
import com.jieli.otasdk.util.DownloadFileUtil
import java.io.File
import java.util.Calendar

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
        val parentFilePath = MainApplication.instance.oTAFileDir
        var fileName = "upgrade.ufw"
        val resultFile = File(File(parentFilePath), fileName)
        if (resultFile.exists()) {
            val fileTypeIndex = fileName.lastIndexOf(".")
            val fileType = fileName.substring(fileTypeIndex)
            val realName = fileName.substring(0, fileTypeIndex)
            val calendar = Calendar.getInstance()
            fileName = realName + "_" + calendar.get(Calendar.MILLISECOND) + fileType
        }
        val resultPath = parentFilePath + File.separator + fileName
        DownloadFileUtil.downloadFile(httpUrl, resultPath) { event ->
            this@DownloadFileViewModel.downloadStatusMLD.postValue((event))
        }
    }
}