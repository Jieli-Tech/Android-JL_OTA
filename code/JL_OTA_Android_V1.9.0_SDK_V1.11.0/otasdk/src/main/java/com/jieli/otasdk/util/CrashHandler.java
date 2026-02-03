package com.jieli.otasdk.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;

import com.jieli.jl_bt_ota.util.JL_Log;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * 异常处理
 *
 * @author zqjasonZhong
 * @since 2020/5/22
 */
public class CrashHandler implements Thread.UncaughtExceptionHandler {
    public final static String TAG = CrashHandler.class.getSimpleName();
    //系统默认的UncaughtException处理类
    private Thread.UncaughtExceptionHandler mDefaultHandler;
    //CrashHandler实例
    @SuppressLint("StaticFieldLeak")
    private volatile static CrashHandler INSTANCE;
    //程序的Context对象
    private Context mContext;
    //用来存储设备信息和异常信息
    private final Map<String, String> infos = new HashMap<>();

    private OnExceptionListener mOnExceptionListener;

    /**
     * 保证只有一个CrashHandler实例
     */
    private CrashHandler() {
    }


    /**
     * 获取CrashHandler实例 ,单例模式
     */
    public static CrashHandler getInstance() {
        if (INSTANCE == null) {
            synchronized (CrashHandler.class) {
                if (INSTANCE == null) {
                    INSTANCE = new CrashHandler();
                }
            }
        }
        return INSTANCE;
    }

    /**
     * 初始化
     *
     * @param context 上下文
     */
    public void init(Context context) {
        mContext = context;
        if (mDefaultHandler == null) {
            //获取系统默认的UncaughtException处理器
            mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler();
            //设置该CrashHandler为程序的默认处理器
            Thread.setDefaultUncaughtExceptionHandler(this);
        }
    }

    public void setOnExceptionListener(OnExceptionListener onExceptionListener) {
        mOnExceptionListener = onExceptionListener;
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        if (null == t || null == e) return;
        if (!handleException(e) && mDefaultHandler != null) {
            //如果用户没有处理则让系统默认的异常处理器来处理
            mDefaultHandler.uncaughtException(t, e);
        } else {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException ex) {
                JL_Log.e(TAG, "InterruptedException error : " + getExceptionMsg(ex));
            }
            mDefaultHandler = null;
            mContext = null;
            //退出程序
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(0);
        }
    }

    /**
     * 自定义错误处理,收集错误信息 发送错误报告等操作均在此完成.
     *
     * @param ex 错误
     * @return true:如果处理了该异常信息;否则返回false.
     */
    private boolean handleException(Throwable ex) {
        if (ex == null) {
            return false;
        }
        //使用Toast来显示异常信息
        /*new Thread() {
            @Override
            public void run() {
                Looper.prepare();
                Toast.makeText(mContext, "很抱歉,程序出现异常,即将退出.", Toast.LENGTH_LONG).show();
                Looper.loop();
            }
        }.start();*/
        if (mOnExceptionListener != null) mOnExceptionListener.onException(ex);
        //收集设备参数信息
        collectDeviceInfo(mContext);
        //保存日志文件
        saveCrashInfo2File(ex);
        return true;
    }

    /**
     * 收集设备参数信息
     *
     * @param ctx 上下文
     */
    private void collectDeviceInfo(Context ctx) {
        try {
            if (null == ctx) return;
            PackageManager pm = ctx.getPackageManager();
            if (pm == null) return;
            PackageInfo pi = pm.getPackageInfo(ctx.getPackageName(), PackageManager.GET_ACTIVITIES);
            if (pi != null) {
                String versionName = pi.versionName == null ? "null" : pi.versionName;
                String versionCode = pi.versionCode + "";
                infos.put("versionName", versionName);
                infos.put("versionCode", versionCode);
            }
        } catch (PackageManager.NameNotFoundException e) {
            JL_Log.e(TAG, "an error occured when collect package info : " + getExceptionMsg(e));
        }
        Field[] fields = Build.class.getDeclaredFields();
        for (Field field : fields) {
            try {
                field.setAccessible(true);
                if (field.get(null) != null) {
                    infos.put(field.getName(), field.get(null).toString());
                    JL_Log.d(TAG, field.getName() + " : " + field.get(null));
                }
            } catch (Exception e) {
                JL_Log.e(TAG, "an error occured when collect crash info : " + getExceptionMsg(e));
            }
        }
    }

    /**
     * 保存错误信息到文件中
     *
     * @param ex 错我
     * @return 返回文件名称, 便于将文件传送到服务器
     */
    private String saveCrashInfo2File(Throwable ex) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : infos.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            sb.append(key).append("=").append(value).append("\n");
        }

        Writer writer = new StringWriter();
        PrintWriter printWriter = new PrintWriter(writer);
        ex.printStackTrace(printWriter);
        Throwable cause = ex.getCause();
        while (cause != null) {
            cause.printStackTrace(printWriter);
            cause = cause.getCause();
        }
        printWriter.close();
        String result = writer.toString();
        sb.append(result);
        JL_Log.e(TAG, sb.toString());
        return null;
    }

    private String getExceptionMsg(Exception e) {
        if (e == null) return null;
        return e.toString();
    }

    public interface OnExceptionListener {
        void onException(Throwable ex);
    }
}
