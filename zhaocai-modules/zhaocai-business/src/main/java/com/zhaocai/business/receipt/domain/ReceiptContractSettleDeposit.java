package com.zhaocai.business.receipt.domain;

import java.math.BigDecimal;

import lombok.Data;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zhaocai.common.core.web.domain.BaseEntity;

/**
 * 结算单押金保证金对象 tb_receipt_contract_settle_deposit
 *
 * @author CFF
 * @date 2024-09-07
 */
@Data
@TableName(value = "tb_receipt_contract_settle_deposit")
@ApiModel(value = "ReceiptContractSettleDeposit对象", description = "结算单押金保证金对象")
public class ReceiptContractSettleDeposit extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 第三方id
     */
    @ApiModelProperty("第三方id")
    private String thirdId;

    /**
     * 押金、保证金基数（1-签订金额 2-结算金额 3-一次性缴纳）
     */
    @ApiModelProperty("押金、保证金基数（1-签订金额 2-结算金额 3-一次性缴纳）")
    private String baseType;

    /**
     * 本期约定押金/保证金金额
     */
    @ApiModelProperty("本期约定押金/保证金金额")
    private BigDecimal currentDepositAmount;

    /**
     * 约定押金/保证金金额
     */
    @ApiModelProperty("约定押金/保证金金额")
    private BigDecimal depositAmount;

    /**
     * 结算明细表关联id
     */
    @ApiModelProperty("结算明细表关联id")
    private String depositId;

    /**
     * 押金、保证金方式（1-现金 2-保函 3-担保）
     */
    @ApiModelProperty("押金、保证金方式（1-现金 2-保函 3-担保）")
    private String depositMode;

    /**
     * 约定押金/保证金比例(%)
     */
    @ApiModelProperty("约定押金/保证金比例(%)")
    private BigDecimal depositRatio;

    /**
     * 押金、保证金类型（1-履约保证金 2-质量保证金）
     */
    @ApiModelProperty("押金、保证金类型（1-履约保证金 2-质量保证金）")
    private String depositType;

    /**
     * 明细表唯一id
     */
    @ApiModelProperty("明细表唯一id")
    private String depositUniqueId;

    /**
     * 结算单ID,是contract_settlement的id
     */
    @ApiModelProperty("结算单ID,是contract_settlement的id")
    private String settleId;

    /**
     * 父级id
     */
    @ApiModelProperty("父级id")
    private String parntId;

    /**
     *
     */
    @ApiModelProperty("")
    private String createDept;

    /**
     * 创建人 id
     */
    @ApiModelProperty("创建人 id")
    private Long createId;

    /**
     * 修改人 id
     */
    @ApiModelProperty("修改人 id")
    private Long updateId;

    /**
     * 删除标志（0代表存在 2代表删除）
     */
    @ApiModelProperty("删除标志（0代表存在 2代表删除）")
    private String delFlag;

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
                .append("id", getId())
                .append("thirdId", getThirdId())
                .append("baseType", getBaseType())
                .append("currentDepositAmount", getCurrentDepositAmount())
                .append("depositAmount", getDepositAmount())
                .append("depositId", getDepositId())
                .append("depositMode", getDepositMode())
                .append("depositRatio", getDepositRatio())
                .append("depositType", getDepositType())
                .append("depositUniqueId", getDepositUniqueId())
                .append("remark", getRemark())
                .append("settleId", getSettleId())
                .append("parntId", getParntId())
                .append("createDept", getCreateDept())
                .append("createBy", getCreateBy())
                .append("createId", getCreateId())
                .append("createTime", getCreateTime())
                .append("updateBy", getUpdateBy())
                .append("updateId", getUpdateId())
                .append("updateTime", getUpdateTime())
                .append("delFlag", getDelFlag())
                .toString();
    }
}
