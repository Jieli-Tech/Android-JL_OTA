package com.jieli.otasdk.data.model.setting

import android.view.View
import android.widget.CompoundButton
import com.chad.library.adapter.base.entity.MultiItemEntity

/**
 *
 * @ClassName:      InputTextSettings
 * @Description:     带输入型的设置选项
 * @Author:         ZhangHuanMing
 * @CreateDate:     2022/9/29 15:14
 */
class InputTextSettings(
    var settingsOpName: String? = null,
    var settingsSwitchStatus: Boolean = false,
    var settingsInputTip: String? = null,
    var settingsInputContent: String? = null,
    var checkedChangeListener: CompoundButton.OnCheckedChangeListener? = null,
    var inputClickListener: View.OnClickListener? = null,
    override val itemType: Int = 1,
    ) : MultiItemEntity {
}