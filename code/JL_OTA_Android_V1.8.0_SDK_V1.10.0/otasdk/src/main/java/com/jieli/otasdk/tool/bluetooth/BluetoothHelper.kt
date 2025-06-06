package com.jieli.otasdk.tool.bluetooth

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothProfile
import com.jieli.jl_bt_ota.util.BluetoothUtil
import com.jieli.jl_bt_ota.util.CHexConver
import com.jieli.jl_bt_ota.util.JL_Log
import com.jieli.otasdk.tool.config.ConfigHelper
import com.jieli.otasdk.tool.ota.ble.BleManager
import com.jieli.otasdk.tool.ota.ble.interfaces.BleEventCallback
import com.jieli.otasdk.tool.ota.ble.model.BleScanInfo
import com.jieli.otasdk.tool.ota.spp.SppManager
import com.jieli.otasdk.tool.ota.spp.interfaces.SppEventCallback
import com.jieli.otasdk.util.AppUtil
import com.jieli.otasdk.data.constant.OtaConstant
import java.util.*

/**
 * @author zqjasonZhong
 * @since 2022/9/14
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 蓝牙操作辅助类
 */
class BluetoothHelper {

    private val configHelper = ConfigHelper.getInstance()
    private val bleManager = BleManager.getInstance()!!
    private val sppManager = SppManager.getInstance()!!
    private val btEventCbHelper = BTEventCbHelper()

    companion object {

        const val TAG: String = "BluetoothHelper"

        @Volatile
        private var instance: BluetoothHelper? = null

        fun getInstance(): BluetoothHelper = instance ?: synchronized(this) {
            instance ?: BluetoothHelper().also { instance = it }
        }
    }

    private val bleEventCallback = object : BleEventCallback() {

        override fun onAdapterChange(bEnabled: Boolean) {
            btEventCbHelper.onAdapterChange(bEnabled)
        }

        override fun onDiscoveryBleChange(bStart: Boolean) {
            btEventCbHelper.onDiscoveryChange(bStart, OtaConstant.PROTOCOL_BLE)
        }

        override fun onDiscoveryBle(device: BluetoothDevice?, bleScanMessage: BleScanInfo?) {
            btEventCbHelper.onDiscovery(device, bleScanMessage)
        }

        override fun onBleConnection(device: BluetoothDevice?, status: Int) {
            btEventCbHelper.onDeviceConnection(device, OtaConstant.PROTOCOL_BLE, status)
        }

        override fun onBleDataNotification(
            device: BluetoothDevice?,
            serviceUuid: UUID?,
            characteristicsUuid: UUID?,
            data: ByteArray?
        ) {
            btEventCbHelper.onReceiveData(
                device,
                OtaConstant.PROTOCOL_BLE,
                characteristicsUuid,
                data
            )
        }

        override fun onBleDataBlockChanged(device: BluetoothDevice?, block: Int, status: Int) {
            btEventCbHelper.onBleMtuChange(device, block, status)
        }
    }

    private val sppEventCallback = object : SppEventCallback() {

        override fun onAdapterChange(bEnabled: Boolean) {
            btEventCbHelper.onAdapterChange(bEnabled)
        }

        override fun onDiscoveryDeviceChange(bStart: Boolean) {
            btEventCbHelper.onDiscoveryChange(bStart, OtaConstant.PROTOCOL_SPP)
        }

        override fun onDiscoveryDevice(device: BluetoothDevice?, rssi: Int) {
            btEventCbHelper.onDiscovery(device, BleScanInfo().setRssi(rssi))
        }

        override fun onSppConnection(device: BluetoothDevice?, uuid: UUID?, status: Int) {
            if (status == BluetoothProfile.STATE_CONNECTED && configHelper.isUseMultiSppChannel()
                && UUID.fromString(configHelper.getCustomSppChannel()) != uuid
            ) {
                JL_Log.i(TAG, "onSppConnection", "skip custom uuid = $uuid")
                return
            }
            JL_Log.d(
                TAG, "onSppConnection",
                "device : ${printDeviceInfo(device)}, uuid = $uuid, status = $status"
            )
            btEventCbHelper.onDeviceConnection(device, OtaConstant.PROTOCOL_SPP, status)
        }

        override fun onReceiveSppData(device: BluetoothDevice?, uuid: UUID?, data: ByteArray?) {
            btEventCbHelper.onReceiveData(device, OtaConstant.PROTOCOL_SPP, uuid, data)
        }
    }

    init {
//        if (configHelper.isBleWay()) {
        bleManager.registerBleEventCallback(bleEventCallback)
//        } else {
        sppManager.registerSppEventCallback(sppEventCallback)
//        }
    }

