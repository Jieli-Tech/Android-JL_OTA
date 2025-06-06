package com.jieli.otasdk_autotest.tool.auto

import android.os.Handler
import android.os.Looper
import com.jieli.jl_bt_ota.util.JL_Log
import com.jieli.otasdk_autotest.data.auto.TestParam
import java.util.*

/**
 * @author zqjasonZhong
 * @since 2022/9/14
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 任务管理器
 */
class TaskManager private constructor() {

    @Volatile
    private var handleThread: HandleThread? = null

    @Volatile
    private var callback: TaskQueueCallback? = null

    /**总的容错次数*/
    @Volatile
    private var faultTolerant: Int = 0

    companion object {
        const val TAG = "TaskManager"

        @Volatile
        private var instance: TaskManager? = null

        fun getInstance() = instance ?: synchronized(this) {
            instance ?: TaskManager().also { instance = it }
        }
    }

    fun isTestRun(): Boolean = if (null == handleThread) false else handleThread?.isAlive!!

    fun destroy() {
        stopTest()
        instance = null
    }

    fun getTestParam(): TestParam? = if (null == handleThread) null else handleThread!!.testParam

    fun startTest(taskList: MutableList<TestTask>, callback: TaskQueueCallback?): Boolean {
        if (isTestRun() || taskList.isEmpty()) return false
        setTaskQueueCallback(callback)
        handleThread = HandleThread(taskList, object : TaskQueueCallback {
            override fun onStart(param: TestParam) {
                callback?.onStart(param)
            }

            override fun onTaskStart(id: Int, task: TestTask, message: String?) {
                callback?.onTaskStart(id, task, message)
            }

            override fun onTaskLogcat(id: Int, task: TestTask, log: String?) {
                callback?.onTaskLogcat(id, task, log)
            }

            override fun onTaskStop(id: Int, task: TestTask, code: Int, message: String?) {
                callback?.onTaskStop(id, task, code, message)
            }

            override fun onFinish(success: Int, code: Int, message: String?) {
                callback?.onFinish(success, code, message)
                handleThread = null
                setTaskQueueCallback(null)
            }

        })
        handleThread?.setFaultTolerant(faultTolerant)
        handleThread?.start()
        return true
    }

    /**
     * 设置容错次数
     */
    fun setFaultTolerantCount(faultTolerant: Int): Unit {
        this@TaskManager.faultTolerant = faultTolerant
    }

    fun stopTest() {
        if (!isTestRun()) return
        handleThread?.stopHandleThread()
        handleThread = null
        setTaskQueueCallback(null)
    }

    private fun setTaskQueueCallback(callback: TaskQueueCallback?) {
        this.callback = callback
    }

