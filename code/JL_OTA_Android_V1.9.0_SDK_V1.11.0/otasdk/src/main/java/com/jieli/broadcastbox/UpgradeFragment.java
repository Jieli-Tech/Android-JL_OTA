package com.jieli.broadcastbox;

import android.bluetooth.BluetoothProfile;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.snackbar.Snackbar;
import com.jieli.broadcastbox.adapter.UpgradeDeviceAdapter;
import com.jieli.broadcastbox.dialog.DialogUpgradeDevice;
import com.jieli.broadcastbox.dialog.DialogUpgradeFilePicker;
import com.jieli.broadcastbox.dialog.NotifyDialog;
import com.jieli.broadcastbox.model.BroadcastBoxInfo;
import com.jieli.broadcastbox.viewmodel.BroadcastBoxViewModel;
import com.jieli.broadcastbox.viewmodel.FileOpViewModel;
import com.jieli.component.utils.ToastUtil;
import com.jieli.jl_bt_ota.util.JL_Log;
import com.jieli.otasdk.R;
import com.jieli.otasdk.ui.base.BaseFragment;
import com.jieli.otasdk.databinding.FragmentUpgradeBinding;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class UpgradeFragment extends BaseFragment {
    private FragmentUpgradeBinding binding;
    private UpgradeDeviceAdapter adapter;
    private BroadcastBoxViewModel viewModel;

    private static final int MSG_UPDATE_DEVICE_LIST = 0X1234;
    private final Handler uiHandler = new Handler(msg -> {
        if (MSG_UPDATE_DEVICE_LIST == msg.what) {
            updateConnectedDeviceList();
        }
        return true;
    });

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentUpgradeBinding.inflate(inflater, container, false);
        binding.btnUpgrade.setEnabled(false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(requireActivity()).get(BroadcastBoxViewModel.class);
        FileOpViewModel fileOpViewModel = new ViewModelProvider(requireActivity()).get(FileOpViewModel.class);
        fileOpViewModel.readFileList();

        viewModel.deviceConnectionMLD.observe(getViewLifecycleOwner(), deviceConnection -> {
            JL_Log.i(TAG, ">>>> deviceConnectionMLD >> " + deviceConnection);
            if (deviceConnection.getState() != BluetoothProfile.STATE_CONNECTING) {
                uiHandler.removeMessages(MSG_UPDATE_DEVICE_LIST);
                uiHandler.sendEmptyMessageDelayed(MSG_UPDATE_DEVICE_LIST, 300);
            }
        });

        binding.btnUpgrade.setOnClickListener(v -> {
            boolean ready = true;
            for (BroadcastBoxInfo info : viewModel.getSelectedDeviceList()) {
                if (info.getSelectFile() == null) {
                    ready = false;
                    break;
                }
            }
            if (ready) {
                DialogUpgradeDevice upgradeDevice = null;
                Fragment fragment = getChildFragmentManager().findFragmentByTag(DialogUpgradeDevice.class.getSimpleName());
                if (fragment instanceof DialogUpgradeDevice) {
                    upgradeDevice = (DialogUpgradeDevice) fragment;
                }
                if (null == upgradeDevice) {
                    upgradeDevice = new DialogUpgradeDevice(viewModel.getSelectedDeviceList(), infos -> {
                        uiHandler.removeMessages(MSG_UPDATE_DEVICE_LIST);
                        uiHandler.sendEmptyMessageDelayed(MSG_UPDATE_DEVICE_LIST, 300);
                    });
                }
                if (!upgradeDevice.isShow()) {
                    upgradeDevice.show(getChildFragmentManager(), DialogUpgradeDevice.class.getSimpleName());
                }
            } else {
                showSnackBar(getString(R.string.ota_please_chose_file));
            }
        });

        adapter = new UpgradeDeviceAdapter();
        binding.rvDeviceList.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvDeviceList.setAdapter(adapter);

        adapter.addChildClickViewIds(R.id.iv_hook, R.id.tv_choose_file);
        adapter.setOnItemChildClickListener((adapter1, view1, position) -> {
            JL_Log.i(TAG, "position=" + position);
            if (R.id.iv_hook == view1.getId()) {// select device
                BroadcastBoxInfo boxInfo = adapter.getItem(position);
                if (boxInfo.getSelectFile() == null) {
                    ToastUtil.showToastLong(getString(R.string.no_file_selected));
                    return;
                }
                boolean state = boxInfo.isChosen();
                boxInfo.setChosen(!state);
                if (!state) {
                    if (!viewModel.getSelectedDeviceList().contains(boxInfo)) {
                        viewModel.getSelectedDeviceList().add(boxInfo);
                    }
                } else {
                    viewModel.getSelectedDeviceList().remove(boxInfo);
                }
                updateUpgradeButton();

//                    adapter.notifyDataSetChanged();
                adapter.notifyItemChanged(position);
            } else if (R.id.tv_choose_file == view1.getId()) {// choose file
                if (fileOpViewModel.getUpgradeFiles().size() <= 0) {
                    showNoFileDialog();
                    return;
                }
                File file = adapter.getItem(position).getSelectFile();
                String filepath = null;
                if (file != null) {
                    filepath = file.getAbsolutePath();
                }

                DialogUpgradeFilePicker upgradeFilePicker;
                int pid = adapter.getItem(position).getPid();
                int uid = adapter.getItem(position).getUid();
                JL_Log.i(TAG, "uid=" + uid + ", pid = " + pid);
                upgradeFilePicker = DialogUpgradeFilePicker.newInstance(filepath, uid, pid);
                upgradeFilePicker.setOnResultListener(file1 -> {
                    adapter.getItem(position).setSelectFile(file1);
                    adapter.notifyItemChanged(position);
                });
                upgradeFilePicker.show(getParentFragmentManager(), upgradeFilePicker.getClass().getSimpleName());
            }
        });

        uiHandler.sendEmptyMessage(MSG_UPDATE_DEVICE_LIST);
    }

    private void showNoFileDialog() {
        NotifyDialog.Builder builder = new NotifyDialog.Builder(getContext());
        NotifyDialog dialog = builder.setCancelable(true)
                .setMessage(requireContext()
                .getString(R.string.no_file_added))
                .setImageResource(R.drawable.ic_fail_small)
                .create();

        dialog.show(getParentFragmentManager(), "NoFileDialog");
    }

    private void updateConnectedDeviceList() {
        List<BroadcastBoxInfo> connectedDevices = viewModel.getConnectedBleDevices(); //已连接设备列表
        List<BroadcastBoxInfo> cacheSelected = viewModel.getSelectedDeviceList();      //缓存的选择设备列表
        List<BroadcastBoxInfo> realTimeList = new ArrayList<>();  //实时更新列表
        JL_Log.i(TAG, "updateConnectedDeviceList >> connectedDevices = " + connectedDevices.size() + ", cacheSelected = " + cacheSelected.size());
        boolean isEmpty = connectedDevices.isEmpty();
        binding.groupConnected.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        binding.groupNoconnect.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        for (BroadcastBoxInfo boxInfo : connectedDevices) {
            for (BroadcastBoxInfo selected : cacheSelected) {
                if (boxInfo.equals(selected)) {
                    boxInfo.setChosen(true);
                    boxInfo.setSelectFile(selected.getSelectFile());
                    boxInfo.setForceUpdate(selected.isForceUpdate());
                    realTimeList.add(boxInfo);
                    break;
                }
            }
        }
        JL_Log.i(TAG, "updateConnectedDeviceList >> realTimeList = " + realTimeList.size());
        adapter.setList(connectedDevices);
        viewModel.getSelectedDeviceList().clear();
        viewModel.getSelectedDeviceList().addAll(realTimeList);
        updateUpgradeButton();
    }

    private void updateUpgradeButton() {
        if (!viewModel.getSelectedDeviceList().isEmpty()) {
            binding.btnUpgrade.setEnabled(true);
            binding.btnUpgrade.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.bg_btn_upgrade, null));
        } else {
            binding.btnUpgrade.setEnabled(false);
            binding.btnUpgrade.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.dbg_btn_unenable, null));
        }
    }

    private void showSnackBar(String msg) {
        Snackbar.make(binding.getRoot(), msg, Snackbar.LENGTH_LONG).show();
    }
}