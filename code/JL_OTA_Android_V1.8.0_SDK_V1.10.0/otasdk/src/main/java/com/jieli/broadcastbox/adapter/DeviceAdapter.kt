package com.jieli.broadcastbox.adapter

import android.bluetooth.BluetoothDevice
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.jieli.broadcastbox.model.BroadcastBoxInfo
import com.jieli.jl_bt_ota.util.BluetoothUtil
import com.jieli.otasdk.R
import com.jieli.otasdk.util.DeviceUtil
import java.util.Locale

/**
 * Des:
 * author: Bob
 * date: 2022/11/14
 * Copyright: Jieli Technology
 * Modify date:
 * Modified by:
 */
class DeviceAdapter : BaseQuickAdapter<BroadcastBoxInfo, BaseViewHolder>(R.layout.item_device_list, mutableListOf()) {

    fun add(dev: BroadcastBoxInfo) {
        val list: MutableList<BroadcastBoxInfo> = data
        if (list.contains(dev)) return
        list.add(list.size, dev)
    }

    fun addDevice(broadcastBoxInfo: BroadcastBoxInfo?) {
        if (!data.contains(broadcastBoxInfo)) {
            broadcastBoxInfo?.let { it1 ->
                data.add(it1)

                data.sortWith { o1, o2 ->
                    o2.rssi.compareTo(o1.rssi)
                }.apply {
                    notifyDataSetChanged()
                }
            }
        }
    }

    fun getBroadcastBoxInfo(device: BluetoothDevice?): BroadcastBoxInfo? {
        data.let {
            for (item in data) {
                if (BluetoothUtil.deviceEquals(item.device, device)) {
                    return item
                }
            }
        }
        return null
    }

    override fun convert(holder: BaseViewHolder, item: BroadcastBoxInfo) {
        holder.setText(R.id.tv_device_name, DeviceUtil.getDeviceName(context, item.device))
        val deviceInfo = String.format(
            Locale.getDefault(), "rssi : %d, address : %s",
            item.rssi, item.device.address
        )

        holder.setText(R.id.tv_device_desc, deviceInfo)
        holder.setImageResource(R.id.iv_device_selected_status, if (item.isConnected) R.drawable.ic_device_choose else 0)

//        BleManager.getInstance().connectedDeviceList?.let {
//            val isConnected = it.contains(item.device)
//            holder.setImageResource(R.id.iv_device_selected_status, if (isConnected) R.drawable.ic_device_choose else 0)
//        }
//        val isConnected = BluetoothUtil.deviceEquals(connectedDevice, item.device)

    }


}