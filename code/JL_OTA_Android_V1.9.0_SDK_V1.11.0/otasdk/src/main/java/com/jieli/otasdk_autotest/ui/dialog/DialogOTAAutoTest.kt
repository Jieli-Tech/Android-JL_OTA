package com.jieli.otasdk_autotest.ui.dialog

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import com.jieli.jl_bt_ota.constant.ErrorCode
import com.jieli.jl_bt_ota.constant.JL_Constant
import com.jieli.jl_bt_ota.util.JL_Log
import com.jieli.otasdk.R
import com.jieli.otasdk.data.constant.OtaConstant
import com.jieli.otasdk.data.model.ota.OTAEnd
import com.jieli.otasdk.data.model.ota.OTAState
import com.jieli.otasdk.data.model.ota.OTAWorking
import com.jieli.otasdk.databinding.FragmentDialogOtaBinding
import com.jieli.otasdk.ui.dialog.CommonDialog
import com.jieli.otasdk.util.AppUtil
import com.jieli.otasdk.util.gone
import com.jieli.otasdk.util.show
import com.jieli.otasdk_autotest.data.auto.TestFinish
import com.jieli.otasdk_autotest.data.auto.TestParam
import com.jieli.otasdk_autotest.data.auto.TestState
import com.jieli.otasdk_autotest.data.auto.TestTaskEnd
import com.jieli.otasdk_autotest.data.auto.TestTaskStart
import com.jieli.otasdk_autotest.tool.auto.TestTask
import com.jieli.otasdk_autotest.ui.auto_ota.OTAAutoTestViewModel
import kotlin.math.ceil

/**
 * OTA进度提示-自动化测试
 */
class DialogOTAAutoTest private constructor(builder: Builder) : CommonDialog(builder) {
    private lateinit var binding: FragmentDialogOtaBinding
    private var isOTAError = false

    override fun createView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        FragmentDialogOtaBinding.inflate(inflater, container, false).also {
            binding = it
            return it.root
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initUI()
        addObserver()
    }

    private fun initUI() {
        if (builder !is Builder) return
        binding.groupAutoTestTitle.apply {
            show()
            binding.clOtaContent.let {
                it.layoutParams.let { lp ->
                    lp.height = getPixelsFromDp(223)
                    it.layoutParams = lp
                }
            }
        }
        binding.tvSureBtn.setOnClickListener {
            builder.viewModel.testStateMLD.value = null
            builder.viewModel.otaStateMLD.value = null
            dismiss()
        }
    }

    //增加观察者
    private fun addObserver() {
        if (builder !is Builder) return
        val viewModel = builder.viewModel
        viewModel.otaStateMLD.observe(viewLifecycleOwner) { otaState ->
            if (otaState == null) return@observe
            updateOtaState(otaState)
        }
        viewModel.testStateMLD.observe(viewLifecycleOwner) { testState ->
            if (testState == null) return@observe
            viewModel.getTestParam()?.let { testParam ->
                updateTestState(testState, testParam)
            }
        }
    }

    private fun formatProgressText(state: String, progress: Int): String =
        OtaConstant.formatString("%s\t\t%d%%", state, progress)

    private fun formatTestLoop(count: Int, total: Int): String =
        OtaConstant.formatString(
            "%s : %d/%d",
            getString(R.string.automated_test_process),
            count,
            total
        )

    private fun updateOtaState(otaState: OTAState) {
        JL_Log.d(TAG, "otaStateMLD", "" + otaState)
        when (otaState.state) {
            OTAState.OTA_STATE_START -> {
//                requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                binding.groupUpgrade.show()
                binding.groupScanDeviceLoading.gone()
                binding.tvUpgradeProgress.text =
                    formatProgressText(getString(R.string.ota_check_file), 0)
                binding.pbUpgradeProgress.progress = 0
            }

            OTAState.OTA_STATE_RECONNECT -> {
                binding.groupUpgrade.gone()
                binding.groupScanDeviceLoading.show()
                binding.tvOtaFileName.gone()
                binding.tvUpgradeProgress.text =
                    formatProgressText(getString(R.string.ota_upgrading), 0)
                binding.pbUpgradeProgress.progress = 0
                binding.tvScanDeviceLoading.text =
                    getString(R.string.verification_file_completed)
            }

            OTAState.OTA_STATE_WORKING -> {
                val otaWorking = otaState as OTAWorking
                val message =
                    if (otaWorking.type == JL_Constant.TYPE_CHECK_FILE) {
                        getString(R.string.ota_check_file)
                    } else {
                        getString(
                            R.string.ota_upgrading
                        )
                    }
                val progress = Math.round(otaWorking.progress)
                binding.groupUpgrade.show()
                binding.groupScanDeviceLoading.gone()
                binding.tvOtaFileName.show()
                binding.tvUpgradeProgress.text = formatProgressText(message, progress)
                binding.pbUpgradeProgress.progress = progress
            }

            OTAState.OTA_STATE_IDLE -> {
//                requireActivity().window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                val otaEnd = otaState as OTAEnd
                when (otaEnd.code) {
                    ErrorCode.ERR_NONE -> {
                        binding.tvUpgradeProgress.text =
                            formatProgressText(getString(R.string.ota_upgrading), 100)
                        binding.pbUpgradeProgress.progress = 100
                    }

                    ErrorCode.ERR_UNKNOWN -> {}
                    else -> {
                        if (otaEnd.code == ErrorCode.SUB_ERR_OTA_IN_HANDLE) { //正在升级中
                            return
                        } else if (otaEnd.code == ErrorCode.SUB_ERR_DATA_NOT_FOUND) { //未找到升级文件
                            (builder as Builder).viewModel.readFileList()
                        }
                        val otaMsg = OtaConstant.formatString(
                            "code: %d(0x%X), %s",
                            otaEnd.code,
                            otaEnd.code,
                            otaEnd.message
                        )
                        binding.tvUpgradeResultReason.text =
                            OtaConstant.formatString(
                                "%s\n%s",
                                getString(R.string.ota_reason),
                                otaMsg
                            )
                        isOTAError = true
                    }
                }
            }
        }
    }

