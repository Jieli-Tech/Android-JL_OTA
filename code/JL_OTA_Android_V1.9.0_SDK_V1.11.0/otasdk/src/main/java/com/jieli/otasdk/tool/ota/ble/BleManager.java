package com.jieli.otasdk.tool.ota.ble;

import static android.os.Build.VERSION_CODES.LOLLIPOP;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.jieli.broadcastbox.multidevice.ReConnectHelper;
import com.jieli.jl_bt_ota.constant.BluetoothConstant;
import com.jieli.jl_bt_ota.util.BluetoothUtil;
import com.jieli.jl_bt_ota.util.CHexConver;
import com.jieli.jl_bt_ota.util.CommonUtil;
import com.jieli.jl_bt_ota.util.JL_Log;
import com.jieli.jl_bt_ota.util.UuidUtil;
import com.jieli.otasdk.MainApplication;
import com.jieli.otasdk.tool.config.ConfigHelper;
import com.jieli.otasdk.tool.ota.ble.interfaces.BleEventCallback;
import com.jieli.otasdk.tool.ota.ble.interfaces.OnWriteDataCallback;
import com.jieli.otasdk.tool.ota.ble.model.BleConnectParam;
import com.jieli.otasdk.tool.ota.ble.model.BleDevice;
import com.jieli.otasdk.tool.ota.ble.model.BleScanInfo;
import com.jieli.otasdk.tool.ota.ble.model.BleSendTask;
import com.jieli.otasdk.util.AppUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Ble连接管理类
 *
 * @author zqjasonZhong
 * @since 2020/7/16
 */
public class BleManager {
    private final static String TAG = BleManager.class.getSimpleName();

    private final static int MIN_CONNECT_TIME = 8 * 1000; //连接最小时间超时
    //BLE服务UUID
    public final static UUID BLE_UUID_SERVICE = BluetoothConstant.UUID_SERVICE;
    //BLE的写特征UUID
    public final static UUID BLE_UUID_WRITE = BluetoothConstant.UUID_WRITE;
    //BLE的通知特征UUID
    public final static UUID BLE_UUID_NOTIFICATION = BluetoothConstant.UUID_NOTIFICATION;
    //BLE的通知特征的描述符UUID
    public final static UUID BLE_UUID_NOTIFICATION_DESCRIPTOR = UUID.fromString("00002902-0000-1000-8000-00805F9B34FB");

    /**
     * 发送数据最大超时 - 8 秒
     */
    public final static int SEND_DATA_MAX_TIMEOUT = 8000; //8 s
    private final static int SCAN_BLE_TIMEOUT = 12 * 1000;  //建议搜索BLE最小时间
    private final static int CONNECT_BLE_TIMEOUT = 40 * 1000;

    private final static int CALLBACK_TIMEOUT = 6000;
    private final static int RECONNECT_BLE_DELAY = 2000;

    /**
     * 搜索BLE设备超时消息
     */
    private final static int MSG_SCAN_BLE_TIMEOUT = 0x1010;
    /**
     * 连接BLE设备超时消息
     */
    private final static int MSG_CONNECT_BLE_TIMEOUT = 0x1011;
    /**
     * 搜索HID设备消息
     */
    private final static int MSG_SCAN_HID_DEVICE = 0X1012;
    /**
     * 使能BLE属性超时消息
     */
    private final static int MSG_NOTIFY_BLE_TIMEOUT = 0x1013;
    /**
     * 调整BLE的MTU超时消息
     */
    private final static int MSG_CHANGE_BLE_MTU_TIMEOUT = 0x1014;
    /**
     * 发现BLE服务超时消息
     */
    private final static int MSG_DISCOVER_SERVICES_TIMEOUT = 0x1015;
    /**
     * 断开BLE超时消息
     */
    private final static int MSG_DISCONNECT_BLE_TIMEOUT = 0x1016;
    /**
     * 检查系统BLE连接超时消息
     */
    private final static int MSG_CHECK_SYSTEM_BLE_TIMEOUT = 0x1017;

    @SuppressLint("StaticFieldLeak")
    private volatile static BleManager instance;
    private final Context mContext;
    private final ConfigHelper configHelper = ConfigHelper.Companion.getInstance();
    private final BluetoothAdapter mBluetoothAdapter;
    private final BluetoothLeScanner mBluetoothLeScanner;
    private final ReConnectHelper mReConnectHelper;
    private BaseBtAdapterReceiver mAdapterReceiver;

    private final Map<String, BleDevice> mBleDeviceMap = new HashMap<>();
    private final List<BluetoothDevice> mDiscoveredBleDevices = new ArrayList<>();
    private final BleEventCallbackManager mCallbackManager = new BleEventCallbackManager();

    private volatile boolean isBleScanning;
    private NotifyCharacteristicRunnable mNotifyCharacteristicRunnable;
    private CheckSystemBleThread checkSystemBleThread;


