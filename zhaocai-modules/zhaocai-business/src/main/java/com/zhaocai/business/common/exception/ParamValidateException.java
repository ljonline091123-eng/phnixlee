package com.zhaocai.business.common.exception;


/**
 * 参数校验异常
 *
 * @author chenming
 * @date 2024/03/29
 */
public class ParamValidateException extends ApplicationException{
	public ParamValidateException(String message) {
		super(ResultCode.PARAM_VALID_ERROR,message);
	}
}
