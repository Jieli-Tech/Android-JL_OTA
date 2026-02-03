package com.jieli.broadcastbox.multidevice.bean

import com.jieli.broadcastbox.multidevice.MultiOTAManager

/**
 * @author zqjasonZhong
 * @since 2022/12/12
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 多设备升级
 */
class MultiOtaValue(manager: MultiOTAManager) {
    val otaManager: MultiOTAManager = manager
    var seq: Int = 0
    var isReadOta: Boolean = false


}