    private final Handler mHandler = new Handler(Looper.getMainLooper(), new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message msg) {
            switch (msg.what) {
                case MSG_SCAN_BLE_TIMEOUT: {
                    JL_Log.i(TAG, "MSG_SCAN_BLE_TIMEOUT", "");
                    stopLeScan();
                    break;
                }
                case MSG_CONNECT_BLE_TIMEOUT: {
                    if (!(msg.obj instanceof BluetoothDevice)) return false;
                    BluetoothDevice device = (BluetoothDevice) msg.obj;
                    JL_Log.i(TAG, "MSG_CONNECT_BLE_TIMEOUT", "device : " + printDeviceInfo(device));
                    disconnectBleDevice(device);
                    break;
                }
                case MSG_SCAN_HID_DEVICE: {
                    findHidDevice();
                    break;
                }
                case MSG_NOTIFY_BLE_TIMEOUT: {
                    if (!(msg.obj instanceof BluetoothDevice)) return false;
                    BluetoothDevice device = (BluetoothDevice) msg.obj;
                    JL_Log.i(TAG, "MSG_NOTIFY_BLE_TIMEOUT", "device : " + printDeviceInfo(device));
                    disconnectBleDevice(device);
                    break;
                }
                case MSG_CHANGE_BLE_MTU_TIMEOUT: {
                    if (!(msg.obj instanceof BluetoothDevice)) return false;
                    BluetoothDevice device = (BluetoothDevice) msg.obj;
                    BleDevice bleDevice = getBleDevice(device);
                    JL_Log.i(TAG, "MSG_CHANGE_BLE_MTU_TIMEOUT", "device : " + printDeviceInfo(device) + ", " + bleDevice);
                    if (null == bleDevice) return false;
                    bleDevice.setMtu(BluetoothConstant.BLE_MTU_MIN);
                    handleBleConnection(device, BluetoothProfile.STATE_CONNECTED);
                    break;
                }
                case MSG_DISCOVER_SERVICES_TIMEOUT: {
                    if (!(msg.obj instanceof BluetoothDevice)) return false;
                    BluetoothDevice device = (BluetoothDevice) msg.obj;
                    BleDevice bleDevice = getBleDevice(device);
                    JL_Log.i(TAG, "MSG_DISCOVER_SERVICES_TIMEOUT", "device : " + printDeviceInfo(device) + ", " + bleDevice);
                    if (null == bleDevice) return false;
                    final BluetoothGatt gatt = bleDevice.getGatt();
                    if (gatt != null) {
                        List<BluetoothGattService> services = gatt.getServices();
                        if (services != null && !services.isEmpty()) {
                            mBluetoothGattCallback.onServicesDiscovered(gatt, BluetoothGatt.GATT_SUCCESS);
                            break;
                        }
                    }
                    if (!bleDevice.isOverReconnectLimit()) {
                        JL_Log.i(TAG, "MSG_DISCOVER_SERVICES_TIMEOUT", "reconnectBleDevice");
                        bleDevice.setNeedReconnect(true);
                    }
                    disconnectBleDevice(device);
                    break;
                }
                case MSG_DISCONNECT_BLE_TIMEOUT: {
                    if (!(msg.obj instanceof BluetoothDevice)) return false;
                    BluetoothDevice device = (BluetoothDevice) msg.obj;
                    final BluetoothGatt gatt = getConnectedBtGatt(device);
                    JL_Log.d(TAG, "MSG_DISCONNECT_BLE_TIMEOUT", "device : " + printDeviceInfo(device) + ", gatt : " + gatt);
                    if (null == gatt) return false;
                    closeGatt(gatt);
                    handleBleConnection(device, BluetoothProfile.STATE_DISCONNECTED);
                    break;
                }
                case MSG_CHECK_SYSTEM_BLE_TIMEOUT: {
                    stopCheckSystemBleThread();
                    break;
                }
            }
            return true;
        }
    });


    private BleManager(Context context) {
        mContext = CommonUtil.checkNotNull(context);
        if (CommonUtil.getMainContext() == null) {
            CommonUtil.setMainContext(context);
        }
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (Build.VERSION.SDK_INT >= LOLLIPOP && mBluetoothAdapter != null) {
            mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
        } else {
            mBluetoothLeScanner = null;
        }
        mReConnectHelper = new ReConnectHelper(context, this);
        registerReceiver();
    }

    public static BleManager getInstance() {
        if (instance == null) {
            synchronized (BleManager.class) {
                if (instance == null) {
                    instance = new BleManager(MainApplication.getInstance());
                    JL_Log.w(TAG, "init", "instance : " + instance);
                }
            }
        }
        return instance;
    }

    public void destroy() {
        JL_Log.w(TAG, "destroy", "instance : " + instance);
        unregisterReceiver();
        clearConnectedBleDevices();

        if (isBleScanning()) stopLeScan();
        setBleScanning(false, 0);
        mDiscoveredBleDevices.clear();
        mReConnectHelper.release();

        mCallbackManager.release();
        mHandler.removeCallbacksAndMessages(null);
        instance = null;
    }

    public void registerBleEventCallback(BleEventCallback callback) {
        mCallbackManager.addCallback(callback);
    }

    public void unregisterBleEventCallback(BleEventCallback callback) {
        mCallbackManager.removeCallback(callback);
    }

    public boolean isBluetoothEnable() {
        return mBluetoothAdapter != null && mBluetoothAdapter.isEnabled();
    }

    public boolean isBleScanning() {
        return isBleScanning;
    }

    @SuppressLint("MissingPermission")
    public boolean startLeScan(long timeout) {
        if (!checkScanEnv("startLeScan")) return false;
        if (!AppUtil.isHasLocationPermission(mContext)) {
            JL_Log.w(TAG, "startLeScan", "Missing location permissions.");
            return false;
        }
        if (timeout <= 0) timeout = SCAN_BLE_TIMEOUT;
        if (isBleScanning()) {
            JL_Log.i(TAG, "startLeScan", "BLE is searching.");
            if (mBluetoothLeScanner != null) {
                mBluetoothLeScanner.flushPendingScanResults(mScanCallback);
            }
            setBleScanning(true, timeout);
            syncSystemBleDevice();
            return true;
        }
        boolean ret;
        if (mBluetoothLeScanner != null) {
            ScanSettings scanSettings;
            int scanMode = ScanSettings.SCAN_MODE_LOW_LATENCY; //修改搜索BLE模式 -- 均衡模式
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                ScanSettings.Builder builder = new ScanSettings.Builder()
                        .setScanMode(scanMode)
                        .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
                        .setNumOfMatches(ScanSettings.MATCH_NUM_MAX_ADVERTISEMENT);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    builder.setPhy(ScanSettings.PHY_LE_ALL_SUPPORTED);
                }
                scanSettings = builder.build();
            } else {
                scanSettings = new ScanSettings.Builder()
                        .setScanMode(scanMode)
                        .build();
            }
            List<ScanFilter> filters = new ArrayList<>();
            mBluetoothLeScanner.startScan(filters, scanSettings, mScanCallback);
            ret = true;
        } else {
            ret = mBluetoothAdapter.startLeScan(mLeScanCallback);
        }
        JL_Log.i(TAG, "startLeScan", CommonUtil.formatString("%s. timeout : %d.", ret, timeout));
        setBleScanning(ret, timeout);
        if (ret) {
            syncSystemBleDevice();
        }
        return ret;
    }

    @SuppressLint("MissingPermission")
    public boolean stopLeScan() {
        if (!checkScanEnv("stopLeScan")) return false;
        if (!isBleScanning()) return false;
        try {
            if (mBluetoothLeScanner != null) {
                mBluetoothLeScanner.stopScan(mScanCallback);
            } else {
                mBluetoothAdapter.stopLeScan(mLeScanCallback);
            }
        } catch (Exception ignore) {

        }
        removeTimeTask(MSG_SCAN_BLE_TIMEOUT);
        mHandler.removeMessages(MSG_SCAN_HID_DEVICE);
        setBleScanning(false, 0);
        return true;
    }

    public BluetoothDevice getConnectedBtDevice() {
        List<BluetoothDevice> connectedBleDevices = getConnectedDeviceList();
        for (BluetoothDevice device : connectedBleDevices) {
            final BleDevice bleDevice = getBleDevice(device);
            if (null == bleDevice) continue;
            if (bleDevice.isConnected() && bleDevice.isUsing()) {
                return device;
            }
        }
        return null;
    }

    public BluetoothGatt getConnectedBtGatt(BluetoothDevice device) {
        BleDevice bleDevice = getBleDevice(device);
        if (null == bleDevice) return null;
        return bleDevice.getGatt();
    }

    public BluetoothDevice getConnectedBLEDevice(String address) {
        if (!BluetoothAdapter.checkBluetoothAddress(address)) return null;
        List<BluetoothDevice> devices = getConnectedDeviceList();
        if (devices.isEmpty()) return null;
        for (BluetoothDevice device : devices) {
            if (device.getAddress().equals(address)) {
                return device;
            }
        }
        return null;
    }

    /**
     * 获取已连接设备列表
     * <p>
     * 按照连接时间倒序
     * </p>
     *
     * @return 设备列表
     */
    public List<BluetoothDevice> getConnectedDeviceList() {
        if (mBleDeviceMap.isEmpty()) return new ArrayList<>();
        List<BleDevice> bleDevices = getSortList();
        List<BluetoothDevice> devices = new ArrayList<>();
        for (BleDevice bleDevice : bleDevices) {
            if (null == bleDevice.getGatt() || bleDevice.getConnection() != BluetoothProfile.STATE_CONNECTED)
                continue;
            final BluetoothDevice device = bleDevice.getGatt().getDevice();
            if (null == device) continue;
            devices.add(device);
        }
        return devices;
    }

    public void reconnectDevice(String address, boolean isUseAdv) {
        JL_Log.d(TAG, "reconnectDevice", "address = " + address + ", isUseAdv = " + isUseAdv);
        boolean ret = mReConnectHelper.putParam(new ReConnectHelper.ReconnectParam(address, isUseAdv));
        JL_Log.d(TAG, "reconnectDevice", "" + ret);
    }

    public boolean isMatchReConnectDevice(String address, String matchAddress) {
        return mReConnectHelper.isMatchAddress(address, matchAddress);
    }

    public boolean isConnecting() {
        return getConnectingDevice() != null;
    }

    public boolean isConnectingDevice(BluetoothDevice device) {
        return BluetoothUtil.deviceEquals(getConnectingDevice(), device);
    }

    public boolean isConnectedDevice(BluetoothDevice device) {
        final BleDevice bleDevice = getBleDevice(device);
        if (null == bleDevice) return false;
        return bleDevice.isConnected() && bleDevice.getGatt() != null;
    }

    public boolean isConnectedGattOverBrEdrDevice(BluetoothDevice device) {
        final BleDevice bleDevice = getBleDevice(device);
        if (null == bleDevice) return false;
        return bleDevice.isConnectGattOverBrEdr() && bleDevice.isConnected();
    }

    public BluetoothDevice getConnectingDevice() {
        if (mBleDeviceMap.isEmpty()) return null;
        for (BleDevice bleDevice : mBleDeviceMap.values()) {
            if (bleDevice.getConnection() == BluetoothProfile.STATE_CONNECTING) {
                return bleDevice.getDevice();
            }
        }
        return null;
    }

    public int getBleMtu(BluetoothDevice device) {
        final BleDevice bleDevice = getBleDevice(device);
        if (null == bleDevice) return 0;
        return bleDevice.getMtu();
    }

    @SuppressLint("MissingPermission")
    public boolean connectBleDevice(BluetoothDevice device, @NonNull BleConnectParam param) {
        if (!checkConnectEnv("connectBleDevice", device) || !isBluetoothEnable())
            return false;
        if (isConnectedDevice(device)) {
            JL_Log.i(TAG, "connectBLEDevice", CommonUtil.formatString("Device(%s) is connected.", printDeviceInfo(device)));
            mCallbackManager.onBleConnection(device, BluetoothProfile.STATE_CONNECTED);
            return true;
        }

        final BluetoothDevice connectingDevice = getConnectingDevice();
        if (connectingDevice != null) {
            boolean isSameDevice = BluetoothUtil.deviceEquals(connectingDevice, device);
            JL_Log.e(TAG, "connectBleDevice", CommonUtil.formatString("Device(%s) is connecting, please wait.", printDeviceInfo(connectingDevice)));
            if (isSameDevice) return true;
            disconnectBleDevice(connectingDevice);
            SystemClock.sleep(300);
        }
        if (isBleScanning()) {
            stopLeScan();
        }
        //回调BLE连接中状态
        final BleDevice bleDevice = addBleDevice(device, param);
        startTimeoutTask(MSG_CONNECT_BLE_TIMEOUT, device, CONNECT_BLE_TIMEOUT);
        handleBleConnection(device, BluetoothProfile.STATE_CONNECTING);

        BluetoothGatt gatt = connectBluetoothGatt(device, param);
        boolean ret = gatt != null;
        bleDevice.setGatt(gatt);
        JL_Log.d(TAG, "connectBleDevice", ret ? CommonUtil.formatString("Prepare to connect to BLE(%s)", printDeviceInfo(device))
                : CommonUtil.formatString("Failed to connect to BLE(%s)", printDeviceInfo(device)));
        if (!ret) {
            handleBleConnection(device, BluetoothProfile.STATE_DISCONNECTED);
        }
        return ret;
    }


    public boolean connectBleDevice(BluetoothDevice device) {
        return connectBleDevice(device, new BleConnectParam()
                .setRequestMtu(configHelper.getBleRequestMtu())
                .setOtaUUID(BLE_UUID_SERVICE, BLE_UUID_WRITE, BLE_UUID_NOTIFICATION));
    }

    @SuppressLint("MissingPermission")
    public void disconnectBleDevice(BluetoothDevice device) {
        if (!checkConnectEnv("disconnectBleDevice", device)) return;
        BleDevice bleDevice = getBleDevice(device);
        JL_Log.d(TAG, "disconnectBleDevice", "device: " + printDeviceInfo(device) + ", " + bleDevice);
        if (null == bleDevice) return;
        final BluetoothGatt gatt = bleDevice.getGatt();
        if (!isBluetoothEnable()) {
            bleDevice.setConnection(BluetoothProfile.STATE_DISCONNECTED);
        }
        switch (bleDevice.getConnection()) {
            case BluetoothProfile.STATE_CONNECTED: {
                if (null != gatt) {
                    JL_Log.d(TAG, "disconnectBleDevice", "Gatt#disconnect");
                    startTimeoutTask(MSG_DISCONNECT_BLE_TIMEOUT, device, CALLBACK_TIMEOUT);
                    gatt.disconnect();
                }
                break;
            }
            case BluetoothProfile.STATE_CONNECTING: {
                if (null != gatt) {
                    gatt.disconnect();
                    closeGatt(gatt);
                }
                handleBleConnection(device, BluetoothProfile.STATE_DISCONNECTED);
                break;
            }
            default:
                removeConnectedBle(device);
                mCallbackManager.onBleConnection(device, BluetoothProfile.STATE_DISCONNECTED);
                break;
        }
    }

    public void writeDataByBleAsync(BluetoothDevice device, UUID serviceUUID, UUID characteristicUUID, byte[] data, OnWriteDataCallback callback) {
        addSendTask(device, serviceUUID, characteristicUUID, data, callback);
    }

    private void setBleScanning(boolean isScanning, long timeout) {
        isBleScanning = isScanning;
        if (isScanning) {
            mDiscoveredBleDevices.clear();
            startTimeoutTask(MSG_SCAN_BLE_TIMEOUT, null, timeout);
        }
        mCallbackManager.onDiscoveryBleChange(isScanning);
        if (isBleScanning && configHelper.isHidDevice()) {
            mHandler.sendEmptyMessage(MSG_SCAN_HID_DEVICE);
        }
    }

    private boolean checkScanEnv(String method) {
        if (null == mBluetoothAdapter) {
            JL_Log.w(TAG, method, "No support for Bluetooth function.");
            return false;
        }
        if (!CommonUtil.checkHasScanPermission(mContext)) {
            JL_Log.w(TAG, method, "Missing permission to search Bluetooth.");
            return false;
        }
        if (!isBluetoothEnable()) {
            JL_Log.w(TAG, method, "Bluetooth is close.");
            return false;
        }
        return true;
    }

    private boolean checkConnectEnv(String method, BluetoothDevice device) {
        if (!CommonUtil.checkHasConnectPermission(mContext)) {
            JL_Log.w(TAG, method, "Missing permission to search Bluetooth.");
            return false;
        }
        if (null == device) {
            JL_Log.w(TAG, method, "Device is null.");
            return false;
        }
        /*if (!isBluetoothEnable()) {
            JL_Log.w(TAG, method, "Bluetooth is close.");
            return false;
        }*/
        return true;
    }

    private BleDevice getBleDevice(BluetoothDevice device) {
        if (null == device) return null;
        return mBleDeviceMap.get(device.getAddress());
    }

    private BleDevice addBleDevice(@NonNull BluetoothDevice device, @NonNull BleConnectParam param) {
        BleDevice cacheDevice = getBleDevice(device);
        if (null == cacheDevice) {
            cacheDevice = new BleDevice(device, param);
            mBleDeviceMap.put(device.getAddress(), cacheDevice);
        }
        return cacheDevice;
    }

    private BleDevice removeConnectedBle(BluetoothDevice device) {
        if (null == device) return null;
        return removeConnectedBle(device.getAddress());
    }

    private BleDevice removeConnectedBle(String address) {
        if (!BluetoothAdapter.checkBluetoothAddress(address)) return null;
        BleDevice bleDevice = mBleDeviceMap.remove(address);
        JL_Log.i(TAG, "removeConnectedBle", "address : " + address + ", " + bleDevice);
        if (null == bleDevice) return null;
        final List<BluetoothDevice> connectedDeviceList = getConnectedDeviceList();
        JL_Log.d(TAG, "removeConnectedBle", "connectedDeviceList size : " + connectedDeviceList.size());
        if (connectedDeviceList.isEmpty()) {
            setConnectedBtDevice(null);
        } else if (BluetoothUtil.deviceEquals(bleDevice.getDevice(), getConnectedBtDevice())) {
            setConnectedBtDevice(connectedDeviceList.get(0));
        }
        return bleDevice;
    }

    @SuppressLint("MissingPermission")
    private BluetoothGatt connectBluetoothGatt(@NonNull BluetoothDevice device, @NonNull BleConnectParam param) {
        BluetoothGatt bluetoothGatt;
        int transport = param.getTransport();
        int phyMask = 1;
        JL_Log.d(TAG, "connectBluetoothGatt", "device : " + device + ", " + param);
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O) {
            bluetoothGatt = device.connectGatt(mContext, false, mBluetoothGattCallback,
                    transport, phyMask, mHandler);
        } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.O) {
            bluetoothGatt = device.connectGatt(mContext, false, mBluetoothGattCallback, transport, phyMask);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            bluetoothGatt = device.connectGatt(mContext, false, mBluetoothGattCallback, transport);
        } else {
            bluetoothGatt = device.connectGatt(mContext, false, mBluetoothGattCallback);
        }

        if (null == bluetoothGatt) {
            JL_Log.i(TAG, "connectBluetoothGatt", "Failed to connect ble.");
            return null;
        }
        return bluetoothGatt;
    }

    @NonNull
    private List<BleDevice> getSortList() {
        if (mBleDeviceMap.isEmpty()) return new ArrayList<>();
        List<BleDevice> bleDevices = new ArrayList<>(mBleDeviceMap.values());
        if (bleDevices.size() > 1) {
            Collections.sort(bleDevices, (o1, o2) -> {
                if (null == o1 && null == o2) return 0;
                if (null == o1) return 1;
                if (null == o2) return -1;
                return Long.compare(o2.getConnectedTime(), o1.getConnectedTime());
            });
        }
        return bleDevices;
    }

    @SuppressLint("MissingPermission")
    private void clearConnectedBleDevices() {
        if (!mBleDeviceMap.isEmpty()) {
            Map<String, BleDevice> clone = new HashMap<>(mBleDeviceMap);
            for (String key : clone.keySet()) {
                BleDevice bleDevice = clone.get(key);
                if (null == bleDevice) continue;
                final BluetoothGatt gatt = bleDevice.getGatt();
                if (null == gatt) continue;
                if (BluetoothUtil.isBluetoothEnable()) {
                    gatt.disconnect();
                    closeGatt(gatt);
                }
                mCallbackManager.onBleConnection(bleDevice.getDevice(), BluetoothProfile.STATE_DISCONNECTED);
            }
            mBleDeviceMap.clear();
        }
    }

    @SuppressLint("MissingPermission")
    private void closeGatt(BluetoothGatt gatt) {
        if (null == gatt) return;
        JL_Log.i(TAG, "closeGatt", "gatt : " + gatt);
        //AppUtil.refreshBleDeviceCache(mContext, gatt); //强制更新缓存
        gatt.close();
    }

    private void setConnectedBtDevice(BluetoothDevice device) {
        final BluetoothDevice connectedDevice = getConnectedBtDevice();
        if (null != device && !BluetoothUtil.deviceEquals(connectedDevice, device)) {
            final BleDevice oldDevice = getBleDevice(connectedDevice);
            if (oldDevice != null) {
                oldDevice.setUsing(false);
            }
            final BleDevice bleDevice = getBleDevice(device);
            boolean isSwitch = false;
            if (bleDevice != null) {
                bleDevice.setUsing(true);
                isSwitch = true;
            }
            if (isSwitch) {
                mCallbackManager.onSwitchBleDevice(device);
            }
        }
    }

    @SuppressLint("MissingPermission")
    private void filterDevice(BluetoothDevice device, int rssi, byte[] scanRecord,
                              boolean isBleEnableConnect) {
        if (AppUtil.checkHasConnectPermission(mContext) && isBluetoothEnable() && !TextUtils.isEmpty(device.getName())) {
//            JL_Log.d(TAG, "notify device : " + printDeviceInfo(device));
            handleDiscoveryBle(device, new BleScanInfo().setRawData(scanRecord).setRssi(rssi).setEnableConnect(isBleEnableConnect));
        }
    }

    private void startTimeoutTask(int what, BluetoothDevice device, long timeout) {
        removeTimeTask(what);
        if (null == device) {
            mHandler.sendEmptyMessageDelayed(what, timeout);
            return;
        }
        mHandler.sendMessageDelayed(mHandler.obtainMessage(what, device), timeout);
    }

    private void removeTimeTask(int what) {
        mHandler.removeMessages(what);
    }

    @SuppressLint("MissingPermission")
    private void syncSystemBleDevice() {
        stopCheckSystemBleThread();
        startTimeoutTask(MSG_CHECK_SYSTEM_BLE_TIMEOUT, null, CALLBACK_TIMEOUT);
        checkSystemBleThread = new CheckSystemBleThread();
        checkSystemBleThread.start();
    }

    private void stopCheckSystemBleThread() {
        removeTimeTask(MSG_CHECK_SYSTEM_BLE_TIMEOUT);
        if (null != checkSystemBleThread) {
            checkSystemBleThread.interrupt();
            checkSystemBleThread = null;
        }
    }

    private void findHidDevice() {
        List<BluetoothDevice> lists = BluetoothUtil.getSystemConnectedBtDeviceList(mContext);
        if (null != lists && AppUtil.checkHasConnectPermission(mContext)) {
            for (BluetoothDevice device : lists) {
                if (isConnectedDevice(device)) continue;
                if (device.getType() != BluetoothDevice.DEVICE_TYPE_CLASSIC &&
                        device.getBondState() == BluetoothDevice.BOND_BONDED) {
                    handleDiscoveryBle(device, new BleScanInfo().setEnableConnect(true));
                }
            }
        }
        if (isBleScanning()) {
            mHandler.sendEmptyMessageDelayed(MSG_SCAN_HID_DEVICE, 1000);
        }
    }

    private void addSendTask(BluetoothDevice device, UUID serviceUUID, UUID characteristicUUID,
                             byte[] data, OnWriteDataCallback callback) {
        boolean ret = false;
        BleDevice bleDevice = getBleDevice(device);
        if (bleDevice != null) {
            ret = bleDevice.addSendTask(serviceUUID, characteristicUUID, data, callback);
        }
        if (!ret && callback != null) {
            callback.onBleResult(device, serviceUUID, characteristicUUID, false, data);
        }
    }

    private void wakeupSendThread(BluetoothGatt gatt, UUID serviceUUID, UUID characteristicUUID,
                                  int status, byte[] data) {
        final BleDevice bleDevice = getBleDevice(gatt.getDevice());
        if (bleDevice != null) {
            BleSendTask task = new BleSendTask(gatt, serviceUUID, characteristicUUID, data, null);
            task.setStatus(status);
            bleDevice.wakeupSendThread(task);
        }
    }

    private void handleDiscoveryBle(BluetoothDevice device, BleScanInfo bleScanInfo) {
        if (!mDiscoveredBleDevices.contains(device)) {
            mDiscoveredBleDevices.add(device);
            JL_Log.d(TAG, "handleDiscoveryBle", "onDiscoveryBle " + printDeviceInfo(device) + ", " + bleScanInfo);
            mCallbackManager.onDiscoveryBle(device, bleScanInfo);
        }
    }

    private void handleBleConnection(BluetoothDevice device, int status) {
        final BleDevice bleDevice = getBleDevice(device);
        if (null == bleDevice) return;
        int prevState = bleDevice.getConnection();
        JL_Log.i(TAG, "handleBleConnection", CommonUtil.formatString("device : " + printDeviceInfo(device)
                + ", prevState : " + BluetoothUtil.printBtConnectionState(prevState)
                + ", status : " + BluetoothUtil.printBtConnectionState(status)));
        if (prevState == status) return; //相同连接状态
        bleDevice.setConnection(status);
        if (status != BluetoothProfile.STATE_CONNECTING) {
            removeTimeTask(MSG_NOTIFY_BLE_TIMEOUT);
        }
        switch (status) {
            case BluetoothProfile.STATE_CONNECTED: {
                if (getConnectedBtDevice() == null) {
                    setConnectedBtDevice(device);
                }
                break;
            }
            case BluetoothProfile.STATE_DISCONNECTED: {
                removeConnectedBle(device);
                break;
            }
        }
        mCallbackManager.onBleConnection(device, status);
    }

    /* ---- BroadcastReceiver Handler ---- */
    private void registerReceiver() {
        if (mAdapterReceiver == null) {
            mAdapterReceiver = new BaseBtAdapterReceiver();
            IntentFilter intentFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
            intentFilter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
            intentFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
            mContext.registerReceiver(mAdapterReceiver, intentFilter);
        }
    }

    private void unregisterReceiver() {
        if (mAdapterReceiver != null) {
            mContext.unregisterReceiver(mAdapterReceiver);
            mAdapterReceiver = null;
        }
    }

    /**
     * 用于开启蓝牙BLE设备Notification服务
     *
     * @param gatt               被连接的ble Gatt服务对象
     * @param serviceUUID        服务UUID
     * @param characteristicUUID characteristic UUID
     * @return 结果 true 则等待系统回调BLE服务
     */
    @SuppressLint("MissingPermission")
    private boolean enableBLEDeviceNotification(BluetoothGatt gatt, UUID serviceUUID, UUID
            characteristicUUID) {
        if (null == gatt || !AppUtil.checkHasConnectPermission(mContext)) {
            JL_Log.w(TAG, "enableBLEDeviceNotification", "Missing permission to connect Bluetooth.");
            return false;
        }
        if (!isBluetoothEnable()) {
            JL_Log.w(TAG, "enableBLEDeviceNotification", "Bluetooth is close.");
            return false;
        }
        BluetoothGattService gattService = gatt.getService(serviceUUID);
        if (null == gattService) {
            JL_Log.w(TAG, "enableBLEDeviceNotification", "BluetoothGattService is null. uuid = " + serviceUUID);
            return false;
        }
        BluetoothGattCharacteristic characteristic = gattService.getCharacteristic(characteristicUUID);
        if (null == characteristic) {
            JL_Log.w(TAG, "enableBLEDeviceNotification", "BluetoothGattCharacteristic is null. uuid = " + characteristicUUID);
            return false;
        }
        boolean bRet = gatt.setCharacteristicNotification(characteristic, true);
        if (bRet) {
            bRet = false; //重置标识
            List<BluetoothGattDescriptor> descriptors = characteristic.getDescriptors();
            for (BluetoothGattDescriptor descriptor : descriptors) {
                if (!BLE_UUID_NOTIFICATION_DESCRIPTOR.equals(descriptor.getUuid()))
                    continue; //跳过不相关描述符
                bRet = tryToWriteDescriptor(gatt, descriptor, 0, false);
                if (!bRet) {
                    JL_Log.w(TAG, "enableBLEDeviceNotification", "(tryToWriteDescriptor) ---> failed");
                } else { //正常只有一个描述符，使能即可
                    break;
                }
            }
        } else {
            JL_Log.w(TAG, "enableBLEDeviceNotification", "(setCharacteristicNotification) ---> failed.");
        }
        JL_Log.d(TAG, "enableBLEDeviceNotification", bRet + ", serviceUUID : " + UuidUtil.read16BitUUID(serviceUUID)
                + ", characteristicUUID : " + UuidUtil.read16BitUUID(characteristicUUID));
        return bRet;
    }

    @SuppressLint("MissingPermission")
    private boolean tryToWriteDescriptor(BluetoothGatt bluetoothGatt, BluetoothGattDescriptor
            descriptor, int retryCount, boolean isSkipSetValue) {
        if (!AppUtil.checkHasConnectPermission(mContext)) return false;
        boolean ret = isSkipSetValue;
        if (!ret) {
            ret = descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            JL_Log.i(TAG, "tryToWriteDescriptor", "setValue : " + ret);
            if (!ret) {
                retryCount++;
                if (retryCount >= 3) {
                    return false;
                } else {
                    JL_Log.i(TAG, "tryToWriteDescriptor", "setValue failed. retryCount : " + retryCount);
                    SystemClock.sleep(50);
                    tryToWriteDescriptor(bluetoothGatt, descriptor, retryCount, false);
                }
            } else {
                retryCount = 0;
            }
        }
        if (ret) {
            ret = bluetoothGatt.writeDescriptor(descriptor);
            JL_Log.i(TAG, "tryToWriteDescriptor", "writeDescriptor : " + ret);
            if (!ret) {
                retryCount++;
                if (retryCount >= 3) {
                    return false;
                } else {
                    JL_Log.i(TAG, "tryToWriteDescriptor", "writeDescriptor failed. retryCount : " + retryCount);
                    SystemClock.sleep(50);
                    tryToWriteDescriptor(bluetoothGatt, descriptor, retryCount, true);
                }
            }
        }
        return ret;
    }


    //开始调整BLE协议MTU
    @SuppressLint("MissingPermission")
    private void startChangeMtu(BluetoothGatt gatt, int mtu) {
        if (gatt == null || !AppUtil.checkHasConnectPermission(mContext)) {
            JL_Log.w(TAG, "startChangeMtu", "Gatt is null.");
            return;
        }
        if (mHandler.hasMessages(MSG_CHANGE_BLE_MTU_TIMEOUT)) {
            JL_Log.w(TAG, "startChangeMtu", "Adjusting the MTU for BLE");
            return;
        }
        final BluetoothDevice device = gatt.getDevice();
        if (device == null) {
            JL_Log.w(TAG, "startChangeMtu", "device is null.");
            return;
        }
        boolean ret = false;
        if (mtu > BluetoothConstant.BLE_MTU_MIN) {
            //开始超时任务
            startTimeoutTask(MSG_CHANGE_BLE_MTU_TIMEOUT, device, CALLBACK_TIMEOUT);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                ret = gatt.requestMtu(mtu + 3);
            }
        }
        JL_Log.d(TAG, "startChangeMtu", "requestMtu : " + ret + ", mtu : " + mtu);
        if (!ret) {
            stopChangeMtu();
            handleBleConnection(device, BluetoothProfile.STATE_CONNECTED);
        }
    }

    //回收调整MTU的超时任务
    private void stopChangeMtu() {
        removeTimeTask(MSG_CHANGE_BLE_MTU_TIMEOUT);
    }

    private String printDeviceInfo(BluetoothDevice device) {
        return BluetoothUtil.printBtDeviceInfo(mContext, device);
    }

    private final BluetoothAdapter.LeScanCallback mLeScanCallback = (device, rssi, scanRecord) ->
            filterDevice(device, rssi, scanRecord, true);

    private final ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            if (result != null && result.getScanRecord() != null) {
                BluetoothDevice device = result.getDevice();
                boolean isBleEnableConnect = true;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    isBleEnableConnect = result.isConnectable();
                }
