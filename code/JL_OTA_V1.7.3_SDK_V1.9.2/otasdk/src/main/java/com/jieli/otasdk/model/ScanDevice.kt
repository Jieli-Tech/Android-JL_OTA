package com.jieli.otasdk.model

import android.bluetooth.BluetoothDevice
import com.jieli.jl_bt_ota.constant.StateCode

/**
 * @author zqjasonZhong
 * @since 2022/9/8
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 扫描设备
 */
open class ScanDevice(val device: BluetoothDevice, var rssi: Int) {
    var data: ByteArray? = null
    var state: Int = StateCode.CONNECTION_DISCONNECT

    constructor(device: BluetoothDevice, rssi: Int, data: ByteArray?) : this(device, rssi){
        this.data = data
    }

    override fun hashCode(): Int {
        return device.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if(other !is ScanDevice) return false
        return device == other.device
    }
}