package com.jieli.otasdk.fragments

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.annotation.RequiresApi
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.jieli.component.utils.ToastUtil
import com.jieli.component.utils.ValueUtil
import com.jieli.jl_bt_ota.util.JL_Log
import com.jieli.otasdk.base.BaseFragment
import com.jieli.otasdk.databinding.FragmentQrCodeBinding
import com.jieli.otasdk.util.AppUtil
import me.devilsen.czxing.code.BarcodeFormat
import me.devilsen.czxing.code.BarcodeReader
import me.devilsen.czxing.util.BitmapUtil
import me.devilsen.czxing.util.SoundPoolUtil
import me.devilsen.czxing.view.ScanListener
import permissions.dispatcher.*
import com.jieli.otasdk.R
import java.util.*

/**
 *
 * @ClassName:      QrCodeFragment
 * @Description:     java类作用描述
 * @Author:         ZhangHuanMing
 * @CreateDate:     2023/4/25 9:23
 */
@RuntimePermissions
class QrCodeFragment : BaseFragment() {
    private lateinit var mScanBinding:FragmentQrCodeBinding
    private var mIsHasStoragePermission = false
    private val mHandler = Handler(Looper.getMainLooper())
    private var mSoundPoolUtil: SoundPoolUtil? = null
    private var isLightOpen = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mIsHasStoragePermission = AppUtil.isHasStoragePermission(context)
    }
    private val mSelectImageLauncher =
        registerForActivityResult(StartActivityForResult()) { result ->
            if (result != null && result.resultCode === Activity.RESULT_OK) {
                decodeImage(result.data)
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        mScanBinding = FragmentQrCodeBinding.inflate(inflater,container,false)
        return mScanBinding.root
    }

    override fun onResume() {
        super.onResume()
        mScanBinding.svQrScan.openCamera()
        mScanBinding.svQrScan.startScan()
        isLightOpen = false
        updateLightUI(false)

    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        mSoundPoolUtil = SoundPoolUtil()
        mSoundPoolUtil?.loadDefault(requireContext())
        initTopBar()
        initScanView()
        mScanBinding.svQrScan.isBanZoom = true
    }
    override fun onPause() {
        super.onPause()
        mScanBinding.svQrScan.stopScan()
        mScanBinding.svQrScan.closeCamera()
        isLightOpen = false
    }
    override fun onDestroy() {
        mScanBinding.svQrScan.onDestroy()
        mSoundPoolUtil!!.release()
        mHandler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        onRequestPermissionsResult(requestCode, grantResults)
    }
    private fun initTopBar() {
        mScanBinding.clQrScanTopbar.tvTopLeft.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.icon_return_white,0,0,0)
        mScanBinding.clQrScanTopbar.tvTopLeft.setOnClickListener { v -> requireActivity().finish() }
        mScanBinding.clQrScanTopbar.tvTopTitle.setText(R.string.scan_qrcode)
        mScanBinding.clQrScanTopbar.tvTopTitle.setTextColor(resources.getColor(R.color.bg_white))
        mScanBinding.clQrScanTopbar.tvTopRight.setText(getString(R.string.photos))
        mScanBinding.clQrScanTopbar.tvTopRight.setTextColor(resources.getColor(R.color.bg_white))
        mScanBinding.clQrScanTopbar.tvTopRight.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                selectPhotoFromAlbumBy33WithPermissionCheck()
                return@setOnClickListener
            }
            selectPhotoFromAlbumWithPermissionCheck()
        }
    }
    private fun initScanView() {
        val resources = resources
        val scanColors = Arrays.asList<Int>(
            resources.getColor(R.color.blue_scan_side),
            resources.getColor(R.color.blue_scan_partial),
            resources.getColor(R.color.blue_scan_middle)
        )
        val scanBox = mScanBinding.svQrScan.scanBox
        scanBox.setMaskColor(resources.getColor(R.color.gray_transparent_9C272626))
        scanBox.setBoxTopOffset(-ValueUtil.dp2px(requireContext(), 100))
        scanBox.setBorderSize(
            ValueUtil.dp2px(requireContext(), 240),
            ValueUtil.dp2px(requireContext(), 240)
        )
        scanBox.setCornerColor(resources.getColor(R.color.blue_558CFF))
        scanBox.setScanLineColor(scanColors)
        scanBox.invisibleFlashLightIcon()
        scanBox.setScanNoticeText(getString(R.string.qrcode_into_box))
        mScanBinding.svQrScan.setScanListener(object : ScanListener {
            override fun onScanSuccess(result: String, format: BarcodeFormat) {
                mSoundPoolUtil!!.play()
                handleQrResult(result)
            }

            override fun onOpenCameraError() {
                requireActivity().finish()
            }
        })
        mScanBinding.svQrScan.setAnalysisBrightnessListener { isDark ->
            mHandler.post {
                if (isDark) {
                    if (!isLightOpen) {
//                        ToastUtil.showToastShort(R.string.scan_env_dark_tips)
                    }
                } else if (isLightOpen) {
//                    ToastUtil.showToastShort(R.string.scan_env_light_tips)
                }
            }
        }
    }
    private fun updateLightUI(isOpen: Boolean) {//暂不做灯开关
    }
    private fun decodeImage(intent: Intent?) {
        if (intent == null) return
        val selectImageUri = intent.data ?: return
        val filePathColumn = arrayOf(MediaStore.Images.Media.DATA)
        val cursor =
            requireActivity().contentResolver.query(
                selectImageUri,
                filePathColumn,
                null,
                null,
                null
            )
                ?: return
        cursor.moveToFirst()
        val columnIndex = cursor.getColumnIndex(filePathColumn[0])
        val picturePath = cursor.getString(columnIndex)
        cursor.close()
        val bitmap = BitmapUtil.getDecodeAbleBitmap(picturePath) ?: return
        val result = BarcodeReader.getInstance().read(bitmap)
        if (result == null) {
//            ToastUtil.showToastShort("未发现二维码")
        } else {
            handleQrResult(result.text)
        }
    }


    @SuppressLint("MissingPermission")
    private fun handleQrResult(text: String) {
        //解析json数据
        val gson: Gson = GsonBuilder().setLenient().create()
        var downloadUrl: String ?= null
        if(Patterns.WEB_URL.matcher(text).matches()/*&&(text.endsWith(".ufw")||text.endsWith(".UFW"))*/){
            downloadUrl = text
        }
        if (downloadUrl != null) {
            val intent = Intent()
            intent.putExtra(QRCODE_HTTP_URL,downloadUrl)
            requireActivity().setResult(QRCODE_HTTP,intent)
            requireActivity().finish()
        } else {
            JL_Log.w(tag, "Not valid content: $text")
            mHandler.post(Runnable {
                ToastUtil.showToastShort(text)
                mScanBinding.svQrScan.resetZoom()
                mScanBinding.svQrScan.startScan()
            })
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    @NeedsPermission(Manifest.permission.READ_MEDIA_IMAGES)
    fun selectPhotoFromAlbumBy33() {
        JL_Log.d(tag, "selectPhotoFromAlbumBy33 >>>>")
        mSelectImageLauncher.launch(
            Intent(
                Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            )
        )
    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    @OnShowRationale(Manifest.permission.READ_MEDIA_IMAGES)
    fun showRelationForReadStorageBy33(request: PermissionRequest) {
        request.proceed()
    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    @OnPermissionDenied(Manifest.permission.READ_MEDIA_IMAGES)
    fun onReadStorageDeniedBy33() {
        ToastUtil.showToastShort(getString(R.string.fail_photos_system_reason))
    }

    @NeedsPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
    fun selectPhotoFromAlbum() {
        JL_Log.d(tag, "selectPhotoFromAlbum >>>>")
        mSelectImageLauncher.launch(
            Intent(
                Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            )
        )
    }

    @OnShowRationale(Manifest.permission.READ_EXTERNAL_STORAGE)
    fun showRelationForReadStorage(request: PermissionRequest) {
        request.proceed()
    }

    @OnPermissionDenied(Manifest.permission.READ_EXTERNAL_STORAGE)
    fun onReadStorageDenied() {
        ToastUtil.showToastShort(getString(R.string.fail_photos_system_reason))
    }
    companion object{
        public  val QRCODE_HTTP = 0x09
        public  val QRCODE_HTTP_URL ="QRCODE_HTTP_URL"
    }
}
