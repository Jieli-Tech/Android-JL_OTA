package com.jieli.broadcastbox.multidevice

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothProfile
import android.content.Context
import com.jieli.jl_bt_ota.constant.BluetoothConstant
import com.jieli.jl_bt_ota.constant.StateCode
import com.jieli.jl_bt_ota.impl.BluetoothOTAManager
import com.jieli.jl_bt_ota.interfaces.IUpgradeCallback
import com.jieli.jl_bt_ota.model.BluetoothOTAConfigure
import com.jieli.jl_bt_ota.model.base.BaseError
import com.jieli.jl_bt_ota.util.BluetoothUtil
import com.jieli.jl_bt_ota.util.CHexConver
import com.jieli.jl_bt_ota.util.JL_Log
import com.jieli.otasdk.tool.config.ConfigHelper
import com.jieli.otasdk.tool.ota.ble.BleManager
import com.jieli.otasdk.tool.ota.ble.interfaces.BleEventCallback
import com.jieli.otasdk.util.AppUtil
import java.util.*

/**
 * 多设备OTA管理类
 */
class MultiOTAManager(context: Context, address: String?) : BluetoothOTAManager(context) {

    /**
     * 目标设备升级地址
     * <p>
     *     Note: 如果升级设备在升级过程中会改变地址，该属性会改变。反之，则不会变化。
     * </p>
     */
    var otaAddress: String? = null    //目标设备升级地址
        private set

    /**
     * 原始设备变化地址
     * <p>
     *     Note: 该属性为输入设备地址，不会改变
     * </p>
     */
    var srcAddress: String? = null    //原始设备变化地址
        private set

    private val configHelper = ConfigHelper.getInstance()
    private val bleManager = BleManager.getInstance()     //只处理BLE

    private val bleEventCallback = object : BleEventCallback() {

        override fun onBleConnection(device: BluetoothDevice?, status: Int) {
            device?.let {
                if (status == BluetoothProfile.STATE_CONNECTING && bleManager.isMatchReConnectDevice(
                        otaAddress,
                        it.address
                    )
                ) {
                    if (otaAddress != it.address) {
                        otaAddress = it.address
                        JL_Log.i(
                            TAG,
                            "onBleConnection >>> change address. device : ${printDeviceInfo(device)}, otaAddress ：$otaAddress"
                        )
                    }
                }
                if (it.address == otaAddress) {
                    val connectionState = AppUtil.changeConnectStatus(status)
                    JL_Log.i(
                        TAG,
                        "onBleConnection >>> device : ${printDeviceInfo(device)}, status ：$status, change status : $connectionState"
                    )
                    onBtDeviceConnection(device, connectionState)
                }
            }
        }

        override fun onBleDataNotification(
            device: BluetoothDevice?,
            serviceUuid: UUID?,
            characteristicsUuid: UUID?,
            data: ByteArray?
        ) {
            device?.let {
                if (it.address == otaAddress) {
                    JL_Log.d(
                        TAG,
                        "onBleDataNotification >>> ${printDeviceInfo(device)}, serviceUuid = $serviceUuid, characteristicsUuid = $characteristicsUuid, " +
                                "data ：${CHexConver.byte2HexStr(data)} "
                    )
                    onReceiveDeviceData(device, data)
                }
            }
        }

        override fun onBleDataBlockChanged(device: BluetoothDevice?, block: Int, status: Int) {
            device?.let {
                if (it.address == otaAddress) {
                    onMtuChanged(bleManager.getConnectedBtGatt(device), block, status)
                }
            }
        }
    }

