package com.jieli.broadcastbox.adapter;

import android.bluetooth.BluetoothAdapter;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;
import com.jieli.broadcastbox.model.UpgradeInfo;
import com.jieli.jl_bt_ota.constant.ErrorCode;
import com.jieli.jl_bt_ota.constant.JL_Constant;
import com.jieli.otasdk.R;

import java.util.Locale;

/**
 * Des:
 * author: Bob
 * date: 2022/11/30
 * Copyright: Jieli Technology
 * Modify date:
 * Modified by:
 */
public class UpgradeProgressAdapter extends BaseQuickAdapter<UpgradeInfo, BaseViewHolder> {
    public UpgradeProgressAdapter() {
        super(R.layout.upgrade_progress_item);
    }

    @Override
    protected void convert(@NonNull BaseViewHolder holder, UpgradeInfo info) {
        holder.setText(R.id.tv_device_name, info.getDeviceName());

        TextView tvState = holder.getView(R.id.tv_upgrade_state);
        ProgressBar progressBar = holder.getView(R.id.pb_upgrade_progress);
        TextView tvProgress = holder.getView(R.id.tv_progress);
        switch (info.getState()) {
            case UpgradeInfo.STATE_WORKING: {
                String title;
                if (info.getUpgradeType() == JL_Constant.TYPE_FIRMWARE_UPGRADE) {
                    title = getContext().getString(R.string.ota_upgrading);
                } else {
                    title = getContext().getString(R.string.ota_check_file);
                }
                tvState.setText(String.format(Locale.getDefault(), "(%s)", title));
                if (progressBar.isIndeterminate()) {
                    progressBar.setIndeterminate(false);
                }
                progressBar.setProgress(info.getProgress());
                setViewVisibility(tvProgress, true);
                tvProgress.setText(String.format(Locale.getDefault(), "%d%%", info.getProgress()));
                break;
            }
            case UpgradeInfo.STATE_RECONNECT: {
                tvState.setText(String.format(Locale.getDefault(), "(%s)", getContext().getString(R.string.reconnecting)));
                setViewVisibility(tvProgress, false);
                if (!progressBar.isIndeterminate()) {
                    progressBar.setIndeterminate(true);
                }
                break;
            }
            default: {
                setViewVisibility(tvProgress, false);
                setViewVisibility(progressBar, false);
                int resId;
                int error = info.getError();
                if (error != ErrorCode.ERR_NONE) {
                    String str = String.format("(%s:0x%x)", getContext().getString(R.string.update_failed), error);
                    holder.setText(R.id.tv_upgrade_state, str);
                    resId = R.drawable.ic_orange_circle_hook;
                } else {
                    holder.getView(R.id.tv_upgrade_state).setVisibility(View.GONE);
                    resId = R.drawable.ic_green_circle_hook;
                }
                ((TextView) holder.getView(R.id.tv_device_name))
                        .setCompoundDrawablesRelativeWithIntrinsicBounds(resId, 0, 0, 0);
                break;
            }
        }
    }

    public UpgradeInfo getCacheInfo(String address) {
        if (!BluetoothAdapter.checkBluetoothAddress(address)) return null;
        for (UpgradeInfo info : getData()) {
            if (address.equals(info.getDeviceAddress())) {
                return info;
            }
        }
        return null;
    }

    public void updateInfo(UpgradeInfo info) {
        int position = getItemPosition(info);
        if (position >= 0 && position < getData().size()) {
            notifyItemChanged(position);
        }
    }

    private void setViewVisibility(@NonNull View view, boolean isShow) {
        int visibility = isShow ? View.VISIBLE : View.GONE;
        if (view.getVisibility() != visibility) {
            view.setVisibility(visibility);
        }
    }
}
