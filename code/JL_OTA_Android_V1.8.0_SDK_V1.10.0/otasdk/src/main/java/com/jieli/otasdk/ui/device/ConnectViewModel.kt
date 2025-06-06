package com.jieli.otasdk.ui.device

import android.bluetooth.BluetoothDevice
import android.os.SystemClock
import androidx.lifecycle.MutableLiveData
import com.jieli.jl_bt_ota.constant.StateCode
import com.jieli.jl_bt_ota.util.BluetoothUtil
import com.jieli.otasdk.MainApplication
import com.jieli.otasdk.data.constant.OtaConstant
import com.jieli.otasdk.data.model.ScanResult
import com.jieli.otasdk.data.model.device.ScanDevice
import com.jieli.otasdk.tool.bluetooth.OnBTEventCallback
import com.jieli.otasdk.tool.ota.ble.model.BleScanInfo
import com.jieli.otasdk.ui.base.BluetoothViewModel
import com.jieli.otasdk.util.AppUtil

/**
 * @author zqjasonZhong
 * @since 2022/9/8
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 连接设备逻辑实现
 */
class ConnectViewModel : BluetoothViewModel() {

    /**
     * 回调蓝牙状态
     */
    val bluetoothStateMLD = MutableLiveData<Boolean>()

    /**
     * 回调搜索结果
     */
    val scanResultMLD = MutableLiveData<ScanResult>()

    /**
     * 缓存的搜索设备列表
     */
    val scanDeviceList = mutableListOf<ScanDevice>()

    private val btEventCallback = object : OnBTEventCallback() {

        override fun onAdapterChange(bEnabled: Boolean) {
            bluetoothStateMLD.postValue(bEnabled)
        }

        override fun onDiscoveryChange(bStart: Boolean, scanType: Int) {
            scanResultMLD.value = ScanResult(
                if (bStart) {
                    ScanResult.SCAN_STATUS_SCANNING
                } else {
                    ScanResult.SCAN_STATUS_IDLE
                }
            )
            if (bStart) {
                getConnectedDevice()?.let { device ->
                    SystemClock.sleep(50)
                    scanResultMLD.value = ScanResult(ScanResult.SCAN_STATUS_FOUND_DEV,
                        ScanDevice(device, 0, ByteArray(0)).apply {
                            state = StateCode.CONNECTION_OK
                        })
                }
            }
        }

        override fun onDiscovery(device: BluetoothDevice?, bleScanMessage: BleScanInfo?) {
            if (null == device) return
            val data = bleScanMessage?.rawData ?: ByteArray(0)
            val result = ScanResult(
                ScanResult.SCAN_STATUS_FOUND_DEV,
                ScanDevice(device, bleScanMessage?.rssi ?: 0, data).apply {
                    state = getDeviceConnection(device)
                })
            scanResultMLD.value = result
        }
    }

    init {
        bluetoothHelper.registerCallback(btEventCallback)
    }

    override fun onCleared() {
        super.onCleared()
        destroy()
    }

    fun isSwitchOtaMode(): Boolean = configHelper.isEnableBroadcastBox()

    fun getScanFilter(): String? = configHelper.getScanFilter()

    fun setScanFilter(filter: String?) {
        configHelper.setScanFilter(filter)
    }

    fun isScanning(): Boolean = bluetoothHelper.isScanning()

    fun startScan() {
        if (!BluetoothUtil.isBluetoothEnable()) {
            AppUtil.enableBluetooth(getContext())
            return
        }
        if (isScanning()) return
        bluetoothHelper.startScan(OtaConstant.SCAN_TIMEOUT)
    }

    fun stopScan() {
        if (!isScanning()) return
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

    override fun destroy() {
        super.destroy()
        stopScan()
        bluetoothHelper.unregisterCallback(btEventCallback)
    }

}