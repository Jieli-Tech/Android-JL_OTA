package com.jieli.otasdk_autotest.data.auto

import com.jieli.otasdk_autotest.tool.auto.TestTask

/**
 * @author zqjasonZhong
 * @since 2022/9/19
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 测试开始参数
 */
class TestParam(val total: Int, val startTime: Long) {

    var taskId: Int = 0
    /** 升级次数*/
    var upgradeCount:Int = 0;
    /** 升级成功次数*/
    var success: Int = 0
    var errorCode: Int = TestTask.ERR_SUCCESS
    var finishTime: Long = 0

    override fun toString(): String {
        return "TestParam(total=$total, startTime=$startTime, success=$success, finishTime=$finishTime)"
    }


}