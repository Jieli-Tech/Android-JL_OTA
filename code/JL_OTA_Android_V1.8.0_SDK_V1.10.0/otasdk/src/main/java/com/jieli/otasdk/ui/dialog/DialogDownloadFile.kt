package com.jieli.otasdk.ui.dialog

import android.os.Bundle
import android.text.TextUtils
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.jieli.otasdk.R
import com.jieli.otasdk.data.constant.OtaConstant
import com.jieli.otasdk.databinding.FragmentDialogDownloadFileBinding
import com.jieli.otasdk.ui.ota.DownloadFileViewModel
import com.jieli.otasdk.util.DownloadFileUtil.DownloadFileEvent

/**
 * 下载文件提示
 */
class DialogDownloadFile private constructor(builder: Builder) : CommonDialog(builder) {
    private lateinit var binding: FragmentDialogDownloadFileBinding

    override fun createView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        FragmentDialogDownloadFileBinding.inflate(inflater, container, false).also {
            binding = it
            return it.root
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initUI()
        addObserver()
    }


    private fun initUI() {
        if (builder !is Builder) return
        val layoutParams = binding.clOtaContent.layoutParams as FrameLayout.LayoutParams
        layoutParams.height = getPixelsFromDp(148)
        val url = builder.viewModel.getHttpUrl()
        val fileNames = url!!.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        var fileName = fileNames[fileNames.size - 1]
        if (fileName == null) {
            fileName = "upgrade.ufw"
        }
        binding.tvOtaFileName.text = fileName
        binding.tvUpgradeProgress.text = formatProgressText(0)
    }

    //增加观察者
    private fun addObserver() {
        if (builder !is Builder) return
        builder.viewModel.downloadStatusMLD.observe(viewLifecycleOwner) { downloadFileEvent: DownloadFileEvent? ->
            if (downloadFileEvent == null || !isAdded) return@observe
            if (TextUtils.equals(downloadFileEvent.type, "onProgress")) {
                val progress = downloadFileEvent.progress.toInt()
                binding.tvUpgradeProgress.text = formatProgressText(progress)
                binding.pbUpgradeProgress.progress = progress
            } else if (TextUtils.equals(downloadFileEvent.type, "onStop")) {
                builder.viewModel.downloadStatusMLD.postValue(null)
                dismiss()
            } else if (TextUtils.equals(downloadFileEvent.type, "onError")) {
                builder.viewModel.downloadStatusMLD.postValue(null)
                dismiss()
            } else if (TextUtils.equals(downloadFileEvent.type, "onStart")) {
            }
        }
    }

    private fun formatProgressText(progress: Int): String =
        OtaConstant.formatString("%s\t\t%d%%", getString(R.string.downloading_file), progress)

    class Builder(val viewModel: DownloadFileViewModel) : CommonDialog.Builder() {

        init {
            cancelable = false
            widthRate = 1.0f
            gravity = Gravity.BOTTOM
        }

        override fun build(): DialogDownloadFile = DialogDownloadFile(this)
    }
}