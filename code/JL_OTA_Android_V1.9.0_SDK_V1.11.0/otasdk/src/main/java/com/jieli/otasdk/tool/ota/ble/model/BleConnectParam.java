package com.jieli.otasdk.tool.ota.ble.model;

import androidx.annotation.NonNull;

import com.jieli.jl_bt_ota.constant.BluetoothConstant;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * BleConnectParam
 *
 * @author zhongzhuocheng
 * email: zhongzhuocheng@zh-jieli.com
 * create: 2026/1/5
 * note: BLE连接参数
 */
public class BleConnectParam {
    /*--------------------------------------------------------------------
     * TRANSPORT
     *--------------------------------------------------------------------*/
    /**
     * No preference of physical transport for GATT connections to remote dual-mode devices
     */
    public static final int TRANSPORT_AUTO = 0;
    /**
     * Constant representing the BR/EDR transport.
     */
    public static final int TRANSPORT_BREDR = 1;
    /**
     * Constant representing the Bluetooth Low Energy (BLE) Transport.
     */
    public static final int TRANSPORT_LE = 2;

    /**
     * 打印BLE透传协议
     *
     * @param transport int BLE透传协议
     * @return String 打印信息
     */
    public static String printBleTransport(int transport) {
        switch (transport) {
            case TRANSPORT_AUTO:
                return "TRANSPORT_AUTO(0)";
            case TRANSPORT_BREDR:
                return "TRANSPORT_BREDR(1)";
            case TRANSPORT_LE:
                return "TRANSPORT_LE(2)";
            default:
                return "UNKNOWN_TRANSPORT : " + transport;
        }
    }

    /**
     * OTA服务UUID
     */
    private static final String KEY_OTA_SERVICE_UUID = "ota_service_uuid";
    /**
     * OTA写特征UUID
     */
    private static final String KEY_OTA_WRITE_CHARACTERISTIC_UUID = "ota_write_characteristic_uuid";
    /**
     * OTA通知特征UUID
     */
    private static final String KEY_OTA_NOTIFY_CHARACTERISTIC_UUID = "ota_notify_characteristic_uuid";

    /**
     * 连接超时时间
     */
    private final long timeout;
    /**
     * 是否需要主动回连
     */
    private boolean isNeedReconnect = true;
    /**
     * GATT连接到双模设备优先传输方式
     */
    private int transport = TRANSPORT_LE;
    /**
     * 请求MTU
     */
    private int requestMtu = BluetoothConstant.BLE_MTU_MIN;
    /**
     * UUID集合
     */
    private final Map<String, UUID> otaUUIDMap;

    public BleConnectParam() {
        this(BluetoothConstant.CONNECT_TIMEOUT);
    }

    public BleConnectParam(long timeout) {
        this.timeout = timeout;

        otaUUIDMap = new HashMap<>();
        setOtaUUID(BluetoothConstant.UUID_SERVICE, BluetoothConstant.UUID_WRITE, BluetoothConstant.UUID_NOTIFICATION);
    }

    public long getTimeout() {
        return timeout;
    }

    public boolean isNeedReconnect() {
        return isNeedReconnect;
    }

    public BleConnectParam setNeedReconnect(boolean needReconnect) {
        isNeedReconnect = needReconnect;
        return this;
    }

    public int getTransport() {
        return transport;
    }

    public BleConnectParam setTransport(int transport) {
        this.transport = transport;
        return this;
    }

    public int getRequestMtu() {
        return requestMtu;
    }

    public BleConnectParam setRequestMtu(int requestMtu) {
        this.requestMtu = requestMtu;
        return this;
    }

    public BleConnectParam setOtaUUID(@NonNull UUID servieUUID, @NonNull UUID writeCharacteristicUUID, @NonNull UUID notifyCharacteristicUUID) {
        otaUUIDMap.put(KEY_OTA_SERVICE_UUID, servieUUID);
        otaUUIDMap.put(KEY_OTA_WRITE_CHARACTERISTIC_UUID, writeCharacteristicUUID);
        otaUUIDMap.put(KEY_OTA_NOTIFY_CHARACTERISTIC_UUID, notifyCharacteristicUUID);
        return this;
    }

    public UUID getOtaServiceUUID() {
        return otaUUIDMap.get(KEY_OTA_SERVICE_UUID);
    }

    public UUID getOtaWriteCharacteristicUUID() {
        return otaUUIDMap.get(KEY_OTA_WRITE_CHARACTERISTIC_UUID);
    }

    public UUID getOtaNotifyCharacteristicUUID() {
        return otaUUIDMap.get(KEY_OTA_NOTIFY_CHARACTERISTIC_UUID);
    }

    @Override
    public String toString() {
        return "BleConnectParam{" +
                "timeout=" + timeout +
                ", isNeedReconnect=" + isNeedReconnect +
                ", transport=" + printBleTransport(transport) +
                ", requestMtu=" + requestMtu +
                ", otaUUIDMap=" + otaUUIDMap +
                '}';
    }
}
