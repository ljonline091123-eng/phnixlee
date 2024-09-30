package com.zhaocai.business.common.exception;

/**
 * 未找到业务异常
 *
 * @author chenming
 * @date 2024/04/01
 */
public class NotFoundException extends ApplicationException{
	public NotFoundException(String message) {
		super(ResultCode.NOT_FOUND,message);
	}
}
