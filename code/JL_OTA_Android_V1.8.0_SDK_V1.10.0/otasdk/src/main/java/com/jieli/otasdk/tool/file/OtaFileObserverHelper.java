package com.jieli.otasdk.tool.file;

import android.os.Handler;
import android.os.Looper;

import java.util.ArrayList;

/**
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 文件监听辅助类
 * @since 2021/5/31
 */
@Deprecated
public class OtaFileObserverHelper {

    private volatile static OtaFileObserverHelper instance;
    private OtaFileObserver mOtaFileObserver;
    private boolean isWatching;
    private String watchPath;

    private final ArrayList<FileObserverCallback> mFileObserverCallbacks = new ArrayList<>();
    private final Handler mHandler = new Handler(Looper.getMainLooper());


    private OtaFileObserverHelper() {
//        String tempPath = MainApplication.getOTAFileDir();
//        updateObserverPath(tempPath);
    }

    public static OtaFileObserverHelper getInstance() {
        if (null == instance) {
            synchronized (OtaFileObserverHelper.class) {
                if (null == instance) {
                    instance = new OtaFileObserverHelper();
                }
            }
        }
        return instance;
    }

    /**
     * 更新监听的文件路径
     */
    public void updateObserverPath(String observerPath) {
        if (isWatching()){
            stopObserver();
        }
        watchPath = observerPath;
        mOtaFileObserver = new OtaFileObserver(watchPath);
        mOtaFileObserver.setFileObserverCallback((event, path) -> mHandler.post(() -> {
            if (!mFileObserverCallbacks.isEmpty()) {
                for (FileObserverCallback callback : new ArrayList<>(mFileObserverCallbacks)) {
                    callback.onChange(event, path);
                }
            }
        }));
    }

    public void registerFileObserverCallback(FileObserverCallback callback) {
        if (callback != null && !mFileObserverCallbacks.contains(callback)) {
            mFileObserverCallbacks.add(callback);
        }
    }

    public void unregisterFileObserverCallback(FileObserverCallback callback) {
        if (callback != null && !mFileObserverCallbacks.isEmpty()) {
            mFileObserverCallbacks.remove(callback);
        }
    }

    public boolean isWatching() {
        return isWatching;
    }

    public String getWatchPath() {
        return watchPath;
    }

    public void startObserver() {
        if (mOtaFileObserver != null) {
            mOtaFileObserver.startWatching();
            isWatching = true;
        }
    }

    public void stopObserver() {
        mOtaFileObserver.stopWatching();
        isWatching = false;
    }

    public void destroy() {
        stopObserver();
        mOtaFileObserver.setFileObserverCallback(null);
        mFileObserverCallbacks.clear();
        instance = null;
    }


}
