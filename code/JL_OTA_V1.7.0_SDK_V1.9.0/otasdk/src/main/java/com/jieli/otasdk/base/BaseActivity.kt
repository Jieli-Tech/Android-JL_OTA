package com.jieli.otasdk.base

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.PopupWindow
import androidx.fragment.app.Fragment
import com.jieli.broadcastbox.BroadcastBoxActivity
import com.jieli.component.base.Jl_BaseActivity
import com.jieli.component.utils.SystemUtil
import com.jieli.otasdk.R
import com.jieli.otasdk.activities.MainActivity
import kotlinx.android.synthetic.main.popup_function_selector.view.*

/**
 *  create Data:2019-07-24
 *  create by:chensenhua
 *
 **/
open class BaseActivity : Jl_BaseActivity() {
    protected val tag: String = javaClass.simpleName
    private val mOnBackPressIntercept: OnBackPressIntercept? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SystemUtil.setImmersiveStateBar(window, true)
    }

    override fun onBackPressed() {
        if (mOnBackPressIntercept == null || !mOnBackPressIntercept.intercept()) {
            super.onBackPressed()
        }
    }

    open fun replaceFragment(containerId: Int, fragmentName: String?) {
        replaceFragment(containerId, fragmentName, null)
    }

    open fun replaceFragment(containerId: Int, fragmentName: String?, bundle: Bundle?) {
        var fragment = supportFragmentManager.findFragmentByTag(fragmentName)
        if (fragment == null && fragmentName != null) {
            try {
                fragment = Class.forName(fragmentName).newInstance() as Fragment
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        if (fragment != null) {
            fragment.arguments = intent.extras
            val fragmentTransaction = supportFragmentManager.beginTransaction()
            for (f in supportFragmentManager.fragments) {
                fragmentTransaction.hide(f!!)
            }
            if (!fragment.isAdded) {
                fragmentTransaction.add(containerId, fragment, fragmentName)
            }
            if (null != bundle) {
                fragment.arguments = bundle
            }
            fragmentTransaction.show(fragment)
            fragmentTransaction.commitAllowingStateLoss()
        }
    }

    open fun isValidActivity(): Boolean {
        return !isFinishing && !isDestroyed
    }

    /**
     * 切换到单设备升级
     */
    fun switchPopupWindow(context: Activity, it : View) {
        context.run {
            val view = LayoutInflater.from(this).inflate(R.layout.popup_function_selector, null)
            val popupWindow = PopupWindow(view, WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT)
            popupWindow.isOutsideTouchable = true
            if (context is BroadcastBoxActivity) {
                view.tv_single_ota.setCompoundDrawablesRelativeWithIntrinsicBounds(0,0,R.drawable.ic_white_hook, 0)
            } else if (context is MainActivity) {
                view.tv_multiple_ota.setCompoundDrawablesRelativeWithIntrinsicBounds(0,0,R.drawable.ic_white_hook, 0)
            }
            popupWindow.showAsDropDown(it)

            view.tv_multiple_ota.setOnClickListener {
                popupWindow.dismiss()
                if (context is BroadcastBoxActivity) {
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    context.isSkipDestroyViewModel = true
                    context.finish()
                }
            }
            view.tv_single_ota.setOnClickListener {
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
}

interface OnBackPressIntercept {
    fun intercept(): Boolean
}