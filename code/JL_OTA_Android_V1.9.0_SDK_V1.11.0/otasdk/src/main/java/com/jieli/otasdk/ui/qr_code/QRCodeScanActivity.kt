package com.jieli.otasdk.ui.qr_code

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.MediaStore
import android.util.Patterns
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.jieli.component.utils.ToastUtil
import com.jieli.jl_bt_ota.util.JL_Log
import com.jieli.otasdk.R
import com.jieli.otasdk.databinding.ActivityQrcodeScanBinding
import com.jieli.otasdk.ui.dialog.PermissionTipsDialog
import com.king.camera.scan.AnalyzeResult
import com.king.camera.scan.CameraScan
import com.king.camera.scan.analyze.Analyzer
import com.king.wechat.qrcode.WeChatQRCodeDetector
import com.king.wechat.qrcode.scanning.WeChatCameraScanActivity
import com.king.wechat.qrcode.scanning.analyze.WeChatScanningAnalyzer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import permissions.dispatcher.NeedsPermission
import permissions.dispatcher.OnPermissionDenied
import permissions.dispatcher.OnShowRationale
import permissions.dispatcher.PermissionRequest
import permissions.dispatcher.PermissionUtils
import permissions.dispatcher.RuntimePermissions

/**
 * QRCodeScanActivity
 *
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 扫码界面
 * @since 2025/11/6
 */
@RuntimePermissions
class QRCodeScanActivity : WeChatCameraScanActivity() {

    companion object {
        const val QRCODE_HTTP = 0x09
        const val QRCODE_HTTP_URL = "QRCODE_HTTP_URL"
    }

    private val tag: String = javaClass.simpleName
    private lateinit var binding: ActivityQrcodeScanBinding
    private var tipsDialog: PermissionTipsDialog? = null

    private val selectImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult? ->
        if (result != null && result.resultCode == RESULT_OK) {
            parsePhoto(result.data)
        }
    }

    override fun initUI() {
        enableEdgeToEdge()
        binding = ActivityQrcodeScanBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom)
            insets
        }
        super.initUI()
        cameraScan.setPlayBeep(true)
        initTopBar()
    }

    override fun initCameraScan(cameraScan: CameraScan<MutableList<String>>) {
        super.initCameraScan(cameraScan)
        cameraScan.setPlayBeep(true)
    }

    override fun createAnalyzer(): Analyzer<MutableList<String>> {
        // 如果需要返回结果二维码位置信息，则初始化分析器时，isOutputVertices参数传 true 即可
        return WeChatScanningAnalyzer(true)
    }

    override fun isContentView(): Boolean {
        return false
    }

    override fun getLayoutId(): Int {
        return R.layout.activity_qrcode_scan;
    }

    override fun getFlashlightId(): Int {
        return View.NO_ID
    }

    override fun onScanResultCallback(result: AnalyzeResult<MutableList<String>>) {
        //停止分析
        cameraScan.setAnalyzeImage(false)

        handleQrResult(result.result[0])
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        onRequestPermissionsResult(requestCode, grantResults)
    }

    @NeedsPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
    fun requestStoragePermission() {
        dismissPermissionTipsDialog()
        selectImageLauncher.launch(
            Intent(
                Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            )
        )
    }

    @OnShowRationale(Manifest.permission.READ_EXTERNAL_STORAGE)
    fun showRelationForStoragePermission(request: PermissionRequest) {
        request.proceed()
    }

    @OnPermissionDenied(Manifest.permission.READ_EXTERNAL_STORAGE)
    fun onStoragePermissionDenied() {
        dismissPermissionTipsDialog()
        showTips(getString(R.string.fail_photos_system_reason))
    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    @NeedsPermission(Manifest.permission.READ_MEDIA_IMAGES)
    fun requestStoragePermissionBy33() {
        dismissPermissionTipsDialog()
        selectImageLauncher.launch(
            Intent(
                Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            )
        )
    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    @OnShowRationale(Manifest.permission.READ_MEDIA_IMAGES)
    fun showRelationForStoragePermissionBy33(request: PermissionRequest) {
        request.proceed()
    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    @OnPermissionDenied(
        Manifest.permission.READ_MEDIA_IMAGES
    )
    fun onStoragePermissionDeniedBy33() {
        dismissPermissionTipsDialog()
        showTips(getString(R.string.fail_photos_system_reason))
    }

    private fun initTopBar() {
        binding.viewTopBar.tvTopLeft.setCompoundDrawablesRelativeWithIntrinsicBounds(
            R.drawable.icon_return_white,
            0,
            0,
            0
        )
        binding.viewTopBar.tvTopLeft.setOnClickListener { finish() }
        binding.viewTopBar.tvTopTitle.setText(R.string.scan_qrcode)
        binding.viewTopBar.tvTopTitle.setTextColor(ContextCompat.getColor(this, R.color.bg_white))
        binding.viewTopBar.tvTopRight.text = getString(R.string.photos)
        binding.viewTopBar.tvTopRight.setTextColor(ContextCompat.getColor(this, R.color.bg_white))
        binding.viewTopBar.tvTopRight.setOnClickListener {
            tryToSelectPhoto()
        }
    }

    private fun showTips(content: String?) {
        JL_Log.d(tag, "showTips", content)
        ToastUtil.showToastShort(content)
    }

    private fun showPermissionTipsDialog(permission: String, desc: String) {
        if (isDestroyed || isFinishing) return
        if (PermissionUtils.hasSelfPermissions(this, permission)) return
        tipsDialog ?: PermissionTipsDialog.Builder()
            .apply {
                tips(desc)
                cancelable = true
            }.also {
                tipsDialog = it.build()
            }
        if (tipsDialog?.isShow == false) {
            tipsDialog?.show(supportFragmentManager, PermissionTipsDialog::class.simpleName)
        }
    }

    private fun dismissPermissionTipsDialog() {
        tipsDialog?.let {
            if (it.isShow) {
                it.dismiss()
            }
            tipsDialog = null
        }
    }

    private fun tryToSelectPhoto() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            showPermissionTipsDialog(
                Manifest.permission.READ_MEDIA_IMAGES,
                getString(R.string.system_set_external_storage)
            )
            requestStoragePermissionBy33WithPermissionCheck()
            return
        }
        showPermissionTipsDialog(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            getString(R.string.system_set_external_storage)
        )
        requestStoragePermissionWithPermissionCheck()
    }

    private fun parsePhoto(data: Intent?) {
        if (null == data) return
        try {
            val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, data.data)
            if (null != bitmap) {
                lifecycleScope.launch(Dispatchers.IO) {
                    val result = WeChatQRCodeDetector.detectAndDecode(bitmap)
                    runOnUiThread {
                        val text = if (result.isEmpty()) "" else result[0]
                        handleQrResult(text)
                    }
                }
                return
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        showTips(getString(R.string.not_found_qr))
    }

    private fun handleQrResult(text: String) {
        //解析json数据
        var downloadUrl: String? = null
        if (Patterns.WEB_URL.matcher(text).matches()) {
            downloadUrl = text
        }
        if (null == downloadUrl) {
            JL_Log.w(tag, "Not valid content: $text")
            showTips(text)
            //继续分析
            cameraScan.setAnalyzeImage(true)
            return
        }
        setResult(QRCODE_HTTP, Intent().apply {
            putExtra(QRCODE_HTTP_URL, downloadUrl)
        })
        finish()
    }
}