    class HandleThread(
        private val taskList: MutableList<TestTask>,
        private var callback: TaskQueueCallback?
    ) : Thread("HandleThread") {
        private val uiHandler = Handler(Looper.getMainLooper())
        private val lock = Object()      //同步锁

        @Volatile
        private var isTaskLock = false   //任务是否阻塞

        @Volatile
        private var taskResult = -1      //任务结果

        lateinit var testParam: TestParam  //测试参数

        /**当前失败次数*/
        @Volatile
        private var currentErrorCount: Int = 0

        /**总的容错次数*/
        @Volatile
        private var faultTolerant: Int = 0

        override fun run() {
            super.run()
            JL_Log.d(TAG, "Handle thread is start.")
            cbStart()
            while (taskList.isNotEmpty()) {
                val testTask = taskList.removeAt(0)
                taskResult = -1  //重置任务结果
                testTask.startTest(object : OnTaskListener {
                    override fun onStart(message: String?) {
                        JL_Log.d(TAG, "Task[${testTask.getName()}] start.\n $message")
                        cbTaskStart(testParam.taskId, testTask, message)
                    }

                    override fun onFinish(code: Int, message: String?) {
                        JL_Log.i(
                            TAG, "Task[${testTask.getName()}] finish.\n" +
                                    "code = $code, message = $message"
                        )
                        taskResult = code
                        cbTaskStop(testParam.taskId, testTask, code, message)
                        unLock()
                    }

                    override fun onLogcat(log: String?) {
                        cbTaskLogcat(testParam.taskId, testTask, log)
                    }
                })
                if (testTask.type == TestTask.TASK_TYPE_UPDATE) {
                    testParam.upgradeCount++
                }
                JL_Log.d(TAG, "start Task: ${testTask.getName()}, result = $taskResult")
                if (taskResult == -1) { //结果没改变，等待任务执行
                    lock()
                }
                handleTaskResult(testTask, taskResult)
            }
            isTaskLock = false
            taskList.clear()
            if (testParam.errorCode == TestTask.ERR_SUCCESS) {
                testParam.finishTime = Calendar.getInstance().timeInMillis
            }
            cbFinish(testParam.errorCode)
            JL_Log.i(TAG, "Handle thread is died.")
        }

        fun setFaultTolerant(faultTolerant: Int): Unit {
            this@HandleThread.faultTolerant = faultTolerant
        }

        @Synchronized
        fun stopHandleThread() {
            JL_Log.i(TAG, "stopHandleThread")
            taskList.clear()
            testParam.errorCode = TestTask.ERR_USE_CANCEL
            testParam.finishTime = Calendar.getInstance().timeInMillis
            unLock()
        }

        private fun handleTaskResult(task: TestTask, result: Int) {
            JL_Log.w(TAG, "handleTaskResult :: result = $result")
            var isFinishTask = false
            if (result != 0) {//结果码不等于成功
                isFinishTask = if (task.type == TestTask.TASK_TYPE_CONNECT) {//连接失败一次就结束
                    true
                } else {
                    currentErrorCount++;
                    currentErrorCount > faultTolerant
                }
            }
            if (isFinishTask) {
                taskList.clear()
                testParam.errorCode = TestTask.ERR_FAILED
                testParam.finishTime = Calendar.getInstance().timeInMillis
            } else {
                if (task.type == TestTask.TASK_TYPE_UPDATE) {
                    if (result == 0) {
                        testParam.success++
                    }
                    sleep(5000)
                }
            }
        }

        private fun cbStart() {
            testParam = TestParam(taskList.size, Calendar.getInstance().timeInMillis)
            uiHandler.post {
                callback?.onStart(testParam)
            }
        }

        private fun cbTaskStart(id: Int, task: TestTask, message: String?) {
            uiHandler.post {
                callback?.onTaskStart(id, task, message)
            }
        }

        private fun cbTaskLogcat(id: Int, task: TestTask, log: String?) {
            uiHandler.post {
                callback?.onTaskLogcat(id, task, log)
            }
        }

        private fun cbTaskStop(id: Int, task: TestTask, code: Int, message: String?) {
            uiHandler.post {
                callback?.onTaskStop(id, task, code, message)
            }
        }

        private fun cbFinish(code: Int) {
            var message = ""
            when (code) {
                TestTask.ERR_SUCCESS -> message = "Task queue is complete."
                TestTask.ERR_FAILED -> message = "Task queue was aborted due to an error."
                TestTask.ERR_USE_CANCEL -> message = "User stop execution."
            }
            uiHandler.post {
                callback?.onFinish(testParam.success, code, message)
            }
        }

        /*加锁*/
        private fun lock() {
            synchronized(lock) {
                if (isTaskLock) {
                    JL_Log.i(TAG, "lock >>>> it is locked.")
                    return
                }
                isTaskLock = true
                try {
                    JL_Log.d(TAG, "lock >>>> ready to lock")
                    lock.wait()
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
                JL_Log.d(TAG, "lock >>>> release lock")
                isTaskLock = false
            }
        }

        /*解锁*/
        private fun unLock() {
            synchronized(lock) {
                if (!isTaskLock) {
                    JL_Log.i(TAG, "unLock >>>> It is unLock.")
                    return
                }
                JL_Log.d(TAG, "unLock >>>> notifyAll")
                lock.notifyAll()
            }
        }
    }
}