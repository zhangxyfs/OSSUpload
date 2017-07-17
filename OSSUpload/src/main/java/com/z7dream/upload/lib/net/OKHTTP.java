package com.z7dream.upload.lib.net;

import android.text.TextUtils;

import com.google.gson.Gson;
import com.z7dream.upload.lib.tool.exception.ApiException;
import com.z7dream.upload.lib.tool.rxjava.BaseEntity;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.util.concurrent.TimeUnit;

import okhttp3.Authenticator;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.logging.HttpLoggingInterceptor;
import okio.Buffer;
import okio.BufferedSource;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * 网络请求基础类
 * Created by xiaoyu.zhang on 2016/11/7 16:07
 * Email:zhangxyfs@126.com
 */
public class OKHTTP {
    private static final String TAG = "OKHTTP";
    private static OKHTTP mInstance;
    private final OkHttpClient mClient;

    public static final int HTTP_CONNECTION_TIMEOUT = 15 * 1000;
    private RequestManager requestManager;

    public static OKHTTP getInstance() {
        if (mInstance == null) {
            synchronized (OKHTTP.class) {
                if (mInstance == null) {
                    mInstance = new OKHTTP();
                }
            }
        }
        return mInstance;
    }

    /**
     * 初始化
     */
    private OKHTTP() {
        //log 拦截器
        HttpLoggingInterceptor logInterceptor = new HttpLoggingInterceptor();
        logInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        //添加head 的拦截器
        Interceptor headInterceptor = chain -> {
            okhttp3.Request originalRequest = chain.request();
            Request.Builder builder = originalRequest.newBuilder();
            okhttp3.Request authorised = builder.build();
            return chain.proceed(authorised);
        };

        //http code 拦截器
        Interceptor codeInterceptor = chain -> {
            Response response = chain.proceed(chain.request());
            ResponseBody responseBody = response.body();
            Charset UTF8 = Charset.forName("UTF-8");

            String message = "";
            if (response.code() != 200) {
                if (response.code() == 500) {
                    message = "请求错误";
                } else if (response.code() == 404) {
                    message = "请求地址错误";
                }
                httpCodeInterceptor(responseBody, UTF8, response, message);
            }
            return response;
        };

        //身份验证拦截器 如果得到401 Not Authorized未授权的错误
        Authenticator authenticator = (route, response) -> {
            if (!response.isSuccessful())
                throw new ApiException(String.valueOf(response.code()), "请求错误");
            return null;
        };

        OkHttpClient client = new OkHttpClient();
        mClient = client.newBuilder().readTimeout(HTTP_CONNECTION_TIMEOUT, TimeUnit.MILLISECONDS)
                .connectTimeout(HTTP_CONNECTION_TIMEOUT, TimeUnit.MILLISECONDS)
                .writeTimeout(HTTP_CONNECTION_TIMEOUT, TimeUnit.MILLISECONDS)
                .retryOnConnectionFailure(true)
                .addInterceptor(logInterceptor)//设置应用拦截器，主要用于设置公共参数，头信息，日志拦截等
                .addInterceptor(codeInterceptor)
                .addNetworkInterceptor(headInterceptor)//设置网络拦截器，主要用于重试或重写
                .authenticator(authenticator).build();

        Retrofit retrofit = new Retrofit.Builder()
                .client(mClient)
                .baseUrl(NetConfig.SERVER_ADD + "/")
                .addConverterFactory(GsonConverterFactory.create())//json转换器
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())//RxJavaCallAdapterFactory
                .build();

        requestManager = retrofit.create(RequestManager.class);
    }

    RequestManager getRequestManager() {
        return requestManager;
    }

    RequestManager getRequestManager(boolean isNeedReset) {
        if (isNeedReset) {
            mInstance = new OKHTTP();
        }
        return requestManager;
    }

    RequestManager getRequestManager(String serverUrl, boolean isNeedGson) {
        if (isNeedGson) {
            return getRequestManager(serverUrl);
        } else {
            return new Retrofit.Builder()
                    .client(mClient)
                    .baseUrl(serverUrl + "/")
                    .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                    .build().create(RequestManager.class);
        }
    }

    RequestManager getRequestManager(String serverUrl) {
        return new Retrofit.Builder()
                .client(mClient)
                .baseUrl(serverUrl + "/")
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .build().create(RequestManager.class);
    }

    private void httpCodeInterceptor(ResponseBody responseBody, Charset UTF8, Response response, String msg) throws IOException {
        BufferedSource source = responseBody.source();
        source.request(Long.MAX_VALUE); // Buffer the entire body.
        Buffer buffer = source.buffer();

        Charset charset = UTF8;
        MediaType contentType = responseBody.contentType();
        if (contentType != null) {
            try {
                charset = contentType.charset(UTF8);
            } catch (UnsupportedCharsetException e) {
                throw new ApiException(String.valueOf(response.code()), response.message());
            }

            if (responseBody.contentLength() != 0) {
                BaseEntity result = new Gson().fromJson(buffer.clone().readString(charset), BaseEntity.class);
                if (result != null && !TextUtils.isEmpty(result.result)) {
                    msg = result.result;
                }
                throw new ApiException(String.valueOf(response.code()), msg);
            }
        }
        throw new ApiException(String.valueOf(response.code()), response.message());
    }

}
