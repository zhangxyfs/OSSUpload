package com.z7dream.upload.lib.net;


import android.text.TextUtils;

import com.z7dream.upload.lib.tool.exception.ApiException;
import com.z7dream.upload.lib.tool.rxjava.BaseEntity;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.ObservableTransformer;
import io.reactivex.functions.Function;

/**
 * User: Axl_Jacobs(Axl.Jacobs@gmail.com)
 * Date: 2016-09-01
 * Time: 20:27
 * FIXME
 * Rx处理服务器返回
 */
class RxResultHelper {
    static <T> ObservableTransformer<BaseEntity<T>, T> handleResult() {
        return tObservable -> tObservable.flatMap(new Function<BaseEntity<T>, ObservableSource<T>>() {
            @Override
            public ObservableSource<T> apply(BaseEntity<T> entity) throws Exception {
                String code = "5000", msg = "未知错误";

                if (entity != null && entity.isOk()) {
                    //防止某些接口返回data为null
                    if (entity.data == null) {
                        entity.data = (T) Boolean.valueOf(true);
                    }

                    return createData(entity.data);
                } else {
                    if (entity != null) {
                        code = entity.code;
                        //添加错误消息判定
                        if (!TextUtils.isEmpty(entity.message)) {
                            msg = entity.message;
                        } else {
                            if (!TextUtils.isEmpty(entity.result))
                                msg = entity.result;
                        }
                    }
                }
                return Observable.error(new ApiException(code, msg));
            }
        });
    }

    private static <T> Observable<T> createData(T t) {
        return Observable.create(emitter -> {
            try {
                emitter.onNext(t);
                emitter.onComplete();
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        });
    }
}