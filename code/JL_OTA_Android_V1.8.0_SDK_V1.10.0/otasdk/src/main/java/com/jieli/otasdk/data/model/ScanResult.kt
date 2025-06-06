package com.jieli.otasdk.data.model

import com.jieli.otasdk.data.model.device.ScanDevice

/**
 * @author zqjasonZhong
 * @since 2022/9/9
 * @email zhongzhuocheng@zh-jieli.com
 * @desc
 */
class ScanResult(var state: Int = SCAN_STATUS_IDLE) {
    var device: ScanDevice? = null

    constructor(state: Int, device: ScanDevice?) : this(state) {
        this.device = device
    }

    companion object {
        const val SCAN_STATUS_IDLE = 0
        const val SCAN_STATUS_SCANNING = 1
        const val SCAN_STATUS_FOUND_DEV = 2
    }
}