package com.z7dream.upload.lib.manager;


import android.content.res.Resources;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.Log;

import com.alibaba.sdk.android.oss.ClientConfiguration;
import com.alibaba.sdk.android.oss.ClientException;
import com.alibaba.sdk.android.oss.OSS;
import com.alibaba.sdk.android.oss.OSSClient;
import com.alibaba.sdk.android.oss.ServiceException;
import com.alibaba.sdk.android.oss.callback.OSSCompletedCallback;
import com.alibaba.sdk.android.oss.internal.OSSAsyncTask;
import com.alibaba.sdk.android.oss.model.ObjectMetadata;
import com.alibaba.sdk.android.oss.model.ResumableUploadRequest;
import com.alibaba.sdk.android.oss.model.ResumableUploadResult;
import com.z7dream.upload.Appli;
import com.z7dream.upload.R;
import com.z7dream.upload.lib.config.Configuration;
import com.z7dream.upload.lib.db.TaskDaoUtils;
import com.z7dream.upload.lib.db.bean.OSSProviderInfo;
import com.z7dream.upload.lib.db.bean.TaskInfo;
import com.z7dream.upload.lib.listener.UploadListener;
import com.z7dream.upload.lib.net.ApiClient;
import com.z7dream.upload.lib.tool.CacheManager;
import com.z7dream.upload.lib.tool.FileUtils;
import com.z7dream.upload.lib.tool.SPreference;
import com.z7dream.upload.lib.tool.rxjava.RxBus;
import com.z7dream.upload.lib.tool.rxjava.RxSchedulersHelper;

import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.objectbox.BoxStore;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;

import static android.content.ContentValues.TAG;
import static com.z7dream.upload.lib.config.Configuration.MAX_UPLOAD_RETRY_NUM;
import static com.z7dream.upload.lib.tool.TaskState.ERROR;
import static com.z7dream.upload.lib.tool.TaskState.FINISH;
import static com.z7dream.upload.lib.tool.TaskState.NONE;
import static com.z7dream.upload.lib.tool.TaskState.RUNNING;
import static com.z7dream.upload.lib.tool.TaskState.STATUS_UPLOAD;

/**
 * Created by Z7Dream on 2017/6/22 9:40.
 * Email:zhangxyfs@126.com
 */

public class UploadManager {
    private TaskDaoUtils taskDaoUtils;
    private Map<String, OSSProviderInfo> ossProviderMap;
    private int upload_num = 0;//当前正在上传的数量
    private boolean whenStop = false;//结束循环

    private HashMap<String, TaskInfo> uploadingQueue, needToReUploadQueue;
    private Observable<Boolean> netProbeObservable;//用于处理当没有任务处理的时候，去做失败任务的重新上传
    private Disposable intervalDetailDisposable;//循环处理正常的上传任务队列
    private Disposable intervalDetailErrorDisposable;//用于循环处理失败任务数据，处理完会停止该循环
    private ClientConfiguration clientConfiguration;
    private OSS oss;
    private String endPoint = "";

    public static final String NET_IDLE_OBSERVABLE = "net_idle_observable";

    public static final int PU_IM = 0x010;//im中各种文件
    public static final int PU_IM_BACKUP = 0x015;//数据备份
    public static final int PU_IM_TEAM_ICON = 0x020;//IM群图片
    public static final int PU_COMPANY_ICON = 0x030;//公司图片
    public static final int PU_PHOTO = 0x040;//用户头像

    public UploadManager(BoxStore boxStore) {
        init(boxStore);
    }

