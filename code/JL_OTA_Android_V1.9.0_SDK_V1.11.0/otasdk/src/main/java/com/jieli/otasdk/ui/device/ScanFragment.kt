package com.jieli.otasdk.ui.device


import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.jieli.jl_bt_ota.constant.StateCode
import com.jieli.jl_bt_ota.interfaces.IActionCallback
import com.jieli.jl_bt_ota.model.base.BaseError
import com.jieli.otasdk.R
import com.jieli.otasdk.data.model.ScanResult
import com.jieli.otasdk.data.model.device.ScanDevice
import com.jieli.otasdk.databinding.FragmentScanBinding
import com.jieli.otasdk.ui.base.BaseActivity
import com.jieli.otasdk.ui.dialog.DialogInputText
import com.jieli.otasdk.util.PermissionUtil
import com.jieli.otasdk.util.gone
import com.jieli.otasdk.util.setViewVisibility
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * 连接设备界面
 */
class ScanFragment : BaseBluetoothFragment() {

    private lateinit var binding: FragmentScanBinding
    private lateinit var viewModel: ConnectViewModel
    private lateinit var adapter: ScanDeviceAdapter

    private var isRefreshing: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        FragmentScanBinding.inflate(inflater, container, false).also {
            binding = it
            return it.root
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this)[ConnectViewModel::class.java]
        initUI()
        addObserver()
    }

    override fun onResume() {
        super.onResume()
        startScanDev()
    }

    override fun onPause() {
        super.onPause()
        viewModel.stopScan()
    }

    private fun initUI() {
        binding.viewTopBar.apply {
            tvTopTitle.text = getString(R.string.connect)
            tvTopRight.setCompoundDrawablesRelativeWithIntrinsicBounds(
                0, 0,
                if (viewModel.isSwitchOtaMode()) {
                    R.drawable.ic_function_switch
                } else {
                    0
                }, 0
            )
            tvTopRight.setOnClickListener {
                if (!viewModel.isSwitchOtaMode()) return@setOnClickListener
                (requireActivity() as BaseActivity).switchPopupWindow(requireActivity(), it)
            }
        }

        binding.viewFilter.apply {
            root.setOnClickListener {
                val content = binding.viewFilter.tvValue.text.toString().trim()
                showInputDialog(content)
            }
            tvTitle.text = getString(R.string.ble_filter)
            tvValue.text = viewModel.getScanFilter()
        }
        binding.srlScan.setOnRefreshListener {
            if (!isRefreshing) {
                startScanDev(true)
                isRefreshing = true
                lifecycleScope.launch {
                    delay(500)
                    if (isRefreshing) {
                        binding.srlScan.isRefreshing = false
                        isRefreshing = false
                    }
                }
            }
        }

        adapter = ScanDeviceAdapter()
        adapter.setOnItemClickListener { _, _, position ->
            if (adapter.data.isEmpty()) return@setOnItemClickListener
            val scanDevice = adapter.getItem(position)
            if (scanDevice.state == StateCode.CONNECTION_CONNECTING) return@setOnItemClickListener
            if (scanDevice.state == StateCode.CONNECTION_OK) { //设备已连接
                viewModel.disconnectBtDevice(scanDevice.device)
                return@setOnItemClickListener
            }
            //设备未连接
            if (viewModel.isConnected()) { //不允许多设备连接
                return@setOnItemClickListener
            }
            viewModel.connectBtDevice(scanDevice.device)
        }
        binding.rvDeviceList.adapter = adapter
    }

    private fun addObserver() {
        viewModel.bluetoothStateMLD.observe(viewLifecycleOwner) { isOpen ->
            if (!isFragmentValid) return@observe
            if (!isOpen) {
                binding.aivLoading.hide()
                binding.aivLoading.gone()
//                binding.pbScanStatus.hide()
//                binding.tvDeviceList.text = getString(R.string.bt_bluetooth_close)
                viewModel.scanDeviceList.clear()
                adapter.setList(viewModel.scanDeviceList)
            } else if (!viewModel.isScanning()) {
                startScanDev()
            }
        }
        viewModel.deviceConnectionMLD.observe(viewLifecycleOwner) { deviceConnection ->
            if (!isFragmentValid) return@observe
            adapter.updateDeviceConnection(deviceConnection)
            deviceConnection.state.let { state ->
                if (state == StateCode.CONNECTION_CONNECTING) {
                    showLoading(getString(R.string.bt_connecting))
                } else {
                    dismissLoading()
                }
            }
        }
        viewModel.scanResultMLD.observe(viewLifecycleOwner) { result ->
            if (!isFragmentValid) return@observe
            when (result.state) {
                ScanResult.SCAN_STATUS_IDLE -> {  //搜索结束
                    binding.aivLoading.hide()
                    binding.aivLoading.gone()
//                    binding.pbScanStatus.hide()
//                    binding.tvDeviceList.text = getString(R.string.scan_tip)
                }

                ScanResult.SCAN_STATUS_SCANNING -> { //搜索开始
                    binding.aivLoading.setViewVisibility(View.VISIBLE)
                    binding.aivLoading.show()
//                    binding.pbScanStatus.show()
//                    binding.tvDeviceList.text = getString(R.string.scaning_tip)
                    viewModel.scanDeviceList.clear()
                    adapter.setList(viewModel.scanDeviceList)

                }

                ScanResult.SCAN_STATUS_FOUND_DEV -> { //发现设备
                    result.device?.let {
                        if (!viewModel.scanDeviceList.contains(it) && isValidDevice(it)) {
                            viewModel.scanDeviceList.add(it)
                            adapter.addDevice(it)
                        }
                    }
                }
            }
        }
    }

    private fun startScanDev(isUser: Boolean = false) {
        if (!isUser && (!PermissionUtil.hasBluetoothPermission(requireContext())
                    || !PermissionUtil.hasLocationPermission(requireContext()))
        ) {
            return
        }
        tryToCheckBluetoothEnvironment(isUser) {
            if (it) viewModel.startScan()
        }
    }

    @SuppressLint("MissingPermission")
    private fun isValidDevice(scanDevice: ScanDevice): Boolean {
        val filterStr = binding.viewFilter.tvValue.text?.toString()?.trim() ?: ""
        if (filterStr.isEmpty()) return true
        var content = scanDevice.device.name
        if (content.isNullOrEmpty()) {
            content = scanDevice.device.address
        }
        return content.startsWith(filterStr.lowercase(Locale.getDefault()))
                || content.startsWith(filterStr.uppercase(Locale.getDefault()))
    }

    private fun showInputDialog(content: String) {
        DialogInputText.Builder()
            .title(getString(R.string.ble_filter))
            .content(content)
            .cancelBtn { dialog, _ ->
                dialog.dismiss()
            }
            .confirmBtn { dialog, _ ->
                val filterStr = (dialog as DialogInputText).getResult()
                dialog.dismiss()
                binding.viewFilter.tvValue.text = filterStr
                viewModel.setScanFilter(filterStr)
                viewModel.scanDeviceList.filter { isValidDevice(it) }.also { list ->
                    list.toMutableList().let { filterList ->
                        viewModel.getConnectedDevice()?.let {
                            filterList.add(ScanDevice(it, 0))
                        }
                        filterList.sortWith { o1, o2 ->
                            o2.rssi.compareTo(o1.rssi)
                        }
                        adapter.setList(filterList)
                    }
                }
            }.build().show(childFragmentManager, DialogInputText::class.simpleName)
    }
}
