package com.jieli.broadcastbox.dialog;

import android.annotation.SuppressLint;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
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

import com.jieli.broadcastbox.adapter.UpgradeProgressAdapter;
import com.jieli.broadcastbox.interfaces.OnResultListener;
import com.jieli.broadcastbox.model.BroadcastBoxInfo;
import com.jieli.broadcastbox.model.UpgradeInfo;
import com.jieli.broadcastbox.model.ota.MultiOTAEnd;
import com.jieli.broadcastbox.model.ota.MultiOTAReconnect;
import com.jieli.broadcastbox.model.ota.MultiOTAState;
import com.jieli.broadcastbox.model.ota.MultiOTAStop;
import com.jieli.broadcastbox.model.ota.MultiOTAWorking;
import com.jieli.broadcastbox.viewmodel.MultiOTAViewModel;
import com.jieli.jl_bt_ota.util.JL_Log;
import com.jieli.otasdk.R;
import com.jieli.otasdk.ui.base.BaseDialogFragment;
import com.jieli.otasdk.databinding.DialogUpgradeDeviceBinding;
import com.jieli.otasdk.util.AppUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Des:
 * author: Bob
 * date: 2022/12/02
 * Copyright: Jieli Technology
 * Modify date:
 * Modified by:
 */
public class DialogUpgradeDevice extends BaseDialogFragment {
    private static final String tag = "DialogUpgradeDevice";
    private final List<BroadcastBoxInfo> deviceInfoList;
    private final OnResultListener<List<BroadcastBoxInfo>> onResultListener;

    private DialogUpgradeDeviceBinding binding;
    private UpgradeProgressAdapter adapter;
    private MultiOTAViewModel viewModel;

    private long refreshTime = 0;

    public DialogUpgradeDevice(List<BroadcastBoxInfo> infos, OnResultListener<List<BroadcastBoxInfo>> listener) {
        this.deviceInfoList = infos;
        this.onResultListener = listener;
    }

    @Override
    public void onStart() {
        super.onStart();
        Window window = null;
        if (getDialog() != null) {
            window = getDialog().getWindow();
        }
        if (window == null) return;
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
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        binding = DialogUpgradeDeviceBinding.inflate(inflater, container, false);
        requireDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));// 设置背景透明
        requireDialog().setOnKeyListener((dialog, keyCode, event) -> keyCode == KeyEvent.KEYCODE_BACK);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        if (getActivity() == null || getActivity().getWindow() == null || getDialog() == null
                || getDialog().getWindow() == null)
            return;
        requireDialog().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        viewModel = new ViewModelProvider(this).get(MultiOTAViewModel.class);

        addObserver();
        final WindowManager.LayoutParams params = getDialog().getWindow().getAttributes();

        params.width = WindowManager.LayoutParams.MATCH_PARENT;
        params.height = WindowManager.LayoutParams.MATCH_PARENT;
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            params.width = displayMetrics.heightPixels * 9 / 10;
            params.height = displayMetrics.heightPixels * 9 / 10;
        } else if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            params.width = displayMetrics.widthPixels * 9 / 10;
