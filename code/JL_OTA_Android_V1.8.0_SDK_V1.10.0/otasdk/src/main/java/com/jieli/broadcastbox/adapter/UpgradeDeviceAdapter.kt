package com.jieli.broadcastbox.adapter

import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.jieli.broadcastbox.model.BroadcastBoxInfo
import com.jieli.otasdk.R
import com.jieli.otasdk.util.DeviceUtil

/**
 * Des:
 * author: Bob
 * date: 2022/11/30
 * Copyright: Jieli Technology
 * Modify date:
 * Modified by:
 */
class UpgradeDeviceAdapter : BaseQuickAdapter<BroadcastBoxInfo, BaseViewHolder>(
    R.layout.upgrade_device_item
) {

    override fun convert(holder: BaseViewHolder, item: BroadcastBoxInfo) {
        holder.setText(R.id.tv_device_name, DeviceUtil.getDeviceName(context, item.device))
        if (item.selectFile != null) {
            holder.setText(R.id.tv_upgrade_filename, item.selectFile.name)
        } else {
            holder.setText(
                R.id.tv_upgrade_filename,
                context.getString(R.string.no_file_selected)
            )
        }
        var resId = R.drawable.ic_hook_nol
        if (item.isChosen) {
            resId = R.drawable.ic_hook_sel
        }
        holder.setImageResource(R.id.iv_hook, resId)
    }
}