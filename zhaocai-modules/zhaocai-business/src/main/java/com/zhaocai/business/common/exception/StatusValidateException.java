package com.zhaocai.business.common.exception;

/**
 * 系统状态验证异常<br/>
 * 可用于各种状态的检验
 *
 * @author chenming
 * @date 2024/04/01
 */
public class StatusValidateException extends BusinessException{

	public StatusValidateException(String message) {
		super(ErrorResultCode.STATUS_ERROR,message);
	}
}
