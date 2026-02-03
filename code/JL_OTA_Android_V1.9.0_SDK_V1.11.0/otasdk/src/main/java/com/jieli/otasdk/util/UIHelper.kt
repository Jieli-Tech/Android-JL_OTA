package com.jieli.otasdk.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.view.View
import android.widget.CompoundButton
import androidx.core.util.Consumer
import androidx.fragment.app.FragmentManager
import com.jieli.component.utils.ToastUtil
import com.jieli.jlFileTransfer.Constants
import com.jieli.jlFileTransfer.FileUtils
import com.jieli.jlFileTransfer.WifiUtils
import com.jieli.jl_bt_ota.constant.ErrorCode
import com.jieli.jl_bt_ota.interfaces.IActionCallback
import com.jieli.jl_bt_ota.model.OTAError
import com.jieli.jl_bt_ota.util.JL_Log
import com.jieli.otasdk.MainApplication
import com.jieli.otasdk.R
import com.jieli.otasdk.databinding.ItemSettingsCheckBinding
import com.jieli.otasdk.databinding.ItemSettingsSwitchBinding
import com.jieli.otasdk.databinding.ItemSettingsTextBinding
import com.jieli.otasdk.ui.dialog.CommonDialog
import com.jieli.otasdk.ui.dialog.DialogFileTransfer
import com.jieli.otasdk.ui.dialog.DialogInputText
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.util.Locale

/**
 * UIHelper
 * @author zqjasonZhong
 * @since 2025/1/13
 * @email zhongzhuocheng@zh-jieli.com
 * @desc UI辅助类
 */
object UIHelper {


    /**
     * 显示网络文件传输对话框
     *
     * @param context Context 上下文
     * @param manager FragmentManager Fragment管理器
     */
    fun showWebFileTransferDialog(
        context: Context,
        manager: FragmentManager,
        callback: Consumer<Boolean>
    ) {
        val ipAddress = NetworkUtil.getWifiIpAddress(context)
        val url = if (null == ipAddress) {
            context.getString(R.string.connect_wifi_tips)
        } else {
            "http://${ipAddress}:${Constants.HTTP_PORT}"
        }
        val tag = context::class.simpleName ?: "UIHelper"
        DialogFileTransfer.Builder()
            .url(url)
            .callback(object : DialogFileTransfer.OnClickCallback {
                override fun onClose(dialog: CommonDialog) {
                    dialog.dismiss()
                    callback.accept(true)
                }

                override fun onCopyAddress(dialog: CommonDialog, uri: String) {
                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val mClipData = ClipData.newPlainText("Label", uri)
                    cm.setPrimaryClip(mClipData)
                    showTips(tag, context.getString(R.string.copy_toast))
                }
            }).build().show(manager, DialogFileTransfer::class.java.simpleName)
    }

    /**
     * 显示保存文件对话框
     *
     * @param context Context 上下文
     * @param manager FragmentManager Fragment管理器
     * @param uri Uri 文件链接
     */
    fun showSaveFileDialog(
        context: Context,
        manager: FragmentManager,
        uri: Uri,
        callback: IActionCallback<Boolean>? = null
    ) {
        val tag = context::class.simpleName ?: "UIHelper"
        try {
            context.contentResolver?.let { contentResolver ->
                val parentFilePath = MainApplication.instance.oTAFileDir
                var fileName = FileUtils.getFileName(context, uri)
                fileName =
                    FileTransferUtil.getNewUpgradeFileName(fileName, File(parentFilePath))
                DialogInputText.Builder()
                    .title(context.getString(R.string.save_file))
                    .content(fileName)
                    .cancelBtn { dialog, _ ->
                        dialog.dismiss()
                    }
                    .confirmBtn(context.getString(R.string.save)) { dialog, _ ->
                        val inputFileNameStr = (dialog as DialogInputText).getResult()
                        dialog.dismiss()
                        if (!inputFileNameStr.endsWith(".UFW", true)) {
                            showTips(tag, context.getString(R.string.ufw_format_file_tips))
                            callback?.onError(OTAError.buildError(ErrorCode.SUB_ERR_PARAMETER))
                            return@confirmBtn
                        }
                        val resultPath = parentFilePath + File.separator + inputFileNameStr
                        if (File(resultPath).exists()) {
                            showTips(tag, context.getString(R.string.file_name_existed))
                            callback?.onError(OTAError.buildError(ErrorCode.SUB_ERR_UPGRADE_SAME_FILE))
                            return@confirmBtn
                        }
                        try {
                            FileUtils.copyFile(
                                contentResolver.openInputStream(uri),
                                resultPath
                            )
                            showTips(tag, context.getString(R.string.please_refresh_web))
                            callback?.onSuccess(true)
                        } catch (e: FileNotFoundException) {
                            showTips(tag, context.getString(R.string.upload_failed))
                            callback?.onError(OTAError.buildError(ErrorCode.SUB_ERR_FILE_NOT_FOUND))
                        }
                    }.build().show(manager, DialogInputText::class.simpleName)
            }
        } catch (e: IOException) {
            showTips(tag, context.getString(R.string.read_file_failed))
            callback?.onError(OTAError.buildError(ErrorCode.SUB_ERR_IO_EXCEPTION))
        }
    }

    fun updateSettingsSwitchUI(
        itemBinding: ItemSettingsSwitchBinding,
        title: String? = null,
        isCheck: Boolean = false,
        isShowLine: Boolean = false,
        listener: CompoundButton.OnCheckedChangeListener? = null
    ) {
        itemBinding.apply {
            title?.let {
                tvTitle.text = it
            }
            swBtn.setCheckedNoEvent(isCheck)
            if (isShowLine) {
                viewLine.show()
            } else {
                viewLine.gone()
            }
            listener?.let {
                swBtn.setOnCheckedChangeListener(it)
            }
        }
    }

    fun getBooleanValue(itemBinding: ItemSettingsSwitchBinding): Boolean =
        itemBinding.swBtn.isChecked

    fun updateSettingsTextUI(
        itemBinding: ItemSettingsTextBinding,
        title: String? = null,
        value: String? = null,
        isShowIcon: Boolean = true,
        isShowLine: Boolean = false,
        listener: View.OnClickListener? = null
    ) {
        itemBinding.apply {
            title?.let {
                tvTitle.text = it
            }
            value?.let {
                tvValue.text = it
            }
            if (isShowIcon) {
                ivImage.show()
            } else {
                ivImage.gone()
            }
            if (isShowLine) {
                viewLine.show()
            } else {
                viewLine.gone()
            }
            listener?.let {
                root.setOnClickListener(it)
            }
        }
    }

    fun getStringValue(itemBinding: ItemSettingsTextBinding): String =
        itemBinding.tvValue.text.toString().trim()


    fun updateSettingsCheckUI(
        itemBinding: ItemSettingsCheckBinding,
        title: String? = null,
        isCheck: Boolean = false,
        isShowLine: Boolean = false,
        listener: View.OnClickListener? = null
    ) {
        itemBinding.apply {
            title?.let {
                tvTitle.text = it
            }
            if (isCheck) {
                ivCheck.show()
            } else {
                ivCheck.gone()
            }
            if (isShowLine) {
                viewLine.show()
            } else {
                viewLine.gone()
            }
            listener?.let {
                root.setOnClickListener(it)
            }
        }
    }

    fun getCheckValue(itemBinding: ItemSettingsCheckBinding): Boolean =
        itemBinding.ivCheck.isShow()

    private fun showTips(tag: String, tips: String) {
        if (tips.isEmpty()) return
        ToastUtil.showToastShort(tips)
        JL_Log.i(tag, tips)
    }
}