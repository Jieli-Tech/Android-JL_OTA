package com.jieli.otasdk.viewmodel

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
import com.jieli.otasdk.model.ota.*
import com.jieli.otasdk_autotest.tool.auto.TestTask
import com.jieli.otasdk.tool.ota.OTAManager
import com.jieli.otasdk.util.FileObserverCallback
import com.jieli.otasdk.util.OtaFileObserverHelper
import java.io.File

/**
 * @author zqjasonZhong
 * @since 2022/9/9
 * @email zhongzhuocheng@zh-jieli.com
 * @desc OTA逻辑实现
 */
class OTAViewModel : BluetoothViewModel() {
    companion object {
        private const val MSG_UPDATE_OTA_FILE_LIST = 0x123
    }
    private val otaManager = OTAManager(MainApplication.getInstance())
    private val fileObserverHelper = OtaFileObserverHelper.getInstance()
    val fileListMLD = MutableLiveData<MutableList<File>>()
    val otaConnectionMLD = MutableLiveData<DeviceConnection>()
    val mandatoryUpgradeMLD = MutableLiveData<BluetoothDevice>()
    val otaStateMLD = MutableLiveData<OTAState>()

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


    init {
        otaManager.registerBluetoothCallback(bluetoothCallback)
//    }
//
//    fun startFileObserver(): Unit {
        fileObserverHelper.registerFileObserverCallback(fileObserverCallback)
        fileObserverHelper.startObserver()
    }

    fun destroy() {
        cancelOTA()
        fileObserverHelper.stopObserver()
        fileObserverHelper.unregisterFileObserverCallback(fileObserverCallback)
        otaManager.unregisterBluetoothCallback(bluetoothCallback)
        otaManager.release()
    }

    fun isUseReconnectWay(): Boolean {
        return configHelper.isUseCustomReConnectWay()
    }

    fun isOTA(): Boolean {
        return otaManager.isOTA
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

    fun startOTA(filePath: String) {
        JL_Log.i(tag, "startOTA , file path = ${filePath}")
        val device = getConnectedDevice()
        if (device == null) {
            JL_Log.w(tag, "startOTA : no connected device.")
            otaStateMLD.value =
                OTAEnd(getConnectedDevice(), TestTask.ERR_FAILED, "Device is disconnect")
            return
        }
        if (otaManager.isOTA) {
            otaStateMLD.value = OTAEnd(device, TestTask.ERR_TASK_IN_PROGRESS, "Ota is running.")
            return
        }
        val context = MainApplication.getInstance()
        otaManager.bluetoothOption.firmwareFilePath = filePath
        otaManager.startOTA(
            CustomUpdateCallback(
                context, device, this
            )
        )
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
        val viewModel: OTAViewModel
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