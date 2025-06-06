package com.jieli.otasdk.tool.ota.spp.interfaces;

import android.bluetooth.BluetoothDevice;

import java.util.UUID;

/**
 * 写数据的结果回调
 *
 * @author zqjasonZhong
 * @since 2021/1/13
 */
public interface OnWriteSppDataCallback {

    /**
     * Spp写数据结果回调
     *
     * @param device  蓝牙设备
     * @param sppUUID Spp通道UUID
     * @param result  结果
     * @param data    数据
     */
    void onSppResult(BluetoothDevice device, UUID sppUUID, boolean result, byte[] data);
}
