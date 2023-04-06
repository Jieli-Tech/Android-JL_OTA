package com.jieli.broadcastbox.viewmodel

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.jieli.broadcastbox.model.BroadcastBoxInfo
import com.jieli.broadcastbox.model.ota.*
import com.jieli.broadcastbox.multidevice.MultiOTAProcessor
import com.jieli.broadcastbox.multidevice.bean.MultiOtaConfig
import com.jieli.broadcastbox.multidevice.bean.MultiOtaParam
import com.jieli.broadcastbox.multidevice.callback.OnMultiOTACallback
import com.jieli.jl_bt_ota.constant.ErrorCode
import com.jieli.jl_bt_ota.util.JL_Log

/**
 * @author zqjasonZhong
 * @since 2022/12/12
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 多设备升级逻辑实现
 */
class MultiOTAViewModel : ViewModel() {
    private val tag = javaClass.simpleName
    private val processor: MultiOTAProcessor = MultiOTAProcessor.getInstance()

    lateinit var multiOtaStateMLD: MutableLiveData<MultiOTAState>

    fun release() {
        processor.setCallback(null)
//        processor.destroy()
    }

    fun startMultiOTA(context: Context, infos: MutableList<BroadcastBoxInfo>) {
        val params: MutableList<MultiOtaParam> = mutableListOf()
        infos.forEach {
            val param = MultiOtaParam(it.device.address, it.uid, it.pid)
            param.filePaths.add(it.selectFile.path)
            params.add(param)
        }
        val config = MultiOtaConfig(params, MultiOTAProcessor.MULTI_OTA_WAY_CONCURRENCY, 0)
        val ret = processor.startMultiOTA(context, config)
        JL_Log.i(tag, "startMultiOTA : $ret, ota config = $config")
        if (ret != ErrorCode.ERR_NONE) {
            multiOtaStateMLD.value = MultiOTAEnd(params.size, ArrayList())
        }
    }


    private val otaCallback: OnMultiOTACallback = object : OnMultiOTACallback {

        override fun onMultiOTAStart(total: Int, otaWay: Int) {
            multiOtaStateMLD.value = MultiOTAStart(total, otaWay)
        }

        override fun onMultiOTAProgress(address: String, type: Int, progress: Float) {
            multiOtaStateMLD.value = MultiOTAWorking(address, type, progress)
        }

        override fun onMultiOTAStop(address: String, code: Int, message: String) {
            multiOtaStateMLD.value = MultiOTAStop(address, code, message)
        }

        override fun onMultiOTANeedReconnect(
            address: String,
            reconnectAddr: String?,
            isUseNewAdv: Boolean
        ) {
            multiOtaStateMLD.value = MultiOTAReconnect(address, reconnectAddr, isUseNewAdv)
        }

        override fun onMultiOTAFinish(total: Int, success: List<MultiOtaParam>) {
            multiOtaStateMLD.value = MultiOTAEnd(total, success)
        }
    }

    init {
        processor.setCallback(otaCallback)
        multiOtaStateMLD = MutableLiveData<MultiOTAState>()
    }
}