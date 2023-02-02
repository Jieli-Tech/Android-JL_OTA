package com.jieli.otasdk.tool.bluetooth

/**
 * @author zqjasonZhong
 * @since 2022/9/14
 * @email zhongzhuocheng@zh-jieli.com
 * @desc  回调处理类
 */
class CallbackRunnable<T> constructor(
    private val callbacks: MutableList<T>,
    private val impl: CallbackImpl<T>?
) :
    Runnable {

    override fun run() {
        if (callbacks.isEmpty() || impl == null) return
        val temp = mutableListOf<T>()
        temp.addAll(callbacks)
        temp.forEach {
            impl.onCallback(it)
        }
    }
}