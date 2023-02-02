package com.jieli.otasdk.fragments

import android.Manifest
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.DialogFragment
import com.jieli.jl_bt_ota.util.BluetoothUtil
import com.jieli.jl_dialog.Jl_Dialog
import com.jieli.otasdk.R
import com.jieli.otasdk.base.BaseFragment
import com.jieli.otasdk.util.AppUtil
import permissions.dispatcher.*

/**
 *
 * @ClassName:      BaseBluetoothSanFragment
 * @Description:    蓝牙扫描权限
 * @Author:         ZhangHuanMing
 * @CreateDate:     2022/11/7 10:58
 */
@RuntimePermissions
open class BaseBluetoothSanFragment : BaseFragment() {
    private var notifyDialog: Jl_Dialog? = null
    private lateinit var requestGPSResult: ActivityResultLauncher<Intent>
    private lateinit var grantBluetoothPermissionResult: ActivityResultLauncher<Intent>
    private lateinit var grantLocationPermissionResult: ActivityResultLauncher<Intent>

    private val callbacks = ArrayList<OnCheckBluetoothEnvironmentCallback>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestGPSResult =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                checkBluetoothEnvironment()
            }
        grantBluetoothPermissionResult =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                checkBluetoothEnvironment()
            }
        grantLocationPermissionResult =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                checkBluetoothEnvironment()
            }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        onRequestPermissionsResult(requestCode, grantResults)
    }

    fun registerOnCheckBluetoothEnvironmentCallback(callback: OnCheckBluetoothEnvironmentCallback) {
        callbacks.add(callback)
    }

    fun unregisterOnCheckBluetoothEnvironmentCallback(callback: OnCheckBluetoothEnvironmentCallback) {
        callbacks.remove(callback)
    }

    /**
     * 检查蓝牙环境
     * 1.蓝牙是否打开
     * 2.定位是否打开
     * 3.检查新蓝牙权限
     * 4.定位权限
     */
    fun checkBluetoothEnvironment(callback: OnCheckBluetoothEnvironmentCallback? = null): Unit {
        callback?.let {
            registerOnCheckBluetoothEnvironmentCallback(it)
        }
        if (!BluetoothUtil.isBluetoothEnable()) {//蓝牙是否打开
            showNotifyBluetoothDialog()
        } else if (!checkGpsProviderEnable(context)) {//检查gps是否打开
            showNotifyGPSDialog()
        } else if (!checkBluetoothPermission()) {//检查蓝牙权限
            grantBluetoothPermissionWithPermissionCheck()
        } else {
            grantLocationPermissionWithPermissionCheck()
        }
    }

    private fun onCheckBluetoothEnvironmentSuccess(): Unit {
        val tempList: ArrayList<OnCheckBluetoothEnvironmentCallback> =
            callbacks.clone() as ArrayList<OnCheckBluetoothEnvironmentCallback>
        tempList.forEach {
            it.onSuccess()
        }
    }

    private fun onCheckBluetoothEnvironmentFailed(): Unit {
        val tempList: ArrayList<OnCheckBluetoothEnvironmentCallback> =
            callbacks.clone() as ArrayList<OnCheckBluetoothEnvironmentCallback>
        tempList.forEach {
            it.onFailed()
        }
    }

    @NeedsPermission(
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION,
    )
    fun grantLocationPermission() {
        onCheckBluetoothEnvironmentSuccess()
    }

    @OnShowRationale(
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION,
    )
    fun showRationaleForLocationPermission(request: PermissionRequest) {
//        Log.e(TAG, "showRationaleForLocationPermission: 显示定位权限细节" )
        request.proceed()
    }

    @OnNeverAskAgain(
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION,
    )
    fun onLocationPermissionsNeverAskAgain() {
        showNotifyLocationPermissionDialog()
        Log.e(TAG, "onLocationPermissionsNeverAskAgain: 请到系统设备打开")
    }

    @OnPermissionDenied(
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION,
    )
    fun onLocationPermissionsDenied() {//
        onCheckBluetoothEnvironmentFailed()
        Log.e(TAG, "onLocationPermissionsDenied: 获取权限被拒绝")
    }

    @NeedsPermission(
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_CONNECT
    )
    fun grantBluetoothPermission(): Unit {
        Log.e(TAG, "grantBluetoothPermission: 获取蓝牙权限成功")
    }

    @OnShowRationale(
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_CONNECT
    )
    fun showRationaleForBluetoothPermission(request: PermissionRequest) {
//        Log.e(TAG, "showRationaleForBluetoothPermission: 显示蓝牙权限细节" )
        request.proceed()
    }

    @OnNeverAskAgain(
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_CONNECT
    )
    fun onBluetoothPermissionsNeverAskAgain(): Unit {
        showNotifyBluetoothPermissionDialog()
        Log.e(TAG, "onBluetoothPermissionsNeverAskAgain: 请到系统设备打开")
    }

    @OnPermissionDenied(
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_CONNECT
    )
    fun onBluetoothPermissionsDenied() {//
        onCheckBluetoothEnvironmentFailed()
        Log.e(TAG, "onBluetoothPermissionsDenied: 获取权限被拒绝")
    }

    /**
     * 检查GPS位置功能是否使能
     *
     * @param context 上下文
     * @return 结果
     */
    private fun checkGpsProviderEnable(context: Context?): Boolean {
        if (context == null) return false
        val locManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    /**
     * 显示打开蓝牙提示框
     */
    private fun showNotifyBluetoothDialog(): Unit {
        if (!isAdded || isDetached) return
        if (notifyDialog == null) {
            notifyDialog = Jl_Dialog.Builder()
                .title(getString(R.string.tips))
                .content(getString(R.string.open_bluetooth_tip))
                .cancel(false)
                .left(getString(R.string.cancel))
                .leftColor(resources.getColor(R.color.gray_text_444444))
                .right(getString(R.string.to_setting))
                .rightColor(resources.getColor(R.color.red_FF688C))
                .leftClickListener { _: View?, _: DialogFragment? ->
                    dismissNotifyDialog()//  不打开蓝牙
                    onCheckBluetoothEnvironmentFailed()
                }
                .rightClickListener { _: View?, _: DialogFragment? ->
                    dismissNotifyDialog()
                    //todo 这里是否要回来继续处理
                    AppUtil.enableBluetooth(context)
                }
                .build()
        }
        if (!this.notifyDialog?.isShow!!) {
            this.notifyDialog?.show(parentFragmentManager, "notify_gps_dialog")
        }
    }

    /**
     * 显示打开定位服务(gps)提示框
     */
    private fun showNotifyGPSDialog() {
        if (!isAdded || isDetached) return
        if (notifyDialog == null) {
            notifyDialog = Jl_Dialog.Builder()
                .title(getString(R.string.tips))
                .content(getString(R.string.open_gpg_tip))
                .cancel(false)
                .left(getString(R.string.cancel))
                .leftColor(resources.getColor(R.color.gray_text_444444))
                .right(getString(R.string.to_setting))
                .rightColor(resources.getColor(R.color.red_FF688C))
                .leftClickListener { _: View?, _: DialogFragment? ->
                    dismissNotifyDialog()//不打开GPS
                    onCheckBluetoothEnvironmentFailed()
                }
                .rightClickListener { _: View?, _: DialogFragment? ->
                    dismissNotifyDialog()
                    requestGPSResult.launch(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                }
                .build()
        }
        if (!this.notifyDialog?.isShow!!) {
            this.notifyDialog?.show(parentFragmentManager, "notify_gps_dialog")
        }
    }

    /**
     * 显示需要跳转设置打开蓝牙权限的提示窗
     */
    private fun showNotifyBluetoothPermissionDialog(): Unit {
        if (!isAdded || isDetached) return
        if (notifyDialog == null) {
            notifyDialog = Jl_Dialog.Builder()
                .title(getString(R.string.tips))
                .content(getString(R.string.grant_bluetooth_permission))
                .cancel(false)
                .left(getString(R.string.cancel))
                .leftColor(resources.getColor(R.color.gray_text_444444))
                .right(getString(R.string.to_setting))
                .rightColor(resources.getColor(R.color.red_FF688C))
                .leftClickListener { _: View?, _: DialogFragment? ->
                    dismissNotifyDialog()//不打开GPS
                    onCheckBluetoothEnvironmentFailed()
                }
                .rightClickListener { _: View?, _: DialogFragment? ->
                    dismissNotifyDialog()
                    Intent().let {
                        it.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        it.data = Uri.parse("package:"+context?.packageName)
                        grantBluetoothPermissionResult.launch(it)
                    }
                }
                .build()
        }
        if (!this.notifyDialog?.isShow!!) {
            this.notifyDialog?.show(
                parentFragmentManager,
                "notify_grant_bluetooth_permission_dialog"
            )
        }
    }

    /**
     * 显示需要跳转设置打开定位权限的提示窗
     */
    private fun showNotifyLocationPermissionDialog(): Unit {
        if (!isAdded || isDetached) return
        if (notifyDialog == null) {
            notifyDialog = Jl_Dialog.Builder()
                .title(getString(R.string.tips))
                .content(getString(R.string.grant_location_permission))
                .cancel(false)
                .left(getString(R.string.cancel))
                .leftColor(resources.getColor(R.color.gray_text_444444))
                .right(getString(R.string.to_setting))
                .rightColor(resources.getColor(R.color.red_FF688C))
                .leftClickListener { _: View?, _: DialogFragment? ->
                    dismissNotifyDialog()//不打开GPS
                    onCheckBluetoothEnvironmentFailed()
                }
                .rightClickListener { _: View?, _: DialogFragment? ->
                    dismissNotifyDialog()
                    Intent().let {
                        it.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        it.data = Uri.parse("package:"+context?.packageName)
                        grantBluetoothPermissionResult.launch(it)
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

    private fun checkBluetoothPermission(): Boolean {
        return AppUtil.checkHasConnectPermission(context)
                && AppUtil.checkHasScanPermission(context)
    }
}

public interface OnCheckBluetoothEnvironmentCallback {
    fun onSuccess();
    fun onFailed()
}