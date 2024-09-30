package com.zhaocai.common.signature.common.exception;


import lombok.Getter;

/**
 * 电子签章 exception
 *
 * @author chenming
 * @date 2024-08-19
 */
@Getter
public class SignatureException extends RuntimeException{

    private String errorCode;

    private final String errorMessage;

    public SignatureException(String errorMessage) {
        super(errorMessage);

        this.errorMessage = errorMessage;
    }

    public SignatureException(String errorCode,String errorMessage) {
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    public SignatureException(String errorCode,String errorMessage, Throwable cause) {
        super(errorMessage, cause);

        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

}
