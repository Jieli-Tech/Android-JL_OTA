package com.jieli.otasdk.ui.base

import android.annotation.SuppressLint
import android.content.Context
import android.net.http.SslError
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.SslErrorHandler
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.os.bundleOf
import com.jieli.jl_bt_ota.util.JL_Log
import com.jieli.otasdk.R
import com.jieli.otasdk.databinding.FragmentWebBinding


/**
 * @author zqjasonZhong
 * @since 2024/8/5
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 网页界面
 */
class WebFragment : BaseFragment() {

    companion object {
        const val KEY_TITLE = "title"
        const val KEY_URL = "url"

        fun goToWebFragment(context: Context, title: String, url: String) {
            ContentActivity.startContentActivity(
                context, WebFragment::class.java.canonicalName, bundleOf(
                    Pair(KEY_TITLE, title),
                    Pair(KEY_URL, url)
                )
            )
        }
    }

    private lateinit var binding: FragmentWebBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        FragmentWebBinding.inflate(inflater, container, false).also {
            binding = it
            return it.root
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val title = arguments?.getString(KEY_TITLE) ?: ""
        val url = arguments?.getString(KEY_URL)
        if (null == url) {
            requireActivity().finish()
            return
        }
        binding.viewToolBar.tvTopLeft.setCompoundDrawablesRelativeWithIntrinsicBounds(
            R.drawable.ic_return,
            0,
            0,
            0
        )
        binding.viewToolBar.tvTopLeft.setOnClickListener {
            finish(0)
        }
        binding.viewToolBar.tvTopTitle.text = title
        binding.webContainer.webViewClient = object : WebViewClient() {

            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                request?.url?.let {
                    JL_Log.d(TAG, "shouldOverrideUrlLoading", "url : $it")
                    view?.loadUrl(it.toString())
                }
                return true
            }

            override fun onLoadResource(view: WebView?, url: String?) {
                super.onLoadResource(view, url)
                JL_Log.d(TAG, "onLoadResource", "url : $url")
            }

            @SuppressLint("WebViewClientOnReceivedSslError")
            override fun onReceivedSslError(
                view: WebView?,
                handler: SslErrorHandler?,
                error: SslError?
            ) {
                JL_Log.d(TAG, "onReceivedSslError", "error : $error")
//                super.onReceivedSslError(view, handler, error)
                handler?.proceed()
            }
        }
        authorization()
        binding.webContainer.loadUrl(url)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun authorization() {
        binding.webContainer.settings.apply {
            defaultTextEncodingName = "UTF-8"
            userAgentString = "User-Agent:Android"
            javaScriptEnabled = true
            javaScriptCanOpenWindowsAutomatically = true
            domStorageEnabled = true
            databaseEnabled = true
            allowFileAccess = true
            savePassword = true
            setSupportZoom(true)
            useWideViewPort = true
        }
    }
}