package com.jieli.otasdk.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.jieli.component.utils.SystemUtil
import com.jieli.jl_bt_ota.util.JL_Log
import com.jieli.otasdk.R
import com.jieli.otasdk.databinding.ActivityMainBinding
import com.jieli.otasdk.ui.base.BaseActivity
import com.jieli.otasdk.ui.device.ScanFragment
import com.jieli.otasdk.ui.dialog.TipsDialog
import com.jieli.otasdk.ui.ota.OtaFragment
import com.jieli.otasdk.ui.settings.ConfigViewModel
import com.jieli.otasdk.ui.settings.SettingsFragment
import com.jieli.otasdk.util.UIHelper
import com.jieli.otasdk_autotest.ui.auto_ota.OtaAutoTestFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * @author zqjasonZhong
 * @since 2025/1/13
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 主界面
 */
class MainActivity : BaseActivity() {

    companion object {
        /**
         * 连接界面
         */
        const val TAB_CONNECT = 0

        /**
         * 更新界面
         */
        const val TAB_UPDATE = 1

        /**
         * 设置界面
         */
        const val TAB_SETTING = 2
    }

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: MainViewModel
    private lateinit var configViewModel: ConfigViewModel

    var isSkipDestroyViewModel: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        viewModel = ViewModelProvider(this)[MainViewModel::class.java]
        configViewModel = ViewModelProvider(this)[ConfigViewModel::class.java]
        initUI()
        switchSubFragment(if (viewModel.isConnected()) TAB_UPDATE else TAB_CONNECT, true)
    }

    override fun onResume() {
        super.onResume()
        viewModel.startWebService(this)
    }

    override fun onPause() {
        super.onPause()
        viewModel.stopWebService(this)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        //  弹窗询问保存的文件名
        handleIntent(intent)
    }

//    override fun onBackPressed() {
//        if (!AppUtil.isFastDoubleClick()) {
//            ToastUtil.showToastShort(R.string.double_tap_to_exit)
//        } else {
//            super.onBackPressed()
//        }
//    }

    override fun onDestroy() {
        super.onDestroy()
        if (!isSkipDestroyViewModel) {
            viewModel.destroy()
        } else {
            isSkipDestroyViewModel = false
        }
    }

    fun switchSubFragment(itemIndex: Int) {
        if (!isValidActivity()) return
        val selectedItemId = when (itemIndex) {
            TAB_UPDATE -> R.id.action_upgrade
            TAB_SETTING -> R.id.action_setting
            else -> R.id.action_connect
        }
        binding.bnvMainBottom.selectedItemId = selectedItemId
        binding.vp2Container.setCurrentItem(itemIndex, true)
    }

    private fun switchSubFragment(itemIndex: Int, smoothScroll: Boolean) {
        if (!isValidActivity()) return
        binding.vp2Container.setCurrentItem(itemIndex, smoothScroll)
    }

    private fun initUI() {
        val appVersion = SystemUtil.getVersioName(applicationContext)
        val appVersionCode = SystemUtil.getVersion(applicationContext)
        JL_Log.i(
            TAG,
            "initUI",
            "App Version : $appVersion($appVersionCode)"
        )
        val isAutoTest = configViewModel.isAutoTest()
        val fragments = if (isAutoTest) {
            arrayOf<Fragment>(ScanFragment(), OtaAutoTestFragment(), SettingsFragment())
        } else {
            arrayOf<Fragment>(ScanFragment(), OtaFragment(), SettingsFragment())
        }
        binding.vp2Container.also {
            it.isUserInputEnabled = false
            it.offscreenPageLimit = 3
            it.adapter = object : FragmentStateAdapter(this) {
                override fun getItemCount(): Int {
                    return fragments.size
                }

                override fun createFragment(position: Int): Fragment {
                    return fragments[position]
                }
            }
        }
        val childView: View = binding.vp2Container.getChildAt(0)
        (childView as? RecyclerView)?.overScrollMode = View.OVER_SCROLL_NEVER

        binding.bnvMainBottom.itemIconTintList = null//不隐藏显示不出原来的logo
        binding.bnvMainBottom.setOnItemSelectedListener { menuItem ->
            var isJumpSwitchFragment = false
            when (menuItem.itemId) {
                R.id.action_connect, R.id.action_upgrade -> {
                    if (configViewModel.isChangeConfig()) {//未保存设置
                        isJumpSwitchFragment = true
                    }
                }
            }
            if (isJumpSwitchFragment) {
                lifecycleScope.launch(Dispatchers.Main) { //要延时，否则刷新失败
                    delay(50)
                    binding.bnvMainBottom.selectedItemId = R.id.action_setting
                    TipsDialog.Builder()
                        .content(getString(R.string.setting_no_save))
                        .cancelBtn(getString(R.string.ignore)) { dialog, _ ->
                            dialog.dismiss()
                            configViewModel.saveConfiguration(false)
                        }
                        .confirmBtn(getString(R.string.save)) { dialog, _ ->
                            dialog.dismiss()
                            configViewModel.saveConfiguration(true)
                        }.also {
                            it.cancelable = false
                        }.build().show(supportFragmentManager, TipsDialog::class.simpleName)
                }
                return@setOnItemSelectedListener true
            }
            val itemIndex = when (menuItem.itemId) {
                R.id.action_upgrade -> TAB_UPDATE
                R.id.action_setting -> TAB_SETTING
                else -> TAB_CONNECT
            }
            switchSubFragment(itemIndex, false)
            return@setOnItemSelectedListener true
        }
    }

    private fun handleIntent(intent: Intent?) {
        intent?.data?.let {
            UIHelper.showSaveFileDialog(this, supportFragmentManager, it)
        }
    }

}
