package com.jieli.otasdk.ui.widget.window

import android.content.Context
import android.view.View
import android.view.View.OnAttachStateChangeListener
import android.view.WindowManager
import android.widget.PopupWindow
import com.jieli.otasdk.R
import com.jieli.otasdk.databinding.DialogAddFileOperationBinding
import com.jieli.otasdk.util.getView

/**
 * SelectFileWayWindow
 *
 * @author zqjasonZhong
 * @since 2025/1/14
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 添加文件方式浮窗
 */
class AddFileWayWindow(context: Context, private val callback: OnOperationCallback) {

    companion object {

        /**
         * 选择本地文件
         */
        const val OP_SELECT_LOCAL_FILE = 1

        /**
         * 选择网络文件
         */
        const val OP_SELECT_WEB_FILE = 2

        /**
         * 选择扫描二维码
         */
        const val OP_SCAN_QR_CODE = 3
    }

    private val popupWindow: PopupWindow

    private var binding: DialogAddFileOperationBinding? = null

    init {
        val view = context.getView(R.layout.dialog_add_file_operation)
        popupWindow = PopupWindow(
            view,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        ).also {
            it.isOutsideTouchable = true
        }
        view.addOnAttachStateChangeListener(object : OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) {
                DialogAddFileOperationBinding.bind(v).also {
                    binding = it
                    initUI()
                }
            }

            override fun onViewDetachedFromWindow(v: View) {
                binding = null
                v.removeOnAttachStateChangeListener(this)
            }
        })
    }

    fun show(parent: View) {
        popupWindow.showAsDropDown(parent)
    }

    private fun initUI() {
        binding?.apply {
            tvUpgradeFileBrowseLocal.setOnClickListener {
                popupWindow.dismiss()
                callback.onOperation(OP_SELECT_LOCAL_FILE)
            }
            tvUpgradeFileHttpTransfer.setOnClickListener {
                popupWindow.dismiss()
                callback.onOperation(OP_SELECT_WEB_FILE)
            }
            tvScanQrCode.setOnClickListener {
                popupWindow.dismiss()
                callback.onOperation(OP_SCAN_QR_CODE)
            }
        }
    }

    fun interface OnOperationCallback {

        fun onOperation(op: Int)
    }
}