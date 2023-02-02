package com.jieli.otasdk.dialog;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.jieli.otasdk.databinding.FragmentDialogFileTransferBinding;
import com.jieli.otasdk.databinding.FragmentDialogSeekBarBinding;

/**
 * @ClassName: DialogSeekBar
 * @Description: 滑动条弹窗
 * @Author: ZhangHuanMing
 * @CreateDate: 2022/10/21 10:19
 */
public class DialogSeekBar extends DialogFragment {
    public DialogSeekBarListener mListener = null;
    private FragmentDialogSeekBarBinding mBinding;
    private final int MAX = 512;
    private final int MIN = 23;
    public String title = null;
    public int progress = MIN;
    public int max = MAX;
    public int min = MIN;
    public String leftText = null;
    public String rightText = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        //设置dialog的基本样式参数
        requireDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);
        Window window = requireDialog().getWindow();
        if (window != null) {
            //去掉dialog默认的padding
            window.getDecorView().setPadding(0, 0, 0, 0);
            WindowManager.LayoutParams layoutParams = window.getAttributes();
            layoutParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
            layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
            layoutParams.gravity = Gravity.CENTER;
            window.setAttributes(layoutParams);
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        mBinding = FragmentDialogSeekBarBinding.inflate(inflater, container, false);
        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (title != null) {
            mBinding.tvDialogTitle.setVisibility(View.VISIBLE);
            mBinding.tvDialogTitle.setText(title);
        }
        mBinding.seekBar.setProgress(progress);
        mBinding.seekBar.setMax(max);
        mBinding.seekBar.setMin(min);
        mBinding.tvMax.setText(String.valueOf(max));
        mBinding.tvMin.setText(String.valueOf(min));
        if (leftText != null) {
            mBinding.btLeft.setVisibility(View.VISIBLE);
            mBinding.btLeft.setText(leftText);
        }
        if (rightText != null) {
            mBinding.btRight.setVisibility(View.VISIBLE);
            mBinding.btRight.setText(rightText);
        }
        if (rightText != null && leftText != null) {
            mBinding.view1.setVisibility(View.VISIBLE);
        }
        mBinding.btLeft.setOnClickListener(v -> {
            if (mListener != null) {
                mListener.onLeftButtonClick(mBinding.seekBar.getProgress());
            }
        });
        mBinding.btRight.setOnClickListener(v -> {
            if (mListener != null) {
                mListener.onRightButtonClick(mBinding.seekBar.getProgress());
            }
        });
    }
}
