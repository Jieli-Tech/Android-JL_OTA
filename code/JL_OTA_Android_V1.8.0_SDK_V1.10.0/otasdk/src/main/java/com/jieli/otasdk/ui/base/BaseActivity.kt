package com.jieli.otasdk.ui.base

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.PopupWindow
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.jieli.broadcastbox.BroadcastBoxActivity
import com.jieli.component.utils.ToastUtil
import com.jieli.jl_bt_ota.util.JL_Log
import com.jieli.otasdk.R
import com.jieli.otasdk.ui.dialog.LoadingDialog
import com.jieli.otasdk.ui.home.MainActivity
import com.jieli.otasdk.util.ViewUtil
import com.jieli.otasdk.util.getView

/**
 * @author zqjasonZhong
 * @since 2025/1/13
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 基类Activity
 */
abstract class BaseActivity : AppCompatActivity() {
    protected val TAG: String = javaClass.simpleName
    protected var loadingDialog: LoadingDialog? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        ViewUtil.setImmersiveStateBar(window, true)
        super.onCreate(savedInstanceState)
    }

    fun isValidActivity(): Boolean = !isFinishing && !isDestroyed

    fun replaceFragment(
        containerId: Int,
        fragmentName: String?,
        bundle: Bundle? = null,
        isReplace: Boolean = false
    ) {
        if (fragmentName.isNullOrEmpty()) return
        val fragment = supportFragmentManager.findFragmentByTag(fragmentName) ?: try {
            Class.forName(fragmentName).newInstance() as Fragment
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
        fragment?.let {
            val transaction = supportFragmentManager.beginTransaction()
            if (null != bundle) {
                it.arguments = bundle
            }
            if (isReplace) {
                //兼容Android 7.0以下，清除Fragment不完全的问题
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                    for (f in supportFragmentManager.fragments) {
                        transaction.remove(f)
                    }
                }
                transaction.replace(containerId, it)
            } else {
                for (f in supportFragmentManager.fragments) {
                    transaction.hide(f)
                }
                if (!it.isAdded) {
                    transaction.add(containerId, it, fragmentName)
                    transaction.addToBackStack(fragmentName)
                }
                transaction.show(it)
            }
            transaction.commitAllowingStateLoss()
        }
    }

    /**
     * 切换到单设备升级
     */
    fun switchPopupWindow(context: Activity, it: View) {
        context.run {
            val view = getView(R.layout.popup_function_selector)
            val popupWindow = PopupWindow(
                view, WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT
            )
            popupWindow.isOutsideTouchable = true
            if (context is BroadcastBoxActivity) {
                view.findViewById<TextView>(R.id.tv_single_ota)
                    .setCompoundDrawablesRelativeWithIntrinsicBounds(
                        0, 0,
                        R.drawable.ic_white_hook, 0
                    )
            } else if (context is MainActivity) {
                view.findViewById<TextView>(R.id.tv_multiple_ota)
                    .setCompoundDrawablesRelativeWithIntrinsicBounds(
                        0, 0,
                        R.drawable.ic_white_hook, 0
                    )
            }
            popupWindow.showAsDropDown(it)

            view.findViewById<TextView>(R.id.tv_multiple_ota).setOnClickListener {
                popupWindow.dismiss()
                if (context is BroadcastBoxActivity) {
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    context.isSkipDestroyViewModel = true
                    context.finish()
                }
            }
            view.findViewById<TextView>(R.id.tv_single_ota).setOnClickListener {
                popupWindow.dismiss()
                if (context is MainActivity) {
                    val intent = Intent(this, BroadcastBoxActivity::class.java)
                    startActivity(intent)
                    context.isSkipDestroyViewModel = true
                    context.finish()
                }
            }
        }
    }


    protected fun removeFragment(fragment: Fragment?) {
        fragment?.let {
            val transaction = supportFragmentManager.beginTransaction()
            transaction.remove(it)
            if (supportFragmentManager.fragments.size >= 2) {
                for ((index, f) in supportFragmentManager.fragments.withIndex()) {
                    if (index == supportFragmentManager.fragments.size - 1 || f == fragment) continue
                    transaction.hide(f)
                }
            }
            transaction.commitAllowingStateLoss()
        }
    }

    protected fun showLoading(content: String? = null) {
        if (loadingDialog != null) {
            dismissLoading()
        }
        if (null == content) {
            LoadingDialog.Builder().build().also {
                loadingDialog = it
                it.show(supportFragmentManager, LoadingDialog::class.java.simpleName)
            }
            return
        }
        LoadingDialog.Builder().text(content).build().also {
            loadingDialog = it
            it.show(supportFragmentManager, LoadingDialog::class.java.simpleName)
        }
    }

    protected fun dismissLoading() {
        if (null != loadingDialog) {
            if (loadingDialog!!.isShow) {
                loadingDialog!!.dismiss()
            }
            loadingDialog = null
        }
    }

    protected fun showTips(tips: String) {
        if (tips.isEmpty()) return
        ToastUtil.showToastShort(tips)
        JL_Log.d(TAG, tips)
    }
}