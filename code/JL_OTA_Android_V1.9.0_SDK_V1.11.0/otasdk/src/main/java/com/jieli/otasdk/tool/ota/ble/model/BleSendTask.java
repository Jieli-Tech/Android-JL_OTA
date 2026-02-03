package com.jieli.otasdk.tool.ota.ble.model;

import android.bluetooth.BluetoothGatt;

import com.jieli.otasdk.tool.ota.ble.interfaces.OnWriteDataCallback;

import java.util.UUID;

/**
 * BleSendTask
 *
 * @author zhongzhuocheng
 * email: zhongzhuocheng@zh-jieli.com
 * create: 2025/10/30
 * note: BLE发送任务
 */
public class BleSendTask {
    private BluetoothGatt mGatt;
    private UUID serviceUUID;
    private UUID characteristicUUID;
    private byte[] data;
    private int status = -1;
    private OnWriteDataCallback mCallback;

    public BleSendTask(BluetoothGatt gatt, UUID serviceUUID, UUID characteristicUUID, byte[] data, OnWriteDataCallback callback) {
        this.mGatt = gatt;
        this.serviceUUID = serviceUUID;
        this.characteristicUUID = characteristicUUID;
        this.data = data;
        this.mCallback = callback;
    }

    public BluetoothGatt getBleGatt() {
        return mGatt;
    }

    public void setDevice(BluetoothGatt gatt) {
        mGatt = gatt;
    }

    public UUID getServiceUUID() {
        return serviceUUID;
    }

    public void setServiceUUID(UUID serviceUUID) {
        this.serviceUUID = serviceUUID;
    }

    public UUID getCharacteristicUUID() {
        return characteristicUUID;
    }

    public void setCharacteristicUUID(UUID characteristicUUID) {
        this.characteristicUUID = characteristicUUID;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public OnWriteDataCallback getCallback() {
        return mCallback;
    }

    public void setCallback(OnWriteDataCallback callback) {
        mCallback = callback;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "BleSendTask{" +
                "mGatt=" + mGatt +
                ", serviceUUID=" + serviceUUID +
                ", characteristicUUID=" + characteristicUUID +
                ", data=" + (null != data ? data.length : 0) +
                ", status=" + status +
                ", mCallback=" + mCallback +
                '}';
    }

    @Override
    public int hashCode() {
        if (mGatt != null && serviceUUID != null && characteristicUUID != null) {
            return mGatt.hashCode() + serviceUUID.hashCode() + characteristicUUID.hashCode();
        }
        return super.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof BleSendTask) {
            BleSendTask other = (BleSendTask) obj;
            if (mGatt != null && serviceUUID != null && characteristicUUID != null) {
                return mGatt.equals(other.getBleGatt()) && serviceUUID.equals(other.getServiceUUID())
                        && characteristicUUID.equals(other.getCharacteristicUUID());
            }
        }
        return false;
    }
}
