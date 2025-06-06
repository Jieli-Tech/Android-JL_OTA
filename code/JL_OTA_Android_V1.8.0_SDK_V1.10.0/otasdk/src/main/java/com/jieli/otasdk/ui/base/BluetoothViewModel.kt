package com.jieli.otasdk.ui.base

import android.bluetooth.BluetoothDevice
import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.jieli.jl_bt_ota.constant.StateCode
import com.jieli.jl_bt_ota.util.BluetoothUtil
import com.jieli.jl_bt_ota.util.JL_Log
import com.jieli.otasdk.MainApplication
import com.jieli.otasdk.data.model.device.DeviceConnection
import com.jieli.otasdk.tool.bluetooth.BluetoothHelper
import com.jieli.otasdk.tool.bluetooth.OnBTEventCallback
import com.jieli.otasdk.tool.config.ConfigHelper
import com.jieli.otasdk.util.AppUtil

/**
 * @author zqjasonZhong
 * @since 2022/9/8
 * @email zhongzhuocheng@zh-jieli.com
 * @desc  蓝牙逻辑处理
 */
open class BluetoothViewModel : ViewModel() {
    protected val tag: String = javaClass.simpleName
    protected val configHelper = ConfigHelper.getInstance()
    protected val bluetoothHelper = BluetoothHelper.getInstance()

    val deviceConnectionMLD = MutableLiveData<DeviceConnection>()

    private val btEventCallback = object : OnBTEventCallback() {

        override fun onDeviceConnection(device: BluetoothDevice?, way: Int, status: Int) {
            JL_Log.i(tag, "onDeviceConnection", "device : $device, status : $status, way : $way")
            deviceConnectionMLD.postValue(
                DeviceConnection(
                    device,
                    AppUtil.changeConnectStatus(status)
                )
            )
        }
    }

    init {
        bluetoothHelper.registerCallback(btEventCallback)
    }

    fun isConnected(): Boolean {
        return bluetoothHelper.isConnected()
    }

    fun isDeviceConnected(device: BluetoothDevice?): Boolean {
        return bluetoothHelper.isDeviceConnected(device)
    }

    fun getConnectedDevice(): BluetoothDevice? {
        return bluetoothHelper.getConnectedDevice()
    }

    fun getContext(): Context = MainApplication.instance

    fun getDeviceConnection(device: BluetoothDevice?): Int {
        if (null == device) return StateCode.CONNECTION_DISCONNECT
        if (isDeviceConnected(device)) return StateCode.CONNECTION_OK
        if (BluetoothUtil.deviceEquals(device, bluetoothHelper.getConnectingDevice())) {
            return StateCode.CONNECTION_CONNECTING
        }
        return StateCode.CONNECTION_DISCONNECT
    }

    open fun destroy() {
        bluetoothHelper.unregisterCallback(btEventCallback)
    }
}