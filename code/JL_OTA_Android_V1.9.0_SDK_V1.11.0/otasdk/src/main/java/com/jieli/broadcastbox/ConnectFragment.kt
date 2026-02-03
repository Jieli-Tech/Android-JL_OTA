package com.jieli.broadcastbox

import android.bluetooth.BluetoothProfile
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener
import com.jieli.broadcastbox.adapter.DeviceAdapter
import com.jieli.broadcastbox.model.BroadcastBoxInfo
import com.jieli.broadcastbox.viewmodel.BroadcastBoxViewModel
import com.jieli.component.utils.ValueUtil
import com.jieli.jl_bt_ota.constant.JL_Constant
import com.jieli.jl_bt_ota.interfaces.IActionCallback
import com.jieli.jl_bt_ota.model.base.BaseError
import com.jieli.jl_bt_ota.util.JL_Log
import com.jieli.jl_bt_ota.util.ParseDataUtil
import com.jieli.jl_dialog.Jl_Dialog
import com.jieli.otasdk.R
import com.jieli.otasdk.databinding.FragmentConnectBinding
import com.jieli.otasdk.ui.device.BaseBluetoothFragment
import com.jieli.otasdk.data.model.device.DeviceConnection
import com.jieli.otasdk.data.model.device.ScanDevice
import com.jieli.otasdk.data.model.ScanResult
import com.jieli.otasdk.ui.widget.SpecialDecoration
import com.jieli.otasdk.util.DeviceUtil
import com.jieli.otasdk.util.PermissionUtil
import com.jieli.otasdk.util.hide
import com.jieli.otasdk.util.show
import java.nio.ByteBuffer
import java.nio.ByteOrder

class ConnectFragment : BaseBluetoothFragment() {
    private lateinit var binding: FragmentConnectBinding
    private lateinit var connectViewModel: BroadcastBoxViewModel
    private lateinit var adapter: DeviceAdapter
    private val handler = Handler(Looper.getMainLooper())
    private var mNotifyDialog: Jl_Dialog? = null
    private var isRefreshing = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentConnectBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        connectViewModel = ViewModelProvider(requireActivity())[BroadcastBoxViewModel::class.java]
        binding.srlSwipeRefresh.setColorSchemeColors(
            ContextCompat.getColor(requireContext(), android.R.color.black),
            ContextCompat.getColor(requireContext(), android.R.color.darker_gray),
            ContextCompat.getColor(requireContext(), android.R.color.black),
            ContextCompat.getColor(requireContext(), android.R.color.background_light)
        )
        binding.srlSwipeRefresh.setProgressBackgroundColorSchemeColor(Color.WHITE)
        binding.srlSwipeRefresh.setSize(SwipeRefreshLayout.DEFAULT)
        binding.srlSwipeRefresh.setOnRefreshListener(onRefreshListener)
        binding.sbBleFilter.setOnCheckedChangeListener { _, isChecked ->
            connectViewModel.isFilterDevice = isChecked

            isRefreshing = true
            connectViewModel.stopScan()
            handler.postDelayed({
                isRefreshing = false
                tryToScan(true)
            }, 300)
        }
        binding.sbBleFilter.setCheckedNoEvent(connectViewModel.isFilterDevice)

        adapter = DeviceAdapter()
        binding.list.adapter = adapter
        binding.list.layoutManager = LinearLayoutManager(requireContext())
        binding.list.addItemDecoration(
            SpecialDecoration(
                requireContext(),
                LinearLayoutManager.VERTICAL,
                ContextCompat.getColor(requireContext(), R.color.rc_decoration),
                ValueUtil.dp2px(requireActivity(), 1)
            )
        )

