package com.jieli.broadcastbox.multidevice

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.Message

import com.jieli.jl_bt_ota.constant.JL_Constant
import com.jieli.jl_bt_ota.model.BleScanMessage
import com.jieli.jl_bt_ota.tool.DeviceReConnectManager
import com.jieli.jl_bt_ota.util.BluetoothUtil
import com.jieli.jl_bt_ota.util.JL_Log
import com.jieli.jl_bt_ota.util.ParseDataUtil
import com.jieli.otasdk.tool.ota.ble.BleManager
import com.jieli.otasdk.tool.ota.ble.interfaces.BleEventCallback
import com.jieli.otasdk.tool.ota.ble.model.BleScanInfo
import java.util.*

/**
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 回连辅助类
 * @since 2022/12/9
 */
class ReConnectHelper(private val mContext: Context, private val mBtManager: BleManager) {
    private val mParams: MutableList<ReconnectParam> = ArrayList()
    private val mBleAdvCache: MutableMap<String, BleScanMessage> = HashMap()
    private val mUIHandler = Handler(Looper.getMainLooper()) { msg: Message ->
        when (msg.what) {
            MSG_RECONNECT_TIMEOUT -> {
                stopBtScan()
                mParams.clear()
            }
            MSG_PROCESS_TASK -> processReconnectTask()
            else -> if (msg.obj is String) {
                val address = msg.obj as String
                removeParam(address)
            }
        }
        true
    }

    fun release() {
        mParams.clear()
        mBleAdvCache.clear()
        mUIHandler.removeCallbacksAndMessages(null)
        mBtManager.unregisterBleEventCallback(bleEventCallback)
    }

    val isReconnecting: Boolean
        get() = mUIHandler.hasMessages(MSG_RECONNECT_TIMEOUT)

    fun isMatchAddress(srcAddress: String, checkAddress: String): Boolean {
        val param = getCacheParam(srcAddress)
        return if (null == param || !BluetoothAdapter.checkBluetoothAddress(checkAddress)) false else checkAddress == param.deviceAddress || checkAddress == param.connectAddress
    }

    fun putParam(param: ReconnectParam?): Boolean {
        if (null == param) return false
        if (!mParams.contains(param)) {
            if (mParams.add(param)) {
                //添加任务超时
                mUIHandler.sendEmptyMessageDelayed(mParams.hashCode(), RECONNECT_TIMEOUT)
                if (!isReconnecting) {
                    mUIHandler.sendMessageDelayed(
                        mUIHandler.obtainMessage(
                            MSG_RECONNECT_TIMEOUT,
                            param.deviceAddress
                        ), RECONNECT_TIMEOUT + 10 * 1000
                    )
                    mUIHandler.sendEmptyMessage(MSG_PROCESS_TASK)
                }
                return true
            }
        } else {
            return true
        }
        return false
    }

    private fun stopBtScan() {
        mBtManager.stopLeScan()
    }

    private fun getCacheParam(address: String): ReconnectParam? {
        if (!BluetoothAdapter.checkBluetoothAddress(address)) return null
        val advMsg = mBleAdvCache[address]
        for (param in ArrayList(mParams)) {
            if (address == param.deviceAddress || advMsg != null && param.deviceAddress == advMsg.oldBleAddress) {
                return param
            }
        }
        return null
    }

    private fun removeParam(address: String) {
        val param = getCacheParam(address) ?: return
        if (mParams.remove(param)) {
            mUIHandler.removeMessages(param.hashCode())
            if (mParams.isEmpty()) {
                mUIHandler.removeMessages(MSG_RECONNECT_TIMEOUT)
                return
            }
        }
        mUIHandler.sendEmptyMessage(MSG_PROCESS_TASK)
    }

    private fun processReconnectTask() {
        if (mBtManager.isBleScanning) {
            mUIHandler.sendEmptyMessageDelayed(MSG_PROCESS_TASK, FAILED_DELAY)
            return
        }
        val connectedDevice = systemConnectedDevice
        if (null != connectedDevice) {
            val param = getCacheParam(connectedDevice.address)
            if (null != param) param.connectAddress = connectedDevice.address
            mBtManager.connectBleDevice(connectedDevice)
            return
        }
        if (!mBtManager.startLeScan(SCAN_TIMEOUT)) {
            JL_Log.i(TAG, "processReconnectTask : start Le scan failed.")
            mUIHandler.sendEmptyMessageDelayed(MSG_PROCESS_TASK, FAILED_DELAY)
        }
    }