    private void init(BoxStore boxStore) {
        clientConfiguration = new ClientConfiguration();
        clientConfiguration.setConnectionTimeout(Configuration.HTTP_CONNECTION_TIMEOUT); // 连接超时，默认15秒
        clientConfiguration.setSocketTimeout(Configuration.HTTP_SOCKET_TIMEOUT); // socket超时，默认15秒
        clientConfiguration.setMaxConcurrentRequest(Configuration.MAX_UPLOAD_NUM); // 最大并发请求书
        clientConfiguration.setMaxErrorRetry(Configuration.MAX_RETRY_NUM); // 失败后最大重试次数
        taskDaoUtils = new TaskDaoUtils(boxStore);

        whenStop = false;
        uploadingQueue = new HashMap<>();
        needToReUploadQueue = new HashMap<>();
        ossProviderMap = new HashMap<>();

        intervalDetailDisposable = Flowable.interval(Configuration.INTERVAL_TIME, TimeUnit.MILLISECONDS)
                .filter(x -> uploadingQueue.size() > 0 && upload_num < Configuration.MAX_UPLOAD_NUM)
                .subscribe(x -> {
                    if (upload_num >= Configuration.MAX_UPLOAD_NUM) return;
                    HashMap<String, TaskInfo> tempQueue = new HashMap<>();
                    for (Map.Entry<String, TaskInfo> entry : uploadingQueue.entrySet()) {
                        if (upload_num >= Configuration.MAX_UPLOAD_NUM) break;
                        tempQueue.put(entry.getKey(), entry.getValue());
                        upload_num = upload_num + 1;
                    }
                    for (Map.Entry<String, TaskInfo> entry : tempQueue.entrySet()) {
                        if (uploadingQueue.get(entry.getKey()) != null)
                            uploadingQueue.remove(entry.getKey());

                        Flowable.just(1).compose(RxSchedulersHelper.fio()).subscribe(e -> uploadFile(entry.getValue()), Throwable::printStackTrace);
                    }
//                    RxBus.get().post("queue_info_observable", "从上传队列中取" + tempQueue.size() + "个数据开始上传\n\n");
                    tempQueue.clear();
                    Thread.sleep(Configuration.INTERVAL_TIME);
                }, error -> {
//                    RxBus.get().post("error_info_observable", "上传队列出现错误：\n" + error.getMessage() + "\n\n");
                });

        netProbeObservable = RxBus.get().register(NET_IDLE_OBSERVABLE, Boolean.class);
        netProbeObservable.compose(RxSchedulersHelper.io())
                .filter(b -> uploadingQueue.size() == 0 && needToReUploadQueue.size() > 0
                        && (intervalDetailErrorDisposable == null || intervalDetailErrorDisposable.isDisposed()))
                .subscribe(b -> {
                    intervalDetailErrorDisposable = Flowable.interval(1000, TimeUnit.MILLISECONDS)
                            .compose(RxSchedulersHelper.fio())
                            .subscribe(time -> {
                                int nowUploadingListSize = uploadingQueue.size();
                                int needToReUploadListSize = needToReUploadQueue.size();

                                if (nowUploadingListSize < Configuration.MAX_UPLOAD_NUM) {
                                    int needSelectNum = Configuration.MAX_UPLOAD_NUM - nowUploadingListSize;

                                    if (needToReUploadListSize > needSelectNum) {
                                        int size = 0;
                                        for (Map.Entry<String, TaskInfo> entry : needToReUploadQueue.entrySet()) {
                                            size++;
//                                            entry.getValue().setUploadCallback(null);
                                            uploadingQueue.put(entry.getKey(), entry.getValue());//向上传队列添加第i个task
                                            needToReUploadQueue.remove(entry.getKey());//需要上传队列移除第一个元素
                                            if (size == needSelectNum) {
                                                break;
                                            }
                                        }
                                    } else if (needToReUploadListSize > 0) {
                                        uploadingQueue.putAll(needToReUploadQueue);
                                        needToReUploadQueue.clear();
                                    }

//                                    RxBus.get().post("queue_info_observable", "处理闲时上传数据：从失败队列中获取" + needSelectNum + "个数据放入上传队列中\n\n");
                                }

                                if (needToReUploadQueue.size() == 0)
                                    intervalDetailErrorDisposable.dispose();
                                else
                                    Thread.sleep(Configuration.INTERVAL_TIME);
                            }, error -> {
//                                RxBus.get().post("error_info_observable", "闲时上传循环出现错误：\n" + error.getMessage() + "\n\n");
                            });
                }, error -> {
                    RxBus.get().unregister(NET_IDLE_OBSERVABLE, netProbeObservable);
                    netProbeObservable = RxBus.get().register(NET_IDLE_OBSERVABLE, Boolean.class);
//                    RxBus.get().post("error_info_observable", "闲时上传队列出现错误：\n" + error.getMessage() + "\n\n");
                });
    }

    /**
     * 初始化oss token
     *
     * @param userId
     */
    public void initOSSProvider(Long userId) {
        ossProviderMap.putAll(taskDaoUtils.getOSSProviderInfoList(userId));

        List<TaskInfo> list = taskDaoUtils.getAllNeedUploadInfo();
        for (int i = 0; i < list.size(); i++) {
            TaskInfo info = list.get(i);
            File file = new File(info.getFilePath());
            if (file.exists() && info.getRetryNum() < MAX_UPLOAD_RETRY_NUM) {
                needToReUploadQueue.put(info.getTaskKey(), info);
            } else {
                taskDaoUtils.deleteInfoByTaskKey(info.getTaskKey());//放弃上传该数据
            }
        }
    }


