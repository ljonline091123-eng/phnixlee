package com.zhaocai.business.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 *
 *  @Description: 消息业务类型枚举
 */
@Getter
@AllArgsConstructor
public enum MsgBusinessTypeEnum {

    PURCHASE_MATERIALS(1, "购买材料"),
    LEASED_MATERIAL(2, "租赁材料"),
    RENTAL_MACHINERY(3, "租赁机械（设备）"),
    SPECIALTY_SUBCONTRACT(4, "专业分包"),
    SERVICE_SUBCONTRACT(5, "劳务分包"),
    OTHER_TYPE(6, "其他");

    private final Integer state;

    private final String desc;
}
