package com.jieli.otasdk.fragments

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.InputType
import android.view.*
import androidx.fragment.app.DialogFragment
import com.jieli.otasdk.R
import kotlinx.android.synthetic.main.fragment_dialog_input_text.*


class DialogInputText : DialogFragment() {
    var title: String? = null
    var content: String? = null
    var leftText: String? = null
    var rightText: String? = null
    var inputType: Int? = null
    var dialogClickListener: DialogClickListener? = null
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
        return inflater.inflate(R.layout.fragment_dialog_input_text, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        title?.run {
            tv_dialog_title.visibility = View.VISIBLE
            tv_dialog_title.setText(this)
        }
        content?.run {
            et_dialog_input.setText(this)
        }
        inputType?.run {
            et_dialog_input.inputType = this
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
        iv_input_delete.setOnClickListener {
            et_dialog_input.setText("")
        }
        bt_right.setOnClickListener {
            dialogClickListener?.rightBtnClick(et_dialog_input.text.toString().trim())
        }
        bt_left.setOnClickListener {
            dialogClickListener?.leftBtnClick(et_dialog_input.text.toString().trim())
        }
    }
}

interface DialogClickListener {
    fun rightBtnClick(inputText: String?): Unit
    fun leftBtnClick(inputText: String?): Unit
}