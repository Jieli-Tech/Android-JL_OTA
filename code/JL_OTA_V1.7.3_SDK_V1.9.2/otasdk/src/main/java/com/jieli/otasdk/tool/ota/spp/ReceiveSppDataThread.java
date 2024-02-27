package com.jieli.otasdk.tool.ota.spp;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;

import com.jieli.jl_bt_ota.util.JL_Log;
import com.jieli.otasdk.util.AppUtil;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

/**
 * 接受Spp数据线程
 *
 * @author zqjasonZhong
 * @since 2020/8/20
 */
public class ReceiveSppDataThread extends Thread {
    private final static String TAG = ReceiveSppDataThread.class.getSimpleName();
    private final Context mContext;
    private final BluetoothDevice mConnectedSppDev;
    private final BluetoothSocket mBluetoothSocket;
    private final int mBlockSize;
    private final OnRecvSppDataListener mOnRecvSppDataListener;
    private final UUID mSppUUID;
    private volatile boolean isRunning;

    public final static int EXIT_REASON_SUCCESS = 0;
    public final static int EXIT_REASON_PARAM_ERROR = 1;
    public final static int EXIT_REASON_IO_EXCEPTION = 2;

    public ReceiveSppDataThread(Context context, BluetoothDevice device, UUID sppUUID, BluetoothSocket socket, OnRecvSppDataListener listener) {
        this(context, device, sppUUID, socket, 4096, listener);
    }

    public ReceiveSppDataThread(Context context, BluetoothDevice device, UUID sppUUID, BluetoothSocket socket, int blockSize, OnRecvSppDataListener listener) {
        super("ReceiveSppDataThread : " + device);
        mContext = context;
        mConnectedSppDev = device;
        mSppUUID = sppUUID;
        mBluetoothSocket = socket;
        mBlockSize = blockSize;
        mOnRecvSppDataListener = listener;
    }

    /**
     * 获取已连接的SPP通道
     *
     * @return 已连接的SPP通道
     */
    public BluetoothSocket getBluetoothSocket() {
        return mBluetoothSocket;
    }

    /**
     * 获取SPP的UUID通道
     *
     * @return UUID通道
     */
    public UUID getSppUUID() {
        return mSppUUID;
    }

    public void stopThread() {
        JL_Log.e(TAG, "ReceiveDataThread stopThread.");
        isRunning = false;
    }

    @Override
    public void run() {
        super.run();
        JL_Log.i(TAG, "ReceiveDataThread start.");
        isRunning = true;
        int exitReason = EXIT_REASON_SUCCESS;
        if (mOnRecvSppDataListener != null) {
            mOnRecvSppDataListener.onThreadStart(getId());
        }
        if (mConnectedSppDev != null && AppUtil.checkHasConnectPermission(mContext)) {
            int iByteReads;
            byte[] bData = new byte[mBlockSize];
            InputStream inputStream = null;
            if (null != mBluetoothSocket) {
                try {
                    inputStream = mBluetoothSocket.getInputStream();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            JL_Log.i(TAG, "ReceiveDataThread isRunning : " + isRunning + ", mBluetoothSocket : " + mBluetoothSocket + ", inputStream : " + inputStream);
            while (isRunning && null != inputStream) {
                try {
                    iByteReads = inputStream.read(bData); //读取socket接收到的数据
                    if (iByteReads <= 0) { //读取不到数据，延时处理
                        Thread.sleep(30);
                        continue;
                    }
                    byte[] data = new byte[iByteReads];
                    System.arraycopy(bData, 0, data, 0, iByteReads);
                    if (mOnRecvSppDataListener != null) {
                        mOnRecvSppDataListener.onRecvSppData(getId(), mConnectedSppDev, mSppUUID, data);
                    }
                } catch (Exception e) {
                    JL_Log.e(TAG, "-ReceiveDataThread- have an exception : " + e + ", sppUUID = " + mSppUUID);
                    e.printStackTrace();
                    exitReason = EXIT_REASON_IO_EXCEPTION;
                    break;
                }
            }
        } else {
            exitReason = EXIT_REASON_PARAM_ERROR;
        }
        isRunning = false;
        if (mOnRecvSppDataListener != null) {
            mOnRecvSppDataListener.onThreadStop(getId(), exitReason, mConnectedSppDev, mSppUUID);
        }
        JL_Log.i(TAG, "ReceiveDataThread exit");
    }


    public interface OnRecvSppDataListener {

        void onThreadStart(long threadID);

        void onRecvSppData(long threadID, BluetoothDevice device, UUID sppUUID, byte[] data);

        void onThreadStop(long threadID, int reason, BluetoothDevice device, UUID sppUUID);
    }
}
