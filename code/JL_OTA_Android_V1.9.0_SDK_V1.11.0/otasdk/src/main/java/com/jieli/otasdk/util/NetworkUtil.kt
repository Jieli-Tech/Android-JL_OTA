package com.jieli.otasdk.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import com.jieli.jl_bt_ota.util.JL_Log
import java.util.Locale

/**
 * NetworkUtil
 * @author zqjasonZhong
 * @since 2025/6/25
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 网络工具
 */
object NetworkUtil {

    // 检查是否连接到Wi-Fi
    fun isWifiConnected(context: Context): Boolean {
        val connectivityManager = context.getSystemService(
            Context.CONNECTIVITY_SERVICE
        ) as ConnectivityManager

        val network = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            connectivityManager.activeNetwork ?: return false
        } else {
            connectivityManager.allNetworks.let { list ->
                if (list.isEmpty()) return false
                list[0]
            }
        }
        val capabilities = connectivityManager.getNetworkCapabilities(network)
        return capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
    }

    // 获取Wi-Fi IP地址（仅当连接Wi-Fi时有效）
    fun getWifiIpAddress(context: Context): String? {
        if (!isWifiConnected(context)) return null
        return try {
            val wifiManager = context.applicationContext.getSystemService(
                Context.WIFI_SERVICE
            ) as WifiManager

            val wifiInfo = wifiManager.connectionInfo
            val ipAddress = wifiInfo.ipAddress

            // 转换整型IP为点分十进制格式
            String.format(
                Locale.ENGLISH,
                "%d.%d.%d.%d",
                ipAddress and 0xff,
                ipAddress shr 8 and 0xff,
                ipAddress shr 16 and 0xff,
                ipAddress shr 24 and 0xff
            )
        } catch (e: Exception) {
            JL_Log.e("NetworkUtil", "getWifiIpAddress", "Error getting WiFi IP. $e")
            null
        }
    }
}