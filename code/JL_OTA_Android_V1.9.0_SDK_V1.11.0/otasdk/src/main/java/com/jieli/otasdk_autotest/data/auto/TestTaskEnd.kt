package com.jieli.otasdk_autotest.data.auto

import com.jieli.otasdk_autotest.tool.auto.TestTask

/**
 * @author zqjasonZhong
 * @since 2022/9/19
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 测试任务结束状态
 */
class TestTaskEnd(val id: Int, val task: TestTask, val code: Int, val message: String?): TestState(
    TEST_STATE_TASK_END
) {

    override fun toString(): String {
        return "TestTaskEnd(id=$id, task=$task, code=$code, message=$message)"
    }
}