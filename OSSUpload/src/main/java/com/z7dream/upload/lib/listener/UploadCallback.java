package com.z7dream.upload.lib.listener;

import com.alibaba.sdk.android.oss.ClientException;
import com.alibaba.sdk.android.oss.ServiceException;
import com.alibaba.sdk.android.oss.model.ResumableUploadRequest;
import com.z7dream.upload.lib.db.bean.TaskInfo;

/**
 * Created by Z7Dream on 2017/1/9 18:15.
 * Email:zhangxyfs@126.com
 */

public interface UploadCallback {
    void onAdd(TaskInfo info);

    void onRemove(TaskInfo info);

    void onStart(TaskInfo info);

    void onProgress(TaskInfo info);

    void onSuccess(TaskInfo info);

    void onFailure(TaskInfo info, ResumableUploadRequest resumableUploadRequest, ClientException clientExcepion, ServiceException serviceException);
}
