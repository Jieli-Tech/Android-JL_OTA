package com.jieli.otasdk.fragments


import android.Manifest
import android.bluetooth.BluetoothDevice
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.PopupWindow
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.jieli.component.utils.ToastUtil
import com.jieli.jlFileTransfer.Constants
import com.jieli.jlFileTransfer.FileUtils
import com.jieli.jlFileTransfer.WifiUtils
import com.jieli.jl_bt_ota.constant.StateCode
import com.jieli.jl_bt_ota.util.JL_Log
import com.jieli.otasdk.MainApplication
import com.jieli.otasdk.R
import com.jieli.otasdk.activities.ContentActivity
import com.jieli.otasdk.activities.MainActivity
import com.jieli.otasdk.dialog.DialogDownloadFile
import com.jieli.otasdk.dialog.DialogFileTransfer
import com.jieli.otasdk.dialog.DialogFileTransferListener
import com.jieli.otasdk.dialog.DialogOTA
import com.jieli.otasdk.dialog.PermissionDialog
import com.jieli.otasdk.model.ota.OTAEnd
import com.jieli.otasdk.model.ota.OTAState
import com.jieli.otasdk.util.AppUtil
import com.jieli.otasdk.util.DownloadFileUtil
import com.jieli.otasdk.util.DownloadFileUtil.DownloadFileCallback
import com.jieli.otasdk.util.FileTransferUtil
import com.jieli.otasdk.util.OtaFileObserverHelper
import com.jieli.otasdk.viewmodel.DownloadFileViewModel
import com.jieli.otasdk.viewmodel.OTAViewModel
import kotlinx.android.synthetic.main.dialog_add_file_operation.view.*
import kotlinx.android.synthetic.main.dialog_file_operation.view.*
import kotlinx.android.synthetic.main.fragment_ota.*
import permissions.dispatcher.*
import java.io.File
import java.io.IOException


/**
 * 升级界面
 */
@RuntimePermissions
class OtaFragment : BaseFileFragment() {
    private lateinit var filePicker: ActivityResultLauncher<String>
    private lateinit var otaViewModel: OTAViewModel
    private lateinit var downloadFileViewModel: DownloadFileViewModel

