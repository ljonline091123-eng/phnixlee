package com.zhaocai.business.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 供应商操作日志类型枚举
 *
 * @author chenming
 * @date 2024-06-25
 */
@Getter
@AllArgsConstructor
public enum VendorOperateLogCodeEnum {

    ADD_TO_BLACK("add_to_black","移入黑名单"),
    REMOVE_FROM_BLACK("remove_from_black","移除黑名单"),
    UPDATE_VENDOR_LEVEL("update_vendor_level","修改供应商等级"),

    ENABLE_VENDOR_CONTACT("enable_vendor_contact","启用供应商联系人"),
    DISABLE_VENDOR_CONTACT("enable_vendor_contact","禁用供应商联系人"),

    ;

    private final String businessCode;

    private final String businessDesc;
}
