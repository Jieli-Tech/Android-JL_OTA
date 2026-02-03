package com.jieli.otasdk.ui.dialog

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
import com.jieli.otasdk.ui.ota.OTAViewModel
import com.jieli.otasdk.util.gone
import com.jieli.otasdk.util.show

/**
 * OTA进度提示
 */
class DialogOTA private constructor(builder: Builder) : CommonDialog(builder) {
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
        val isAutoTest = builder.viewModel.isAutoTestOTA()
        binding.groupAutoTestTitle.apply {
            if (isAutoTest) show() else gone()
            binding.clOtaContent.let {
                it.layoutParams.let { lp ->
                    lp.height = getPixelsFromDp(if (isAutoTest) 223 else 148)
                    it.layoutParams = lp
                }
            }
        }
        binding.tvSureBtn.setOnClickListener {
            builder.viewModel.otaStateMLD.value = null
            dismiss()
        }
    }

    //增加观察者
    private fun addObserver() {
        if (builder !is Builder) return
        builder.viewModel.otaStateMLD.observe(viewLifecycleOwner) { otaState ->
            if (otaState == null) return@observe
            handleOtaState(otaState)
        }
    }

    private fun handleOtaState(otaState: OTAState) {
        if (builder !is Builder || !isAdded) return
        JL_Log.d(TAG, "handleOtaState", otaState.toString())
        val isAutoTest = builder.viewModel.isAutoTestOTA()
        when (otaState.state) {
            OTAState.OTA_STATE_START -> {
                requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
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
                binding.tvScanDeviceLoading.setText(R.string.verification_file_completed)
            }

            OTAState.OTA_STATE_WORKING -> {
                val otaWorking = otaState as OTAWorking
                val message =
                    if (otaWorking.type == JL_Constant.TYPE_CHECK_FILE) getString(R.string.ota_check_file)
                    else getString(R.string.ota_upgrading)
                val progress = Math.round(otaWorking.progress)
                binding.groupUpgrade.show()
                binding.groupScanDeviceLoading.gone()
                binding.tvOtaFileName.apply {
                    if (isAutoTest) {
                        show()
                    } else {
                        gone()
                    }
                }
                binding.tvUpgradeProgress.text = formatProgressText(message, progress)
                binding.pbUpgradeProgress.progress = progress
            }

            OTAState.OTA_STATE_IDLE -> {
                requireActivity().window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                var isUpgradeSuccess = false
                val otaEnd = otaState as OTAEnd
                when (otaEnd.code) {
                    ErrorCode.ERR_NONE -> {
                        binding.tvUpgradeProgress.text =
                            formatProgressText(getString(R.string.ota_upgrading), 100)
                        binding.pbUpgradeProgress.progress = 100
                        // TODO: 2022/11/1  单次升级结束
                        isUpgradeSuccess = true
                    }

                    ErrorCode.ERR_UNKNOWN -> {}
                    else -> {
                        if (otaEnd.code == ErrorCode.SUB_ERR_OTA_IN_HANDLE) { //正在升级中
                            return
                        } else if (otaEnd.code == ErrorCode.SUB_ERR_DATA_NOT_FOUND) { //未找到升级文件
                            builder.viewModel.readFileList()
                        }
                        val otaMsg = OtaConstant.formatString(
                            "code: %d(0x%X), %s", otaEnd.code, otaEnd.code, otaEnd.message
                        )
                        binding.tvUpgradeResultReason.text = OtaConstant.formatString(
                            "%s\n%s",
                            getString(R.string.ota_reason),
                            otaMsg
                        )
                        isOTAError = true
                    }
                }
                if (!isAutoTest) {//单次升级
                    binding.groupScanDeviceLoading.gone()
                    binding.groupUpgrade.gone()
                    binding.groupUpgradeResult.show()
                    binding.clOtaContent.let {
                        it.layoutParams.let { lp ->
                            if (isUpgradeSuccess) {
                                binding.ivUpgradeResultLogo.setImageResource(R.drawable.ic_success_big)
                                binding.tvUpgradeResultTip.setText(R.string.ota_complete)
                                lp.height = getPixelsFromDp(196)
                            } else {
                                binding.ivUpgradeResultLogo.setImageResource(R.drawable.ic_fail_big)
                                binding.tvUpgradeResultTip.setText(R.string.update_failed)
                                binding.tvUpgradeResultReason.visibility = View.VISIBLE
                                lp.height = FrameLayout.LayoutParams.WRAP_CONTENT
                            }
                            it.layoutParams = lp
                        }
                    }
                }
            }
        }
    }

    private fun formatProgressText(state: String, progress: Int): String =
        OtaConstant.formatString("%s\t\t%d%%", state, progress)

    class Builder(val viewModel: OTAViewModel) : CommonDialog.Builder() {

        init {
            cancelable = false
            widthRate = 1.0f
            gravity = Gravity.BOTTOM
        }

        override fun build(): DialogOTA = DialogOTA(this)

    }
}