    private lateinit var adapter: FileAdapter
    private var mIsFirstFileObserver = true
    private var mIsFirstResume = true
    private var mDialogFileTransfer: DialogFileTransfer? = null;
    private var mIsHasStoragePermission = false
    private var isUserNeverAskAgain = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mIsHasStoragePermission = AppUtil.isHasStoragePermission(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_ota, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        otaViewModel = ViewModelProvider(requireActivity()).get(OTAViewModel::class.java)
        downloadFileViewModel =
            ViewModelProvider(requireActivity()).get(DownloadFileViewModel::class.java)
        initUI()
        observeCallback()
        filePicker = registerForActivityResult(ActivityResultContracts.GetContent()) {
            it?.let {
                try {
                    activity?.contentResolver?.let { contentResolver ->
                        val parentFilePath = MainApplication.getOTAFileDir()
                        var fileName = FileUtils.getFileName(context, it)
                        fileName =
                            FileTransferUtil.getNewUpgradeFileName(fileName, File(parentFilePath))
                        val saveFileDialog = DialogInputText().run {
                            this.title = this@OtaFragment.getString(R.string.save_file)
                            this.content = fileName
                            this.leftText = this@OtaFragment.getString(R.string.cancel)
                            this.rightText = this@OtaFragment.getString(R.string.save)
                            this.dialogClickListener = object : DialogClickListener {
                                override fun rightBtnClick(inputText: String?) {
                                    var inputFileNameStr: String = (inputText ?: "").trim()
                                    if (!inputFileNameStr.toUpperCase().endsWith(".UFW")) {
                                        ToastUtil.showToastShort(this@OtaFragment.getString(R.string.ufw_format_file_tips))
                                        return
                                    }
                                    val resultPath =
                                        parentFilePath + File.separator + inputFileNameStr
                                    if (File(resultPath).exists()) {
                                        ToastUtil.showToastShort(this@OtaFragment.getString(R.string.file_name_existed))
                                        return
                                    } else {
                                        FileUtils.copyFile(
                                            contentResolver.openInputStream(it),
                                            resultPath
                                        )
                                        Toast.makeText(
                                            context,
                                            R.string.please_refresh_web,
                                            Toast.LENGTH_LONG
                                        )
                                            .show()
                                    }
                                    this@run.dismiss()
                                }

                                override fun leftBtnClick(inputText: String?) {
                                    this@run.dismiss()
                                }
                            }
                            this
                        }
                        saveFileDialog.show(
                            this@OtaFragment.parentFragmentManager,
                            "scanFilterDialog"
                        )
                    }
//                            RxBus.get().post(Constants.RxBusEventType.LOAD_BOOK_LIST, 0)
                } catch (e: IOException) {
                    Toast.makeText(context, R.string.read_file_failed, Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
        updateOTAConnectionUI(otaViewModel.isConnected(), otaViewModel.getConnectedDevice())
        otaViewModel.readFileList()
    }

    override fun onResume() {
        super.onResume()
        if (mIsHasStoragePermission != AppUtil.isHasStoragePermission(context)) {//存储权限发生变化
            mIsHasStoragePermission = AppUtil.isHasStoragePermission(context)
            if (mIsFirstFileObserver && mIsHasStoragePermission) {
                mIsFirstFileObserver = false
                val tempPath = MainApplication.getOTAFileDir()
                OtaFileObserverHelper.getInstance().updateObserverPath(tempPath)
            }
        }

        if (mIsFirstResume) {
            mIsFirstResume = false
            checkExternalStorage()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        otaViewModel.destroy()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        onRequestPermissionsResult(requestCode, grantResults)
    }

    private fun initUI() {
        rv_file_list.layoutManager = LinearLayoutManager(requireContext())
        adapter = FileAdapter(false, mutableListOf())
        adapter.setOnItemClickListener { _, _, position ->
            adapter.setSelectedIndex(position)
        }
        adapter.setOnItemLongClickListener { adapter, view1, position ->
            val file: File = adapter.data.get(position) as File
            this.context?.run {
                val view = LayoutInflater.from(this).inflate(R.layout.dialog_file_operation, null)
                val popupWindow = PopupWindow(
                    view,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT
                )
                popupWindow.isOutsideTouchable = true
                popupWindow.showAsDropDown(view1, 0, -100, Gravity.RIGHT or Gravity.BOTTOM)
                view.tv_delete_file.setOnClickListener {
                    if (file.exists()) {
                        file.delete()
                    }
                    popupWindow.dismiss()
                }
            }
            return@setOnItemLongClickListener false
        }
        val emptyView =
            LayoutInflater.from(requireContext()).inflate(R.layout.view_file_empty, null)
        adapter.setEmptyView(emptyView)
        rv_file_list.adapter = adapter

        ibtn_file_operation.setOnClickListener {
            checkExternalStorage(object : OnCheckExternalStorageEnvironmentCallback {
                override fun onSuccess() {
                    this@OtaFragment.context?.run {
                        val view =
                            LayoutInflater.from(this)
                                .inflate(R.layout.dialog_add_file_operation, null)
                        val popupWindow = PopupWindow(
                            view,
                            WindowManager.LayoutParams.WRAP_CONTENT,
                            WindowManager.LayoutParams.WRAP_CONTENT
                        )
                        popupWindow.isOutsideTouchable = true
                        popupWindow.showAsDropDown(it)
                        view.tv_upgrade_file_browse_local.setOnClickListener {
                            //  跳到本地文件浏览
                            filePicker.launch("application/octet-stream")
                            popupWindow.dismiss()
                        }
                        view.tv_upgrade_file_http_transfer.setOnClickListener {
                            //  传输升级文件 从电脑
                            popupWindow.dismiss()
                            mDialogFileTransfer = DialogFileTransfer().run {
                                val ipAddr: String = WifiUtils.getDeviceIpAddress()
                                val address = "http://$ipAddr:${Constants.HTTP_PORT}"
                                this.isCancelable = false
                                this.httpUrl = address
                                this.mListener = object : DialogFileTransferListener {
                                    override fun onLeftButtonClick() {
//                                WebService.stop(context)
                                        this@run.dismiss()
                                        mDialogFileTransfer = null
                                    }

                                    override fun onRightButtonClick() {
                                        val cm =
                                            context!!.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val mClipData = ClipData.newPlainText("Label", address)
                                        cm.setPrimaryClip(mClipData)
                                        Toast.makeText(
                                            context,
                                            context!!.getString(R.string.copy_toast),
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                                this.show(this@OtaFragment.parentFragmentManager, "file_transfer")
                                this
                            }
                        }
                        view.tv_scan_qr_code.setOnClickListener {
                            toQrScanFragmentWithPermissionCheck()
                            popupWindow.dismiss()
                        }
                    }

                }

                override fun onFailed() {
                }
            })
        }
        btn_upgrade.setOnClickListener {
            val pathList = adapter.getSelectedItems()
            JL_Log.w(TAG, "ota file size : ${pathList.size}")
            if (pathList.isEmpty()) {
                ToastUtil.showToastShort(getString(R.string.ota_please_chose_file))
                return@setOnClickListener
            }
            if (otaViewModel.getDeviceInfo() == null) {
                ToastUtil.showToastShort(getString(R.string.bt_not_connect_device))
                return@setOnClickListener
            }
            showOTADialog()
            otaViewModel.startOTA(pathList[0])
        }
    }

    private fun checkExternalStorage(callback: OnCheckExternalStorageEnvironmentCallback? = null): Unit {
        Log.d(TAG, "checkExternalStorage: ")
        checkExternalStorageEnvironment(object : OnCheckExternalStorageEnvironmentCallback {
            override fun onSuccess() {
                if (mIsFirstFileObserver) {
                    Log.d(TAG, "checkExternalStorage: mIsFirstFileObserver")
                    mIsFirstFileObserver = false
                    Log.d(TAG, "checkExternalStorage: mIsFirstFileObserver11111")
                    val tempPath = MainApplication.getOTAFileDir()
                    OtaFileObserverHelper.getInstance().updateObserverPath(tempPath)
                    OtaFileObserverHelper.getInstance().startObserver()
                }
                Log.d(TAG, "checkExternalStorage: onSuccess")
                unregisterOnCheckBluetoothEnvironmentCallback(this)
                otaViewModel.readFileList()
                callback?.onSuccess()
            }

            override fun onFailed() {
                unregisterOnCheckBluetoothEnvironmentCallback(this)
                callback?.onFailed()
            }
        })
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
            adapter.setNewInstance(fileList)
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
            ToastUtil.showToastShort(R.string.device_must_mandatory_upgrade)
        }
        otaViewModel.otaStateMLD.observe(viewLifecycleOwner) { otaState ->
            JL_Log.d(TAG, "otaStateMLD : >>> $otaState")
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
        btn_upgrade?.let {
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
        if (!isValidFragment()) return
        updateOTABtn(isConnected)
        updateConnectedDeviceInfo(device)
    }

    private fun updateConnectedDeviceInfo(device: BluetoothDevice?) {
        if (!isValidFragment()) return
        val isConnectedDevice = otaViewModel.isDeviceConnected(device)
        tv_connect_dev_name_vale?.let {
            it.text = if (isConnectedDevice) {
                AppUtil.getDeviceName(requireContext(), device)
            } else {
                ""
            }
        }
        tv_connect_dev_address_vale?.let {
            it.text = if (isConnectedDevice) {
                device!!.address
            } else {
                ""
            }
        }
        tv_connect_dev_type_vale?.let {
            it.text = if (isConnectedDevice) {
                getBtDeviceTypeString(AppUtil.getDeviceType(requireContext(), device))
            } else {
                ""
            }
        }
        tv_connect_status?.let {
            it.text = if (isConnectedDevice) {
                getString(R.string.device_status_connected)
            } else {
                getString(R.string.device_status_disconnected)
            }
        }
    }

    private fun getBtDeviceTypeString(type: Int?): String {
        if (type == null) return ""
        var typeName = ""
        when (type) {
            BluetoothDevice.DEVICE_TYPE_CLASSIC -> typeName =
                getString(R.string.device_type_classic)
            BluetoothDevice.DEVICE_TYPE_LE -> typeName = getString(R.string.device_type_ble)
            BluetoothDevice.DEVICE_TYPE_DUAL -> typeName = getString(R.string.device_type_dual_mode)
            BluetoothDevice.DEVICE_TYPE_UNKNOWN -> typeName =
                getString(R.string.device_type_unknown)
        }
        return typeName
    }

    private fun showOTADialog(): Unit {
        DialogOTA().run {
            this.isCancelable = false
            this.show(this@OtaFragment.parentFragmentManager, "ota_dev")
            this
        }
    }

    private fun showDownloadDialog(): Unit {
        Log.e(TAG, "showDownloadDialog: ")
        DialogDownloadFile().run {
            this.isCancelable = false
            this.show(this@OtaFragment.parentFragmentManager, "downloadFile")
            this
        }
    }

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

    @NeedsPermission(Manifest.permission.CAMERA)
    fun toQrScanFragment() {
        ContentActivity.startContentActivityForResult(
            this,
            QrCodeFragment::class.java.canonicalName, null, qrCodeLauncher
        )
    }

    @OnShowRationale(Manifest.permission.CAMERA)
    fun showRelationForCamera(request: PermissionRequest?) {
        showCameraDialog(request)
        isUserNeverAskAgain = true
    }

    @OnPermissionDenied(Manifest.permission.CAMERA)
    fun onCameraDenied() {
    }

    @OnNeverAskAgain(Manifest.permission.CAMERA)
    fun onCameraNeverAskAgain() {
        if (isUserNeverAskAgain) {
            isUserNeverAskAgain = false
        } else {
            showCameraDialog(null)
        }
    }

    private fun showCameraDialog(request: PermissionRequest?) {
        val permissionDialog = PermissionDialog(Manifest.permission.CAMERA, request)
        permissionDialog.setCancelable(true)
        permissionDialog.show(childFragmentManager, PermissionDialog::class.java.getCanonicalName())
    }
}