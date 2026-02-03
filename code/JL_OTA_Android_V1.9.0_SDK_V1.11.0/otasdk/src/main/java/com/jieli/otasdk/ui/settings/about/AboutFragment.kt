package com.jieli.otasdk.ui.settings.about

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.jieli.component.utils.SystemUtil
import com.jieli.otasdk.R
import com.jieli.otasdk.data.constant.OtaConstant
import com.jieli.otasdk.databinding.FragmentAboutBinding
import com.jieli.otasdk.ui.base.BaseFragment
import com.jieli.otasdk.ui.base.ContentActivity
import com.jieli.otasdk.ui.base.WebFragment
import com.jieli.otasdk.util.UIHelper
import com.jieli.otasdk.util.hide
import com.jieli.otasdk.util.show


/**
 * 关于界面
 */
class AboutFragment : BaseFragment() {

    companion object {
        private const val ICP_URL = "https://beian.miit.gov.cn/"

        /**
         * 是否显示【用户反馈】界面
         */
        private const val ENABLE_FEEDBACK = false
    }

    private lateinit var binding: FragmentAboutBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        FragmentAboutBinding.inflate(inflater, container, false).also {
            binding = it
            return it.root
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initUI()
    }

    private fun initUI() {
        binding.viewTopBar.tvTopTitle.text = getString(R.string.about)
        binding.viewTopBar.tvTopLeft.setCompoundDrawablesRelativeWithIntrinsicBounds(
            R.drawable.ic_return,
            0,
            0,
            0
        )
        binding.viewTopBar.tvTopLeft.setOnClickListener {
            finish(0)
        }

        binding.tvAppName.text = SystemUtil.getAppName(requireContext())
        binding.tvAppVersion.text = OtaConstant.formatString(
            "%s : V%s", getString(R.string.current_app_version),
            SystemUtil.getVersionName(requireContext())
        )

        UIHelper.updateSettingsTextUI(
            binding.viewUserAgreement,
            getString(R.string.user_agreement),
            isShowIcon = true,
            isShowLine = true
        ) {
            WebFragment.goToWebFragment(
                requireContext(),
                getString(R.string.user_agreement),
                getString(R.string.user_agreement_url)
            )
        }
        UIHelper.updateSettingsTextUI(
            binding.viewPrivacyPolicy,
            getString(R.string.privacy_policy),
            isShowIcon = true,
            isShowLine = ENABLE_FEEDBACK
        ) {
            WebFragment.goToWebFragment(
                requireContext(),
                getString(R.string.privacy_policy),
                getString(R.string.app_privacy_policy)
            )
        }
        UIHelper.updateSettingsTextUI(
            binding.viewFeedback,
            getString(R.string.feedback),
            isShowIcon = true
        ) {
            ContentActivity.startContentActivity(
                requireContext(),
                FeedBackFragment::class.java.canonicalName
            )
        }

        binding.tvIcpMessage.text =
            OtaConstant.formatString(
                "%s : %s",
                getString(R.string.icp_message),
                getString(R.string.icp_number)
            )
        binding.tvIcpMessage.setOnClickListener {
            WebFragment.goToWebFragment(requireContext(), getString(R.string.icp_number), ICP_URL)
        }
        binding.tvCopyright.text = getString(R.string.copy_right)

        if (ENABLE_FEEDBACK) {
            binding.viewFeedback.root.show()
        } else {
            binding.viewFeedback.root.hide()
        }
    }

}