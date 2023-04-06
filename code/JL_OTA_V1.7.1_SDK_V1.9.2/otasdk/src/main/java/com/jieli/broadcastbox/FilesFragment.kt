package com.jieli.broadcastbox

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.*
import android.widget.PopupWindow
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.GetContent
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.jieli.broadcastbox.adapter.UpgradeFileAdapter
import com.jieli.broadcastbox.viewmodel.BroadcastBoxViewModel
import com.jieli.broadcastbox.viewmodel.FileOpViewModel
import com.jieli.component.utils.ToastUtil
import com.jieli.jlFileTransfer.Constants
import com.jieli.jlFileTransfer.FileUtils
import com.jieli.jlFileTransfer.WifiUtils
import com.jieli.jl_bt_ota.util.JL_Log
import com.jieli.otasdk.MainApplication
import com.jieli.otasdk.R
import com.jieli.otasdk.base.BaseFragment
import com.jieli.otasdk.databinding.FragmentFilesBinding
import com.jieli.otasdk.dialog.DialogFileTransfer
import com.jieli.otasdk.dialog.DialogFileTransferListener
import com.jieli.otasdk.fragments.DialogClickListener
import com.jieli.otasdk.fragments.DialogInputText
import com.jieli.otasdk.util.FileTransferUtil
import com.jieli.otasdk.util.OtaFileObserverHelper
import kotlinx.android.synthetic.main.dialog_add_file_operation.view.*
import kotlinx.android.synthetic.main.fragment_files.view.*
import java.io.File
import java.io.IOException

class FilesFragment : BaseFragment() {
    private lateinit var binding: FragmentFilesBinding
    private var adapter: UpgradeFileAdapter? = null
    private lateinit var fileOpViewModel: FileOpViewModel
    private lateinit var broadcastBoxViewModel: BroadcastBoxViewModel
    private var filePicker: ActivityResultLauncher<String>? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentFilesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        fileOpViewModel = ViewModelProvider(requireActivity())[FileOpViewModel::class.java]
        broadcastBoxViewModel = ViewModelProvider(requireActivity())[BroadcastBoxViewModel::class.java]
        binding.rvFileList.layoutManager = LinearLayoutManager(requireContext())
        if (adapter == null) {
            adapter = UpgradeFileAdapter()
        }
        binding.rvFileList.adapter = adapter

//        val emptyView = LayoutInflater.from(requireContext()).inflate(R.layout.view_file_empty, null)
//        adapter!!.setEmptyView(emptyView)

