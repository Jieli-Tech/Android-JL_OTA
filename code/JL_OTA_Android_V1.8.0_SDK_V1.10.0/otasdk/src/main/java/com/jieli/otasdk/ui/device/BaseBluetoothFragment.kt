package com.jieli.otasdk.ui.device

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
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
    private var callback: IActionCallback<Boolean>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        permissionHandler = {
            checkBluetoothEnvironment()
        }
        openBtLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                checkBluetoothEnvironment()
            }
        requestGPSLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                checkBluetoothEnvironment()
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
        checkBluetoothEnvironment()
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
        checkBluetoothEnvironment()
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

    fun tryToCheckBluetoothEnvironment(callback: IActionCallback<Boolean>) {
        if (this.callback != null) return
        this.callback = callback
        checkBluetoothEnvironment()
    }

    /**
     * 检查蓝牙环境
     * 1. 检查新蓝牙权限
     * 2. 检查定位权限
     * 3. 蓝牙是否打开
     * 4. 定位服务是否打开
     */
    private fun checkBluetoothEnvironment() {
        if (!PermissionUtil.hasBluetoothPermission(requireContext())) { //检查蓝牙新权限
            showPermissionTipsDialog("操作蓝牙设备，需要用户同意使用附近蓝牙设备")
            grantBluetoothPermissionWithPermissionCheck()
            return
        }
        if (!PermissionUtil.hasLocationPermission(requireContext())) { //检查定位权限
            showPermissionTipsDialog("操作蓝牙设备，需要用户授权使用位置信息")
            grantLocationPermissionWithPermissionCheck()
            return
        }
        if (!BluetoothUtil.isBluetoothEnable()) { //检查蓝牙是否打开
            showOperationTipsDialog(getString(R.string.open_bluetooth_tip)) {
                openBtLauncher.launch(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
            }
            return
        }
        if (!PermissionUtil.isLocationServiceEnabled(requireContext())) { //检查定位服务是否打开
            showOperationTipsDialog(getString(R.string.open_gpg_tip)) {
                requestGPSLauncher.launch(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            }
            return
        }
        onSuccess()
    }

    private fun onSuccess() {
        callback?.onSuccess(true)
        callback = null
    }

    private fun onFail() {
        callback?.onSuccess(false)
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