    /**
     * 更新oss token
     */
    public void updateOSSMap() {
        ApiClient.getOSSProviderInfo(SPreference.getUserId(), null).subscribe(bean -> {
            taskDaoUtils.saveOSSProvider(SPreference.getUserId(), null, bean);
            OSSProviderInfo pi = taskDaoUtils.getOSSProviderInfo(SPreference.getUserId(), null);
            ossProviderMap.put(String.valueOf(SPreference.getUserId()), pi);
        }, error -> {
//            RxBus.get().post("error_info_observable", "获取oss token出现错误：\n" + error.getMessage() + "\n\n");
        });
    }

    /**
     * 更新oss
     *
     * @param companyId
     */
    public void updateOSSMap(Long companyId) {
        if (ossProviderMap.get(SPreference.getUserId() + "_" + companyId) != null) {
            return;
        }
        ApiClient.getOSSProviderInfo(SPreference.getUserId(), companyId).subscribe(bean -> {
            taskDaoUtils.saveOSSProvider(SPreference.getUserId(), bean.companyId, bean);
            OSSProviderInfo pi = taskDaoUtils.getOSSProviderInfo(SPreference.getUserId(), null);
            ossProviderMap.put(SPreference.getUserId() + "_" + bean.companyId, pi);
        }, error -> {
        });
    }

    /**
     * 添加任务
     *
     * @param filePath         文件路径
     * @param originalFilePath 原文件
     * @param whichUpload
     * @param companyId        公司id
     * @param thingsId         事事id
     * @param reName           重命名
     * @param listener         回调
     */
    public void addTask(String filePath, String originalFilePath, int whichUpload, Long companyId, Long thingsId, String reName, UploadListener listener) {
        String taskKey = String.valueOf(filePath.hashCode());
        TaskInfo info = taskDaoUtils.getTaskInfoByKey(taskKey);

        if (info == null) {
            File file = new File(filePath);
            if (file.length() == 0 || !file.exists() || file.isDirectory()) {
                return;
            }
            String newFileName = file.getName();
            String exc = FileUtils.getExtensionName(newFileName);
            if (reName != null) {
                if (reName.split("\\.").length > 0)
                    newFileName = reName;
                else
                    newFileName = reName.split("\\.")[0] + "." + exc;
            }
            String uploadPath = getUploadPath(whichUpload, companyId, thingsId);

            info = new TaskInfo();
            info.setTaskKey(taskKey);
            info.setState(NONE);
            info.setStatus(STATUS_UPLOAD);
            info.setFilePath(filePath);
            info.setFileName(newFileName);
            info.setOriginalFilePath(originalFilePath);
            info.setUserId(SPreference.getUserId());
            info.setCompanyId(companyId);
            info.setToUploadPath(uploadPath);
            info.setFileUrl(getOSSFileUrl(newFileName, whichUpload, companyId, thingsId));
            info.setUploadCallback(listener);

            OSSProviderInfo providerInfo = ossProviderMap.get(SPreference.getUserId() + (companyId != null && companyId >= 0 ? "_" + companyId : ""));
            if (providerInfo == null || TextUtils.equals("xxxx", providerInfo.getAccessKeyId())) {//判断是否有数据，没数据的话就去取个默认数据
                providerInfo = taskDaoUtils.getOSSProviderInfo(SPreference.getUserId(), companyId);
                ossProviderMap.put(SPreference.getUserId() + (companyId != null && companyId >= 0 ? "_" + companyId : ""), providerInfo);
            }
            if (providerInfo == null || TextUtils.equals("xxxx", providerInfo.getAccessKeyId())) {//如果还是没有数据就去服务器取
                taskDaoUtils.saveOrUpdateInfo(info);

                ApiClient.getOSSProviderInfo(info.getUserId(), companyId).subscribe(bean -> {//获取成功
                    taskDaoUtils.saveOSSProvider(SPreference.getUserId(), companyId, bean);
                    OSSProviderInfo pi = taskDaoUtils.getOSSProviderInfo(SPreference.getUserId(), companyId);
                    ossProviderMap.put(SPreference.getUserId() + (companyId != null && companyId >= 0 ? "_" + companyId : ""), pi);

                    TaskInfo ti = taskDaoUtils.getTaskInfoByKey(taskKey);
                    ti.setBucketName(pi.getBucketName());
                    ti.setEndPoint(pi.getEndPoint());
                    if (listener != null) listener.onAdd(ti);
                    taskDaoUtils.saveOrUpdateInfo(ti);
                    uploadingQueue.put(taskKey, ti);
                }, error -> {//获取失败
                    listener.onFailure(taskDaoUtils.getTaskInfoByKey(taskKey), null, null, null);
//                    RxBus.get().post("error_info_observable", "获取oss token出现错误：将自动调用监听的失败回掉\n" + error.getMessage() + "\n\n");
                });
            } else {
                info.setBucketName(providerInfo.getBucketName());
                info.setEndPoint(providerInfo.getEndPoint());
                if (listener != null) listener.onAdd(info);

                taskDaoUtils.saveOrUpdateInfo(info);
                uploadingQueue.put(taskKey, info);
            }
        } else if (info.getState() == FINISH) {
            if (listener != null) listener.onSuccess(info);
        } else {
            info.setUploadCallback(listener);
            uploadingQueue.put(taskKey, info);
        }
    }

