package com.jieli.otasdk.data.constant

import java.util.Locale
import java.util.UUID

/**
 * 常量声明
 *
 * @author zqjasonZhong
 * @date 2019/12/30
 */
class OtaConstant {

    companion object {

        val UUID_A2DP: UUID = UUID.fromString("0000110b-0000-1000-8000-00805f9b34fb")
        val UUID_SPP: UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb")

        //Ble协议
        const val PROTOCOL_BLE = 0

        //Spp协议
        const val PROTOCOL_SPP = 1

        const val CURRENT_PROTOCOL = PROTOCOL_BLE

        //是否使用设备认证
        const val IS_NEED_DEVICE_AUTH = true

        //是否HID设备连接
        const val HID_DEVICE_WAY = false

        //是否需要自定义连接方式
        const val NEED_CUSTOM_RECONNECT_WAY = false

        //是否使用SPP多通道连接
        const val USE_SPP_MULTIPLE_CHANNEL = false

        //是否使用自动化测试
        const val AUTO_TEST_OTA = false

        //自动化测试次数
        const val AUTO_TEST_COUNT = 30

        //是否自动化测试时允许容错
        const val AUTO_FAULT_TOLERANT = false

        //容错次数
        const val AUTO_FAULT_TOLERANT_COUNT = 1

        //dir
        const val DIR_ROOT = "JieLiOTA"
        const val DIR_UPGRADE = "upgrade"
        const val DIR_LOGCAT = "logcat"

        const val SCAN_TIMEOUT = 30 * 1000L


        fun formatString(format: String, vararg args: Any?): String {
            return String.format(Locale.ENGLISH, format, *args)
        }

    }
}