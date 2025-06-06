package com.jieli.otasdk.ui.base

import android.content.DialogInterface
import android.util.DisplayMetrics
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.jieli.component.utils.ToastUtil
import com.jieli.jl_bt_ota.util.JL_Log
import com.jieli.otasdk.util.ViewUtil

/**
 * @author zqjasonZhong
 * @since 2025/1/13
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 基类DialogFragment
 */
open class BaseDialogFragment : DialogFragment() {
    @JvmField
    protected val TAG: String = javaClass.simpleName
    var isShow = false
        private set
    private val displayMetrics: DisplayMetrics?
        get() {
            return if (requireContext().resources == null) null else requireContext().resources.displayMetrics
        }

    override fun show(manager: FragmentManager, tag: String?) {
//        super.show(manager, tag);
        isShow = true
        val ft = manager.beginTransaction()
        ft.add(this, tag)
        ft.commitAllowingStateLoss()
    }

    override fun onResume() {
        isShow = true
        super.onResume()
    }

    override fun dismiss() {
        isShow = false
        super.dismissAllowingStateLoss()
    }

    override fun onDismiss(dialog: DialogInterface) {
        isShow = false
        super.onDismiss(dialog)
    }

    override fun onDestroyView() {
        isShow = false
        super.onDestroyView()
    }

    val screenWidth: Int
        get() {
            val displayMetrics = displayMetrics
            return displayMetrics?.widthPixels ?: 0
        }

    val screenHeight: Int
        get() {
            val displayMetrics = displayMetrics
            return displayMetrics?.heightPixels ?: 0
        }


    protected fun getPixelsFromDp(size: Int): Int = ViewUtil.dp2px(requireContext(), size)

    protected fun showTips(tips: String) {
        if (tips.isEmpty()) return
        ToastUtil.showToastShort(tips)
        JL_Log.i(tag, tips)
    }


}