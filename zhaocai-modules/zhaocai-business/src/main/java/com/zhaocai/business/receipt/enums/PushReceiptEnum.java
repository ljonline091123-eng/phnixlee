package com.zhaocai.business.receipt.enums;


import lombok.AllArgsConstructor;
import lombok.Getter;


/**
 * 底层逻辑平台 url
 *
 * @author cff
 * @date 2024-09-11
 */
@Getter
@AllArgsConstructor
public enum PushReceiptEnum {
    /**
     * 招采更新成控设备租赁台账供应商状态
     */
    LEASE_WRITE_BACK_SUPPLIER_STATUS("/rest/reconciliation/lease/writeBackSupplierStatus","招采更新成控设备租赁台账供应商状态"),
    /**
     * 招采更新成控周材租赁台账供应商状态
     */
    TURNLEDGER_WRITE_BACK_SUPPLIER_STATUS("/rest/reconciliation/turnLedger/writeBackSupplierStatus","招采更新成控周材租赁台账供应商状态"),
    /**
     * 招采更新成控材料对账单供应商状态
     */
    RECONCILIATION_WRITE_BACK_SUPPLIER_STATUS("/rest/reconciliation/writeBackSupplierStatus","招采更新成控材料对账单供应商状态")
    ;


    /**
     * url
     */
    private final String url;

    /**
     * 描述
     */
    private final String desc;
}
