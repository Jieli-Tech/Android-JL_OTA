package com.jieli.otasdk_autotest.data.auto

import com.jieli.otasdk_autotest.tool.auto.TestTask

/**
 * @author zqjasonZhong
 * @since 2022/9/19
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 测试任务日志状态
 */
class TestTaskLog(val id: Int, val task: TestTask, val log: String?) :
    TestState(TEST_STATE_TASK_LOG) {

    override fun toString(): String {
        return "TestTaskLog(id=$id, task=$task, log=$log)"
    }
}