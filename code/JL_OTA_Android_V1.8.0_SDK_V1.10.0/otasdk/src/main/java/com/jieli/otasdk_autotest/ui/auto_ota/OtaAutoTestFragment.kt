package com.jieli.otasdk_autotest.ui.auto_ota


import android.Manifest
import android.bluetooth.BluetoothDevice
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.PopupWindow
import android.widget.TextView
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.jieli.jl_bt_ota.constant.StateCode
import com.jieli.jl_bt_ota.interfaces.IActionCallback
import com.jieli.jl_bt_ota.model.base.BaseError
import com.jieli.jl_bt_ota.util.JL_Log
import com.jieli.otasdk.R
import com.jieli.otasdk.data.model.ota.OTAEnd
import com.jieli.otasdk.data.model.ota.OTAState
import com.jieli.otasdk.databinding.FragmentOtaBinding
import com.jieli.otasdk.tool.config.ConfigHelper
import com.jieli.otasdk.ui.base.ContentActivity
import com.jieli.otasdk.ui.dialog.DialogDownloadFile
import com.jieli.otasdk.ui.home.MainActivity
import com.jieli.otasdk.ui.ota.BaseFileFragment
import com.jieli.otasdk.ui.ota.DownloadFileViewModel
import com.jieli.otasdk.ui.ota.FileAdapter
import com.jieli.otasdk.ui.ota.onRequestPermissionsResult
import com.jieli.otasdk.ui.qr_code.QrCodeFragment
import com.jieli.otasdk.ui.widget.window.AddFileWayWindow
import com.jieli.otasdk.util.AppUtil
import com.jieli.otasdk.util.DeviceUtil
import com.jieli.otasdk.util.UIHelper
import com.jieli.otasdk.util.getView
import com.jieli.otasdk_autotest.ui.dialog.DialogOTAAutoTest
import permissions.dispatcher.NeedsPermission
import permissions.dispatcher.OnPermissionDenied
import permissions.dispatcher.OnShowRationale
import permissions.dispatcher.PermissionRequest
import permissions.dispatcher.RuntimePermissions
import java.io.File


/**
 * 升级界面
 */
@RuntimePermissions
class OtaAutoTestFragment : BaseFileFragment() {
    private lateinit var binding: FragmentOtaBinding
    private lateinit var otaViewModel: OTAAutoTestViewModel
    private lateinit var downloadFileViewModel: DownloadFileViewModel
    private lateinit var adapter: FileAdapter

    private lateinit var filePickerLauncher: ActivityResultLauncher<String>

