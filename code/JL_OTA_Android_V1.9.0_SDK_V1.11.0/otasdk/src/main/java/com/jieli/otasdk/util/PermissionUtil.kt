package com.jieli.otasdk.util

import android.Manifest
import android.content.Context
import android.location.LocationManager
import android.os.Build
import permissions.dispatcher.PermissionUtils

/**
 * PermissionUtil
 * @author zqjasonZhong
 * @since 2024/8/12
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 权限工具类
 */
object PermissionUtil {

    /**
     * 应用是否具有位置权限
     *
     * @param context Context 上下文
     * @return Boolean 结果
     */
    fun hasLocationPermission(context: Context): Boolean {
        return PermissionUtils.hasSelfPermissions(context, Manifest.permission.ACCESS_FINE_LOCATION)
    }

    /**
     * 是否授予蓝牙连接权限
     *
     * @param context Context 上下文
     * @return Boolean 结果
     */
    fun hasBluetoothConnectPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PermissionUtils.hasSelfPermissions(context, Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            return true
        }
    }

    /**
     * 是否授予蓝牙扫描权限
     *
     * @param context Context 上下文
     * @return Boolean 结果
     */
    fun hasBluetoothScanPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PermissionUtils.hasSelfPermissions(context, Manifest.permission.BLUETOOTH_SCAN)
        } else {
            return true
        }
    }

    /**
     * 是否授予蓝牙权限
     *
     * @param context Context 上下文
     * @return Boolean 结果
     */
    fun hasBluetoothPermission(context: Context): Boolean {
        return hasBluetoothScanPermission(context) && hasBluetoothConnectPermission(context)
    }

    /**
     * 手机系统是否开启位置服务
     *
     * @param context Context 上下文
     * @return Boolean 结果
     */
    fun isLocationServiceEnabled(context: Context): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    /**
     * 应用是否具有读取外部存储器权限
     *
     * @param context Context 上下文
     * @return Boolean 结果
     */
    fun hasReadStoragePermission(context: Context): Boolean {
        return PermissionUtils.hasSelfPermissions(
            context,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
    }

    /**
     * 应用是否具有写入外部存储器权限
     *
     * @param context Context 上下文
     * @return Boolean 结果
     */
    fun hasWriteStoragePermission(context: Context): Boolean {
        return PermissionUtils.hasSelfPermissions(
            context,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }
}