package com.jieli.broadcastbox

import android.Manifest
import android.net.Uri
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
import androidx.activity.result.contract.ActivityResultContracts.GetContent
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.jieli.broadcastbox.adapter.UpgradeFileAdapter
import com.jieli.broadcastbox.viewmodel.BroadcastBoxViewModel
import com.jieli.broadcastbox.viewmodel.FileOpViewModel
import com.jieli.jl_bt_ota.interfaces.IActionCallback
import com.jieli.jl_bt_ota.model.base.BaseError
import com.jieli.jl_bt_ota.util.JL_Log
import com.jieli.otasdk.R
import com.jieli.otasdk.databinding.FragmentFilesBinding
import com.jieli.otasdk.ui.base.BaseFragment
import com.jieli.otasdk.util.UIHelper
import com.jieli.otasdk.util.getView

class FilesFragment : BaseFragment() {
    private lateinit var binding: FragmentFilesBinding
    private lateinit var adapter: UpgradeFileAdapter
    private lateinit var fileOpViewModel: FileOpViewModel
    private lateinit var broadcastBoxViewModel: BroadcastBoxViewModel

    private var filePicker: ActivityResultLauncher<String>? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentFilesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        fileOpViewModel = ViewModelProvider(requireActivity())[FileOpViewModel::class.java]
        broadcastBoxViewModel =
            ViewModelProvider(requireActivity())[BroadcastBoxViewModel::class.java]
        adapter = UpgradeFileAdapter()
        binding.rvFileList.layoutManager = LinearLayoutManager(requireContext())
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
            if (files.size == 0) {
                binding.root.gravity = Gravity.CENTER
                binding.ivFileEmpty.visibility = View.VISIBLE
                binding.ivAddFileTips.visibility = View.VISIBLE
            } else {
                binding.root.gravity = Gravity.TOP
                binding.ivFileEmpty.visibility = View.GONE
                binding.ivAddFileTips.visibility = View.GONE
            }
            if (Build.VERSION.SDK_INT >= 24) {
                files.sortWith { o1, o2 ->
                    if (o1.lastModified() > o2.lastModified()) {
                        -1
                    } else {
                        1
                    }
                }
            }
            adapter.setList(files)
        }
    }

    override fun onResume() {
        super.onResume()
        fileOpViewModel.startWebService(requireActivity().applicationContext)
    }

    override fun onPause() {
        super.onPause()
        fileOpViewModel.stopWebService(requireActivity().applicationContext)
    }

    fun chooseFilePopupWindow(it: View) {
        val view = requireContext().getView(R.layout.dialog_add_file_operation)
        val popupWindow = PopupWindow(
            view, WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
        view.findViewById<TextView>(R.id.tv_upgrade_file_browse_local).setOnClickListener {
            popupWindow.dismiss()
            tryToAddFile()
        }
        view.findViewById<TextView>(R.id.tv_upgrade_file_http_transfer).setOnClickListener {
            //  传输升级文件 从电脑
            popupWindow.dismiss()
            UIHelper.showWebFileTransferDialog(requireContext(), childFragmentManager){
                fileOpViewModel.readFileList()
            }
        }
        popupWindow.isOutsideTouchable = true
        popupWindow.showAsDropDown(it)
    }

    private fun pickFileDialog(it: Uri) {
        UIHelper.showSaveFileDialog(requireContext(), childFragmentManager, it, object : IActionCallback<Boolean>{
            override fun onSuccess(message: Boolean?) {
                fileOpViewModel.readFileList()
            }

            override fun onError(error: BaseError?) {

            }
        })
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