    /**
     * 根据文件url获取文件路径
     *
     * @param fileUrl
     * @return
     */
    public String getFilePath(String fileUrl) {
        return taskDaoUtils.getFilePath(fileUrl);
    }

    /**
     * 生成 oss文件url
     *
     * @param fileName 文件名
     * @param which
     * @return
     */
    public String getOSSFileUrl(String fileName, int which, Long companyId, Long thingId) {
        return getUploadUrlHead(companyId) + getUploadPath(which, companyId, thingId) + fileName;
    }

    public String getUploadUrlHead(Long companyId) {
        String HEAD;
        OSSProviderInfo info = taskDaoUtils.getOSSProviderInfo(SPreference.getUserId(), companyId);
        HEAD = info == null ? "" : info.getBucketName() + "." + info.getEndPoint();
        return "http://" + (HEAD.endsWith("/") ? HEAD : (HEAD + "/"));
    }

    private void uploadFile(TaskInfo info) {
        if (!TextUtils.equals(endPoint, info.getEndPoint()) || oss == null) {
            endPoint = info.getEndPoint();
            OSSProvider ossProvider = new OSSProvider(taskDaoUtils, SPreference.getUserId(), info.getCompanyId());
            oss = new OSSClient(Appli.getContext(), endPoint, ossProvider, clientConfiguration);
        }

        File file = new File(info.getFilePath());
        info.setTotalLength(file.length());
        long mPreviousTime = System.currentTimeMillis(); //上次更新的时间，用于计算下载速度
        String uploadPath = info.getToUploadPath();
        String recordDirectory = CacheManager.getCachePath(Appli.getContext(), CacheManager.OSS);

        // 创建断点上传请求，参数中给出断点记录文件的保存位置，需是一个文件夹的绝对路径
        ResumableUploadRequest request = new ResumableUploadRequest(info.getBucketName(), uploadPath + info.getFileName(), info.getFilePath(), recordDirectory);
        ObjectMetadata objectMetadata = new ObjectMetadata();
        objectMetadata.setContentLength(file.length());
        objectMetadata.setContentType(getContentType(info.getFilePath()));
        request.setMetadata(objectMetadata);

        // 设置上传过程回调
        request.setProgressCallback((request1, currentSize, totalSize) -> {
            info.setTotalLength(totalSize);
            info.setCurrentSize(currentSize);
            info.setProgress((currentSize * 1f) / (totalSize * 1f));
            info.setState(RUNNING);

            //计算下载速度
            long totalTime = (System.currentTimeMillis() - mPreviousTime) / 1000;
            if (totalTime == 0) {
                totalTime += 1;
            }
            long networkSpeed = currentSize / totalTime;
            info.setNetworkSpeed(networkSpeed);

            taskDaoUtils.saveOrUpdateInfo(info);

            if (info.getUploadCallback() != null)
                info.getUploadCallback().onProgress(info);
        });
        if (info.getUploadCallback() != null)
            info.getUploadCallback().onStart(info);

        OSSAsyncTask ossAsyncTask = oss.asyncResumableUpload(request, new OSSCompletedCallback<ResumableUploadRequest, ResumableUploadResult>() {
            @Override
            public void onSuccess(ResumableUploadRequest resumableUploadRequest, ResumableUploadResult resumableUploadResult) {
                info.setState(FINISH);
                info.setNetworkSpeed(0);
                taskDaoUtils.saveOrUpdateInfo(info);

                if (info.getUploadCallback() != null)
                    info.getUploadCallback().onSuccess(info);
//                taskDaoUtils.clearAlreadyUploadTempFile(info.getTaskKey());
                upload_num--;
//                RxBus.get().post("queue_info_observable", "上传成功：" + info.getFileUrl() + "\n\n");
            }

            @Override
            public void onFailure(ResumableUploadRequest resumableUploadRequest, ClientException clientExcepion, ServiceException serviceException) {
                if (clientExcepion != null) {
                    // 本地异常如网络异常等
                    clientExcepion.printStackTrace();
                }
                if (serviceException != null) {
                    // 服务异常
                    Log.e(TAG, serviceException.getErrorCode());
                    Log.e(TAG, serviceException.getRequestId());
                    Log.e(TAG, serviceException.getHostId());
                    Log.e(TAG, serviceException.getRawMessage());
                }
                info.setState(ERROR);
                info.setNetworkSpeed(0);
                info.setRetryNum(info.getRetryNum() + 1);

                File file = new File(info.getFilePath());
                if (info.getRetryNum() < Configuration.MAX_UPLOAD_RETRY_NUM && file.exists()) {
                    taskDaoUtils.saveOrUpdateInfo(info);
                    needToReUploadQueue.put(info.getTaskKey(), info);
                } else {
                    taskDaoUtils.deleteInfoByTaskKey(info.getTaskKey());//放弃上传该数据
                }
//                uploadingQueue.remove(info.getTaskKey());
                if (info.getUploadCallback() != null)
                    info.getUploadCallback().onFailure(info, resumableUploadRequest, clientExcepion, serviceException);
                upload_num--;
//                RxBus.get().post("error_info_observable", "\n任务上传失败，文件url=" + info.getFileUrl() + "\n，尝试次数：" + info.getRetryNum() + "\n" + clientExcepion.getMessage() + "\n\n");
            }
        });

        info.ossAsyncTask = ossAsyncTask;

//            ossAsyncTask.waitUntilFinished();//可以等待直到任务完成
    }

