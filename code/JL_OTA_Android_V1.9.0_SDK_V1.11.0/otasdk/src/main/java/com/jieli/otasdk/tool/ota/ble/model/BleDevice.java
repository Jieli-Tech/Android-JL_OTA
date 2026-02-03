package com.jieli.otasdk.tool.ota.ble.model;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;

import androidx.annotation.NonNull;

import com.jieli.jl_bt_ota.constant.BluetoothConstant;
import com.jieli.jl_bt_ota.util.BluetoothUtil;
import com.jieli.jl_bt_ota.util.CHexConver;
import com.jieli.jl_bt_ota.util.JL_Log;
import com.jieli.otasdk.MainApplication;
import com.jieli.otasdk.tool.ota.ble.SendBleDataThread;
import com.jieli.otasdk.tool.ota.ble.interfaces.IBleOp;
import com.jieli.otasdk.tool.ota.ble.interfaces.OnThreadStateListener;
import com.jieli.otasdk.tool.ota.ble.interfaces.OnWriteDataCallback;
import com.jieli.otasdk.util.AppUtil;

import java.util.UUID;

/**
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc BLE设备
 * @since 2022/12/5
 */
public class BleDevice {
    private final static String TAG = "BleManager";

    private final static int MAX_RETRY_CONNECT_COUNT = 1;//最大尝试连接次数

    /**
     * 蓝牙设备类型
     */
    @NonNull
    private final BluetoothDevice device;
    /**
     * 连接参数
     */
    @NonNull
    private final BleConnectParam param;
    /**
     * 重连次数限制
     */
    private final int reconnectLimit;
    /**
     * 连接状态
     */
    private int connection = BluetoothProfile.STATE_DISCONNECTED;
    /**
     * 协商后的MTU值
     */
    private int mtu = BluetoothConstant.BLE_MTU_MIN;
    /**
     * GATT对象
     */
    private BluetoothGatt gatt;
    /**
     * 发送数据线程
     */
    private SendBleDataThread sendDataThread;
    /**
     * 连接时间戳
     */
    private long connectedTime;
    /**
     * 是否需要主动回连
     */
    private boolean isNeedReconnect;
    /**
     * 重连次数
     */
    private int reconnectCount = 0;
    /**
     * 是否正在使用
     */
    private boolean isUsing;

    public BleDevice(@NonNull BluetoothDevice device, @NonNull BleConnectParam param) {
        this(device, param, MAX_RETRY_CONNECT_COUNT);
    }

    public BleDevice(@NonNull BluetoothDevice device, @NonNull BleConnectParam param, int reconnectLimit) {
        this.device = device;
        this.param = param;
        this.reconnectLimit = reconnectLimit;
    }

    @NonNull
    public BluetoothDevice getDevice() {
        return device;
    }

    @NonNull
    public BleConnectParam getParam() {
        return param;
    }

    public int getConnection() {
        return connection;
    }

    public BleDevice setConnection(int connection) {
        if (this.connection != connection) {
            this.connection = connection;
            if (connection == BluetoothProfile.STATE_CONNECTED) {
                setConnectedTime(System.currentTimeMillis());
                startSendDataThread();
            } else if (connection == BluetoothProfile.STATE_DISCONNECTED) {
                setConnectedTime(0L);
                setUsing(false);
                stopSendDataThread();
            } else if (connection == BluetoothProfile.STATE_CONNECTING) {
                setConnectedTime(System.currentTimeMillis());
                setUsing(false);
            }
        }
        return this;
    }

    public boolean isConnecting() {
        return connection == BluetoothProfile.STATE_CONNECTING;
    }

    public boolean isConnected() {
        return connection == BluetoothProfile.STATE_CONNECTED;
    }

    public BluetoothGatt getGatt() {
        return gatt;
    }

    public BleDevice setGatt(BluetoothGatt gatt) {
        this.gatt = gatt;
        return this;
    }

    public int getMtu() {
        int realMtu = mtu;
        if (realMtu > 128) {
            realMtu -= 6;
        }
        return realMtu;
    }

    public BleDevice setMtu(int mtu) {
        this.mtu = mtu;
        return this;
    }

    public long getConnectedTime() {
        return connectedTime;
    }

