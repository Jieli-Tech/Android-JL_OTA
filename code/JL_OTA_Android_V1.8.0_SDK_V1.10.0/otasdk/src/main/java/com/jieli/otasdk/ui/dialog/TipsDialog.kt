package com.jieli.otasdk.ui.dialog

import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.jieli.jl_bt_ota.util.JL_Log
import com.jieli.otasdk.R
import com.jieli.otasdk.databinding.DialogTipsBinding
import com.jieli.otasdk.util.gone
import com.jieli.otasdk.util.hide
import com.jieli.otasdk.util.show

/**
 * @author zqjasonZhong
 * @since 2023/7/3
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 提示对话框
 */
class TipsDialog private constructor(builder: Builder) : CommonDialog(builder) {
    private lateinit var binding: DialogTipsBinding

    class Builder : CommonDialog.Builder() {
        var titleStyle: TextStyle? = null
        var contentStyle: TextStyle? = null

        var cancelBtnStyle: ButtonStyle? = null
        var sureBtnStyle: ButtonStyle? = null

        var data: Any? = null

        init {
            widthRate = 0f
        }

        fun title(text: String): Builder {
            if (titleStyle == null) {
                titleStyle = TextStyle(text, isBold = true)
            } else {
                titleStyle?.text = text
            }
            return this
        }

        fun content(text: String, isBold: Boolean = true, topDrawableRes: Int = 0): Builder {
            if (contentStyle == null) {
                contentStyle = TextStyle(text)
            } else {
                contentStyle!!.text = text
            }
            contentStyle!!.isBold = isBold
            contentStyle!!.topDrawableRes = topDrawableRes
            return this
        }

        fun cancelBtn(
            text: String = "",
            color: Int = 0,
            isBold: Boolean = true,
            listener: OnViewClick? = null
        ): Builder {
            if (cancelBtnStyle == null) {
                cancelBtnStyle = ButtonStyle(text)
            } else {
                cancelBtnStyle?.text = text
            }
            cancelBtnStyle?.let {
                it.color = color
                it.isBold = isBold
                it.onClick = listener
            }
            return this
        }

        fun confirmBtn(
            text: String = "",
            color: Int = 0,
            isBold: Boolean = true,
            listener: OnViewClick? = null
        ): Builder {
            if (sureBtnStyle == null) {
                sureBtnStyle = ButtonStyle(text)
            } else {
                sureBtnStyle?.text = text
            }
            sureBtnStyle?.let {
                it.color = color
                it.isBold = isBold
                it.onClick = listener
            }
            return this
        }

        fun extraData(data: Any?): Builder {
            this.data = data
            return this
        }

        override fun build(): TipsDialog = TipsDialog(this)
        override fun toString(): String {
            return "Builder(titleStyle=$titleStyle, contentStyle=$contentStyle, cancelBtnStyle=$cancelBtnStyle, sureBtnStyle=$sureBtnStyle, data=$data)" +
                    "\n${super.toString()}"
        }


    }

    override fun createView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        DialogTipsBinding.inflate(inflater, container, false).also {
            binding = it
            return it.root
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        refreshUI()
    }

    fun refreshUI() {
        if (builder !is Builder) return
        JL_Log.i(TAG, "refreshUI", "$builder")
        binding.tvTitle.apply {
            if (builder.titleStyle == null) gone() else show()
            builder.titleStyle?.let {
                text = it.text
                setTextColor(
                    ContextCompat.getColor(
                        context,
                        if (it.color == 0) R.color.text_color else it.color
                    )
                )
                textSize = (if (it.size == 0) 18 else it.size).toFloat()
                typeface =
                    if (it.isBold) Typeface.DEFAULT_BOLD else Typeface.defaultFromStyle(Typeface.NORMAL)
                gravity = it.gravity
                it.onClick?.let { listener ->
                    setOnClickListener {view ->
                        listener.onClick(this@TipsDialog, view)
                    }
                }
            }
        }
        binding.tvContent.apply {
            if (builder.contentStyle == null) hide() else show()
            builder.contentStyle?.let {
                text = it.text
                setTextColor(
                    ContextCompat.getColor(
                        context,
                        if (it.color == 0) R.color.black_99000000 else it.color
                    )
                )
                textSize = (if (it.size == 0) 16 else it.size).toFloat()
                typeface =
                    if (it.isBold) Typeface.DEFAULT_BOLD else Typeface.defaultFromStyle(Typeface.NORMAL)
                gravity = it.gravity
                it.onClick?.let { listener ->
                    setOnClickListener{view ->
                        listener.onClick(this@TipsDialog, view)
                    }
                }
                setCompoundDrawablesRelativeWithIntrinsicBounds(0, it.topDrawableRes, 0, 0)
            }
        }
        binding.btnCancel.apply {
            if (builder.cancelBtnStyle == null) gone() else show()
            builder.cancelBtnStyle?.let {
                text = it.text.ifEmpty { getString(R.string.cancel) }
                setTextColor(
                    ContextCompat.getColor(
                        context,
                        if (it.color == 0) R.color.black_66000000 else it.color
                    )
                )
                textSize = (if (it.size == 0) 18 else it.size).toFloat()
                typeface =
                    if (it.isBold) Typeface.DEFAULT_BOLD else Typeface.defaultFromStyle(Typeface.NORMAL)
                gravity = it.gravity
                setOnClickListener { view ->
                    it.onClick?.onClick(this@TipsDialog, view)
                }
            }
        }
        binding.btnSure.apply {
            if (builder.sureBtnStyle == null) gone() else show()
            builder.sureBtnStyle?.let { sureBtn ->
                text = sureBtn.text.ifEmpty { getString(R.string.confirm) }
                setTextColor(
                    ContextCompat.getColor(
                        context,
                        if (sureBtn.color == 0) R.color.blue_558CFF else sureBtn.color
                    )
                )
                textSize = (if (sureBtn.size == 0) 18 else sureBtn.size).toFloat()
                typeface =
                    if (sureBtn.isBold) Typeface.DEFAULT_BOLD else Typeface.defaultFromStyle(
                        Typeface.NORMAL
                    )
                gravity = sureBtn.gravity
                setOnClickListener {view ->
                    sureBtn.onClick?.onClick(this@TipsDialog, view)
                }
            }
        }

        if (builder.cancelBtnStyle != null && builder.sureBtnStyle != null) {
            binding.viewHorizontalLine.show()
            binding.viewLine.show()
            binding.btnCancel.show()
            binding.btnSure.show()
        } else if (builder.cancelBtnStyle == null && builder.sureBtnStyle == null) {
            binding.viewHorizontalLine.gone()
            binding.viewLine.gone()
            binding.btnCancel.gone()
            binding.btnSure.gone()
        } else {
            binding.viewHorizontalLine.show()
            binding.viewLine.gone()
            if (builder.cancelBtnStyle != null) {
                binding.btnSure.gone()
                binding.btnCancel.show()
            }
            if (builder.sureBtnStyle != null) {
                binding.btnCancel.gone()
                binding.btnSure.show()
            }
        }
    }
}