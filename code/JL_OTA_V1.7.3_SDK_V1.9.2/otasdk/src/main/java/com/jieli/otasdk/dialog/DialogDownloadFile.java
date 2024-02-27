package com.jieli.otasdk.dialog;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.TextUtils;
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

import com.jieli.otasdk.R;
import com.jieli.otasdk.base.BaseDialogFragment;
import com.jieli.otasdk.databinding.FragmentDialogDownloadFileBinding;
import com.jieli.otasdk.viewmodel.DownloadFileViewModel;

/**
 * 下载文件提示
 */
public class DialogDownloadFile extends BaseDialogFragment {
    private FragmentDialogDownloadFileBinding mBinding;
    private String TAG = this.getClass().getSimpleName();
    private DownloadFileViewModel mDownloadFileViewModel = null;

    public DialogDownloadFile() {
    }

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
        mBinding = FragmentDialogDownloadFileBinding.inflate(inflater, container, false);
        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mDownloadFileViewModel = new ViewModelProvider(requireActivity()).get(DownloadFileViewModel.class);
        initUI();
        observeCallback();

    }

    private void initUI() {
        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) mBinding.clOtaContent.getLayoutParams();
        layoutParams.height = getPixelsFromDp(148);
        String url = mDownloadFileViewModel.getHttpUrl();
        String[] fileNames = url.split("/");
        String fileName = fileNames[fileNames.length - 1];
        if (fileName == null) {
            fileName = "upgrade.ufw";
        }
        mBinding.tvOtaFileName.setText(fileName);
        mBinding.tvUpgradeProgress.setText(getString(R.string.downloading_file) + 0 + "%");
    }

    //增加观察者
    private void observeCallback() {
        mDownloadFileViewModel.getDownloadStatusMLD().observe(getViewLifecycleOwner(), downloadFileEvent -> {
            if (downloadFileEvent == null) return;
            if (TextUtils.equals(downloadFileEvent.type, "onProgress")) {
                int progress = downloadFileEvent.progress.intValue();
                mBinding.tvUpgradeProgress.setText(getString(R.string.downloading_file) + progress + "%");
                mBinding.pbUpgradeProgress.setProgress(progress);
            } else if (TextUtils.equals(downloadFileEvent.type, "onStop")) {
                mDownloadFileViewModel.getDownloadStatusMLD().postValue(null);
                dismiss();
            } else if (TextUtils.equals(downloadFileEvent.type, "onError")) {
                mDownloadFileViewModel.getDownloadStatusMLD().postValue(null);
                dismiss();
            } else if (TextUtils.equals(downloadFileEvent.type, "onStart")) {
            }
        });
    }

    private int getPixelsFromDp(int size) {
        DisplayMetrics metrics = new DisplayMetrics();
        this.getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);
        return (size * metrics.densityDpi) / DisplayMetrics.DENSITY_DEFAULT;
    }
}