    private fun updateTestState(testState: TestState, testParam: TestParam) {
        JL_Log.d(TAG, "testStateMLD", "" + testState)
        val testCount = ceil((testParam.total.toDouble()) / 2).toInt()
        when (testState.state) {
            TestState.TEST_STATE_WORKING -> {
                //测试开始
                requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) //保持屏幕常亮
                binding.groupAutoTestTitle.show()
                binding.tvAutoTestTitle.text = formatTestLoop(0, testCount)
            }

            TestState.TEST_STATE_IDLE -> {
                //测试结束
                requireActivity().window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) //关闭屏幕常亮
                val testFinish = testState as TestFinish
                binding.tvOtaFileName.gone()
                binding.groupUpgrade.gone()
                binding.groupScanDeviceLoading.gone()
                binding.groupUpgradeResult.show()
                val layoutParams =
                    binding.clOtaContent.layoutParams as FrameLayout.LayoutParams
                binding.tvAutoTestTitle.text = OtaConstant.formatString(
                    "%s : %s",
                    getString(R.string.automated_test_process),
                    getString(R.string.end)
                )
                if (testFinish.code == TestTask.ERR_SUCCESS) {
                    binding.ivUpgradeResultLogo.setImageResource(R.drawable.ic_success_small)
                    binding.tvUpgradeResultTip.text = getString(R.string.ota_complete)
                    layoutParams.height = FrameLayout.LayoutParams.WRAP_CONTENT
                } else {
                    binding.ivUpgradeResultLogo.setImageResource(R.drawable.ic_fail_small)
                    binding.tvUpgradeResultTip.text = getString(R.string.update_failed)
                    binding.tvUpgradeResultReason.show()
                    /*if (!isOTAError) {
                        binding.tvUpgradeResultReason.text = OtaConstant.formatString(
                            "%s\n%s",
                            getString(R.string.ota_reason),
                            testFinish.message
                        )
                    }*/
                    layoutParams.height = FrameLayout.LayoutParams.WRAP_CONTENT
                }
                binding.tvUpgradeResultHit.show()
                binding.tvUpgradeResultHit.text =
                    OtaConstant.formatString(
                        "%s : %d\n%s : %d",
                        getString(R.string.test_tasks),
                        testParam.upgradeCount,
                        getString(R.string.successful_tests),
                        testParam.success
                    )
                binding.clOtaContent.layoutParams = layoutParams
            }

            TestState.TEST_STATE_TASK_START -> {
                //测试任务开始
                val testTaskStart = testState as TestTaskStart
                binding.tvAutoTestTitle.show()
                binding.tvAutoTestTitle.text =
                    formatTestLoop(testParam.upgradeCount, testCount)
                if (testTaskStart.task.type == TestTask.TASK_TYPE_UPDATE) { //已经开始升级内容
                    binding.groupUpgrade.show()
                    binding.groupScanDeviceLoading.gone()
                    binding.tvOtaFileName.text =
                        AppUtil.getFileNameByPath(testTaskStart.message)
                } else if (testTaskStart.task.type == TestTask.TASK_TYPE_CONNECT) { //还在连接设备
                    binding.tvOtaFileName.gone()
                    binding.groupUpgrade.gone()
                    binding.groupScanDeviceLoading.show()
                    /*binding.tvScanDeviceLoading.text =
                        getString(R.string.auto_test_reconnect, testParam.success)*/
                }
            }

            TestState.TEST_STATE_TASK_LOG -> {}
            TestState.TEST_STATE_TASK_END -> {
                //测试任务结束
                val testTaskEnd = testState as TestTaskEnd
                if (testTaskEnd.code == TestTask.ERR_SUCCESS) { //升级成功 。开始进行回连进入下一次升级
                    binding.tvScanDeviceLoading.text =
                        getString(R.string.auto_test_reconnect, testParam.upgradeCount)
                } else {
                    binding.tvScanDeviceLoading.text =
                        getString(R.string.auto_test_fail_reconnect, testParam.upgradeCount)
                    binding.tvUpgradeResultReason.text =
                        OtaConstant.formatString(
                            "%s\n%s",
                            getString(R.string.ota_reason),
                            testTaskEnd.message
                        )
                }
            }
        }
    }

    class Builder(val viewModel: OTAAutoTestViewModel) : CommonDialog.Builder() {

        init {
            cancelable = false
            widthRate = 1.0f
            gravity = Gravity.BOTTOM
        }

        override fun build(): DialogOTAAutoTest = DialogOTAAutoTest(this)
    }
}