package com.jieli.broadcastbox.multidevice.bean

/**
 * @author zqjasonZhong
 * @since 2022/12/12
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 多设备升级配置
 */
class MultiOtaConfig(val params: List<MultiOtaParam>, val otaWay: Int, val testCount: Int) {


    override fun toString(): String {
        return "MultiOtaConfig(params=$params, otaWay=$otaWay, testCount=$testCount)"
    }
}