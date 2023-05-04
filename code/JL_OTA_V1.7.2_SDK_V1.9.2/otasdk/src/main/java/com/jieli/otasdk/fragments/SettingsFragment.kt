package com.jieli.otasdk.fragments

import android.content.pm.PackageManager
import android.content.pm.PackageManager.NameNotFoundException
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.entity.MultiItemEntity
import com.jieli.component.ActivityManager
import com.jieli.component.utils.ToastUtil
import com.jieli.otasdk.MainApplication
import com.jieli.otasdk.R
import com.jieli.otasdk.activities.ContentActivity
import com.jieli.otasdk.base.BaseFragment
import com.jieli.otasdk.dialog.DialogSeekBar
import com.jieli.otasdk.dialog.DialogSeekBarListener
import com.jieli.otasdk.dialog.DialogTip
import com.jieli.otasdk.dialog.DialogTipClickListener
import com.jieli.otasdk.model.setting.BaseSettings
import com.jieli.otasdk.tool.config.ConfigHelper
import com.jieli.otasdk.util.AppUtil
import com.jieli.otasdk.viewmodel.ConfigViewModel
import kotlinx.android.synthetic.main.fragment_settings.*
import java.util.*


class SettingsFragment : BaseFragment() {
    private var customUUID: String? = null
    private var bleMTU: Int = 20
    private var isUseMultiSppChannel: Boolean = false
    private lateinit var configViewModel: ConfigViewModel
    private var isUseDevAuth: Boolean = false;
    private var isHidDevice: Boolean = false;
    private var isUseCustomReconnectWay: Boolean = false;
    private var isAutoTest: Boolean = false;
    private var autoTestCount: Int = 10;
    private var isFaultTolerant: Boolean = false;
    private var faultTolerantCount: Int = 1;
    private var isUseSppChannel: Boolean = false;

