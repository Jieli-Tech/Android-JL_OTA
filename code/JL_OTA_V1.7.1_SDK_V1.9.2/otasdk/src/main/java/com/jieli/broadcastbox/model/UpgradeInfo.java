package com.jieli.broadcastbox.model;

import java.util.Objects;

/**
 * Des:
 * author: Bob
 * date: 2022/11/30
 * Copyright: Jieli Technology
 * Modify date:
 * Modified by:
 */
public class UpgradeInfo {
    public static final int STATE_STOP = 0;
    public static final int STATE_WORKING = 1;
    public static final int STATE_RECONNECT = 2;

    private final String deviceAddress;  //upgrade device mac
    private String deviceName;// upgrade device name
    private String filename;// upgrade file
    private int progress = 0;// ota progress
    private int upgradeType = 0;// ota type: 0->bootloader, 1->fw upgrade
    private int state = STATE_STOP;// OTA state
    private int error;// OTA error
    private String message; //error message

    public UpgradeInfo(String deviceAddress) {
        this.deviceAddress = deviceAddress;
    }

    public String getDeviceAddress() {
        return deviceAddress;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public UpgradeInfo setDeviceName(String deviceName) {
        this.deviceName = deviceName;
        return this;
    }

    public String getFilename() {
        return filename;
    }

    public UpgradeInfo setFilename(String filename) {
        this.filename = filename;
        return this;
    }

    public int getProgress() {
        return progress;
    }

    public UpgradeInfo setProgress(int progress) {
        this.progress = progress;
        return this;
    }

    public int getUpgradeType() {
        return upgradeType;
    }

    public UpgradeInfo setUpgradeType(int upgradeType) {
        this.upgradeType = upgradeType;
        return this;
    }

    public int getError() {
        return error;
    }

    public UpgradeInfo setError(int error) {
        this.error = error;
        return this;
    }

    public String getMessage() {
        return message;
    }

    public UpgradeInfo setMessage(String message) {
        this.message = message;
        return this;
    }

    public int getState() {
        return state;
    }

    public UpgradeInfo setState(int state) {
        this.state = state;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UpgradeInfo info = (UpgradeInfo) o;
        return Objects.equals(deviceAddress, info.deviceAddress);
    }

    @Override
    public int hashCode() {
        return Objects.hash(deviceAddress);
    }
}
