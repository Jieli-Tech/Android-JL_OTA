package com.jieli.broadcastbox.model.ota;

import com.jieli.broadcastbox.multidevice.bean.MultiOtaParam;

import java.util.List;

/**
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 多设备升级结束状态
 * @since 2022/12/12
 */
public class MultiOTAEnd extends MultiOTAState {

    private final int total;
    private final List<MultiOtaParam> successList;

    public MultiOTAEnd(int total, List<MultiOtaParam> successList) {
        super(STATE_IDLE);
        this.total = total;
        this.successList = successList;
    }

    public int getTotal() {
        return total;
    }

    public List<MultiOtaParam> getSuccessList() {
        return successList;
    }
}
