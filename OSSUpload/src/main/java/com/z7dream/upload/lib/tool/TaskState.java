package com.z7dream.upload.lib.tool;

/**
 * Created by Z7Dream on 2017/6/22 13:19.
 * Email:zhangxyfs@126.com
 */

public class TaskState {
    public static final int NONE = 0;         //无状态  --> 等待

    public static final int WAITING = 1;      //等待    --> 下载，暂停

    public static final int RUNNING = 2;      //进行中  --> 暂停，完成，错误

    public static final int PAUSE = 3;        //暂停    --> 等待，进行

    public static final int FINISH = 4;       //完成    --> 重新下载

    public static final int ERROR = 5;        //错误    --> 等待

    public static final int STATUS_DOWNLOAD = 0;

    public static final int STATUS_UPLOAD = 1;
}
