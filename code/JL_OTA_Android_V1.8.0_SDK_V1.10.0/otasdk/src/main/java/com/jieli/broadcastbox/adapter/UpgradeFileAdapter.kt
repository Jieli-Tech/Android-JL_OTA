package com.jieli.broadcastbox.adapter

import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.jieli.otasdk.R
import java.io.File
import java.util.ArrayList

/**
 * Des:
 * author: Bob
 * date: 2022/11/26
 * Copyright: Jieli Technology
 * Modify date:
 * Modified by:
 */
class UpgradeFileAdapter : BaseQuickAdapter<File, BaseViewHolder>(
    R.layout.upgrade_file_item
) {
    private val selectedList: List<File> = ArrayList()

    override fun convert(holder: BaseViewHolder, item: File) {
        holder.setText(R.id.tv_file_name, item.name)
        holder.setText(R.id.tv_file_path, item.path)
    }
}