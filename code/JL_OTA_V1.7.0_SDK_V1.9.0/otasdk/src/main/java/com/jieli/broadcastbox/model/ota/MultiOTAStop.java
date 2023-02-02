package com.jieli.broadcastbox.model.ota;

/**
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 多设备OTA停止回调
 * @since 2022/12/12
 */
public class MultiOTAStop extends MultiOTAState {
    private final String address;
    private final int code;
    private final String message;

    public MultiOTAStop(String address, int code, String message) {
        super(STATE_OTA_STOP);
        this.address = address;
        this.code = code;
        this.message = message;
    }

    public String getAddress() {
        return address;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
