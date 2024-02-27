package com.jieli.otasdk.tool.ota.spp;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelUuid;
import android.os.Parcelable;
import android.os.SystemClock;

import com.jieli.jl_bt_ota.impl.RcspAuth;
import com.jieli.jl_bt_ota.util.BluetoothUtil;
import com.jieli.jl_bt_ota.util.CHexConver;
import com.jieli.jl_bt_ota.util.CommonUtil;
import com.jieli.jl_bt_ota.util.JL_Log;
import com.jieli.jl_bt_ota.util.PreferencesHelper;
import com.jieli.otasdk.MainApplication;
import com.jieli.otasdk.tool.config.ConfigHelper;
import com.jieli.otasdk.tool.ota.spp.interfaces.OnWriteSppDataCallback;
import com.jieli.otasdk.tool.ota.spp.interfaces.SppEventCallback;
import com.jieli.otasdk.util.AppUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Spp 管理类
 *
 * @author zqjasonZhong
 * @since 2021/1/13
 */
public class SppManager implements SendSppDataThread.ISppOp {
    private static final String TAG = "SppManager";
    @SuppressLint("StaticFieldLeak")
    private static volatile SppManager instance;
    private final Context mContext;
    private final boolean isNeedAuth;
    private final BluetoothAdapter mBtAdapter;
    private final SppEventCallbackManager mSppEventCallbackManager;
    //已发现经典蓝牙设备列表
    private final List<BluetoothDevice> mDiscoveredEdrDevices = new ArrayList<>();
    //已连接SPP通道集合
    private final Map<String, ReceiveSppDataThread> mConnectedSppMap = new HashMap<>();
    //设备认证流程封装类
    private final RcspAuth mRcspAuth;
    //设备是否已经认证成功
    private volatile boolean isDeviceAuth;

    //已连接SPP设备
    private volatile BluetoothDevice mConnectedSppDevice;
    //正在连接的SPP设备
    private volatile BluetoothDevice mConnectingSppDevice;
    //正在配对的设备
    private volatile BluetoothDevice mBondingDevice;

    private long mScanTimeout;
    private DiscoveryReceiver mDiscoveryReceiver;

    private UUID mSppUUID;

    private BluetoothReceiver mBluetoothReceiver;
    private ConnectionSppThread mConnectSppThread;
    private SendSppDataThread mSendSppDataThread;

    private final boolean useSppPrivateChannel = ConfigHelper.Companion.getInstance().isUseMultiSppChannel();


    /**
     * 设备SPP的特征值
     */
    public final static UUID UUID_SPP = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
    public final static UUID UUID_DEFAULT_CUSTOM = UUID.fromString("fe010000-1234-5678-abcd-00805f9b34fb");
    /**
     * 自定义SPP通道的特征值
     */
    public final UUID customSppUUID;//UUID.fromString("fb349b5f-8000-cdab-7856-3412000001fe");//UUID.fromString("0000fe01-0000-1000-8000-00805f9b34fb");//UUID.fromString("fb349b5f-8000-cdab-7856-3412000001fe");

    private final static String KEY_SPP_UUID = "spp_uuid";
    private final static int BOND_DEV_TIMEOUT = 30 * 1000; //配对设备超时时间 - 30s
    private final static int CONNECT_DEV_TIMEOUT = 40 * 1000; //连接设备超时时间 - 40s

    private final static int MSG_DISCOVERY_EDR_TIMEOUT = 1022;
    private final static int MSG_CREATE_BOND_TIMEOUT = 1023;
    private final static int MSG_CONNECT_SPP_TIMEOUT = 1024;
    private final Handler mHandler = new Handler(msg -> {
        switch (msg.what) {
            case MSG_DISCOVERY_EDR_TIMEOUT:
                boolean isScanning = isScanning();
                JL_Log.w(TAG, "call MSG_DISCOVERY_EDR_TIMEOUT >> isScanning = " + isScanning);
                if (isScanning) {
                    stopDeviceScan();
                }
                break;
            case MSG_CREATE_BOND_TIMEOUT: {
                if (msg.obj instanceof BluetoothDevice) {
                    BluetoothDevice device = (BluetoothDevice) msg.obj;
                    Bundle bundle = msg.getData();
                    UUID sppUUID = UUID_SPP;
                    if (bundle != null) {
                        sppUUID = (UUID) bundle.getSerializable(KEY_SPP_UUID);
                    }
                    JL_Log.w(TAG, "call MSG_CREATE_BOND_TIMEOUT >> device = " + printDeviceInfo(device) + ", sppUUID = " + sppUUID);
                    if (isPaired(device)) {
                        startConnectSppThread(device, sppUUID);
                    } else {
                        handleSppConnection(device, sppUUID, BluetoothProfile.STATE_DISCONNECTED);
                    }
                }
                break;
            }
            case MSG_CONNECT_SPP_TIMEOUT: {
                if (msg.obj instanceof BluetoothDevice) {
                    BluetoothDevice device = (BluetoothDevice) msg.obj;
                    Bundle bundle = msg.getData();
                    UUID sppUUID = UUID_SPP;
                    if (bundle != null) {
                        sppUUID = (UUID) bundle.getSerializable(KEY_SPP_UUID);
                    }
                    JL_Log.w(TAG, "call MSG_CONNECT_SPP_TIMEOUT >> device = " + printDeviceInfo(device) + ", sppUUID = " + sppUUID);
                    if (!isSppSocketConnected(device, sppUUID)) {
                        handleSppConnection(device, sppUUID, BluetoothProfile.STATE_DISCONNECTED);
                    }
                }
                break;
            }
        }
        return true;
    });

