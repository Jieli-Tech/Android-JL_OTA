package com.jieli.otasdk.fragments


import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.jieli.component.utils.ValueUtil
import com.jieli.jl_bt_ota.constant.StateCode
import com.jieli.jl_bt_ota.util.BluetoothUtil
import com.jieli.jl_dialog.Jl_Dialog
import com.jieli.otasdk.R
import com.jieli.otasdk.model.ScanDevice
import com.jieli.otasdk.model.ScanResult
import com.jieli.otasdk.tool.config.ConfigHelper
import com.jieli.otasdk.util.AppUtil
import com.jieli.otasdk.viewmodel.ConnectViewModel
import com.jieli.otasdk.widget.SpecialDecoration
import kotlinx.android.synthetic.main.fragment_scan.*

/**
 * 连接设备界面
 */
class ScanFragment : BaseBluetoothSanFragment() {

    private lateinit var viewModel: ConnectViewModel
    private lateinit var adapter: ScanDeviceAdapter
    private var mNotifyDialog: Jl_Dialog? = null
    private val mConfigHelper = ConfigHelper.getInstance()
    private val mScanDeviceList = ArrayList<ScanDevice>(1)
    private val mUIHandler: Handler = Handler(Looper.getMainLooper())

    private var isRefreshing: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_scan, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this).get(ConnectViewModel::class.java)
        init()
        observerCallback()
