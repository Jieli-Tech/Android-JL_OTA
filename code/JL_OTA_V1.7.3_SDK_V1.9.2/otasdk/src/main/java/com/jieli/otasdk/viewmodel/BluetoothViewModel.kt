package com.jieli.otasdk.viewmodel

import android.bluetooth.BluetoothDevice
import androidx.lifecycle.ViewModel
import com.jieli.otasdk.tool.bluetooth.BluetoothHelper
import com.jieli.otasdk.tool.config.ConfigHelper

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


    open fun isConnected(): Boolean {
        return bluetoothHelper.isConnected()
    }

    open fun isDeviceConnected(device: BluetoothDevice?): Boolean {
        return bluetoothHelper.isDeviceConnected(device)
    }

    open fun getConnectedDevice(): BluetoothDevice? {
        return bluetoothHelper.getConnectedDevice()
    }
}