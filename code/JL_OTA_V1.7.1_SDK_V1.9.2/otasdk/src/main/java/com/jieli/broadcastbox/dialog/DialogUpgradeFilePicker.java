package com.jieli.broadcastbox.dialog;

import android.content.res.Configuration;
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.jieli.broadcastbox.adapter.UpgradeFilePickerAdapter;
import com.jieli.broadcastbox.viewmodel.FileOpViewModel;
import com.jieli.jl_bt_ota.util.JL_Log;
import com.jieli.jl_bt_ota.util.ParseDataUtil;
import com.jieli.otasdk.databinding.DialogUpgradeFilePickerBinding;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashSet;

/**
 * Des:
 * author: Bob
 * date: 2022/11/30
 * Copyright: Jieli Technology
 * Modify date:
 * Modified by:
 */
public class DialogUpgradeFilePicker extends DialogFragment {
    private final String tag = getClass().getSimpleName();
    private DialogUpgradeFilePickerBinding binding;
    private UpgradeFilePickerAdapter adapter;
    private FileOpViewModel viewModel;
    private OnResultListener onResultListener;
    private String selectedFilePath;
    private int uid = -1;
    private int pid = -1;

    public static DialogUpgradeFilePicker newInstance(String path, int uid, int pid) {
        DialogUpgradeFilePicker filePicker = new DialogUpgradeFilePicker();
        Bundle bundle = new Bundle();
        bundle.putString("file_path", path);
        bundle.putInt("uid", uid);
        bundle.putInt("pid", pid);
        filePicker.setArguments(bundle);
        return filePicker;
    }

    public interface OnResultListener {
        void onResult(File file);
    }

    public void setOnResultListener(OnResultListener onResultListener) {
        this.onResultListener = onResultListener;
    }

    @Override
    public void onStart() {
        super.onStart();
        Window window = requireDialog().getWindow();
        if(window == null) return;
        WindowManager.LayoutParams windowParams = window.getAttributes();
        windowParams.dimAmount = 0.5f;
        windowParams.flags |= WindowManager.LayoutParams.FLAG_DIM_BEHIND;
        window.setAttributes(windowParams);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_TITLE, android.R.style.Theme_Light_NoTitleBar);
        setCancelable(true);
        if (getDialog() != null) {
            getDialog().setCanceledOnTouchOutside(true);
        }

        Bundle bundle = getArguments();
        if (bundle != null) {
            selectedFilePath = bundle.getString("file_path");
            uid = bundle.getInt("uid");
            pid = bundle.getInt("pid");
            JL_Log.w(tag, "uid = " + uid + ", pid = " + pid);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        if (getDialog() != null) {
            getDialog().setCanceledOnTouchOutside(true);
            getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));// 设置背景透明
        }
        binding = DialogUpgradeFilePickerBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        if (getActivity() == null || getActivity().getWindow() == null || getDialog() == null
                || getDialog().getWindow() == null)
            return;
        final WindowManager.LayoutParams params = getDialog().getWindow().getAttributes();

        params.width = WindowManager.LayoutParams.MATCH_PARENT;
        params.height = WindowManager.LayoutParams.MATCH_PARENT;
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
            params.width = displayMetrics.heightPixels * 4 / 5;
            params.height = displayMetrics.heightPixels * 5 / 6;
        } else if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT){
            params.width = displayMetrics.widthPixels * 4 / 5;
            params.height = displayMetrics.widthPixels * 5 / 6;
        }
        params.gravity = Gravity.CENTER;
        getDialog().getWindow().setAttributes(params);

        viewModel = new ViewModelProvider(requireActivity()).get(FileOpViewModel.class);

        if (null == adapter) {
            adapter = new UpgradeFilePickerAdapter();
        }
        adapter.setSelectedFile(selectedFilePath);
        binding.rvFileList.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvFileList.setAdapter(adapter);

        addFile();

//        adapter.addData(viewModel.getUpgradeFiles());
//        for (int i = 0; i < 8; i++) {
//            UpgradeInfo upgradeInfo = new UpgradeInfo();
//            upgradeInfo.setDevice("AC701-1" + i);
//            upgradeInfo.setFile("update.ufw");
//            adapter.addData(upgradeInfo);
//        }

        adapter.setOnItemClickListener((adapter1, view1, position) -> {
            dismiss();
            JL_Log.e(tag, "onItemClick=" + adapter.getItem(position).getName());
            if (onResultListener != null) {
                onResultListener.onResult(adapter.getItem(position));
            }
        });
    }

    private void addFile() {
        new Thread(() -> {
            HashSet<File> fileHashSet = viewModel.getUpgradeFiles();
            for (File file : fileHashSet) {
                if (uid <= 0 && pid <= 0) {
                    requireActivity().runOnUiThread(() -> adapter.addData(file));
                    continue;
                }
                byte[] bytes = readFile(file);
                if (bytes == null) {
                    JL_Log.e(tag, "read file error");
                    break;
                }
                int ret = ParseDataUtil.filterFile(bytes, uid, pid);
                if (ret == 0) {
                    requireActivity().runOnUiThread(() -> {
                        adapter.addData(file);
//                                adapter.notifyDataSetChanged();
                    });
                } else {
                    JL_Log.e(tag, "Filter file error: " + ret);
                }
            }
        }).start();
    }

    private byte[] readFile(File file) {
        FileInputStream inputStream = null;
//        String path = getExternalFilesDir(null) + "/update.ufw";
        try {
            inputStream = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        if (inputStream != null) {
            byte[] buffer = new byte[0];
            try {
                buffer = new byte[inputStream.available()];
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                inputStream.read(buffer);
            } catch (IOException e) {
                e.printStackTrace();
            }

            try {
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return buffer;
        }
        return null;
    }
}
