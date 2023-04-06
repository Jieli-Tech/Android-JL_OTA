package com.jieli.otasdk.dialog;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.jieli.jl_bt_ota.constant.ErrorCode;
import com.jieli.jl_bt_ota.constant.JL_Constant;
import com.jieli.jl_bt_ota.util.JL_Log;
import com.jieli.otasdk.R;
import com.jieli.otasdk.base.BaseDialogFragment;
import com.jieli.otasdk.databinding.FragmentDialogOtaBinding;
import com.jieli.otasdk.model.ota.OTAEnd;
import com.jieli.otasdk.model.ota.OTAState;
import com.jieli.otasdk.model.ota.OTAWorking;
import com.jieli.otasdk.tool.config.ConfigHelper;
import com.jieli.otasdk.viewmodel.OTAViewModel;

import java.util.Locale;

/**
 * OTA进度提示
 */
public class DialogOTA extends BaseDialogFragment {
    private int testOTAType = ConfigHelper.Companion.getInstance().isAutoTest() ? 1 : 0;//0：简单的单次测试 1：自动化测试OTA
    private FragmentDialogOtaBinding mBinding;
    private OTAViewModel otaViewModel = null;
    private String TAG = this.getClass().getSimpleName();
    private boolean isOTAError = false;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        //设置dialog的基本样式参数
        requireDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);
        Window window = requireDialog().getWindow();
        if (window != null) {
            //去掉dialog默认的padding
            window.getDecorView().setPadding(0, 0, 0, 0);
            WindowManager.LayoutParams layoutParams = window.getAttributes();
            layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
            layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
            layoutParams.gravity = Gravity.BOTTOM;
            window.setAttributes(layoutParams);
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        mBinding = FragmentDialogOtaBinding.inflate(inflater, container, false);
        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        otaViewModel = new ViewModelProvider(requireActivity()).get(OTAViewModel.class);
        if (testOTAType == 0) {
            initUISingleTest();
        } else {
            initUIAutoTest();
        }
        observeCallback();
        mBinding.tvSureBtn.setOnClickListener(v -> {
            otaViewModel.getOtaStateMLD().setValue(null);
            dismiss();
        });
    }

    //单次测试
    private void initUISingleTest() {
        mBinding.groupAutoTestTitle.setVisibility(View.GONE);
        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) mBinding.clOtaContent.getLayoutParams();
        layoutParams.height = getPixelsFromDp(148);
    }

    //自动化测试OTA
    private void initUIAutoTest() {
        mBinding.groupAutoTestTitle.setVisibility(View.VISIBLE);
        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) mBinding.clOtaContent.getLayoutParams();
        layoutParams.height = getPixelsFromDp(223);
    }

    //增加观察者
    private void observeCallback() {
        otaViewModel.getOtaStateMLD().observe(getViewLifecycleOwner(), otaState -> {
            if (otaState == null) return;
            JL_Log.d(TAG, "otaStateMLD : >>>" + otaState);
            switch (otaState.getState()) {
                case OTAState.OTA_STATE_START://OTA开始
                    requireActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    mBinding.groupUpgrade.setVisibility(View.VISIBLE);
                    mBinding.groupScanDeviceLoading.setVisibility(View.GONE);
                    mBinding.tvUpgradeProgress.setText(getString(R.string.ota_check_file) + "  " + "0%");
                    mBinding.pbUpgradeProgress.setProgress(0);
                    break;
                case OTAState.OTA_STATE_RECONNECT:
                    mBinding.groupUpgrade.setVisibility(View.GONE);
                    mBinding.groupScanDeviceLoading.setVisibility(View.VISIBLE);
                    mBinding.tvOtaFileName.setVisibility(View.GONE);
                    mBinding.tvUpgradeProgress.setText(getString(R.string.ota_upgrading) + "  " + "0%");
                    mBinding.pbUpgradeProgress.setProgress(0);
                    mBinding.tvScanDeviceLoading.setText(R.string.verification_file_completed);
                    break;
                case OTAState.OTA_STATE_WORKING://OTA进行中
                    OTAWorking otaWorking = (OTAWorking) otaState;
                    String message = otaWorking.getType() == JL_Constant.TYPE_CHECK_FILE ? getString(R.string.ota_check_file) : getString(R.string.ota_upgrading);
                    int progress = Math.round(otaWorking.getProgress());
                    mBinding.groupUpgrade.setVisibility(View.VISIBLE);
                    mBinding.groupScanDeviceLoading.setVisibility(View.GONE);
                    if (testOTAType == 1) {
                        mBinding.tvOtaFileName.setVisibility(View.VISIBLE);
                    } else {
                        mBinding.tvOtaFileName.setVisibility(View.GONE);
                    }
                    mBinding.tvUpgradeProgress.setText(message + "  " + progress + "%");
                    mBinding.pbUpgradeProgress.setProgress(progress);
                    break;
                case OTAState.OTA_STATE_IDLE://OTA结束
                    requireActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    boolean isUpgradeSuccess = false;
                    OTAEnd otaEnd = (OTAEnd) otaState;
                    switch (otaEnd.getCode()) {
                        case ErrorCode.ERR_NONE://升级完成
                            mBinding.tvUpgradeProgress.setText(getString(R.string.ota_upgrading) + "  " + 100 + "%");
                            mBinding.pbUpgradeProgress.setProgress(100);
                            // TODO: 2022/11/1  单词升级结束
                            isUpgradeSuccess = true;
                            break;
                        case ErrorCode.ERR_UNKNOWN://升级取消
                            break;
                        default://升级失败
                            if (otaEnd.getCode() == ErrorCode.SUB_ERR_OTA_IN_HANDLE) {//正在升级中
                                return;
                            } else if (otaEnd.getCode() == ErrorCode.SUB_ERR_DATA_NOT_FOUND) {//未找到升级文件
                                otaViewModel.readFileList();
                            }
                            String otaMsg = String.format(Locale.getDefault(), "code:%d, %s", otaEnd.getCode(), otaEnd.getMessage());
                            mBinding.tvUpgradeResultReason.setText(getString(R.string.ota_reason) + otaMsg);
                            isOTAError = true;
                            break;
                    }
                    if (testOTAType == 0) {//单次升级
                        mBinding.groupScanDeviceLoading.setVisibility(View.GONE);
                        mBinding.groupUpgrade.setVisibility(View.GONE);
                        mBinding.groupUpgradeResult.setVisibility(View.VISIBLE);
                        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) mBinding.clOtaContent.getLayoutParams();
                        if (isUpgradeSuccess) {
                            mBinding.ivUpgradeResultLogo.setImageResource(R.drawable.ic_success_big);
                            mBinding.tvUpgradeResultTip.setText(R.string.update_finish);
                            layoutParams.height = getPixelsFromDp(196);
                        } else {
                            mBinding.ivUpgradeResultLogo.setImageResource(R.drawable.ic_fail_big);
                            mBinding.tvUpgradeResultTip.setText(R.string.update_failed);
                            mBinding.tvUpgradeResultReason.setVisibility(View.VISIBLE);
                            layoutParams.height = FrameLayout.LayoutParams.WRAP_CONTENT;
                        }
                        mBinding.clOtaContent.setLayoutParams(layoutParams);
                    }
                    break;

            }
        });
    }

    private int getPixelsFromDp(int size) {
        DisplayMetrics metrics = new DisplayMetrics();
        this.getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);
        return (size * metrics.densityDpi) / DisplayMetrics.DENSITY_DEFAULT;
    }
}