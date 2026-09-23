/*
 *      Copyright (c) 2018-2028, Chill Zhuang All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are met:
 *
 *  Redistributions of source code must retain the above copyright notice,
 *  this list of conditions and the following disclaimer.
 *  Redistributions in binary form must reproduce the above copyright
 *  notice, this list of conditions and the following disclaimer in the
 *  documentation and/or other materials provided with the distribution.
 *  Neither the name of the dreamlu.net developer nor the names of its
 *  contributors may be used to endorse or promote products derived from
 *  this software without specific prior written permission.
 *  Author: Chill 庄骞 (smallchill@163.com)
 */
package com.zhaocai.common.core.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 用户类型枚举
 *
 * @author Chill
 */
@Getter
@AllArgsConstructor
public enum UserTypeEnum {

	/**
	 * web
	 */
	WEB("web", 1),

	/**
	 * app
	 */
	APP("app", 2),

	/**
	 * other
	 */
	OTHER("other", 3),

	/**
	 * purchase 采购端
	 */
	PURCHASE("purchase", 4),

	/**
	 * supplier 供应商
	 */
	VENDOR("vendor", 5),

	/**
	 * expert 专家
	 */
	EXPERT("expert", 6);





	final String name;
	final int category;


	public static UserTypeEnum getUserEnumByCategory(int category){
		UserTypeEnum[] values = UserTypeEnum.values();
		UserTypeEnum userEnum = null;
		for (int i = 0; i < values.length; i++) {
			if(values[i].getCategory()== category){
				userEnum = values[i];
				break;
			}
		}
		return userEnum;
	}

}
