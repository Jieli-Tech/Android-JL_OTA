package com.jieli.otasdk.model.ota

import android.bluetooth.BluetoothDevice

/**
 * @author zqjasonZhong
 * @since 2022/9/9
 * @email zhongzhuocheng@zh-jieli.com
 * @desc OTA状态
 */
open class OTAState(val state: Int = OTA_STATE_IDLE, var device: BluetoothDevice?) {

    companion object {

        const val OTA_STATE_IDLE = 0
        const val OTA_STATE_START = 1
        const val OTA_STATE_WORKING = 2
        const val OTA_STATE_RECONNECT = 3
    }

    override fun toString(): String {
        return "OTAState(state=$state, device=$device)"
    }
}