    fun destroy() {
        bleManager.unregisterBleEventCallback(bleEventCallback)
        bleManager.destroy()
        sppManager.unregisterSppEventCallback(sppEventCallback)
        sppManager.release()
        btEventCbHelper.release()
        instance = null
    }

    fun registerCallback(callback: OnBTEventCallback) {
        btEventCbHelper.registerCallback(callback)
    }

    fun unregisterCallback(callback: OnBTEventCallback) {
        btEventCbHelper.unregisterCallback(callback)
    }

    fun isConnected(): Boolean {
        return getConnectedDevice() != null
    }

    fun isDeviceConnected(device: BluetoothDevice?): Boolean {
        return BluetoothUtil.deviceEquals(getConnectedDevice(), device)
    }

    fun isScanning(): Boolean = if (configHelper.isBleWay()) {
        bleManager.isBleScanning
    } else {
        sppManager.isScanning
    }

    fun isConnecting(): Boolean = if (configHelper.isBleWay()) {
        bleManager.isBleScanning
    } else {
        sppManager.isSppConnecting
    }

    fun getConnectedDevice(): BluetoothDevice? {
        return if (configHelper.isBleWay()) {
            bleManager.connectedBtDevice
        } else {
            sppManager.connectedSppDevice
        }
    }

    fun getConnectingDevice(): BluetoothDevice? {
        return if (configHelper.isBleWay()) {
            bleManager.connectingDevice
        } else {
            sppManager.connectingSppDevice
        }
    }

    fun getConnectedGatt(): BluetoothGatt? {
        return if (configHelper.isBleWay()) {
            bleManager.getConnectedBtGatt(getConnectedDevice())
        } else {
            null
        }
    }

    fun getBleMtu(): Int {
        return if (configHelper.isBleWay()) {
            return bleManager.getBleMtu(getConnectedDevice())
        } else {
            20
        }
    }

    fun startScan(timeout: Long): Boolean {
        return if (configHelper.isBleWay()) {
            bleManager.startLeScan(timeout)
        } else {
            sppManager.startDeviceScan(timeout)
        }
    }

    fun stopScan() {
        if (configHelper.isBleWay()) {
            bleManager.stopLeScan()
        } else {
            sppManager.stopDeviceScan()
        }
    }

    fun connectDevice(device: BluetoothDevice?): Boolean =
        if (configHelper.isBleWay()) {
            bleManager.connectBleDevice(device)
        } else {
            sppManager.connectSpp(device)
        }

    fun disconnectDevice(device: BluetoothDevice?) {
        if (configHelper.isBleWay()) {
            bleManager.disconnectBleDevice(device)
        } else {
            sppManager.disconnectSpp(device, null)
        }
    }

    fun connectBleDevice(device: BluetoothDevice?): Boolean =
        bleManager.connectBleDevice(device)

    fun reconnectDevice(address: String?, isUseNewAdv: Boolean) {
        if (configHelper.isBleWay()) {
            bleManager.reconnectDevice(address, isUseNewAdv)
        } else {
            //TODO:需要增加SPP自定义回连方式
        }
    }

    fun writeDataToDevice(bluetoothDevice: BluetoothDevice?, byteArray: ByteArray?): Boolean {
        if (null == bluetoothDevice || null == byteArray || byteArray.isEmpty()) return false
        if (bleManager.connectedBtDevice != null) {//目前连接的设备是ble
            bleManager.writeDataByBleAsync(
                bluetoothDevice,
                BleManager.BLE_UUID_SERVICE,
                BleManager.BLE_UUID_WRITE,
                byteArray
            ) { device, _, _, result, data ->
                JL_Log.d(
                    TAG, "writeDataByBleAsync",
                    "device : ${printDeviceInfo(device)}, result : $result,\n" +
                            "data : [${CHexConver.byte2HexStr(data)}]"
                )
            }
        } else if (sppManager.connectedSppDevice != null) {//目前连接的设备是Spp
            sppManager.writeDataToSppAsync(
                bluetoothDevice,
                SppManager.UUID_SPP,
                byteArray
            ) { device, sppUUID, result, data ->
                JL_Log.d(
                    TAG,
                    "writeDataToSppAsync",
                    "device : ${printDeviceInfo(device)}, uuid : $sppUUID, result : $result," +
                            "\ndata : ${CHexConver.byte2HexStr(data)}"
                )
            }
        }
        return true
    }

    private fun printDeviceInfo(device: BluetoothDevice?): String? {
        return AppUtil.printBtDeviceInfo(device)
    }

}