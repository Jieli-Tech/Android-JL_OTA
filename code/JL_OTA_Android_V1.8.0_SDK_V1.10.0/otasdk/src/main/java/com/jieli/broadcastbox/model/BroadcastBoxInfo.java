package com.jieli.broadcastbox.model;

import android.bluetooth.BluetoothDevice;

import androidx.annotation.NonNull;

import com.jieli.jl_bt_ota.util.BluetoothUtil;
import com.jieli.otasdk.MainApplication;
import com.jieli.otasdk.data.model.device.ScanDevice;
import com.jieli.otasdk.util.AppUtil;

import java.io.File;
import java.util.Objects;

/**
 * Des:
 * author: Bob
 * date: 2022/12/02
 * Copyright: Jieli Technology
 * Modify date:
 * Modified by:
 */
public class BroadcastBoxInfo extends ScanDevice {
    private int uid = -1;
    private int pid = -1;
    private int type;// 设备类型, 0x00 -- 音箱类型
    private File selectFile;// 选中文件
    private boolean chosen;// 选中设备
    private boolean connected;// 连接状态
    private boolean forceUpdate;// 强制更新
//    private MultiOTAManager otaManager;// 每个设备对应的升级管理类

    public BroadcastBoxInfo(@NonNull BluetoothDevice device, int rssi) {
        super(device, rssi);
    }

    public int getUid() {
        return uid;
    }

    public void setUid(int uid) {
        this.uid = uid;
    }

    public int getPid() {
        return pid;
    }

    public void setPid(int pid) {
        this.pid = pid;
    }

    public File getSelectFile() {
        return selectFile;
    }

    public void setSelectFile(File selectFile) {
        this.selectFile = selectFile;
    }

    public boolean isChosen() {
        return chosen;
    }

    public void setChosen(boolean chosen) {
        this.chosen = chosen;
    }

    public boolean isForceUpdate() {
        return forceUpdate;
    }

    public void setForceUpdate(boolean forceUpdate) {
        this.forceUpdate = forceUpdate;
    }

//    public MultiOTAManager getOtaManager() {
//        return otaManager;
//    }
//
//    public void setOtaManager(MultiOTAManager otaManager) {
//        this.otaManager = otaManager;
//    }

    public boolean isConnected() {
        return connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BroadcastBoxInfo)) return false;
        if (!super.equals(o)) return false;
        BroadcastBoxInfo that = (BroadcastBoxInfo) o;
        return BluetoothUtil.deviceEquals(getDevice(), that.getDevice());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getDevice());
    }

    @Override
    public String toString() {
        return "BroadcastBoxInfo{" +
                "uid=" + uid +
                ", pid=" + pid +
                ", type=" + type +
                ", selectFile=" + selectFile +
                ", getName=" + AppUtil.getDeviceName(MainApplication.getInstance(), getDevice()) +
                ", getAddress=" + getDevice().getAddress() +
                '}';
    }

}
