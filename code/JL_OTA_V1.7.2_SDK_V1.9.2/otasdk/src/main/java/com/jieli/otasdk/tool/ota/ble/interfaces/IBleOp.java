package com.jieli.otasdk.tool.ota.ble.interfaces;

import android.bluetooth.BluetoothGatt;

import java.util.UUID;

/**
 * @author zqjasonZhong
 * @since 2020/12/24
 */
public interface IBleOp {

    int getBleMtu();

    boolean writeDataByBle(BluetoothGatt gatt, UUID serviceUUID, UUID characteristicUUID, byte[] data);
}
