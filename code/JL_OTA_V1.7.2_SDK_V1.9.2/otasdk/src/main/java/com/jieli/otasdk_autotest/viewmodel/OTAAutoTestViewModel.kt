package com.jieli.otasdk_autotest.viewmodel

import android.bluetooth.BluetoothDevice
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.jieli.jl_bt_ota.constant.ErrorCode
import com.jieli.jl_bt_ota.constant.StateCode
import com.jieli.jl_bt_ota.interfaces.BtEventCallback
import com.jieli.jl_bt_ota.interfaces.IUpgradeCallback
import com.jieli.jl_bt_ota.model.base.BaseError
import com.jieli.jl_bt_ota.model.response.TargetInfoResponse
import com.jieli.jl_bt_ota.util.BluetoothUtil
import com.jieli.jl_bt_ota.util.CHexConver
import com.jieli.jl_bt_ota.util.JL_Log
import com.jieli.otasdk.MainApplication
import com.jieli.otasdk.R
import com.jieli.otasdk.model.DeviceConnection
import com.jieli.otasdk_autotest.model.auto.*
import com.jieli.otasdk.model.ota.*
import com.jieli.otasdk_autotest.tool.auto.TaskManager
import com.jieli.otasdk_autotest.tool.auto.TaskQueueCallback
import com.jieli.otasdk_autotest.tool.auto.TestTask
import com.jieli.otasdk_autotest.tool.auto.task.ReConnectTask
import com.jieli.otasdk_autotest.tool.auto.task.UpdateTask
import com.jieli.otasdk.tool.ota.OTAManager
import com.jieli.otasdk.util.FileObserverCallback
import com.jieli.otasdk.util.OtaFileObserverHelper
import com.jieli.otasdk.viewmodel.BluetoothViewModel
import java.io.File

/**
 * @author zqjasonZhong
 * @since 2022/9/9
 * @email zhongzhuocheng@zh-jieli.com
 * @desc OTA逻辑实现
 */
class OTAAutoTestViewModel : BluetoothViewModel() {
    companion object {
        private const val MSG_UPDATE_OTA_FILE_LIST = 0x123
    }

    private val otaManager = OTAManager(MainApplication.getInstance())
    private val fileObserverHelper = OtaFileObserverHelper.getInstance()
    private val taskManager = TaskManager.getInstance()


    val fileListMLD = MutableLiveData<MutableList<File>>()
    val otaConnectionMLD = MutableLiveData<DeviceConnection>()
    val mandatoryUpgradeMLD = MutableLiveData<BluetoothDevice>()
    val otaStateMLD = MutableLiveData<OTAState>()
    val testStateMLD = MutableLiveData<TestState>()

    private val mUIHandler: Handler = Handler(Looper.getMainLooper(), Handler.Callback {
        when (it.what) {
            MSG_UPDATE_OTA_FILE_LIST -> readFileList()
        }
        return@Callback true
    })

    private val bluetoothCallback = object : BtEventCallback() {

        override fun onConnection(device: BluetoothDevice?, status: Int) {
            if ((status == StateCode.CONNECTION_FAILED || status == StateCode.CONNECTION_DISCONNECT) && bluetoothHelper.getConnectedDevice() != null) {
                bluetoothHelper.disconnectDevice(bluetoothHelper.getConnectedDevice())
            }
            otaConnectionMLD.value = DeviceConnection(device, status)
        }

        override fun onMandatoryUpgrade(device: BluetoothDevice?) {
            mandatoryUpgradeMLD.postValue(device!!)
        }
    }

    private val fileObserverCallback = FileObserverCallback { event, path ->
        if (event >= 2 && path != null) {
            mUIHandler.removeMessages(MSG_UPDATE_OTA_FILE_LIST)
            mUIHandler.sendEmptyMessageDelayed(MSG_UPDATE_OTA_FILE_LIST, 500)
        }
    }

