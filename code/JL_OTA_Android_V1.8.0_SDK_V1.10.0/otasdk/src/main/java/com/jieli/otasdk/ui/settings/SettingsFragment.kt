package com.jieli.otasdk.ui.settings

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.jieli.component.ActivityManager
import com.jieli.jl_bt_ota.constant.BluetoothConstant
import com.jieli.jl_bt_ota.constant.JL_Constant
import com.jieli.jl_bt_ota.util.JL_Log
import com.jieli.otasdk.R
import com.jieli.otasdk.data.constant.OtaConstant
import com.jieli.otasdk.data.model.setting.OtaConfiguration
import com.jieli.otasdk.databinding.FragmentSettingsBinding
import com.jieli.otasdk.tool.config.ConfigHelper
import com.jieli.otasdk.ui.base.BaseFragment
import com.jieli.otasdk.ui.base.ContentActivity
import com.jieli.otasdk.ui.dialog.DialogInputText
import com.jieli.otasdk.ui.dialog.DialogSeekBar
import com.jieli.otasdk.ui.dialog.TipsDialog
import com.jieli.otasdk.ui.settings.about.AboutFragment
import com.jieli.otasdk.ui.settings.log.FileListFragment
import com.jieli.otasdk.util.AppUtil
import com.jieli.otasdk.util.UIHelper
import com.jieli.otasdk.util.ViewUtil
import com.jieli.otasdk.util.gone
import com.jieli.otasdk.util.show

/**
 * 配置界面
 */
class SettingsFragment : BaseFragment() {
    private lateinit var binding: FragmentSettingsBinding
    private lateinit var viewModel: ConfigViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        FragmentSettingsBinding.inflate(inflater, container, false).also {
            binding = it
            return it.root
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(requireActivity())[ConfigViewModel::class.java]
        initUI()
        addObserver()
    }

    private fun initUI() {
        binding.viewTopBar.tvTopTitle.text = getString(R.string.setting)
        binding.viewTopBar.tvTopRight.setOnClickListener {
            viewModel.saveConfiguration(true)
        }

        /*打印日志位置*/
        val savePath = OtaConstant.formatString(
            "%s : %s",
            getString(R.string.log_file_path),
            viewModel.getLogFileDirPath()
        )
        binding.tvLogSavePath.text = savePath

        /*通讯方式配置*/
        binding.tvCommunicationWay.setOnClickListener {
            if (AppUtil.isFastContinuousClick(1000, 5)) {//连续点击5次打开
                val configHelper = ConfigHelper.getInstance()
                val enable = configHelper.isEnableBroadcastBox()
                showTips(
                    if (!enable) getString(R.string.open_ota_mode_switch_func)
                    else getString(R.string.close_ota_mode_switch_func)
                )
                configHelper.enableBroadcastBox(!enable)// open or close box
            }
        }

        /*打印日志文件*/
        UIHelper.updateSettingsTextUI(binding.viewLogFile, getString(R.string.log_files)) {
            ContentActivity.startContentActivity(
                requireContext(),
                FileListFragment::class.java.getCanonicalName()
            )
        }

        /*版本信息*/
        //更新SDKVersionName
        val libVersionName = JL_Constant.getLibVersionName()
        val libVersionCode = JL_Constant.getLibVersionCode()
        val libVersionTxt = OtaConstant.formatString(
            "V%s(%d)", libVersionName, libVersionCode
        )
        UIHelper.updateSettingsTextUI(
            binding.viewSdkVersion,
            getString(R.string.sdk_version),
            libVersionTxt, false
        ) {
            if (viewModel.isAutoTest()) return@updateSettingsTextUI
            if (AppUtil.isFastContinuousClick(1000) == 7) {//连续点击7次打开
                showTips(getString(R.string.enter_develop_mode_tip))
                // TODO: 目前的做法暂时 直接打开自动化测试OTA，为和iOS统一
                val cfg = getCurrentOtaConfiguration()
                cfg.isAutoTest = true
                refreshUI(cfg)
            }
        }

        //更新AppVersionName
        val appVersionTxt = OtaConstant.formatString(
            "V%s(%d)",
            ViewUtil.getAppVersionName(requireContext()),
            ViewUtil.getAppVersion(requireContext())
        )
        UIHelper.updateSettingsTextUI(binding.viewAbout, getString(R.string.about), appVersionTxt) {
            ContentActivity.startContentActivity(
                requireContext(),
                AboutFragment::class.java.canonicalName
            )
        }
        JL_Log.i(TAG, "initUI", "appVersionTxt : $appVersionTxt, libVersionTxt: $libVersionTxt")
    }

