package com.z7dream.upload.lib.listener;

import com.alibaba.sdk.android.oss.ClientException;
import com.alibaba.sdk.android.oss.ServiceException;
import com.alibaba.sdk.android.oss.model.ResumableUploadRequest;
import com.z7dream.upload.lib.db.bean.TaskInfo;

/**
 * Created by Z7Dream on 2017/1/10 14:21.
 * Email:zhangxyfs@126.com
 */

public abstract class UploadListener implements UploadCallback {

    /**
     * 在添加任务的时候
     *
     * @param info
     */
    @Override
    public void onAdd(TaskInfo info) {

    }

    /**
     * 当任务移除时候
     *
     * @param info
     */
    @Override
    public void onRemove(TaskInfo info) {

    }

    /**
     * 开始下载时候
     *
     * @param info
     */
    @Override
    public void onStart(TaskInfo info) {

    }

    /**
     * 下载进度
     *
     * @param info
     */
    @Override
    public void onProgress(TaskInfo info) {

    }

    /**
     * 成功
     *
     * @param info
     */
    public abstract void onSuccess(TaskInfo info);

    /**
     * 出错
     *
     * @param info
     * @param resumableUploadRequest
     * @param clientExcepion
     * @param serviceException
     */
    @Override
    public void onFailure(TaskInfo info, ResumableUploadRequest resumableUploadRequest, ClientException clientExcepion, ServiceException serviceException) {

    }
}
