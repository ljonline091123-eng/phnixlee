package com.zhaocai.business.agreement.vo.res;

import com.zhaocai.business.common.annotations.DictCache;
import com.zhaocai.business.common.annotations.MoneyFormat;
import com.zhaocai.business.common.base.AdviceObject;
import com.zhaocai.business.common.enums.DictBizEnum;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
@Data
public class AgreementBookmarkVO extends AdviceObject {
    @ApiModelProperty(value = "合同名称")
    private String agreementName;

    @ApiModelProperty(value = "合同编号")
    private String agreementCode;

    @ApiModelProperty(value = "单位内部合同管理编码")
    private String innerAgreementCode;

    @ApiModelProperty(value = "甲方名称")
    private String partyAName;

    @ApiModelProperty(value = "乙方名称")
    private String partyBName;

    @ApiModelProperty(value = "支出业务分类")
    private Integer expenditureBusinessType;

    @ApiModelProperty(value = "交易标的物名称")
    private String subjectMatterName;

    @ApiModelProperty(value = "支付周期")
    private String paymentCycle;

    @ApiModelProperty(value = "支付方式")
    private String paymentWay;


    @ApiModelProperty(value = "乙方法人代表")
    private String partyBLegalName;

    @ApiModelProperty(value = "乙方法人代表身份证")
    private String partyBLegalIdCard;

    @ApiModelProperty(value = "乙方法人代表联系方式")
    private String partyBLegalPhone;

    @ApiModelProperty(value = "乙方现场实际履职负责人")
    private String partyBResponsibleName;

    @ApiModelProperty(value = "乙方现场实际履职负责人身份证")
    private String partyBResponsibleIdCard;

    @ApiModelProperty(value = "乙方现场实际履职负责人联系方式")
    private String partyBResponsiblePhone;

    @ApiModelProperty(value = "合同履行地")
    private String agreementPerformAddress;

    @ApiModelProperty(value = "国家地区代码(履行地)")
    private String agreementPerformCountry;

    @ApiModelProperty(value = "行政区划代码(履行地)")
    private String agreementPerformDistrict;

    @ApiModelProperty(value = "计租方式")
    private Integer rentalMethod;

    @ApiModelProperty(value = "支出业务分类")
    @DictCache(dictBizEnum = DictBizEnum.PROCUREMENT_PLAN_TYPE,filedName = "expenditureBusinessType")
    private String expenditureBusinessTypeText;

    @ApiModelProperty(value = "支付周期")
    private String paymentCycleText;

    @ApiModelProperty(value = "支付方式")
    private String paymentWayText;

    @DictCache(dictBizEnum = DictBizEnum.AGREEMENT_RENTAL_METHOD,filedName = "rentalMethod")
    @ApiModelProperty(value = "计租方式")
    private String rentalMethodText;


    //合同款项信息
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

    @ApiModelProperty(value =  "币种-文本")
    private String currencyText;

    @ApiModelProperty(value =  "发票类型-文本")
    private String invoiceTypeText;


}
