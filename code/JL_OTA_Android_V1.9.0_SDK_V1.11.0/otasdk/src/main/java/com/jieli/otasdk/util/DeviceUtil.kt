package com.jieli.otasdk.util

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.Context
import com.jieli.otasdk.R

/**
 * DeviceUtil
 * @author zqjasonZhong
 * @since 2025/1/14
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 设备工具类
 */
object DeviceUtil {

    @SuppressLint("MissingPermission")
    fun getDeviceName(context: Context, device: BluetoothDevice?): String {
        if (!PermissionUtil.hasBluetoothConnectPermission(context) || device == null) return "N/A"
        val name = device.name ?: "N/A"
        return name.ifEmpty { "N/A" }
    }

    /**
     *
     */
    @SuppressLint("MissingPermission")
    fun getBtDeviceTypeString(context: Context, device: BluetoothDevice?): String {
        if (!PermissionUtil.hasBluetoothConnectPermission(context) || device == null) return ""
        return when (device.type) {
            BluetoothDevice.DEVICE_TYPE_CLASSIC -> context.getString(R.string.device_type_classic)
            BluetoothDevice.DEVICE_TYPE_LE -> context.getString(R.string.device_type_ble)
            BluetoothDevice.DEVICE_TYPE_DUAL -> context.getString(R.string.device_type_dual_mode)
            BluetoothDevice.DEVICE_TYPE_UNKNOWN -> context.getString(R.string.device_type_unknown)
            else -> ""
        }
    }
}