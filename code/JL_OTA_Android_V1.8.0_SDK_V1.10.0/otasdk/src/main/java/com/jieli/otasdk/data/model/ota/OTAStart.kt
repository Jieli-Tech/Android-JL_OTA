package com.jieli.otasdk.data.model.ota

import android.bluetooth.BluetoothDevice

/**
 * @author zqjasonZhong
 * @since 2022/9/9
 * @email zhongzhuocheng@zh-jieli.com
 * @desc OTA开始状态
 */
class OTAStart(device: BluetoothDevice?) : OTAState(OTA_STATE_START, device)