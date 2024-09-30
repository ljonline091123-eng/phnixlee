package com.zhaocai.business.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 业务编号
 *
 * @author chenming
 * @date 2024/05/27
 */
@Getter
@AllArgsConstructor
public enum BusinessCodeEnum {

    PROCUREMENT_PLAN("CGJH","采购计划",9),
    PROCUREMENT_SCHEME("CGRW","采购方案",9),
    AGREEMENT("CGHT","采购合同",6),

    ;

    private final String businessCode;

    private final String businessName;

    private final Integer codeLength;
}
