package com.jieli.otasdk.fragments

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
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
            device?.run {
                val temp = it.find { it.device == this.device }
                if (temp == null) {
                    it.add(device)
                } else {
                    temp.rssi = this.rssi
                }
                it.sortWith { o1, o2 ->
                    o2.rssi.compareTo(o1.rssi)
                }.apply {
                    notifyDataSetChanged()
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