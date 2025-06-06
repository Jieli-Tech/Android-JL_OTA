package com.jieli.otasdk.tool.ota

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.content.Context
import com.jieli.jl_bt_ota.constant.BluetoothConstant
import com.jieli.jl_bt_ota.constant.StateCode
import com.jieli.jl_bt_ota.impl.BluetoothOTAManager
import com.jieli.jl_bt_ota.impl.RcspAuth
import com.jieli.jl_bt_ota.model.BluetoothOTAConfigure
import com.jieli.jl_bt_ota.util.BluetoothUtil
import com.jieli.jl_bt_ota.util.CHexConver
import com.jieli.jl_bt_ota.util.JL_Log
import com.jieli.otasdk.tool.bluetooth.BluetoothHelper
import com.jieli.otasdk.tool.bluetooth.OnBTEventCallback
import com.jieli.otasdk.tool.config.ConfigHelper
import com.jieli.otasdk.tool.ota.spp.SppManager
import com.jieli.otasdk.util.AppUtil
import com.jieli.otasdk.data.constant.OtaConstant
import java.util.*

/**
 * 用于RCSP的第三方SDK接入OTA流程
 */
class OTAManager(context: Context) : BluetoothOTAManager(context) {

    private val configHelper = ConfigHelper.getInstance()
    private val bluetoothHelper = BluetoothHelper.getInstance()

    private val btEventCallback = object : OnBTEventCallback() {

        override fun onDeviceConnection(device: BluetoothDevice?, way: Int, status: Int) {
            super.onDeviceConnection(device, way, status)
            val connectionState = AppUtil.changeConnectStatus(status)
            JL_Log.i(
                TAG, "onDeviceConnection", "device : ${printDeviceInfo(device)}, way = $way, " +
                        "status ：$status, change status : $connectionState"
            )
            onBtDeviceConnection(device, connectionState)
        }

        override fun onReceiveData(
            device: BluetoothDevice?, way: Int, uuid: UUID?, data: ByteArray?
        ) {
            super.onReceiveData(device, way, uuid, data)
            JL_Log.d(
                TAG, "onReceiveData",
                "device : ${printDeviceInfo(device)}, way = $way," +
                        "\nuuid = $uuid, data ：${CHexConver.byte2HexStr(data)}"
            )
            if (way == OtaConstant.PROTOCOL_SPP && SppManager.UUID_SPP != uuid) {
                JL_Log.d(TAG, "onReceiveData", "skip spec")
                return
            }
            onReceiveDeviceData(device, data)
        }

        override fun onBleMtuChange(device: BluetoothDevice?, mtu: Int, status: Int) {
            super.onBleMtuChange(device, mtu, status)
            onMtuChanged(bluetoothHelper.getConnectedGatt(), mtu, status)
        }

    }

    init {
        val bluetoothOption = BluetoothOTAConfigure()
        //选择通讯方式
        bluetoothOption.priority = if (configHelper.isBleWay()) {
            BluetoothOTAConfigure.PREFER_BLE
        } else {
            BluetoothOTAConfigure.PREFER_SPP
        }
        //是否需要自定义回连方式(默认不需要，如需要自定义回连方式，需要客户自行实现)
        bluetoothOption.isUseReconnect =
            (configHelper.isUseCustomReConnectWay() && configHelper.isHidDevice())
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
        RcspAuth.setAuthTimeout(5000)
        bluetoothHelper.registerCallback(btEventCallback)
        if (bluetoothHelper.isConnected()) {
            onBtDeviceConnection(bluetoothHelper.getConnectedDevice(), StateCode.CONNECTION_OK)
            if (configHelper.isBleWay()) {
                onMtuChanged(
                    bluetoothHelper.getConnectedGatt(),
                    bluetoothHelper.getBleMtu() + 3,
                    BluetoothGatt.GATT_SUCCESS
                )
            }
        }
    }


    override fun getConnectedDevice(): BluetoothDevice? = bluetoothHelper.getConnectedDevice()

    override fun getConnectedBluetoothGatt(): BluetoothGatt? = bluetoothHelper.getConnectedGatt()

    override fun connectBluetoothDevice(bluetoothDevice: BluetoothDevice?) {
        //仅仅作为回连设备，回连设备现在仅支持BLE
        val result: Boolean = bluetoothHelper.connectBleDevice(bluetoothDevice)
        if (!result) {
            onBtDeviceConnection(bluetoothDevice, StateCode.CONNECTION_FAILED)
        }
    }

    override fun disconnectBluetoothDevice(bluetoothDevice: BluetoothDevice?) {
        bluetoothHelper.disconnectDevice(bluetoothDevice)
    }

    override fun sendDataToDevice(bluetoothDevice: BluetoothDevice?, bytes: ByteArray?): Boolean {
        JL_Log.d(
            TAG, "sendDataToDevice", "device : ${printDeviceInfo(bluetoothDevice)}\n"
                    + "data = [${CHexConver.byte2HexStr(bytes)}]"
        )
        return bluetoothHelper.writeDataToDevice(bluetoothDevice, bytes)
    }

    override fun release() {
        super.release()
        bluetoothHelper.unregisterCallback(btEventCallback)
    }

    fun setReconnectAddr(addr: String?) {
//        setReconnectAddress(addr)
    }

    private fun printDeviceInfo(device: BluetoothDevice?): String? {
        return BluetoothUtil.printBtDeviceInfo(context, device)
    }

}
