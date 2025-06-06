package com.jieli.otasdk.ui.device

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.jieli.jl_bt_ota.util.BluetoothUtil
import com.jieli.otasdk.R
import com.jieli.otasdk.data.constant.OtaConstant
import com.jieli.otasdk.data.model.device.DeviceConnection
import com.jieli.otasdk.data.model.device.ScanDevice
import com.jieli.otasdk.util.DeviceUtil

/**
 * 扫描设备适配器
 */
class ScanDeviceAdapter :
    BaseQuickAdapter<ScanDevice, BaseViewHolder>(R.layout.item_device_list) {

    override fun convert(holder: BaseViewHolder, item: ScanDevice) {
        item.run {
            holder.setText(R.id.tv_device_name, DeviceUtil.getDeviceName(context, item.device))
            holder.setText(R.id.tv_device_desc, getDeviceDesc(item))
            holder.setImageResource(
                R.id.iv_device_selected_status,
                if (item.isDevConnected()) R.drawable.ic_device_choose else 0
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

    fun updateDeviceConnection(connection: DeviceConnection) {
        val item = findItemByDevice(connection.device) ?: return
        if (item.state != connection.state) {
            item.state = connection.state
            notifyItemChanged(getItemPosition(item))
        }
    }

    private fun getDeviceDesc(device: ScanDevice?): String {
        if (null == device) return ""
        return OtaConstant.formatString(
            "rssi : %d, address : %s",
            device.rssi,
            device.device.address
        )
    }

    private fun findItemByDevice(device: BluetoothDevice?): ScanDevice? {
        if (null == device) return null
        for (item in data) {
            if (BluetoothUtil.deviceEquals(device, item.device)) {
                return item
            }
        }
        return null
    }

}