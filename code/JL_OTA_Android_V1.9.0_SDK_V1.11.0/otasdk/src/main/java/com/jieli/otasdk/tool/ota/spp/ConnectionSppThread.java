package com.jieli.otasdk.tool.ota.spp;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import com.jieli.jl_bt_ota.util.BluetoothUtil;
import com.jieli.jl_bt_ota.util.JL_Log;
import com.jieli.otasdk.util.AppUtil;

import java.util.UUID;

/**
 * 连接SPP线程
 *
 * @author zqjasonZhong
 * @since 2021/1/13
 */
public class ConnectionSppThread extends Thread {
    private final static String TAG = "ConnectionSppThread";
    private final Context mContext;
    private final BluetoothDevice mDevice;
    private final UUID mSppUUID;
    private final OnConnectSppListener mListener;

    private final Handler mHandler = new Handler(Looper.getMainLooper());

    public ConnectionSppThread(Context context, BluetoothDevice device, UUID sppUUID, OnConnectSppListener listener) {
        super("ConnectionSppThread");
        mContext = context;
        mDevice = device;
        mSppUUID = sppUUID;
        mListener = listener;
    }

    @Override
    public synchronized void run() {
        onThreadStart(getId());
        if (mDevice == null || !AppUtil.checkHasConnectPermission(mContext)) {
            onThreadStop(getId(), false, null, null, null);
            return;
        }
        boolean ret = false;
        BluetoothSocket socket = null;
        JL_Log.e(TAG, "-ConnectionSppThread- connect device : " + BluetoothUtil.printBtDeviceInfo(mContext, mDevice) + ", uuid = " + mSppUUID);
        try {
            socket = mDevice.createRfcommSocketToServiceRecord(mSppUUID);
            socket.connect();
            ret = true;
        } catch (Exception e) {
            e.printStackTrace();
            JL_Log.e(TAG, "-ConnectionSppThread- exception : " + e.getMessage() + ", uuid = " + mSppUUID);
        }
        if (ret) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                JL_Log.i(TAG, "-ConnectionSppThread- connect spp ok, recv max = " + socket.getMaxReceivePacketSize() + ", send max = " + socket.getMaxTransmitPacketSize());
            }
        }
        onThreadStop(getId(), ret, mDevice, mSppUUID, socket);
    }

    public void onThreadStart(final long id) {
        if (mListener != null) {
            mHandler.post(() -> mListener.onThreadStart(id));
        }
    }

    public void onThreadStop(final long threadID, final boolean result, final BluetoothDevice device, final UUID sppUUID, final BluetoothSocket socket) {
        if (mListener != null) {
            mHandler.post(() -> mListener.onThreadStop(threadID, result, device, sppUUID, socket));
        }
    }

    public interface OnConnectSppListener {

        void onThreadStart(long threadID);

        void onThreadStop(long threadID, boolean result, BluetoothDevice device, UUID sppUUID, BluetoothSocket socket);
    }
}
