package com.z7dream.upload.lib.tool.rxjava;

import android.text.TextUtils;

import com.z7dream.upload.lib.tool.exception.ApiException;

import java.net.ConnectException;
import java.net.SocketTimeoutException;

import io.reactivex.functions.Consumer;

/**
 * Created by Z7Dream on 2017/3/3 10:06.
 * Email:zhangxyfs@126.com
 */

public abstract class RxSubscribeError implements Consumer<Throwable> {
    @Override
    public void accept(Throwable except) throws Exception {
        if (except != null) {
            String msg = TextUtils.isEmpty(except.getMessage()) ? "" : except.getMessage();
            if (msg.contains("timeout") || except instanceof SocketTimeoutException) {
                except = new ApiException("5000", "网络不给力\n请重新尝试");
            } else if (msg.contains("failed to connect") || except instanceof ConnectException) {
                except = new ApiException("5000", "无法连接服务器\n请检查您的网络");
            } else {
                otherError(except);
            }
        }
        error(except);
    }

    public abstract void error(Throwable t);

    public void otherError(Throwable except) {

    }
}