    private val qrCodeLauncher = registerForActivityResult(
        StartActivityForResult(),
        object : ActivityResultCallback<ActivityResult?> {
            override fun onActivityResult(result: ActivityResult?) {
                result?.let {
                    if (result.resultCode == QrCodeFragment.QRCODE_HTTP) {
                        val httpUrl =
                            result.data?.getStringExtra(QrCodeFragment.QRCODE_HTTP_URL) ?: return
                        showDownloadDialog()
                        downloadFileViewModel.downloadFile(httpUrl)
                    }
                }
            }
        })

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        FragmentOtaBinding.inflate(inflater, container, false).also {
            binding = it
            return it.root
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        filePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) {
            it?.let { uri ->
                UIHelper.showSaveFileDialog(requireContext(), childFragmentManager, uri, object :IActionCallback<Boolean>{
                    override fun onSuccess(message: Boolean?) {
                        otaViewModel.readFileList()
                    }

                    override fun onError(error: BaseError?) {

                    }
                })
            }
        }
        otaViewModel = ViewModelProvider(requireActivity())[OTAAutoTestViewModel::class.java]
        downloadFileViewModel =
            ViewModelProvider(requireActivity())[DownloadFileViewModel::class.java]
        initUI()
        observeCallback()
        updateOTAConnectionUI(otaViewModel.isConnected(), otaViewModel.getConnectedDevice())
    }

    override fun onResume() {
        super.onResume()
        otaViewModel.readFileList()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if(!hidden){
            otaViewModel.readFileList()
        }
    }

    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION")
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        onRequestPermissionsResult(requestCode, grantResults)
    }

    override fun onDestroy() {
        super.onDestroy()
        otaViewModel.destroy()
    }

    @NeedsPermission(Manifest.permission.CAMERA)
    fun toQrScanFragment() {
        ContentActivity.startContentActivity(
            requireContext(),
            QrCodeFragment::class.java.canonicalName, launcher = qrCodeLauncher
        )
    }

    @OnShowRationale(Manifest.permission.CAMERA)
    fun showRelationForCamera(request: PermissionRequest?) {
        request?.proceed()
    }

    @OnPermissionDenied(Manifest.permission.CAMERA)
    fun onCameraDenied() {
        JL_Log.w(TAG, "onCameraDenied", "")
    }

    private fun initUI() {
        binding.viewTopBar.tvTopTitle.text = getString(R.string.upgrade)

        adapter = FileAdapter(otaViewModel.isAutoTest(), mutableListOf())
        adapter.setOnItemClickListener { _, _, position ->
            adapter.setSelectedIndex(position)
        }
        adapter.setOnItemLongClickListener { adapter, view1, position ->
            val file = this@OtaAutoTestFragment.adapter.data[position]
            if (this@OtaAutoTestFragment.adapter.isSelectedFile(file)) return@setOnItemLongClickListener false
            val view = requireContext().getView(R.layout.dialog_file_operation)
            val popupWindow = PopupWindow(
                view,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT
            )
            popupWindow.isOutsideTouchable = true
            popupWindow.showAsDropDown(view1, 0, -100, Gravity.END or Gravity.BOTTOM)
            view.findViewById<TextView>(R.id.tv_delete_file).setOnClickListener {
                if (file.exists()) {
                    file.delete()
                }
                popupWindow.dismiss()
                otaViewModel.readFileList()
            }
            return@setOnItemLongClickListener true
        }
        adapter.setEmptyView(requireContext().getView(R.layout.view_file_empty))
        binding.rvFileList.layoutManager = LinearLayoutManager(requireContext())
        binding.rvFileList.adapter = adapter

        binding.ibtnFileOperation.setOnClickListener { view ->
            tryToCheckStorageEnvironment(object : IActionCallback<Boolean> {
                override fun onSuccess(message: Boolean?) {
                    if (message == true) {
                        AddFileWayWindow(requireContext()) { op ->
                            when (op) {
                                AddFileWayWindow.OP_SELECT_LOCAL_FILE -> {
                                    //  跳到本地文件浏览
                                    filePickerLauncher.launch("application/octet-stream")
                                }

                                AddFileWayWindow.OP_SELECT_WEB_FILE -> {
                                    //  传输升级文件 从电脑
                                    UIHelper.showWebFileTransferDialog(
                                        requireContext(),
                                        childFragmentManager
                                    ){
                                        otaViewModel.readFileList()
                                    }
                                }

                                AddFileWayWindow.OP_SCAN_QR_CODE -> {
                                    toQrScanFragmentWithPermissionCheck()
                                }
                            }
                        }.show(view)
                    }
                }

                override fun onError(error: BaseError?) {

                }
            })
        }
        binding.btnUpgrade.setOnClickListener {
            var testCount = if (otaViewModel.isAutoTest()) {
                otaViewModel.getAutoTestCount()
            } else 1
            var faultTolerantCount =
                if (otaViewModel.isAutoTest() && otaViewModel.isFaultTolerant()) {
                    otaViewModel.getFaultTolerantCount()
                } else 0
            val pathList = adapter.getSelectedItems()
            JL_Log.w(TAG, "ota file size : ${pathList.size}")
            if (pathList.isEmpty()) {
                showTips(getString(R.string.ota_please_chose_file))
                return@setOnClickListener
            }
            if (otaViewModel.getDeviceInfo() == null) {
                showTips(getString(R.string.bt_not_connect_device))
                return@setOnClickListener
            }
            if (otaViewModel.isAutoTest() && otaViewModel.getDeviceInfo()!!.isMandatoryUpgrade) {
                showTips(getString(R.string.mandatory_upgrade_no_auto_test_recorvery))
                testCount = 1
                faultTolerantCount = 0
            }
            showOTADialog()
            otaViewModel.startOTA(testCount, faultTolerantCount, pathList)
        }
    }

    private fun observeCallback() {
        otaViewModel.fileListMLD.observe(viewLifecycleOwner) { fileList ->
            JL_Log.i(TAG, "readFileList ---> ${fileList.size}")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                fileList.sortWith { o1, o2 ->
                    if (o1.lastModified() > o2.lastModified()) {
                        -1
                    } else {
                        1
                    }
                }
            }
            adapter.onUpdateDataList(fileList)
            adapter.setList(fileList)
        }
        otaViewModel.otaConnectionMLD.observe(viewLifecycleOwner) { otaConnection ->
            requireActivity().run {
                JL_Log.d(TAG, "otaConnectionMLD : >>> $otaConnection")
                updateOTAConnectionUI(
                    otaConnection.state == StateCode.CONNECTION_OK,
                    otaConnection.device
                )
            }
        }
        otaViewModel.mandatoryUpgradeMLD.observe(viewLifecycleOwner) { device ->
            JL_Log.d(
                TAG, "mandatoryUpgradeMLD : >>> ${
                    AppUtil.printBtDeviceInfo(device)
                }"
            )
            (requireActivity() as MainActivity).switchSubFragment(1)
            if(!otaViewModel.isOTA()){
                showTips(getString(R.string.device_must_mandatory_upgrade))
            }
        }
        otaViewModel.otaStateMLD.observe(viewLifecycleOwner) { otaState ->
            JL_Log.d(TAG, "otaStateMLD", "" + otaState)
            if (otaState == null) return@observe
            requireActivity().run {
                when (otaState.state) {
                    OTAState.OTA_STATE_IDLE -> { //OTA结束
                        val otaEnd = otaState as OTAEnd
                        updateConnectedDeviceInfo(otaEnd.device)
                    }
                }
            }
        }
    }


    private fun updateOTABtn(isConnected: Boolean) {
        binding.btnUpgrade.let {
            it.isEnabled = isConnected
            it.visibility = if (otaViewModel.isOTA()) {
                View.INVISIBLE
            } else {
                View.VISIBLE
            }
            it.setBackgroundResource(
                if (isConnected) {
                    R.drawable.bg_btn_upgrade
                } else {
                    R.drawable.dbg_btn_unenable
                }
            )
        }
    }

    private fun updateOTAConnectionUI(isConnected: Boolean, device: BluetoothDevice?) {
        if (!isFragmentValid) return
        updateOTABtn(isConnected)
        updateConnectedDeviceInfo(device)
    }

    private fun updateConnectedDeviceInfo(device: BluetoothDevice?) {
        if (!isFragmentValid) return
        binding.tvConnectDevTypeVale.text =
            DeviceUtil.getBtDeviceTypeString(requireContext(), device)
        binding.tvConnectStatus.let {
            it.text = if (otaViewModel.isDeviceConnected(device)) {
                getString(R.string.device_status_connected)
            } else {
                getString(R.string.device_status_disconnected)
            }
        }
    }

    private fun showOTADialog() {
        DialogOTAAutoTest.Builder(otaViewModel)
            .build().show(childFragmentManager, DialogOTAAutoTest::class.simpleName)
    }

    private fun showDownloadDialog() {
        DialogDownloadFile.Builder(downloadFileViewModel).build()
            .show(childFragmentManager, DialogDownloadFile::class.simpleName)
    }
}