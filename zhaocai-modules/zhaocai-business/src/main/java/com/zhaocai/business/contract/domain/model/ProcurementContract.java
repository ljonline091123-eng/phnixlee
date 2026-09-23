package com.zhaocai.business.contract.domain.model;

import com.baomidou.mybatisplus.annotation.TableName;
import com.zhaocai.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Date;

/** 采购合同聚合根，承载合同业务状态，不混入电子签章平台字段。 */
@Getter
@Setter
@ApiModel("采购合同")
@TableName("tb_procurement_contract")
public class ProcurementContract extends BaseEntity {
    public static final int DRAFT = 0;
    public static final int PENDING_SIGN = 1;
    public static final int SIGNED = 2;
    public static final int EXECUTING = 3;
    public static final int COMPLETED = 4;
    public static final int CANCELLED = 5;

    @ApiModelProperty("定标决策ID")
    private Long awardDecisionId;
    @ApiModelProperty("旧合同ID，用于兼容迁移和回调关联")
    private Long legacyAgreementId;
    @ApiModelProperty("采购方案ID")
    private Long schemeId;
    @ApiModelProperty("供应商ID")
    private Long vendorId;
    @ApiModelProperty("合同编号")
    private String contractNo;
    @ApiModelProperty("合同名称")
    private String contractName;
    @ApiModelProperty("合同含税金额")
    private BigDecimal totalAmount;
    @ApiModelProperty("合同签署日期")
    private Date signedAt;
    @ApiModelProperty("合同生效日期")
    private Date effectiveAt;
    @ApiModelProperty("合同状态")
    private Integer status;

    public void submitForSign() {
        if (!Integer.valueOf(DRAFT).equals(status)) {
            throw new IllegalStateException("只有草稿合同可以发起签署");
        }
        status = PENDING_SIGN;
    }

    public void markSigned() {
        if (!Integer.valueOf(PENDING_SIGN).equals(status)) {
            throw new IllegalStateException("只有待签署合同可以标记签署完成");
        }
        status = SIGNED;
    }

    public void startExecution() {
        if (!Integer.valueOf(SIGNED).equals(status)) {
            throw new IllegalStateException("只有已签署合同可以进入履约");
        }
        status = EXECUTING;
    }
}
