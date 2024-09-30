package com.zhaocai.business.agreement.vo.res;

import com.zhaocai.business.common.annotations.DictCache;
import com.zhaocai.business.common.annotations.MoneyFormat;
import com.zhaocai.business.common.base.AdviceObject;
import com.zhaocai.business.common.enums.DictBizEnum;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;


/**
 * 协议付款项目vo
 *
 * @author chenming
 * @date 2024-06-21
 */
@Data
public class AgreementPaymentItemVO extends AdviceObject {

    @ApiModelProperty(value = "主键 id")
    private Long id;

    @ApiModelProperty(value = "合同id")
    private Long agreementId;

    @ApiModelProperty(value =  "币种")
    private String currency;

    @ApiModelProperty(value =  "发票类型")
    private String invoiceType;

    @ApiModelProperty(value =  "合同税率")
    private BigDecimal contractTaxRate;

    @ApiModelProperty(value =  "约定预付款比例")
    private BigDecimal prepaymentRatio;

    @ApiModelProperty(value =  "约定预付款金额")
    private BigDecimal prepaymentAmount;

    @ApiModelProperty(value =  "预付款扣回条件")
    private String prepaymentDeductionConditions;

    @ApiModelProperty(value =  "预付款全部扣回截止点")
    private BigDecimal prepaymentDeductionDeadline;

    @ApiModelProperty(value =  "允许合同外结算占合同比例")
    private BigDecimal outOfSettlementRatio;

    @ApiModelProperty(value =  "是否关联我的钢铁网价格")
    private Integer isRelatedMySteel;

    @ApiModelProperty(value =  "我的钢铁网价格浮动值")
    private BigDecimal mySteelPriceFluctuation;

    @ApiModelProperty(value =  "停滞台班结算比例")
    private BigDecimal stagnationRatio;

    @ApiModelProperty(value = "合同签订金额(含税)")
    private BigDecimal totalAmountIncTax;

    @ApiModelProperty(value = "合同签订金额(不含税)")
    private BigDecimal totalAmountExcTax;

    @MoneyFormat(filedName = "contractTaxRate",scale = 2)
    @ApiModelProperty(value =  "合同税率")
    private String contractTaxRateText;

    @MoneyFormat(filedName = "prepaymentRatio",scale = 2)
    @ApiModelProperty(value =  "约定预付款比例")
    private String prepaymentRatioText;

    @MoneyFormat(filedName = "prepaymentAmount")
    @ApiModelProperty(value =  "约定预付款金额")
    private String prepaymentAmountText;

    @MoneyFormat(filedName = "mySteelPriceFluctuation")
    @ApiModelProperty(value =  "我的钢铁网价格浮动值")
    private String mySteelPriceFluctuationText;

    @MoneyFormat(filedName = "stagnationRatio",scale = 2)
    @ApiModelProperty(value =  "停滞台班结算比例")
    private String stagnationRatioText;

    @MoneyFormat(filedName = "totalAmountIncTax",scale = 2)
    @ApiModelProperty(value = "合同签订金额(含税)")
    private String totalAmountIncTaxText;

    @MoneyFormat(filedName = "totalAmountExcTax",scale = 2)
    @ApiModelProperty(value = "合同签订金额(不含税)")
    private String totalAmountExcTaxText;

    @MoneyFormat(filedName = "prepaymentDeductionDeadline",scale = 2)
    @ApiModelProperty(value =  "预付款全部扣回截止点")
    private String prepaymentDeductionDeadlineText;

    @MoneyFormat(filedName = "outOfSettlementRatio",scale = 2)
    @ApiModelProperty(value =  "允许合同外结算占合同比例")
    private String outOfSettlementRatioText;

    @DictCache(dictBizEnum = DictBizEnum.AGREEMENT_IS_RELATED_MY_STEEL,filedName = "isRelatedMySteel")
    @ApiModelProperty(value =  "是否关联我的钢铁网价格-文本")
    private String isRelatedMySteelText;

    @ApiModelProperty(value =  "币种-文本")
    private String currencyText;

    @ApiModelProperty(value =  "发票类型-文本")
    private String invoiceTypeText;
}