    public BleDevice setConnectedTime(long connectedTime) {
        this.connectedTime = connectedTime;
        return this;
    }

    public boolean isOverReconnectLimit() {
        reconnectCount++;
        return reconnectCount > reconnectLimit;
    }

    public boolean isNeedReconnect() {
        return isNeedReconnect;
    }

    public BleDevice setNeedReconnect(boolean needReconnect) {
        isNeedReconnect = needReconnect;
        return this;
    }

    public boolean isUsing() {
        return isUsing;
    }

    public void setUsing(boolean using) {
        isUsing = using;
    }

    /**
     * 是否连接Gatt over BR/EDR
     */
    public boolean isConnectGattOverBrEdr() {
        return param.getTransport() == BluetoothConstant.TRANSPORT_BREDR;
    }

    public void wakeupSendThread(BleSendTask task) {
        if (sendDataThread != null && task != null && gatt.equals(task.getBleGatt())) {
            sendDataThread.wakeupSendThread(task);
        }
    }

    public boolean addSendTask(UUID serviceUUID, UUID characteristicUUID, byte[] data, OnWriteDataCallback callback) {
        boolean ret = false;
        if (sendDataThread != null && sendDataThread.isRunning()) {
            ret = sendDataThread.addSendTask(gatt, serviceUUID, characteristicUUID, data, callback);
        }
        return ret;
    }

    private void startSendDataThread() {
        if (sendDataThread == null || !sendDataThread.isRunning()) {
            sendDataThread = new SendBleDataThread(new IBleOp() {
                @Override
                public int getBleMtu() {
                    return getMtu();
                }

                @Override
                public boolean writeDataByBle(BluetoothGatt gatt, UUID serviceUUID, UUID characteristicUUID, byte[] data) {
                    return writeDataToDeviceByBle(gatt, serviceUUID, characteristicUUID, data);
                }
            }, new OnThreadStateListener() {
                @Override
                public void onStart(long id, String name) {

                }

                @Override
                public void onEnd(long id, String name) {
                    if (sendDataThread != null && sendDataThread.getId() == id) {
                        sendDataThread = null;
                    }
                }
            });
            sendDataThread.start();
        }
    }

    private void stopSendDataThread() {
        if (sendDataThread != null) {
            sendDataThread.stopThread();
        }
    }

    @SuppressLint("MissingPermission")
    private boolean writeDataToDeviceByBle(BluetoothGatt gatt, UUID serviceUUID, UUID characteristicUUID, byte[] data) {
        if (gatt == null || null == serviceUUID || null == characteristicUUID || null == data || data.length == 0
                || !AppUtil.checkHasConnectPermission(MainApplication.getInstance())) {
            JL_Log.d(TAG, "writeDataByBle", "param is invalid.");
            return false;
        }
        BluetoothGattService gattService = gatt.getService(serviceUUID);
        if (null == gattService) {
            JL_Log.d(TAG, "writeDataByBle", "service is null.");
            return false;
        }
        BluetoothGattCharacteristic gattCharacteristic = gattService.getCharacteristic(characteristicUUID);
        if (null == gattCharacteristic) {
            JL_Log.d(TAG, "writeDataByBle", "characteristic is null");
            return false;
        }
        boolean ret = false;
        try {
            gattCharacteristic.setValue(data);
            ret = gatt.writeCharacteristic(gattCharacteristic);
        } catch (Exception e) {
            e.printStackTrace();
        }
        JL_Log.d(TAG, "writeDataByBle", "device : " + gatt.getDevice() + ", put send queue : " + ret + ", data = " + CHexConver.byte2HexStr(data));
        return ret;
    }

    @Override
    public String toString() {
        return "BleDevice{" +
                "device=" + device +
                ", reconnectLimit=" + reconnectLimit +
                ", connection=" + BluetoothUtil.printBtConnectionState(connection) +
                ", mtu=" + mtu +
                ", connectedTime=" + connectedTime +
                ", gatt=" + gatt +
                ", sendDataThread=" + sendDataThread +
                ", reconnectCount=" + reconnectCount +
                ", isNeedReconnect=" + isNeedReconnect +
                ", isUsing=" + isUsing +
                '}';
    }
}
