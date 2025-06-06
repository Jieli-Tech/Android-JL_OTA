package com.jieli.otasdk.data.model.setting

import android.widget.CompoundButton
import com.chad.library.adapter.base.entity.MultiItemEntity

/**
 *
 * @ClassName:      BaseSettings
 * @Description:     基础的设置项
 * @Author:         ZhangHuanMing
 * @CreateDate:     2022/9/29 15:13
 */
class BaseSettings(
    var settingsOpName: String? = null,
    var settingsSwitchStatus: Boolean = false,
    var checkedChangeListener: CompoundButton.OnCheckedChangeListener? = null,
    override val itemType: Int = 0,
    ) : MultiItemEntity {

}