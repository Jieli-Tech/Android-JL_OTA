package com.jieli.otasdk;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import com.jieli.component.ActivityManager;
import com.jieli.component.utils.ToastUtil;
import com.jieli.jlFileTransfer.FileUtils;
import com.jieli.jl_bt_ota.util.CommonUtil;
import com.jieli.jl_bt_ota.util.JL_Log;
import com.jieli.jl_bt_ota.util.PreferencesHelper;
import com.jieli.otasdk.util.AppUtil;
import com.jieli.otasdk.util.CrashHandler;
import com.jieli.otasdk.util.OtaConstant;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.LinkedBlockingQueue;

public class MainApplication extends Application {
    private static MainApplication instance;
    private final boolean isDebug = BuildConfig.DEBUG;
    @SuppressLint("StaticFieldLeak")
    private static SaveLogFileThread mSaveLogFileThread;
    private static String TAG_PREFIX = "";
    public static long FILE_SIZE_LIMIT = 300 * 1024 * 1024; //300M

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        handleLog(false);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        CrashHandler.getInstance().init(this);

        ActivityManager.init(this);
        ToastUtil.init(this);
        CommonUtil.setMainContext(this);
        handleLog(isDebug);
        getLogFileDir();
        getOTAFileDir();
        //第一次使用新的OTA文件夹地址
//        checkOldOTAFileDir();
    }

    public static MainApplication getInstance() {
        return instance;
    }

    public static String getOTAFileDir() {
//        return AppUtil.createFilePath(instance, OtaConstant.DIR_UPGRADE);
        return AppUtil.createDownloadFolderFilePath(instance, OtaConstant.DIR_ROOT, OtaConstant.DIR_UPGRADE);
    }

    public static String getLogFileDir() {
        return AppUtil.createFilePath(instance, OtaConstant.DIR_LOGCAT);
    }

    private void handleLog(boolean isDebug) {
        mSaveLogFileThread = new SaveLogFileThread(this);
        mSaveLogFileThread.start();
        JL_Log.setLog(isDebug);
        JL_Log.setIsSaveLogFile(this, false);
        JL_Log.ILogOutput logOutput = s -> {
            mSaveLogFileThread.addLog(s.getBytes());
        };
        JL_Log.setLogOutput(logOutput);
    }

    private static class SaveLogFileThread extends Thread {
        private final LinkedBlockingQueue<byte[]> mQueue = new LinkedBlockingQueue<>();
        private final Context mContext;
        private volatile boolean isWaiting;
        private volatile boolean isSaving;
        private long fileSize;
        private FileOutputStream mLogFileOutputStream;

        public SaveLogFileThread(Context context) {
            super("SaveLogFileThread");
            mContext = context;
        }

        public void addLog(byte[] data) {
            boolean ret = false;
            if (data != null) {
                try {
                    mQueue.put(data);
                    ret = true;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            if (ret) {
                wakeupSaveThread();
            }
        }

        @Override
        public synchronized void start() {
            fileSize = 0;
            isSaving = mContext != null;
            super.start();
        }

        public synchronized void closeSaveFile() {
            isSaving = false;
            wakeupSaveThread();
        }

        private void wakeupSaveThread() {
            if (isWaiting) {
                synchronized (mQueue) {
                    mQueue.notify();
                }
            }
        }

        @Override
        public void run() {
            createFile(mContext);
            synchronized (mQueue) {
                while (isSaving) {
                    if (mQueue.isEmpty()) {
                        isWaiting = true;
                        try {
                            mQueue.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    } else {
                        isWaiting = false;
                        byte[] data = mQueue.poll();
                        if (data != null && mLogFileOutputStream != null) {
                            try {
                                mLogFileOutputStream.write(data);
                                fileSize += data.length;
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            if (fileSize >= FILE_SIZE_LIMIT) { //文件过大
//                                isSaving = false;
                                //关闭文件流
                                try {
                                    mLogFileOutputStream.close();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                //重新创建文件
                                fileSize=0;
                                createFile(mContext);
//                                break;
                            }
                        }
                    }
                }
            }
            isSaving = false;
            isWaiting = false;
            mQueue.clear();
            if (mLogFileOutputStream != null) {
                try {
                    mLogFileOutputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            mSaveLogFileThread = null;
        }

        private String currentTimeString() {
            final SimpleDateFormat yyyyMMdd_HHmmssSSS = new SimpleDateFormat(
                    "yyyyMMddHHmmss.SSS", Locale.getDefault());
            return yyyyMMdd_HHmmssSSS.format(Calendar.getInstance().getTime());
        }

        private void createFile(Context context) {
            if (null == context) return;
            String saveLogPath = MainApplication.getLogFileDir() + "/" + "ota_log_app_" + currentTimeString() + ".txt";
            try {
                mLogFileOutputStream = new FileOutputStream(saveLogPath, true);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    private void checkOldOTAFileDir() {//删除新升级文件夹的所有文件，拷贝旧文件夹的文件进去
        SharedPreferences sharedPreferences = PreferencesHelper.getSharedPreferences(this);
        //是不是第一次使用新的OTA文件路径
        boolean isFirstUseNewOTAFileDir = sharedPreferences.getBoolean("isFirstUseNewOTAFileDir", true);
        if (!isFirstUseNewOTAFileDir) return;
        String newPath = getOTAFileDir();
        File newFolder = new File(newPath);
        JL_Log.d("TAG", "checkOldOTAFileDir: " + newFolder.listFiles().length);
        if (newFolder.listFiles().length > 0) {
            for (File tempFile : newFolder.listFiles()) {
                tempFile.delete();
            }
        }
        String oldPath = AppUtil.createFilePath(instance, OtaConstant.DIR_UPGRADE);
        File oldFolder = new File(oldPath);
        if (oldFolder.listFiles().length > 0) {//原升级文件夹存在文件
            for (File tempFile : oldFolder.listFiles()) {
                String resultPath =
                        newPath + File.separator + tempFile.getName();
                File resultFile = new File(resultPath);
                if (!resultFile.exists()) {
                    InputStream inputStream = null;
                    try {
                        inputStream = new FileInputStream(tempFile);
                        FileUtils.copyFile(inputStream, resultPath);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        PreferencesHelper.putBooleanValue(this, "isFirstUseNewOTAFileDir", false);
    }
}

