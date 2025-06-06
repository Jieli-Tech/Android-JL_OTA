package com.jieli.otasdk.ui.settings.log;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;
import com.jieli.otasdk.R;
import com.jieli.otasdk.databinding.FragmentFileDetailBinding;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link FileDetailFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class FileDetailFragment extends Fragment {
    public static String KEY_FILE_PATH = "file_path";
    private FragmentFileDetailBinding mBinding;
    private int MSG_UPDATE_CONTENT = 1001;
    private Thread mReadFileThread = null;
    private FileDetailAdapter mAdapter = null;
    private Handler mHandler = new Handler(Looper.getMainLooper(), new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message msg) {
            if (msg.what == MSG_UPDATE_CONTENT) {
//                mBinding.tvFileDetail.append((String) msg.obj);//追加显示数据
                mAdapter.addData((String) msg.obj);
            }
            return false;
        }
    });

    public static FileDetailFragment newInstance() {
        return new FileDetailFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        mBinding = FragmentFileDetailBinding.inflate(inflater, container, false);
        return mBinding.getRoot();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mReadFileThread != null) {
            mReadFileThread.interrupt();
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (getArguments() == null) {
            requireActivity().finish();
            return;
        }
        String filePath = getArguments().getString(KEY_FILE_PATH);
        Log.d("ZHM", "onViewCreated: filePath :" + filePath);
        if (filePath == null) return;
        File file = new File(filePath);
        mBinding.viewMainTopBar.tvTopLeft.setCompoundDrawablesRelativeWithIntrinsicBounds(
                0,
                0,
                R.drawable.ic_return,
                0
        );
        mBinding.viewMainTopBar.tvTopTitle.setEllipsize(TextUtils.TruncateAt.valueOf("START"));
        mBinding.viewMainTopBar.tvTopTitle.setText(file.getName());
        mBinding.viewMainTopBar.tvTopRight.setCompoundDrawablesRelativeWithIntrinsicBounds(
                0,
                0,
                R.drawable.icon_share,
                0
        );
        mBinding.viewMainTopBar.tvTopLeft.setOnClickListener(v -> requireActivity().finish());
        mBinding.viewMainTopBar.tvTopRight.setOnClickListener(v -> {
            if (file.exists()) {
//                Uri uri = FileProvider.getUriForFile(getContext(), getContext().getPackageName() + ".provider", file);
//                Intent intent = new Intent();
//                intent.setAction(Intent.ACTION_SEND);
//                intent.setType("text/plain");
//                intent.setComponent(new ComponentName(getContext().getPackageName(), getClass().getName()));
//                intent.putExtra(Intent.EXTRA_STREAM, uri);
//                Intent chooserIntent = Intent.createChooser(intent, "分享到:");
//                getContext().startActivity(chooserIntent);
             /*   Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("text/plain");
                intent.putExtra(Intent.EXTRA_TEXT, "分享文本");
                startActivity(Intent.createChooser(intent, "标题"));*/
                shareOtherApp(getContext(), file);
            } else {
                Toast.makeText(getContext(), "文件不存在", Toast.LENGTH_LONG).show();
            }
        });
        mAdapter = new FileDetailAdapter();
        mBinding.rvFileDetail.setAdapter(mAdapter);
        if (!file.exists()) {//如果文件不存在

        } else {//如果文件已经存在
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    try {
//                        FileInputStream fr = new FileInputStream(filePath);//文件输出流
                        BufferedReader br = new BufferedReader(new FileReader(new File(filePath)));//缓冲读取文件数据
                        String line = "";//记录每一行数据
                        String content = "";
                        int i = 0;
                        Log.d("ZHM", "开始读取文件: " + mReadFileThread);
                        while (!mReadFileThread.isInterrupted() && ((line = br.readLine()) != null)) {//如果还有下一行数据
                            content += line + "n";
                            if (content.length() > 40000) {
                                i++;
                                Message message = new Message();
                                message.what = MSG_UPDATE_CONTENT;
                                message.obj = content;
                                mHandler.sendMessage(message);
                                content = "";
                                if (!mReadFileThread.isInterrupted()) {
                                    Thread.sleep(1000);
                                }
                                Log.d("TEST", "run: " + i);
                            }
                        }
                        Log.d("ZHM", "读取文件结束: ");
                        Message message = new Message();
                        message.what = MSG_UPDATE_CONTENT;
                        message.obj = content;
                        mHandler.sendMessage(message);
                        br.close();//关闭文件输出流
//                        fr.close();//关闭缓冲区
                    } catch (IOException | InterruptedException e) {//抛出异常
                        e.printStackTrace();
                    }
                }
            };
            mReadFileThread = new Thread(runnable);
            mReadFileThread.start();
        }
    }

    /**
     * 直接文件到微信好友
     *
     * @param picFile 文件路径
     */
    public static void shareOtherApp(Context mContext, File picFile) {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEND);
        String type = "*/*";
        intent.setType(type);
        Uri uri = null;
        if (picFile != null) {
            //这部分代码主要功能是判断了下文件是否存在，在android版本高过7.0（包括7.0版本）
            // 当前APP是不能直接向外部应用提供file开头的的文件路径，需要通过FileProvider转换一下。否则在7.0及以上版本手机将直接crash。
            try {
                ApplicationInfo applicationInfo = mContext.getApplicationInfo();
                int targetSDK = applicationInfo.targetSdkVersion;
                if (targetSDK >= Build.VERSION_CODES.N && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    uri = FileProvider.getUriForFile(mContext, mContext.getApplicationContext().getPackageName() + ".provider", picFile);
                } else {
                    uri = Uri.fromFile(picFile);
                }
                intent.putExtra(Intent.EXTRA_STREAM, uri);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivity(Intent.createChooser(intent, "Share"));

    }

    private class FileDetailAdapter extends BaseQuickAdapter<String, BaseViewHolder> {

        public FileDetailAdapter() {
            super(R.layout.item_file_detail_list);
        }

        @Override
        protected void convert(@NonNull BaseViewHolder baseViewHolder, String s) {
            baseViewHolder.setText(R.id.textView, s);
        }
    }

}