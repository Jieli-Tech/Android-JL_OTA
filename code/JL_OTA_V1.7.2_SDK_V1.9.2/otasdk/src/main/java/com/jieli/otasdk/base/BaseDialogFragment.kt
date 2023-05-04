package com.jieli.otasdk.base

import android.content.DialogInterface
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager

/**
 ************************************
 *@Author revolve
 *创建时间：2019/7/31  15:44
 *用途
 ************************************
 */
open class BaseDialogFragment : DialogFragment() {
    protected var TAG: String = javaClass.simpleName
    private var isShow = false

    open fun isShow(): Boolean {
        return isShow
    }

    override fun show(manager: FragmentManager, tag: String?) {
//        super.show(manager, tag);
        setShow(true)
        val ft = manager.beginTransaction()
        ft.add(this, tag)
        ft.commitAllowingStateLoss()
    }

    override fun onResume() {
        setShow(true)
        super.onResume()
    }

    override fun dismiss() {
        setShow(false)
        super.dismissAllowingStateLoss()
    }

    override fun onDismiss(dialog: DialogInterface) {
        setShow(false)
        super.onDismiss(dialog)
    }

    override fun onDestroyView() {
        setShow(false)
        super.onDestroyView()
    }

    private fun setShow(isShow: Boolean) {
        this.isShow = isShow
    }
}