    private fun addObserver() {
        viewModel.configChangeMLD.observe(viewLifecycleOwner) {
            updateSaveBtnUI(it)
        }
        viewModel.saveSettingMLD.observe(viewLifecycleOwner) {
            if (it) {
                updateSetting()
            } else {
                refreshUI(viewModel.getOtaConfiguration())
            }
        }
    }

    private fun updateSaveBtnUI(isChange: Boolean) {
        binding.viewTopBar.tvTopRight.apply {
            isClickable = isChange
            text = getString(R.string.save)
            setTextColor(
                ContextCompat.getColor(
                    requireContext(), if (isChange) {
                        R.color.main_color
                    } else {
                        R.color.gray_838383
                    }
                )
            )
        }
    }

    private fun formatBleMtu(mtu: Int): String = (mtu + 3).toString()

    private fun updateCommonSettings(cfg: OtaConfiguration) {
        UIHelper.updateSettingsSwitchUI(
            binding.switchDeviceAuth,
            getString(R.string.device_auth),
            cfg.isUseDeviceAuth
        ) { _, _ ->
            compareOtaConfiguration()
        }
        UIHelper.updateSettingsSwitchUI(
            binding.switchHidDevice,
            getString(R.string.hid_device),
            cfg.isHidDevice
        ) { _, _ ->
            compareOtaConfiguration()
        }
        UIHelper.updateSettingsSwitchUI(
            binding.switchCustomConnect,
            getString(R.string.custom_reconnect),
            cfg.isCustomReconnect
        ) { _, _ ->
            compareOtaConfiguration()
        }
    }

    private fun refreshAutoTestUI(cfg: OtaConfiguration) {
        val isAutoTest = cfg.isAutoTest
        binding.clAutoTest.apply {
            if (isAutoTest) show() else gone()
        }
        UIHelper.updateSettingsSwitchUI(
            binding.switchAutoTest,
            getString(R.string.auto_test),
            isAutoTest, true
        ) { _, _ ->
            compareOtaConfiguration()
        }
        UIHelper.updateSettingsTextUI(
            binding.viewTestCount, getString(R.string.test_times),
            cfg.autoTestCount.toString(), isShowIcon = true, isShowLine = true
        ) {
            if (!UIHelper.getBooleanValue(binding.switchAutoTest)) return@updateSettingsTextUI
            UIHelper.getStringValue(binding.viewTestCount).toIntOrNull()?.let { testCount ->
                showInputNumberDialog(
                    getString(R.string.test_times),
                    testCount.toString()
                ) { value ->
                    UIHelper.updateSettingsTextUI(
                        binding.viewTestCount, null,
                        value.toString(), isShowIcon = true, isShowLine = true
                    )
                    compareOtaConfiguration()
                }
            }
        }
        UIHelper.updateSettingsSwitchUI(
            binding.switchFaultTolerant,
            getString(R.string.fault_tolerant),
            cfg.isAllowFaultTolerant, true
        ) { _, _ ->
            compareOtaConfiguration()
        }
        UIHelper.updateSettingsTextUI(
            binding.viewFaultTolerantCount,
            getString(R.string.fault_tolerant_count),
            cfg.faultTolerantCount.toString()
        ) {
            if (!UIHelper.getBooleanValue(binding.switchAutoTest)
                || !UIHelper.getBooleanValue(binding.switchFaultTolerant)
            ) {
                return@updateSettingsTextUI
            }
            UIHelper.getStringValue(binding.viewFaultTolerantCount).toIntOrNull()?.let { count ->
                showInputNumberDialog(
                    getString(R.string.fault_tolerant_count),
                    count.toString()
                ) { value ->
                    UIHelper.updateSettingsTextUI(
                        binding.viewFaultTolerantCount,
                        value = value.toString()
                    )
                    compareOtaConfiguration()
                }
            }
        }
    }

