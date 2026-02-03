package com.jieli.otasdk.ui.settings.about

import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import com.jieli.jl_bt_ota.util.CommonUtil
import com.jieli.jl_bt_ota.util.JL_Log
import com.jieli.otasdk.R
import com.jieli.otasdk.databinding.FragmentFeedbackBinding
import com.jieli.otasdk.ui.base.BaseFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.Callback
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

/**
 * FeedBackFragment
 *
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 用户反馈界面
 * @since 2025/8/7
 */
class FeedBackFragment : BaseFragment() {
    private lateinit var binding: FragmentFeedbackBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        FragmentFeedbackBinding.inflate(inflater, container, false).also {
            binding = it
            return it.root
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initUI()
    }

    private fun initUI() {
        binding.viewTopBar.tvTopTitle.text = getString(R.string.feedback)
        binding.viewTopBar.tvTopLeft.setCompoundDrawablesRelativeWithIntrinsicBounds(
            0,
            0,
            R.drawable.ic_return,
            0
        )
        binding.viewTopBar.tvTopLeft.setOnClickListener { finish() }
        binding.etFeedback.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                var num = s.length
                if (num > CONTENT_LIMIT) {
                    binding.etFeedback.setText(s.subSequence(0, CONTENT_LIMIT))
                    binding.etFeedback.setSelection(CONTENT_LIMIT)
                    num = CONTENT_LIMIT
                }
                updateContentLen(num)
            }

            override fun afterTextChanged(s: Editable) {
            }
        })
        binding.btFeedbackCommit.setOnClickListener {
            val feedBackContentText = binding.etFeedback.text.toString()
            val feedBackPhoneText = binding.etFeedbackPhone.text.toString()
            if (TextUtils.isEmpty(feedBackContentText)) {
                showTips(getString(R.string.tips_input_feedback))
                return@setOnClickListener
            }
            if (TextUtils.isEmpty(feedBackPhoneText)) {
                showTips(getString(R.string.tips_input_phone))
                return@setOnClickListener
            }
            if (feedBackPhoneText.contains(" ") || feedBackPhoneText.length != 11) {
                showTips(getString(R.string.tips_input_right_phone))
                return@setOnClickListener
            }
            postAsynsRequest(feedBackContentText, feedBackPhoneText)
        }
        updateContentLen(0)
    }

    private fun updateContentLen(len: Int) {
        binding.tvFeedbackTextLen.text =
            CommonUtil.formatString("%d/%d", len, CONTENT_LIMIT)
    }

    /**
     * 异步发送数据
     */
    private fun postAsynsRequest(feedbackContent: String, feedbackPhone: String) {
        val okhttpClient = OkHttpClient()
        val formBody = FormBody.Builder() //创建表单请求体
        formBody.add(KEY_FEEDBACK_CONTENT, feedbackContent)
        formBody.add(KEY_FEEDBACK_PHONE, feedbackPhone)
        val request = Request.Builder()
            .url(FEEDBACK_URL)
            .post(formBody.build())
            .build()
        val call2 = okhttpClient.newCall(request)
        call2.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                JL_Log.w(tag, "postAsynsRequest", "onFailure ---> $e")
                lifecycleScope.launch(Dispatchers.Main) {
                    showTips(getString(R.string.tips_commit_failure))
                }
            }

            override fun onResponse(call: Call, response: Response) {
                JL_Log.d(tag, "postAsynsRequest", "onResponse ---> success")
                lifecycleScope.launch(Dispatchers.Main) {
                    showTips(getString(R.string.tips_commit_success))
                    finish()
                }
            }
        })
    }

    companion object {

        private const val KEY_FEEDBACK_CONTENT = "feedbackContent"
        private const val KEY_FEEDBACK_PHONE = "feedbackPhone"

        private const val FEEDBACK_URL = "https://www.zh-jieli.com/"

        private const val CONTENT_LIMIT = 200
    }
}
