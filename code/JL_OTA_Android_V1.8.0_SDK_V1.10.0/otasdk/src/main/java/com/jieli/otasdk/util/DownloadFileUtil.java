package com.jieli.otasdk.util;

import android.util.Log;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * @ClassName: DownloadFileUtil
 * @Description: 下载文件
 * @Author: ZhangHuanMing
 * @CreateDate: 2023/4/25 13:50
 */
public class DownloadFileUtil {
    private static final ExecutorService mExecutorService = Executors.newSingleThreadExecutor();

    public static void release() {
        if (mExecutorService != null && !mExecutorService.isShutdown()) {
            mExecutorService.shutdownNow();
        }
    }

    public interface DownloadFileCallback {
        void onEvent(DownloadFileEvent event);
    }

    public static class DownloadFileEvent {
        public String type;
        public Float progress = null;
        public Integer errorCode = null;
        public String errorMsg = null;
    }

    static class DownloadTask extends Thread {
        private String url;
        private DownloadFileCallback callback;
        private String mOutPath;

        public DownloadTask(String url, String outPath, DownloadFileCallback callback) {
            this.callback = callback;
            this.mOutPath = outPath;
            this.url = url;
        }

        @Override
        public void run() {
            super.run();
            final long startTime = System.currentTimeMillis();
            Log.i("DOWNLOAD", "startTime=" + startTime);
            OkHttpClient okHttpClient = new OkHttpClient();

            Request request = new Request.Builder().url(url).build();
            okHttpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onResponse(@NonNull okhttp3.Call call, @NonNull Response response) throws IOException {
//                    callback.onStart();
                    DownloadFileEvent event = new DownloadFileEvent();
                    event.type = "onStart";
                    callback.onEvent(event);
                    InputStream is = null;
                    byte[] buf = new byte[2048];
                    int len = 0;
                    FileOutputStream fos = null;
                    // 储存下载文件的目录
//                        String savePath = Environment.getExternalStorageDirectory().getAbsolutePath();
                    try {
                        is = response.body().byteStream();
                        long total = response.body().contentLength();
                        File file = new File(mOutPath);
//                            File file = new File(savePath, url.substring(url.lastIndexOf("/") + 1));
                        fos = new FileOutputStream(file);
                        long sum = 0;
                        while ((len = is.read(buf)) != -1) {
                            fos.write(buf, 0, len);
                            sum += len;
                            int progress = (int) (sum * 1.0f / total * 100);
                            // 下载中
                            DownloadFileEvent event1 = new DownloadFileEvent();
                            event1.type = "onProgress";
                            event1.progress = Float.valueOf(progress);
                            Log.d("tAG", "progress: "+progress);
                            callback.onEvent(event1);
                        }
                        fos.flush();
                        // 下载完成
                        DownloadFileEvent event2 = new DownloadFileEvent();
                        event2.type = "onStop";
                        callback.onEvent(event2);
//                        callback.onStop();
                        Log.i("DOWNLOAD", "download success");
                        Log.i("DOWNLOAD", "totalTime=" + (System.currentTimeMillis() - startTime));
                    } catch (Exception e) {
                        e.printStackTrace();
                        DownloadFileEvent event3 = new DownloadFileEvent();
                        event3.type = "onError";
                        event3.errorCode = -1;
                        event3.errorMsg = e.getMessage();
                        callback.onEvent(event3);
//                        callback.onError(-1, e.getMessage());
                        Log.i("DOWNLOAD", "download failed");
                    } finally {
                        try {
                            if (is != null)
                                is.close();
                        } catch (IOException e) {
                        }
                        try {
                            if (fos != null)
                                fos.close();
                        } catch (IOException e) {
                        }
                    }

                }

                @Override
                public void onFailure(@NonNull okhttp3.Call call, @NonNull IOException e) {
                    // 下载失败
                    e.printStackTrace();
                    DownloadFileEvent event = new DownloadFileEvent();
                    event.type = "onError";
                    event.errorCode = -1;
                    event.errorMsg = e.getMessage();
                    callback.onEvent(event);
//                    callback.onError(-1, e.getMessage());
                    Log.i("DOWNLOAD", "download failed");
                }
            });
        }
    }

    public static void downloadFile(String url, String outPath, DownloadFileCallback callback) {
        if (!mExecutorService.isShutdown()) {
            mExecutorService.submit(new DownloadTask(url, outPath, callback));
        }
    }
}
