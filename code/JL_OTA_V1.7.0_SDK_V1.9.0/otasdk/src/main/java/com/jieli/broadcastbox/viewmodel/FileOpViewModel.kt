package com.jieli.broadcastbox.viewmodel

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.FileObserver
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.jieli.jlFileTransfer.TransferFolder
import com.jieli.jlFileTransfer.TransferFolderCallback
import com.jieli.jlFileTransfer.WebService
import com.jieli.otasdk.MainApplication
import com.jieli.otasdk.util.FileObserverCallback
import com.jieli.otasdk.util.OtaFileObserverHelper
import java.io.File

/**
 * @author zqjasonZhong
 * @since 2022/12/6
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 文件操作逻辑实现
 */
class FileOpViewModel : ViewModel() {
    val TAG: String = javaClass.simpleName

    val fileListMLD = MutableLiveData<MutableList<File>>()
    var upgradeFiles: HashSet<File> = HashSet()
    private val fileObserverHelper = OtaFileObserverHelper.getInstance()
    private val handler = Handler(Looper.getMainLooper())

    private val fileObserverCallback = FileObserverCallback { event, path ->
        if (event >= FileObserver.MODIFY && !TextUtils.isEmpty(path)) {
            handler.removeCallbacks(readRunnable)
            handler.postDelayed(readRunnable, 500)
        }
    }

    private val readRunnable = Runnable {
        readFileList()
    }

    init {
        fileObserverHelper.registerFileObserverCallback(fileObserverCallback)
    }

    fun startFileObserver() {
        fileObserverHelper.startObserver()
    }

    fun readFileList() {
        val dirPath: String? = MainApplication.getOTAFileDir()
        dirPath?.let {
            val parent = File(it)
            val files = mutableListOf<File>()
            if (parent.exists()) {
                Log.d(TAG, "readFileList: " + (parent.listFiles()?.size ?: 0))
                parent.listFiles()?.forEach { file ->
                    if (file.name.endsWith(".ufw") || file.name.endsWith(".bfu")) {
                        files.add(file)
                    }
                }
            } else {
                parent.mkdirs()
            }
            upgradeFiles.addAll(files)
            fileListMLD.postValue(files)
        }
    }

    fun checkIfGranted(context: Context, permission: String?): Boolean {
        // 适配android M，检查权限
        return if (Build.VERSION.SDK_INT >= 23) {
            ContextCompat.checkSelfPermission(context, permission!!) == PackageManager.PERMISSION_GRANTED
        } else true
    }

    fun startWebService(context: Context) {
        //开启传文件服务
        val folderList = ArrayList<TransferFolder>()
        folderList.add(TransferFolder().run {
            this.id = 0
            this.folder = File(MainApplication.getOTAFileDir())
            this.describe = "升级文件"
            this.fileType = ".ufw"
            this.callback = object : TransferFolderCallback {
                override fun onCreateFile(file: File?): Boolean {
                    return true;
                }

                override fun onDeleteFile(file: File?): Boolean {
                    return file?.delete() == true;
                }
            }
            this
        })
        folderList.add(TransferFolder().run {
            this.id = 1
            this.folder = File(MainApplication.getLogFileDir())
            this.describe = "Log文件"
            this.fileType = ".txt"
            this.callback = object : TransferFolderCallback {
                override fun onCreateFile(file: File?): Boolean {
                    return true;
                }

                override fun onDeleteFile(file: File?): Boolean {
                    return file?.delete() == true;
                }
            }
            this
        })
        WebService.setTransferFolderList(folderList)
        WebService.start(context)
    }

    fun stopWebService(context: Context) {
        WebService.stop(context)
    }

    override fun onCleared() {
        fileObserverHelper.unregisterFileObserverCallback(fileObserverCallback)
        handler.removeCallbacksAndMessages(null)
        upgradeFiles.clear()
    }
}