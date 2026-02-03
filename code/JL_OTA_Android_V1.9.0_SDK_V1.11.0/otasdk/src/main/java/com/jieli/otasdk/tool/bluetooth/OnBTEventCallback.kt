package com.jieli.otasdk.tool.bluetooth

import android.bluetooth.BluetoothDevice
import com.jieli.otasdk.tool.ota.ble.model.BleScanInfo
import java.util.*

/**
 * @author zqjasonZhong
 * @since 2022/9/14
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 蓝牙事件回调
 */
open class OnBTEventCallback {

    open fun onAdapterChange(bEnabled: Boolean) {

    }

    open fun onDiscoveryChange(bStart: Boolean, scanType: Int) {

    }

    open fun onDiscovery(device: BluetoothDevice?, bleScanMessage: BleScanInfo?) {

    }

    open fun onDeviceConnection(device: BluetoothDevice?, way: Int, status: Int) {

    }

    open fun onReceiveData(device: BluetoothDevice?, way: Int, uuid: UUID?, data: ByteArray?) {

    }

    open fun onBleMtuChange(device: BluetoothDevice?, mtu: Int, status: Int) {

    }
}