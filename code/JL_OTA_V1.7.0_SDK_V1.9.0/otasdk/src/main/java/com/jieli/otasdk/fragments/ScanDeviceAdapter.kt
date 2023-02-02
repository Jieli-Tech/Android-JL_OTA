package com.jieli.otasdk.fragments

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.jieli.jl_bt_ota.util.JL_Log
import com.jieli.otasdk.R
import com.jieli.otasdk.model.ScanDevice
import com.jieli.otasdk.util.AppUtil
import com.jieli.otasdk.viewmodel.ConnectViewModel
import java.util.*

/**
 * 扫描设备适配器
 */
class ScanDeviceAdapter(private val viewModel: ConnectViewModel, data: MutableList<ScanDevice>?) :
    BaseQuickAdapter<ScanDevice, BaseViewHolder>(R.layout.item_device_list, data) {

    override fun convert(holder: BaseViewHolder, item: ScanDevice) {
        item.run {
            holder.setText(R.id.tv_device_name, AppUtil.getDeviceName(context, item.device))
            holder.setText(R.id.tv_device_desc, getDeviceDesc(item))
            holder.setImageResource(
                R.id.iv_device_selected_status,
                if (isConnectedDevice(item.device)) R.drawable.ic_device_choose else 0
            )
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun addDevice(device: ScanDevice?) {
        data.let {
            if (!it.contains(device)) {
                device?.let { it1 ->
                    it.add(it1)

                    it.sortWith { o1, o2 ->
                        o2.rssi.compareTo(o1.rssi)
                    }.apply {
                        notifyDataSetChanged()
                    }
                }
            }
        }
    }

    private fun isConnectedDevice(device: BluetoothDevice?): Boolean {
        return viewModel.isDeviceConnected(device)
    }

    private fun getDeviceDesc(device: ScanDevice?): String {
        if (null == device) return ""
        return String.format(
            Locale.getDefault(),
            "rssi : %d, address : %s",
            device.rssi,
            device.device.address
        )
    }

}