    private val taskQueueCallback = object : TaskQueueCallback {
        override fun onStart(param: TestParam) {
            testStateMLD.value = TestWorking(param)
        }

        override fun onTaskStart(id: Int, task: TestTask, message: String?) {
            testStateMLD.value = TestTaskStart(id, task, message)
        }

        override fun onTaskLogcat(id: Int, task: TestTask, log: String?) {
            testStateMLD.value = TestTaskLog(id, task, log)
        }

        override fun onTaskStop(id: Int, task: TestTask, code: Int, message: String?) {
            testStateMLD.value = TestTaskEnd(id, task, code, message)
        }

        override fun onFinish(success: Int, code: Int, message: String?) {
            testStateMLD.value = TestFinish(success, code, message)
        }
    }

    init {
        otaManager.registerBluetoothCallback(bluetoothCallback)
        fileObserverHelper.registerFileObserverCallback(fileObserverCallback)
        fileObserverHelper.startObserver()
    }

    fun destroy() {
        cancelOTA()
        fileObserverHelper.stopObserver()
        fileObserverHelper.unregisterFileObserverCallback(fileObserverCallback)
        taskManager.stopTest()
        otaManager.unregisterBluetoothCallback(bluetoothCallback)
        otaManager.release()
    }

    fun isAutoTest(): Boolean {
        return configHelper.isAutoTest()
    }
    fun isFaultTolerant(): Boolean {
        return configHelper.isFaultTolerant()
    }
    fun isUseReconnectWay(): Boolean {
        return configHelper.isUseCustomReConnectWay()
    }

    fun isOTA(): Boolean {
        return otaManager.isOTA
    }

    fun getAutoTestCount(): Int {
        return configHelper.getAutoTestCount()
    }

    fun getTestParam(): TestParam? {
        return taskManager.getTestParam()
    }

    fun getDeviceInfo(): TargetInfoResponse? {
        return otaManager.getDeviceInfo(getConnectedDevice());
    }

    fun readFileList() {
        val dirPath: String? = MainApplication.getOTAFileDir()
        dirPath?.let {
            val parent = File(it)
            val files = mutableListOf<File>()
            if (parent.exists()) {
//                Log.d("TAG", "readFileList: " + parent.listFiles().size)
                parent.listFiles()?.forEach { file ->
                    if (file.name.endsWith(".ufw") || file.name.endsWith(".bfu")) {
                        files.add(file)
                    }
                }
            } else {
                parent.mkdirs()
            }
            fileListMLD.postValue(files)
        }
    }

    fun startOTA(otaLoop: Int, faultTolerantCount: Int,filePathList: MutableList<String>) {
        JL_Log.i(tag, "startOTA : otaLoop = $otaLoop, file size = ${filePathList.size}")
        if (otaLoop == 0 || filePathList.isEmpty()) {
            if (isAutoTest()) {
                testStateMLD.value = TestFinish(0, TestTask.ERR_INVALID_PARAM, "Param error")
            } else {
                otaStateMLD.value =
                    OTAEnd(getConnectedDevice(), TestTask.ERR_INVALID_PARAM, "Param error")
            }
            return
        }
        val device = getConnectedDevice()
        if (device == null) {
            JL_Log.w(tag, "startOTA : no connected device.")
            if (isAutoTest()) {
                testStateMLD.value = TestFinish(0, TestTask.ERR_FAILED, "Device is disconnect")
            } else {
                otaStateMLD.value =
                    OTAEnd(getConnectedDevice(), TestTask.ERR_FAILED, "Device is disconnect")
            }
            return
        }
        if (taskManager.isTestRun()) {
            JL_Log.w(tag, "startOTA : It is running.")
            if (isAutoTest()) {
                testStateMLD.value =
                    TestFinish(0, TestTask.ERR_TASK_IN_PROGRESS, "Auto Test is in progress.")
            } else {
                otaStateMLD.value = OTAEnd(device, TestTask.ERR_TASK_IN_PROGRESS, "Ota is running.")
            }
            return
        }
        val temp = otaLoop / filePathList.size
        val loopNum = if (otaLoop % filePathList.size == 0) temp else temp + 1
        val taskCount = (otaLoop - 1) * 2 + 1
        val taskList = mutableListOf<TestTask>()
        val context = MainApplication.getInstance()
        JL_Log.d(tag, "startOTA : loopNum = $loopNum, taskCount = $taskCount")
        for (i in 0 until loopNum) {
            if (taskList.size >= taskCount) break
            val isEnd = i == loopNum - 1
            for ((j, filePath) in filePathList.withIndex()) {
                val isListEnd = j == filePathList.size - 1
                taskList.add(
                    UpdateTask(
                        context, otaManager, filePath,
                        CustomUpdateCallback(
                            context, device, this
                        )
                    )
                )
                if (taskList.size >= taskCount) break
                if (isEnd && isListEnd) continue
                taskList.add(ReConnectTask(bluetoothHelper, otaManager, device.address))
                if (taskList.size >= taskCount) break
            }
        }
        if (taskList.isEmpty()) {
            JL_Log.w(tag, "startOTA : No valid task.")
            if (isAutoTest()) {
                testStateMLD.value = TestFinish(0, TestTask.ERR_INVALID_PARAM, "No valid task.")
            } else {
                otaStateMLD.value = OTAEnd(device, TestTask.ERR_INVALID_PARAM, "No valid task.")
            }
            return
        }
//        if (isAutoTest()) {
//            configHelper.setAutoTestCount(otaLoop)
//        }
        taskManager.setFaultTolerantCount(faultTolerantCount)
        taskManager.startTest(taskList, taskQueueCallback)
    }

