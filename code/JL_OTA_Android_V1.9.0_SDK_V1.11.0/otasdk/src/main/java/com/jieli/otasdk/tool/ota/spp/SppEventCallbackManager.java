package com.jieli.otasdk.tool.ota.spp;

import android.bluetooth.BluetoothDevice;
import android.os.Handler;
import android.os.Looper;

import com.jieli.otasdk.tool.ota.spp.interfaces.ISppEventCallback;
import com.jieli.otasdk.tool.ota.spp.interfaces.SppEventCallback;

import java.util.ArrayList;
import java.util.UUID;

/**
 * Spp事件回调管理类
 *
 * @author zqjasonZhong
 * @since 2021/1/13
 */
public class SppEventCallbackManager implements ISppEventCallback {
    private final ArrayList<SppEventCallback> mCallbacks = new ArrayList<>();
    private final Handler mHandler = new Handler(Looper.getMainLooper());

    public void registerSppEventCallback(SppEventCallback callback) {
        if (callback != null && !mCallbacks.contains(callback)) {
            mCallbacks.add(callback);
        }
    }

    public void unregisterSppEventCallback(SppEventCallback callback) {
        if (callback != null && !mCallbacks.isEmpty()) {
            mCallbacks.remove(callback);
        }
    }

    public void release() {
        mCallbacks.clear();
        mHandler.removeCallbacksAndMessages(null);
    }

    @Override
    public void onAdapterChange(boolean bEnabled) {
        callbackSppEvent(new SppEventCallbackImpl() {
            @Override
            public void onCallback(SppEventCallback callback) {
                callback.onAdapterChange(bEnabled);
            }
        });
    }

    @Override
    public void onDiscoveryDeviceChange(final boolean bStart) {
        callbackSppEvent(new SppEventCallbackImpl() {
            @Override
            public void onCallback(SppEventCallback callback) {
                callback.onDiscoveryDeviceChange(bStart);
            }
        });
    }

    @Override
    public void onDiscoveryDevice(final BluetoothDevice device, final int rssi) {
        callbackSppEvent(new SppEventCallbackImpl() {
            @Override
            public void onCallback(SppEventCallback callback) {
                callback.onDiscoveryDevice(device, rssi);
            }
        });
    }

    @Override
    public void onSppConnection(final BluetoothDevice device, final UUID sppUUID, final int status) {
        callbackSppEvent(new SppEventCallbackImpl() {
            @Override
            public void onCallback(SppEventCallback callback) {
                callback.onSppConnection(device, sppUUID, status);
            }
        });
    }

    @Override
    public void onReceiveSppData(final BluetoothDevice device, final UUID uuid, final byte[] data) {
        callbackSppEvent(new SppEventCallbackImpl() {
            @Override
            public void onCallback(SppEventCallback callback) {
                callback.onReceiveSppData(device, uuid, data);
            }
        });
    }

    private void callbackSppEvent(SppEventCallbackImpl impl) {
        if (null == impl) return;
        OnSppEventRunnable runnable = new OnSppEventRunnable(impl);
        if (Thread.currentThread().getId() == Looper.getMainLooper().getThread().getId()) {
            runnable.run();
        } else {
            mHandler.post(runnable);
        }
    }

    private static abstract class SppEventCallbackImpl {

        public abstract void onCallback(SppEventCallback callback);
    }

    private class OnSppEventRunnable implements Runnable {
        private final SppEventCallbackImpl mImpl;

        public OnSppEventRunnable(SppEventCallbackImpl impl) {
            mImpl = impl;
        }

        @Override
        public void run() {
            if (!mCallbacks.isEmpty() && mImpl != null) {
                for (SppEventCallback callback : new ArrayList<>(mCallbacks)) {
                    if (callback != null) {
                        mImpl.onCallback(callback);
                    }
                }
            }
        }
    }
}
