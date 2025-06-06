package com.jieli.otasdk.tool.bluetooth

import android.bluetooth.BluetoothDevice
import android.os.Handler
import android.os.Looper
import com.jieli.otasdk.tool.ota.ble.model.BleScanInfo
import java.util.*

/**
 * @author zqjasonZhong
 * @since 2022/9/14
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 蓝牙事件回调辅助类
 */
class BTEventCbHelper : OnBTEventCallback() {
    private val callbacks = mutableListOf<OnBTEventCallback>()
    private val uiHandler = Handler(Looper.getMainLooper())

    fun registerCallback(callback: OnBTEventCallback) {
        if (callbacks.contains(callback)) return
        callbacks.add(callback)
    }

    fun unregisterCallback(callback: OnBTEventCallback) {
        if (callbacks.isEmpty()) return
        callbacks.remove(callback)
    }

    fun release() {
        callbacks.clear()
        uiHandler.removeCallbacksAndMessages(null)
    }

    override fun onAdapterChange(bEnabled: Boolean) {
        callbackEvent(object : CallbackImpl<OnBTEventCallback> {
            override fun onCallback(callback: OnBTEventCallback) {
                callback.onAdapterChange(bEnabled)
            }
        })
    }

    override fun onDiscoveryChange(bStart: Boolean, scanType: Int) {
        callbackEvent(object : CallbackImpl<OnBTEventCallback> {
            override fun onCallback(callback: OnBTEventCallback) {
                callback.onDiscoveryChange(bStart, scanType)
            }
        })
    }

    override fun onDiscovery(device: BluetoothDevice?, bleScanMessage: BleScanInfo?) {
        callbackEvent(object : CallbackImpl<OnBTEventCallback> {
            override fun onCallback(callback: OnBTEventCallback) {
                callback.onDiscovery(device, bleScanMessage)
            }
        })
    }

    override fun onDeviceConnection(device: BluetoothDevice?, way: Int, status: Int) {
        callbackEvent(object : CallbackImpl<OnBTEventCallback> {
            override fun onCallback(callback: OnBTEventCallback) {
                callback.onDeviceConnection(device, way, status)
            }
        })
    }

    override fun onReceiveData(device: BluetoothDevice?, way: Int, uuid: UUID?, data: ByteArray?) {
        callbackEvent(object : CallbackImpl<OnBTEventCallback> {
            override fun onCallback(callback: OnBTEventCallback) {
                callback.onReceiveData(device, way, uuid, data)
            }
        })
    }

    override fun onBleMtuChange(device: BluetoothDevice?, mtu: Int, status: Int) {
        callbackEvent(object : CallbackImpl<OnBTEventCallback> {
            override fun onCallback(callback: OnBTEventCallback) {
                callback.onBleMtuChange(device, mtu, status)
            }
        })
    }

    private fun callbackEvent(impl: CallbackImpl<OnBTEventCallback>?) {
        if (null == impl) return
        val runnable = CallbackRunnable(callbacks, impl)
        if (Thread.currentThread().id == Looper.getMainLooper().thread.id) {
            runnable.run()
        } else {
            uiHandler.post(runnable)
        }
    }

    interface CallbackImpl<T> {

        fun onCallback(callback: T)
    }

    class CallbackRunnable<T>(
        private val callbacks: MutableList<T>,
        private val impl: CallbackImpl<T>?
    ) :
        Runnable {

        override fun run() {
            if (callbacks.isEmpty() || impl == null) return
            val temp = mutableListOf<T>()
            temp.addAll(callbacks)
            temp.forEach {
                impl.onCallback(it)
            }
        }
    }
}