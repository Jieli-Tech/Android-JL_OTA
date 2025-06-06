package com.jieli.otasdk.ui.home

import android.content.Context
import android.content.Intent
import com.jieli.jlFileTransfer.TransferFolder
import com.jieli.jlFileTransfer.TransferFolderCallback
import com.jieli.jlFileTransfer.WebService
import com.jieli.otasdk.MainApplication
import com.jieli.otasdk.R
import com.jieli.otasdk.ui.base.BluetoothViewModel
import java.io.File

/**
 * @author zqjasonZhong
 * @since 2022/9/8
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 主界面逻辑处理
 */
class MainViewModel : BluetoothViewModel() {

    override fun destroy() {
        super.destroy()
        bluetoothHelper.destroy()
    }

    fun startWebService(context: Context) {
        try {
            //开启传文件服务
            val folderList = ArrayList<TransferFolder>()
            folderList.add(TransferFolder().run {
                this.id = 0
                this.folder = File(MainApplication.instance.oTAFileDir)
                this.describe = context.getString(R.string.update_file)
                this.fileType = ".ufw"
                this.callback = object : TransferFolderCallback {
                    override fun onCreateFile(file: File?): Boolean {
                        return true
                    }

                    override fun onDeleteFile(file: File?): Boolean {
                        return file?.delete() == true
                    }
                }
                this
            })
            folderList.add(TransferFolder().run {
                this.id = 1
                this.folder = File(MainApplication.instance.logFileDir)
                this.describe = context.getString(R.string.log_file)
                this.fileType = ".txt"
                this.callback = object : TransferFolderCallback {
                    override fun onCreateFile(file: File?): Boolean {
                        return true
                    }

                    override fun onDeleteFile(file: File?): Boolean {
                        return file?.delete() == true
                    }
                }
                this
            })
            WebService.setTransferFolderList(folderList)
            WebService.start(context)
        } catch (e: Exception) {

        }
    }

    fun stopWebService(context: Context) {
        context.stopService(Intent(context, WebService::class.java))
    }
}