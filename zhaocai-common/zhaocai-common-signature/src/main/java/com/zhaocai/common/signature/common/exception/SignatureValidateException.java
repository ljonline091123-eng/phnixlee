package com.zhaocai.common.signature.common.exception;


/**
 * 电子签章参数校验 exception
 *
 * @author chenming
 * @date 2024-08-19
 */
public class SignatureValidateException extends RuntimeException{

    public SignatureValidateException(String errorMessage) {
        super(errorMessage);
    }
}
