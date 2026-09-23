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
public enum AccountEnum {

    /** 账户枚举类 */
    GYS_TYPE(1,"供应商账户"),

    PERSON_TYPE(2,"人员账户"),
;
    private final Integer type;

    private final String desc;
}
