package com.jieli.otasdk_autotest.tool.auto.task

import android.bluetooth.BluetoothDevice
import android.os.Handler
import android.os.Looper
import com.jieli.jl_bt_ota.constant.ErrorCode
import com.jieli.jl_bt_ota.constant.JL_Constant
import com.jieli.jl_bt_ota.constant.StateCode
import com.jieli.jl_bt_ota.interfaces.BtEventCallback
import com.jieli.jl_bt_ota.model.base.BaseError
import com.jieli.jl_bt_ota.util.BluetoothUtil
import com.jieli.jl_bt_ota.util.JL_Log
import com.jieli.jl_bt_ota.util.ParseDataUtil
import com.jieli.otasdk.tool.bluetooth.BluetoothHelper
import com.jieli.otasdk.tool.bluetooth.OnBTEventCallback
import com.jieli.otasdk.tool.ota.OTAManager
import com.jieli.otasdk.tool.ota.ble.model.BleScanInfo
import com.jieli.otasdk.util.AppUtil
import com.jieli.otasdk.data.constant.OtaConstant
import com.jieli.otasdk_autotest.tool.auto.OnTaskListener
import com.jieli.otasdk_autotest.tool.auto.TestTask

/**
 * @author zqjasonZhong
 * @since 2022/9/14
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 回连设备任务
 */
