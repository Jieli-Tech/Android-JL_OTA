package com.jieli.otasdk.tool.ota.spp;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.jieli.jl_bt_ota.util.CHexConver;
import com.jieli.jl_bt_ota.util.JL_Log;
import com.jieli.otasdk.tool.ota.spp.interfaces.OnWriteSppDataCallback;
import com.jieli.otasdk.util.AppUtil;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 发送SPP数据线程
 *
 * @author zqjasonZhong
 * @since 2021/1/13
 */
public class SendSppDataThread extends Thread {
    private final static String TAG = "SendSppDataThread";
    private final LinkedBlockingQueue<SppSendTask> mQueue = new LinkedBlockingQueue<>();
    private volatile boolean isDataSend = false;
    private volatile boolean isQueueEmpty = false;
    private final Context mContext;
    private final ISppOp mSppOp;
    private final OnSendDataListener mListener;
    private final Handler mHandler = new Handler(Looper.getMainLooper());


    public SendSppDataThread(Context context, ISppOp op, OnSendDataListener listener) {
        mContext = context;
        mSppOp = op;
        mListener = listener;
    }

    @Override
    public synchronized void start() {
        isDataSend = true;
        super.start();
    }

    public synchronized void stopThread() {
        isDataSend = false;
        wakeupThread();
    }

    public void addSendTask(SppSendTask task) {
        if (!isDataSend) return;
        boolean ret = false;
        try {
            mQueue.put(task);
            ret = true;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (ret && isQueueEmpty) {
            isQueueEmpty = false;
            synchronized (mQueue) {
                mQueue.notifyAll();
            }
        }
    }

    private void wakeupThread() {
        synchronized (mQueue) {
            if (isQueueEmpty) {
                mQueue.notifyAll();
            }
        }
    }

    @Override
    public void run() {
        final long threadId = getId();
        if (mListener != null) {
            mHandler.post(() -> mListener.onThreadStart(threadId));
        }
        if (mSppOp == null || !AppUtil.checkHasConnectPermission(mContext)) {
            if (mListener != null) {
                mHandler.post(() -> mListener.onThreadStop(threadId));
            }
            return;
        }
        synchronized (mQueue) {
            while (isDataSend) {
                isQueueEmpty = mQueue.isEmpty();
                if (isQueueEmpty) {
                    JL_Log.d(TAG, "queue is empty, so waiting for data");
                    try {
                        mQueue.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } else {
                    final SppSendTask currentTask = mQueue.poll();
                    if (currentTask != null) {
                        final boolean ret;
                        try {
                            ret = mSppOp.writeDataToSppDevice(currentTask.device, currentTask.sppUUID, currentTask.data);
                        } catch (IOException e) { //发数异常，连接断开
                            e.printStackTrace();
                            isDataSend = false;
                            break;
                        }
                        mHandler.post(() -> {
                            if (currentTask.callback != null) {
                                currentTask.callback.onSppResult(currentTask.device, currentTask.sppUUID, ret, currentTask.data);
                            }
                        });
                    }
                }
            }
        }
        mQueue.clear();
        if (mListener != null) {
            mHandler.post(() -> mListener.onThreadStop(threadId));
        }
    }

    public interface ISppOp {

        boolean writeDataToSppDevice(BluetoothDevice device, UUID sppUUID, byte[] data) throws IOException;
    }

    public interface OnSendDataListener {

        void onThreadStart(long threadID);

        void onThreadStop(long threadID);
    }

    public static class SppSendTask {
        public BluetoothDevice device;
        public UUID sppUUID;
        public byte[] data;
        public OnWriteSppDataCallback callback;

        public SppSendTask(BluetoothDevice device, UUID sppUUID, byte[] data, OnWriteSppDataCallback callback) {
            this.device = device;
            this.sppUUID = sppUUID;
            this.data = data;
            this.callback = callback;
        }

        @Override
        public String toString() {
            return "SppSendTask{" +
                    "device=" + device +
                    ", sppUUID=" + sppUUID +
                    ", data=" + CHexConver.byte2HexStr(data) +
                    ", callback=" + callback +
                    '}';
        }
    }
}
