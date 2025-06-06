package com.jieli.otasdk_autotest.data.auto

/**
 * @author zqjasonZhong
 * @since 2022/9/19
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 测试结束状态
 */
class TestFinish(val success: Int, val code: Int, val message: String?) :
    TestState(TEST_STATE_IDLE) {

    override fun toString(): String {
        return "TestFinish(success=$success, code=$code, message=$message)"
    }

}