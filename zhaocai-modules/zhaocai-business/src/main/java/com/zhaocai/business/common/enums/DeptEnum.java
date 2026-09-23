package com.zhaocai.business.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 部门枚举类
 *
 * @author cff
 */
@AllArgsConstructor
@Getter
public enum DeptEnum {

    /** 部门枚举类 */
    ONEDEPT_TYPE(1,"一级部门"),

    TWODEPT_TYPE(2,"二级部门"),
    ;

    private final Integer type;

    private final String desc;
}
