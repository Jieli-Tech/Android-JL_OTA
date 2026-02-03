package com.jieli.otasdk.tool.ota.spp.interfaces;

import android.bluetooth.BluetoothDevice;

import java.util.UUID;

/**
 * Spp事件回调抽象类
 *
 * @author zqjasonZhong
 * @since 2021/1/13
 */
public abstract class SppEventCallback implements ISppEventCallback {

    @Override
    public void onAdapterChange(boolean bEnabled) {

    }

    @Override
    public void onDiscoveryDeviceChange(boolean bStart) {

    }

    @Override
    public void onDiscoveryDevice(BluetoothDevice device, int rssi) {

    }

    @Override
    public void onSppConnection(BluetoothDevice device, UUID uuid, int status) {

    }

    @Override
    public void onReceiveSppData(BluetoothDevice device, UUID uuid, byte[] data) {

    }
}
