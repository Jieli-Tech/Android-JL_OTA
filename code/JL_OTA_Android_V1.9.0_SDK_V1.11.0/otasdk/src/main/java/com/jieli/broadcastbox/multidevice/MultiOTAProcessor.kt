package com.jieli.broadcastbox.multidevice

import android.bluetooth.BluetoothDevice
import android.content.Context
import android.os.Handler
import android.os.Looper
import com.jieli.broadcastbox.multidevice.bean.MultiOtaConfig
import com.jieli.broadcastbox.multidevice.bean.MultiOtaParam
import com.jieli.broadcastbox.multidevice.bean.MultiOtaValue
import com.jieli.broadcastbox.multidevice.callback.OnMultiOTACallback
import com.jieli.jl_bt_ota.constant.ErrorCode
import com.jieli.jl_bt_ota.constant.StateCode
import com.jieli.jl_bt_ota.interfaces.BtEventCallback
import com.jieli.jl_bt_ota.interfaces.IUpgradeCallback
import com.jieli.jl_bt_ota.model.OTAError
import com.jieli.jl_bt_ota.model.base.BaseError
import com.jieli.jl_bt_ota.util.JL_Log
import java.util.*

/**
 * @author zqjasonZhong
 * @since 2022/12/8
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 多设备OTA处理器
 */
class MultiOTAProcessor {

    private val tag = javaClass.simpleName

    /**
     * OTAManager集合
     */
    private val otaManagerMap: HashMap<String, MultiOtaValue> = HashMap()

    /**
     * 任务列表
     */
    private val taskList: MutableList<MultiOtaParam> = Collections.synchronizedList(mutableListOf())
    private var multiOTAResult: MultiOTAResult? = null

    private var callback: OnMultiOTACallback? = null

    private val uiHandler: Handler = Handler(Looper.myLooper()!!) {
        return@Handler true
    }

    fun destroy() {
        uiHandler.removeCallbacksAndMessages(null)
        setCallback(null)
        if (otaManagerMap.isNotEmpty()) {
            val temp = HashMap(otaManagerMap)
            temp.forEach { (_, value) ->
                value.otaManager.release()
            }
            otaManagerMap.clear()
        }
        instance = null
    }

    fun setCallback(callback: OnMultiOTACallback?) {
        this.callback = callback
    }

    fun startMultiOTA(context: Context, config: MultiOtaConfig): Int {
        if (config.params.isEmpty()) return ErrorCode.SUB_ERR_PARAMETER
        if (isUpgrading()) return ErrorCode.SUB_ERR_OTA_IN_HANDLE
        multiOTAResult = MultiOTAResult(config)

        for (param in config.params) {
            val key = param.address
            taskList.add(param)
            if (otaManagerMap.containsKey(key)) continue
            val otaManager = MultiOTAManager(context, param.address)
            otaManager.registerBluetoothCallback(CustomBtEventCallback(key))
            val value = MultiOtaValue(otaManager)
            otaManagerMap[key] = value
            value.seq = otaManagerMap.size - 1
        }

        callbackStartOTA(taskList.size, config.otaWay)
        return if (config.otaWay == MULTI_OTA_WAY_QUEUE) {
            executeQueueOTA()
        } else {
            executeMultiOTA()
        }
    }

    private fun isUpgrading(): Boolean {
        if (otaManagerMap.isEmpty()) return false
        for (value in otaManagerMap.values) {
            if (value.otaManager.isOTA) {
                return true
            }
        }
        return false
    }

    private fun getQueueMultiOtaValue(): MultiOtaValue? {
        if (taskList.isEmpty()) return null
        var value: MultiOtaValue? = null
        for (param in ArrayList(taskList)) {
            val otaValue = getOTAManager(param.address) ?: continue
            if (otaValue.otaManager.isInitOk()) {
                value = otaValue
                break
            }
        }
        if (value == null) {
            value = getOTAManager(taskList[0].address)
        }
        return value
    }

    private fun getOTAParam(address: String): MultiOtaParam? {
        var result: MultiOtaParam? = null
        multiOTAResult?.apply {
            for (param in getOtaConfig().params) {
                if (param.address == address) {
                    result = param
                    break
                }
            }
        }
        return result
    }

    private fun getOTAManager(address: String): MultiOtaValue? {
        if (!otaManagerMap.containsKey(address)) return null
        return otaManagerMap[address]
    }

    private fun executeQueueOTA(): Int {
        val value = getQueueMultiOtaValue() ?: return ErrorCode.SUB_ERR_PARAMETER
        if (!value.otaManager.isInitOk()) {
            value.isReadOta = true
        } else {
            val param = getOTAParam(value.otaManager.srcAddress!!)!!
            value.otaManager.bluetoothOption.firmwareFilePath = param.filePaths[0]
            value.otaManager.startOTA(CustomOTACallback(param.address))
        }
        return ErrorCode.ERR_NONE
    }

    private fun executeMultiOTA(): Int {
        for (param in taskList) {
            val value = getOTAManager(param.address) ?: continue
            if (!value.otaManager.isInitOk()) {
                JL_Log.d(tag, "executeMultiOTA : device is not init.")
                value.isReadOta = true
                continue
            }
            value.otaManager.bluetoothOption.firmwareFilePath = param.filePaths[0]
            JL_Log.i(tag, "executeMultiOTA : startOTA >> ${param.address}")
            value.otaManager.startOTA(CustomOTACallback(param.address))
        }
        return ErrorCode.ERR_NONE
    }

