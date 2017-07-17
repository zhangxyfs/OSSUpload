package com.z7dream.upload.lib.tool.rxjava;


import com.z7dream.upload.lib.tool.exception.ApiException;

import io.reactivex.functions.Consumer;

/**
 * Created by Z7Dream on 2017/4/14 18:54.
 * Email:zhangxyfs@126.com
 */

public abstract class RxAPIError implements Consumer<ApiException> {
    @Override
    public void accept(ApiException except) throws Exception {
        if (except != null) {
            error(except.getCode(), except);
        }
    }

    public abstract void error(String code, Throwable t);
}
