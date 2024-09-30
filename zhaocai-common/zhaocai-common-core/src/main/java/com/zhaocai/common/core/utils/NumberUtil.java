package com.zhaocai.common.core.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;

/**
 * 数字处理工具类
 *
 * @author chenming
 * @date 2024/05/28
 */
public class NumberUtil {


	/**
	 * 判断是否为 null OR 0
	 * @param value
	 * @return
	 */
	public static boolean isNullOrZero(Long value)  {
		return value == null || value == 0L;
	}

	/**
	 * 判断是否不为 null OR 0
	 * @param value
	 * @return
	 */
	public static boolean isNotNullAndZero(Long value)  {
		return value != null && value != 0L;
	}

	/**
	 * null 转换为 0
	 * @param value
	 * @return
	 */
	public static BigDecimal nullToZero(BigDecimal  value) {
		return value == null ? BigDecimal.ZERO : value;
	}

	/**
	 * 转换为 BigDecimal
	 * @param value
	 * @return
	 */
	public static BigDecimal toBigDecimal(Long value) {
		return value == null ? BigDecimal.ZERO : BigDecimal.valueOf(value);
	}

	/**
	 * 保留小数
	 * @param value		数据
	 * @param scale		保留位数
	 * @return
	 */
	public static BigDecimal round(BigDecimal value,int scale ) {
		value = value == null ? BigDecimal.ZERO : value;
		if (scale < 0) {
			scale = 0;
		}

		return value.setScale(scale,RoundingMode.HALF_UP);
	}

	/**
	 * 乘法，保留小数
	 * @param value1	乘数
	 * @param value2	被乘数
	 * @return
	 */
	public static BigDecimal multiply(BigDecimal value1,Long value2) {
		value1 = value1 == null ? BigDecimal.ZERO : value1;
		BigDecimal value21 = value2 == null ? BigDecimal.ZERO : toBigDecimal(value2);

		return value1.multiply(value21);

	}

	/**
	 * 乘法，不做保留小数处理
	 * @param value1	乘数
	 * @param value2	被乘数
	 * @return
	 */
	public static BigDecimal multiply(BigDecimal value1,BigDecimal value2) {
		value1 = value1 == null ? BigDecimal.ZERO : value1;
		value2 = value2 == null ? BigDecimal.ZERO : value2;

		return value1.multiply(value2);
	}

	/**
	 * 乘法，保留小数
	 * @param value1	乘数
	 * @param value2	被乘数
	 * @param scale		保留小数位
	 * @return
	 */
	public static BigDecimal multiply(BigDecimal value1, BigDecimal value2, int scale) {
		value1 = value1 == null ? BigDecimal.ZERO : value1;
		value2 = value2 == null ? BigDecimal.ZERO : value2;

		BigDecimal result = value1.multiply(value2);
		return round(result,scale);
	}

	/**
	 * 加法，保留小数
	 * @param value1	乘数
	 * @param value2	被乘数
	 * @return
	 */
	public static BigDecimal add(BigDecimal value1,BigDecimal value2) {
		value1 = value1 == null ? BigDecimal.ZERO : value1;
		value2 = value2 == null ? BigDecimal.ZERO : value2;

		return value1.add(value2);
	}

	/**
	 * 加法，保留小鼠
	 * @param scale
	 * @param values
	 * @return
	 */
	public static BigDecimal add(int scale,BigDecimal... values) {
		BigDecimal result = add(values);

		return round(result,scale);
	}

	/**
	 * 加法
	 * @param values
	 * @return
	 */
	public static BigDecimal add(BigDecimal... values) {
		BigDecimal result = BigDecimal.ZERO;
		for (BigDecimal value : values) {
			value = value == null ? BigDecimal.ZERO : value;
			result = result.add(value);
		}

		return result;
	}

	/**
	 * 减法
	 * @param value1
	 * @param value2
	 * @return
	 */
	public static BigDecimal subtract(BigDecimal value1,BigDecimal value2) {
		value1 = value1 == null ? BigDecimal.ZERO : value1;
		value2 = value2 == null ? BigDecimal.ZERO : value2;
		return value1.subtract(value2);
	}

	/**
	 * 减法
	 * @param value1
	 * @param value2
	 * @return
	 */
	public static BigDecimal subtract(BigDecimal value1,BigDecimal value2,int scale) {
		BigDecimal result = subtract(value1,value2);
		return round(result,scale);
	}

	/**
	 * 比较两个数值大小
	 * @param v1
	 * @param v2
	 * @return
	 */
	public static int compare(Long v1, Long v2) {
		v1 = v1 == null ? 0L : v1;
		v2 = v2 == null ? 0L : v2;
		return v1.compareTo(v2);
	}

	public static long numberSub(Long v1,Long v2) {
		long  v11 = v1 == null ? 0L : v1;
		long  v21 = v2 == null ? 0L : v2;
		return v11 - v21;
	}

	/**
	 * 触发，保留 scale 位小数
	 * @param value1
	 * @param value2
	 * @param scale
	 * @return
	 */
	public static BigDecimal divide(BigDecimal value1, BigDecimal value2, int scale) {
		value1 = value1 == null ? BigDecimal.ZERO : value1;
		if (value2 == null || value2.equals(BigDecimal.ZERO)) {
			throw new RuntimeException("被除数不能为 null 或者 0 ");
		}
		return divide(value1, value2, scale, RoundingMode.HALF_UP);
	}

	public static BigDecimal divide(BigDecimal value1, BigDecimal value2, int scale, RoundingMode mode) {
		return cn.hutool.core.util.NumberUtil.div(value1, value2, scale, mode);
	}

	/**
	 * 除法，不考虑保留位数
	 * @param value1
	 * @param value2
	 * @return
	 */
	public static BigDecimal divide(BigDecimal value1, BigDecimal value2) {
		value1 = value1 == null ? BigDecimal.ZERO : value1;
		if (value2 == null || value2.equals(BigDecimal.ZERO)) {
			throw new RuntimeException("被除数不能为 null 或者 0 ");
		}

		return value1.divide(value2);
	}

	/**
	 * 格式化 BigDecimal，不补 0
	 * @param value
	 * @param scale
	 * @return
	 */
	public static String decimalFormat(BigDecimal value, int scale) {
		String result = null;
		if (value != null) {
			DecimalFormat format = new DecimalFormat();
			int valueScale = value.stripTrailingZeros().scale();
			/*
			 * 补零规则：
			 * valueScale < 2   ---> 补零 2位
			 * valueScale > 2   ---> 不补零
			 */
			if (valueScale <= 2) {
				format.setMinimumFractionDigits(2);
				format.setMaximumFractionDigits(2);
			} else {
				format.setMinimumFractionDigits(0);
				format.setMaximumFractionDigits(scale);
			}

			format.setGroupingUsed(true);
			format.setGroupingSize(3);

			result = format.format(value);
		}

		return result;
	}

	/**
	 * 比较两个值的大小
	 * @param value1
	 * @param value2
	 * @return
	 */
	public static int compare(BigDecimal value1, BigDecimal value2) {
		value1 = value1 == null ? BigDecimal.ZERO : value1;
		value2 = value2 == null ? BigDecimal.ZERO : value2;
		return value1.compareTo(value2);
	}
}
