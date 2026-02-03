package com.jieli.otasdk_autotest.tool.auto

/**
 * @author zqjasonZhong
 * @since 2022/9/14
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 任务事件监听器
 */
interface OnTaskListener {

    fun onStart(message: String?)

    fun onLogcat(log: String?)

    fun onFinish(code: Int, message: String?)

}