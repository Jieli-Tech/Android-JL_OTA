package com.jieli.otasdk.fragments;

import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;
import com.jieli.otasdk.MainApplication;
import com.jieli.otasdk.R;
import com.jieli.otasdk.activities.ContentActivity;
import com.jieli.otasdk.databinding.FragmentFileListBinding;
import com.jieli.otasdk.dialog.DialogTip;
import com.jieli.otasdk.dialog.DialogTipClickListener;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * 文件列表
 */
public class FileListFragment extends Fragment {
    private FragmentFileListBinding mBinding = null;
    private FileListAdapter fileListAdapter = null;

    public static FileListFragment newInstance() {
        return new FileListFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mBinding = FragmentFileListBinding.inflate(inflater, container, false);
        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mBinding.viewMainTopBar.tvTopLeft.setCompoundDrawablesRelativeWithIntrinsicBounds(
                0,
                0,
                R.drawable.ic_return,
                0
        );
        mBinding.viewMainTopBar.tvTopTitle.setText(R.string.log_files);
        mBinding.viewMainTopBar.tvTopRight.setCompoundDrawablesRelativeWithIntrinsicBounds(
                0,
                0,
                R.drawable.ic_delete,
                0
        );
        mBinding.viewMainTopBar.tvTopLeft.setOnClickListener(v -> requireActivity().finish());
        mBinding.viewMainTopBar.tvTopRight.setOnClickListener(v -> {
            DialogTip saveDialog = new DialogTip();
            saveDialog.setTitle(getString(R.string.delete_all_log_files));
            saveDialog.setLeftText(getString(R.string.cancel));
            saveDialog.setRightText(getString(R.string.delete));
            saveDialog.setDialogClickListener(new DialogTipClickListener() {
                @Override
                public void leftBtnClick() {
                    saveDialog.dismiss();
                }

                @Override
                public void rightBtnClick() {
                    File dir = new File(MainApplication.getLogFileDir());
                    if (dir.exists() && dir.isDirectory()) {
                        File[] files = dir.listFiles();
                        if (files != null) {
                            for (File file : files) {
                                file.delete();
                            }
                        }
                    }
                    fileListAdapter.setNewInstance(new ArrayList<>());
                    saveDialog.dismiss();
                }
            });
            saveDialog.show(getParentFragmentManager(), "saveDialog");
        });
        fileListAdapter = new FileListAdapter();
        mBinding.rvFileList.setAdapter(fileListAdapter);
        fileListAdapter.setOnItemClickListener((adapter, view1, position) -> {
            List<File> dataList = (List<File>) adapter.getData();
            File file = dataList.get(position);
            Bundle bundle = new Bundle();
            bundle.putString(FileDetailFragment.KEY_FILE_PATH, file.getPath());
            ContentActivity.startContentActivity(getContext(), FileDetailFragment.class.getCanonicalName(), bundle);
        });
        File dir = new File(MainApplication.getLogFileDir());
        if (dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles();
            List<File> fileList = Arrays.asList(files);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                fileList.sort(new Comparator<File>() {
                    @Override
                    public int compare(File o1, File o2) {
                        if (o1.lastModified() > o2.lastModified()) {
                            return -1;
                        } else {
                            return 1;
                        }
                    }
                });
            }
            if (files != null) {
                fileListAdapter.setNewInstance(fileList);
            }
        }
    }

    private class FileListAdapter extends BaseQuickAdapter<File, BaseViewHolder> {
        public FileListAdapter() {
            super(R.layout.item_file_list_2);
        }

        @Override
        protected void convert(@NonNull BaseViewHolder baseViewHolder, File file) {
            baseViewHolder.setText(R.id.tv_item_file_name, file.getName());
        }
    }
}