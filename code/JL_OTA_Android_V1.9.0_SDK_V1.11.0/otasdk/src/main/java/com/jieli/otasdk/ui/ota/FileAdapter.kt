package com.jieli.otasdk.ui.ota

import android.annotation.SuppressLint
import android.text.TextUtils
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.jieli.otasdk.R
import java.io.File
import java.util.*

/**
 * 文件适配器
 *
 * @author zqjasonZhong
 * @since 2021/1/7
 */
class FileAdapter(private var isAutoTest: Boolean, data: MutableList<File>?) :
    BaseQuickAdapter<File, BaseViewHolder>(R.layout.item_file_list, data) {

    private val selectedFilePaths = mutableListOf<String>()


    override fun convert(holder: BaseViewHolder, item: File) {
        holder.setText(R.id.tv_item_file_name, getFileMsg(item))
        holder.setText(R.id.tv_file_path, item.path)
        if (isAutoTest) {
            holder.setText(R.id.iv_item_file_select, getSelectedFilePosition(item).run {
                this?.toString()
                    ?: ""
            })
            holder.setBackgroundResource(
                R.id.iv_item_file_select,
                if (isSelectedFile(item)) R.drawable.bg_blue_15_shape else R.drawable.ic_file_choose_nol
            )
        } else {
            holder.setBackgroundResource(
                R.id.iv_item_file_select,
                if (isSelectedFile(item)) R.drawable.ic_file_choose_sel else R.drawable.ic_file_choose_nol
            )
        }
        //        holder.setImageResource(
//            R.id.iv_item_file_select,
//            if (isSelectedFile(item)) R.drawable.ic_file_choose_sel else R.drawable.ic_file_choose_nol
//        )
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setSelectedIndex(pos: Int) {
        if (pos < 0 || pos >= data.size) return
        val file = getItem(pos)
        if (isSelectedFile(file)) {
            selectedFilePaths.remove(file.path)
        } else {
            if (!isAutoTest && selectedFilePaths.size == 1) {
                selectedFilePaths.clear()
            }
            selectedFilePaths.add(file.path)
        }
        notifyDataSetChanged()
    }

    fun onUpdateDataList(files: List<File>) {
        if (files.isEmpty()) {
            selectedFilePaths.clear()
        } else {
            val pathsIterator = selectedFilePaths.iterator()
            while (pathsIterator.hasNext()) {
                val path = pathsIterator.next()
                var isContain = false;
                for (file in files) {
                    if (TextUtils.equals(file.path, path)) {
                        isContain = true;
                        break;
                    }
                }
                if (!isContain) {
                    pathsIterator.remove()
                }
            }
        }
    }


    fun getSelectedItems(): MutableList<String> {
        val clone = mutableListOf<String>()
        for (path in selectedFilePaths) {
            File(path).run {
                if (this.exists() && this.isFile) {
                    clone.add(path)
                }
            }
        }
        return clone
    }

    fun isSelectedFile(file: File?): Boolean {
        if (null == file) return false
        return selectedFilePaths.contains(file.path)
    }

    private fun getSelectedFilePosition(file: File?): Int? {
        if (null == file) return null
        return selectedFilePaths.indexOf(file.path)+1
    }

    private fun getFileMsg(file: File?): String {
        if (null == file) return ""
        val fileSize: Float = file.length() / 1024f / 1024f
        return String.format(Locale.getDefault(), "%s\t\t%.2fMb", file.name, fileSize)
    }
}