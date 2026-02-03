package com.jieli.otasdk.data.model.ota

import android.bluetooth.BluetoothDevice

/**
 * @author zqjasonZhong
 * @since 2022/9/9
 * @email zhongzhuocheng@zh-jieli.com
 * @desc OTA回连状态
 */
class OTAReconnect(device: BluetoothDevice?, var reconnectAddress: String?, var isNewWay: Boolean) :
    OTAState(
        OTA_STATE_RECONNECT, device
    ) {

    override fun toString(): String {
        return "OTAReconnect(reconnectAddress=$reconnectAddress, isNewWay=$isNewWay)"
    }
}

