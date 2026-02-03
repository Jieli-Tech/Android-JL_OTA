package com.jieli.otasdk.ui.base

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.jieli.component.utils.ToastUtil
import com.jieli.jl_bt_ota.util.JL_Log
import com.jieli.otasdk.ui.dialog.LoadingDialog
import com.jieli.otasdk.ui.dialog.PermissionTipsDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * @author zqjasonZhong
 * @since 2025/1/13
 * @email zhongzhuocheng@zh-jieli.com
 * @desc Fragment基类
 */
open class BaseFragment : Fragment() {
    @JvmField
    protected val TAG: String = javaClass.simpleName
    private var backJob: Job? = null
    private var loadingDialog: LoadingDialog? = null
    private var permissionTipsDialog: PermissionTipsDialog? = null

    var permissionHandler: ((result: ActivityResult) -> Unit)? = null

    private val grantPermissionsLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            permissionHandler?.let { handler -> handler(it) }
        }


    override fun onDestroyView() {
        super.onDestroyView()
        dismissPermissionTipsDialog()
    }

    /**
     * 替换Fragment
     *
     * @param containerId 控件ID
     * @param fragmentName Fragment路径
     * @param bundle 参数
     * @param isReplace 是否替换
     */
    fun replaceFragment(
        containerId: Int,
        fragmentName: String?,
        bundle: Bundle? = null,
        isReplace: Boolean = false
    ) {
        if (fragmentName.isNullOrEmpty()) return
        val fragment = childFragmentManager.findFragmentByTag(fragmentName) ?: try {
            Class.forName(fragmentName).newInstance() as Fragment
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
        fragment?.let {
            val fragmentTransaction = childFragmentManager.beginTransaction()
            if (null != bundle) {
                it.arguments = bundle
            }
            if (isReplace) {
                //兼容Android 7.0以下，清除Fragment不完全的问题
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                    for (f in childFragmentManager.fragments) {
                        fragmentTransaction.remove(f)
                    }
                }
                fragmentTransaction.replace(containerId, it)
            } else {
                for (f in childFragmentManager.fragments) {
                    if (f == null) continue
                    fragmentTransaction.hide(f)
                }
                if (!it.isAdded) {
                    fragmentTransaction.add(containerId, it, fragmentName)
                    fragmentTransaction.addToBackStack(fragmentName)
                }
                fragmentTransaction.show(it)
            }
            fragmentTransaction.commitAllowingStateLoss()
        }
    }

    protected val isFragmentValid: Boolean
        get() = !isDetached && isAdded

    protected fun showLoading(content: String? = null) {
        if (loadingDialog != null) {
            dismissLoading()
        }
        if (null == content) {
            LoadingDialog.Builder().build().also {
                loadingDialog = it
                it.show(childFragmentManager, LoadingDialog::class.java.simpleName)
            }
            return
        }
        LoadingDialog.Builder().text(content).build().also {
            loadingDialog = it
            it.show(childFragmentManager, LoadingDialog::class.java.simpleName)
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

    protected fun showPermissionTipsDialog(content: String) {
        dismissPermissionTipsDialog()
        PermissionTipsDialog.Builder().tips(content)
            .build().also {
                permissionTipsDialog = it
                it.show(childFragmentManager, PermissionTipsDialog::class.java.simpleName)
            }
    }

    protected fun dismissPermissionTipsDialog() {
        permissionTipsDialog?.let {
            if (it.isShow) {
                it.dismiss()
            }
        }
        permissionTipsDialog = null
    }

    protected fun goToAppDetailsSettings() {
        grantPermissionsLauncher.launch(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            val uri = Uri.fromParts("package", requireContext().packageName, null)
            setData(uri)
        })
    }

    protected fun showTips(tips: String) {
        if (tips.isEmpty()) return
        ToastUtil.showToastShort(tips)
        JL_Log.d(TAG, tips)
    }

    protected fun back(delay: Long = 300L, handler: (() -> Unit)? = null) {
        exit(1, delay, handler)
    }

    protected fun finish(delay: Long = 300L, handler: (() -> Unit)? = null) {
        exit(0, delay, handler)
    }

    private fun exit(op: Int, delay: Long = 0L, handler: (() -> Unit)? = null) {
        if (!isFragmentValid) return
        backJob?.cancel()
        if (delay <= 0) {
            if (op == 1) {
                requireActivity().onBackPressed()
            } else {
                requireActivity().finish()
            }
        } else {
            backJob = lifecycleScope.launch(Dispatchers.IO) {
                handler?.let { it() }
                kotlinx.coroutines.delay(delay)
                withContext(Dispatchers.Main) {
                    if (op == 1) {
                        requireActivity().onBackPressed()
                    } else {
                        requireActivity().finish()
                    }
                }
                backJob = null
            }
        }
    }
}