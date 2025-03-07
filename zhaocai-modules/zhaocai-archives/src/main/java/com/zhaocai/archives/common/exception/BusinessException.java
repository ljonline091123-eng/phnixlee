package com.zhaocai.archives.common.exception;

import com.zhaocai.common.core.web.bean.IResultCode;


/**
 * 业务类异常
 *
 * @author chenming
 * @date 2024/03/29
 */
public class BusinessException extends ApplicationException {

    public BusinessException(String message) {
        super(message);
    }

    public BusinessException(IResultCode resultCode, String message) {
        super(resultCode, message);
    }

    public BusinessException(IResultCode resultCode, Throwable cause) {
        super(resultCode, cause);
    }

    public BusinessException(IResultCode resultCode, String message, Throwable cause) {
        super(resultCode, message, cause);
    }
}
