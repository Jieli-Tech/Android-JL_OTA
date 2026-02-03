package com.jieli.otasdk_autotest.data.auto

/**
 * @author zqjasonZhong
 * @since 2022/9/19
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 测试工作状态
 */
class TestWorking(val testParam: TestParam) :
    TestState(TEST_STATE_WORKING) {

    override fun toString(): String {
        return "TestWorking(total=$testParam)"
    }
}