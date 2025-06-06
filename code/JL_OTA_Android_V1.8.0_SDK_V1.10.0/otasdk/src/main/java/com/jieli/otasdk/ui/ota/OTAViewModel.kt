package com.jieli.otasdk.ui.ota

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
import com.jieli.otasdk.R
import com.jieli.otasdk.data.model.device.DeviceConnection
import com.jieli.otasdk.data.model.ota.OTAEnd
import com.jieli.otasdk.data.model.ota.OTAReconnect
import com.jieli.otasdk.data.model.ota.OTAStart
import com.jieli.otasdk.data.model.ota.OTAState
import com.jieli.otasdk.data.model.ota.OTAWorking
import com.jieli.otasdk.tool.file.FileManager
import com.jieli.otasdk.tool.ota.OTAManager
import com.jieli.otasdk.ui.base.BluetoothViewModel
import com.jieli.otasdk_autotest.tool.auto.TestTask
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

    private val otaManager = OTAManager(getContext())
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
            JL_Log.i(tag, "onConnection", "device : $device, status : $status")
            if (status == StateCode.CONNECTION_FAILED || status == StateCode.CONNECTION_DISCONNECT) {
                getConnectedDevice()?.let { connectedDevice ->
                    bluetoothHelper.disconnectDevice(connectedDevice)
                }
            }
            otaConnectionMLD.value = DeviceConnection(device, status)
        }

        override fun onMandatoryUpgrade(device: BluetoothDevice?) {
            mandatoryUpgradeMLD.postValue(device!!)
        }
    }


    init {
        otaManager.registerBluetoothCallback(bluetoothCallback)
    }

    override fun onCleared() {
        super.onCleared()
        destroy()
    }

    override fun destroy() {
        super.destroy()
        cancelOTA()
        otaManager.unregisterBluetoothCallback(bluetoothCallback)
        otaManager.release()
    }

    fun isUseReconnectWay(): Boolean = configHelper.isUseCustomReConnectWay()

    fun isAutoTestOTA(): Boolean = configHelper.isAutoTest()

    fun isOTA(): Boolean = otaManager.isOTA

    fun getDeviceInfo(): TargetInfoResponse? = otaManager.getDeviceInfo(getConnectedDevice())

    fun readFileList() {
        val files = FileManager.readUpgradeFile()
        fileListMLD.postValue(files)
    }

    fun startOTA(filePath: String) {
        JL_Log.i(tag, "startOTA , file path = $filePath")
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
        otaManager.bluetoothOption.firmwareFilePath = filePath
        otaManager.startOTA(
            CustomUpdateCallback(
                getContext(), device, this
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