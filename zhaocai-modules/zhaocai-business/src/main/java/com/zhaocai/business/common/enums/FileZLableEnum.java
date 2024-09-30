package com.zhaocai.business.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 *  FileZ 标签
 *
 * @author chenming
 * @date 2024-07-29
 */
@Getter
@AllArgsConstructor
public enum FileZLableEnum {

    AGREEMENT_CODE("合同编号"),
    PARTY_A_NAME("甲方名称"),
    PARTY_B_NAME("乙方名称"),
    OBJECT_NAME("项目名称"),
    TOTAL_AMOUNT_INCL_TAX("总金额含税"),
    TOTAL_AMOUNT_CHINESE_INCL_TAX("总金额大写含税"),
    TOTAL_AMOUNT_EXCL_TAX("总金额不含税"),
    TOTAL_AMOUNT_CHINESE_EXCL_TAX("总金额大写不含税"),
    MATERIALS_LIST("清单列表"),
    PARTY_A_CONTACT_NAME("甲方联系人名称"),
    PARTY_A_CONTACT_PHONE("甲方联系人电话"),
    PARTY_A_CONTACT_ID_CARD("甲方联系人身份证"),
    PARTY_B_CONTACT_NAME("乙方联系人名称"),
    PARTY_B_CONTACT_PHONE("乙方联系人电话"),
    PARTY_B_CONTACT_ID_CARD("乙方联系人身份证"),
    PARTY_B_INVOICE_TILE("乙方发票-抬头"),
    PARTY_B_INVOICE_IDENTIFICATION_NUMBER("乙方发票-纳税人识别号"),
    PARTY_B_INVOICE_ADDRESS("乙方发票-地址"),
    PARTY_B_INVOICE_PHONE("乙方发票-电话"),
    PARTY_B_INVOICE_OPEN_BANK("乙方发票-开户行"),
    PARTY_B_INVOICE_ACCOUNT("乙方发票-账号"),
    PARTY_B_INVOICE_AMOUNT("乙方发票-开票金额"),

    ;

    private final String label;
}
