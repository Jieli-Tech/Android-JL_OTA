package com.jieli.broadcastbox.viewmodel

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.jieli.broadcastbox.model.BroadcastBoxInfo
import com.jieli.otasdk.data.model.device.DeviceConnection
import com.jieli.otasdk.data.model.device.ScanDevice
import com.jieli.otasdk.data.model.ScanResult
import com.jieli.otasdk.tool.ota.ble.BleManager
import com.jieli.otasdk.tool.ota.ble.interfaces.BleEventCallback
import com.jieli.otasdk.tool.ota.ble.model.BleScanInfo
import com.jieli.otasdk.data.constant.OtaConstant
import java.util.*

/**
 * Des:
 * author: Bob
 * date: 2022/12/02
 * Copyright: Jieli Technology
 * Modify date:
 * Modified by:
 */
class BroadcastBoxViewModel : ViewModel() {
    private val tag = javaClass.simpleName
    private val bleManager: BleManager = BleManager.getInstance()

    val cacheAdvInfo: HashMap<String, BroadcastBoxInfo> = HashMap()
    val connectedBleDevices: MutableList<BroadcastBoxInfo> =
        Collections.synchronizedList(ArrayList())
    val selectedDeviceList: MutableList<BroadcastBoxInfo> =
        Collections.synchronizedList(ArrayList())
    var isFilterDevice = true
    var isAddObserver = false

    val bluetoothStateMLD = MutableLiveData<Boolean>()
    val scanResultMLD = MutableLiveData<ScanResult>()
    lateinit var deviceConnectionMLD: MutableLiveData<DeviceConnection>

    private fun isScanning(): Boolean {
        return bleManager.isBleScanning
    }

    fun startScan() {
        if (!isScanning()) {
            bleManager.startLeScan(OtaConstant.SCAN_TIMEOUT)
        }
    }

    fun stopScan() {
        bleManager.stopLeScan()
    }

    fun isConnectedDevice(device: BluetoothDevice?): Boolean {
        return bleManager.isConnectedDevice(device)
    }

    fun getConnectedDevices(): MutableList<BluetoothDevice> {
        return bleManager.connectedDeviceList
    }

    fun connectBle(device: BluetoothDevice?) {
        bleManager.connectBleDevice(device)
    }

    fun disconnectBle(device: BluetoothDevice?) {
        bleManager.disconnectBleDevice(device)
    }

    fun destroy() {
        bleManager.unregisterBleEventCallback(bleEventCallback)
        selectedDeviceList.clear()
        connectedBleDevices.clear()
        cacheAdvInfo.clear()
    }

    fun findCacheAdvMessage(address: String?): BroadcastBoxInfo? {
        if (!BluetoothAdapter.checkBluetoothAddress(address)) return null
        return cacheAdvInfo[address]
    }

    private val bleEventCallback = object : BleEventCallback() {

        override fun onAdapterChange(bEnabled: Boolean) {
            bluetoothStateMLD.value = bEnabled
        }

        override fun onDiscoveryBleChange(bStart: Boolean) {
            val result = if (bStart) {
                ScanResult(ScanResult.SCAN_STATUS_SCANNING)
            } else {
                ScanResult(ScanResult.SCAN_STATUS_IDLE)
            }
            scanResultMLD.postValue(result)
        }

        override fun onDiscoveryBle(device: BluetoothDevice?, bleScanMessage: BleScanInfo?) {
            val result = ScanResult(
                ScanResult.SCAN_STATUS_FOUND_DEV,
                device?.let {
                    val data = bleScanMessage?.rawData ?: ByteArray(0)
                    ScanDevice(it, bleScanMessage?.rssi ?: 0, data)
                })
            scanResultMLD.value = result
        }

        override fun onBleConnection(device: BluetoothDevice?, status: Int) {
            deviceConnectionMLD.value = DeviceConnection(device, status)
        }
    }

    init {
        deviceConnectionMLD = MutableLiveData<DeviceConnection>()
        bleManager.registerBleEventCallback(bleEventCallback)
    }
}