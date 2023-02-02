package com.jieli.otasdk.viewmodel

import com.jieli.otasdk.util.OtaFileObserverHelper

/**
 * @author zqjasonZhong
 * @since 2022/9/8
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 主界面逻辑处理
 */
class MainViewModel : BluetoothViewModel() {

    fun destroy() {
        OtaFileObserverHelper.getInstance().destroy()
        bluetoothHelper.destroy()
    }

}