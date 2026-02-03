package com.jieli.otasdk.ui.launcher

import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.jieli.component.ActivityManager
import com.jieli.otasdk.MainApplication
import com.jieli.otasdk.R
import com.jieli.otasdk.tool.config.ConfigHelper
import com.jieli.otasdk.ui.base.BaseActivity
import com.jieli.otasdk.ui.base.WebFragment
import com.jieli.otasdk.ui.dialog.PrivacyPolicyDialog
import com.jieli.otasdk.ui.home.MainActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


/**
 * 欢迎界面
 */
class WelcomeActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)
        checkAgreePolicy()
    }

    private fun goToMainActivity() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun checkAgreePolicy() {
        val isAgree = ConfigHelper.getInstance().isAgreePolicy()
        if (isAgree) {
            MainApplication.instance.init()
            lifecycleScope.launch {
                delay(1000)
                goToMainActivity()
            }
            return
        }
        PrivacyPolicyDialog.Builder(object : PrivacyPolicyDialog.OnClickListener {
            override fun onUserService(dialog: PrivacyPolicyDialog) {
                WebFragment.goToWebFragment(
                    this@WelcomeActivity,
                    getString(R.string.user_agreement),
                    getString(R.string.user_agreement_url)
                )
            }

            override fun onPrivacyPolicy(dialog: PrivacyPolicyDialog) {
                WebFragment.goToWebFragment(
                    this@WelcomeActivity,
                    getString(R.string.privacy_policy),
                    getString(R.string.app_privacy_policy)
                )
            }

            override fun onAgree(dialog: PrivacyPolicyDialog) {
                dialog.dismiss()
                ConfigHelper.getInstance().setAgreePolicyVersion(this@WelcomeActivity)
                MainApplication.instance.init()
                checkAgreePolicy()
            }

            override fun onDisagree(dialog: PrivacyPolicyDialog) {
                dialog.dismiss()
                this@WelcomeActivity.finish()
                ActivityManager.getInstance().popAllActivity()
            }
        }).build().show(supportFragmentManager, PrivacyPolicyDialog::class.simpleName)
    }
}
