package com.jieli.otasdk.data.model.ota

import android.bluetooth.BluetoothDevice

/**
 * @author zqjasonZhong
 * @since 2022/9/9
 * @email zhongzhuocheng@zh-jieli.com
 * @desc OTA工作状态
 */
class OTAWorking(device: BluetoothDevice?, var type: Int, var progress: Float) :
    OTAState(OTA_STATE_WORKING, device) {

    override fun toString(): String {
        return "OTAWorking(type=$type, progress=$progress)"
    }
}