//        viewModel.startScan()
        startScanDev()
    }


    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        if (isVisibleToUser) {
            if (!viewModel.isScanning() && BluetoothUtil.isBluetoothEnable()) {
//                viewModel.startScan()
                startScanDev()
            }
        } else {
            viewModel.stopScan()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.destroy()
    }

    private fun init() {
        tv_tv_scan_content.setText(mConfigHelper.getScanFilter())
        adapter = ScanDeviceAdapter(viewModel, mutableListOf())
        adapter.let {
            it.setOnItemClickListener { _, _, position ->
                if (adapter.data.size <= position) return@setOnItemClickListener
                val scanDevice: ScanDevice = adapter.getItem(position)
//                viewModel.stopScan()
                if (viewModel.isDeviceConnected(scanDevice.device)) { //设备已连接
                    viewModel.disconnectBtDevice(scanDevice.device)
                } else { //设备未连接
                    if (viewModel.isConnected()) { //不允许多设备连接
                        return@setOnItemClickListener
                    }
                    viewModel.connectBtDevice(scanDevice.device)
                }
            }
        }
        rc_device_list.adapter = adapter
        rc_device_list.layoutManager = LinearLayoutManager(requireContext())
        rc_device_list.addItemDecoration(
            SpecialDecoration(
                requireContext(),
                LinearLayoutManager.VERTICAL,
                resources.getColor(R.color.rc_decoration),
                ValueUtil.dp2px(requireActivity(), 1)
            )
        )
        tv_scan_filter.setOnClickListener {
            val scanFilterDialog = DialogInputText().run {
                this.title = this@ScanFragment.getString(R.string.ble_filter)
                this.content = this@ScanFragment.tv_tv_scan_content.text.toString()
                this.leftText = this@ScanFragment.getString(R.string.cancel)
                this.rightText = this@ScanFragment.getString(R.string.confirm)
                this.dialogClickListener = object : DialogClickListener {
                    override fun rightBtnClick(inputText: String?) {
                        var filterStr: String = inputText ?: ""
                        filterStr = filterStr.trim()
                        this@ScanFragment.tv_tv_scan_content.setText(filterStr)
                        mConfigHelper.setScanFilter(filterStr)
                        val filterList = ArrayList<ScanDevice>(0)
                        viewModel.getConnectedDevice()?.let {
                            filterList.add(ScanDevice(it, 0))
                        }
                        mScanDeviceList.forEach {
                            val filter = filterStr
                            if (AppUtil.checkHasConnectPermission(requireContext())
                                && !TextUtils.isEmpty(it.device.name)
                                && (TextUtils.isEmpty(filter) || it.device.name!!.contains(
                                    filter,
                                    true
                                ))
                            ) {
                                filterList.add(it)
                            }
                        }
                        filterList.sortWith { o1, o2 ->
                            o2.rssi.compareTo(o1.rssi)
                        }.apply {
                        }
                        adapter.setNewInstance(filterList)
                        this@run.dismiss()
                    }

                    override fun leftBtnClick(inputText: String?) {
                        this@run.dismiss()
                    }
                }
                this
            }
            scanFilterDialog.show(parentFragmentManager, "scanFilterDialog")
        }
        srl_scan.setOnRefreshListener {
            if (!isRefreshing) {
//                adapter.data.clear()
//                adapter.notifyDataSetChanged()
//                viewModel.startScan()
                startScanDev()
                isRefreshing = true
                mUIHandler.postDelayed({
                    if (isRefreshing) {
                        srl_scan.isRefreshing = false
                        isRefreshing = false
                    }
                }, 500)
            }
        }
    }

    private fun observerCallback() {
        viewModel.bluetoothStateMLD.observe(viewLifecycleOwner) { isOpen ->
            if (!isOpen) {
                pb_scan_status?.visibility = View.INVISIBLE
                tv_scan_tip?.text = getString(R.string.bt_bluetooth_close)
                adapter.setList(mutableListOf())
            } else if (!viewModel.isScanning()) {
                tv_scan_tip?.text = getString(R.string.scan_tip)
//                viewModel.startScan()
                startScanDev()
            }
        }
        viewModel.scanResultMLD.observe(viewLifecycleOwner) { result ->
            requireActivity().run {
                when (result.state) {
                    ScanResult.SCAN_STATUS_IDLE -> {  //搜索结束
                        pb_scan_status?.visibility = View.INVISIBLE
                        tv_scan_tip?.text = getString(R.string.scan_tip)
                    }
                    ScanResult.SCAN_STATUS_SCANNING -> { //搜索开始
                        pb_scan_status?.visibility = View.VISIBLE
                        tv_scan_tip?.text = getString(R.string.scaning_tip)
                        adapter.apply {
                            this.setList(mutableListOf())
                            viewModel.getConnectedDevice()?.let {
                                addDevice(ScanDevice(it, 0))
                            }
                        }
                        mScanDeviceList.clear()
                    }
                    ScanResult.SCAN_STATUS_FOUND_DEV -> {
                        result.device.let {
                            if (it != null) {
                                var isContain = false;
                                for (index in 0..mScanDeviceList.size - 1) {
                                    var scanDevice: ScanDevice? = null
                                    if (!mScanDeviceList.isEmpty()) {
                                        scanDevice = mScanDeviceList.get(index)
                                    }
                                    if (BluetoothUtil.deviceEquals(it.device, scanDevice?.device)) {
                                        mScanDeviceList.set(index, it)
                                        isContain = true
                                        break
                                    }
                                }
                                if (!isContain) {
                                    mScanDeviceList.add(it)
                                }
                            }
                            val filterStr = tv_tv_scan_content?.text?.toString()?.trim()
                            val filterList = ArrayList<ScanDevice>(0)
                            viewModel.getConnectedDevice()?.let {
                                filterList.add(ScanDevice(it, 0))
                            }
                            mScanDeviceList.forEach {
                                val filter = filterStr
                                if (AppUtil.checkHasConnectPermission(requireContext())
                                    && !TextUtils.isEmpty(it.device.name)
                                    && (TextUtils.isEmpty(filter) || it.device.name!!.contains(
                                        filterStr!!,
                                        true
                                    ))
                                ) {
                                    filterList.add(it)
                                }
                            }
                            filterList.sortWith { o1, o2 ->
                                o2.rssi.compareTo(o1.rssi)
                            }.apply {
                            }
                            adapter.setNewInstance(filterList)
                        }
                    }
                }
            }
        }
        viewModel.deviceConnectionMLD.observe(viewLifecycleOwner) { deviceConnection ->
            if (!isVisible) return@observe
            requireActivity().run {
                when (deviceConnection.state) {
                    StateCode.CONNECTION_CONNECTING -> showConnectionDialog()
                    else -> {
                        dismissConnectionDialog()
                        if (deviceConnection.state == StateCode.CONNECTION_CONNECTED||deviceConnection.state == StateCode.CONNECTION_OK) {
                            deviceConnection.device?.let {
                                adapter.addDevice(ScanDevice(deviceConnection.device, 0))
                            }
                        } else {
                            adapter.notifyDataSetChanged()
                        }
                        if (BluetoothUtil.isBluetoothEnable() && !viewModel.isScanning()) {
//                            viewModel.startScan()
//                            startScanDev()
                        }
                    }
                }
            }
        }
    }

    private fun startScanDev(): Unit {
        checkBluetoothEnvironment(object : OnCheckBluetoothEnvironmentCallback {
            override fun onSuccess() {
                unregisterOnCheckBluetoothEnvironmentCallback(this)
                viewModel.startScan()
            }

            override fun onFailed() {
                unregisterOnCheckBluetoothEnvironmentCallback(this)
            }
        })
    }

    private fun showConnectionDialog() {
        if (!isValidFragment()) return
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
                it.show(childFragmentManager, "connect_to_ble")
            }
        }
    }

    private fun dismissConnectionDialog() {
        mNotifyDialog?.let {
            if (it.isShow && isValidFragment()) {
                it.dismiss()
            }
            mNotifyDialog = null
        }
    }
}
