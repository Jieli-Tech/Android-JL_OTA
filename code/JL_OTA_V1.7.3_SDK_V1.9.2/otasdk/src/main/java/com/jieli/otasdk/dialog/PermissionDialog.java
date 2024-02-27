package com.jieli.otasdk.dialog;

import android.Manifest;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;


import com.jieli.otasdk.R;

import permissions.dispatcher.PermissionRequest;

/**
 * @ClassName: PermissionDialog
 * @Description: java类作用描述
 * @Author: ZhangHuanMing
 * @CreateDate: 2021/11/24 10:31
 */
public class PermissionDialog extends BaseDialogFragment {
    private String permission;
    private PermissionRequest request;

    public PermissionDialog(String permission, PermissionRequest request) {
        this.permission = permission;
        this.request = request;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (getDialog() != null) {
            getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);
            Window window = getDialog().getWindow();
            if (window != null) {
                //去掉dialog默认的padding
                window.getDecorView().setPadding(0, 0, 0, 0);
                WindowManager.LayoutParams lp = window.getAttributes();
                lp.width = Math.round(0.9f * getScreenWidth());
                lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
                lp.gravity = Gravity.CENTER;
                //设置dialog的动画
//                lp.windowAnimations = R.style.BottomToTopAnim;
                window.setAttributes(lp);
                window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            }
            View view = inflater.inflate(R.layout.dialog_permission, container, false);
            TextView tvDialogTitle = view.findViewById(R.id.tv_dialog_permission_title);
            TextView tvDialogContent = view.findViewById(R.id.tv_dialog_permission_content);
            Button btNoRequire = view.findViewById(R.id.btn_chose_no_require);
            Button btRequire = view.findViewById(R.id.btn_chose_require);
            switch (permission) {
                case Manifest.permission.CAMERA:
                    tvDialogContent.setText(request != null ? getString(R.string.access_camera_reason)
                            : getString(R.string.system_set_camera));
                    break;
                case Manifest.permission.READ_EXTERNAL_STORAGE:
                case Manifest.permission.READ_MEDIA_IMAGES:
                case Manifest.permission.READ_MEDIA_AUDIO:
                case Manifest.permission.READ_MEDIA_VIDEO:
                    tvDialogContent.setText(request != null ? getString(R.string.access_photos_reason)
                            : getString(R.string.system_set_external_storage));
                    break;
            }
            btNoRequire.setOnClickListener(v -> dismiss());
            btRequire.setOnClickListener(v -> {
                if (request != null) {
                    request.proceed();
                } else {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", getContext().getPackageName(), null);
                    intent.setData(uri);
                    startActivity(intent);
                }
                dismiss();
            });
            return view;
        }
        return super.onCreateView(inflater, container, savedInstanceState);
    }

}
