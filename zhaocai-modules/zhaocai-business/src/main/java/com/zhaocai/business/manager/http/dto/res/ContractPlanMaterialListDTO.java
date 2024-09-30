package com.zhaocai.business.manager.http.dto.res;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 合同计划材料清单dto
 *
 * @author chenming
 * @date 2024-07-10
 */
@Data
public class ContractPlanMaterialListDTO {

    /**
     * 工作内容
     */
    private String basicJob;

    /**
     * id
     */
    private String id;

    /**
     * 可抵扣进项税,=单价（不含税）*增值税税率
     */
    private String inputTax;

    /**
     * 计量单位，如m
     */
    private String measureUnit;

    /**
     * 计量规则，如按体积计算
     */
    private String metrologicalRules;

    /**
     * 金额（无税）,=单价（不含税）*工程量
     */
    private BigDecimal ntaxAmount;

    /**
     * 单价（不含税）
     */
    private BigDecimal ntaxPrice;

    /**
     * 商务策划书主表id
     */
    private String planMainId;

    /**
     * 工程量
     */
    private BigDecimal quantity;

    /**
     * 指导价参考（不含税），最高
     */
    private String referencePriceMax;

    /**
     * 指导价参考（不含税），最低
     */
    private String referencePriceMin;

    /**
     * 规格型号
     */
    private String specs;

    /**
     * 成本科目编码
     */
    private String subjectCode;

    /**
     * 成本子目唯一ID
     */
    private String subjectDtlUniqueId;

    /**
     * 成本子目编码
     */
    private String subjectDtlCode;

    /**
     * 成本子目ID
     */
    private String subjectDtlId;

    /**
     * 成本子目名称
     */
    private String subjectDtlName;

    /**
     * 成本科目档案ID
     */
    private String subjectId;

    /**
     * 成本科目名称
     */
    private String subjectName;

    /**
     * 剩余使用数量
     */
    private BigDecimal surplusQuantity;

    /**
     * 税额，单价（不含税）*税率
     */
    private BigDecimal tax;

    /**
     * 金额（含税）,=单价（含税）*工程量
     */
    private BigDecimal taxAmount;

    /**
     * 单价（含税）,=单价（不含税）*（1+增值税率）=单价（含税）
     */
    private BigDecimal taxPrice;

    /**
     * 增值税税率
     */
    private BigDecimal taxRate;

    /**
     * 转换数量
     */
    private BigDecimal transferQuantity;

    /**
     * 已使用数量
     */
    private BigDecimal usedQuantity;

    /**
     * 租赁方式(1-日 2-月租 3-工作量)
     */
    private String rentMode;

    /**
     * 租赁时间
     */
    private BigDecimal rentTime;

    /**
     * 租赁数量
     */
    private BigDecimal rentQuantity;
}
