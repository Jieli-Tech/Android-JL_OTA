package com.jieli.otasdk.ui.device

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.util.Consumer
import com.jieli.jl_bt_ota.interfaces.IActionCallback
import com.jieli.jl_bt_ota.util.BluetoothUtil
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
 * @ClassName:      BaseBluetoothSanFragment
 * @Description:    蓝牙扫描权限
 * @Author:         ZhangHuanMing
 * @CreateDate:     2022/11/7 10:58
 */
@RuntimePermissions
open class BaseBluetoothFragment : BaseFragment() {

    /**
     * 打开蓝牙意图
     */
    private lateinit var openBtLauncher: ActivityResultLauncher<Intent>

    /**
     * 请求位置服务意图
     */
    private lateinit var requestGPSLauncher: ActivityResultLauncher<Intent>

    /**
     * 操作回调
     */
    private var callback: Consumer<Boolean>? = null

    /**
     * 是否用户操作
     */
    private var isUser: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        permissionHandler = {
            checkBluetoothEnvironment(isUser)
        }
        openBtLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                checkBluetoothEnvironment(isUser)
            }
        requestGPSLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                checkBluetoothEnvironment(isUser)
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

    @NeedsPermission(
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION,
    )
    fun grantLocationPermission() {
        dismissPermissionTipsDialog()
        checkBluetoothEnvironment(isUser)
    }

    @OnShowRationale(
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION,
    )
    fun showRationaleForLocationPermission(request: PermissionRequest) {
        request.proceed()
    }

    @OnNeverAskAgain(
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION,
    )
    fun onLocationPermissionsNeverAskAgain() {
        onLocationPermissionsDenied()
    }

    @OnPermissionDenied(
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION,
    )
    fun onLocationPermissionsDenied() {
        dismissPermissionTipsDialog()
        showOperationTipsDialog(getString(R.string.grant_location_permission)) {
            goToAppDetailsSettings()
        }
    }


    @NeedsPermission(
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_CONNECT
    )
    fun grantBluetoothPermission() {
        dismissPermissionTipsDialog()
        checkBluetoothEnvironment(isUser)
    }

    @OnShowRationale(
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_CONNECT
    )
    fun showRationaleForBluetoothPermission(request: PermissionRequest) {
        request.proceed()
    }

    @OnNeverAskAgain(
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_CONNECT
    )
    fun onBluetoothPermissionsNeverAskAgain() {
        onBluetoothPermissionsDenied()
    }

    @OnPermissionDenied(
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_CONNECT
    )
    fun onBluetoothPermissionsDenied() {
        dismissPermissionTipsDialog()
        showOperationTipsDialog(getString(R.string.grant_bluetooth_permission)) {
            goToAppDetailsSettings()
        }
    }

    fun tryToCheckBluetoothEnvironment(isUser: Boolean, callback: Consumer<Boolean>) {
        if (this.callback != null) return
        this.callback = callback
        checkBluetoothEnvironment(isUser)
    }

    /**
     * 检查蓝牙环境
     * 1. 检查新蓝牙权限
     * 2. 检查定位权限
     * 3. 蓝牙是否打开
     * 4. 定位服务是否打开
     */
    private fun checkBluetoothEnvironment(isUser: Boolean) {
        this.isUser = isUser
        if (!PermissionUtil.hasBluetoothPermission(requireContext())) { //检查蓝牙新权限
            showPermissionTipsDialog(getString(R.string.grant_bluetooth_permission))
            grantBluetoothPermissionWithPermissionCheck()
            return
        }
        if (!PermissionUtil.hasLocationPermission(requireContext())) { //检查定位权限
            showPermissionTipsDialog(getString(R.string.grant_location_permission))
            grantLocationPermissionWithPermissionCheck()
            return
        }
        if (!BluetoothUtil.isBluetoothEnable()) { //检查蓝牙是否打开
            if (isUser) {
                showOperationTipsDialog(getString(R.string.open_bluetooth_tip)) {
                    openBtLauncher.launch(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
                }
                return
            }
            onFail()
            return
        }
        if (!PermissionUtil.isLocationServiceEnabled(requireContext())) { //检查定位服务是否打开
            if (isUser) {
                showOperationTipsDialog(getString(R.string.open_gpg_tip)) {
                    requestGPSLauncher.launch(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                }
                return
            }
            onFail()
            return
        }
        onSuccess()
    }

    private fun onSuccess() {
        isUser = false
        callback?.accept(true)
        callback = null
    }

    private fun onFail() {
        isUser = false
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