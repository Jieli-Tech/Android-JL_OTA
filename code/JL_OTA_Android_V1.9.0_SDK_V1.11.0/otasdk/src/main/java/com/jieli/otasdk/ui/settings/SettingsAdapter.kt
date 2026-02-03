package com.jieli.otasdk.ui.settings

import android.view.View
import com.chad.library.adapter.base.BaseMultiItemQuickAdapter
import com.chad.library.adapter.base.entity.MultiItemEntity
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.jieli.otasdk.R
import com.jieli.otasdk.data.model.setting.BaseSettings
import com.jieli.otasdk.data.model.setting.InputTextSettings
import com.kyleduo.switchbutton.SwitchButton

/**
 *
 * @ClassName:      SettingsAdapter
 * @Description:     java类作用描述
 * @Author:         ZhangHuanMing
 * @CreateDate:     2022/9/29 15:06
 */
class SettingsAdapter : BaseMultiItemQuickAdapter<MultiItemEntity, BaseViewHolder>() {
    init {
        addItemType(0, R.layout.item_settings_switch)
        addItemType(1, R.layout.item_settings_input)
    }

    override fun convert(holder: BaseViewHolder, item: MultiItemEntity) {
        when (holder.itemViewType) {
            0 -> {
                (item as BaseSettings?)?.let {
                    holder.setText(R.id.tv_setting_op_name, it.settingsOpName)
                    val switchButton: SwitchButton = holder.getView(R.id.sw_setting_op)
                    switchButton.isChecked = it.settingsSwitchStatus
                    switchButton.setOnCheckedChangeListener(it.checkedChangeListener)
                }
            }
            1 -> {
                (item as InputTextSettings?)?.let {
                    holder.setText(R.id.tv_setting_op_name, it.settingsOpName)
                    holder.setText(R.id.tv_setting_input_tip, it.settingsInputTip)
                    holder.setText(R.id.tv_setting_input_content, it.settingsInputContent)
                    val switchButton: SwitchButton = holder.getView(R.id.sw_setting_op)
                    switchButton.isChecked = it.settingsSwitchStatus
                    switchButton.setOnCheckedChangeListener(it.checkedChangeListener)
                    val inputView: View = holder.getView(R.id.tv_setting_input_tip)
                    inputView.setOnClickListener(it.inputClickListener)
                }
            }
        }
    }
}