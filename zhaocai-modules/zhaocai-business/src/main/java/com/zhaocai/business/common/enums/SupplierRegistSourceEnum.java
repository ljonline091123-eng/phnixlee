package com.zhaocai.business.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 供应商注册来源枚举
 *
 * @author zhangxu
 * @date 2024/09/23
 */

@Getter
@AllArgsConstructor
public enum SupplierRegistSourceEnum {
    /**
     * 供应商注册来源枚举
     */
    PLATFORM_REGIST("platform_regist", "平台注册"),
    EMM_PUSH("emm_push", "易料市集推送"),
    ;

    private final String supplierSourse;

    private final String desc;

    public Boolean equalsSupplierSourse(String supplierSourse) {
        return this.getSupplierSourse().equals(supplierSourse);
    }
}
