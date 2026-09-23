package com.zhaocai.business.agreement.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.zhaocai.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 合同款项信息对象 tb_agreement_payment_item
 *
 * @author chenming
 * @date 2024-05-24
 */
@Getter
@Setter
@TableName(value = "tb_agreement_payment_item")
public class AgreementPaymentItem extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /**
     * 合同id
     */
    @ApiModelProperty(value = "合同id")
    private Long agreementId;

    /**
     * 币种
     */
    @ApiModelProperty(value = "币种")
    private String currency;

    /**
     * 发票类型
     */
    @ApiModelProperty(value = "发票类型")
    private String invoiceType;

    /**
     * 合同税率
     */
    @ApiModelProperty(value = "合同税率")
    private BigDecimal contractTaxRate;

    /**
     * 约定预付款比例
     */
    @ApiModelProperty(value = "约定预付款比例")
    private BigDecimal prepaymentRatio;

    /**
     * 约定预付款金额
     */
    @ApiModelProperty(value = "约定预付款金额")
    private BigDecimal prepaymentAmount;

    /**
     * 预付款扣回条件
     */
    @ApiModelProperty(value = "预付款扣回条件")
    private String prepaymentDeductionConditions;

    /**
     * 预付款全部扣回截止点
     */
    @ApiModelProperty(value = "预付款全部扣回截止点")
    private BigDecimal prepaymentDeductionDeadline;

    /**
     * 允许合同外结算占合同比例
     */
    @ApiModelProperty(value = "允许合同外结算占合同比例")
    private BigDecimal outOfSettlementRatio;

    /**
     * 是否关联我的钢铁网价格
     */
    @ApiModelProperty(value = "是否关联我的钢铁网价格")
    private Integer isRelatedMySteel;

    /**
     * 我的钢铁网价格浮动值
     */
    @ApiModelProperty(value = "我的钢铁网价格浮动值")
    private BigDecimal mySteelPriceFluctuation;

    /**
     * 停滞台班结算比例
     */
    @ApiModelProperty(value = "停滞台班结算比例")
    private BigDecimal stagnationRatio;
}
