package com.jieli.otasdk.util;

import android.os.FileObserver;

import androidx.annotation.Nullable;

import com.jieli.jl_bt_ota.util.JL_Log;

/**
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc  文件监听类
 * @since 2021/5/31
 */
public class OtaFileObserver extends FileObserver {

    private FileObserverCallback mFileObserverCallback;


    public OtaFileObserver(String path) {
        super(path);
    }

    public void setFileObserverCallback(FileObserverCallback fileObserverCallback) {
        mFileObserverCallback = fileObserverCallback;
    }

    @Override
    public void onEvent(int event, @Nullable String path) {
        if(mFileObserverCallback != null && null != path){
//            JL_Log.d("zzc_observer", "-onEvent- event = " + event + ", path = " + path);
            mFileObserverCallback.onChange(event, path);
        }
    }
}
