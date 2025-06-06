package com.jieli.otasdk.ui.dialog

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.jieli.otasdk.databinding.FragmentDialogFileTransferBinding

/**
 * @ClassName: DialogFileTransfer
 * @Description: 文件传输弹窗
 * @Author: ZhangHuanMing
 * @CreateDate: 2022/10/14 10:31
 */
class DialogFileTransfer private constructor(builder: Builder) : CommonDialog(builder) {
    private lateinit var binding: FragmentDialogFileTransferBinding

    override fun createView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        FragmentDialogFileTransferBinding.inflate(inflater, container, false).also {
            binding = it
            return it.root
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initUI()
    }

    private fun initUI() {
        if (builder !is Builder) return
        binding.tvHttpUrl.text = builder.httpUrl
        binding.btLeft.setOnClickListener {
            builder.callback?.onClose(this)
        }
        binding.btRight.setOnClickListener {
            builder.callback?.onCopyAddress(this, builder.httpUrl)
        }
    }

    class Builder : CommonDialog.Builder() {
        var httpUrl: String = ""
            private set
        var callback: OnClickCallback? = null
            private set

        init {
            widthRate = 1.0f
            cancelable = false
            gravity = Gravity.BOTTOM
        }

        fun url(url: String): Builder {
            httpUrl = url
            return this
        }

        fun callback(callback: OnClickCallback?): Builder {
            this.callback = callback
            return this
        }

        override fun build(): DialogFileTransfer = DialogFileTransfer(this)

    }

    interface OnClickCallback {

        fun onClose(dialog: CommonDialog)

        fun onCopyAddress(dialog: CommonDialog, uri: String)
    }
}
