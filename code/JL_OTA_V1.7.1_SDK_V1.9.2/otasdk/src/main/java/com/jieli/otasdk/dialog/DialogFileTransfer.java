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

/**
 * @ClassName: DialogFileTransfer
 * @Description: 文件传输弹窗
 * @Author: ZhangHuanMing
 * @CreateDate: 2022/10/14 10:31
 */
public class DialogFileTransfer extends DialogFragment {
    public String httpUrl = "";
    public DialogFileTransferListener mListener = null;
    private FragmentDialogFileTransferBinding mBinding;

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
            layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
            layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
            layoutParams.gravity = Gravity.BOTTOM;
            window.setAttributes(layoutParams);
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        mBinding = FragmentDialogFileTransferBinding.inflate(inflater, container, false);
        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mBinding.tvHttpUrl.setText(httpUrl);
        mBinding.btLeft.setOnClickListener(v -> {
            if (mListener != null) {
                mListener.onLeftButtonClick();
            }
        });
        mBinding.btRight.setOnClickListener(v -> {
            if (mListener != null) {
                mListener.onRightButtonClick();
            }
        });
    }
}
