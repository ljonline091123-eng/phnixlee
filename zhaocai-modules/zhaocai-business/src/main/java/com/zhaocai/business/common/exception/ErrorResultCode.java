package com.zhaocai.business.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 系统异常枚举
 *
 * @author chenming
 * @date 2024/04/01
 */
@Getter
@AllArgsConstructor
public enum ErrorResultCode implements IResultCode {
	STATUS_ERROR(400100,"状态不一致");

	private final Integer code;

	private final String message;

	@Override
	public String getMessage() {
		return null;
	}

	@Override
	public int getCode() {
		return 0;
	}
}
