package com.z7dream.upload.lib.tool;

import android.content.Context;
import android.os.Environment;

import com.z7dream.upload.Appli;

import java.io.File;

/**
 * 缓存目录控制
 * Created by xiaoyu.zhang on 2016/11/10 14:57
 *  
 */
public class CacheManager {
    public static final int OSS = 0x01500;
    private static final String STR_OSS = "oss_record";

    private static final String NOMEDIA = ".nomedia";


    public static String getSystemPicCachePath() {
        File file = Appli.getContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if (file != null) {
            return file.getAbsolutePath() + File.separator;
        }
        return "Android" + File.separator + "data" + File.separator + "com.z7dream.upload" + File.separator + "files" + File.separator + "Pictures" + File.separator;
    }

    public static File getSystemPicCachePathFile() {
        File file = Appli.getContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if (file != null) {
            return file;
        }
        file = new File("Android" + File.separator + "data" + File.separator + "com.z7dream.upload" + File.separator + "files" + File.separator + "Pictures");
        if (!file.exists()) {
            file.mkdirs();
        }
        return file;
    }

    /**
     * 获取缓存路径
     */
    public static String getCachePath(Context context) {
        String savePath = getSaveFilePath() + File.separator + context.getPackageName() + File.separator + "cache";
        File fDir = new File(savePath);
        if (!fDir.exists()) {
            fDir.mkdirs();
        }
        return savePath;
    }

    public static String getRelativePath(Context context) {
        String savePath = getSaveFilePath() + File.separator + context.getPackageName() + File.separator + "cache";
        String relatviePath = File.separator + context.getPackageName() + File.separator + "cache";
        File fDir = new File(savePath);
        if (!fDir.exists()) {
            fDir.mkdirs();
        }

        return relatviePath;
    }

    public static String getCachePath() {
        String savePath = getSaveFilePath() + File.separator + "com.z7dream.upload" + File.separator + "cache";
        File fDir = new File(savePath);
        if (!fDir.exists()) {
            fDir.mkdirs();
        }
        return savePath;
    }

    public static String getRelativePath() {
        String savePath = getSaveFilePath() + File.separator + "com.z7dream.upload" + File.separator + "cache";
        String relatviePath = File.separator + "com.z7dream.upload" + File.separator + "cache";
        File fDir = new File(savePath);
        if (!fDir.exists()) {
            fDir.mkdirs();
        }
        return relatviePath;
    }

    public static String getCachePath(int which) {
        return getCachePath(null, which);
    }

    public static String getCachePathNoSepa(int which) {
        String path = getCachePath(null, which);
        if (path.endsWith(File.separator)) {
            return path.substring(0, path.length() - 1);
        }
        return path;
    }

    public static String getCachePath(Context context, int which) {
        String savePath = "";
        if (context == null) {
            savePath = getCachePath();
        } else {
            savePath = getCachePath(context);
        }
        savePath = getSavePath(savePath, which);
        String nomediaPath = savePath + NOMEDIA;

        File fDir = new File(savePath);
        if (!fDir.exists()) {
            fDir.mkdirs();
        }
        return savePath;
    }

    public static String getRelativePath(Context context, int which) {
        String savePath = "";
        if (context == null) {
            savePath = getRelativePath();
        } else {
            savePath = getRelativePath(context);
        }
        savePath = getSavePath(savePath, which);
        File fDir = new File(savePath);
        if (!fDir.exists()) {
            fDir.mkdirs();
        }
        return savePath;
    }

    private static String getSavePath(String savePath, int which) {
        savePath += File.separator;
        if (which == OSS) {
            savePath += STR_OSS;
        }
        savePath += File.separator;
        return savePath;
    }

    /**
     * 帮你创建个目录
     *
     * @param which           父目录
     * @param childFolderName 子目录名
     * @return
     */
    public static String getPath(int which, String childFolderName) {
        String path = getCachePath(Appli.getContext(), which);
        String needPath = path + childFolderName + File.separator;
        String nomediaPath = needPath + NOMEDIA;

        File fDir = new File(needPath);
        if (!fDir.exists()) {
            fDir.mkdirs();
        }
        File npDir = new File(nomediaPath);
        if (!npDir.exists()) {
            npDir.mkdir();
        }
        return needPath;
    }


    public static String getSaveFilePath() {
        File file = null;
        String rootPath = "";
        String status = Environment.getExternalStorageState();
        if (status.equals(Environment.MEDIA_MOUNTED)) {
            file = Environment.getExternalStorageDirectory();//获取跟目录
            rootPath = file.getPath();
        } else {
            Dev_MountInfo dev = Dev_MountInfo.getInstance();
            Dev_MountInfo.DevInfo info = dev.getInternalInfo();
            if (info != null) {
                rootPath = info.getPath();
            } else {
                return "";
            }
        }
        return rootPath;
    }

}