//                JL_Log.d("onScanResult", printDeviceInfo(device));
                filterDevice(device, result.getRssi(), result.getScanRecord().getBytes(), isBleEnableConnect);
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            JL_Log.w(TAG, "onScanFailed", "code : " + errorCode);
            if (errorCode == ScanCallback.SCAN_FAILED_ALREADY_STARTED) return;
            stopLeScan();
        }
    };

    private final BluetoothGattCallback mBluetoothGattCallback = new BluetoothGattCallback() {

        public void onConnectionUpdated(BluetoothGatt gatt, int interval, int latency, int timeout, int status) {
            if (null == gatt || !AppUtil.checkHasConnectPermission(mContext)) return;
            BluetoothDevice device = gatt.getDevice();
            if (null == device) return;
            JL_Log.e(TAG, "onConnectionUpdated", "device : " + printDeviceInfo(device) + ", interval : "
                    + interval + ", latency : " + latency + ", timeout : " + timeout + ", status : " + status);
            mCallbackManager.onConnectionUpdated(device, interval, latency, timeout, status);
        }

        @SuppressLint("MissingPermission")
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (null == gatt) return;
            final BluetoothDevice device = gatt.getDevice();
            if (null == device) return;
            final BleDevice bleDevice = getBleDevice(device);
            if (null == bleDevice) return;
            JL_Log.i(TAG, "onConnectionStateChange", CommonUtil.formatString("device : %s, status = %d, newState = %s.\n%s",
                    printDeviceInfo(device), status, BluetoothUtil.printBtConnectionState(newState), bleDevice));
            if (newState == BluetoothProfile.STATE_DISCONNECTED || newState == BluetoothProfile.STATE_DISCONNECTING
                    || newState == BluetoothProfile.STATE_CONNECTED) {
                removeTimeTask(MSG_CONNECT_BLE_TIMEOUT);
                if (newState == BluetoothProfile.STATE_CONNECTED) {  //BLE连接成功
                    bleDevice.setGatt(gatt);
                    startTimeoutTask(MSG_DISCOVER_SERVICES_TIMEOUT, device, CALLBACK_TIMEOUT);
                    boolean ret = gatt.discoverServices();
                    JL_Log.d(TAG, "onConnectionStateChange", "discoverServices : " + ret);
                    if (!ret) {
                        disconnectBleDevice(device);
                    }
                    return;
                } else {
                    removeTimeTask(MSG_DISCOVER_SERVICES_TIMEOUT);
                    removeTimeTask(MSG_DISCONNECT_BLE_TIMEOUT);
                    if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        closeGatt(gatt);

                        if (!bleDevice.isConnected()) { //非连接成功过程才触发重连机制
                            long usedConnectTime = System.currentTimeMillis() - bleDevice.getConnectedTime(); //连接已耗费时间
                            boolean isNeedReconnect = bleDevice.isNeedReconnect() || (bleDevice.getParam().isNeedReconnect()
                                    && status != 19 && usedConnectTime > 0 && usedConnectTime < MIN_CONNECT_TIME);
                            JL_Log.d(TAG, "onConnectionStateChange", "usedConnectTime = " + usedConnectTime
                                    + ", limit time = " + MIN_CONNECT_TIME + ", isNeedReconnect : " + isNeedReconnect);
                            if (isNeedReconnect) { //遇到了异常断开情况, 尝试重连设备
                                if (!bleDevice.isOverReconnectLimit()) {
                                    JL_Log.i(TAG, "onConnectionStateChange", "Ready to reconnect device.");
                                    bleDevice.setConnection(BluetoothProfile.STATE_DISCONNECTED)
                                            .setMtu(BluetoothConstant.BLE_MTU_MIN);
                                    SystemClock.sleep(1000);
                                    if (!connectBleDevice(device, bleDevice.getParam())) {
                                        bleDevice.setConnection(BluetoothProfile.STATE_CONNECTING);
                                        handleBleConnection(device, BluetoothProfile.STATE_DISCONNECTED);
                                    }
                                    return;
                                }
                            }
                        }
                    }
                }
            }
            handleBleConnection(device, newState);
        }

        @SuppressLint("MissingPermission")
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (null == gatt) return;
            BluetoothDevice device = gatt.getDevice();
            if (null == device) return;
            final BleDevice bleDevice = getBleDevice(device);
            if (null == bleDevice) return;
            removeTimeTask(MSG_DISCOVER_SERVICES_TIMEOUT);
            mCallbackManager.onBleServiceDiscovery(device, status, gatt.getServices());
            boolean ret = false;
            if (status == BluetoothGatt.GATT_SUCCESS) {
                AppUtil.printBleGattServices(mContext, device, gatt, status);
                UUID serviceUUID = bleDevice.getParam().getOtaServiceUUID();
                UUID writeUUID = bleDevice.getParam().getOtaWriteCharacteristicUUID();
                UUID notifyUUID = bleDevice.getParam().getOtaNotifyCharacteristicUUID();
                for (BluetoothGattService service : gatt.getServices()) {
                    if (UuidUtil.equalUUID(serviceUUID, service.getUuid()) && null != service.getCharacteristic(writeUUID)
                            && null != service.getCharacteristic(notifyUUID)) {
                        JL_Log.i(TAG, "onServicesDiscovered", "start NotifyCharacteristicRunnable...");
                        mNotifyCharacteristicRunnable = new NotifyCharacteristicRunnable(gatt, serviceUUID, notifyUUID);
                        mHandler.post(mNotifyCharacteristicRunnable);
                        ret = true;
                        break;
                    }
                }
            }
            JL_Log.i(TAG, "onServicesDiscovered", "" + ret);
            if (!ret) {
                disconnectBleDevice(device);
            }
        }

        @SuppressLint("MissingPermission")
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            if (null == gatt) return;
            BluetoothDevice device = gatt.getDevice();
            if (null == device || null == characteristic) return;
            UUID serviceUUID = null;
            UUID characteristicUUID = characteristic.getUuid();
            byte[] data = characteristic.getValue();
            BluetoothGattService gattService = characteristic.getService();
            if (gattService != null) {
                serviceUUID = gattService.getUuid();
            }
            JL_Log.d(TAG, CommonUtil.formatString("[onCharacteristicChanged] <<< deice : %s, serviceUuid = %s, characteristicUuid = %s, \ndata : [%s]",
                    device, UuidUtil.read16BitUUID(serviceUUID), UuidUtil.read16BitUUID(characteristicUUID),
                    CHexConver.byte2HexStr(data)));
            mCallbackManager.onBleDataNotification(device, serviceUUID, characteristicUUID, data);
        }

        @SuppressLint("MissingPermission")
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (null == gatt || null == gatt.getDevice() || null == characteristic) return;
            BluetoothDevice device = gatt.getDevice();
            UUID serviceUUID = null;
            UUID characteristicUUID = characteristic.getUuid();
            BluetoothGattService gattService = characteristic.getService();
            if (gattService != null) serviceUUID = gattService.getUuid();
            byte[] data = characteristic.getValue();
            JL_Log.d(TAG, CommonUtil.formatString("[onCharacteristicWrite] >>> device : %s, serviceUuid = %s," +
                            " characteristicUuid = %s, status = %s, \ndata : [%s]", device, UuidUtil.read16BitUUID(serviceUUID),
                    UuidUtil.read16BitUUID(characteristicUUID), CommonUtil.formatInt(status), CHexConver.byte2HexStr(data)));
            wakeupSendThread(gatt, serviceUUID, characteristicUUID, status, data);
            mCallbackManager.onBleWriteStatus(device, serviceUUID, characteristicUUID, data, status);
        }

        @SuppressLint("MissingPermission")
        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            if (null == gatt) return;
            BluetoothDevice device = gatt.getDevice();
            if (null == device || null == descriptor) return;
            UUID serviceUuid = null;
            UUID characteristicUuid = null;
            UUID descriptorUuid = descriptor.getUuid();
            BluetoothGattCharacteristic characteristic = descriptor.getCharacteristic();
            if (null != characteristic) {
                characteristicUuid = characteristic.getUuid();
                BluetoothGattService bluetoothGattService = characteristic.getService();
                if (null != bluetoothGattService) {
                    serviceUuid = bluetoothGattService.getUuid();
                }
            }
            JL_Log.i(TAG, "onDescriptorWrite", CommonUtil.formatString("device : %s, serviceUuid = %s, characteristicUuid = %s," +
                            " descriptorUuid = %s, status = %d", device, UuidUtil.read16BitUUID(serviceUuid), UuidUtil.read16BitUUID(characteristicUuid),
                    UuidUtil.read16BitUUID(descriptorUuid), status));
            mCallbackManager.onBleNotificationStatus(device, serviceUuid, characteristicUuid, status);
            if (mNotifyCharacteristicRunnable != null && mNotifyCharacteristicRunnable.isSame(device, serviceUuid, characteristicUuid, descriptorUuid)) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    mNotifyCharacteristicRunnable = null;
                    BleDevice bleDevice = getBleDevice(device);
                    if (null == bleDevice) return;
                    int requestMTU = bleDevice.getParam().getRequestMtu();
                    if (requestMTU > 509) {
                        requestMTU = 509;
                    }
                    if (bleDevice.isConnectGattOverBrEdr()) { //Gatt Over BR/EDR服务不能调节MTU，默认为请求MTU值
                        bleDevice.setMtu(requestMTU);
                        mCallbackManager.onBleDataBlockChanged(device, requestMTU + 3, status);
                        handleBleConnection(device, BluetoothProfile.STATE_CONNECTED);
                        return;
                    }
                    startChangeMtu(gatt, requestMTU);
                    return;
                }
                int num = mNotifyCharacteristicRunnable.getRetryNum();
                if (num < 3) {
                    mNotifyCharacteristicRunnable.setRetryNum(++num);
                    mHandler.postDelayed(mNotifyCharacteristicRunnable, 100);
                } else {
                    mNotifyCharacteristicRunnable = null;
                    disconnectBleDevice(device);
                }
            }
        }

        @SuppressLint("MissingPermission")
        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            if (null == gatt) return;
            BluetoothDevice device = gatt.getDevice();
            if (null == device) return;
            JL_Log.d(TAG, "onMtuChanged", CommonUtil.formatString("device : %s, mtu = %d, status = %d", printDeviceInfo(device), mtu, status));
            mCallbackManager.onBleDataBlockChanged(device, mtu, status);
            final BleDevice bleDevice = getBleDevice(device);
            if (null == bleDevice) return;
            if (mHandler.hasMessages(MSG_CHANGE_BLE_MTU_TIMEOUT)) { //调整MTU的回调
                // 需要减去3个字节的数据包头部信息
                int bleMtu = status == BluetoothGatt.GATT_SUCCESS ? mtu - 3 : BluetoothConstant.BLE_MTU_MIN;
                stopChangeMtu();
                bleDevice.setMtu(bleMtu);
                JL_Log.i(TAG, "onMtuChanged", "MTU modified successfully");
                handleBleConnection(device, BluetoothProfile.STATE_CONNECTED);
            }
        }
    };

    private class BaseBtAdapterReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null) return;
            String action = intent.getAction();
            if (action == null) return;
            switch (action) {
                case BluetoothAdapter.ACTION_STATE_CHANGED: {
                    int state = intent.getIntExtra(BluetoothAdapter.EXTRA_CONNECTION_STATE, -1);
                    int prevState = intent.getIntExtra(BluetoothAdapter.EXTRA_PREVIOUS_STATE, -1);
                    if (mBluetoothAdapter != null && state == -1) {
                        state = mBluetoothAdapter.getState();
                    }
                    if (state == prevState) return;
                    if (state == BluetoothAdapter.STATE_OFF) {
                        setBleScanning(false, 0);
                        mDiscoveredBleDevices.clear();
                        clearConnectedBleDevices();
                        mCallbackManager.onAdapterChange(false);
                    } else if (state == BluetoothAdapter.STATE_ON) {
                        mCallbackManager.onAdapterChange(true);
                    }
                    break;
                }
                case BluetoothDevice.ACTION_ACL_CONNECTED: {
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    JL_Log.i(TAG, "ACTION_ACL_CONNECTED", "device : " + printDeviceInfo(device));
                    break;
                }
                case BluetoothDevice.ACTION_ACL_DISCONNECTED: {
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    JL_Log.i(TAG, "ACTION_ACL_DISCONNECTED", "device : " + printDeviceInfo(device));
                    break;
                }

            }
        }
    }

    private class NotifyCharacteristicRunnable implements Runnable {
        private final BluetoothGatt bluetoothGatt;
        private final UUID serviceUUID;
        private final UUID characteristicUUID;
        public final UUID descriptorUUID = BLE_UUID_NOTIFICATION_DESCRIPTOR;
        private int retryNum = 0;

        private NotifyCharacteristicRunnable(BluetoothGatt gatt, UUID serviceUUID, UUID characteristicUUID) {
            this.bluetoothGatt = gatt;
            this.serviceUUID = serviceUUID;
            this.characteristicUUID = characteristicUUID;
        }

        private void setRetryNum(int retryNum) {
            this.retryNum = retryNum;
        }

        private int getRetryNum() {
            return retryNum;
        }

        public boolean isSame(BluetoothDevice device, UUID serviceUUID, UUID characteristicUUID, UUID descriptorUUID) {
            return BluetoothUtil.deviceEquals(getBleDevice(), device) &&
                    UuidUtil.equalUUID(this.serviceUUID, serviceUUID) &&
                    UuidUtil.equalUUID(this.characteristicUUID, characteristicUUID) &&
                    UuidUtil.equalUUID(this.descriptorUUID, descriptorUUID);
        }

        public BluetoothDevice getBleDevice() {
            if (bluetoothGatt == null) return null;
            return bluetoothGatt.getDevice();
        }

        @Override
        public void run() {
            final BluetoothDevice device = getBleDevice();
            if (null == device) return;
            startTimeoutTask(MSG_NOTIFY_BLE_TIMEOUT, device, CALLBACK_TIMEOUT);
            boolean ret = enableBLEDeviceNotification(bluetoothGatt, serviceUUID, characteristicUUID);
            JL_Log.i(TAG, "enableBLEDeviceNotification", CommonUtil.formatString("%s, service uuid = %s, characteristic uuid = %s",
                    ret, UuidUtil.read16BitUUID(serviceUUID), UuidUtil.read16BitUUID(characteristicUUID)));
            if (!ret) {
                removeTimeTask(MSG_NOTIFY_BLE_TIMEOUT);
                disconnectBleDevice(device);
            }
        }
    }

    private class CheckSystemBleThread extends Thread {

        @Override
        public void run() {
            if (!AppUtil.checkHasConnectPermission(mContext)) return;
            final BluetoothManager btManager = (BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);
            if (btManager == null) return;
            try {
                List<BluetoothDevice> sysConnectedBleList = btManager.getConnectedDevices(BluetoothProfile.GATT);
                if (null == sysConnectedBleList) return;
                for (BluetoothDevice bleDev : sysConnectedBleList) {
                    if (isConnectedDevice(bleDev)) continue;
                    handleDiscoveryBle(bleDev, new BleScanInfo().setEnableConnect(true));
                }
                removeTimeTask(MSG_CHECK_SYSTEM_BLE_TIMEOUT);
            } catch (Exception ignore) {

            }
        }
    }
}
