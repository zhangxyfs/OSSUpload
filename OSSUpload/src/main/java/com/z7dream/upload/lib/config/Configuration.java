package com.z7dream.upload.lib.config;

/**
 * Created by Z7Dream on 2017/7/17 15:10.
 * Email:zhangxyfs@126.com
 */

public interface Configuration {
    int HTTP_CONNECTION_TIMEOUT = 60 * 1000;// 连接超时时间
    int HTTP_SOCKET_TIMEOUT = 60 * 1000;//socket超时
    int MAX_UPLOAD_NUM = 1;//最大并发请求书
    int MAX_RETRY_NUM = 1;//失败后最大重试次数
    int MAX_UPLOAD_RETRY_NUM = 5;//上传失败循环尝试次数
    int INTERVAL_TIME = 1000;//上传队列循环间隔
    int MAX_DELAY_TIME = 3600 - 500;
}
