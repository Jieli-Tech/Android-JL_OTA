package com.jieli.otasdk_autotest.tool.auto

import androidx.annotation.NonNull

/**
 * @author zqjasonZhong
 * @since 2022/9/14
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 测试任务
 */
abstract class TestTask(val type: Int) {

    companion object {

        const val TASK_TYPE_CONNECT = 1
        const val TASK_TYPE_UPDATE = 2

        //Error Code
        const val ERR_SUCCESS = 0
        const val ERR_FAILED = 1
        const val ERR_INVALID_PARAM = 2
        const val ERR_TASK_IN_PROGRESS = 3
        const val ERR_USE_CANCEL = 4
    }

    protected val tag: String = javaClass.simpleName

    abstract fun getName(): String

    abstract fun isRun(): Boolean

    abstract fun startTest(@NonNull listener: OnTaskListener)

    abstract fun stopTest(): Boolean

    override fun equals(other: Any?): Boolean {
        if (other !is TestTask) return false
        return type == other.type
    }

    override fun hashCode(): Int {
        return type.hashCode()
    }

    override fun toString(): String {
        return "TestTask(type=$type), name = ${getName()}, isRun = ${isRun()}"
    }


}