        adapter.setOnItemClickListener { _, _, position ->
            val scanResult = adapter.getItem(position)
            connectViewModel.stopScan()
            if (connectViewModel.isConnectedDevice(scanResult.device)) { //设备已连接
                connectViewModel.disconnectBle(scanResult.device)
            } else { //设备未连接
                connectViewModel.connectBle(scanResult.device)
            }
        }
        JL_Log.d(TAG, "onViewCreated >>> ")
        setObserverListener()
    }

    override fun onResume() {
        super.onResume()
        tryToScan()
    }

    override fun onStop() {
        super.onStop()
        JL_Log.d(TAG, "onStop >>> ")
        connectViewModel.stopScan()
        dismissConnectionDialog()
    }

    override fun onDestroy() {
        super.onDestroy()
        JL_Log.d(TAG, "onDestroy >>> ")
        if (connectViewModel.isAddObserver) {
            connectViewModel.deviceConnectionMLD.removeObserver(btConnectionObserver)
            connectViewModel.isAddObserver = false
        }
        connectViewModel.destroy()
    }

    private val btConnectionObserver = Observer<DeviceConnection> { connection ->
        JL_Log.d(TAG, ">>>>>>>>> deviceConnectionMLD >> ${connection.state}")
        if (connection.state == BluetoothProfile.STATE_CONNECTING) {
            showConnectionDialog()
        } else {
            dismissConnectionDialog()
            updateConnectedDevices()
            if (connection.state == BluetoothProfile.STATE_DISCONNECTED && isVisible) {
                tryToScan()
            }
        }
    }

    private fun tryToScan(isUser: Boolean = false) {
        if (!isUser && (!PermissionUtil.hasBluetoothPermission(requireContext())
                    || !PermissionUtil.hasLocationPermission(requireContext()))
        ) {
            return
        }
        tryToCheckBluetoothEnvironment(isUser) {
            if (it) {
                binding.pbSearchingBt.show()
                JL_Log.e(TAG, "tryToScan >>> startScan")
                connectViewModel.startScan()
            } else {
                JL_Log.e(TAG, "tryToScan >>> onFailed")
                binding.pbSearchingBt.hide()
            }
        }
    }

    private fun setObserverListener() {
        // Bluetooth state observer
        connectViewModel.bluetoothStateMLD.observe(viewLifecycleOwner) { isOpen ->
            if (!isOpen!!) {
                binding.pbSearchingBt.visibility = View.INVISIBLE
                binding.tvScanStatus.text = getString(R.string.bt_bluetooth_close)
                adapter.setList(mutableListOf())
            } else {
                binding.tvScanStatus.text = getString(R.string.scan_tip)
                tryToScan()
            }
        }

        // Searching ble observer
        connectViewModel.scanResultMLD.observe(viewLifecycleOwner) { scanResult ->
            JL_Log.i(TAG, "connect onChanged=" + scanResult.state)
            when (scanResult.state) {
                ScanResult.SCAN_STATUS_IDLE -> { // 搜索结束
                    binding.pbSearchingBt.visibility = View.INVISIBLE
                }

                ScanResult.SCAN_STATUS_SCANNING -> { // 搜索开始
                    binding.pbSearchingBt.visibility = View.VISIBLE
                    adapter.apply {
                        data.clear()
                        connectViewModel.getConnectedDevices().forEach { device ->
                            val boxInfo = BroadcastBoxInfo(device, 0)
                            boxInfo.isConnected = true
                            val advInfo = connectViewModel.findCacheAdvMessage(device.address)
                            if (advInfo != null) {
                                boxInfo.uid = advInfo.uid
                                boxInfo.pid = boxInfo.pid
                            } else {
                                boxInfo.uid = 0
                                boxInfo.pid = 0
                            }
                            if (!connectViewModel.connectedBleDevices.contains(boxInfo)) {
                                connectViewModel.connectedBleDevices.add(boxInfo)
                            }
                            adapter.addDevice(boxInfo)
                        }
                    }
                }

                ScanResult.SCAN_STATUS_FOUND_DEV -> { // 发现设备
                    scanResult.device?.let {
                        //                    JL_Log.i(TAG, "Found:" + it.device.name)
                        //                    JL_Log.w(TAG, "data=" + (it.data?.get(0) ?: 0) + ", "
                        //                            + (CHexConver.byteToInt(it.data?.get(1)?:0)))
                        val boxInfo: BroadcastBoxInfo?
                        if (connectViewModel.isFilterDevice) {
                            boxInfo = filterBroadcastBox(it)
                        } else {
                            boxInfo = BroadcastBoxInfo(it.device, it.rssi)
                            boxInfo.uid = 0
                            boxInfo.pid = 0
                        }
                        if (boxInfo != null) {
                            connectViewModel.cacheAdvInfo[it.device.address] = boxInfo
                            adapter.addDevice(boxInfo)
                        }
                    }
                }
            }
        }

        // BLE connect state observer
        if (!connectViewModel.isAddObserver) {
            connectViewModel.deviceConnectionMLD.observeForever(btConnectionObserver)
            connectViewModel.isAddObserver = true
        }
    }

    private fun showConnectionDialog() {
        if (!isFragmentValid) {
            JL_Log.w(TAG, "showConnectionDialog >> not valid")
            return
        }
        if (mNotifyDialog == null) {
            mNotifyDialog = Jl_Dialog.builder()
                .title(getString(R.string.tips))
                .content(getString(R.string.bt_connecting))
                .showProgressBar(true)
                .width(0.8f)
                .cancel(false)
                .build()
        }

        mNotifyDialog?.let {
            if (!it.isShow) {
                it.show(childFragmentManager, "connecting_ble")
            }
        }
    }

    private fun dismissConnectionDialog() {
        if (!isFragmentValid) {
            JL_Log.w(TAG, "showConnectionDialog >> not valid")
            return
        }
        mNotifyDialog?.let {
            if (it.isShow && isFragmentValid) {
                it.dismiss()
            }
            mNotifyDialog = null
        }
    }

    private fun updateConnectedDevices() {
        val connectedDevices = connectViewModel.getConnectedDevices()
        JL_Log.e(TAG, "updateConnectedDevices >> ${connectedDevices.size}")
        connectViewModel.connectedBleDevices.clear()
        adapter.apply {
            data.forEach { info ->
                info.isConnected = connectViewModel.isConnectedDevice(info.device)
                JL_Log.d(TAG, "device = ${info.device}, isConnected = ${info.isConnected}")
                if (info.isConnected) {
                    info.rssi = 0
                    if (!connectViewModel.connectedBleDevices.contains(info)) {
                        connectViewModel.connectedBleDevices.add(info)
                    }
                }
            }
            data.sortWith { o1, o2 ->
                o2.rssi.compareTo(o1.rssi)
            }
            JL_Log.e(
                TAG,
                "updateConnectedDevices >> after...${connectViewModel.connectedBleDevices.size}"
            )
            notifyDataSetChanged()
        }
    }

    private val onRefreshListener = OnRefreshListener {
        if (!isRefreshing) {
            isRefreshing = true
            handler.postDelayed({
                isRefreshing = false
                //显示或隐藏刷新进度条
                binding.srlSwipeRefresh.isRefreshing = false
            }, 1000)
            tryToScan(true)
        }
    }

    private fun filterBroadcastBox(scanDevice: ScanDevice): BroadcastBoxInfo? {
        val bleScanMessage =
            ParseDataUtil.parseOTAFlagFilterWithBroad(scanDevice.data, JL_Constant.OTA_IDENTIFY)
        if (bleScanMessage != null && bleScanMessage.isOTA) {
            val name = DeviceUtil.getDeviceName(requireContext(), scanDevice.device)
            JL_Log.e(
                TAG, "Need to upgrade: uid=${bleScanMessage.uid}, pid=${bleScanMessage.pid}," +
                        " ble=$name, ble=${scanDevice.device.address}"
            )
            val vid = bleScanMessage.vid
            val uid = bleScanMessage.uid
            val pid = bleScanMessage.pid
            val type = bleScanMessage.deviceType
            JL_Log.i(
                TAG,
                "uid=$uid, pid=$pid, type=$type, ble=$name, ble=${scanDevice.device.address}"
            )
            if (type != 0) {
                JL_Log.e(TAG, "Not broadcast box type:${type}")
                return null
            }
            val boxInfo = BroadcastBoxInfo(scanDevice.device, scanDevice.rssi)
            boxInfo.pid = pid
            boxInfo.uid = uid
            boxInfo.isForceUpdate = true
            return boxInfo
        }

        if (scanDevice.data == null) {
            JL_Log.e(TAG, "scan device data is null")
            return null
        }
        if (scanDevice.data!!.size < 9) {
            JL_Log.e(TAG, "scan device data is < 9")
            return null
        }
        val totalLen = scanDevice.data?.get(0) ?: 0

        val payloadLen = totalLen - 1
        if (payloadLen < 8) {
//            JL_Log.e(TAG, "scan device data=${scanDevice.data!!.size}, payloadLen=$payloadLen")
            return null
        }
        //匹配厂商标识
        val otaScanRecord = ByteArray(totalLen - 1)
        scanDevice.data?.let { it1 ->
            System.arraycopy(it1, 2, otaScanRecord, 0, otaScanRecord.size)
            val buffer = ByteBuffer.wrap(otaScanRecord)
            buffer.order(ByteOrder.LITTLE_ENDIAN)
            val vid = buffer.short.toInt()
            val uid = buffer.short.toInt()
            val pid = buffer.short.toInt()
            val type = (buffer.get().toInt() shr 4) and 0xFF
//            JL_Log.i(TAG, "uid=$uid, pid=$pid, type=$type, ble=${scanDevice.device.name}, ble=${scanDevice.device.address}")
            if (type != 0) {
                JL_Log.e(TAG, "Not broadcast box type:${type}")
                return null
            }
            val boxInfo = BroadcastBoxInfo(scanDevice.device, scanDevice.rssi)
            boxInfo.pid = pid
            boxInfo.uid = uid
            return boxInfo
        }
        return null
    }
}
