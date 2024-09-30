package com.zhaocai.common.signature.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 签署状态枚举
 *
 * @author chenming
 * @date 2024-08-28
 */
@Getter
@AllArgsConstructor
public enum SignStateEnum {

    CREATED(0,"创建"),
    SIGN_SUCCESS(1,"签署成功"),
    ;

    private final Integer state;

    private final String desc;
}
