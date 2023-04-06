package com.jieli.otasdk.tool.bluetooth

/**
 * @author zqjasonZhong
 * @since 2022/9/14
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 回调实现类
 */
interface CallbackImpl<T> {

    fun onCallback(callback: T)
}