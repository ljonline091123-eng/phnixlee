package com.zhaocai.business.agreement.vo.res;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.zhaocai.business.common.annotations.DictCache;
import com.zhaocai.business.common.annotations.MoneyFormat;
import com.zhaocai.business.common.base.AdviceObject;
import com.zhaocai.business.common.enums.DictBizEnum;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 合同结算与付款节点信息对象
 *
 * @author chenming
 * @date 2024-05-24
 */
@Data
public class AgreementPaymentListVO extends AdviceObject {
    @ApiModelProperty(value = "合同id")
    private Long agreementId;

    @ApiModelProperty(value =  "付款阶段名称")
    private String paymentName;

    @ApiModelProperty(value =  "付款基数")
    private String paymentBasis;

    @ApiModelProperty(value =  "约定付款比例(%)")
    private BigDecimal paymentRatio;

    @ApiModelProperty(value =  "约定付款金额")
    private BigDecimal paymentAmount;

    @ApiModelProperty(value =  "付款说明")
    private String paymentRemark;

    @ApiModelProperty(hidden = true)
    private Integer currentPaymentPoint;

    @DictCache(dictBizEnum = DictBizEnum.AGREEMENT_CURRENT_PAYMENT_POINT,filedName = "currentPaymentPoint")
    @ApiModelProperty(value =  "当前付款节点-文本")
    private String currentPaymentPointText;

    @ApiModelProperty(value =  "付款基数")
    private String paymentBasisText;

    @MoneyFormat(filedName = "paymentRatio",scale = 2)
    @ApiModelProperty(value =  "约定付款比例(%)")
    private String paymentRatioText;

    @MoneyFormat(filedName = "paymentAmount")
    @ApiModelProperty(value =  "约定付款金额")
    private String paymentAmountText;
}