//            params.height = displayMetrics.widthPixels * 9 / 10;
            int h = 100 + 60 * deviceInfoList.size();// 根据升级设备个数来设置大小
            params.height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, h, getResources().getDisplayMetrics());
        }
        params.y = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 28, getResources().getDisplayMetrics());
        params.gravity = Gravity.BOTTOM;
        getDialog().getWindow().setAttributes(params);

        if (null == adapter) {
            adapter = new UpgradeProgressAdapter();
        }
        binding.rvUpgradeList.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvUpgradeList.setAdapter(adapter);

        viewModel.startMultiOTA(requireContext(), deviceInfoList);
    }

    @Override
    public void onDestroyView() {
        if (requireDialog().getWindow() != null) {
            requireDialog().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        JL_Log.i(tag, "on Destroy");
        viewModel.release();
    }

    @SuppressLint("NotifyDataSetChanged")
    private void addObserver() {
        viewModel.multiOtaStateMLD.observe(this, multiOTAState -> {
            switch (multiOTAState.getState()) {
                case MultiOTAState.STATE_IDLE: {
                    MultiOTAEnd otaEnd = (MultiOTAEnd) multiOTAState;
                    binding.tvUpgradeWarning.setVisibility(View.GONE);
                    binding.viewLine.setVisibility(View.VISIBLE);
                    binding.tvOkButton.setVisibility(View.VISIBLE);
                    binding.tvTitle.setText(requireActivity().getString(R.string.ota_finish));
                    binding.tvOkButton.setOnClickListener(v -> {
                        dismiss();
                        if (onResultListener != null) {
                           /* List<BroadcastBoxInfo> successList = new ArrayList<>();
                            List<MultiOtaParam> params = otaEnd.getSuccessList();
                            if (params != null && !params.isEmpty()) {
                                for (MultiOtaParam param : params) {
                                    for (BroadcastBoxInfo info : deviceInfoList) {
                                        if (param.getAddress().equals(info.getDevice().getAddress())) {
                                            successList.add(info);
                                            break;
                                        }
                                    }
                                }
                            }*/
                            onResultListener.onResult(deviceInfoList);
                        }
                    });
                    break;
                }
                case MultiOTAState.STATE_START: {
//                    MultiOTAStart otaStart = (MultiOTAStart) multiOTAState;
                    List<UpgradeInfo> list = new ArrayList<>();
                    for (final BroadcastBoxInfo info : deviceInfoList) {
                        UpgradeInfo upgradeInfo = new UpgradeInfo(info.getDevice().getAddress())
                                .setDeviceName(AppUtil.getDeviceName(requireContext(), info.getDevice()))
                                .setFilename(info.getSelectFile().getName())
                                .setState(UpgradeInfo.STATE_WORKING);
                        list.add(upgradeInfo);
                    }
                    adapter.setList(list);
                    break;
                }
                case MultiOTAState.STATE_WORKING: {
                    MultiOTAWorking otaWorking = (MultiOTAWorking) multiOTAState;
                    UpgradeInfo info = adapter.getCacheInfo(otaWorking.getAddress());
                    if (null == info) return;
                    boolean isChange = false;
                    if (info.getState() != UpgradeInfo.STATE_WORKING) {
                        info.setState(UpgradeInfo.STATE_WORKING);
                        isChange = true;
                    }
                    if (info.getUpgradeType() != otaWorking.getOtaType()) {
                        info.setUpgradeType(otaWorking.getOtaType());
                        isChange = true;
                    }
                    int progress = Math.round(otaWorking.getOtaProgress());
                    if (progress > 100) progress = 100;
                    if (info.getProgress() != progress) {
                        info.setProgress(progress);
                        JL_Log.d(tag, "STATE_WORKING : type = " + info.getUpgradeType() + ", progress = " + progress);
                        isChange = true;
                    }
                    if (isChange) {
                        adapter.notifyDataSetChanged();
                    }
                    break;
                }
                case MultiOTAState.STATE_OTA_RECONNECT: {
                    MultiOTAReconnect otaReconnect = (MultiOTAReconnect) multiOTAState;
                    UpgradeInfo info = adapter.getCacheInfo(otaReconnect.getAddress());
                    if (null == info) return;
                    info.setState(UpgradeInfo.STATE_RECONNECT)
                            .setUpgradeType(0)
                            .setProgress(0);
                    adapter.updateInfo(info);
                    break;
                }
                case MultiOTAState.STATE_OTA_STOP: {
                    MultiOTAStop otaStop = (MultiOTAStop) multiOTAState;
                    UpgradeInfo info = adapter.getCacheInfo(otaStop.getAddress());
                    if (null == info) return;
                    info.setState(UpgradeInfo.STATE_STOP)
                            .setError(otaStop.getCode())
                            .setMessage(otaStop.getMessage());
                    adapter.updateInfo(info);
                    break;
                }
            }
        });
    }
}