    private SppManager(Context context) {
        this(context, false);
    }

    private SppManager(Context context, boolean isNeedAuth) {
        mContext = context;
        if (CommonUtil.getMainContext() == null) {
            CommonUtil.setMainContext(mContext);
        }
        customSppUUID = UUID.fromString(PreferencesHelper.getSharedPreferences(mContext).getString(KEY_SPP_UUID, UUID_DEFAULT_CUSTOM.toString()));
        this.isNeedAuth = isNeedAuth;
        isDeviceAuth = !isNeedAuth;
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        mSppEventCallbackManager = new SppEventCallbackManager();
        mRcspAuth = new RcspAuth(mContext, (bluetoothDevice, bytes) -> {
            writeDataToSppAsync(bluetoothDevice, UUID_SPP, bytes, (device, sppUUID1, result, data) ->
                    JL_Log.i(TAG, "-sendAuthDataToDevice- device = " + printDeviceInfo(device) + ", result = " + result));
            return true;
        }, mOnRcspAuthListener);

        registerBluetoothReceiver();
    }

    public static SppManager getInstance() {
        if (instance == null) {
            synchronized (SppManager.class) {
                if (instance == null) {
                    instance = new SppManager(MainApplication.getInstance());
                }
            }
        }
        return instance;
    }

    /**
     * 注册SPP事件监听器
     *
     * @param callback SPP事件监听器
     */
    public void registerSppEventCallback(SppEventCallback callback) {
        mSppEventCallbackManager.registerSppEventCallback(callback);
    }

    /**
     * 注销SPP事件监听器
     *
     * @param callback SPP事件监听器
     */
    public void unregisterSppEventCallback(SppEventCallback callback) {
        mSppEventCallbackManager.unregisterSppEventCallback(callback);
    }

    /**
     * 释放资源
     */
    public void release() {
        unregisterDiscoveryReceiver();
        unregisterBluetoothReceiver();
        mDiscoveredEdrDevices.clear();
        stopConnectSppThread();
        if (mConnectedSppDevice != null) {
            disconnectSpp(mConnectedSppDevice, null);
        }
        clearConnectedSppMap();
        mHandler.removeCallbacksAndMessages(null);
        mSppEventCallbackManager.release();
        mRcspAuth.removeListener(mOnRcspAuthListener);
        mRcspAuth.destroy();

        instance = null;
    }

    /**
     * 是否发现设备中
     *
     * @return 结果
     */
    @SuppressLint("MissingPermission")
    public boolean isScanning() {
        if (!AppUtil.checkHasScanPermission(mContext)) return false;
        return mBtAdapter != null && mBtAdapter.isDiscovering();
    }

    /**
     * 开始发现设备
     *
     * @param timeout 超时时间
     * @return 操作结果
     */
    @SuppressLint("MissingPermission")
    public boolean startDeviceScan(long timeout) {
        if (mBtAdapter == null || !AppUtil.checkHasScanPermission(mContext)) {
            JL_Log.e(TAG, "this device is not supported bluetooth.");
            return false;
        }
        if (!BluetoothUtil.isBluetoothEnable()) {
            JL_Log.e(TAG, "Bluetooth is not enable.");
            return false;
        }
        if (isScanning()) {
            boolean ret = mBtAdapter.cancelDiscovery();
            if (ret) {
                unregisterDiscoveryReceiver();
                int count = 0;
                while (mBtAdapter.isDiscovering()) {
                    SystemClock.sleep(100);
                    count += 100;
                    if (count > 2000) {
                        break;
                    }
                }
                mDiscoveredEdrDevices.clear();
            } else {
                return false;
            }
        }
        boolean ret = mBtAdapter.startDiscovery();
        JL_Log.i(TAG, "-startDiscovery- >>>>>> ret : " + ret);
        if (!ret) {
            return false;
        }
        if (timeout < 3000) {
            mScanTimeout = 3000;
        } else {
            mScanTimeout = timeout;
        }
        registerDiscoverReceiver();
        mDiscoveredEdrDevices.clear();
        startScanTimeoutTask();
        mSppEventCallbackManager.onDiscoveryDeviceChange(true);
        syncSystemConnectedDevice();
        return true;
    }

