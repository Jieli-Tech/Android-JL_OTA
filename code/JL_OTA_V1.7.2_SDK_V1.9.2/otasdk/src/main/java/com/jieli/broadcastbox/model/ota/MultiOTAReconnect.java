package com.jieli.broadcastbox.model.ota;

/**
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 多设备OTA回连状态
 * @since 2022/12/12
 */
public class MultiOTAReconnect extends MultiOTAState {
    private final String address;
    private final String reconnectAddr;
    private final boolean isUseNewAdv;

    public MultiOTAReconnect(String address, String reconnectAddr, boolean isUseNewAdv) {
        super(STATE_OTA_RECONNECT);
        this.address = address;
        this.reconnectAddr = reconnectAddr;
        this.isUseNewAdv = isUseNewAdv;
    }

    public String getAddress() {
        return address;
    }

    public String getReconnectAddr() {
        return reconnectAddr;
    }

    public boolean isUseNewAdv() {
        return isUseNewAdv;
    }
}
