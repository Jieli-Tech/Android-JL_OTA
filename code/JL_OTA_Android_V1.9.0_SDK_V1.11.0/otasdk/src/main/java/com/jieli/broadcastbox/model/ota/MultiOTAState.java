package com.jieli.broadcastbox.model.ota;

/**
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 多设备OTA状态
 * @since 2022/12/12
 */
public abstract class MultiOTAState {
    public static final int STATE_IDLE = 0;
    public static final int STATE_START = 1;
    public static final int STATE_WORKING = 2;
    /**
     * 回连状态
     */
    public static final int STATE_OTA_RECONNECT = 3;
    /**
     * ota结束
     */
    public static final int STATE_OTA_STOP = 4;

    private final int state;

    public MultiOTAState(int state) {
        this.state = state;
    }

    public int getState() {
        return state;
    }
}
