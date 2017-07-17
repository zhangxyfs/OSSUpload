package com.z7dream.upload.lib.net;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.z7dream.upload.lib.config.OSSInfo;
import com.z7dream.upload.lib.tool.rxjava.RxSchedulersHelper;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import io.reactivex.Observable;


/**
 *  * Created by xiaoyu.zhang on 2016/11/10 17:54
 *  
 */
public class ApiClient {
    /**
     * 获取OSS信息
     *
     * @return
     */
    public static Observable<OSSInfo> getOSSInfo(String userId, String companyId) {
        return OKHTTP.getInstance().getRequestManager().getOSSInfo(userId, companyId).compose(RxSchedulersHelper.io_main()).compose(RxResultHelper.handleResult());
    }

    /**
     * 获取OSS信息
     *
     * @return
     */
    public static Observable<OSSInfo> getOSSPublicInfo(String userId) {
        return OKHTTP.getInstance().getRequestManager().getOSSPublicInfo(userId).compose(RxSchedulersHelper.io_main()).compose(RxResultHelper.handleResult());
    }

    /**
     * 获取oss 信息
     *
     * @param userId
     * @param companyId（可以为null）
     * @return
     */
    public static Observable<OSSInfo> getOSSProviderInfo(Long userId, Long companyId) {
        if (companyId == null || companyId <= 0) {
            return getOSSPublicInfo(String.valueOf(userId));
        }
        return getOSSInfo(String.valueOf(userId), String.valueOf(companyId));
    }
    //---------------------------------------------------------------------------------------------------------------------------------------------------------------------


    public static String createParams(Map<String, String> map) {
        String params = "?";
        if (map == null) {
            return "";
        }
        for (String key : map.keySet()) {
            params += key + "=" + map.get(key) + "&";
        }
        return params.substring(0, params.length() - 1);
    }

    public static Map<String, String> getParam(String url) {
        Map<String, String> map = new HashMap<>();
        if (TextUtils.isEmpty(url)) {
            return map;
        }
//        if (url.startsWith("http://")) {
        String strs[] = url.split("\\?");
        if (strs.length > 1) {
            String params[] = strs[1].split("\\&");
            for (String param : params) {
                String ky[] = param.split("=");
                if (ky.length > 1) {
                    map.put(ky[0], ky[1]);
                }
            }
        }
//        }

        return map;
    }

    /**
     * 生成json
     *
     * @param map
     * @return
     */
    private static String getParamJSON(@NonNull Map<String, String> map) {
        String paramValue = "";
        JSONObject jsonObject = null;
        Set<String> set = map.keySet();
        try {
            jsonObject = new JSONObject();
            for (String key : set) {
                String value = map.get(key);
                jsonObject.put(key, value);
            }
            paramValue = jsonObject.toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return paramValue;
    }
}
