package com.jieli.otasdk.ui.dialog

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.*
import androidx.annotation.FloatRange
import androidx.core.content.ContextCompat
import com.jieli.otasdk.ui.base.BaseDialogFragment
import kotlin.math.roundToInt

/**
 * @author zqjasonZhong
 * @since 2023/5/25
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 通用对话框
 */
abstract class CommonDialog(val builder: Builder) : BaseDialogFragment() {

    open class TextStyle(
        var text: String,
        var color: Int = 0,
        var size: Int = 0,
        var isBold: Boolean = false,
        var gravity: Int = Gravity.CENTER,
        var onClick: OnViewClick? = null,
        var topDrawableRes: Int = 0
    ){
        override fun toString(): String {
            return "${javaClass.simpleName}(text='$text', color=$color, size=$size, isBold=$isBold, gravity=$gravity, onClick=$onClick, topDrawableRes=$topDrawableRes)"
        }
    }

    fun interface OnViewClick {

        fun onClick(dialog: CommonDialog, view: View)
    }

    class ButtonStyle(
        text: String,
        color: Int = 0,
        size: Int = 0,
        isBold: Boolean = false,
        gravity: Int = Gravity.CENTER,
        onClick: OnViewClick? = null,
        topDrawableRes: Int = 0
    ) : TextStyle(text, color, size, isBold, gravity, onClick, topDrawableRes)

    @SuppressLint("ResourceAsColor")
    override fun onStart() {
        super.onStart()
        val window = requireDialog().window
        window?.apply {
            val mLayoutParams = attributes
            mLayoutParams.dimAmount = builder.dimAmount
            mLayoutParams.gravity = builder.gravity
            mLayoutParams.flags = mLayoutParams.flags or WindowManager.LayoutParams.FLAG_DIM_BEHIND

            mLayoutParams.width =
                if (builder.widthRate == 0f) WindowManager.LayoutParams.WRAP_CONTENT else (builder.widthRate * screenWidth).roundToInt()
            mLayoutParams.height =
                if (builder.heightRate == 0f) WindowManager.LayoutParams.WRAP_CONTENT else (builder.heightRate * screenHeight).roundToInt()

            if (builder.x != 0) mLayoutParams.x = builder.x
            if (builder.y != 0) mLayoutParams.y = builder.y

            attributes = mLayoutParams
            val color =
                if (builder.backgroundColor == 0) Color.TRANSPARENT else ContextCompat.getColor(
                    requireContext(),
                    builder.backgroundColor
                )
            setBackgroundDrawable(ColorDrawable(color))
            decorView.rootView?.setBackgroundColor(color)
        }
    }

    //会导致部分手机显示黑色背景
    /*override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, android.R.style.Theme_Light_NoTitleBar)
    }*/

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        requireDialog().requestWindowFeature(Window.FEATURE_NO_TITLE)
        return createView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireDialog().setCanceledOnTouchOutside(builder.cancelable)
        isCancelable = builder.cancelable
    }

    abstract fun createView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View

    abstract class Builder {
        var gravity: Int = Gravity.CENTER

        @FloatRange(from = 0.0, to = 1.0)
        var widthRate: Float = 0.9f

        @FloatRange(from = 0.0, to = 1.0)
        var heightRate: Float = 0f
        var cancelable: Boolean = true
        var x: Int = 0
        var y: Int = 0
        var backgroundColor: Int = 0

        @FloatRange(from = 0.0, to = 1.0)
        var dimAmount: Float = 0.5f

        abstract fun build(): CommonDialog

        override fun toString(): String {
            return "Builder(gravity=$gravity, widthRate=$widthRate, heightRate=$heightRate, cancelable=$cancelable, x=$x, y=$y)"
        }
    }
}