    private val systemConnectedDevice: BluetoothDevice?
        private get() {
            val list = BluetoothUtil.getSystemConnectedBtDeviceList(mContext)
            if (null == list || list.isEmpty()) return null
            for (device in list) {
                if (isReconnectDevice(device, null)) {
                    return device
                }
            }
            return null
        }

    private fun isReconnectDevice(device: BluetoothDevice?, message: BleScanMessage?): Boolean {
        if (null == device || mParams.isEmpty()) return false
        var ret = false
        for (param in ArrayList(mParams)) {
            ret = if (param.isUseNewADV && message != null && message.isOTA) {
                param.deviceAddress == message.oldBleAddress
            } else {
                param.deviceAddress == device.address
            }
            if (ret) break
        }
        return ret
    }

    private val bleEventCallback: BleEventCallback = object : BleEventCallback() {
        override fun onAdapterChange(bEnabled: Boolean) {
            if (!isReconnecting) return
            if (bEnabled) {
                JL_Log.i(TAG, "onAdapterChange : bluetooth is on, try to start le scan.")
                mUIHandler.sendEmptyMessage(MSG_PROCESS_TASK)
            }
        }

        override fun onDiscoveryBleChange(bStart: Boolean) {
            if (!isReconnecting) return
            val isConnecting = mBtManager.isConnecting
            JL_Log.i(TAG, "onDiscoveryBleChange : $bStart, isConnecting = $isConnecting")
            if (!bStart) {
                if (!isConnecting) {
                    mUIHandler.sendEmptyMessage(MSG_PROCESS_TASK)
                }
            }
        }

        override fun onDiscoveryBle(device: BluetoothDevice?, bleScanMessage: BleScanInfo) {
            if (!isReconnecting || null == device) return
            val advMsg = ParseDataUtil.parseOTAFlagFilterWithBroad(
                bleScanMessage.rawData,
                JL_Constant.OTA_IDENTIFY
            )
            if (advMsg != null) {
                mBleAdvCache[device.address] = advMsg
                JL_Log.d(TAG, "onDiscoveryBle : put data in map.")
            }
            val isReconnectDevice = isReconnectDevice(device, advMsg)
            JL_Log.d(
                TAG,
                "onDiscoveryBle : $device, isReconnectDevice = $isReconnectDevice, $advMsg"
            )
            if (isReconnectDevice) {
                stopBtScan()
                val param = getCacheParam(device.address)
                if (null != param) param.connectAddress = device.address
                JL_Log.d(TAG, "onDiscoveryBle : $device, param = $param")
                mBtManager.connectBleDevice(device)
            }
        }

        override fun onBleConnection(device: BluetoothDevice?, status: Int) {
            if (!isReconnecting || null == device) return
            val advMsg = mBleAdvCache[device.address]
            val isReconnectDevice = isReconnectDevice(device, advMsg)
            if (!isReconnectDevice) return
            JL_Log.i(TAG, "onBleConnection : $device, status = $status, $advMsg")
            if (status == BluetoothProfile.STATE_CONNECTED) {
                JL_Log.w(TAG, "onBleConnection : removeParam >>> " + device.address)
                removeParam(device.address)
            } else if (status == BluetoothProfile.STATE_DISCONNECTED) {
                JL_Log.i(TAG, "-onConnection- resume reconnect task.")
                mUIHandler.sendEmptyMessage(MSG_PROCESS_TASK)
            }
        }
    }

    class ReconnectParam(val deviceAddress: String, val isUseNewADV: Boolean) {
        var connectAddress: String? = null

        override fun equals(o: Any?): Boolean {
            if (this === o) return true
            if (o == null || javaClass != o.javaClass) return false
            val that = o as ReconnectParam
            return isUseNewADV == that.isUseNewADV && deviceAddress == that.deviceAddress
        }

        override fun hashCode(): Int {
            return Objects.hash(deviceAddress, isUseNewADV)
        }

        override fun toString(): String {
            return "ReconnectParam{" +
                    "deviceAddress='" + deviceAddress + '\'' +
                    ", isUseNewADV=" + isUseNewADV +
                    ", connectAddress='" + connectAddress + '\'' +
                    '}'
        }
    }

    companion object {
        private val TAG = ReConnectHelper::class.java.simpleName
        private val RECONNECT_TIMEOUT = DeviceReConnectManager.RECONNECT_TIMEOUT //65秒
        private const val SCAN_TIMEOUT = 20 * 1000L //20秒
        private const val FAILED_DELAY = 3 * 1000L
        private const val MSG_RECONNECT_TIMEOUT = 0x01
        private const val MSG_PROCESS_TASK = 0x02
    }

    init {
        mBtManager.registerBleEventCallback(bleEventCallback)
    }
}