    private String getContentType(String extName) {
        String returnType = "";
        if (extName.toLowerCase().endsWith(".jpg")) {
            returnType = "image/jpeg";
        } else if (extName.toLowerCase().endsWith(".bmp")) {
            returnType = "image/bmp";
        } else if (extName.toLowerCase().endsWith(".png")) {
            returnType = "image/png";
        } else if (extName.toLowerCase().endsWith(".doc") || extName.toLowerCase().endsWith(".docx")) {
            returnType = "application/msword";
        } else if (extName.toLowerCase().endsWith(".pdf")) {
            returnType = "application/pdf";
        } else if (extName.toLowerCase().endsWith(".rar")) {
            returnType = "aplication/rar";
        } else if (extName.toLowerCase().endsWith(".zip")) {
            returnType = "aplication/zip";
        } else if (extName.toLowerCase().endsWith(".xls") || extName.toLowerCase().endsWith(".xlsx")) {
            returnType = "application/vnd.ms-excel";
        } else if (extName.toLowerCase().endsWith(".ppt") || extName.toLowerCase().endsWith(".pptx")) {
            returnType = "application/vnd.ms-powerpoint";
        } else if (extName.toLowerCase().endsWith(".apk")) {
            returnType = "application/vnd.android.package-archive";
        }
        return returnType;
    }

    /**
     * 生成 上传路径
     *
     * @param which     {@link UploadManager}
     * @param companyId 如果为im，可以不传
     * @param tingsId   私有的情况下使用
     * @return
     */
    public static String getUploadPath(int which, Long companyId, Long tingsId) {
        String path = "";
        String dateStr = DateFormat.format("yyyy-MM-dd", new Date()).toString();
        Resources resources = Appli.getContext().getResources();
        switch (which) {
            case PU_IM:
                path = resources.getString(R.string.pu_im, dateStr);
                break;
            case PU_IM_BACKUP:
                path = resources.getString(R.string.pu_im_backup);
                break;
            case PU_IM_TEAM_ICON://废弃了
                path = resources.getString(R.string.pu_im_team_icon, companyId, tingsId);
                break;
            case PU_COMPANY_ICON:
                path = resources.getString(R.string.pu_company_icon, companyId, tingsId);
                break;
            case PU_PHOTO:
                path = resources.getString(R.string.pu_photo, dateStr);
                break;
        }
        return path;
    }

    public void destory() {
        if (taskDaoUtils != null) {
            taskDaoUtils.destory();
        }
        taskDaoUtils = null;
        whenStop = true;

        if (netProbeObservable != null) {
            RxBus.get().unregister(NET_IDLE_OBSERVABLE, netProbeObservable);
        }
        netProbeObservable = null;

        if (intervalDetailDisposable != null && !intervalDetailDisposable.isDisposed()) {
            intervalDetailDisposable.dispose();
            intervalDetailDisposable = null;
        }
        if (intervalDetailErrorDisposable != null && !intervalDetailErrorDisposable.isDisposed()) {
            intervalDetailErrorDisposable.dispose();
            intervalDetailErrorDisposable = null;
        }
    }
}
