package com.z7dream.upload.lib.net;


import com.z7dream.upload.lib.config.OSSInfo;
import com.z7dream.upload.lib.tool.rxjava.BaseEntity;

import io.reactivex.Observable;
import retrofit2.http.GET;
import retrofit2.http.Path;

/**
 *  * Created by xiaoyu.zhang on 2016/11/7 16:14
 *  
 */
interface RequestManager {
    /**
     * 获取oss信息
     *
     * @param userId
     * @param companyId
     * @return
     */
    @GET(NetConfig.OSS.GET_OSS_INFO)
    Observable<BaseEntity<OSSInfo>> getOSSInfo(@Path("userId") String userId, @Path("companyId") String companyId);

    /**
     * 获取oss信息
     *
     * @param userId
     * @return
     */
    @GET(NetConfig.OSS.GET_PUBLIC_OSS_INFO)
    Observable<BaseEntity<OSSInfo>> getOSSPublicInfo(@Path("userId") String userId);
}
