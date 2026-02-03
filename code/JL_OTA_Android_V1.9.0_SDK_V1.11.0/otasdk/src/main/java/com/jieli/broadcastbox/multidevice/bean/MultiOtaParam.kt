package com.jieli.broadcastbox.multidevice.bean

/**
 * @author zqjasonZhong
 * @since 2022/12/12
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 多设备OTA参数
 */
class MultiOtaParam(val address: String, val uid: Int, val pid: Int) {
    val filePaths: MutableList<String> = mutableListOf()

    fun getKey(): String {
        var key = address
        if (uid != 0) {
            key += "-$uid"
        }
        if (pid != 0) {
            key += "-$pid"
        }
        return key
    }
}