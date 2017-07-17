package com.z7dream.upload.lib.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

import com.z7dream.upload.lib.manager.UploadManager;
import com.z7dream.upload.lib.tool.SPreference;
import com.z7dream.upload.lib.tool.Utils;

import io.objectbox.BoxStore;

/**
 * 上传服务
 */
public class UploadService extends Service {
    private static UploadManager INSTANCE;
    private static BoxStore mBoxStore;

    private static Long nowLoginUserId = null;

    public UploadService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        getUploadManager(SPreference.getUserId());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (INSTANCE != null)
            INSTANCE.destory();
        INSTANCE = null;
        mBoxStore = null;
    }

    /**
     * 启动服务
     *
     * @param context
     * @param boxStore
     */
    public static void startService(Context context, BoxStore boxStore) {
        if (!Utils.isServiceRunning(context, UploadService.class.getName())) {
            context.startService(new Intent(context, UploadService.class));
        }
        mBoxStore = boxStore;
    }

    /**
     * 停止服务
     *
     * @param context
     */
    public static void stopService(Context context) {
        if (!Utils.isServiceRunning(context, UploadService.class.getName())) {
            context.stopService(new Intent(context, UploadService.class));
        }
    }

    public static UploadManager getUploadManager(Long userId) {
        if (userId == null && INSTANCE != null) {
            return INSTANCE;
        }

        if (INSTANCE == null) {
            synchronized (UploadManager.class) {
                if (INSTANCE == null) {
                    INSTANCE = new UploadManager(mBoxStore);
                }
            }
        }

        if (nowLoginUserId == null || nowLoginUserId.longValue() != userId.longValue()) {
            INSTANCE.initOSSProvider(userId);
            nowLoginUserId = userId;
        }

        return INSTANCE;
    }
}