    /**
     * 停止发现设备
     *
     * @return 操作结果
     */
    @SuppressLint("MissingPermission")
    public boolean stopDeviceScan() {
        if (mBtAdapter == null || !AppUtil.checkHasScanPermission(mContext)) {
            JL_Log.e(TAG, "-stopDeviceScan- :: this device is not supported bluetooth.");
            return false;
        }
        if (!BluetoothUtil.isBluetoothEnable()) {
            JL_Log.e(TAG, "-stopDeviceScan- :: Bluetooth is not enable.");
            unregisterDiscoveryReceiver();
            return true;
        }
        if (!mBtAdapter.isDiscovering()) {
            return true;
        }
        boolean ret = mBtAdapter.cancelDiscovery();
        if (!ret) {
            return false;
        }
        stopScanTimeoutTask();
        return true;
    }

    /**
     * 设备是否已配对
     *
     * @param device 蓝牙设备
     * @return 结果
     */
    @SuppressLint("MissingPermission")
    public boolean isPaired(BluetoothDevice device) {
        if (!AppUtil.checkHasConnectPermission(mContext)) return false;
        return null != device && BluetoothDevice.BOND_BONDED == device.getBondState();
    }

    /**
     * SPP是否连接中
     *
     * @return 结果
     */
    public boolean isSppConnecting() {
        return mConnectingSppDevice != null;
    }

    /**
     * 获取连接中设备
     *
     * @return 蓝牙设备
     */
    public BluetoothDevice getConnectingSppDevice() {
        return mConnectingSppDevice;
    }

    /**
     * 获取已连接设备
     *
     * @return 蓝牙设备
     */
    public BluetoothDevice getConnectedSppDevice() {
        return mConnectedSppDevice;
    }

    /**
     * SPP是否已连接
     *
     * @return 结果
     */
    public boolean isSppConnected() {
        return mConnectedSppDevice != null;
    }

    /**
     * 连接SPP通道
     *
     * @param address 设备地址
     * @return 操作结果
     */
    public boolean connectSpp(String address) {
        return connectSpp(BluetoothUtil.getRemoteDevice(address));
    }

    /**
     * 连接默认的SPP通道
     *
     * @param device 蓝牙设备
     * @return 操作结果
     */
    public boolean connectSpp(BluetoothDevice device) {
        return connectSpp(device, UUID_SPP);
    }

    /**
     * 连接SPP通道
     *
     * @param device  蓝牙设备
     * @param sppUUID 指定的SPP通道
     * @return 操作结果
     */
    @SuppressLint("MissingPermission")
    public boolean connectSpp(BluetoothDevice device, UUID sppUUID) {
        if (!AppUtil.checkHasConnectPermission(mContext)) {
            JL_Log.w(TAG, "-connectSpp- miss bluetooth permission.");
            return false;
        }
        if (device == null || device.getType() == BluetoothDevice.DEVICE_TYPE_LE) {
            JL_Log.w(TAG, "-connectSpp-  device is bad object. ");
            return false;
        }
        JL_Log.i(TAG, "-connectSpp- >> device : " + printDeviceInfo(device) + ", sppUUID = " + sppUUID);
        if (mConnectingSppDevice != null) {
            JL_Log.i(TAG, "-connectSpp- >>  device is connecting. device :" + printDeviceInfo(mConnectedSppDevice));
            return false;
        }
        if (mConnectedSppDevice != null) {
            if (BluetoothUtil.deviceEquals(mConnectedSppDevice, device)) {
                if (isSppSocketConnected(device, sppUUID)) { //已连接
                    if (!isNeedAuth || isDevAuth(sppUUID)) {
                        handleSppConnection(device, sppUUID, BluetoothProfile.STATE_CONNECTED);
                        return true;
                    } else {
                        JL_Log.i(TAG, "-connectSpp- >>  device in process of certification. device :" + printDeviceInfo(device));
                        return false;
                    }
                }
            } else {
                if (disconnectSpp(mConnectedSppDevice, null)) {
                    SystemClock.sleep(500);
                }
            }
        }
        boolean ret;
        setConnectingSppDevice(device);
        setSppUUID(sppUUID);
        boolean isPaired = isPaired(device);
        JL_Log.i(TAG, "-connectSpp- >> isPaired = " + isPaired);
        if (!isPaired) {//设备未配对
            ret = BluetoothUtil.createBond(device);
            JL_Log.i(TAG, "-connectSpp- >> createBond = " + ret);
            if (!ret) {
                handleSppConnection(device, sppUUID, BluetoothProfile.STATE_DISCONNECTED);
                return false;
            } else {//设备开始配对
                startPairTimeoutTask(device, sppUUID);
            }
        } else {//设备已配对
            if (device.getUuids() == null || !BluetoothUtil.deviceHasProfile(device, sppUUID)) {
                ret = device.fetchUuidsWithSdp(); //更新UUID
                JL_Log.i(TAG, "-connectSpp- >> fetchUuidsWithSdp = " + ret);
                if (!ret) {
                    handleSppConnection(device, sppUUID, BluetoothProfile.STATE_DISCONNECTED);
                    return false;
                }
            } else {//开始连接设备
                startConnectSppThread(device, sppUUID);
            }
        }
        //进入连接中状态，开始连接超时任务
        handleSppConnection(device, sppUUID, BluetoothProfile.STATE_CONNECTING);
        startConnectTimeoutTask(device, sppUUID);
        return true;
    }

