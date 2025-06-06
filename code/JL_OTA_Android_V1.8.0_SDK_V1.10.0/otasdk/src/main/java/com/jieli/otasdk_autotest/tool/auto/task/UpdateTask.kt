package com.jieli.otasdk_autotest.tool.auto.task

import android.content.Context
import com.jieli.jl_bt_ota.interfaces.IUpgradeCallback
import com.jieli.jl_bt_ota.model.base.BaseError
import com.jieli.otasdk.R
import com.jieli.otasdk_autotest.tool.auto.OnTaskListener
import com.jieli.otasdk_autotest.tool.auto.TestTask
import com.jieli.otasdk.tool.ota.OTAManager

/**
 * @author zqjasonZhong
 * @since 2022/9/14
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 更新设备任务
 */
class UpdateTask(
    private val context: Context,
    private val otaManager: OTAManager,
    private val otaFilePath: String?,
    private val callback: IUpgradeCallback
) :
    TestTask(TASK_TYPE_UPDATE) {

    override fun getName(): String {
        return "Update Device"
    }

    override fun isRun(): Boolean {
        return otaManager.isOTA
    }

    override fun startTest(listener: OnTaskListener) {
        otaManager.bluetoothOption.firmwareFilePath = otaFilePath

        otaManager.startOTA(object : IUpgradeCallback {
            override fun onStartOTA() {
                callback.onStartOTA()
                listener.onStart(otaFilePath)
            }

            override fun onNeedReconnect(addr: String?, isNewReconnectWay: Boolean) {
                callback.onNeedReconnect(addr, isNewReconnectWay)
            }

            override fun onProgress(type: Int, progress: Float) {
                callback.onProgress(type, progress)
            }

            override fun onStopOTA() {
                callback.onStopOTA()
                listener.onFinish(
                    ERR_SUCCESS,
                    context.getString(R.string.ota_complete)
                )
            }

            override fun onCancelOTA() {
                callback.onCancelOTA()
                listener.onFinish(
                    ERR_USE_CANCEL,
                    context.getString(R.string.ota_upgrade_cancel)
                )
            }

            override fun onError(error: BaseError?) {
                callback.onError(error)
                listener.onFinish(
                    ERR_FAILED, context.getString(
                        R.string.ota_upgrade_failed,
                        String.format("code = ${error?.subCode}, message = ${error?.message}")
                    )
                )
            }
        })
    }

    override fun stopTest(): Boolean {
        if (!isRun()) return false
        otaManager.cancelOTA()
        return true
    }
}