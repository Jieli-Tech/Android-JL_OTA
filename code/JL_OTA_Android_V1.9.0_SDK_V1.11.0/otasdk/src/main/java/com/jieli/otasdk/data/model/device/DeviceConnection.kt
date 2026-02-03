package com.jieli.otasdk.data.model.device

import android.bluetooth.BluetoothDevice

/**
 * @author zqjasonZhong
 * @since 2022/9/9
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 设备连接状态
 */
class DeviceConnection(val device: BluetoothDevice?, var state: Int) {

    override fun hashCode(): Int {
        return device.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (other !is DeviceConnection) return false
        return device == other.device
    }

    override fun toString(): String {
        return "DeviceConnection(device=$device, state=$state)"
    }

}