        binding.root.gravity = Gravity.CENTER
        filePicker = registerForActivityResult(GetContent()) {
            if (it != null) {
                pickFileDialog(it)
            }
        }
        fileOpViewModel.readFileList()
        fileOpViewModel.fileListMLD.observe(viewLifecycleOwner) { files ->
            JL_Log.i(TAG, "readFileList --->" + files.size)
            if(files.size == 0) {
                binding.root.gravity = Gravity.CENTER
                binding.root.iv_file_empty.visibility = View.VISIBLE
                binding.root.iv_add_file_tips.visibility = View.VISIBLE
            } else {
                binding.root.gravity = Gravity.TOP
                binding.root.iv_file_empty.visibility = View.GONE
                binding.root.iv_add_file_tips.visibility = View.GONE
            }
            if (Build.VERSION.SDK_INT >= 24) { // TODO ?
                files.sortWith { o1, o2 ->
                    if (o1.lastModified() > o2.lastModified()) {
                        -1
                    } else {
                        1
                    }
                }
            }
            adapter!!.setNewInstance(files)
            adapter!!.notifyDataSetChanged()
        }

    }

    override fun onResume() {
        super.onResume()
        fileOpViewModel.startFileObserver()
        fileOpViewModel.startWebService(MainApplication.getInstance())
    }

    override fun onPause() {
        super.onPause()
        fileOpViewModel.stopWebService(MainApplication.getInstance())
    }

    fun chooseFilePopupWindow(it : View) {
//        val it : View =
        this.context?.run {
            val view = LayoutInflater.from(MainApplication.getInstance()).inflate(R.layout.dialog_add_file_operation, null)
            val popupWindow = PopupWindow(view, WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT)
            popupWindow.isOutsideTouchable = true
            popupWindow.showAsDropDown(it)

            view.tv_upgrade_file_browse_local.setOnClickListener {
                popupWindow.dismiss()
                tryToAddFile()
            }
            view.tv_upgrade_file_http_transfer.setOnClickListener {
                //  传输升级文件 从电脑
                popupWindow.dismiss()
                DialogFileTransfer().run {
                    val ipAddr: String = WifiUtils.getDeviceIpAddress()
                    val address = "http://$ipAddr:${Constants.HTTP_PORT}"
                    this.isCancelable = false
                    this.httpUrl = address
                    this.mListener = object : DialogFileTransferListener {
                        override fun onLeftButtonClick() {
                            this.run { dismiss() }
                        }

                        override fun onRightButtonClick() {
                            val cm = context!!.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val mClipData = ClipData.newPlainText("Label", address)
                            cm.setPrimaryClip(mClipData)
                            Toast.makeText(context, context!!.getString(R.string.copy_toast),Toast.LENGTH_LONG).show()
                        }
                    }
                    this.show(this@FilesFragment.parentFragmentManager, "file_transfer")
                }
            }
        }
    }

    private fun pickFileDialog(it: Uri) {
        it.let {
            try {
                activity?.contentResolver?.let { contentResolver ->
                    val parentFilePath = MainApplication.getOTAFileDir()
                    var fileName = FileUtils.getFileName(context, it)

                    fileName = FileTransferUtil.getNewUpgradeFileName(fileName, File(parentFilePath))
                    val saveFileDialog = DialogInputText().run {
                        this.title = this@FilesFragment.getString(R.string.save_file)
                        this.content = fileName
                        this.leftText = this@FilesFragment.getString(R.string.cancel)
                        this.rightText = this@FilesFragment.getString(R.string.save)

                        this.dialogClickListener = object : DialogClickListener {
                            override fun rightBtnClick(inputText: String?) {
                                val inputFileNameStr: String = (inputText ?: "").trim()

                                if (!inputFileNameStr.toUpperCase().endsWith(".UFW")) {
                                    ToastUtil.showToastShort(this@FilesFragment.getString(R.string.ufw_format_file_tips))
                                    return
                                }
                                val resultPath =
                                    parentFilePath + File.separator + inputFileNameStr
                                if (File(resultPath).exists()) {
                                    ToastUtil.showToastShort(this@FilesFragment.getString(R.string.file_name_existed))
                                    return
                                } else {
                                    FileUtils.copyFile(contentResolver.openInputStream(it), resultPath)
                                    Toast.makeText(context, R.string.please_refresh_web, Toast.LENGTH_LONG).show()
                                    fileOpViewModel.readFileList()
                                }
                                this@run.dismiss()
                            }

                            override fun leftBtnClick(inputText: String?) {
                                this@run.dismiss()
                            }
                        }
                        this
                    }
                    saveFileDialog.show(parentFragmentManager,"scanFilterDialog")
                }
            } catch (e: IOException) {
                Toast.makeText(context, R.string.read_file_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun tryToAddFile() {
        val writePermission = Manifest.permission.WRITE_EXTERNAL_STORAGE
        if (fileOpViewModel.checkIfGranted(requireActivity(), writePermission)) {
            //  跳到本地文件浏览
            filePicker!!.launch("application/octet-stream")
        } else {
            val permissions = arrayOf(writePermission, Manifest.permission.READ_EXTERNAL_STORAGE)
            storagePermissionLauncher.launch(permissions)
        }
    }

    private val storagePermissionLauncher =
        registerForActivityResult(RequestMultiplePermissions()) { result: Map<String?, Boolean> ->
            var isGrantAll = true
            for (value in result.values) {
                isGrantAll = isGrantAll && value
                JL_Log.i(TAG, "isGrantAll=$isGrantAll, $value")
            }
            if (isGrantAll) {
                //  跳到本地文件浏览
                filePicker!!.launch("application/octet-stream")
            } else {
                JL_Log.e(TAG, "No permission")
            }
        }
}