class ReConnectTask(
    private val bluetoothHelper: BluetoothHelper,
    private val otaManager: OTAManager,
    private val reconnectAddress: String
) : TestTask(TASK_TYPE_CONNECT) {

    @Volatile
    private var taskListener: OnTaskListener? = null

    private var isConnecting: Boolean = false

    companion object {

        private const val RECONNECT_TIMEOUT: Long = 120 * 1000L
        private const val DELAY_TIME: Long = 3 * 1000L

        private const val MSG_RECONNECT_DEVICE = 0x1234
        private const val MSG_RECONNECT_DEVICE_TIMEOUT = 0x1235
    }

    private val uiHandler = Handler(Looper.getMainLooper()) { message ->
        when (message.what) {
            MSG_RECONNECT_DEVICE -> {
                reconnectDevice()
            }

            MSG_RECONNECT_DEVICE_TIMEOUT -> {
                cbTaskFinish(ERR_FAILED, "回连任务超时结束")
            }
        }
        return@Handler true
    }

    override fun getName(): String {
        return "Reconnect Device"
    }

    override fun isRun(): Boolean = uiHandler.hasMessages(MSG_RECONNECT_DEVICE_TIMEOUT)

    override fun startTest(listener: OnTaskListener) {
        if (isRun()) {
            JL_Log.i(tag, "cbTaskFinish",
                "code : ERR_TASK_IN_PROGRESS, 任务进行中，请勿重复开启")
            listener.onFinish(ERR_TASK_IN_PROGRESS, "任务进行中，请勿重复开启")
            return
        }
        taskListener = listener
        cbTaskStart()
        if (bluetoothHelper.isConnected()) {  //设备已连接
            cbTaskLog("设备[${printBtDeviceMsg(bluetoothHelper.getConnectedDevice())}]已连接!!!")
            cbTaskFinish(ERR_SUCCESS, "[${getName()}]任务完成")
        } else { //设备未连接
            cbTaskLog("正在回连设备[$reconnectAddress]...")
            uiHandler.removeMessages(MSG_RECONNECT_DEVICE)
            uiHandler.sendEmptyMessage(MSG_RECONNECT_DEVICE)
        }
    }

    override fun stopTest(): Boolean {
        if (!isRun()) return false
        cbTaskFinish(ERR_USE_CANCEL, "用户取消任务")
        return true
    }

    private fun reconnectDevice() {
        var ret = false
        if (!BluetoothUtil.isBluetoothEnable()) {
            cbTaskLog("蓝牙未开启, 请打开蓝牙继续任务")
        } else if (bluetoothHelper.isConnecting()) { //设备正在连接
            cbTaskLog("设备[$reconnectAddress]正在连接...")
        } else if (bluetoothHelper.isScanning()) {
            cbTaskLog("正在搜索设备...")
            ret = true
        } else {
            ret = bluetoothHelper.startScan(OtaConstant.SCAN_TIMEOUT)
            cbTaskLog("搜索设备 >>> 结果: ${if (ret) "成功" else "失败"}")
        }
        if (!ret) {
            //开始失败计时
            uiHandler.removeMessages(MSG_RECONNECT_DEVICE)
            uiHandler.sendEmptyMessageDelayed(MSG_RECONNECT_DEVICE, DELAY_TIME)
        }
    }

    private fun cbTaskStart() {
        bluetoothHelper.registerCallback(btEventCallback)
        otaManager.registerBluetoothCallback(otaBtEventCallback)
        uiHandler.sendEmptyMessageDelayed(MSG_RECONNECT_DEVICE_TIMEOUT, RECONNECT_TIMEOUT)
        JL_Log.i(tag, "cbTaskStart", "")
        taskListener?.onStart("")
    }

    private fun cbTaskLog(logcat: String?) {
        JL_Log.d(tag, "cbTaskLog", logcat)
        taskListener?.onLogcat(logcat)
    }

    private fun cbTaskFinish(code: Int, message: String?) {
        bluetoothHelper.stopScan()
        bluetoothHelper.unregisterCallback(btEventCallback)
        otaManager.unregisterBluetoothCallback(otaBtEventCallback)
        uiHandler.removeMessages(MSG_RECONNECT_DEVICE)
        uiHandler.removeMessages(MSG_RECONNECT_DEVICE_TIMEOUT)
        JL_Log.i(tag, "cbTaskFinish", "code : $code, $message")
        taskListener?.onFinish(code, message)
    }

    private fun isReConnectDevice(device: BluetoothDevice?, bleScanInfo: BleScanInfo?): Boolean {
        var ret = false
        val bleScanMessage =
            ParseDataUtil.parseOTAFlagFilterWithBroad(
                bleScanInfo?.rawData,
                JL_Constant.OTA_IDENTIFY
            )
        if (bleScanMessage != null) {
            ret = reconnectAddress.equals(bleScanMessage.oldBleAddress, ignoreCase = true)
        }
        if (!ret) {
            ret = reconnectAddress.equals(device?.address, ignoreCase = true)
        }
        return ret
    }

//    private fun isReConnectDevice(device: BluetoothDevice?): Boolean {
//        // TODO: 兼容新回连方式
//        if (null == device) return false
//        return reconnectAddress == device.address
//    }

    private fun printBtDeviceMsg(device: BluetoothDevice?): String? {
        return AppUtil.printBtDeviceInfo(device)
    }

    private val scanInfoMap = mutableMapOf<BluetoothDevice, BleScanInfo>()
    private val btEventCallback = object : OnBTEventCallback() {

        override fun onDiscoveryChange(bStart: Boolean, scanType: Int) {
            if (!bStart && isRun() && !isConnecting) {
                uiHandler.removeMessages(MSG_RECONNECT_DEVICE)
                uiHandler.sendEmptyMessage(MSG_RECONNECT_DEVICE)
            }
        }

        override fun onDiscovery(device: BluetoothDevice?, bleScanMessage: BleScanInfo?) {
            if (isRun() && isReConnectDevice(device, bleScanMessage) && !isConnecting) {
                isConnecting = true
                if (device != null && bleScanMessage != null) {
                    scanInfoMap[device] = bleScanMessage
                }
                JL_Log.d(
                    tag,
                    "onDiscovery", "found target device : ${
                        printBtDeviceMsg(device)
                    }, \n$bleScanMessage"
                )
                bluetoothHelper.stopScan()
                if (!bluetoothHelper.connectDevice(device)) {
                    isConnecting = false
                    uiHandler.removeMessages(MSG_RECONNECT_DEVICE)
                    uiHandler.sendEmptyMessage(MSG_RECONNECT_DEVICE)
                }
            }
        }
    }

    private val otaBtEventCallback = object : BtEventCallback() {

        override fun onConnection(device: BluetoothDevice?, status: Int) {
            if (isRun() && isReConnectDevice(device, scanInfoMap[device])) {
                isConnecting = status == StateCode.CONNECTION_CONNECTING
                when (status) {
                    StateCode.CONNECTION_CONNECTING -> {
                        cbTaskLog("设备[${printBtDeviceMsg(device)}]正在连接...")
                    }

                    StateCode.CONNECTION_OK -> {
                        scanInfoMap.remove(device)
                        cbTaskLog("设备[${printBtDeviceMsg(device)}]已连接!!!")
                        cbTaskFinish(ERR_SUCCESS, "[${getName()}]任务完成")
                    }

                    StateCode.CONNECTION_FAILED,
                    StateCode.CONNECTION_DISCONNECT -> {
                        scanInfoMap.remove(device)
                        cbTaskLog("设备[${printBtDeviceMsg(device)}]连接失败!!!")
                        uiHandler.removeMessages(MSG_RECONNECT_DEVICE)
                        uiHandler.sendEmptyMessage(MSG_RECONNECT_DEVICE)
                    }
                }
            }
        }

        override fun onError(error: BaseError?) {
            if (error?.subCode == ErrorCode.SUB_ERR_AUTH_DEVICE) {
                bluetoothHelper.disconnectDevice(bluetoothHelper.getConnectedDevice())
            }
        }
    }

}