package com.z7dream.upload.lib.tool.exception;

import java.io.IOException;

/**
 * User: Axl_Jacobs(Axl.Jacobs@gmail.com)
 * Date: 2016-05-06
 * Time: 15:37
 * FIXME
 */
public class ApiException extends IOException {
    String ResultMessage;
    String ResultCode;

    public ApiException(String resultCode, String resultMessage) {
        ResultMessage = resultMessage;
        ResultCode = resultCode;
    }

    @Override
    public String getMessage() {
        return ResultMessage;
    }

    public String getCode() {
        return ResultCode;
    }

}
