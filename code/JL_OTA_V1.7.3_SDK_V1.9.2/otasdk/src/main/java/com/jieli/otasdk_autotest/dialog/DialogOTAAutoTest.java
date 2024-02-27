package com.jieli.otasdk_autotest.dialog;

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
import com.jieli.otasdk_autotest.model.auto.TestFinish;
import com.jieli.otasdk_autotest.model.auto.TestParam;
import com.jieli.otasdk_autotest.model.auto.TestState;
import com.jieli.otasdk_autotest.model.auto.TestTaskEnd;
import com.jieli.otasdk_autotest.model.auto.TestTaskStart;
import com.jieli.otasdk.model.ota.OTAEnd;
import com.jieli.otasdk.model.ota.OTAState;
import com.jieli.otasdk.model.ota.OTAWorking;
import com.jieli.otasdk.util.AppUtil;
import com.jieli.otasdk_autotest.tool.auto.TestTask;
import com.jieli.otasdk_autotest.viewmodel.OTAAutoTestViewModel;

import java.util.Locale;

/**
 * OTA进度提示-自动化测试
 */
public class DialogOTAAutoTest extends BaseDialogFragment {
    private FragmentDialogOtaBinding mBinding;
    private OTAAutoTestViewModel otaViewModel = null;
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
        otaViewModel = new ViewModelProvider(requireActivity()).get(OTAAutoTestViewModel.class);
        initUIAutoTest();
        observeCallback();
        mBinding.tvSureBtn.setOnClickListener(v -> {
            otaViewModel.getTestStateMLD().setValue(null);
            otaViewModel.getOtaStateMLD().setValue(null);
            dismiss();
        });
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
                    if (!otaViewModel.isAutoTest()) {
                        requireActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    }
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
                    mBinding.tvOtaFileName.setVisibility(View.VISIBLE);
                    mBinding.tvUpgradeProgress.setText(message + "  " + progress + "%");
                    mBinding.pbUpgradeProgress.setProgress(progress);
                    break;
                case OTAState.OTA_STATE_IDLE://OTA结束
                    if (!otaViewModel.isAutoTest()) {
                        requireActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    }
                    OTAEnd otaEnd = (OTAEnd) otaState;
                    switch (otaEnd.getCode()) {
                        case ErrorCode.ERR_NONE://升级完成
                            mBinding.tvUpgradeProgress.setText(getString(R.string.ota_upgrading) + "  " + 100 + "%");
                            mBinding.pbUpgradeProgress.setProgress(100);
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
                    break;
            }
        });
        otaViewModel.getTestStateMLD().observe(getViewLifecycleOwner(), testState -> {
            if (testState == null) return;
            JL_Log.d(TAG, "testStateMLD : >>> " + testState);
            TestParam testParam = otaViewModel.getTestParam();
            if (testParam != null) {
                int testCount = (int) Math.ceil(((double) testParam.getTotal()) / 2);
                switch (testState.getState()) {
                    case TestState.TEST_STATE_WORKING: { //测试开始
                        requireActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);//保持屏幕常亮
                        mBinding.groupAutoTestTitle.setVisibility(View.VISIBLE);
                        mBinding.tvAutoTestTitle.setText(getString(R.string.automated_test_process) + ": 0/" + testCount);
                        break;
                    }
                    case TestState.TEST_STATE_IDLE: {  //测试结束
                        requireActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);//关闭屏幕常亮
                        TestFinish testFinish = (TestFinish) testState;
                        mBinding.tvOtaFileName.setVisibility(View.GONE);
                        mBinding.groupUpgrade.setVisibility(View.GONE);
                        mBinding.groupScanDeviceLoading.setVisibility(View.GONE);
                        mBinding.groupUpgradeResult.setVisibility(View.VISIBLE);
                        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) mBinding.clOtaContent.getLayoutParams();
                        mBinding.tvAutoTestTitle.setText(getString(R.string.automated_test_process) + ": " + getString(R.string.end));
                        if (testFinish.getCode() == TestTask.ERR_SUCCESS) {
                            mBinding.ivUpgradeResultLogo.setImageResource(R.drawable.ic_success_small);
                            mBinding.tvUpgradeResultTip.setText(R.string.update_finish);
                            layoutParams.height = FrameLayout.LayoutParams.WRAP_CONTENT;
                        } else {
                            mBinding.ivUpgradeResultLogo.setImageResource(R.drawable.ic_fail_small);
                            mBinding.tvUpgradeResultTip.setText(R.string.update_failed);
                            mBinding.tvUpgradeResultReason.setVisibility(View.VISIBLE);
//                                if (!isOTAError) {
//                                    mBinding.tvUpgradeResultReason.setText("原因：" + testFinish.getMessage());
//                                }
                            layoutParams.height = FrameLayout.LayoutParams.WRAP_CONTENT;
                        }
                        mBinding.tvUpgradeResultHit.setVisibility(View.VISIBLE);
                        mBinding.tvUpgradeResultHit.setText(getString(R.string.test_tasks) + ": " + testParam.getUpgradeCount() + ";" + getString(R.string.successful_tests) + ": " + testParam.getSuccess());
                        mBinding.clOtaContent.setLayoutParams(layoutParams);
                        break;
                    }
                    case TestState.TEST_STATE_TASK_START: { //测试任务开始
                        TestTaskStart testTaskStart = (TestTaskStart) testState;
                        mBinding.tvAutoTestTitle.setVisibility(View.VISIBLE);
                        mBinding.tvAutoTestTitle.setText(getString(R.string.automated_test_process) + ": " + testParam.getUpgradeCount() + "/" + testCount);
                        if (testTaskStart.getTask().getType() == TestTask.TASK_TYPE_UPDATE) {//已经开始升级内容
                            mBinding.groupUpgrade.setVisibility(View.VISIBLE);
                            mBinding.groupScanDeviceLoading.setVisibility(View.GONE);
                            String fileName = AppUtil.getFileNameByPath(testTaskStart.getMessage());
                            mBinding.tvOtaFileName.setText(fileName);
                        } else if (testTaskStart.getTask().getType() == TestTask.TASK_TYPE_CONNECT) {//还在连接设备
                            mBinding.tvOtaFileName.setVisibility(View.GONE);
                            mBinding.groupUpgrade.setVisibility(View.GONE);
                            mBinding.groupScanDeviceLoading.setVisibility(View.VISIBLE);
//                                mBinding.tvScanDeviceLoading.setText(String.format(Locale.getDefault(), getString(R.string.auto_test_reconnect), testParam.getSuccess()));
                        }
                        break;
                    }
                    case TestState.TEST_STATE_TASK_LOG: {  //测试任务日志
                        break;
                    }
                    case TestState.TEST_STATE_TASK_END: {  //测试任务结束
                        TestTaskEnd testTaskEnd = (TestTaskEnd) testState;
                        if (testTaskEnd.getCode() == TestTask.ERR_SUCCESS) {//升级成功 。开始进行回连进入下一次升级
                            mBinding.tvScanDeviceLoading.setText(String.format(Locale.getDefault(), getString(R.string.auto_test_reconnect), testParam.getUpgradeCount()));
                        } else {
                            mBinding.tvScanDeviceLoading.setText(String.format(Locale.getDefault(), getString(R.string.auto_test_fail_reconnect), testParam.getUpgradeCount()));
                            mBinding.tvUpgradeResultReason.setText(getString(R.string.ota_reason) + testTaskEnd.getMessage());
                        }
                        break;
                    }
                }
            }
        });
    }

    private int getPixelsFromDp(int size) {
        DisplayMetrics metrics = new DisplayMetrics();
        this.getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);
        return (size * metrics.densityDpi) / DisplayMetrics.DENSITY_DEFAULT;
    }
}