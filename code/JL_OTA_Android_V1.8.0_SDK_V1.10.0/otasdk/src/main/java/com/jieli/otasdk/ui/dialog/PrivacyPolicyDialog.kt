package com.jieli.otasdk.ui.dialog

import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.jieli.otasdk.R
import com.jieli.otasdk.databinding.DialogPrivacyPolicyBinding

/**
 * PrivacyPolicyDialog
 * @author zqjasonZhong
 * @since 2025/4/8
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 隐私政策对话框
 */
class PrivacyPolicyDialog private constructor(builder: Builder) : CommonDialog(builder) {

    private lateinit var binding: DialogPrivacyPolicyBinding

    override fun createView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        DialogPrivacyPolicyBinding.inflate(inflater, container, false).also {
            binding = it
            return it.root
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initUI()
    }

    private fun initUI() {
        if (builder !is Builder) return
        binding.btnAgree.setOnClickListener {
            builder.callback.onAgree(this)
        }
        binding.btnDisagree.setOnClickListener {
            builder.callback.onDisagree(this)
        }

        val appName = getString(R.string.app_name)
        var content = getString(R.string.privacy_policy_dialog_content, appName, appName)
        val userService = getString(R.string.user_agreement_name)
        val privacyPolicy = getString(R.string.privacy_policy_name)
        val startPos = content.indexOf("####")
        if (startPos == -1) return
        val endPos = startPos + userService.length
        content = content.replace("####", userService)
        val startPos1 = content.indexOf("****")
        if (startPos1 == -1) return
        val endPos1 = startPos1 + privacyPolicy.length
        content = content.replace("****", privacyPolicy)
        val span = SpannableString(content)
        span.setSpan(object : ClickableSpan() {
            override fun onClick(widget: View) {
                builder.callback.onUserService(this@PrivacyPolicyDialog)
            }
        }, startPos, endPos, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        span.setSpan(
            ForegroundColorSpan(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.main_color
                )
            ), startPos, endPos, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        span.setSpan(object : ClickableSpan() {
            override fun onClick(widget: View) {
                builder.callback.onPrivacyPolicy(this@PrivacyPolicyDialog)
            }
        }, startPos1, endPos1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        span.setSpan(
            ForegroundColorSpan(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.main_color
                )
            ), startPos1, endPos1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        binding.tvContent.append(span)
        binding.tvContent.movementMethod = LinkMovementMethod.getInstance()
        binding.tvContent.isLongClickable = false
    }

    interface OnClickListener {

        fun onUserService(dialog: PrivacyPolicyDialog)

        fun onPrivacyPolicy(dialog: PrivacyPolicyDialog)

        fun onAgree(dialog: PrivacyPolicyDialog)

        fun onDisagree(dialog: PrivacyPolicyDialog)

    }

    class Builder(val callback: OnClickListener) : CommonDialog.Builder() {

        init {
            cancelable = false
        }

        override fun build(): PrivacyPolicyDialog = PrivacyPolicyDialog(this)
    }
}