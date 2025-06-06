package com.jieli.otasdk_autotest.data.auto

/**
 * @author zqjasonZhong
 * @since 2022/9/19
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 自动测试状态
 */
open class TestState(val state: Int) {

    companion object {
        const val TEST_STATE_IDLE = 0
        const val TEST_STATE_WORKING = 1
        const val TEST_STATE_TASK_START = 2
        const val TEST_STATE_TASK_LOG = 3
        const val TEST_STATE_TASK_END = 4
    }
}