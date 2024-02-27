package com.jieli.broadcastbox.model.ota;

/**
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 多设备OTA工作状态
 * @since 2022/12/12
 */
public class MultiOTAWorking extends MultiOTAState {
    private final String address;
    private final int otaType;
    private final float otaProgress;

    public MultiOTAWorking(String address, int otaType, float otaProgress) {
        super(STATE_WORKING);
        this.address = address;
        this.otaType = otaType;
        this.otaProgress = otaProgress;
    }

    public String getAddress() {
        return address;
    }

    public int getOtaType() {
        return otaType;
    }

    public float getOtaProgress() {
        return otaProgress;
    }
}
