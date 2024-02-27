package com.jieli.otasdk;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.content.Context;

import androidx.annotation.NonNull;

import com.jieli.jl_bt_ota.constant.JL_Constant;
import com.jieli.jl_bt_ota.model.BleScanMessage;
import com.jieli.jl_bt_ota.util.CHexConver;
import com.jieli.jl_bt_ota.util.ParseDataUtil;

import org.junit.Test;

import java.util.List;

/**
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc
 * @since 2023/3/13
 */
class ReconnectDemo {


    @Test
    public void reconnectDevice(Context context, String mac, boolean isNewAdv) {
        //第一步:搜索回连设备
        final BluetoothLeScanner scanner = BluetoothAdapter.getDefaultAdapter().getBluetoothLeScanner();
        if (null == scanner) return;
        //开始搜索设备
        scanner.startScan(new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                super.onScanResult(callbackType, result);
                final BluetoothDevice device = result.getDevice();
                final ScanRecord record = result.getScanRecord();
                if (device == null || record == null) return;
                //第二步: 解析广播包数据，找到回连设备
                if (isNewAdv) { //使用改地址的广播包
                    //由于Android 13返回的数据有问题，需要重新解析下数据
                    BleScanMessage scanMessage = parseOTAFlagFilterWithBroad(record.getBytes(), JL_Constant.OTA_IDENTIFY);
                    if (scanMessage == null) return;
                    if (scanMessage.isOTA() && mac.equals(scanMessage.getOldBleAddress())) {
                        //第三步: 找到回连目标设备, 连接设备
                        scanner.stopScan(this);
                        device.connectGatt(context, false, bluetoothGattCallback, BluetoothDevice.TRANSPORT_LE);
                    }
                } else if (mac.equals(device.getAddress())) {
                    //第三步: 找到回连目标设备, 连接设备
                    scanner.stopScan(this);
                    device.connectGatt(context, false, bluetoothGattCallback, BluetoothDevice.TRANSPORT_LE);
                }
            }

            @Override
            public void onBatchScanResults(List<ScanResult> results) {
                super.onBatchScanResults(results);
                //搜索的设备列表
            }

            @Override
            public void onScanFailed(int errorCode) {
                super.onScanFailed(errorCode);
                //开启搜索失败
            }
        });
    }

    /**
     * OTA标识过滤规则
     *
     * @param advData    广播包数据
     * @param filterFlag 过滤标识
     * @return BLE广播信息
     */
    public BleScanMessage parseOTAFlagFilterWithBroad(byte[] advData, String filterFlag) {
        if (null == advData || advData.length <= 2) return null;
        BleScanMessage bleScanMessage = null;
        int offset = 0;
        //LTV格式
        while ((offset + 2) <= advData.length) {
            int totalLen = CHexConver.byteToInt(advData[offset]);
            if (totalLen == 0) {//兼容Android 13的补0操作
                offset++; //跳过补0位置
                continue;
            }
            if (totalLen >= 1 && offset + 1 + totalLen < advData.length) {  //自定义数据包最大长度31
                int type = CHexConver.byteToInt(advData[offset + 1]);
                if (type == 0xFF) { //匹配厂商标识
                    byte[] otaScanRecord = new byte[totalLen - 1];
                    System.arraycopy(advData, offset + 2, otaScanRecord, 0, otaScanRecord.length);
                    bleScanMessage = ParseDataUtil.parseWithOTAFlagFilter(otaScanRecord, filterFlag);
                    if (bleScanMessage != null) break;
                }
                offset += (totalLen + 1);
            } else {
                break;
            }
        }
        return bleScanMessage;
    }

    private final BluetoothGattCallback bluetoothGattCallback = new BluetoothGattCallback() {
        @Override
        public void onPhyUpdate(BluetoothGatt gatt, int txPhy, int rxPhy, int status) {
            super.onPhyUpdate(gatt, txPhy, rxPhy, status);
        }

        @Override
        public void onPhyRead(BluetoothGatt gatt, int txPhy, int rxPhy, int status) {
            super.onPhyRead(gatt, txPhy, rxPhy, status);
        }

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorRead(gatt, descriptor, status);
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
        }

        @Override
        public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
            super.onReliableWriteCompleted(gatt, status);
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            super.onReadRemoteRssi(gatt, rssi, status);
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            super.onMtuChanged(gatt, mtu, status);
        }

        @Override
        public void onServiceChanged(@NonNull BluetoothGatt gatt) {
            super.onServiceChanged(gatt);
        }
    };
}
