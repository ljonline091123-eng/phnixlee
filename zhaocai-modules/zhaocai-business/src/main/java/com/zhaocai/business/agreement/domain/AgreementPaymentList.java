package com.zhaocai.business.agreement.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.zhaocai.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 合同结算与付款节点信息对象 tb_agreement_payment_list
 *
 * @author chenming
 * @date 2024-05-24
 */
@Getter
@Setter
@TableName(value = "tb_agreement_payment_list")
public class AgreementPaymentList extends BaseEntity {
    private static final long serialVersionUID = 1L;


    /**
     * 合同id
     */
    @ApiModelProperty(value = "合同id")
    private Long agreementId;

    /**
     * 结算阶段  枚举值：{@link com.zhaocai.business.agreement.enums.SettlementStageStatusEnum}
     */
    @ApiModelProperty(value = "结算阶段")
    private String settlementStage;

    /**
     * 付款阶段名称
     */
    @ApiModelProperty(value = "付款阶段名称")
    private String paymentName;

    /**
     * 付款基数
     */
    @ApiModelProperty(value = "付款基数")
    private String paymentBasis;

    /**
     * 约定付款比例(%)
     */
    @ApiModelProperty(value = "约定付款比例(%)")
    private BigDecimal paymentRatio;

    /**
     * 约定付款金额
     */
    @ApiModelProperty(value = "约定付款金额")
    private BigDecimal paymentAmount;

    /**
     * 付款说明
     */
    @ApiModelProperty(value = "付款说明")
    private String paymentRemark;

    /**
     * 当前付款节点
     */
    @ApiModelProperty(value = "当前付款节点")
    private Integer currentPaymentPoint;
}
