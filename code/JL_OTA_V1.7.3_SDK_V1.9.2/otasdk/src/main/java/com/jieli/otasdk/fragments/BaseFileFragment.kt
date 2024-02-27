package com.jieli.otasdk.fragments

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.DialogFragment
import com.jieli.jl_dialog.Jl_Dialog
import com.jieli.otasdk.R
import com.jieli.otasdk.base.BaseFragment
import permissions.dispatcher.*

/**
 *
 * @ClassName:      BaseFileFragment
 * @Description:    不读取外部文件夹是不需要这个权限的
 * @Author:         ZhangHuanMing
 * @CreateDate:     2022/11/7 11:30
 */
@RuntimePermissions
open class BaseFileFragment : BaseFragment() {
    private var notifyDialog: Jl_Dialog? = null
    private lateinit var grantExternalStoragePermissionResult: ActivityResultLauncher<Intent>
    private val callbacks = ArrayList<OnCheckExternalStorageEnvironmentCallback>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        grantExternalStoragePermissionResult =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                checkExternalStorageEnvironment()
            }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        onRequestPermissionsResult(requestCode, grantResults)
    }

    fun registerOnCheckBluetoothEnvironmentCallback(callback: OnCheckExternalStorageEnvironmentCallback) {
        callbacks.add(callback)
    }

    fun unregisterOnCheckBluetoothEnvironmentCallback(callback: OnCheckExternalStorageEnvironmentCallback) {
        callbacks.remove(callback)
    }

    fun checkExternalStorageEnvironment(callback: OnCheckExternalStorageEnvironmentCallback? = null): Unit {
        callback?.let {
            registerOnCheckBluetoothEnvironmentCallback(it)
        }
        grantExternalPermissionWithPermissionCheck()
    }

    @NeedsPermission(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )
    fun grantExternalPermission() {
        onCheckExternalStorageEnvironmentSuccess()
    }

    @OnShowRationale(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )
    fun showRationaleForPermission(request: PermissionRequest) {
        request.proceed()
    }

    @OnNeverAskAgain(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )
    fun onPermissionsNeverAskAgain() {
        showNotifyExternalStoragePermissionDialog()
    }

    @OnPermissionDenied(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )
    fun onPermissionsDenied() {
        onCheckExternalStorageEnvironmentFailed()
    }

    private fun onCheckExternalStorageEnvironmentSuccess(): Unit {
        val tempList: ArrayList<OnCheckExternalStorageEnvironmentCallback> =
            callbacks.clone() as ArrayList<OnCheckExternalStorageEnvironmentCallback>
        tempList.forEach {
            it.onSuccess()
        }
    }

    private fun onCheckExternalStorageEnvironmentFailed(): Unit {
        val tempList: ArrayList<OnCheckExternalStorageEnvironmentCallback> =
            callbacks.clone() as ArrayList<OnCheckExternalStorageEnvironmentCallback>
        tempList.forEach {
            it.onFailed()
        }
    }

    /**
     * 显示需要跳转设置打开存储权限的提示窗
     */
    private fun showNotifyExternalStoragePermissionDialog(): Unit {
        if (!isAdded || isDetached) return
        if (notifyDialog == null) {
            notifyDialog = Jl_Dialog.Builder()
                .title(getString(R.string.tips))
                .content(getString(R.string.grant_external_storage_permission))
                .cancel(false)
                .left(getString(R.string.cancel))
                .leftColor(resources.getColor(R.color.gray_text_444444))
                .right(getString(R.string.to_setting))
                .rightColor(resources.getColor(R.color.red_FF688C))
                .leftClickListener { _: View?, _: DialogFragment? ->
                    dismissNotifyDialog()//不打开GPS
                    onCheckExternalStorageEnvironmentFailed()
                }
                .rightClickListener { _: View?, _: DialogFragment? ->
                    dismissNotifyDialog()
                    Intent().let {
                        it.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        it.data = Uri.parse("package:" + context?.packageName)
                        grantExternalStoragePermissionResult.launch(it)
                    }
                }
                .build()
        }
        if (!this.notifyDialog?.isShow!!) {
            this.notifyDialog?.show(
                parentFragmentManager,
                "notify_grant_location_permission_dialog"
            )
        }
    }

    private fun dismissNotifyDialog() {
        notifyDialog?.run {
            if (this.isShow && isValidFragment()) {
                this.dismiss()
            }
            notifyDialog = null
        }
    }
}

interface OnCheckExternalStorageEnvironmentCallback {
    fun onSuccess();
    fun onFailed()
}