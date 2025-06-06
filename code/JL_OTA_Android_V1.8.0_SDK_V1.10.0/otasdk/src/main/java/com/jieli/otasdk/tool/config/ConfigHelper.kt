package com.jieli.otasdk.tool.config

import android.annotation.SuppressLint
import android.content.Context
import androidx.annotation.IntRange
import com.jieli.component.utils.SystemUtil
import com.jieli.jl_bt_ota.constant.BluetoothConstant
import com.jieli.otasdk.MainApplication
import com.jieli.otasdk.data.constant.OtaConstant

/**
 * @author zqjasonZhong
 * @since 2022/9/8
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 配置辅助类
 */
open class ConfigHelper private constructor(context: Context) {

    companion object {
        /**
         * 最新的协议对应的APP版本号
         */
        private const val LATEST_POLICY_APP_VERSION = 10800

        //download文件夹
        private const val KEY_DOWNLOAD_URI = "download_uri"

        //同意协议版本号
        private const val KEY_AGREE_POLICY_VERSION = "agree_policy_version"

        //通讯方式
        private const val KEY_COMMUNICATION_WAY = "communication_way"

        //是否使用设备认证
        private const val KEY_IS_USE_DEVICE_AUTH = "is_use_device_auth"

        //是否HID设备
        private const val KEY_IS_HID_DEVICE = "is_hid_device"

        //是否使用自定义回连方式
        private const val KEY_USE_CUSTOM_RECONNECT_WAY = "use_custom_reconnect_way"

        //BLE的MTU请求值
        private const val KEY_BLE_MTU_VALUE = "ble_mtu_value"

        //是否启用SPP多通道
        private const val KEY_SPP_MULTIPLE_CHANNEL = "spp_multiple_channel"

        //自定义SPP通道
        private const val KEY_SPP_CUSTOM_UUID = "spp_custom_uuid"

        //是否自动测试OTA
        private const val KEY_AUTO_TEST_OTA = "auto_test_ota"

        //自动化测试次数
        private const val KEY_AUTO_TEST_COUNT = "auto_test_count"

        //是否自动测试OTA时，允许容错
        private const val KEY_FAULT_TOLERANT = "fault_tolerant"

        //容错次数
        private const val KEY_FAULT_TOLERANT_COUNT = "fault_tolerant_count"

        //扫描过滤参数
        private const val KEY_SCAN_FILTER_STRING = "scan_filter_string"

        //开发者模式
        private const val KEY_DEVELOP_MODE = "develop_mode"

        // 广播音箱模式
        private const val KEY_BROADCAST_BOX = "broadcast_box_switch"

        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var instance: ConfigHelper? = null
        fun getInstance() =
            instance ?: synchronized(this) {
                instance ?: ConfigHelper(MainApplication.instance).also { instance = it }
            }
    }

    private val preferences = context.getSharedPreferences("ota_config_data", Context.MODE_PRIVATE)

    fun isAgreePolicy(): Boolean {
        val cacheVersion = preferences.getInt(KEY_AGREE_POLICY_VERSION, 0) //获取同意协议版本号
        if (cacheVersion <= 0) return false
        return cacheVersion >= LATEST_POLICY_APP_VERSION
    }

    fun setAgreePolicyVersion(context: Context) {
        val appVersion = SystemUtil.getVersion(context) //获取当前APP版本号
        preferences.edit().putInt(KEY_AGREE_POLICY_VERSION, appVersion).apply()
    }

    fun isBleWay(): Boolean = preferences.getInt(
        KEY_COMMUNICATION_WAY,
        OtaConstant.CURRENT_PROTOCOL
    ) == OtaConstant.PROTOCOL_BLE

    fun setBleWay(isBle: Boolean) {
        val way = if (isBle) OtaConstant.PROTOCOL_BLE else OtaConstant.PROTOCOL_SPP
        preferences.edit().putInt(KEY_COMMUNICATION_WAY, way).apply()
    }

    fun isUseDeviceAuth(): Boolean =
        preferences.getBoolean(KEY_IS_USE_DEVICE_AUTH, OtaConstant.IS_NEED_DEVICE_AUTH)

    fun setUseDeviceAuth(isAuth: Boolean) {
        preferences.edit().putBoolean(KEY_IS_USE_DEVICE_AUTH, isAuth).apply()
    }

