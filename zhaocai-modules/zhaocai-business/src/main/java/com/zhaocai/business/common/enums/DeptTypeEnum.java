package com.zhaocai.business.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 部门类型枚举类
 * 用于 RemoteSystemService.getDeptAndNextDept 的 type 参数
 *
 * @author cff
 */
@AllArgsConstructor
@Getter
public enum DeptTypeEnum {

    /** 部门类型枚举类 */
    ALL_DEPT_TYPE("1", "含公司、部门、项目部"),

    COMPANY_DEPT_TYPE("2", "含公司,不含部门、项目部"),

    NOT_BM_DEPT_TYPE("3", "含公司、项目部,不含部门"),

    NOT_X_DEPT_TYPE("4", "含公司、部门,不含项目部"),
    ;

    private final String type;

    private final String desc;
}
