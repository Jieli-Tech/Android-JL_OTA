package com.jieli.otasdk.viewmodel

import android.bluetooth.BluetoothDevice
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.jieli.component.utils.ToastUtil
import com.jieli.jl_bt_ota.constant.BluetoothConstant
import com.jieli.jl_bt_ota.util.BluetoothUtil
import com.jieli.jl_bt_ota.util.CHexConver
import com.jieli.jl_bt_ota.util.JL_Log
import com.jieli.otasdk.MainApplication
import com.jieli.otasdk.tool.config.ConfigHelper
import com.jieli.otasdk.tool.ota.spp.SppManager
import com.jieli.otasdk.tool.ota.spp.interfaces.SppEventCallback
import java.util.*

/**
 * @author zqjasonZhong
 * @since 2022/9/13
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 配置逻辑实现
 */
class ConfigViewModel : ViewModel() {
    private val tag = ConfigHelper::class.java.simpleName
    private val configHelper = ConfigHelper.getInstance()
    private val sppManager = SppManager.getInstance()
    private val sppEventCallback = object : SppEventCallback() {
        override fun onReceiveSppData(device: BluetoothDevice?, uuid: UUID?, data: ByteArray?) {
            if (sppManager.customSppUUID == uuid) {
                ToastUtil.showToastShort("接收到的SPP数据==> data = ${CHexConver.byte2HexStr(data)}")
                JL_Log.i(
                    tag,
                    "-onReceiveSppData- device = ${
                        BluetoothUtil.printBtDeviceInfo(
                            MainApplication.getInstance(),
                            device
                        )
                    }, data = ${CHexConver.byte2HexStr(data)}"
                )
            }
        }
    }
    val configChangeMLD = MutableLiveData(false)
    val saveSettingMLD = MutableLiveData(false)

    //配置是否更改
    var isChangeConfig = false
    var isIgnoreSaveSetting = false

    init {
        sppManager.registerSppEventCallback(sppEventCallback)
    }

    fun isBleWay(): Boolean = configHelper.isBleWay()

    fun isUseDevAuth(): Boolean = configHelper.isUseDeviceAuth()

    fun isHidDevice(): Boolean = configHelper.isHidDevice()

    fun isUseCustomReConnectWay(): Boolean = configHelper.isUseCustomReConnectWay()

    fun isUseMultiSppChannel(): Boolean = configHelper.isUseMultiSppChannel()

    fun getRequestBleMtu(): Int = configHelper.getBleRequestMtu()

    fun getCustomSppChannel(): String? = configHelper.getCustomSppChannel()

    fun isAutoTest(): Boolean = configHelper.isAutoTest()

    fun getAutoTestCount(): Int {
        return configHelper.getAutoTestCount()
    }
    fun isDevelopMode(): Boolean {
        return configHelper.isDevelopMode()
    }
    fun isFaultTolerant(): Boolean = configHelper.isFaultTolerant()

    fun getFaultTolerantCount(): Int {
        return configHelper.getFaultTolerantCount()
    }
    fun updateSettingConfigure(
        isBleWay: Boolean,
        isUseDevAuth: Boolean,
        isHidDevice: Boolean,
        isUseCustomReconnectWay: Boolean,
        isAutoTest: Boolean,
        autoTestCount: Int,
        isUseMultiSppChannel: Boolean,
        bleRequestMtu: Int,
        customUUID: String?,
        isFaultTolerant:Boolean,
        faultTolerantCount:Int
    ): Boolean {
        var mtu = bleRequestMtu
        if (mtu < BluetoothConstant.BLE_MTU_MIN) {
            mtu = BluetoothConstant.BLE_MTU_MIN
        }
        if (mtu > BluetoothConstant.BLE_MTU_MAX) {
            mtu = BluetoothConstant.BLE_MTU_MAX
        }
        var isChange = false
        if (isBleWay != isBleWay()) {
            configHelper.setBleWay(isBleWay)
            isChange = true
        }
        if (isUseDevAuth != isUseDevAuth()) {
            configHelper.setUseDeviceAuth(isUseDevAuth)
            isChange = true
        }
        if (isHidDevice != isHidDevice()) {
            configHelper.setHidDevice(isHidDevice)
            isChange = true
        }
        if (isUseCustomReconnectWay != isUseCustomReConnectWay()) {
            configHelper.setUseCustomReConnectWay(isUseCustomReconnectWay)
            isChange = true
        }
        if (isAutoTest != isAutoTest()) {
            configHelper.setAutoTest(isAutoTest)
            isChange = true
        }
        if (autoTestCount != getAutoTestCount()) {
            configHelper.setAutoTestCount(autoTestCount)
            isChange = true
        }
        if (isUseMultiSppChannel != isUseMultiSppChannel()) {
            configHelper.setUseMultiSppChannel(isUseMultiSppChannel)
            isChange = true
        }
        if (mtu != getRequestBleMtu()) {
            configHelper.setBleRequestMtu(mtu)
            isChange = true
        }
        if (customUUID != getCustomSppChannel()) {
            configHelper.setCustomSppChannel(customUUID)
            isChange = true
        }
        if (isFaultTolerant != isFaultTolerant()) {
            configHelper.setFaultTolerant(isFaultTolerant)
            isChange = true
        }
        if (faultTolerantCount != getFaultTolerantCount()) {
            configHelper.setFaultTolerantCount(faultTolerantCount)
            isChange = true
        }
        return isChange
    }

    fun sendSppData(customUUID: String?, data: ByteArray?): Boolean {
        if (!sppManager.isSppConnected) return false
        sppManager.writeDataToSppAsync(
            sppManager.connectedSppDevice,
            UUID.fromString(customUUID),
            data
        ) { device, sppUUID, result, sendData ->
            val msg = "sendSppData :: device = ${
                BluetoothUtil.printBtDeviceInfo(
                    MainApplication.getInstance(),
                    device
                )
            }, sppUUID = $sppUUID, result = $result, data = ${CHexConver.byte2HexStr(sendData)}"
            ToastUtil.showToastLong(msg)
            JL_Log.d(tag, msg)
        }
        return true
    }

    fun destroy() {
        sppManager.unregisterSppEventCallback(sppEventCallback)
    }
}