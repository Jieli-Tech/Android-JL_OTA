package com.jieli.otasdk.data.model.setting

import android.os.Parcel
import android.os.Parcelable
import com.jieli.jl_bt_ota.constant.BluetoothConstant
import com.jieli.otasdk.data.constant.OtaConstant

/**
 * OtaConfiguration
 * @author zqjasonZhong
 * @since 2025/2/11
 * @email zhongzhuocheng@zh-jieli.com
 * @desc OTA配置
 */
class OtaConfiguration() : Parcelable {
    /*<<<--- 通用配置 --->>>*/
    /**
     * 设备认证
     */
    var isUseDeviceAuth: Boolean = false

    /**
     * 是否回连HID设备
     */
    var isHidDevice: Boolean = false

    /**
     * 是否自定义回连方式
     */
    var isCustomReconnect: Boolean = false

    /**
     * 是否开发者模式
     */
    var isDevelopMode: Boolean = false

    /*<<<--- 测试容错配置 --->>>*/
    /**
     * 是否自动化测试OTA
     */
    var isAutoTest: Boolean = false

    /**
     * 测试次数
     */
    var autoTestCount: Int = OtaConstant.AUTO_TEST_COUNT

    /**
     * 是否允许容错
     */
    var isAllowFaultTolerant = false

    /**
     * 容错次数
     */
    var faultTolerantCount = OtaConstant.AUTO_FAULT_TOLERANT_COUNT

    /*<<<--- 通讯配置 --->>>*/
    /**
     * 连接方式
     */
    var connectionWay: Int = BluetoothConstant.PROTOCOL_TYPE_BLE

    /**
     * BLE的调整MTU
     */
    var bleMtu: Int = BluetoothConstant.BLE_MTU_MAX


    constructor(parcel: Parcel) : this() {
        isUseDeviceAuth = parcel.readByte() != 0.toByte()
        isHidDevice = parcel.readByte() != 0.toByte()
        isCustomReconnect = parcel.readByte() != 0.toByte()
        isDevelopMode = parcel.readByte() != 0.toByte()
        isAutoTest = parcel.readByte() != 0.toByte()
        autoTestCount = parcel.readInt()
        isAllowFaultTolerant = parcel.readByte() != 0.toByte()
        faultTolerantCount = parcel.readInt()
        connectionWay = parcel.readInt()
        bleMtu = parcel.readInt()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeByte(if (isUseDeviceAuth) 1 else 0)
        parcel.writeByte(if (isHidDevice) 1 else 0)
        parcel.writeByte(if (isCustomReconnect) 1 else 0)
        parcel.writeByte(if (isDevelopMode) 1 else 0)
        parcel.writeByte(if (isAutoTest) 1 else 0)
        parcel.writeInt(autoTestCount)
        parcel.writeByte(if (isAllowFaultTolerant) 1 else 0)
        parcel.writeInt(faultTolerantCount)
        parcel.writeInt(connectionWay)
        parcel.writeInt(bleMtu)
    }

    override fun describeContents(): Int {
        return 0
    }

    fun isBleWay(): Boolean = connectionWay == BluetoothConstant.PROTOCOL_TYPE_BLE

    override fun toString(): String {
        return "OtaConfiguration(isUseDeviceAuth=$isUseDeviceAuth, isHidDevice=$isHidDevice, isCustomReconnect=$isCustomReconnect, isDevelopMode=$isDevelopMode, " +
                "isAutoTest=$isAutoTest, autoTestCount=$autoTestCount, isAllowFaultTolerant=$isAllowFaultTolerant, faultTolerantCount=$faultTolerantCount, " +
                "connectionWay=$connectionWay, bleMtu=$bleMtu)"
    }

    companion object CREATOR : Parcelable.Creator<OtaConfiguration> {
        override fun createFromParcel(parcel: Parcel): OtaConfiguration {
            return OtaConfiguration(parcel)
        }

        override fun newArray(size: Int): Array<OtaConfiguration?> {
            return arrayOfNulls(size)
        }
    }


}