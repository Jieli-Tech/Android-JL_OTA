package com.jieli.otasdk.activities

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import com.jieli.jl_dialog.Jl_Dialog
import com.jieli.otasdk.R
import com.jieli.otasdk.base.BaseActivity
import com.jieli.otasdk.util.OtaFileObserverHelper


/**
 * 欢迎界面
 */
class WelcomeActivity : BaseActivity() {
    private val mUIHandler = Handler(Looper.getMainLooper())
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)
        mUIHandler.postDelayed({
            goToMainActivity()
        }, 1000)
    }

    private fun goToMainActivity() {
        OtaFileObserverHelper.getInstance().startObserver()
        if (isTaskRoot) {//是不是任务栈的第一个任务
            startActivity(Intent(this, MainActivity::class.java))
        }
        finish()
    }
}
