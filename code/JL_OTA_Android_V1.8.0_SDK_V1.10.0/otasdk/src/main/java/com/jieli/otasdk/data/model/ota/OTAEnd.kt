package com.jieli.otasdk.data.model.ota

import android.bluetooth.BluetoothDevice

/**
 * @author zqjasonZhong
 * @since 2022/9/9
 * @email zhongzhuocheng@zh-jieli.com
 * @desc OTA结束状态
 */
class OTAEnd(device: BluetoothDevice?, val code: Int, var message: String?) :
    OTAState(OTA_STATE_IDLE, device) {


    override fun toString(): String {
        return "OTAEnd(code=$code, message=$message)"
    }

}