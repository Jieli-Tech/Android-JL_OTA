package com.jieli.otasdk.ui.ota


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
import com.jieli.otasdk.ui.base.ContentActivity
import com.jieli.otasdk.ui.dialog.DialogDownloadFile
import com.jieli.otasdk.ui.dialog.DialogOTA
import com.jieli.otasdk.ui.home.MainActivity
import com.jieli.otasdk.ui.qr_code.QrCodeFragment
import com.jieli.otasdk.ui.widget.window.AddFileWayWindow
import com.jieli.otasdk.util.AppUtil
import com.jieli.otasdk.util.DeviceUtil
import com.jieli.otasdk.util.UIHelper
import com.jieli.otasdk.util.getView
import permissions.dispatcher.NeedsPermission
import permissions.dispatcher.OnPermissionDenied
import permissions.dispatcher.OnShowRationale
import permissions.dispatcher.PermissionRequest
import permissions.dispatcher.RuntimePermissions


/**
 * 升级界面
 */
@RuntimePermissions
class OtaFragment : BaseFileFragment() {

    private lateinit var binding: FragmentOtaBinding
    private lateinit var otaViewModel: OTAViewModel
    private lateinit var downloadFileViewModel: DownloadFileViewModel
    private lateinit var adapter: FileAdapter

    private lateinit var filePickerLauncher: ActivityResultLauncher<String>

    private val qrCodeLauncher = registerForActivityResult(StartActivityForResult()) { result ->
        if (null == result) return@registerForActivityResult
        if (result.resultCode == QrCodeFragment.QRCODE_HTTP) {
            val httpUrl = result.data?.getStringExtra(QrCodeFragment.QRCODE_HTTP_URL)
                ?: return@registerForActivityResult
            showDownloadDialog()
            downloadFileViewModel.downloadFile(httpUrl)
        }
    }

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
                UIHelper.showSaveFileDialog(requireContext(), childFragmentManager, uri, object : IActionCallback<Boolean>{
                    override fun onSuccess(message: Boolean?) {
                        otaViewModel.readFileList()
                    }

                    override fun onError(error: BaseError?) {

                    }
                })
            }
        }
        otaViewModel = ViewModelProvider(requireActivity())[OTAViewModel::class.java]
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

        adapter = FileAdapter(false, mutableListOf())
        adapter.setOnItemClickListener { _, _, position ->
            adapter.setSelectedIndex(position)
        }
        adapter.setOnItemLongClickListener { _, view1, position ->
            val file = this@OtaFragment.adapter.data[position]
            if (this@OtaFragment.adapter.isSelectedFile(file)) return@setOnItemLongClickListener false
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
                                    ) {
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
            showOTADialog()
            otaViewModel.startOTA(pathList[0])
        }
    }

    private fun observeCallback() {
        otaViewModel.fileListMLD.observe(viewLifecycleOwner) { fileList ->
            JL_Log.i(TAG, "fileListMLD", "file size ---> ${fileList.size}")
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
            JL_Log.d(TAG, "otaConnection", "$otaConnection")
            updateOTAConnectionUI(
                otaConnection.state == StateCode.CONNECTION_OK,
                otaConnection.device
            )
        }
        otaViewModel.mandatoryUpgradeMLD.observe(viewLifecycleOwner) { device ->
            JL_Log.d(
                TAG, "mandatoryUpgrade", "device : ${
                    AppUtil.printBtDeviceInfo(device)
                }"
            )
            (requireActivity() as MainActivity).switchSubFragment(1)
            if (!otaViewModel.isOTA()) {
                showTips(getString(R.string.device_must_mandatory_upgrade))
            }
        }
        otaViewModel.otaStateMLD.observe(viewLifecycleOwner) { otaState ->
            otaState?.let {
                JL_Log.d(TAG, "otaStateMLD", "$it")
                when (otaState.state) {
                    OTAState.OTA_STATE_IDLE -> { //OTA结束
                        val otaEnd = otaState as OTAEnd
                        updateConnectedDeviceInfo(
                            otaViewModel.isDeviceConnected(otaEnd.device),
                            otaEnd.device
                        )
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
        updateConnectedDeviceInfo(isConnected, device)
    }

    private fun updateConnectedDeviceInfo(isConnected: Boolean, device: BluetoothDevice?) {
        if (!isFragmentValid) return
        binding.tvConnectDevTypeVale.text = if (!isConnected) {
            ""
        } else {
            DeviceUtil.getBtDeviceTypeString(requireContext(), device)
        }
        binding.tvConnectStatus.let {
            it.text = if (isConnected) {
                getString(R.string.device_status_connected)
            } else {
                getString(R.string.device_status_disconnected)
            }
        }
    }

    private fun showOTADialog() {
        DialogOTA.Builder(otaViewModel).build()
            .show(childFragmentManager, DialogOTA::class.simpleName)
    }

    private fun showDownloadDialog() {
        DialogDownloadFile.Builder(downloadFileViewModel).build()
            .show(childFragmentManager, DialogDownloadFile::class.simpleName)
    }
}