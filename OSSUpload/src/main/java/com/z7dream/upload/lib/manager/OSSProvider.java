package com.z7dream.upload.lib.manager;

import com.alibaba.sdk.android.oss.common.auth.OSSFederationCredentialProvider;
import com.alibaba.sdk.android.oss.common.auth.OSSFederationToken;
import com.z7dream.upload.lib.config.Configuration;
import com.z7dream.upload.lib.db.TaskDaoUtils;
import com.z7dream.upload.lib.db.bean.OSSProviderInfo;
import com.z7dream.upload.lib.net.ApiClient;
import com.z7dream.upload.lib.net.OKHTTP;

import io.reactivex.disposables.Disposable;


/**
 * Created by Z7Dream on 2017/6/22 11:52.
 * Email:zhangxyfs@126.com
 */

public class OSSProvider extends OSSFederationCredentialProvider {
    private OSSProviderInfo info;

    private Disposable disposable;
    private OSSFederationToken ossFederationToken;
    private TaskDaoUtils taskDaoUtils;
    private Long userId, companyId;

    public OSSProvider(TaskDaoUtils taskDaoUtils, Long userId, Long companyId) {
        this.info = taskDaoUtils.getOSSProviderInfo(userId, companyId);
        this.taskDaoUtils = taskDaoUtils;
        this.userId = userId;
        this.companyId = companyId;
    }

    public OSSProvider(TaskDaoUtils taskDaoUtils, OSSProviderInfo ossProviderInfo) {
        this.taskDaoUtils = taskDaoUtils;
        this.info = ossProviderInfo;
    }


    @Override
    public OSSFederationToken getFederationToken() {
        if (info != null && System.currentTimeMillis() - info.getTime() < Configuration.MAX_DELAY_TIME) {
            ossFederationToken = new OSSFederationToken(info.getAccessKeyId(), info.getAccessKeySecret(), info.getSecurityToken(), info.getExpiration());
        } else {
            if (companyId == null) {
                disposable = ApiClient.getOSSPublicInfo(String.valueOf(userId)).subscribe(bean -> {
                    if (taskDaoUtils != null) {
                        taskDaoUtils.saveOSSProvider(userId, null, bean);
                    }
                    ossFederationToken = new OSSFederationToken(bean.accessKeyId, bean.accessKeySecret, bean.securityToken, bean.expiration);
                }, error -> {
                });
            } else {
                disposable = ApiClient.getOSSInfo(String.valueOf(userId), String.valueOf(companyId)).subscribe(bean -> {
                    taskDaoUtils.saveOSSProvider(userId, companyId, bean);
                    ossFederationToken = new OSSFederationToken(bean.accessKeyId, bean.accessKeySecret, bean.securityToken, bean.expiration);
                }, error -> {
                });
            }
        }
        toSleep();
        return ossFederationToken;
    }

    public void destory() {
        if (disposable != null && !disposable.isDisposed()) {
            disposable.dispose();
        }
        taskDaoUtils = null;
    }

    private void toSleep() {
        int time = 500;

        while (time < OKHTTP.HTTP_CONNECTION_TIMEOUT) {
            if (ossFederationToken == null) {
                sleep();
                time += 500;
            } else {
                break;
            }
        }
    }

    private void sleep() {
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