    private fun callbackStartOTA(total: Int, otaWay: Int) {
        callback?.let {
            uiHandler.post { it.onMultiOTAStart(total, otaWay) }
        }
    }

    private fun callbackOTAProgress(address: String, type: Int, progress: Float) {
        callback?.let {
            uiHandler.post {
                it.onMultiOTAProgress(address, type, progress)
            }
        }
    }

    private fun callbackOTAStop(address: String, code: Int, message: String) {
        callback?.let {
            uiHandler.post {
                it.onMultiOTAStop(address, code, message)
                //转MessageQueue处理
                multiOTAResult?.let { result ->
                    val param = getOTAParam(address)
                    if (code == ErrorCode.ERR_NONE || code == ERR_CANCEL_OTA) { //成功或者取消，都视为一种情况处理
                        param?.let {
                            if (taskList.remove(it)) {
                                result.success.add(it)
                            }
                        }
                    } else { //升级失败
                        if (result.getOtaConfig().otaWay == MULTI_OTA_WAY_QUEUE) {
                            taskList.clear() //清空任务队列
                        } else {
                            param?.let {
                                taskList.remove(it)
                            }
                        }
                    }
                    if (taskList.isEmpty()) {
                        callbackMultiOTAFinish(
                            result.getOtaConfig().params.size,
                            result.success.toList()
                        )
                    } else if (result.getOtaConfig().otaWay == MULTI_OTA_WAY_QUEUE) { //队列式，就执行下一个OTA任务
                        executeQueueOTA()
                    }
                }
            }
        }
    }

    private fun callbackNeedReconnect(
        address: String,
        reconnectAddr: String?,
        isNewReconnectWay: Boolean
    ) {
        callback?.let {
            uiHandler.post { it.onMultiOTANeedReconnect(address, reconnectAddr, isNewReconnectWay) }
        }
    }

    private fun callbackMultiOTAFinish(total: Int, success: List<MultiOtaParam>) {
        callback?.let {
            uiHandler.post { it.onMultiOTAFinish(total, success) }
        }
    }

    private inner class MultiOTAResult(config: MultiOtaConfig) {
        private val otaConfig = config

        val success: MutableList<MultiOtaParam> = mutableListOf()

        fun getOtaConfig(): MultiOtaConfig {
            return otaConfig
        }
    }

    private inner class CustomBtEventCallback(private val address: String) : BtEventCallback() {

        override fun onConnection(device: BluetoothDevice?, status: Int) {
            val otaManager = getOTAManager(address)
            otaManager?.let { value ->
                JL_Log.i(tag, "onConnection >> ${device}, status =  $status")
                if (status == StateCode.CONNECTION_OK) {
                    if (!value.otaManager.isOTA && value.isReadOta) {
                        value.isReadOta = false
                        val param = getOTAParam(address)!!
                        value.otaManager.bluetoothOption.firmwareFilePath = param.filePaths[0]
                        value.otaManager.startOTA(CustomOTACallback(address))
                    }
                } else if ((status == StateCode.CONNECTION_CONNECTED || status == StateCode.CONNECTION_FAILED) && value.isReadOta) {
                    value.isReadOta = false
                    callbackOTAStop(
                        address,
                        ErrorCode.SUB_ERR_REMOTE_NOT_CONNECTED,
                        OTAError.buildError(ErrorCode.SUB_ERR_REMOTE_NOT_CONNECTED).message
                    )
                }
            }
        }
    }

    private inner class CustomOTACallback(private val address: String) : IUpgradeCallback {

        override fun onStartOTA() {
            callbackOTAProgress(address, 0, 0f)
        }

        override fun onNeedReconnect(addr: String?, isNewReconnectWay: Boolean) {
            callbackNeedReconnect(address, addr, isNewReconnectWay)
        }

        override fun onProgress(type: Int, progress: Float) {
            callbackOTAProgress(address, type, progress)
        }

        override fun onStopOTA() {
            callbackOTAStop(address, ErrorCode.ERR_NONE, "OTA is success.")
        }

        override fun onCancelOTA() {
            callbackOTAStop(address, ErrorCode.SUB_ERR_OTA_FAILED, "User canceled the ota process.")
        }

        override fun onError(error: BaseError?) {
            error?.let {
                callbackOTAStop(address, it.subCode, it.message)
                if (it.subCode != ErrorCode.SUB_ERR_OTA_IN_HANDLE && it.subCode != ErrorCode.SUB_ERR_FILE_NOT_FOUND
                    && it.subCode != ErrorCode.SUB_ERR_DATA_NOT_FOUND
                ) {
                    val value = getOTAManager(address)
                    value?.let { it1 ->
                        it1.otaManager.disconnectBluetoothDevice(it1.otaManager.connectedDevice)
                    }
                }
            }
        }
    }

    companion object {
        const val ERR_CANCEL_OTA = -0x12

        /**
         * 并发式OTA
         */
        const val MULTI_OTA_WAY_CONCURRENCY = 0

        /**
         * 队列式OTA
         */
        const val MULTI_OTA_WAY_QUEUE = 1

        @Volatile
        private var instance: MultiOTAProcessor? = null

        fun getInstance() = instance ?: synchronized(this) {
            instance ?: MultiOTAProcessor().also {
                instance = it
            }
        }
    }
}