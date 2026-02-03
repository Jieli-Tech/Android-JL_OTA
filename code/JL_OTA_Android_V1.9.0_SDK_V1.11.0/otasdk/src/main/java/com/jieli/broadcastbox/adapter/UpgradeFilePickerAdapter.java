package com.jieli.broadcastbox.adapter;

import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;
import com.jieli.otasdk.R;

import java.io.File;

/**
 * Des:
 * author: Bob
 * date: 2022/11/30
 * Copyright: Jieli Technology
 * Modify date:
 * Modified by:
 */
public class UpgradeFilePickerAdapter extends BaseQuickAdapter<File, BaseViewHolder> {
    private String selectedFile;

    public UpgradeFilePickerAdapter() {
        super(R.layout.upgrade_file_picker_item);
    }

    public void setSelectedFile(String filepath) {
        selectedFile = filepath;
    }

    @Override
    protected void convert(@NonNull BaseViewHolder holder, File file) {
        holder.setText(R.id.tv_file_name, file.getName());
        if (!TextUtils.isEmpty(selectedFile) && selectedFile.equals(file.getAbsolutePath())) {
            holder.setImageResource(R.id.iv_check, R.drawable.ic_blue_hook);
        } else {
            holder.setImageResource(R.id.iv_check, 0);
        }
    }
}
