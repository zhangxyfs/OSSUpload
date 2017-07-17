package com.z7dream.upload.lib.net;

/**
 * Created by Z7Dream on 2017/7/17 15:43.
 * Email:zhangxyfs@126.com
 */

public class NetConfig {
    public static final boolean isLocal = true;

    static final String SERVER_ADD;

    static {
        if (isLocal) {
            SERVER_ADD = "";
        }
    }

    static class OSS {
        private static final String oss = "osscenter";

        static final String GET_OSS_INFO = oss + "/getOSSToken/{userId}/{companyId}";

        static final String GET_PUBLIC_OSS_INFO = oss + "/getOSSPublicToken/{userId}";
    }
}