    /**
     * 断开SPP通道
     *
     * @param device  已连接设备
     * @param sppUUID 指定SPP通道
     *                (若为null,则断开设备的所有SPP连接)
     * @return 操作结果
     */
    @SuppressLint("MissingPermission")
    public boolean disconnectSpp(BluetoothDevice device, UUID sppUUID) {
        if (!AppUtil.checkHasConnectPermission(mContext)) {
            JL_Log.w(TAG, "-disconnectSpp- miss bluetooth permission.");
            return false;
        }
        if (!BluetoothUtil.deviceEquals(device, mConnectedSppDevice)) {
            JL_Log.e(TAG, "-disconnectSpp- >> device is not connected. device = " + printDeviceInfo(device)
                    + ",\n ConnectedSppDevice = " + printDeviceInfo(mConnectedSppDevice));
            return false;
        }
        JL_Log.i(TAG, "-disconnectSpp- device = " + printDeviceInfo(device) + ", sppUUID = " + sppUUID);
        if (sppUUID == null) {
            if (!isConnectedSocketMapEmpty(device)) {
                Set<String> ketSet = mConnectedSppMap.keySet();
                ArrayList<UUID> uuidList = new ArrayList<>();
                String address = device.getAddress();
                for (String ket : ketSet) {
                    String[] strings = ket.split("_");
                    if (strings.length == 2 && address.equals(strings[0])) {
                        uuidList.add(UUID.fromString(strings[1]));
                    }
                }
                for (UUID uuid : uuidList) {
                    disconnectSpp(device, uuid);
                }
            }
        } else {
            ReceiveSppDataThread receiveSppDataThread = removeRecvSppDataThread(device, sppUUID);
            if (receiveSppDataThread != null) {
                BluetoothSocket socket = receiveSppDataThread.getBluetoothSocket();
                if (socket != null) {
                    try {
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                receiveSppDataThread.stopThread();
            }
            handleSppConnection(device, sppUUID, BluetoothProfile.STATE_DISCONNECTED);
        }
        return true;
    }


    /**
     * 通过SPP通道发送数据包
     *
     * @param device 经典蓝牙设备
     * @param data   数据包
     * @return 发送结果
     */
    @SuppressLint("MissingPermission")
    @Override
    public synchronized boolean writeDataToSppDevice(BluetoothDevice device, UUID sppUUID, byte[] data) throws IOException {
        if (!AppUtil.checkHasConnectPermission(mContext)) {
            JL_Log.w(TAG, "-writeDataToSppDevice- miss bluetooth permission.");
            return false;
        }
        if (null == device || null == data) {
            JL_Log.e(TAG, "-writeDataToSppDevice- param is error.");
            return false;
        }
        if (!BluetoothUtil.deviceEquals(device, mConnectedSppDevice)) {
            JL_Log.e(TAG, "-writeDataToSppDevice- device is error. device = " + printDeviceInfo(device));
            return false;
        }
        ReceiveSppDataThread receiveSppDataThread = getRecvSppDataThread(device, sppUUID);
        if (receiveSppDataThread == null) {
            JL_Log.e(TAG, "-writeDataToSppDevice- receiveSppDataThread is null. device = " + printDeviceInfo(device) + ", sppUUID = " + sppUUID);
            return false;
        }
        BluetoothSocket socket = receiveSppDataThread.getBluetoothSocket();
        if (null == socket || !socket.isConnected() || socket.getOutputStream() == null) {
            JL_Log.e(TAG, "-writeDataToSppDevice- spp socket is close.");
            return false;
        }
        socket.getOutputStream().write(data);
        JL_Log.d(TAG, "-writeDataToSppDevice- device = " + printDeviceInfo(device) + ", sppUUID = " + sppUUID
                + "\n send ret = true, raw data = " + CHexConver.byte2HexStr(data));
        return true;
    }

    /**
     * 通过SPP通道发送数据包（异步方式）
     *
     * @param device   蓝牙设备
     * @param data     数据
     * @param callback 结果回调
     */
    public void writeDataToSppAsync(BluetoothDevice device, UUID sppUUID, byte[] data, OnWriteSppDataCallback callback) {
        addSendTask(device, sppUUID, data, callback);
    }

    /**
     * 检查设备是否通过设备认证
     *
     * @param device  蓝牙设备
     * @param sppUUID SPP通道
     * @return 结果
     */
    public boolean checkDeviceIsAuth(BluetoothDevice device, UUID sppUUID) {
        return BluetoothUtil.deviceEquals(device, mConnectedSppDevice) && (!isNeedAuth || isDevAuth(sppUUID));
    }

    /**
     * SPP通道是否已连接
     *
     * @param device  蓝牙设备
     * @param sppUUID SPP通道
     * @return 结果
     */
    @SuppressLint("MissingPermission")
    public boolean isSppSocketConnected(BluetoothDevice device, UUID sppUUID) {
        if (!AppUtil.checkHasConnectPermission(mContext)) {
            JL_Log.w(TAG, "-isSppSocketConnected- miss bluetooth permission.");
            return false;
        }
        ReceiveSppDataThread thread = getRecvSppDataThread(device, sppUUID);
        if (thread == null) return false;
        BluetoothSocket socket = thread.getBluetoothSocket();
        return socket != null && socket.isConnected();
    }

    private void setConnectingSppDevice(BluetoothDevice mConnectingSppDevice) {
        this.mConnectingSppDevice = mConnectingSppDevice;
    }

    private void setConnectedSppDevice(BluetoothDevice mConnectedSppDevice) {
        this.mConnectedSppDevice = mConnectedSppDevice;
        if (mConnectedSppDevice != null) {
            setConnectingSppDevice(null);
        } else {
            isDeviceAuth = false;
        }
    }

    private void setSppUUID(UUID mSppUUID) {
        this.mSppUUID = mSppUUID;
    }

    private boolean isDevAuth(UUID sppUUID) {
        if (!UUID_SPP.equals(sppUUID)) return true;
        return isDeviceAuth;
    }

    private String getSocketUUID(BluetoothDevice device, UUID sppUUID) {
        if (null == device || null == sppUUID) return null;
        return device.getAddress() + "_" + sppUUID;
    }

    private void addRecvSppDataThread(String socketUUID, ReceiveSppDataThread thread) {
        if (null == socketUUID || null == thread) return;
        mConnectedSppMap.put(socketUUID, thread);
    }

    private ReceiveSppDataThread getRecvSppDataThread(BluetoothDevice device, UUID sppUUID) {
        if (null == getSocketUUID(device, sppUUID)) return null;
        return mConnectedSppMap.get(getSocketUUID(device, sppUUID));
    }

    private ReceiveSppDataThread removeRecvSppDataThread(BluetoothDevice device, UUID sppUUID) {
        if (null == getSocketUUID(device, sppUUID)) return null;
        return mConnectedSppMap.remove(getSocketUUID(device, sppUUID));
    }

    private boolean isConnectedSocketMapEmpty(BluetoothDevice device) {
        if (device == null) return true;
        if (mConnectedSppMap.isEmpty()) return true;
        String address = device.getAddress();
        Set<String> keySet = mConnectedSppMap.keySet();
        for (String key : keySet) {
            String[] strings = key.split("_");
            if (strings.length == 2 && address.equals(strings[0])) {
                return false;
            }
        }
        return true;
    }

    private void clearConnectedSppMap() {
        if (!mConnectedSppMap.isEmpty()) {
            Set<String> keySet = mConnectedSppMap.keySet();
            for (String key : keySet) {
                ReceiveSppDataThread thread = mConnectedSppMap.get(key);
                if (thread != null) {
                    thread.stopThread();
                }
            }
            mConnectedSppMap.clear();
        }
    }

    private void syncSystemConnectedDevice() {
        if (!AppUtil.checkHasConnectPermission(mContext)) return;
        List<BluetoothDevice> list = BluetoothUtil.getSystemConnectedBtDeviceList(mContext);
        if (null == list || list.isEmpty()) return;
        for (BluetoothDevice device : list) {
            if (isSppDevice(device) && !BluetoothUtil.deviceEquals(mConnectedSppDevice, device)) {
                if (!mDiscoveredEdrDevices.contains(device)) {
                    mDiscoveredEdrDevices.add(device);
                    mSppEventCallbackManager.onDiscoveryDevice(device, 0);
                }
            }
        }
    }

    private void startScanTimeoutTask() {
        if (mScanTimeout == 0) return;
        JL_Log.d(TAG, "-startScanTimeoutTask- mScanTimeout = " + mScanTimeout);
        mHandler.removeMessages(MSG_DISCOVERY_EDR_TIMEOUT);
        mHandler.sendEmptyMessageDelayed(MSG_DISCOVERY_EDR_TIMEOUT, mScanTimeout);
    }

    private void stopScanTimeoutTask() {
        JL_Log.d(TAG, "-stopScanTimeoutTask-");
        mHandler.removeMessages(MSG_DISCOVERY_EDR_TIMEOUT);
    }

    private void startPairTimeoutTask(BluetoothDevice device, UUID sppUUID) {
        JL_Log.d(TAG, "-startPairTimeoutTask- device = " + printDeviceInfo(device) + ", sppUUID = " + sppUUID);
        mHandler.removeMessages(MSG_CREATE_BOND_TIMEOUT);
        mBondingDevice = device;
        Bundle bundle = new Bundle();
        bundle.putSerializable(KEY_SPP_UUID, sppUUID);
        Message message = mHandler.obtainMessage(MSG_CREATE_BOND_TIMEOUT, device);
        message.setData(bundle);
        mHandler.sendMessageDelayed(message, BOND_DEV_TIMEOUT);
    }

    private void stopPairTimeoutTask(BluetoothDevice device) {
        JL_Log.d(TAG, "-stopPairTimeoutTask- device = " + printDeviceInfo(device));
        if (BluetoothUtil.deviceEquals(device, mBondingDevice)) {
            mHandler.removeMessages(MSG_CREATE_BOND_TIMEOUT);
            mBondingDevice = null;
        }
    }

    private void startConnectTimeoutTask(BluetoothDevice device, UUID sppUUID) {
        JL_Log.i(TAG, "-startConnectTimeoutTask- device = " + printDeviceInfo(device) + ", sppUUID = " + sppUUID);
        mHandler.removeMessages(MSG_CONNECT_SPP_TIMEOUT);
        Bundle bundle = new Bundle();
        bundle.putSerializable(KEY_SPP_UUID, sppUUID);
        Message message = mHandler.obtainMessage(MSG_CONNECT_SPP_TIMEOUT, device);
        message.setData(bundle);
        mHandler.sendMessageDelayed(message, CONNECT_DEV_TIMEOUT);
    }

    private void stopConnectTimeoutTask() {
        JL_Log.w(TAG, "-stopConnectTimeoutTask-");
        mHandler.removeMessages(MSG_CONNECT_SPP_TIMEOUT);
    }

    private void registerDiscoverReceiver() {
        if (mDiscoveryReceiver == null) {
            mDiscoveryReceiver = new DiscoveryReceiver();
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
            intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
            intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
            mContext.registerReceiver(mDiscoveryReceiver, intentFilter);
        }
    }

    private void unregisterDiscoveryReceiver() {
        if (mDiscoveryReceiver != null) {
            mContext.unregisterReceiver(mDiscoveryReceiver);
            mDiscoveryReceiver = null;
        }
    }

    private void registerBluetoothReceiver() {
        if (mBluetoothReceiver == null) {
            mBluetoothReceiver = new BluetoothReceiver();
            IntentFilter intentFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
            intentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
            intentFilter.addAction(BluetoothDevice.ACTION_UUID);
            mContext.registerReceiver(mBluetoothReceiver, intentFilter);
        }
    }

    private void unregisterBluetoothReceiver() {
        if (mBluetoothReceiver != null) {
            mContext.unregisterReceiver(mBluetoothReceiver);
            mBluetoothReceiver = null;
        }
    }

    private String printDeviceInfo(BluetoothDevice device) {
        return BluetoothUtil.printBtDeviceInfo(mContext, device);
    }

    private void startConnectSppThread(BluetoothDevice device, UUID sppUUID) {
        JL_Log.d(TAG, "-startConnectSppThread- device = " + printDeviceInfo(device) + ", sppUUID = " + sppUUID);
        if (mConnectSppThread == null) {
            mConnectSppThread = new ConnectionSppThread(mContext, device, sppUUID, new ConnectionSppThread.OnConnectSppListener() {
                @Override
                public void onThreadStart(long threadID) {

                }

                @Override
                public void onThreadStop(long threadID, boolean result, BluetoothDevice device, UUID sppUUID, BluetoothSocket socket) {
                    if (mConnectSppThread != null && mConnectSppThread.getId() == threadID) {
                        mConnectSppThread = null;
                    }
                    if (result) {
                        //设置已连接设备
                        setConnectedSppDevice(device);
                        //开启读数据线程
                        startReceiveSppDataThread(device, sppUUID, socket);
                        //开启发数据线程
                        startSendSppDataThread();
                        if (isNeedAuth && !checkDeviceIsAuth(device, sppUUID)) { //需要设备认证
                            mRcspAuth.stopAuth(device, false);
                            if (!mRcspAuth.startAuth(device)) {//开启设备认证失败
                                disconnectSpp(device, sppUUID);
                            } else {
                                setSppUUID(sppUUID);
                            }
                        } else { //不需要设备认证
                            handleSppConnection(device, sppUUID, BluetoothProfile.STATE_CONNECTED);
                        }
                    } else {
                        handleSppConnection(device, sppUUID, BluetoothProfile.STATE_DISCONNECTED);
                    }
                }
            });
            mConnectSppThread.start();
        }
    }

    private void stopConnectSppThread() {
        if (mConnectSppThread != null) {
            if (mConnectSppThread.isAlive()) {
                mConnectSppThread.interrupt();
            }
            mConnectSppThread = null;
        }
    }

    private void startReceiveSppDataThread(BluetoothDevice device, UUID sppUUID, BluetoothSocket socket) {
        ReceiveSppDataThread receiveSppDataThread = getRecvSppDataThread(device, sppUUID);
        if (receiveSppDataThread == null) {
            receiveSppDataThread = new ReceiveSppDataThread(mContext, device, sppUUID, socket,
                    new ReceiveSppDataThread.OnRecvSppDataListener() {
                        @Override
                        public void onThreadStart(long threadID) {

                        }

                        @Override
                        public void onRecvSppData(long threadID, BluetoothDevice device, UUID sppUUID, byte[] data) {
                            JL_Log.d(TAG, "-onRecvSppData- device = " + printDeviceInfo(device) + ", sppUUID = " + sppUUID
                                    + ", \n raw data = " + CHexConver.byte2HexStr(data));
                            mSppEventCallbackManager.onReceiveSppData(device, sppUUID, data);
                            if (!checkDeviceIsAuth(device, sppUUID)) {
                                mRcspAuth.handleAuthData(device, data);
                            }
                        }

                        @Override
                        public void onThreadStop(long threadID, int reason, BluetoothDevice device, UUID sppUUID) {
                            if (reason == ReceiveSppDataThread.EXIT_REASON_IO_EXCEPTION) {
                                disconnectSpp(device, sppUUID);
                            }
                        }
                    });
            addRecvSppDataThread(getSocketUUID(device, sppUUID), receiveSppDataThread);
            receiveSppDataThread.start();
        }
    }

    private void stopReceiveSppDataThread(BluetoothDevice device, UUID sppUUID) {
        ReceiveSppDataThread receiveSppDataThread = removeRecvSppDataThread(device, sppUUID);
        if (receiveSppDataThread != null) {
            receiveSppDataThread.stopThread();
        }
    }

    private void startSendSppDataThread() {
        if (mSendSppDataThread == null) {
            mSendSppDataThread = new SendSppDataThread(mContext, this,
                    new SendSppDataThread.OnSendDataListener() {
                        @Override
                        public void onThreadStart(long threadID) {

                        }

                        @Override
                        public void onThreadStop(long threadID) {
                            if (mSendSppDataThread != null && mSendSppDataThread.getId() == threadID) {
                                mSendSppDataThread = null;
                            }
                        }
                    });
            mSendSppDataThread.start();
        }
    }

    private void stopSendSppDataThread() {
        if (mSendSppDataThread != null) {
            mSendSppDataThread.stopThread();
        }
    }

    private void addSendTask(BluetoothDevice device, UUID sppUUID, byte[] data, OnWriteSppDataCallback callback) {
        if (mSendSppDataThread != null) {
            mSendSppDataThread.addSendTask(new SendSppDataThread.SppSendTask(device, sppUUID, data, callback));
        }
    }

    private boolean isValidDevice(BluetoothDevice device) {
        return BluetoothUtil.deviceEquals(mConnectingSppDevice, device) || BluetoothUtil.deviceEquals(device, mBondingDevice)
                || BluetoothUtil.deviceEquals(device, mConnectedSppDevice);
    }

    private void handleSppConnection(BluetoothDevice device, UUID sppUUID, int status) {
        boolean isValidDevice = isValidDevice(device);
        JL_Log.i(TAG, "-handleSppConnection- device = " + printDeviceInfo(device) + ", isValidDevice = " + isValidDevice + ", sppUUID = " + sppUUID + ", status = " + status);
        if (isValidDevice) {
            if (status == BluetoothProfile.STATE_DISCONNECTED || status == BluetoothProfile.STATE_CONNECTED) {
                if (BluetoothUtil.deviceEquals(device, mConnectingSppDevice)) {
                    setConnectingSppDevice(null);
                }
                stopConnectTimeoutTask();
                if (status == BluetoothProfile.STATE_DISCONNECTED) {
                    if (BluetoothUtil.deviceEquals(device, mConnectedSppDevice)) {
                        stopReceiveSppDataThread(device, sppUUID);
                        if (isConnectedSocketMapEmpty(device)) { //如果剩余socket为空，则停止发数
                            stopSendSppDataThread();
                            setConnectedSppDevice(null);
                        }
                    }
                } else {
                    if (mConnectedSppDevice == null) {
                        setConnectedSppDevice(device);
                    }
                }
            }
            mSppEventCallbackManager.onSppConnection(device, sppUUID, status);
            if (useSppPrivateChannel) {
                if (status == BluetoothProfile.STATE_CONNECTED) {
                    if (!customSppUUID.equals(sppUUID)) {
                        connectSpp(device, customSppUUID);
                    }
                } else if (status == BluetoothProfile.STATE_DISCONNECTED && BluetoothUtil.deviceEquals(mConnectedSppDevice, device)) {
                    disconnectSpp(mConnectedSppDevice, null);
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private boolean isSppDevice(BluetoothDevice device) {
        if (!AppUtil.checkHasConnectPermission(mContext)) return false;
        return device != null && device.getType() != BluetoothDevice.DEVICE_TYPE_LE;
    }

    private final RcspAuth.OnRcspAuthListener mOnRcspAuthListener = new RcspAuth.OnRcspAuthListener() {
        @Override
        public void onInitResult(boolean result) {
            JL_Log.e(TAG, "-onInitResult- " + result);
        }

        @Override
        public void onAuthSuccess(BluetoothDevice device) {
            JL_Log.w(TAG, "-onAuthSuccess- >>> auth ok, handleSppConnection : " + printDeviceInfo(device));
            isDeviceAuth = true;
            handleSppConnection(device, mSppUUID, BluetoothProfile.STATE_CONNECTED);
        }

        @Override
        public void onAuthFailed(BluetoothDevice device, int code, String message) {
            JL_Log.w(TAG, String.format(Locale.getDefault(), "-onAuthFailed- device : %s, code : %d, message : %s",
                    printDeviceInfo(device), code, message));
            isDeviceAuth = false;
            disconnectSpp(device, mSppUUID);
        }
    };

    private class DiscoveryReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null) return;
            String action = intent.getAction();
            if (action == null) return;
            switch (action) {
                case BluetoothAdapter.ACTION_DISCOVERY_STARTED: {
                    JL_Log.d(TAG, "recv action : ACTION_DISCOVERY_STARTED");
//                    mDiscoveredEdrDevices.clear();
//                    startScanTimeoutTask();
//                    mSppEventCallbackManager.onDiscoveryDeviceChange(true);
                    break;
                }
                case BluetoothAdapter.ACTION_DISCOVERY_FINISHED: {
                    JL_Log.d(TAG, "recv action : ACTION_DISCOVERY_FINISHED");
                    stopScanTimeoutTask();
                    unregisterDiscoveryReceiver();
                    mSppEventCallbackManager.onDiscoveryDeviceChange(false);
                    break;
                }
                case BluetoothDevice.ACTION_FOUND: {
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    short rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, (short) -1);
//                    JL_Log.d(TAG, "recv action : ACTION_FOUND, device = " + printDeviceInfo(device) + ", rssi = " + rssi);
                    if (isSppDevice(device) && BluetoothUtil.isBluetoothEnable()) {
                        if (!mDiscoveredEdrDevices.contains(device)) {
                            mDiscoveredEdrDevices.add(device);
                            mSppEventCallbackManager.onDiscoveryDevice(device, rssi);
                        }
                    }
                    break;
                }
            }
        }
    }

    private class BluetoothReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null) return;
            String action = intent.getAction();
            if (action == null) return;
            switch (action) {
                case BluetoothAdapter.ACTION_STATE_CHANGED: {
                    int state = intent.getIntExtra(BluetoothAdapter.EXTRA_CONNECTION_STATE, -1);
                    if (mBtAdapter != null && state == -1) {
                        state = mBtAdapter.getState();
                    }
                    if (state == BluetoothAdapter.STATE_OFF) { //蓝牙已关闭
                        mDiscoveredEdrDevices.clear();
                        mSppEventCallbackManager.onDiscoveryDeviceChange(false);
                        disconnectSpp(getConnectedSppDevice(), null);
                        mSppEventCallbackManager.onAdapterChange(false);
                    } else if (state == BluetoothAdapter.STATE_ON) { //蓝牙已打开
                        mSppEventCallbackManager.onAdapterChange(true);
                    }
                    break;
                }
                case BluetoothDevice.ACTION_BOND_STATE_CHANGED: {
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    if (device == null || !AppUtil.checkHasConnectPermission(context)) return;
                    int bond = device.getBondState();
                    boolean isValidDevice = isValidDevice(device);
                    JL_Log.i(TAG, "recv action : ACTION_BOND_STATE_CHANGED >>> device = " + printDeviceInfo(device) + ", bond = " + bond + ", isValidDevice = " + isValidDevice);
                    if (bond == BluetoothDevice.BOND_NONE || bond == BluetoothDevice.BOND_BONDED) {
                        if (isValidDevice) {
                            stopPairTimeoutTask(device);
                            if (bond == BluetoothDevice.BOND_BONDED) { //配对成功
                                startConnectSppThread(device, mSppUUID);
                            } else {
                                handleSppConnection(device, mSppUUID, BluetoothProfile.STATE_DISCONNECTED);
                            }
                        }
                    }
                    break;
                }
                case BluetoothDevice.ACTION_UUID: {
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    if (null == device) return;
                    Parcelable[] parcelUuids = intent.getParcelableArrayExtra(BluetoothDevice.EXTRA_UUID);
                    if (null == parcelUuids) {
                        JL_Log.e(TAG, "recv action : ACTION_UUID >>> no uuids");
                    } else {
                        ParcelUuid[] uuids = new ParcelUuid[parcelUuids.length];
                        for (int i = 0; i < parcelUuids.length; i++) {
                            uuids[i] = ParcelUuid.fromString(parcelUuids[i].toString());
                            JL_Log.i(TAG, "recv action : ACTION_UUID >>> index = " + i + " uuid = " + uuids[i]);
                        }
                    }
                    JL_Log.d(TAG, "recv action : ACTION_UUID >>> mConnectingSppDevice = " + printDeviceInfo(mConnectingSppDevice)
                            + ", device = " + printDeviceInfo(device));
                    if (BluetoothUtil.deviceEquals(mConnectingSppDevice, device)) {
                        startConnectSppThread(device, mSppUUID);
                    }
                    break;
                }
            }
        }
    }
}
