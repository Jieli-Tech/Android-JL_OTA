package com.jieli.otasdk.ui.ota

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.core.util.Consumer
import com.jieli.otasdk.R
import com.jieli.otasdk.ui.base.BaseFragment
import com.jieli.otasdk.ui.dialog.TipsDialog
import com.jieli.otasdk.util.PermissionUtil
import permissions.dispatcher.NeedsPermission
import permissions.dispatcher.OnNeverAskAgain
import permissions.dispatcher.OnPermissionDenied
import permissions.dispatcher.OnShowRationale
import permissions.dispatcher.PermissionRequest
import permissions.dispatcher.RuntimePermissions

/**
 *
 * @ClassName:      BaseFileFragment
 * @Description:    不读取外部文件夹是不需要这个权限的
 * @Author:         ZhangHuanMing
 * @CreateDate:     2022/11/7 11:30
 */
@RuntimePermissions
open class BaseFileFragment : BaseFragment() {

    private var callback: Consumer<Boolean>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        permissionHandler = {
            checkExternalStorageEnvironment()
        }
    }

    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION")
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        onRequestPermissionsResult(requestCode, grantResults)
    }

    fun tryToCheckStorageEnvironment(callback: Consumer<Boolean>) {
        if (this.callback != null) return
        this.callback = callback
        checkExternalStorageEnvironment()
    }

    @NeedsPermission(
        Manifest.permission.READ_EXTERNAL_STORAGE
    )
    fun grantExternalPermission() {
        dismissPermissionTipsDialog()
        onSuccess()
    }

    @OnShowRationale(
        Manifest.permission.READ_EXTERNAL_STORAGE
    )
    fun showRationaleForPermission(request: PermissionRequest) {
        request.proceed()
    }

    @OnNeverAskAgain(
        Manifest.permission.READ_EXTERNAL_STORAGE
    )
    fun onPermissionsNeverAskAgain() {
        onPermissionsDenied()
    }

    @OnPermissionDenied(
        Manifest.permission.READ_EXTERNAL_STORAGE
    )
    fun onPermissionsDenied() {
        dismissPermissionTipsDialog()
        onFail()
        showOperationTipsDialog(getString(R.string.grant_external_storage_permission)) {
            goToAppDetailsSettings()
        }
    }

    private fun checkExternalStorageEnvironment() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU && !PermissionUtil.hasReadStoragePermission(requireContext())) {
            showPermissionTipsDialog(getString(R.string.grant_external_storage_permission))
            grantExternalPermissionWithPermissionCheck()
            return
        }
        onSuccess()
    }

    private fun onSuccess() {
        callback?.accept(true)
        callback = null
    }

    private fun onFail() {
        callback?.accept(false)
        callback = null
    }

    private fun showOperationTipsDialog(content: String, method: () -> Unit) {
        if (!isFragmentValid) return
        TipsDialog.Builder()
            .title(getString(R.string.tips))
            .content(content)
            .cancelBtn(color = R.color.gray_text_444444) { dialog, _ ->
                dialog.dismiss()
                onFail()
            }
            .confirmBtn(getString(R.string.to_setting), R.color.red_FF688C) { dialog, _ ->
                dialog.dismiss()
                method()
            }.build().also {
                it.isCancelable = false
                it.show(childFragmentManager, TipsDialog::class.simpleName)
            }
    }
}