package com.jieli.otasdk.activities

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.jieli.component.utils.SystemUtil
import com.jieli.component.utils.ToastUtil
import com.jieli.jlFileTransfer.FileUtils
import com.jieli.jlFileTransfer.TransferFolder
import com.jieli.jlFileTransfer.TransferFolderCallback
import com.jieli.jlFileTransfer.WebService
import com.jieli.jl_bt_ota.util.JL_Log
import com.jieli.otasdk.MainApplication
import com.jieli.otasdk.R
import com.jieli.otasdk.base.BaseActivity
import com.jieli.otasdk.dialog.DialogTip
import com.jieli.otasdk.dialog.DialogTipClickListener
import com.jieli.otasdk.fragments.*
import com.jieli.otasdk.tool.config.ConfigHelper
import com.jieli.otasdk.util.FileTransferUtil
import com.jieli.otasdk.viewmodel.ConfigViewModel
import com.jieli.otasdk.viewmodel.MainViewModel
import com.jieli.otasdk_autotest.fragments.OtaAutoTestFragment
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.view_top_bar.view.*
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException


class MainActivity : BaseActivity() {

    private lateinit var viewModel: MainViewModel
    private lateinit var configViewModel: ConfigViewModel
    private val mHandler: Handler = Handler(Looper.getMainLooper())
    var isSkipDestroyViewModel: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)
        setContentView(R.layout.activity_main)
        viewModel = ViewModelProvider(this).get(MainViewModel::class.java)
        configViewModel = ViewModelProvider(this).get(ConfigViewModel::class.java)
        initUI()
        initViewModel()
        view_main_top_bar.tv_top_title.text = getString(R.string.upgrade)
        switchSubFragment(if (viewModel.isConnected()) 1 else 0, true)
        /*if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            == PackageManager.PERMISSION_GRANTED
        ) {
            Log.d(TAG, "onCreate: 获取到写权限")
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
            == PackageManager.PERMISSION_GRANTED
        ) {
            Log.d(TAG, "onCreate: 获取到读权限")
        }*/
    }

    override fun onResume() {
        super.onResume()
        //开启传文件服务
        val folderList = ArrayList<TransferFolder>()
        folderList.add(TransferFolder().run {
            this.id = 0
            this.folder = File(MainApplication.getOTAFileDir())
            this.describe = "升级文件"
            this.fileType = ".ufw"
            this.callback = object : TransferFolderCallback {
                override fun onCreateFile(file: File?): Boolean {
                    return true;
                }

                override fun onDeleteFile(file: File?): Boolean {
                    return file?.delete() == true;
                }
            }
            this
        })
        folderList.add(TransferFolder().run {
            this.id = 1
            this.folder = File(MainApplication.getLogFileDir())
            this.describe = "Log文件"
            this.fileType = ".txt"
            this.callback = object : TransferFolderCallback {
                override fun onCreateFile(file: File?): Boolean {
                    return true;
                }

                override fun onDeleteFile(file: File?): Boolean {
                    return file?.delete() == true;
                }
            }
            this
        })
        WebService.setTransferFolderList(folderList)
        WebService.start(this)
    }

    override fun onPause() {
        super.onPause()
        WebService.stop(this)
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
        if (isValidActivity()) {
            var selectedItemId = R.id.action_connect
            when (itemIndex) {
                0 -> {
                    selectedItemId = R.id.action_connect
                }
                1 -> {
                    selectedItemId = R.id.action_upgrade
                }
                2 -> {
                    selectedItemId = R.id.action_setting
                }
            }
            bnv_main_bottom.selectedItemId = selectedItemId
            vp2_container?.setCurrentItem(itemIndex, true)
        }
    }

    private fun switchSubFragment(itemIndex: Int, smoothScroll: Boolean) {
        if (isValidActivity()) {
            vp2_container?.setCurrentItem(itemIndex, smoothScroll)
            var rightTextVisibility = true
            if (itemIndex == 0) {
                rightTextVisibility = ConfigHelper.getInstance().isEnableBroadcastBox()
                view_main_top_bar.tv_top_right.isClickable = true
                view_main_top_bar.tv_top_right.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    R.drawable.ic_function_switch,
                    0,
                    0,
                    0
                )
            }
            view_main_top_bar.tv_top_right.visibility =
                if (rightTextVisibility) {
                    View.VISIBLE
                } else View.INVISIBLE
        }
    }

    private fun initUI() {
        val appVersion = SystemUtil.getVersioName(applicationContext)
        JL_Log.i(tag, "appVersion = $appVersion")
        view_main_top_bar.tv_top_right.setOnClickListener {
            val index = vp2_container.currentItem
            if (0 == index) {
                switchPopupWindow(this@MainActivity, it)
            } else if (2 == index) {
                configViewModel.saveSettingMLD.value = true
            }
        }
        val isAutoTest = ConfigHelper.Companion.getInstance().isAutoTest()
        val fragments = if (isAutoTest) {
            arrayOf<Fragment>(ScanFragment(), OtaAutoTestFragment(), SettingsFragment())
        } else {
            arrayOf<Fragment>(ScanFragment(), OtaFragment(), SettingsFragment())
        }
        vp2_container.isUserInputEnabled = false
        vp2_container.offscreenPageLimit = 3
        vp2_container.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount(): Int {
                return fragments.size
            }

            override fun createFragment(position: Int): Fragment {
                return fragments[position]
            }
        }
        val childView: View = vp2_container.getChildAt(0)
        (childView as? RecyclerView)?.overScrollMode = View.OVER_SCROLL_NEVER

        bnv_main_bottom.itemIconTintList = null//不隐藏显示不出原来的logo
        bnv_main_bottom.setOnItemSelectedListener { menuItem ->
            var isJumpSwitchFragment = false;
            when (menuItem.itemId) {
                R.id.action_connect, R.id.action_upgrade -> {
                    if (configViewModel.configChangeMLD.value == true && !configViewModel.isIgnoreSaveSetting) {//未保存设置
                        isJumpSwitchFragment = true
                        mHandler.postDelayed({//要延时，否则刷新失败
                            bnv_main_bottom.selectedItemId = R.id.action_setting
                        }, 10)
                        val saveDialog = DialogTip().run {
                            this.title = this@MainActivity.getString(R.string.setting_no_save)
                            this.leftText = this@MainActivity.getString(R.string.ignore)
                            this.rightText = this@MainActivity.getString(R.string.save)
                            this.dialogClickListener = object : DialogTipClickListener {
                                override fun rightBtnClick() {
                                    configViewModel.saveSettingMLD.setValue(true)
                                    this@run.dismiss()
                                }

                                override fun leftBtnClick() {
                                    configViewModel.isIgnoreSaveSetting = true
                                    this@MainActivity.bnv_main_bottom.selectedItemId =
                                        R.id.action_setting
                                    this@run.dismiss()
                                }
                            }
                            this
                        }
                        saveDialog.isCancelable = false
                        saveDialog.show(this.supportFragmentManager, "saveDialog")
                    }
                }
            }
            var funcName = ""
            var saveName = ""
            var itemIndex = 0
            var resId = 0
            when (menuItem.itemId) {
                R.id.action_connect -> {
                    funcName = getString(R.string.connect)
                    itemIndex = 0
                    resId = R.drawable.ic_function_switch
                }
                R.id.action_upgrade -> {
                    funcName = getString(R.string.upgrade)
                    itemIndex = 1
                }
                R.id.action_setting -> {
                    funcName = getString(R.string.setting)
                    itemIndex = 2
                    saveName = getString(R.string.save)
                }
            }
            if (!isJumpSwitchFragment) {
                switchSubFragment(itemIndex, false)
            }
            view_main_top_bar.tv_top_right.setCompoundDrawablesRelativeWithIntrinsicBounds(
                resId,
                0,
                0,
                0
            )
            view_main_top_bar.tv_top_right.text = saveName
            view_main_top_bar.tv_top_title.text = funcName
            return@setOnItemSelectedListener true
        }
    }

    private fun initViewModel(): Unit {
        configViewModel.configChangeMLD.observe(this, Observer {
            it?.run {
                if (it) {
                    view_main_top_bar.tv_top_right.isClickable = true
                    view_main_top_bar.tv_top_right.setTextColor(resources.getColor(R.color.blue_398BFF))
                } else {
                    val index = vp2_container.currentItem
                    if (index != 0) {
                        view_main_top_bar.tv_top_right.isClickable = false
                        view_main_top_bar.tv_top_right.setTextColor(resources.getColor(R.color.black_242424))
                    }
                }
            }
        })
    }

    private fun handleIntent(intent: Intent?): Unit {
        intent?.data?.let {
            try {
                contentResolver?.let { contentResolver ->
                    val parentFilePath = MainApplication.getOTAFileDir()
                    var fileName = FileUtils.getFileName(this@MainActivity, it)
                    fileName =
                        FileTransferUtil.getNewUpgradeFileName(fileName, File(parentFilePath))
                    val saveFileDialog = DialogInputText().run {
                        this.title = "保存文件"
                        this.content = fileName
                        this.leftText = "取消"
                        this.rightText = "保存"
                        this.dialogClickListener = object : DialogClickListener {
                            override fun rightBtnClick(inputText: String?) {
                                var inputFileNameStr: String = (inputText ?: "").trim()
                                if (!inputFileNameStr.toUpperCase().endsWith(".UFW")) {
                                    ToastUtil.showToastShort("请以xxx.ufw格式命名文件")
                                    return
                                }
                                val resultPath = parentFilePath + File.separator + inputFileNameStr
                                if (File(resultPath).exists()) {
                                    ToastUtil.showToastShort("该文件名已存在")
                                    return
                                } else {
                                    try {
                                        val inputStream =
                                            contentResolver.openInputStream(intent.data!!)
                                        FileUtils.copyFile(
                                            inputStream,
                                            resultPath
                                        )
                                        Toast.makeText(
                                            this@MainActivity,
                                            R.string.please_refresh_web,
                                            Toast.LENGTH_LONG
                                        ).show()
                                    } catch (e: FileNotFoundException) {
                                        Toast.makeText(
                                            this@MainActivity,
                                            R.string.upload_failed,
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                                this@run.dismiss()
                            }

                            override fun leftBtnClick(inputText: String?) {
                                this@run.dismiss()
                            }
                        }
                        this
                    }
                    saveFileDialog.show(this.supportFragmentManager, "scanFilterDialog")
                }
            } catch (e: IOException) {
                Toast.makeText(this, R.string.read_file_failed, Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }
}
