package com.jieli.otasdk_autotest.data.auto

import com.jieli.otasdk_autotest.tool.auto.TestTask

/**
 * @author zqjasonZhong
 * @since 2022/9/19
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 测试任务开始状态
 */
class TestTaskStart(val id: Int, val task: TestTask, val message: String?) :
    TestState(TEST_STATE_TASK_START) {

    override fun toString(): String {
        return "TestTaskStart(id=$id, task=$task, message=$message)"
    }
}