    private fun cancelOTA() {
        if (isOTA()) {
            otaManager.cancelOTA()
        }
    }

    fun reconnectDev(devAddr: String?, isUseNewAdv: Boolean) {
        //Step0.转换成目标地址， 比如地址+1
        JL_Log.i(tag, "change addr before : $devAddr")
        val data = BluetoothUtil.addressCovertToByteArray(devAddr)
        val value = CHexConver.byteToInt(data[data.size - 1]) + 1
        data[data.size - 1] = CHexConver.intToByte(value)
        val newAddr = BluetoothUtil.hexDataCovetToAddress(data)
        JL_Log.i(tag, "change addr after: $newAddr")
        //Step1.更新回连的地址
        otaManager.setReconnectAddr(newAddr)
        //Step2.主动实现回连方式
        bluetoothHelper.reconnectDevice(devAddr, isUseNewAdv)
    }

    private class CustomUpdateCallback(
        val context: Context,
        val device: BluetoothDevice?,
        val viewModel: OTAAutoTestViewModel
    ) : IUpgradeCallback {

        override fun onStartOTA() {
            viewModel.otaStateMLD.value = OTAStart(device)
        }

        override fun onNeedReconnect(addr: String?, isNewReconnectWay: Boolean) {
            viewModel.otaStateMLD.value = OTAReconnect(device, addr, isNewReconnectWay)
            if (viewModel.isUseReconnectWay()) {
                viewModel.reconnectDev(addr, isNewReconnectWay)
            }
        }

        override fun onProgress(type: Int, progress: Float) {
            viewModel.otaStateMLD.value = OTAWorking(device, type, progress)
        }

        override fun onStopOTA() {
            viewModel.otaStateMLD.value =
                OTAEnd(device, ErrorCode.ERR_NONE, context.getString(R.string.ota_complete))
            val handler = Handler(Looper.getMainLooper())
            handler.postDelayed({
//                Log.d("ZHM", "onStopOTA: disconnectDevice connected : "+viewModel.bluetoothHelper.isConnected()+ " getConnectedDevice: "+viewModel.bluetoothHelper.getConnectedDevice())
                //变地址导致无法判断device是否一致
                if (viewModel.bluetoothHelper.isConnected()/* && BluetoothUtil.deviceEquals(
                        device,
                        viewModel.bluetoothHelper.getConnectedDevice()
                    )*/
                ) {
                    Log.d("ZHM", "onStopOTA: disconnectDevice")
                    viewModel.bluetoothHelper.disconnectDevice(viewModel.otaManager.connectedDevice)
                }
            }, 500)
        }

        override fun onCancelOTA() {
            viewModel.otaStateMLD.value =
                OTAEnd(
                    device,
                    ErrorCode.ERR_UNKNOWN,
                    context.getString(R.string.ota_upgrade_cancel)
                )
        }

        override fun onError(error: BaseError?) {
            error?.let {
                viewModel.otaStateMLD.value = OTAEnd(device, it.subCode, it.message)
            }
            viewModel.bluetoothHelper.disconnectDevice(viewModel.otaManager.connectedDevice)//升级失败，断开设备，方便自动化继续升级
        }
    }
}