package com.jieli.otasdk.data.model.device

import android.bluetooth.BluetoothDevice
import com.jieli.jl_bt_ota.constant.StateCode

/**
 * @author zqjasonZhong
 * @since 2022/9/8
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 扫描设备
 */
open class ScanDevice @JvmOverloads constructor(val device: BluetoothDevice, var rssi: Int, var data: ByteArray? = null) {
    var state: Int = StateCode.CONNECTION_DISCONNECT

    open fun isDevConnected(): Boolean = state == StateCode.CONNECTION_OK

    override fun hashCode(): Int {
        return device.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (other !is ScanDevice) return false
        return device == other.device
    }

    override fun toString(): String {
        return "ScanDevice(device=$device, rssi=$rssi, data=${data?.contentToString()}, state=$state)"
    }
}