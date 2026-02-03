package com.jieli.broadcastbox.model.ota;

/**
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 多设备升级开始状态
 * @since 2022/12/12
 */
public class MultiOTAStart extends MultiOTAState {
    private final int total;
    private final int otaWay;

    public MultiOTAStart(int total, int otaWay) {
        super(STATE_START);
        this.total = total;
        this.otaWay = otaWay;
    }

    public int getTotal() {
        return total;
    }

    public int getOtaWay() {
        return otaWay;
    }
}
