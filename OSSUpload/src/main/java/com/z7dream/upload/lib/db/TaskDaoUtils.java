package com.z7dream.upload.lib.db;


import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.z7dream.upload.lib.config.OSSInfo;
import com.z7dream.upload.lib.db.bean.OSSProviderInfo;
import com.z7dream.upload.lib.db.bean.OSSProviderInfo_;
import com.z7dream.upload.lib.db.bean.TaskInfo;
import com.z7dream.upload.lib.db.bean.TaskInfo_;
import com.z7dream.upload.lib.tool.TaskState;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.objectbox.Box;
import io.objectbox.BoxStore;
import io.objectbox.query.QueryBuilder;

/**
 * Created by Z7Dream on 2017/6/21 17:30.
 * Email:zhangxyfs@126.com
 */

public class TaskDaoUtils {
    private Box<OSSProviderInfo> ossProviderBox;
    private Box<TaskInfo> taskBox;

    public TaskDaoUtils(BoxStore boxStore) {
        ossProviderBox = boxStore.boxFor(OSSProviderInfo.class);
        taskBox = boxStore.boxFor(TaskInfo.class);
    }

    /**
     * 获取oss token
     *
     * @param userId
     * @param companyId
     * @return
     */
    public OSSProviderInfo getOSSProviderInfo(Long userId, Long companyId) {
        boolean isPublic = companyId == null || companyId <= 0;
        QueryBuilder<OSSProviderInfo> queryBuilder = ossProviderBox.query().equal(OSSProviderInfo_.nowLoginUserId, userId).equal(OSSProviderInfo_.isPublic, isPublic);
        if (companyId != null) {
            queryBuilder.equal(OSSProviderInfo_.companyId, companyId);
        }
        OSSProviderInfo info = queryBuilder.build().findUnique();
        if (info == null) {
            info = new OSSProviderInfo();
            info.setAccessKeyId("xxxx");
            info.setAccessKeySecret("xxxx");
            info.setExpiration("xxxx");
            info.setSecurityToken("xxxx");
            info.setEndPoint("http://oss-cn-xxxx.aliyuncs.com");
            info.setTime(0);
            info.setBucketName("xxxx");
        }
        return info;
    }

    /**
     * 获取oss token 列表
     *
     * @param userId
     * @return
     */
    public Map<String, OSSProviderInfo> getOSSProviderInfoList(Long userId) {
        Map<String, OSSProviderInfo> map = new HashMap<>();
        List<OSSProviderInfo> list = ossProviderBox.query().equal(OSSProviderInfo_.nowLoginUserId, userId).build().find();
        for (int i = 0; i < list.size(); i++) {
            OSSProviderInfo info = list.get(i);
            String key = String.valueOf(info.getNowLoginUserId()) +
                    (info.getCompanyId() == null || info.getCompanyId() <= 0 ? "" : "_" + info.getCompanyId());
            map.put(key, info);
        }
        return map;
    }

    /**
     * 保存oss token
     *
     * @param userId
     * @param companyId
     * @param dataBean
     */
    public void saveOSSProvider(@NonNull Long userId, Long companyId, OSSInfo dataBean) {
        if (userId < 0) {
            return;
        }
        boolean isPublic = (companyId == null || companyId <= 0);
        OSSProviderInfo info = getOSSProviderInfo(userId, companyId);
        if (info.isNull()) {
            info = new OSSProviderInfo();
            info.setId(userId + (isPublic ? 0L : companyId));
        }
        if (dataBean != null) {
            info.setNowLoginUserId(userId);
            info.setCompanyId(companyId);
            info.setAccessKeyId(dataBean.accessKeyId);
            info.setAccessKeySecret(dataBean.accessKeySecret);
            info.setExpiration(dataBean.expiration);
            info.setSecurityToken(dataBean.securityToken);
            info.setTime(dataBean.expirationTime);
            info.setEndPoint(dataBean.endpoint.startsWith("http://") ? dataBean.endpoint.replace("http://", "") : dataBean.endpoint);
            info.setBucketName(dataBean.bucketName);
            info.setIsPublic(isPublic);
        } else {
            info.setAccessKeyId("xxxx");
            info.setAccessKeySecret("xxxx");
            info.setExpiration("xxxx");
            info.setSecurityToken("xxxx");
            info.setEndPoint("http://oss-cn-xxxx.aliyuncs.com");
            info.setTime(0);
            info.setBucketName("xxxx");
        }

        ossProviderBox.put(info);
    }

    /**
     * 获取任务信息
     *
     * @param key
     * @return
     */
    public TaskInfo getTaskInfoByKey(String key) {
        return taskBox.query().equal(TaskInfo_.taskKey, key).build().findUnique();
    }

    public String getFilePath(String fileUrl) {
        String filePath = "";
        List<TaskInfo> list = taskBox.query().equal(TaskInfo_.fileUrl, fileUrl).build().find();
        if (list.size() > 0) {
            filePath = list.get(0).getFilePath();
        }
        if (!TextUtils.isEmpty(filePath)) {
            File file = new File(filePath);
            if (file.exists()) {
                return filePath;
            }
        }
        return "";
    }

    public void deleteAliyunInfoByKey(String key) {
        TaskInfo taskInfo = getTaskInfoByKey(key);

        if (taskInfo == null)
            return;

        if (!TextUtils.isEmpty(taskInfo.getFilePath())) {
            File file = new File(taskInfo.getFilePath());
            if (file.exists()) {
                file.delete();
            }
        }
        taskBox.remove(taskInfo);
    }

    /**
     * 清除已经上传成功的文件
     */
    public void clearAlreadyUploadTempFile() {
        List<TaskInfo> list = taskBox.query().equal(TaskInfo_.state, TaskState.FINISH).notNull(TaskInfo_.filePath).build().find();
        for (int i = 0; i < list.size(); i++) {
            TaskInfo info = list.get(i);
            File file = new File(info.getFilePath());
            if (file.exists()) {
                file.delete();
            }
            info.setFilePath(null);
        }
        taskBox.put(list);
    }

    public void clearAlreadyUploadTempFile(String taskKey) {
        TaskInfo info = taskBox.query().equal(TaskInfo_.taskKey, taskKey).build().findUnique();
        if (info != null) {
            if (!TextUtils.isEmpty(info.getFilePath())) {
                File file = new File(info.getFilePath());
                if (file.exists()) {
                    file.delete();
                }
            }
            info.setFilePath(null);
            taskBox.put(info);
        }
    }

    /**
     * 获取所有需要上传的任务
     *
     * @return
     */
    public List<TaskInfo> getAllNeedUploadInfo() {
        return taskBox.query().notEqual(TaskInfo_.state, TaskState.FINISH).notNull(TaskInfo_.filePath).build().find();
    }

    /**
     * 保存或更新任务
     *
     * @param info
     */
    public void saveOrUpdateInfo(TaskInfo info) {
        taskBox.put(info);
    }

    /**
     * 删除任务
     *
     * @param info
     */
    public void deleteInfo(TaskInfo info) {
        taskBox.remove(info);
    }

    /**
     * 删除任务
     *
     * @param taskKey
     */
    public void deleteInfoByTaskKey(String taskKey) {
        TaskInfo taskInfo = getTaskInfoByKey(taskKey);
        deleteInfo(taskInfo);
    }


    public void destory() {
        ossProviderBox = null;
        taskBox = null;
    }
}
