package com.zhaocai.business.common.utils;

import com.zhaocai.business.common.exception.NotFoundException;
import com.zhaocai.business.common.exception.ParamValidateException;
import org.apache.commons.lang3.StringUtils;

import java.util.function.Predicate;

/**
 * 系统校验工具类
 *
 * @author chenming
 * @date 2024/05/28
 */
public class ValidateUtils {

	/**
	 * 为空抛出异常
	 * @param t					校验参数
	 * @param errorMessage		异常信息
	 */
	public static <T> void isNullException(T t, String errorMessage) {
		if (t == null) {
			throw new NotFoundException(errorMessage);
		}
	}

	/**
	 * 不为空抛出异常
	 * @param t					校验参数
	 * @param errorMessage		异常信息
	 */
	public static <T> void isNotNullException(T t, String errorMessage) {
		if (t != null) {
			throw new NotFoundException(errorMessage);
		}
	}


	/**
	 * 字符串为空抛出异常
	 * @param str			待校验的字符串
	 * @param errorMessage	抛出的异常信息
	 */
	public static void isBlankException(String str, String errorMessage) {
		if (StringUtils.isBlank(str)) {
			throw new ParamValidateException(errorMessage);
		}
	}

	/**
	 * 判断数字是否为 null 或者 0
	 * @param value
	 * @param errorMessage
	 */
	public static void isNullOrZeroException(Number value, String errorMessage) {
		if (value == null || value.intValue() == 0) {
			throw new ParamValidateException(errorMessage);
		}
	}


	/**
	 * 校验状态是否一致，一致抛出异常
	 * @param validator			校验器
	 * @param status			值
	 * @param errorMessage		异常信息
	 */
	public static <T> void validateStatusEquals(Predicate<T> validator,T status, String errorMessage) {
		if (validator.test(status)) {
			throw new ParamValidateException(errorMessage);
		}
	}


	/**
	 * 校验状态是否一致，不一致抛出异常
	 * @param validator			校验器
	 * @param status			值
	 * @param errorMessage		异常信息
	 */
	public static <T> void validateStatusNotEquals(Predicate<T> validator,T status, String errorMessage) {
		if (!validator.test(status)) {
			throw new ParamValidateException(errorMessage);
		}
	}
}
