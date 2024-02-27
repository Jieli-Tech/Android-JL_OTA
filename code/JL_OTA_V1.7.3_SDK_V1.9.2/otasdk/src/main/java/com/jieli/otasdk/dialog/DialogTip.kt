package com.jieli.otasdk.dialog

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.*
import androidx.fragment.app.DialogFragment
import com.jieli.otasdk.R
import kotlinx.android.synthetic.main.fragment_dialog_tip.*


class DialogTip : DialogFragment() {
    var title: String? = null
    var leftText: String? = null
    var rightText: String? = null
    var dialogClickListener: DialogTipClickListener? = null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        //设置dialog的基本样式参数
        requireDialog().requestWindowFeature(Window.FEATURE_NO_TITLE)
        val window = requireDialog().window
        if (window != null) {
            //去掉dialog默认的padding
            window.decorView.setPadding(0, 0, 0, 0)
            val lp = window.attributes
            lp.width = WindowManager.LayoutParams.WRAP_CONTENT
            lp.height = WindowManager.LayoutParams.WRAP_CONTENT
            lp.gravity = Gravity.CENTER
            //设置dialog的动画
//            lp.windowAnimations = R.style.BottomToTopAnim
            window.attributes = lp
            window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
        return inflater.inflate(R.layout.fragment_dialog_tip, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        title?.run {
            tv_dialog_title.visibility = View.VISIBLE
            tv_dialog_title.setText(this)
        }
        leftText?.run {
            bt_left.visibility = View.VISIBLE
            bt_left.setText(this)
        }
        rightText?.run {
            bt_right.visibility = View.VISIBLE
            bt_right.setText(this)
        }
        if (leftText != null && rightText != null) {
            view1.visibility = View.VISIBLE
        }

        bt_right.setOnClickListener {
            dialogClickListener?.rightBtnClick()
        }
        bt_left.setOnClickListener {
            dialogClickListener?.leftBtnClick()
        }
    }
}

interface DialogTipClickListener {
    fun rightBtnClick(): Unit
    fun leftBtnClick(): Unit
}