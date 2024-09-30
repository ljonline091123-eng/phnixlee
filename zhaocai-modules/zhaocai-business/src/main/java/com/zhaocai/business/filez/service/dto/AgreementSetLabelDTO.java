package com.zhaocai.business.filez.service.dto;

import lombok.Data;

/**
 * 合同标签集
 *
 * @author chenming
 * @date 2024-07-29
 */
@Data
public class AgreementSetLabelDTO {

    /**
     * 合同文件
     */

    private String agreementFileUrl;

    /**
     * 合同名称
     */
    private String agreementName;

    /**
     * 合同编号
     */
    private String agreementCode;

    /**
     * 甲方名称
     */
    private String partyAName;

    /**
     * 乙方名称
     */
    private String partyBName;

    /**
     * 项目名称
     */
    private String objectName;

    /**
     * 总金额-含税
     */
    private String totalAmountInclTax;

    /**
     * 总金额大写-含税
     */
    private String totalAmountChineseInclTax;

    /**
     * 总金额-不含税
     */
    private String totalAmountExclTax;

    /**
     * 总金额大写-不含税
     */
    private String totalAmountChineseExclTax;

    /**
     * 清单
     */
    private String materialsListFileUrl;

    /**
     * 甲方联系人名称
     */
    private String partyAContactName;

    /**
     * 甲方联系人电话
     */
    private String partyAContactPhone;

    /**
     * 甲方联系人身份证
     */
    private String partyAContactIdCard;

    /**
     * 乙方联系人名称
     */
    private String partyBContactName;

    /**
     * 乙方联系人电话
     */
    private String partyBContactPhone;

    /**
     * 乙方联系人身份证
     */
    private String partyBContactIdCard;

    /**
     * 乙方发票-抬头
     */
    private String partyBInvoiceTile;

    /**
     * 乙方发票-纳税人识别号
     */
    private String partyBInvoiceIdentificationNumber;

    /**
     * 乙方发票-地址
     */
    private String partyBInvoiceAddress;

    /**
     * 乙方发票-电话
     */
    private String partyBInvoicePhone;

    /**
     * 乙方发票-开户行
     */
    private String partyBInvoiceOpenBank;

    /**
     * 乙方发票-账号
     */
    private String partyBInvoiceAccount;

    /**
     * 乙方发票-开票金额
     */
    private String partyBInvoiceAmount;

}
