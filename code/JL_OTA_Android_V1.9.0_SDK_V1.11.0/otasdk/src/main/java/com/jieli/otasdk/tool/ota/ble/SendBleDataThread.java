package com.jieli.otasdk.tool.ota.ble;

import android.bluetooth.BluetoothGatt;

import com.jieli.jl_bt_ota.util.JL_Log;
import com.jieli.otasdk.tool.ota.ble.interfaces.IBleOp;
import com.jieli.otasdk.tool.ota.ble.interfaces.OnThreadStateListener;
import com.jieli.otasdk.tool.ota.ble.interfaces.OnWriteDataCallback;
import com.jieli.otasdk.tool.ota.ble.model.BleSendTask;

import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 发送BLE数据线程
 *
 * @author zqjasonZhong
 * @date 2019/10/8
 */
public class SendBleDataThread extends Thread {
    private final static String TAG = "SendBleDataThread";
    private final LinkedBlockingQueue<BleSendTask> mQueue = new LinkedBlockingQueue<>();
    private volatile boolean isDataSend = false;
    private volatile boolean isThreadWaiting = false;
    private volatile boolean isWaitingForCallback = false;
    private volatile int retryNum = 0;
    private final IBleOp mBleManager;
    private final OnThreadStateListener mListener;

    private BleSendTask mCurrentTask;

    public SendBleDataThread(IBleOp manager, OnThreadStateListener listener) {
        super(TAG);
        mBleManager = manager;
        mListener = listener;
    }

    public boolean isRunning() {
        return isDataSend;
    }

    public boolean addSendTask(BluetoothGatt gatt, UUID serviceUUID, UUID characteristicUUID, byte[] data, OnWriteDataCallback callback) {
        if (null == mBleManager || null == gatt || null == serviceUUID || null == characteristicUUID || null == data || data.length == 0) {
            return false;
        }
        int mtu = mBleManager.getBleMtu();
        JL_Log.d(TAG, "addSendTask : " + mtu);
        int dataLen = data.length;
        int blockCount = dataLen / mtu;
        boolean ret = false;
        for (int i = 0; i < blockCount; i++) {
            byte[] mBlockData = new byte[mtu];
            System.arraycopy(data, i * mtu, mBlockData, 0, mBlockData.length);
            ret = addSendData(gatt, serviceUUID, characteristicUUID, mBlockData, callback);
        }

        if (0 != dataLen % mtu) {
            byte[] noBlockData = new byte[dataLen % mtu];
            System.arraycopy(data, dataLen - dataLen % mtu, noBlockData, 0, noBlockData.length);
            ret = addSendData(gatt, serviceUUID, characteristicUUID, noBlockData, callback);
        }
        return ret;
    }

    public void wakeupSendThread(BleSendTask sendTask) {
        if (null == sendTask || mCurrentTask != null && mCurrentTask.equals(sendTask)) {
            if (sendTask != null) {
                sendTask.setCallback(mCurrentTask.getCallback());
                mCurrentTask = sendTask;
            }
            synchronized (mQueue) {
                if (isThreadWaiting) {
                    if (isWaitingForCallback) {
                        mQueue.notifyAll();
                    } else {
                        mQueue.notify();
                    }
                } else if (isWaitingForCallback) {
                    mQueue.notify();
                }
            }
        }
    }

    private boolean addSendData(BluetoothGatt gatt, UUID serviceUUID, UUID characteristicUUID, byte[] data, OnWriteDataCallback callback) {
        boolean ret = false;
        if (isDataSend) {
            BleSendTask sendTask = new BleSendTask(gatt, serviceUUID, characteristicUUID, data, callback);
            try {
                mQueue.put(sendTask);
                ret = true;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (ret && isThreadWaiting && !isWaitingForCallback) {
                isThreadWaiting = false;
                synchronized (mQueue) {
                    mQueue.notify();
                }
            }
        }
        return ret;
    }

    @Override
    public synchronized void start() {
        isDataSend = true;
        super.start();
    }

    public synchronized void stopThread() {
        isDataSend = false;
        wakeupSendThread(null);
    }

    private void callbackResult(BleSendTask task, boolean result) {
        if (task != null && task.getCallback() != null) {
            if (task.getBleGatt() == null) return;
            task.getCallback().onBleResult(task.getBleGatt().getDevice(), task.getServiceUUID(),
                    task.getCharacteristicUUID(), result, task.getData());
        } else {
            JL_Log.i(TAG, "getCallback is null.");
        }
    }

    @Override
    public void run() {
        JL_Log.d(TAG, "send ble data thread is started.");
        if (mListener != null) {
            mListener.onStart(getId(), getName());
        }
        if (mBleManager != null) {
            synchronized (mQueue) {
                while (isDataSend) {
                    mCurrentTask = null;
                    isThreadWaiting = false;
                    isWaitingForCallback = false;
                    if (mQueue.isEmpty()) {
                        isThreadWaiting = true;
                        JL_Log.d(TAG, "queue is empty, so waiting for data");
                        try {
                            mQueue.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    } else {
                        mCurrentTask = mQueue.peek();
                        if (mCurrentTask != null) {
                            isWaitingForCallback = mBleManager.writeDataByBle(mCurrentTask.getBleGatt(), mCurrentTask.getServiceUUID(),
                                    mCurrentTask.getCharacteristicUUID(), mCurrentTask.getData());
                            if (isWaitingForCallback) {
                                try {
                                    mQueue.wait(BleManager.SEND_DATA_MAX_TIMEOUT);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            } else {
                                mCurrentTask.setStatus(-1);
                            }
                            JL_Log.d(TAG, "data send ret :" + mCurrentTask.getStatus());
                            if (mCurrentTask.getStatus() != BluetoothGatt.GATT_SUCCESS) { //发送失败
                                retryNum++;
                                if (retryNum >= 3) { //重发次数超过限制
                                    callbackResult(mCurrentTask, false);
                                    mQueue.clear();
                                } else {
                                    if (mCurrentTask.getStatus() != -1) {
                                        mCurrentTask.setStatus(-1);
                                        try {
                                            sleep(10);
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                    continue;
                                }
                            } else { //发送成功
                                callbackResult(mCurrentTask, true);
                            }
                        }
                        retryNum = 0;
                        if (!mQueue.isEmpty()) mQueue.poll();
                    }
                }
            }

            isWaitingForCallback = false;
            isThreadWaiting = false;
            mQueue.clear();
            if (mListener != null) {
                mListener.onEnd(getId(), getName());
            }
            JL_Log.d(TAG, "send ble data thread exit.");
        }
    }
}