    //todo 后续应该用这个控制显示哪些 开发选项
    private var isDevelopMode: Boolean = false;

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.let {
            configViewModel = ViewModelProvider(it).get(ConfigViewModel::class.java)
        }
        initUI()
        initViewModel()
    }

    private fun initUI() {
        //打印日志位置
        val savePath = String.format(
            Locale.getDefault(),
            "%s : %s",
            getString(R.string.log_file_path),
            MainApplication.getLogFileDir()
        )
        tv_log_save_path.setText(savePath)
        //更新AppVersionName
        tv_app_version_content.let {
            var versionName = "v0.0"
            this.activity?.run {
                val packageManager: PackageManager = this.application.packageManager
                val packageName = this.application.packageName
                try {
                    versionName = packageManager.getPackageInfo(packageName, 0).versionName
                } catch (e: NameNotFoundException) {
                    e.printStackTrace()
                }
            }
            it.setText("V$versionName")
        }
        tv_app_version.setOnClickListener {
            Log.d(TAG, "initUI: ")
            if (!configViewModel.isAutoTest()) {
                if (AppUtil.isFastContinuousClick(1000) == 7) {//连续点击7次打开
                    ToastUtil.showToastShort(getString(R.string.enter_develop_mode_tip))
                    // TODO: 显示自动化测试
                    refreshDeveloperUI()
                }
            }
        }
        tv_current_communication_mode.setOnClickListener {
            if (AppUtil.isFastContinuousClick(1000, 5)) {//连续点击5次打开
                ToastUtil.showToastShort(getString(android.R.string.ok))
                val configHelper = ConfigHelper.getInstance()
                val enable = configHelper.isEnableBroadcastBox()
                configHelper.enableBroadcastBox(!enable)// open or close box
            }
        }
        //更新SDKVersionName
        tv_sdk_version_content.let {
            var versionName = "V1.9.0"
            //todo sdk留个接口获取版本号
            it.setText(getString(R.string.sdk_version) + "：$versionName")
        }
        val adapter = SettingsAdapter()
        rv_settings_list.adapter = adapter
//        for (index in 0..3) {
//            adapter.addData(object : MultiItemEntity {
//                override val itemType: Int
//                    get() = -1
//            })
//        }
        tv_communication_way_ble.setOnClickListener {
            isUseSppChannel = false
            updateCommunicationChannelUI(isUseSppChannel)
            compareSetting()
        }
        tv_communication_way_spp.setOnClickListener {
            isUseSppChannel = true
            updateCommunicationChannelUI(isUseSppChannel)
            compareSetting()
        }

        tv_change_mtu.setOnClickListener {//调整MTU
            DialogSeekBar().run {
                this.title = this@SettingsFragment.getString(R.string.adjust_mtu)
                this.leftText = this@SettingsFragment.getString(R.string.cancel)
                this.rightText = this@SettingsFragment.getString(R.string.sure)
                this.max = 512
                this.min = 23
                // TODO: 设置当前MTU
                this.progress = bleMTU + 3
                this.isCancelable = false
                this.mListener = object : DialogSeekBarListener {
                    override fun onLeftButtonClick(progress: Int) {
                        this@run.dismiss()
                    }

                    override fun onRightButtonClick(progress: Int) {
                        bleMTU = progress - 3
                        this@SettingsFragment.tv_change_mtu_content.setText("$progress")
                        compareSetting()
                        this@run.dismiss()
                    }
                }
                this.show(this@SettingsFragment.parentFragmentManager, "file_transfer")
            }
        }
        tv_log_file.setOnClickListener {//浏览log文件列表
            ContentActivity.startContentActivity(
                requireContext(),
                FileListFragment::class.java.getCanonicalName()
            )
        }
    }

    private fun initViewModel() {
        configViewModel.saveSettingMLD.observe(
            viewLifecycleOwner
        ) {
            if (it == true) {
                updateSetting()
            }
        }
        restoreSetting()
        compareSetting()
    }

    private fun restoreSetting(): Unit {
        isUseDevAuth = configViewModel.isUseDevAuth();
        isHidDevice = configViewModel.isHidDevice();
        isUseCustomReconnectWay = configViewModel.isUseCustomReConnectWay();
        isAutoTest = configViewModel.isAutoTest();
        autoTestCount = configViewModel.getAutoTestCount();
        isUseSppChannel = !configViewModel.isBleWay();
        isUseMultiSppChannel = configViewModel.isUseMultiSppChannel()
        bleMTU = configViewModel.getRequestBleMtu()
        customUUID = configViewModel.getCustomSppChannel()
        isDevelopMode = configViewModel.isDevelopMode()
        isFaultTolerant = configViewModel.isFaultTolerant()
        faultTolerantCount = configViewModel.getFaultTolerantCount()
        refreshUI()
    }

    private fun updateSetting(): Unit {
        val saveDialog = DialogTip().run {
            this.title = this@SettingsFragment.getString(R.string.save_setting)
            this.leftText = this@SettingsFragment.getString(R.string.cancel)
            this.rightText = this@SettingsFragment.getString(R.string.restart)
            this.dialogClickListener = object : DialogTipClickListener {
                override fun rightBtnClick() {
                    //  更新setting
                    configViewModel.updateSettingConfigure(
                        !isUseSppChannel,
                        isUseDevAuth,
                        isHidDevice,
                        isUseCustomReconnectWay,
                        isAutoTest,
                        autoTestCount,
                        isUseMultiSppChannel,
                        bleMTU,
                        customUUID,
                        isFaultTolerant,
                        faultTolerantCount
                    )
                    this@SettingsFragment.activity?.finish()
                    ActivityManager.getInstance().popAllActivity()
                    this@run.dismiss()
                }

                override fun leftBtnClick() {
                    ToastUtil.showToastShort(this@SettingsFragment.getString(R.string.save_setting_failed))
                    this@run.dismiss()
                }
            }
            this
        }
        saveDialog.show(parentFragmentManager, "saveDialog")
    }

    private fun refreshUI() {
        updateCommunicationChannelUI(isUseSppChannel)
        tv_change_mtu_content.setText("" + (bleMTU + 3))
        (rv_settings_list.adapter as BaseQuickAdapter<MultiItemEntity, *>).let {
            it.addData(
                0,
                BaseSettings(//设备认证
                    getString(R.string.device_auth),
                    isUseDevAuth,
                    { buttonView, isChecked ->
                        isUseDevAuth = isChecked
                        compareSetting()
                    })
            )
            it.addData(
                1,
                BaseSettings(//HID设备
                    getString(R.string.hid_device),
                    isHidDevice,
                    { buttonView, isChecked ->
                        isHidDevice = isChecked
                        compareSetting()
                    })
            )
            it.addData(
                2,
                BaseSettings(//自定义回连方式
                    getString(R.string.custom_reconnect),
                    isUseCustomReconnectWay,
                    { buttonView, isChecked ->
                        isUseCustomReconnectWay = isChecked
                        compareSetting()
                    })
            )
        }
        if (configViewModel.isAutoTest()) {
            refreshAutoTestUI()
        }
    }

    private fun refreshAutoTestUI() {
        cl_auto_test.visibility=View.VISIBLE
        tv_auto_test_input_content.setText(autoTestCount.toString())
        tv_fault_tolerant_input_content.setText(faultTolerantCount.toString())
        sw_auto_test_op.isChecked = isAutoTest
        sw_auto_test_op.setOnCheckedChangeListener { buttonView, isChecked ->
            isAutoTest = isChecked
            compareSetting()
        }
        sw_fault_tolerant_op.isChecked = isFaultTolerant
        sw_fault_tolerant_op.setOnCheckedChangeListener { buttonView, isChecked ->
            isFaultTolerant = isChecked
            compareSetting()
        }
        tv_auto_test_input_tip.setOnClickListener {
            if (isAutoTest) {
                val autoTestDialog = DialogInputText().run {
                    this.title = this@SettingsFragment.getString(R.string.test_times)
                    this.content = "" + autoTestCount
                    this.leftText = this@SettingsFragment.getString(R.string.cancel)
                    this.rightText = this@SettingsFragment.getString(R.string.sure)
                    this.inputType = InputType.TYPE_CLASS_NUMBER
                    this.dialogClickListener = object : DialogClickListener {
                        override fun rightBtnClick(inputText: String?) {
                            inputText?.run {
                                var testCount = this.toIntOrNull()
                                if (testCount == null) testCount = 1
                                if (testCount > 999 || testCount <= 0) {
                                    ToastUtil.showToastShort(getString(R.string.auto_test_range))
                                    return
                                }
                                autoTestCount = testCount
                                this@SettingsFragment.tv_auto_test_input_content.setText(autoTestCount.toString())
                                compareSetting()
                            }
                            this@run.dismiss()
                        }

                        override fun leftBtnClick(inputText: String?) {
                            this@run.dismiss()
                        }
                    }
                    this
                }
                autoTestDialog.show(parentFragmentManager, "scanFilterDialog")
            }
        }
        tv_fault_tolerant_input_tip.setOnClickListener {
            if (isFaultTolerant) {
                val faultTolerantDialog = DialogInputText().run {
                    this.title = this@SettingsFragment.getString(R.string.fault_tolerant_count)
                    this.content = "" + faultTolerantCount
                    this.leftText = this@SettingsFragment.getString(R.string.cancel)
                    this.rightText = this@SettingsFragment.getString(R.string.sure)
                    this.inputType = InputType.TYPE_CLASS_NUMBER
                    this.dialogClickListener = object : DialogClickListener {
                        override fun rightBtnClick(inputText: String?) {
                            inputText?.run {
                                var testCount = this.toIntOrNull()
                                if (testCount == null) testCount = 1
                                if (testCount!! > 999 || testCount!! <= 0) {
                                    ToastUtil.showToastShort(getString(R.string.auto_test_range))
                                    return
                                }
                                faultTolerantCount = testCount!!
                                this@SettingsFragment.tv_fault_tolerant_input_content.setText(faultTolerantCount.toString())
                                compareSetting()
                            }
                            this@run.dismiss()
                        }

                        override fun leftBtnClick(inputText: String?) {
                            this@run.dismiss()
                        }
                    }
                    this
                }
                faultTolerantDialog.show(parentFragmentManager, "scanFilterDialog")
            }
        }
//        (rv_settings_list.adapter as BaseQuickAdapter<MultiItemEntity, *>).let {
//            val inputTextSettings = InputTextSettings(//自动化测试OTA
//                getString(R.string.auto_test),
//                isAutoTest,
//                getString(R.string.test_times),
//                "" + autoTestCount,
//                { buttonView, isChecked ->
//                    isAutoTest = isChecked
//                    compareSetting()
//                },
//                {
//                    if (isAutoTest) {
//                        val autoTestDialog = DialogInputText().run {
//                            this.title = this@SettingsFragment.getString(R.string.test_times)
//                            this.content = "" + autoTestCount
//                            this.leftText = this@SettingsFragment.getString(R.string.cancel)
//                            this.rightText = this@SettingsFragment.getString(R.string.sure)
//                            this.inputType = InputType.TYPE_CLASS_NUMBER
//                            this.dialogClickListener = object : DialogClickListener {
//                                override fun rightBtnClick(inputText: String?) {
//                                    inputText?.run {
//                                        var testCount = this.toIntOrNull()
//                                        if (testCount == null) testCount = 1
//                                        if(testCount>999||testCount<=0){
//                                            ToastUtil.showToastShort(getString(R.string.auto_test_range))
//                                            return
//                                        }
//                                        autoTestCount = testCount
//                                        compareSetting()
//                                        refreshAutoTestUI(false)
//                                    }
//                                    this@run.dismiss()
//                                }
//
//                                override fun leftBtnClick(inputText: String?) {
//                                    this@run.dismiss()
//                                }
//                            }
//                            this
//                        }
//                        autoTestDialog.show(parentFragmentManager, "scanFilterDialog")
//                    }
//                }
//            )
//            if (isAdd) {
//                it.addData(3, inputTextSettings)
//            } else {
//                it.setData(3, inputTextSettings)
//            }
//        }
    }

    /**
     *比较目前设置是否和缓存的一致
     */
    private fun compareSetting() {
        var isChange = false
        if (isUseSppChannel != !configViewModel.isBleWay()) {
            isChange = true
        }
        if (isUseDevAuth != configViewModel.isUseDevAuth()) {
            isChange = true
        }
        if (isHidDevice != configViewModel.isHidDevice()) {
            isChange = true
        }
        if (isUseCustomReconnectWay != configViewModel.isUseCustomReConnectWay()) {
            isChange = true
        }
        if (isAutoTest != configViewModel.isAutoTest()) {
            isChange = true
        }
        if (autoTestCount != configViewModel.getAutoTestCount()) {
            isChange = true
        }
        if (isUseMultiSppChannel != configViewModel.isUseMultiSppChannel()) {
            isChange = true
        }
        if (bleMTU != configViewModel.getRequestBleMtu()) {
            isChange = true
        }
        if (customUUID != configViewModel.getCustomSppChannel()) {
            isChange = true
        }
        if (isFaultTolerant != configViewModel.isFaultTolerant()) {
            isChange = true
        }
        if (faultTolerantCount != configViewModel.getFaultTolerantCount()) {
            isChange = true
        }
        configViewModel.isIgnoreSaveSetting = false
        configViewModel.configChangeMLD.value = isChange
    }

    //更新当前通讯方式UI
    private fun updateCommunicationChannelUI(isSpp: Boolean) {
        iv_communication_way_ble.visibility = if (isSpp) View.GONE else View.VISIBLE
        iv_communication_way_spp.visibility = if (isSpp) View.VISIBLE else View.GONE
    }

    /**
     * 显示开发者
     */
    private fun refreshDeveloperUI(): Unit {
        // TODO: 目前的做法暂时 直接打开自动化测试OTA，为和iOS统一
        isAutoTest = true
        compareSetting()
        refreshAutoTestUI()
    }
}