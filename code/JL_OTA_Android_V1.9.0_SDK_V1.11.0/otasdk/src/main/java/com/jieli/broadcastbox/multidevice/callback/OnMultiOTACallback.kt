package com.jieli.broadcastbox.multidevice.callback

import com.jieli.broadcastbox.multidevice.bean.MultiOtaParam

/**
 * @author zqjasonZhong
 * @since 2022/12/12
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 多设备升级回调
 */
interface OnMultiOTACallback {
    /**
     * 多设备OTA开始
     *
     * @param total 升级总数
     * @param otaWay 多设备升级方式
     */
    fun onMultiOTAStart(total: Int, otaWay: Int)

    /**
     * 多设备OTA进度
     *
     * @param address 设备标识
     * @param type 升级进度类型
     * @param progress 升级进度
     */
    fun onMultiOTAProgress(address: String, type: Int, progress: Float)

    /**
     * 多设备OTA结束
     *
     * @param address 设备标识
     * @param code 结果码
     * @param message 结果描述
     */
    fun onMultiOTAStop(address: String, code: Int, message: String)

    /**
     * 多设备OTA需要回连
     *
     * @param address 设备标识
     * @param reconnectAddr 回连标识
     * @param isUseNewAdv 是否用新的广播包回连
     */
    fun onMultiOTANeedReconnect(address: String, reconnectAddr: String?, isUseNewAdv: Boolean)

    /**
     * 多设备OTA结束
     *
     * @param total 测试总数
     * @param success 成功列表
     */
    fun onMultiOTAFinish(total: Int, success: List<MultiOtaParam>)
}