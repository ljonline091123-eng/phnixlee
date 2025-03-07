package com.zhaocai.archives.common.exception;

import com.zhaocai.common.core.web.bean.IResultCode;
import lombok.Getter;

/**
 * 应用程序异常
 *
 * @author chenming
 * @date 2024/03/29
 */
public class ApplicationException extends RuntimeException {

    @Getter
    private String message;

    @Getter
    private IResultCode resultCode;

    public ApplicationException(String message) {
        super(message);
        this.message = message;
    }

    public ApplicationException(IResultCode resultCode) {
        super(resultCode.getMessage());
        this.resultCode = resultCode;
    }

    public ApplicationException(IResultCode resultCode, String message) {
        super(String.format("%s:{%s}", resultCode.getMessage(), message));
        this.resultCode = resultCode;
        this.message = message;
    }

    public ApplicationException(IResultCode resultCode, Throwable cause) {
        super(resultCode.getMessage(), cause);
        this.resultCode = resultCode;
    }

    public ApplicationException(IResultCode resultCode, String message, Throwable cause) {
        super(String.format("%s:{%s}", resultCode.getMessage(), message), cause);
        this.resultCode = resultCode;
        this.message = message;
    }
}
