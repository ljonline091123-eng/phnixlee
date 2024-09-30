package com.zhaocai.business.agreement.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.zhaocai.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModelProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 合同保证金对象 tb_agreement_deposit
 *
 * @author chenming
 * @date 2024-05-24
 */
@Getter
@Setter
@TableName(value = "tb_agreement_deposit")
@EqualsAndHashCode(callSuper = true)
public class AgreementDeposit extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /**
     * 合同 id
     */
    @ApiModelProperty(value = "合同 id")
    private Long agreementId;

    /**
     * 保证金类型
     */
    @ApiModelProperty(value = "保证金类型")
    private String depositType;

    /**
     * 保证金方式
     */
    @ApiModelProperty(value = "保证金方式")
    private String depositWay;

    /**
     * 保证金基数
     */
    @ApiModelProperty(value = "保证金基数")
    private String depositBaseAmount;

    /**
     * 缴纳金额
     */
    @ApiModelProperty(value = "缴纳金额")
    private BigDecimal paymentAmount;

    /**
     * 约定保证金比例
     */
    @ApiModelProperty(value = "约定保证金比例")
    private BigDecimal depositRatio;

    /**
     * 约定保证金金额
     */
    @ApiModelProperty(value = "约定保证金金额")
    private BigDecimal depositAmount;

    /**
     * 返还条件
     */
    @ApiModelProperty(value = "返还条件")
    private String returnCondition;

    /**
     * 备注
     */
    @ApiModelProperty(value = "备注")
    private String remark;
}