    //更新当前通讯方式UI
    private fun updateCommunicationChannelUI(isSpp: Boolean, mtu: Int = -1) {
        UIHelper.updateSettingsCheckUI(
            binding.viewBleWay,
            getString(R.string.communication_way_ble),
            !isSpp,
            true
        ) {
            updateCommunicationChannelUI(false)
            compareOtaConfiguration()
        }
        UIHelper.updateSettingsCheckUI(
            binding.viewSppWay,
            getString(R.string.communication_way_spp),
            isSpp
        ) {
            updateCommunicationChannelUI(true)
            compareOtaConfiguration()
        }
        if (mtu >= 0) {
            UIHelper.updateSettingsTextUI(
                binding.viewAdjustMtu,
                getString(R.string.adjust_ble_mtu),
                formatBleMtu(mtu)
            ) {
                UIHelper.getStringValue(binding.viewAdjustMtu).toIntOrNull()?.let { bleMtu ->
                    DialogSeekBar.Builder()
                        .title(getString(R.string.adjust_ble_mtu))
                        .progress(bleMtu)
                        .cancelBtn { dialog, _ ->
                            dialog.dismiss()
                        }
                        .confirmBtn { dialog, _ ->
                            val progress = (dialog as DialogSeekBar).getProgress()
                            dialog.dismiss()
                            UIHelper.updateSettingsTextUI(
                                binding.viewAdjustMtu,
                                value = progress.toString()
                            )
                            compareOtaConfiguration()
                        }.build().show(childFragmentManager, DialogSeekBar::class.simpleName)
                }
            }
        }
    }

    private fun refreshUI(cfg: OtaConfiguration) {
        /*通用配置*/
        updateCommonSettings(cfg)
        /*自动化测试*/
        refreshAutoTestUI(cfg)
        /*通讯方式*/
        updateCommunicationChannelUI(!cfg.isBleWay(), cfg.bleMtu)
    }

    private fun getCurrentOtaConfiguration(): OtaConfiguration {
        return OtaConfiguration().apply {
            isUseDeviceAuth = UIHelper.getBooleanValue(binding.switchDeviceAuth)
            isHidDevice = UIHelper.getBooleanValue(binding.switchHidDevice)
            isCustomReconnect = UIHelper.getBooleanValue(binding.switchCustomConnect)

            isAutoTest = UIHelper.getBooleanValue(binding.switchAutoTest)
            autoTestCount =
                UIHelper.getStringValue(binding.viewTestCount).toIntOrNull()
                    ?: OtaConstant.AUTO_TEST_COUNT
            isAllowFaultTolerant = UIHelper.getBooleanValue(binding.switchFaultTolerant)
            faultTolerantCount =
                UIHelper.getStringValue(binding.viewFaultTolerantCount).toIntOrNull()
                    ?: OtaConstant.AUTO_FAULT_TOLERANT_COUNT

            connectionWay = if (UIHelper.getCheckValue(binding.viewSppWay)) {
                BluetoothConstant.PROTOCOL_TYPE_SPP
            } else {
                BluetoothConstant.PROTOCOL_TYPE_BLE
            }
            bleMtu = (UIHelper.getStringValue(binding.viewAdjustMtu).toIntOrNull()
                ?: (BluetoothConstant.BLE_MTU_MAX + 3)) - 3
        }
    }

    private fun compareOtaConfiguration() {
        viewModel.updateSettingConfigure(getCurrentOtaConfiguration())
    }

    private fun tryToSaveConfiguration() {
        viewModel.updateSettingConfigure(getCurrentOtaConfiguration(), true)
    }

    private fun showInputNumberDialog(
        title: String,
        content: String,
        handle: (value: Int) -> Unit
    ) {
        DialogInputText.Builder()
            .title(title)
            .content(content)
            .inputType(InputType.TYPE_CLASS_NUMBER)
            .cancelBtn { dialog, _ ->
                dialog.dismiss()
            }
            .confirmBtn { dialog, _ ->
                val result = (dialog as DialogInputText).getResult()
                dialog.dismiss()
                val value = result.toIntOrNull() ?: 1
                if (value > 999 || value <= 0) {
                    showTips(getString(R.string.auto_test_range))
                    return@confirmBtn
                }
                handle(value)
            }.build().show(childFragmentManager, DialogInputText::class.simpleName)
    }

    private fun updateSetting() {
        TipsDialog.Builder()
            .content(getString(R.string.save_setting))
            .cancelBtn { dialog, _ ->
                dialog.dismiss()
                showTips(this@SettingsFragment.getString(R.string.save_setting_failed))
            }
            .confirmBtn(getString(R.string.restart)) { dialog, _ ->
                dialog.dismiss()
                tryToSaveConfiguration()
                requireActivity().finish()
                ActivityManager.getInstance().popAllActivity()

            }.build().show(childFragmentManager, TipsDialog::class.simpleName)
    }
}