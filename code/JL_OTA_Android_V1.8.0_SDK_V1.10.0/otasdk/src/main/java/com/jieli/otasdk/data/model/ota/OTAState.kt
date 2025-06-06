package com.jieli.otasdk.data.model.ota

import android.bluetooth.BluetoothDevice
import com.jieli.otasdk.data.constant.OtaConstant

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

        fun printOTAState(state: Int): String = when (state) {
            OTA_STATE_IDLE -> "OTA_STATE_IDLE(0)"
            OTA_STATE_START -> "OTA_STATE_START(1)"
            OTA_STATE_WORKING -> "OTA_STATE_WORKING(2)"
            OTA_STATE_RECONNECT -> "OTA_STATE_RECONNECT(3)"
            else -> OtaConstant.formatString("OTA_STATE_UNKNOWN(%d)", state)
        }
    }

    override fun toString(): String {
        return "OTAState(state=${printOTAState(state)}, device=$device)"
    }
}