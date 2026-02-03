package com.jieli.otasdk.tool.ota.spp.interfaces;

import android.bluetooth.BluetoothDevice;

import java.util.UUID;

/**
 * Spp事件回调
 *
 * @author zqjasonZhong
 * @since 2021/1/13
 */
public interface ISppEventCallback {
    /**
     * 蓝牙适配器开关回调
     *
     * @param bEnabled 开关
     */
    void onAdapterChange(boolean bEnabled);

    /**
     * 搜索蓝牙设备的状态回调
     *
     * @param bStart 搜索状态
     */
    void onDiscoveryDeviceChange(boolean bStart);

    /**
     * 发现蓝牙设备的回调
     *
     * @param device 蓝牙设备对象
     * @param rssi   信号强度
     */
    void onDiscoveryDevice(BluetoothDevice device, int rssi);

    /**
     * spp连接状态回调
     *
     * @param device 蓝牙设备对象
     * @param uuid   spp通道
     * @param status 连接状态
     *               <p>
     *               参考{@link android.bluetooth.BluetoothProfile#STATE_DISCONNECTED} : 未连接<br>
     *               {@link android.bluetooth.BluetoothProfile#STATE_CONNECTING} : 连接中<br>
     *               {@link android.bluetooth.BluetoothProfile#STATE_CONNECTED} : 已连接<br>
     *               {@link android.bluetooth.BluetoothProfile#STATE_DISCONNECTING} : 正在断开<br>
     *               </p>
     */
    void onSppConnection(BluetoothDevice device, UUID uuid, int status);

    /**
     * 从spp通道接收到的数据
     *
     * @param device 蓝牙设备对象
     * @param uuid   UUID值
     * @param data   裸数据
     */
    void onReceiveSppData(BluetoothDevice device, UUID uuid, byte[] data);
}
