package com.jieli.otasdk_autotest.tool.auto

import com.jieli.otasdk_autotest.data.auto.TestParam

/**
 * @author zqjasonZhong
 * @since 2022/9/19
 * @email zhongzhuocheng@zh-jieli.com
 * @desc  任务队列回调
 */
interface TaskQueueCallback {

    /**
     * 回调任务队列开始
     *
     * @param param 测试开始参数
     */
    fun onStart(param: TestParam)

    /**
     * 回调任务开始
     *
     * @param id 任务ID
     * @param task 任务对象
     * @param message 信息
     */
    fun onTaskStart(id: Int, task: TestTask, message: String?)

    /**
     * 回调任务日志
     *
     * @param id 任务ID
     * @param task 任务对象
     * @param log 打印日志
     */
    fun onTaskLogcat(id: Int, task: TestTask, log: String?)

    /**
     * 回调任务结束
     *
     * @param id 任务ID
     * @param task 任务对象
     * @param code 结果码
     * @param message 描述信息
     */
    fun onTaskStop(id: Int, task: TestTask, code: Int, message: String?)

    /**
     * 回调任务队列结束
     *
     * @param success 成功次数
     * @param code 结果码
     * @param message 描述信息
     */
    fun onFinish(success: Int, code: Int, message: String?)
}