package com.jieli.otasdk.ui.settings.log

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.jieli.otasdk.MainApplication
import com.jieli.otasdk.R
import com.jieli.otasdk.databinding.FragmentFileListBinding
import com.jieli.otasdk.ui.base.BaseFragment
import com.jieli.otasdk.ui.base.ContentActivity
import com.jieli.otasdk.ui.dialog.TipsDialog
import com.jieli.otasdk.util.FileUtil
import java.io.File

/**
 * 文件列表
 */
class FileListFragment : BaseFragment() {
    private lateinit var binding: FragmentFileListBinding
    private lateinit var adapter: FileListAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        FragmentFileListBinding.inflate(inflater, container, false).also {
            binding = it
            return it.root
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initUI()
        loadLogFiles()
    }

    private fun initUI() {
        binding.viewMainTopBar.tvTopLeft.setCompoundDrawablesRelativeWithIntrinsicBounds(
            0,
            0,
            R.drawable.ic_return,
            0
        )
        binding.viewMainTopBar.tvTopTitle.setText(R.string.log_files)
        binding.viewMainTopBar.tvTopRight.setCompoundDrawablesRelativeWithIntrinsicBounds(
            0,
            0,
            R.drawable.ic_delete,
            0
        )
        binding.viewMainTopBar.tvTopLeft.setOnClickListener { requireActivity().finish() }
        binding.viewMainTopBar.tvTopRight.setOnClickListener {
            TipsDialog.Builder()
                .content(getString(R.string.delete_all_log_files))
                .cancelBtn { dialog, _ ->
                    dialog.dismiss()
                }
                .confirmBtn { dialog, _ ->
                    dialog.dismiss()
                    FileUtil.deleteFile(File(MainApplication.instance.logFileDir))
                    adapter.setList(mutableListOf())
                }.build().show(childFragmentManager, TipsDialog::class.simpleName)
        }
        adapter = FileListAdapter()
        binding.rvFileList.adapter = adapter
        adapter.setOnItemClickListener { adapter, _, position ->
            val file = (adapter as FileListAdapter).getItem(position)
            ContentActivity.startContentActivity(
                requireContext(),
                FileDetailFragment::class.java.canonicalName,
                bundleOf(Pair(FileDetailFragment.KEY_FILE_PATH, file.path))
            )
        }
    }

    private fun loadLogFiles() {
        val dir = File(MainApplication.instance.logFileDir)
        if (dir.exists() && dir.isDirectory) {
            val files = dir.listFiles()?.toMutableList() ?: mutableListOf()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                files.sortByDescending { it.lastModified() }
            }
            adapter.setList(files)
        }
    }

    private inner class FileListAdapter :
        BaseQuickAdapter<File, BaseViewHolder>(R.layout.item_file_list_2) {
        override fun convert(holder: BaseViewHolder, item: File) {
            holder.setText(R.id.tv_item_file_name, item.name)
        }
    }

    companion object {
        fun newInstance(): FileListFragment {
            return FileListFragment()
        }
    }
}