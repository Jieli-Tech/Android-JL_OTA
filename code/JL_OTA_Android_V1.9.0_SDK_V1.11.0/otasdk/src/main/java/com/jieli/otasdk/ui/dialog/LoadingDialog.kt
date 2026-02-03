package com.jieli.otasdk.ui.dialog

import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.jieli.otasdk.R
import com.jieli.otasdk.databinding.DialogLoadingBinding
import com.jieli.otasdk.util.gone
import com.jieli.otasdk.util.show

/**
 * @author zqjasonZhong
 * @since 2023/7/4
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 等待框
 */
class LoadingDialog private constructor(builder: Builder) : CommonDialog(builder) {
    private lateinit var binding: DialogLoadingBinding

    class Builder : CommonDialog.Builder() {
        var isChangeWidth: Boolean = false
        var loadingColor: Int = 0
        var textStyle: TextStyle? = null

        fun text(text: String): Builder {
            if (textStyle == null) {
                textStyle = TextStyle(text)
            } else {
                textStyle?.text = text
            }
            return this
        }

        override fun build(): LoadingDialog = LoadingDialog(this.apply {
            if (!isChangeWidth) widthRate = 0f
        })
    }

    override fun createView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        DialogLoadingBinding.inflate(inflater, container, false).also {
            binding = it
            return it.root
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updateUI()
    }

    fun updateUI() {
        if (!isAdded || isDetached) return
        if (builder !is Builder) return
        if (builder.loadingColor != 0) {
            binding.aivLoading.setIndicatorColor(builder.loadingColor)
        }
        binding.aivLoading.show()
        binding.tvText.apply {
            if (builder.textStyle == null) gone() else show()
            builder.textStyle?.let {
                text = it.text
                setTextColor(
                    ContextCompat.getColor(
                        context,
                        if (it.color == 0) R.color.white else it.color
                    )
                )
                textSize = (if (it.size == 0 or it.size) 14 else it.size).toFloat()
                typeface =
                    if (it.isBold) Typeface.DEFAULT_BOLD else Typeface.defaultFromStyle(Typeface.NORMAL)
                gravity = it.gravity
                it.onClick?.let { listener ->
                    setOnClickListener { view ->
                        listener.onClick(this@LoadingDialog, view)
                    }
                }
            }
        }
    }
}