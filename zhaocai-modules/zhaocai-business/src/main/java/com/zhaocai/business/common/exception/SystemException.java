package com.zhaocai.business.common.exception;

/**
 * 系统类异常
 *
 * @author chenming
 * @date 2024/03/29
 */
public class SystemException extends ApplicationException{
	public SystemException(IResultCode resultCode,String message) {
		super(resultCode,message);
	}

	public SystemException(IResultCode resultCode,Throwable cause) {
		super(resultCode,cause);
	}

	public SystemException(IResultCode resultCode,String message,Throwable cause) {
		super(resultCode,message,cause);
	}
}