    init {
        val bluetoothOption = BluetoothOTAConfigure()
        //选择通讯方式
        bluetoothOption.priority = BluetoothOTAConfigure.PREFER_BLE
        //是否需要自定义回连方式(默认不需要，如需要自定义回连方式，需要客户自行实现)
        bluetoothOption.isUseReconnect = true
        //是否启用设备认证流程(与固件工程师确认)
        bluetoothOption.isUseAuthDevice = configHelper.isUseDeviceAuth()
        //设置BLE的MTU
        bluetoothOption.mtu = BluetoothConstant.BLE_MTU_MIN
        //是否需要改变BLE的MTU
        bluetoothOption.isNeedChangeMtu = false
        //是否启用杰理服务器(暂时不支持)
        bluetoothOption.isUseJLServer = false

        //配置OTA参数
        configure(bluetoothOption)

        srcAddress = address
        otaAddress = srcAddress
        bleManager.registerBleEventCallback(bleEventCallback)
        if (connectedDevice != null) {
            onBtDeviceConnection(connectedDevice, StateCode.CONNECTION_OK)
            onMtuChanged(
                connectedBluetoothGatt,
                bleManager.getBleMtu(connectedDevice) + 3,
                BluetoothGatt.GATT_SUCCESS
            )
        }
    }

    fun isInitOk(): Boolean {
        return connectedDevice != null && deviceInfo != null
    }

    override fun startOTA(callback: IUpgradeCallback?) {
        super.startOTA(CustomOTACallback(callback))
    }

    override fun getConnectedDevice(): BluetoothDevice? =
        bleManager.getConnectedBLEDevice(otaAddress)

    override fun getConnectedBluetoothGatt(): BluetoothGatt? =
        bleManager.getConnectedBtGatt(connectedDevice)

    override fun connectBluetoothDevice(bluetoothDevice: BluetoothDevice?) {
        bluetoothDevice?.let {
            if (it.address != otaAddress) {
                otaAddress = it.address
            }
        }
        val result: Boolean = bleManager.connectBleDevice(bluetoothDevice)
        if (!result) {
            onBtDeviceConnection(bluetoothDevice, StateCode.CONNECTION_FAILED)
        }
    }

    override fun disconnectBluetoothDevice(bluetoothDevice: BluetoothDevice?) {
        bleManager.disconnectBleDevice(bluetoothDevice)
    }

    override fun sendDataToDevice(bluetoothDevice: BluetoothDevice?, bytes: ByteArray?): Boolean {
        if (null == connectedDevice || null == bluetoothDevice) return false;
        JL_Log.d(
            TAG,
            "sendDataToDevice : device = ${printDeviceInfo(bluetoothDevice)}\n" +
                    "data = [${CHexConver.byte2HexStr(bytes)}]"
        )
        bleManager.writeDataByBleAsync(
            bluetoothDevice,
            BleManager.BLE_UUID_SERVICE,
            BleManager.BLE_UUID_WRITE,
            bytes
        ) { device, serviceUUID, characteristicUUID, result, data ->
            JL_Log.i(
                TAG,
                "-writeDataByBleAsync- device:${
                    printDeviceInfo(device)
                }, result = $result"
            )
        }
        return true
    }

    override fun release() {
        super.release()
        bleManager.unregisterBleEventCallback(bleEventCallback)
    }

    fun setReconnectAddr(addr: String?) {
//        setReconnectAddress(addr)
    }

    private fun printDeviceInfo(device: BluetoothDevice?): String? {
        return BluetoothUtil.printBtDeviceInfo(context, device)
    }

    inner class CustomOTACallback(callback: IUpgradeCallback?) : IUpgradeCallback {
        private val mCallback = callback

        override fun onStartOTA() {
            mCallback?.onStartOTA()
        }

        override fun onNeedReconnect(addr: String?, isNewReconnectWay: Boolean) {
            bleManager.reconnectDevice(addr, isNewReconnectWay)
            mCallback?.onNeedReconnect(addr, isNewReconnectWay)
        }

        override fun onProgress(type: Int, progress: Float) {
            mCallback?.onProgress(type, progress)
        }

        override fun onStopOTA() {
            if (srcAddress != null && srcAddress != otaAddress) { //目标地址修改过，重置
                otaAddress = srcAddress
            }
            mCallback?.onStopOTA()
        }

        override fun onCancelOTA() {
            bleManager.disconnectBleDevice(connectedDevice)
            mCallback?.onCancelOTA()
        }

        override fun onError(error: BaseError?) {
            mCallback?.onError(error)
        }
    }

}
