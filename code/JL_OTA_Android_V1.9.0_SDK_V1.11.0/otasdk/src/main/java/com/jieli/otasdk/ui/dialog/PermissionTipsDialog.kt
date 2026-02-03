package com.jieli.otasdk.ui.dialog

import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.jieli.otasdk.R
import com.jieli.otasdk.databinding.DialogPermissionTipsBinding
import com.jieli.otasdk.util.gone
import com.jieli.otasdk.util.show

/**
 * PermissionTipsDialog
 * @author zqjasonZhong
 * @since 2024/8/12
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 权限申请说明弹窗
 */
class PermissionTipsDialog private constructor(builder: Builder) : CommonDialog(builder) {

    private lateinit var binding: DialogPermissionTipsBinding

    override fun createView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        DialogPermissionTipsBinding.inflate(inflater, container, false).also {
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
        binding.tvTips.apply {
            if (builder.tipsStyle == null) gone() else show()
            builder.tipsStyle?.let {
                text = it.text
                setTextColor(
                    ContextCompat.getColor(
                        context,
                        if (it.color == 0) R.color.text_color else it.color
                    )
                )
                textSize = (if (it.size == 0) 14 else it.size).toFloat()
                typeface =
                    if (it.isBold) Typeface.DEFAULT_BOLD else Typeface.defaultFromStyle(Typeface.NORMAL)
                gravity = it.gravity
                it.onClick?.let { listener ->
                    setOnClickListener { view ->
                        listener.onClick(this@PermissionTipsDialog, view)
                    }
                }
            }
        }
    }

    class Builder : CommonDialog.Builder() {
        var tipsStyle: TextStyle? = null

        init {
            gravity = Gravity.TOP
        }

        fun tips(text: String): Builder {
            if (tipsStyle == null) {
                tipsStyle = TextStyle(text)
            }
            tipsStyle?.text = text
            return this
        }

        override fun build(): PermissionTipsDialog {
            return PermissionTipsDialog(this)
        }
    }
}