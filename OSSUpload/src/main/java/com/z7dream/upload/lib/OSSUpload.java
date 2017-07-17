package com.z7dream.upload.lib;

import android.content.Context;

import com.z7dream.upload.lib.db.bean.MyObjectBox;
import com.z7dream.upload.lib.listener.UploadListener;
import com.z7dream.upload.lib.manager.UploadManager;
import com.z7dream.upload.lib.service.NetProbeService;
import com.z7dream.upload.lib.service.UploadService;
import com.z7dream.upload.lib.tool.SPreference;

import io.objectbox.BoxStore;

/**
 * Created by Z7Dream on 2017/6/22 9:47.
 * Email:zhangxyfs@126.com
 */

public class OSSUpload {
    public static void init(Context context, BoxStore boxStore) {
        UploadService.startService(context, boxStore);
        NetProbeService.startService(context);
    }

    public static void init(Context applicationContext) {
        UploadService.startService(applicationContext, MyObjectBox.builder().androidContext(applicationContext).build());
        NetProbeService.startService(applicationContext);
    }


    public static void destory(Context context) {
        UploadService.stopService(context);
        NetProbeService.stopService(context);
    }

//    public static void updateOSSMap() {
//        UploadService.getUploadManager(SPreference.getUserId()).updateOSSMap();
//    }
//
//    public static void updateOSSMap(Long companyId) {
//        UploadService.getUploadManager(SPreference.getUserId()).updateOSSMap(companyId);
//    }

    /**
     * im 上传
     *
     * @param filePath         上传文件的路径
     * @param originalFilePath 上传文件原路径（没有为null）
     * @param listener         回调接口
     */
    public static void uploadIM(String filePath, String originalFilePath, UploadListener listener) {
        upload(filePath, originalFilePath, SPreference.getUserId(), UploadManager.PU_IM, null, null, null, listener);
    }


    /**
     * 用户头像上传
     *
     * @param filePath         上传文件的路径
     * @param originalFilePath 上传文件原路径（没有为null）
     * @param listener         回调接口
     */
    public static void uploadUserPhoto(String filePath, String originalFilePath, UploadListener listener) {
        upload(filePath, originalFilePath, SPreference.getUserId(), UploadManager.PU_PHOTO, null, null, null, listener);
    }


    /**
     * 上传会话数据
     *
     * @param filePath
     */
    public static void uploadBackupJSON(String filePath) {
        upload(filePath, null, SPreference.getUserId(), UploadManager.PU_IM_BACKUP, null, null, null, null);
    }

    /**
     * 根据url获取文件路径
     *
     * @param fileUrl
     */
    public static void getFilePath(String fileUrl) {
        UploadService.getUploadManager(SPreference.getUserId()).getFilePath(fileUrl);
    }

    /**
     * 生成文件url
     *
     * @param fileName  文件名
     * @param which     哪个
     * @param companyId 公司id
     * @param thingsId  事事id
     * @return
     */
    public static String createFileUrl(String fileName, int which, Long companyId, Long thingsId) {
        return UploadService.getUploadManager(SPreference.getUserId()).getOSSFileUrl(fileName, which, companyId, thingsId);
    }

    /**
     * 上传
     *
     * @param filePath         文件路径
     * @param originalFilePath 原文件路径（可以为null）
     * @param userId           用户id
     * @param whichUpload      上传到的地方
     * @param companyId        公司id（可以为null）
     * @param thingsId         事id（可以为null）
     * @param rename           重命名（可以为null，代表不重命名）
     * @param listener         回调
     */
    public static void upload(String filePath, String originalFilePath, Long userId, int whichUpload, Long companyId, Long thingsId, String rename, UploadListener listener) {
        UploadService.getUploadManager(userId).addTask(filePath, originalFilePath, whichUpload, companyId, thingsId, rename, listener);
    }
}
