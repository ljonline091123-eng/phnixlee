package com.zhaocai.business.agreement.vo.res;

import com.zhaocai.business.common.annotations.MoneyFormat;
import com.zhaocai.business.common.base.AdviceObject;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 合同保证金对象
 *
 * @author chenming
 * @date 2024-05-24
 */
@Data
public class AgreementDepositVO extends AdviceObject {

    @ApiModelProperty(value = "id")
    private Long id;

    @ApiModelProperty(value = "合同 id")
    private Long agreementId;

    @ApiModelProperty(hidden = true)
    private String depositType;

    @ApiModelProperty(hidden = true)
    private String depositWay;

    @ApiModelProperty(value =  "保证金基数")
    private String depositBaseAmount;

    @ApiModelProperty(value = "缴纳金额")
    private BigDecimal paymentAmount;

    @ApiModelProperty(value =  "约定保证金比例")
    private BigDecimal depositRatio;

    @ApiModelProperty(value =  "约定保证金金额")
    private BigDecimal depositAmount;

    @ApiModelProperty(value =  "返还条件")
    private String returnCondition;

    @ApiModelProperty(value = "备注")
    private String remark;

    @MoneyFormat(filedName = "depositRatio",scale = 2)
    @ApiModelProperty(value =  "约定保证金比例")
    private String depositRatioText;

    @MoneyFormat(filedName = "depositAmount")
    @ApiModelProperty(value =  "约定保证金金额")
    private String depositAmountText;

    @ApiModelProperty(value =  "保证金类型-文本")
    private String depositTypeText;

    @ApiModelProperty(value =  "保证金方式-文本")
    private String depositWayText;

    @ApiModelProperty(value =  "保证金基数")
    private String depositBaseAmountText;

    @MoneyFormat(filedName = "paymentAmount")
    @ApiModelProperty(value = "缴纳金额")
    private String paymentAmountText;
}
