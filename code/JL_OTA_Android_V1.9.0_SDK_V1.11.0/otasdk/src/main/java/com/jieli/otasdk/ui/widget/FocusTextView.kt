package com.jieli.otasdk.ui.widget

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView

/**
 * FocusTextView
 * @author zqjasonZhong
 * @since 2024/8/8
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 默认焦点的TextView
 */
class FocusTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatTextView(context, attrs, defStyleAttr) {

    override fun isFocused(): Boolean {
        return true
    }
}