    fun isHidDevice(): Boolean =
        preferences.getBoolean(KEY_IS_HID_DEVICE, OtaConstant.HID_DEVICE_WAY)

    fun setHidDevice(isHid: Boolean) {
        preferences.edit().putBoolean(KEY_IS_HID_DEVICE, isHid).apply()
    }

    fun isUseCustomReConnectWay(): Boolean = preferences.getBoolean(
        KEY_USE_CUSTOM_RECONNECT_WAY,
        OtaConstant.NEED_CUSTOM_RECONNECT_WAY
    )

    fun setUseCustomReConnectWay(isCustom: Boolean) {
        preferences.edit().putBoolean(KEY_USE_CUSTOM_RECONNECT_WAY, isCustom).apply()
    }

    fun getBleRequestMtu(): Int =
        preferences.getInt(KEY_BLE_MTU_VALUE, BluetoothConstant.BLE_MTU_MAX)

    fun setBleRequestMtu(@IntRange(from = 20, to = 509) mtu: Int) {
        preferences.edit().putInt(KEY_BLE_MTU_VALUE, mtu).apply()
    }

    fun isUseMultiSppChannel(): Boolean = preferences.getBoolean(
        KEY_SPP_MULTIPLE_CHANNEL,
        OtaConstant.USE_SPP_MULTIPLE_CHANNEL
    )

    fun setUseMultiSppChannel(isUseMulti: Boolean) {
        preferences.edit().putBoolean(KEY_SPP_MULTIPLE_CHANNEL, isUseMulti).apply()
    }

    fun getCustomSppChannel(): String? =
        preferences.getString(KEY_SPP_CUSTOM_UUID, OtaConstant.UUID_SPP.toString())

    fun setCustomSppChannel(uuid: String?) {
        preferences.edit().putString(KEY_SPP_CUSTOM_UUID, uuid).apply()
    }

    fun isAutoTest(): Boolean = preferences.getBoolean(KEY_AUTO_TEST_OTA, OtaConstant.AUTO_TEST_OTA)

    fun setAutoTest(isAutoTest: Boolean) {
        preferences.edit().putBoolean(KEY_AUTO_TEST_OTA, isAutoTest).apply()
    }

    fun getAutoTestCount(): Int = preferences.getInt(
        KEY_AUTO_TEST_COUNT,
        OtaConstant.AUTO_TEST_COUNT
    )

    fun setAutoTestCount(count: Int) {
        if (!isAutoTest()) return
        preferences.edit().putInt(KEY_AUTO_TEST_COUNT, count).apply()
    }

    fun isFaultTolerant(): Boolean =
        preferences.getBoolean(KEY_FAULT_TOLERANT, OtaConstant.AUTO_FAULT_TOLERANT)

    fun setFaultTolerant(isFaultTolerant: Boolean) {
        preferences.edit().putBoolean(KEY_FAULT_TOLERANT, isFaultTolerant).apply()
    }

    fun getFaultTolerantCount(): Int = preferences.getInt(
        KEY_FAULT_TOLERANT_COUNT,
        OtaConstant.AUTO_FAULT_TOLERANT_COUNT
    )

    fun setFaultTolerantCount(count: Int) {
        if (!isFaultTolerant()) return
        preferences.edit().putInt(KEY_FAULT_TOLERANT_COUNT, count).apply()
    }

    fun getScanFilter(): String? =
        preferences.getString(KEY_SCAN_FILTER_STRING, "")

    fun setScanFilter(scanFilter: String?) {
        preferences.edit().putString(KEY_SCAN_FILTER_STRING, scanFilter).apply()
    }

    fun isDevelopMode(): Boolean =
        preferences.getBoolean(KEY_DEVELOP_MODE, false)

    fun setDevelopMode(developMode: Boolean) {
        preferences.edit().putBoolean(KEY_DEVELOP_MODE, developMode).apply()
    }

    fun isEnableBroadcastBox(): Boolean =
        preferences.getBoolean(KEY_BROADCAST_BOX, false)

    fun enableBroadcastBox(enable: Boolean) {
        preferences.edit().putBoolean(KEY_BROADCAST_BOX, enable).apply()
    }
}