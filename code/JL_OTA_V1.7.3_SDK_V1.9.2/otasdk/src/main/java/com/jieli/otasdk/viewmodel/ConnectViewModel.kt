package com.jieli.otasdk.viewmodel

import android.bluetooth.BluetoothDevice
import androidx.lifecycle.MutableLiveData
import com.jieli.jl_bt_ota.util.BluetoothUtil
import com.jieli.jl_bt_ota.util.JL_Log
import com.jieli.otasdk.MainApplication
import com.jieli.otasdk.model.DeviceConnection
import com.jieli.otasdk.model.ScanDevice
import com.jieli.otasdk.model.ScanResult
import com.jieli.otasdk.tool.bluetooth.OnBTEventCallback
import com.jieli.otasdk.tool.ota.ble.model.BleScanInfo
import com.jieli.otasdk.util.AppUtil
import com.jieli.otasdk.util.OtaConstant

/**
 * @author zqjasonZhong
 * @since 2022/9/8
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 连接设备逻辑实现
 */
open class ConnectViewModel : BluetoothViewModel() {

    val bluetoothStateMLD = MutableLiveData<Boolean>()
    val scanResultMLD = MutableLiveData<ScanResult>()
    val deviceConnectionMLD = MutableLiveData<DeviceConnection>()

    private val btEventCallback = object : OnBTEventCallback() {

        override fun onAdapterChange(bEnabled: Boolean) {
            bluetoothStateMLD.postValue(bEnabled)
        }

        override fun onDiscoveryChange(bStart: Boolean, scanType: Int) {
            val result = if (bStart) {
                ScanResult(ScanResult.SCAN_STATUS_SCANNING)
            } else {
                ScanResult(ScanResult.SCAN_STATUS_IDLE)
            }
            scanResultMLD.value = result
        }

        override fun onDiscovery(device: BluetoothDevice?, bleScanMessage: BleScanInfo?) {
            val result = ScanResult(ScanResult.SCAN_STATUS_FOUND_DEV,
                device?.let {
                    val data = bleScanMessage?.rawData ?: ByteArray(0)
                    ScanDevice(it, bleScanMessage?.rssi ?: 0, data)
                })
            scanResultMLD.value = result
        }

        override fun onDeviceConnection(device: BluetoothDevice?, way: Int, status: Int) {
            deviceConnectionMLD.value =
                DeviceConnection(device, AppUtil.changeConnectStatus(status))
        }
    }

    init {
        bluetoothHelper.registerCallback(btEventCallback)
    }

    fun isScanning(): Boolean = bluetoothHelper.isScanning()

    fun startScan() {
        if (!BluetoothUtil.isBluetoothEnable()) {
            AppUtil.enableBluetooth(MainApplication.getInstance())
            return
        }
        bluetoothHelper.startScan(OtaConstant.SCAN_TIMEOUT)
    }

    fun stopScan() {
        bluetoothHelper.stopScan()
    }

    fun connectBtDevice(device: BluetoothDevice?) {
        if (null == device) return
        bluetoothHelper.connectDevice(device)
    }

    fun disconnectBtDevice(device: BluetoothDevice?) {
        if (null == device) return
        bluetoothHelper.disconnectDevice(device)
    }

    fun destroy() {
        stopScan()
        bluetoothHelper.unregisterCallback(btEventCallback)
    }

}