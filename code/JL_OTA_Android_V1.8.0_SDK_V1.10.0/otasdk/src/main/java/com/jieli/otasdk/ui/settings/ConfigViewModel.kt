package com.jieli.otasdk.ui.settings

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.jieli.jl_bt_ota.constant.BluetoothConstant
import com.jieli.jl_bt_ota.util.JL_Log
import com.jieli.otasdk.MainApplication
import com.jieli.otasdk.data.model.setting.OtaConfiguration
import com.jieli.otasdk.tool.config.ConfigHelper

/**
 * @author zqjasonZhong
 * @since 2022/9/13
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 配置逻辑实现
 */
class ConfigViewModel : ViewModel() {
    private val tag = ConfigViewModel::class.java.simpleName
    private val configHelper = ConfigHelper.getInstance()

    /**
     * 配置更改回调
     */
    val configChangeMLD = MutableLiveData<Boolean>()

    /**
     * 保存配置回调
     */
    val saveSettingMLD = MutableLiveData<Boolean>()

    init {
        configChangeMLD.postValue(false)
        saveSettingMLD.postValue(false)
    }

    fun getLogFileDirPath(): String = MainApplication.instance.logFileDir

    fun isChangeConfig(): Boolean = configChangeMLD.value == true

    fun isAutoTest(): Boolean = configHelper.isAutoTest()

    fun getOtaConfiguration(): OtaConfiguration = OtaConfiguration().apply {
        isUseDeviceAuth = configHelper.isUseDeviceAuth()
        isHidDevice = configHelper.isHidDevice()
        isCustomReconnect = configHelper.isUseCustomReConnectWay()

        isAutoTest = configHelper.isAutoTest()
        autoTestCount = configHelper.getAutoTestCount()
        isAllowFaultTolerant = configHelper.isFaultTolerant()
        faultTolerantCount = configHelper.getFaultTolerantCount()

        connectionWay = if (configHelper.isBleWay()) {
            BluetoothConstant.PROTOCOL_TYPE_BLE
        } else {
            BluetoothConstant.PROTOCOL_TYPE_SPP
        }
        bleMtu = configHelper.getBleRequestMtu()
    }

    fun updateSettingConfigure(newCfg: OtaConfiguration, isSave: Boolean = false) {
        var isChangeCfg = false
        getOtaConfiguration().let { cfg ->
            JL_Log.d(tag, "updateSettingConfigure", "save cfg : $cfg, \n new cfg : $newCfg")
            if (cfg.isUseDeviceAuth != newCfg.isUseDeviceAuth) {
                if (isSave) {
                    configHelper.setUseDeviceAuth(newCfg.isUseDeviceAuth)
                }
                isChangeCfg = true
            }
            if (cfg.isHidDevice != newCfg.isHidDevice) {
                if (isSave) {
                    configHelper.setHidDevice(newCfg.isHidDevice)
                }
                isChangeCfg = true
            }
            if (cfg.isCustomReconnect != newCfg.isCustomReconnect) {
                if (isSave) {
                    configHelper.setUseCustomReConnectWay(newCfg.isCustomReconnect)
                }
                isChangeCfg = true
            }

            if (cfg.isAutoTest != newCfg.isAutoTest) {
                if (isSave) {
                    configHelper.setAutoTest(newCfg.isAutoTest)
                }
                isChangeCfg = true
            }
            if (cfg.autoTestCount != newCfg.autoTestCount) {
                if (isSave) {
                    configHelper.setAutoTestCount(newCfg.autoTestCount)
                }
                isChangeCfg = true
            }
            if (cfg.isAllowFaultTolerant != newCfg.isAllowFaultTolerant) {
                if (isSave) {
                    configHelper.setFaultTolerant(newCfg.isAllowFaultTolerant)
                }
                isChangeCfg = true
            }
            if (cfg.faultTolerantCount != newCfg.faultTolerantCount) {
                if (isSave) {
                    configHelper.setFaultTolerantCount(newCfg.faultTolerantCount)
                }
                isChangeCfg = true
            }

            if (cfg.isBleWay() != newCfg.isBleWay()) {
                if (isSave) {
                    configHelper.setBleWay(newCfg.isBleWay())
                }
                isChangeCfg = true
            }
            val bleMtu = formatBleMtu(cfg.bleMtu)
            val newBleMtu = formatBleMtu(newCfg.bleMtu)
            if (bleMtu != newBleMtu) {
                if (isSave) {
                    configHelper.setBleRequestMtu(newBleMtu)
                }
                isChangeCfg = true
            }
        }
        JL_Log.d(tag, "updateSettingConfigure", "isChangeCfg : $isChangeCfg, \n isSave : $isSave")
        if (isChangeCfg && isSave) {
            isChangeCfg = false
            saveSettingMLD.value = false //重置值
        }
        configChangeMLD.postValue(isChangeCfg)
    }

    fun saveConfiguration(isSave: Boolean) {
        if (!isChangeConfig()) return
        if (!isSave) {
            configChangeMLD.value = false
        }
        saveSettingMLD.postValue(isSave)
    }

    private fun formatBleMtu(mtu: Int): Int {
        if (mtu < BluetoothConstant.BLE_MTU_MIN) {
            return BluetoothConstant.BLE_MTU_MIN
        }
        if (mtu > BluetoothConstant.BLE_MTU_MAX) {
            return BluetoothConstant.BLE_MTU_MAX
        }
        return mtu;
    }
}