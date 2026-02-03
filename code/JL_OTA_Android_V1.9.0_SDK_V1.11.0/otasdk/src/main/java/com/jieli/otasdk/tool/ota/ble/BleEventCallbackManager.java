package com.jieli.otasdk.tool.ota.ble;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattService;
import android.os.Handler;
import android.os.Looper;

import com.jieli.jl_bt_ota.tool.callback.BaseCallbackHelper;
import com.jieli.otasdk.tool.ota.ble.interfaces.BleEventCallback;
import com.jieli.otasdk.tool.ota.ble.model.BleScanInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Ble事件回调管理器
 *
 * @author zqjasonZhong
 * @since 2020/12/24
 */
public class BleEventCallbackManager extends BaseCallbackHelper<BleEventCallback> {

    public void onAdapterChange(boolean bEnabled) {
        callbackEvent(callback -> callback.onAdapterChange(bEnabled));
    }

    public void onDiscoveryBleChange(boolean bStart) {
        callbackEvent(callback -> callback.onDiscoveryBleChange(bStart));
    }

    public void onDiscoveryBle(BluetoothDevice device, BleScanInfo bleScanMessage) {
        callbackEvent(callback -> callback.onDiscoveryBle(device, bleScanMessage));
    }

    public void onBleConnection(BluetoothDevice device, int status) {
        callbackEvent(callback -> callback.onBleConnection(device, status));
    }

    public void onBleServiceDiscovery(BluetoothDevice device, int status, List<BluetoothGattService> services) {
        callbackEvent(callback -> callback.onBleServiceDiscovery(device, status, services));
    }

    public void onBleNotificationStatus(BluetoothDevice device, UUID serviceUuid, UUID characteristicUuid, int status) {
        callbackEvent(callback -> callback.onBleNotificationStatus(device, serviceUuid, characteristicUuid, status));
    }

    public void onBleDataBlockChanged(BluetoothDevice device, int block, int status) {
        callbackEvent(callback -> callback.onBleDataBlockChanged(device, block, status));
    }

    public void onBleDataNotification(BluetoothDevice device, UUID serviceUuid, UUID characteristicsUuid, byte[] data) {
        callbackEvent(callback -> callback.onBleDataNotification(device, serviceUuid, characteristicsUuid, data));
    }

    public void onBleWriteStatus(BluetoothDevice device, UUID serviceUuid, UUID characteristicsUuid, byte[] data, int status) {
        callbackEvent(callback -> callback.onBleWriteStatus(device, serviceUuid, characteristicsUuid, data, status));
    }

    public void onConnectionUpdated(BluetoothDevice device, int interval, int latency, int timeout, int status) {
        callbackEvent(callback -> callback.onConnectionUpdated(device, interval, latency, timeout, status));
    }

    public void onSwitchBleDevice(BluetoothDevice device) {
        callbackEvent(callback -> callback.onSwitchBleDevice(device));
    }
}
